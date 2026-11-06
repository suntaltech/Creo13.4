"""Metadata/analysis checks. Real evidence path supplied externally; no generated native logs."""
import csv,importlib.util,json,shutil,sys,tempfile,unittest
from pathlib import Path
root=Path(__file__).resolve().parents[1];spec=importlib.util.spec_from_file_location('comparison',root/'tools/summarize_step10_runs.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
real=Path(sys.argv[1]) if len(sys.argv)>1 else None
if len(sys.argv)>1:del sys.argv[1]
class ComparisonTest(unittest.TestCase):
 def observation(self,directory,**changes):
  row={k:'' for k in m.COLUMNS};row.update(run_order='1',request_id='METADATA_ONLY',unresponsive='YES');row.update(changes)
  p=Path(directory)/'observations.csv'
  with p.open('w',newline='') as f:
   w=csv.DictWriter(f,fieldnames=m.COLUMNS);w.writeheader();w.writerow(row)
  return p
 def test_missing_pause_is_unknown_not_zero(self):
  with tempfile.TemporaryDirectory() as td:
   rows,issues=m.observations(self.observation(td));self.assertIsNone(rows['METADATA_ONLY']['pauseDurationMs']);self.assertFalse(rows['METADATA_ONLY']['observationComplete']);self.assertTrue(issues)
 def test_timezone_required(self):
  with self.assertRaises(ValueError):m.instant('2026-11-06T12:00:00')
 def test_negative_interval_rejected(self):
  with self.assertRaises(ValueError):m.interval('2026-11-06T12:00:02+08:00','2026-11-06T12:00:01+08:00')
 def test_no_pause_does_not_invent_zero_duration(self):
  with tempfile.TemporaryDirectory() as td:
   rows,issues=m.observations(self.observation(td,unresponsive='NO',observed_command_start='2026-11-06T12:00:00+08:00',observed_command_end='2026-11-06T12:00:08+08:00'));self.assertIsNone(rows['METADATA_ONLY']['pauseDurationMs']);self.assertTrue(rows['METADATA_ONLY']['observationComplete']);self.assertFalse(issues)
 def test_contradictory_no_pause_rejected(self):
  with tempfile.TemporaryDirectory() as td:
   with self.assertRaises(ValueError):m.observations(self.observation(td,unresponsive='NO',unresponsive_start='2026-11-06T12:00:00+08:00'))
 def test_empty_directory_pending(self):
  with tempfile.TemporaryDirectory() as td:
   r=m.analyze(Path(td));self.assertEqual(r['distinctCompletedRequests'],0);self.assertEqual(r['status'],'PENDING_REAL_RUNS_OR_OBSERVATIONS');self.assertIsNone(r['statistics']['elapsedMs']['median'])
 @unittest.skipUnless(real and real.is_dir(),'supply original real Step10 evidence directory')
 def test_real_one_run_not_three(self):
  r=m.analyze(real);self.assertEqual(r['distinctCompletedRequests'],1);self.assertEqual(r['runs'][0]['businessRows'],6432);self.assertEqual(r['runs'][0]['stderrAttempts'],27);self.assertEqual(r['runs'][0]['readActionGroups'],29415);self.assertEqual(r['status'],'PENDING_REAL_RUNS_OR_OBSERVATIONS');self.assertFalse(r['stableBusinessFieldsEqualAcrossProvidedRuns'])
 @unittest.skipUnless(real and real.is_dir(),'supply original real Step10 evidence directory')
 def test_three_copies_count_once(self):
  with tempfile.TemporaryDirectory() as td:
   p=Path(td);business=next(real.glob('geometry-sampling*'));life=next(real.glob('*lifecycle*'))
   for i in range(3):
    d=p/str(i);d.mkdir();shutil.copyfile(business,d/business.name);shutil.copyfile(life,d/life.name)
   r=m.analyze(p);self.assertEqual(r['distinctCompletedRequests'],1);self.assertEqual(r['status'],'INVALID_OR_CHANGED_EVIDENCE');self.assertEqual(sum('DUPLICATE_REQUEST_NOT_COUNTED' in x for x in r['issues']),2)
 @unittest.skipUnless(real and real.is_dir(),'supply original real Step10 evidence directory')
 def test_missing_lifecycle_not_success(self):
  with tempfile.TemporaryDirectory() as td:
   p=Path(td);f=next(real.glob('geometry-sampling*'));shutil.copyfile(f,p/f.name);r=m.analyze(p);self.assertEqual(r['distinctCompletedRequests'],0);self.assertEqual(r['status'],'INVALID_OR_CHANGED_EVIDENCE')
if __name__=='__main__':unittest.main()
