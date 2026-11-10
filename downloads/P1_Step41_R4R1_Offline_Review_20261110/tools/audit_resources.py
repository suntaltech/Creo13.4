"""Offline structural audit; never loads a DLL or calls a Creo SDK API."""
import argparse
import hashlib
import json
from pathlib import Path

FILES = {
    'p1_fc.txt': ['P1_BODY_FACE_COUNT_LABEL', 'P1_BODY_FACE_COUNT_HELP', 'P1_BODY_FACE_COUNT_DESC'],
    'p1_readonly_model_info.txt': ['P1_READONLY_MODEL_INFO_LABEL', 'P1_READONLY_MODEL_INFO_HELP', 'P1_READONLY_MODEL_INFO_DESC'],
    'p1_readonly_ping.txt': ['P1_READONLY_PING_LABEL', 'P1_READONLY_PING_HELP', 'P1_READONLY_PING_DESCRIPTION'],
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def audit_resource(data, keys):
    result = {'sha256': digest(data), 'byte_count': len(data),
              'has_utf8_bom': data.startswith(b'\xef\xbb\xbf'),
              'has_nul': b'\x00' in data, 'crlf_count': data.count(b'\r\n'),
              'lf_count': data.count(b'\n'), 'sdk_parser_executed': False}
    try:
        text = data.decode('ascii')
    except UnicodeDecodeError:
        result['ascii_decode_pass'] = False
        result['structural_parity_pass'] = False
        return result
    lines = text.splitlines()
    entries = []
    for key in keys:
        indices = [i for i, line in enumerate(lines) if line == key]
        value = lines[indices[0] + 1] if len(indices) == 1 and indices[0] + 1 < len(lines) else None
        entries.append({'key': key, 'occurrences': len(indices),
                        'line_1_based': indices[0] + 1 if len(indices) == 1 else None,
                        'key_ascii_length': len(key), 'value': value,
                        'value_ascii_length': len(value) if value is not None else None})
    expected = len(lines) == 12 and all(lines[i] == key for i, key in zip((0, 4, 8), keys))
    expected = expected and all(lines[i] == '' for i in (2, 3, 6, 7, 10, 11))
    expected = expected and all(e['occurrences'] == 1 and e['value'] for e in entries)
    result.update(ascii_decode_pass=True, entries=entries,
                  structural_parity_pass=bool(expected),
                  structural_parity_definition='three key/value/blank/blank groups; observed common layout only')
    return result


def audit_log(data):
    rows = [json.loads(line) for line in data.decode('utf-8-sig').splitlines() if line.strip()]
    start = max(i for i, row in enumerate(rows) if row['event'] == 'INIT_ENTER')
    init = rows[start]
    seg = [row for row in rows[start:] if (row['pid'], row['load_token']) == (init['pid'], init['load_token'])]
    identities = [row['facts'] for row in seg if row['event'] == 'CMD_IDENTITY_BEFORE_DESIGNATE']
    ids = [int(row['command_id'], 16) for row in identities]
    statuses = [row['facts']['pro_error'] for row in seg if row['event'].endswith(('_ACTION_ADD_RETURN', '_DESIGNATE_RETURN'))]
    # Publish equality evidence without republishing raw pointer values.
    mapping = [{k: v for k, v in row.items() if k != 'command_id'} for row in identities]
    for i, row in enumerate(mapping):
        row['id_equality_group'] = ids.index(ids[i]) + 1
    return {'input_log_sha256': digest(data), 'pid': init['pid'], 'load_token': init['load_token'],
            'older_rows_ignored': start, 'identity_mapping': mapping,
            'identity_count': len(ids), 'distinct_id_count': len(set(ids)),
            'all_three_ids_distinct': len(ids) == 3 and len(set(ids)) == 3,
            'sdk_return_codes': statuses, 'six_returns_zero': statuses == [0] * 6,
            'init_return_codes': [row['facts']['return_code'] for row in seg if row['event'] == 'INIT_BEFORE_RETURN'],
            'body_action_enter_observed': any(row['event'] == 'BODY_FACE_COUNT_ENTER' for row in seg),
            'ui_visibility_proven_by_log': False, 'sdk_root_cause_established': False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resources', required=True, type=Path, help='Explicit directory with the three known resource files')
    parser.add_argument('--log', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path, help='New report directory; must not already exist')
    args = parser.parse_args()
    resources = {name: audit_resource((args.resources / name).read_bytes(), keys) for name, keys in FILES.items()}
    logs = audit_log(args.log.read_bytes())
    args.output.mkdir(parents=True, exist_ok=False)
    for name, report in [('resource-review.json', resources), ('runtime-review.json', logs)]:
        (args.output / name).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
