"""Synthetic metadata fixtures only; never fake SDK compilation or DLL loading."""
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import audit_sample_contract as tool


class ContractAuditTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def sample(self, entries):
        path = self.root / 'fixture.zip'
        with zipfile.ZipFile(path, 'w') as z:
            for name, text in entries.items():
                z.writestr(name, text)
        return path

    def test_configuration_scope_and_path_redaction(self):
        xml = r'''<Project xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
<ItemDefinitionGroup Condition="'$(Configuration)|$(Platform)'=='Release|x64'">
<ClCompile><RuntimeLibrary>MultiThreaded</RuntimeLibrary></ClCompile>
<Link><ModuleDefinitionFile>C:\private-sentinel\sample.def</ModuleDefinitionFile>
<AdditionalOptions>/INCLUDE:fixture_anchor /DEF:"C:\private-sentinel\sample.def"</AdditionalOptions>
<AdditionalDependencies>fixture.lib;%(AdditionalDependencies)</AdditionalDependencies></Link>
</ItemDefinitionGroup>
<ItemDefinitionGroup Condition="'$(Configuration)|$(Platform)'=='Debug|x64'">
<ClCompile><RuntimeLibrary>MultiThreadedDebug</RuntimeLibrary></ClCompile></ItemDefinitionGroup>
<Target Name="NeverExecute"><Exec Command="do-not-run-sample-code"/></Target></Project>'''
        result = tool.audit(self.sample({'fixture.vcxproj': xml}))
        props = result['files'][0]['project_properties']
        runtimes = [p for p in props if p['property'] == 'RuntimeLibrary']
        self.assertEqual(runtimes[0]['conditions'], ['Release|x64'])
        self.assertEqual(runtimes[1]['conditions'], ['Debug|x64'])
        self.assertNotIn('private-sentinel', json.dumps(result))
        self.assertFalse(result['native_tools_or_sample_code_executed'])

    def test_makefile_project_command_is_scoped_redacted_and_never_run(self):
        xml = r'''<Project>
<PropertyGroup Condition="'$(Configuration)|$(Platform)'=='Release|x64'">
<ConfigurationType>Makefile</ConfigurationType><PlatformToolset>v143</PlatformToolset>
<NMakeBuildCommandLine>nmake /f "C:\private-sentinel\fixture.mak" $(BuildTarget) &amp; echo never-run</NMakeBuildCommandLine>
<NMakeCleanCommandLine>del C:\private-sentinel\fixture.dll</NMakeCleanCommandLine>
<NMakeOutput>C:\private-sentinel\fixture.dll</NMakeOutput>
<NMakeIncludeSearchPath>C:\private-sentinel</NMakeIncludeSearchPath>
</PropertyGroup></Project>'''
        result = tool.audit(self.sample({'fixture.vcxproj': xml}))
        props = result['files'][0]['project_properties']
        build = next(x for x in props if x['property'] == 'NMakeBuildCommandLine')
        self.assertEqual(build['conditions'], ['Release|x64'])
        self.assertIn('fixture.mak', build['value']['file_reference_candidates'])
        self.assertEqual(build['value']['macro_names'], ['BuildTarget'])
        self.assertFalse(build['value']['command_executed'])
        self.assertFalse(build['value']['raw_command_exported'])
        self.assertNotIn('private-sentinel', json.dumps(result))
        self.assertEqual(next(x['value'] for x in props if x['property'] == 'NMakeOutput'), 'fixture.dll')

    def test_def_candidate_is_not_promoted_to_required_export(self):
        sample = self.sample({'fixture.def': 'LIBRARY fixture\nEXPORTS\n fixture_anchor @1\n user_initialize\nSECTIONS\n .data READ WRITE\n'})
        log = self.root / 'exports.log'
        log.write_text(' 1 0 00001000 user_initialize\n', encoding='utf-8')
        result = tool.audit(sample, export_log=log)
        self.assertEqual(result['candidate_exports_across_all_def_files'], ['fixture_anchor', 'user_initialize'])
        self.assertEqual(result['candidate_names_absent_from_log_not_proven_required'], ['fixture_anchor'])
        self.assertNotIn('.data', result['candidate_exports_across_all_def_files'])

    def test_flag_prefix_is_not_a_different_flag(self):
        found = tool.flags('/DEFAULTLIB:libcmt.lib /INCLUDE:fixture_anchor /NODEFAULTLIB:bad.lib')
        self.assertEqual([x['flag'] for x in found], ['/INCLUDE', '/NODEFAULTLIB'])

    def test_external_entities_rejected(self):
        xml = '<!DOCTYPE Project [<!ENTITY x SYSTEM "file:///private-sentinel">]><Project>&x;</Project>'
        result = tool.audit(self.sample({'fixture.vcxproj': xml}))
        self.assertEqual(result['issues'][0]['code'], 'TEXT_OR_XML_PARSE_REJECTED')
        self.assertEqual(result['files'], [])

    def test_path_traversal_member_is_not_extracted_or_read_as_contract(self):
        result = tool.audit(self.sample({'../escape.def': 'EXPORTS\n bad\n', 'safe.def': 'EXPORTS\n fixture_anchor\n'}))
        self.assertEqual(result['issues'][0]['code'], 'UNSAFE_MEMBER_SKIPPED')
        self.assertFalse((self.root.parent / 'escape.def').exists())
        self.assertEqual(result['candidate_exports_across_all_def_files'], ['fixture_anchor'])

    def test_bounds_are_reported(self):
        with patch.object(tool, 'MAX_MEMBER', 20):
            result = tool.audit(self.sample({'large.def': 'x' * 21}))
        self.assertEqual(result['issues'][0]['code'], 'MEMBER_TOO_LARGE')
        with patch.object(tool, 'MAX_FILES', 1):
            result = tool.audit(self.sample({'a.def': 'EXPORTS\n first\n', 'b.def': 'EXPORTS\n second\n'}))
        self.assertTrue(result['limited'])

    def test_makefile_commands_are_data_and_missing_contract_is_not_pass(self):
        result = tool.audit(self.sample({'makefile': 'LIBS=fixture.lib\nLDFLAGS=/DEF:sample.def /ENTRY:fixture_entry\nall:\n\techo never-execute\n'}))
        facts = result['files'][0]
        self.assertEqual(facts['library_candidates'], ['fixture.lib'])
        self.assertEqual(facts['link_flag_candidates'][0]['value'], 'sample.def')
        self.assertTrue(result['absence_of_candidates_is_not_proof_of_no_bootstrap_requirement'])

    def test_cli_refuses_existing_output(self):
        sample = self.sample({'sample.def': 'EXPORTS\n fixture_anchor\n'})
        out = self.root / 'existing'
        out.mkdir()
        sentinel = out / 'sentinel'
        sentinel.write_text('keep', encoding='utf-8')
        with patch('sys.argv', ['audit', '--sample-zip', str(sample), '--output-dir', str(out)]):
            self.assertEqual(tool.main(), 2)
        self.assertEqual(sentinel.read_text(), 'keep')


if __name__ == '__main__':
    unittest.main()
