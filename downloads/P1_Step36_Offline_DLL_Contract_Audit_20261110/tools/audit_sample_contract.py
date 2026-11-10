"""Read-only sample text audit. No SDK compilation, MSBuild evaluation or DLL loading."""
import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

MAX_MEMBER = 1_048_576
MAX_TOTAL = 8_388_608
MAX_FILES = 100
SYMBOL = re.compile(r"[_?@A-Za-z][A-Za-z0-9_?@$.]*\Z")


def sha_file(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(65536), b''):
            h.update(block)
    return h.hexdigest()


def basename(value):
    return value.strip().strip('"').replace('\\', '/').rsplit('/', 1)[-1]


def decode(data):
    if data.startswith((b'\xff\xfe', b'\xfe\xff')):
        return data.decode('utf-16')
    return data.decode('utf-8-sig', errors='replace')


def libs(text):
    return sorted(set(re.findall(r'(?i)[A-Za-z0-9_.+-]+\.lib\b', text)))


def flags(text):
    result = []
    pattern = r'(?i)(?:/|-)(EXPORT|INCLUDE|ENTRY|DEF|WHOLEARCHIVE|NODEFAULTLIB)(?=[:\s]|$)(?::\s*("[^"\r\n]*"|[^\s;<>]+))?'
    for match in re.finditer(pattern, text):
        name, value = match.group(1).upper(), (match.group(2) or '').strip('"')
        if name in {'DEF', 'WHOLEARCHIVE', 'NODEFAULTLIB'}:
            value = basename(value)
            if value and not re.fullmatch(r'[A-Za-z0-9_.+-]+', value):
                value = 'UNRESOLVED_OR_COMPLEX_TOKEN'
        elif value and not SYMBOL.fullmatch(value):
            value = 'UNRESOLVED_OR_COMPLEX_TOKEN'
        item = {'flag': '/' + name, 'value': value or None}
        if item not in result:
            result.append(item)
    return result


def condition_summary(node, parents):
    found = []
    while node is not None:
        condition = node.attrib.get('Condition')
        if condition:
            # Preserve recognized configuration labels, never an arbitrary path expression.
            labels = re.findall(r'\b(?:Debug|Release)\|(?:x64|Win32|ARM64)\b', condition, re.I)
            found.extend(labels or ['OTHER_CONDITION_NOT_EVALUATED'])
        node = parents.get(node)
    return found or ['NO_LOCAL_CONDITION']


def project_facts(text):
    if re.search(r'<!\s*(?:DOCTYPE|ENTITY)\b', text, re.I):
        raise ValueError('DTD_OR_ENTITY_REJECTED')
    root = ET.fromstring(text)
    parents = {child: parent for parent in root.iter() for child in parent}
    result = []
    fields = {'PlatformToolset', 'ConfigurationType', 'WindowsTargetPlatformVersion',
              'RuntimeLibrary', 'ModuleDefinitionFile', 'EntryPointSymbol',
              'ForceSymbolReferences', 'AdditionalOptions', 'AdditionalDependencies',
              'TargetMachine', 'IgnoreAllDefaultLibraries', 'IgnoreSpecificDefaultLibraries',
              'PreprocessorDefinitions'}
    for node in root.iter():
        name = node.tag.rsplit('}', 1)[-1]
        if name not in fields:
            continue
        value = node.text or ''
        if name in {'AdditionalDependencies', 'IgnoreSpecificDefaultLibraries'}:
            derived = libs(value)
        elif name == 'PreprocessorDefinitions':
            derived = sorted(set(re.findall(r'(?:^|;)\s*([A-Za-z_][A-Za-z0-9_]*)', value)))
        elif name == 'AdditionalOptions':
            derived = flags(value)
        elif name == 'ModuleDefinitionFile':
            candidate = basename(value)
            derived = candidate if re.fullmatch(r'[A-Za-z0-9_.+-]+', candidate) else 'UNRESOLVED'
        elif name in {'EntryPointSymbol', 'ForceSymbolReferences'}:
            derived = [x for x in re.split(r'[;\s]+', value) if SYMBOL.fullmatch(x)]
        else:
            derived = value.strip() if re.fullmatch(r'[A-Za-z0-9_.-]+', value.strip()) else 'UNRESOLVED'
        result.append({'property': name, 'owner': parents[node].tag.rsplit('}', 1)[-1] if node in parents else None,
                       'conditions': condition_summary(node, parents),
                       'value': derived, 'unresolved_expression_present': '$(' in value or '%(' in value})
    return result


def def_exports(text):
    result = []
    active = False
    for line in text.splitlines():
        line = line.split(';', 1)[0].strip()
        if re.match(r'^EXPORTS\b', line, re.I):
            active = True
            line = re.sub(r'^EXPORTS\b', '', line, flags=re.I).strip()
        elif re.match(r'^(?:LIBRARY|DESCRIPTION|SECTIONS|STACKSIZE|HEAPSIZE|VERSION)\b', line, re.I):
            active = False
        if active and line:
            name = re.split(r'[\s=]', line, maxsplit=1)[0]
            if SYMBOL.fullmatch(name) and name not in result:
                result.append(name)
    return result


def source_facts(text):
    clean = re.sub(r'/\*.*?\*/|//[^\r\n]*', '', text, flags=re.S)
    callbacks = []
    for m in re.finditer(r'\b(user_initialize|user_terminate)\s*\(([^;{}]*)\)\s*(?:;|\{)', clean):
        parameters = re.sub(r'\s+', ' ', m.group(2)).strip()
        if len(parameters) <= 600 and re.fullmatch(r'[A-Za-z0-9_*,:\[\]\s()]*', parameters):
            callbacks.append({'name': m.group(1), 'parameters': parameters})
    identifiers = sorted(set(re.findall(r'\b(?:ProToolkit|ProTK|ProTk)[A-Za-z0-9_]*\b', clean)))[:200]
    return {'callback_declarations_or_definitions': callbacks,
            'sdk_identifier_candidates_not_export_requirements': identifiers}


def summarize(name, data):
    text = decode(data)
    extension = PurePosixPath(name).suffix.lower()
    record = {'name': basename(name), 'sha256': hashlib.sha256(data).hexdigest(),
              'bytes': len(data), 'condition_evaluation': 'NOT_PERFORMED'}
    if extension in {'.vcxproj', '.props', '.targets'}:
        record['project_properties'] = project_facts(text)
    elif extension == '.def':
        record['export_candidates'] = def_exports(text)
    elif extension in {'.cpp', '.c', '.h', '.hpp'}:
        record['source_facts'] = source_facts(text)
    else:
        clean = '\n'.join(line for line in text.splitlines() if not line.lstrip().startswith('#'))
        record['library_candidates'] = libs(clean)
        record['link_flag_candidates'] = flags(clean)
        record['make_variables_and_conditions_evaluated'] = False
    return record


def audit(sample_zip, headers=(), export_log=None):
    records, issues = [], []
    total = 0
    limited = False
    with zipfile.ZipFile(sample_zip) as archive:
        if len(archive.infolist()) > 5000:
            raise ValueError('ZIP_ENTRY_LIMIT')
        for item in archive.infolist():
            path = PurePosixPath(item.filename.replace('\\', '/'))
            if path.is_absolute() or '..' in path.parts or re.match(r'^[A-Za-z]:', str(path)):
                issues.append({'code': 'UNSAFE_MEMBER_SKIPPED'})
                continue
            suffix = path.suffix.lower()
            selected = suffix in {'.vcxproj', '.props', '.targets', '.def', '.mak', '.mk', '.c', '.cpp', '.h', '.hpp'} or path.name.lower() in {'makefile', 'nmakefile'}
            if item.is_dir() or not selected:
                continue
            if len(records) >= MAX_FILES or total + item.file_size > MAX_TOTAL:
                limited = True
                break
            if item.file_size > MAX_MEMBER:
                issues.append({'name': path.name, 'code': 'MEMBER_TOO_LARGE'})
                continue
            with archive.open(item) as stream:
                data = stream.read(MAX_MEMBER + 1)
            if len(data) > MAX_MEMBER:
                issues.append({'name': path.name, 'code': 'MEMBER_TOO_LARGE'})
                continue
            total += len(data)
            try:
                records.append(summarize(path.name, data))
            except (ValueError, ET.ParseError, UnicodeError):
                issues.append({'name': path.name, 'code': 'TEXT_OR_XML_PARSE_REJECTED'})
    for header in headers:
        with Path(header).open('rb') as stream:
            data = stream.read(MAX_MEMBER + 1)
        if len(data) > MAX_MEMBER:
            issues.append({'name': Path(header).name, 'code': 'HEADER_TOO_LARGE'})
        else:
            records.append(summarize(Path(header).name, data))
    candidates = sorted({x for record in records for x in record.get('export_candidates', [])})
    actual = None
    if export_log is not None:
        with Path(export_log).open('rb') as stream:
            raw = stream.read(MAX_MEMBER + 1)
        if len(raw) > MAX_MEMBER:
            raise ValueError('EXPORT_LOG_TOO_LARGE')
        actual = sorted(set(re.findall(r'^\s*\d+\s+[0-9A-Fa-f]+\s+[0-9A-Fa-f]{8,16}\s+([_?@A-Za-z][A-Za-z0-9_?@$.]*)\s*$', decode(raw), re.M)))
    return {'schema_version': 1, 'scope': 'OFFLINE_TEXT_AUDIT_ONLY_NO_SDK_BUILD_OR_DLL_LOAD',
            'sample_zip_sha256': sha_file(sample_zip), 'files': records, 'issues': issues,
            'limited': limited, 'candidate_exports_across_all_def_files': candidates,
            'export_names_from_supplied_log': actual,
            'candidate_names_absent_from_log_not_proven_required': sorted(set(candidates) - set(actual)) if actual is not None else None,
            'conditional_projects_imports_and_make_variables_evaluated': False,
            'absence_of_candidates_is_not_proof_of_no_bootstrap_requirement': True,
            'native_tools_or_sample_code_executed': False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sample-zip', required=True, type=Path)
    parser.add_argument('--dll-header', action='append', default=[], type=Path)
    parser.add_argument('--export-log', type=Path)
    parser.add_argument('--output-dir', required=True, type=Path)
    args = parser.parse_args()
    if len(args.dll_header) > 2:
        parser.error('At most two explicitly selected headers')
    try:
        result = audit(args.sample_zip, args.dll_header, args.export_log)
        args.output_dir.mkdir(parents=True, exist_ok=False)
        target = args.output_dir / 'sample-contract-audit.json'
        target.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        (args.output_dir / 'SHA256SUMS').write_text(sha_file(target) + '  ' + target.name + '\n', encoding='ascii')
        print('Static audit written; no native tool or sample code executed.')
    except (OSError, ValueError, zipfile.BadZipFile, RuntimeError) as error:
        print('Audit failed: ' + type(error).__name__ + '; existing outputs are not overwritten.', file=sys.stderr)
        return 2
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
