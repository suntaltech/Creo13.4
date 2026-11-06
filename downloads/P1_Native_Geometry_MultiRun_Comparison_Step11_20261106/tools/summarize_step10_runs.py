#!/usr/bin/env python3
"""Read-only input analysis. No Creo/SDK/network/process control. Standard library only."""
import argparse, collections, csv, datetime as dt, hashlib, json, statistics, sys
from pathlib import Path

VOLATILE = {'timestamp','requestId','candidateEvaluationTimestamp','elapsedMs','timingTraceId'}
COLUMNS = ['run_order','request_id','observed_command_start','observed_command_end','unresponsive','unresponsive_start','unresponsive_end','session_id','model_context','configuration_id','notes']

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def instant(value):
    if not value or value.strip().upper() == 'UNKNOWN': return None
    value = dt.datetime.fromisoformat(value.strip().replace('Z','+00:00'))
    if value.utcoffset() is None: raise ValueError('timestamp requires timezone')
    return value

def interval(start, end):
    a,b = instant(start),instant(end)
    if a is None or b is None: return None
    if b < a: raise ValueError('end before start')
    return (b-a).total_seconds()*1000

def observations(path):
    result,issues = {},[]
    if path is None: return result,['OBSERVATIONS_NOT_PROVIDED']
    with path.open(encoding='utf-8-sig',newline='') as f:
        reader=csv.DictReader(f)
        if not set(COLUMNS).issubset(reader.fieldnames or []): raise ValueError('missing observation columns')
        orders=set()
        for line,row in enumerate(reader,2):
            req=(row.get('request_id') or '').strip()
            if not req: continue # planned blank rows are not executed runs
            if req in result: raise ValueError('duplicate observation request_id at line '+str(line))
            order=int(row['run_order'])
            if order<1 or order in orders: raise ValueError('invalid/duplicate run_order')
            orders.add(order)
            state=row['unresponsive'].strip().upper()
            if state not in ['YES','NO','UNKNOWN']: raise ValueError('invalid unresponsive state')
            external=interval(row['observed_command_start'],row['observed_command_end'])
            pause=interval(row['unresponsive_start'],row['unresponsive_end'])
            if state=='NO' and (instant(row['unresponsive_start']) or instant(row['unresponsive_end'])): raise ValueError('NO contradicts pause timestamps')
            if state=='UNKNOWN' and (instant(row['unresponsive_start']) or instant(row['unresponsive_end'])): raise ValueError('UNKNOWN contradicts provided pause boundary')
            row['run_order']=order;row['unresponsive']=state;row['observedCommandDurationMs']=external
            row['pauseDurationMs']=pause if state=='YES' else None
            row['observationComplete']=external is not None and state!='UNKNOWN' and (state!='YES' or pause is not None)
            if not row['observationComplete']: issues.append('INCOMPLETE_EXTERNAL_OBSERVATION:'+req)
            result[req]=row
    return result,issues

def jsonl(path):
    with path.open(encoding='utf-8-sig') as f:
        rows=[json.loads(line) for line in f if line.strip()]
    if not rows: raise ValueError('empty JSONL')
    return rows

def same_series(rows):
    """No native identities are invented: only stable recorded fields are compared."""
    core=[{k:v for k,v in x.items() if k not in VOLATILE} for x in rows if not x['event'].startswith('TIMING_')]
    return hashlib.sha256(json.dumps(core,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest(),len(core)

def load_journals(root,issues):
    indexed={}
    for path in sorted(root.rglob('geometry-timing-step10-lifecycle-*.jsonl')):
        try:
            rows=jsonl(path);trace=rows[0]['timingTraceId']
            if [x['sequence'] for x in rows]!=list(range(1,len(rows)+1)): raise ValueError('non-contiguous lifecycle sequence')
            if any(x['timingTraceId']!=trace or x['collectorStep']!='STEP_10' or x['commandId']!='SISWReadOnlyGeometrySample10' for x in rows): raise ValueError('wrong trace/collector/command')
            if any(b['monotonicNs']<a['monotonicNs'] for a,b in zip(rows,rows[1:])): raise ValueError('non-monotonic lifecycle')
            prior=indexed.get(trace)
            if prior:
                common=min(len(prior['rows']),len(rows))
                if prior['rows'][:common]!=rows[:common]: raise ValueError('conflicting same-trace lifecycle files')
                if len(rows)<=len(prior['rows']): continue
            indexed[trace]={'rows':rows,'fileName':path.name,'sha256':digest(path)}
        except (ValueError,KeyError,TypeError,OSError) as error:
            issues.append('INVALID_LIFECYCLE:'+path.name+':'+type(error).__name__+':'+str(error))
    return indexed

def single_run(path,journals):
    rows=jsonl(path);req=rows[0]['requestId']
    if any(x['requestId']!=req or x['collectorStep']!='STEP_10' or x['commandId']!='SISWReadOnlyGeometrySample10' for x in rows): raise ValueError('mixed/wrong business request or command')
    if rows[0]['event']!='COLLECTION_STARTED' or rows[-1]['event']!='COLLECTION_SUMMARY': raise ValueError('missing first/final business event')
    final=rows[-1];trace=final['timingTraceId'];journal=journals[trace];life=journal['rows']
    scoped=[x for x in life if x['requestId']==req]
    def event(name):
        values=[x for x in scoped if x['event']==name]
        if len(values)!=1: raise ValueError('missing/duplicate '+name)
        return values[0]
    end=event('COMMAND_EXIT');full=event('RUN_OBSERVATION_TOTALS');tot=full['businessCountersThroughClose']['totals'];policy=full['stderrPolicyThroughClose']
    contexts=[x for x in rows if x['event']=='TIMING_COMMAND_CONTEXT']
    if len(contexts)!=1 or contexts[0]['commandEnteredMonotonicNs']!=end['beginMonotonicNs']: raise ValueError('command entry mismatch')
    entries=[x for x in life if x['event']=='COMMAND_ENTER' and x['enteredMonotonicNs']==end['beginMonotonicNs']]
    if len(entries)!=1 or end['elapsedNs']!=end['endMonotonicNs']-end['beginMonotonicNs']: raise ValueError('unmatched command span')
    if end['outcome']!='COLLECT_AND_LOG_CLOSE_RETURNED' or not full['businessWriterCloseCompleted']: raise ValueError('command or file close did not complete')
    if any(x['stderrMode']!=policy['effectiveMode'] for x in rows): raise ValueError('mixed stderr modes')
    n=len(rows)
    for key in ['businessOutputAttempts','businessOutputCompleted','writerWriteCalls','writerNewLineCalls','writerFlushCalls']:
        if tot[key]!=n: raise ValueError('incomplete output count '+key)
    if tot['businessOutputFailed'] or policy['selectionDecisionsAfterSuccessfulBusinessFlush']!=n or policy['mirrorSelectedAttempts']!=tot['stderrPrintlnCalls'] or policy['mirrorSelectedAttempts']+policy['stderrSuppressedRecords']!=n: raise ValueError('output/policy counters do not reconcile')
    phases=[x for x in scoped if x['event']=='PHASE_END'];begins=[x for x in scoped if x['event']=='PHASE_BEGIN']
    if not phases or len(phases)!=len(begins) or len({x['phase'] for x in phases})!=len(phases): raise ValueError('phase boundary count mismatch')
    for a,b in zip(begins,phases):
        if a['phase']!=b['phase'] or a['beginMonotonicNs']!=b['beginMonotonicNs'] or b['elapsedNs']!=b['endMonotonicNs']-b['beginMonotonicNs']: raise ValueError('phase span mismatch')
    groups=full['businessCountersThroughClose']['actionGroups']
    for target,source in [('readActionGroups','actionGroups'),('readActionNs','actionNs'),('readNullGroups','returnedNullGroups'),('readExceptionGroups','exceptionGroups')]:
        if tot[target]!=sum(x[source] for x in groups): raise ValueError('read aggregate mismatch')
    for phase in phases:
        d=phase['businessCounterDelta']
        if d['businessOutputCompleted']!=sum(x['timingPhase']==phase['phase'] for x in rows): raise ValueError('phase output mismatch')
        if any(v<0 for v in d.values()): raise ValueError('negative phase counter')
        grouped=[x for x in groups if x['phaseAndNormalizedOperation'].split(' :: ',1)[0]==phase['phase']]
        for target,source in [('readActionGroups','actionGroups'),('readActionNs','actionNs'),('readNullGroups','returnedNullGroups'),('readExceptionGroups','exceptionGroups')]:
            if d[target]!=sum(x[source] for x in grouped):raise ValueError('phase read aggregate mismatch')
    coreHash,coreRows=same_series(rows)
    warnings=[]
    if final['errors'] or final['truncated']: warnings.append('COLLECTION_ERRORS_OR_TRUNCATION')
    if end['journalState']['journalWriteErrors']: warnings.append('JOURNAL_ERROR_OBSERVED')
    if full['businessCountersThroughClose']['actionGroupsOverflowed']: warnings.append('READ_AGGREGATE_OVERFLOW')
    model=next((x for x in rows if x['event']=='MODEL_BEFORE'),{})
    return {'requestId':req,'traceId':trace,'businessFileName':path.name,'businessSha256':digest(path),'lifecycleFileName':journal['fileName'],'lifecycleSha256':journal['sha256'],
        'commandEnteredUtc':end['enteredUtc'],'commandExitedUtc':end['endUtc'],'commandCallbackMs':end['elapsedNs']/1e6,'elapsedMs':final['elapsedMs'],
        'readActionGroups':tot['readActionGroups'],'readMs':tot['readActionNs']/1e6,'outputMs':tot['observedOutputNs']/1e6,'stderrMs':tot['stderrPrintlnNs']/1e6,'flushMs':tot['writerFlushNs']/1e6,
        'businessRows':n,'stderrAttempts':tot['stderrPrintlnCalls'],'stderrSuppressedRecords':policy['stderrSuppressedRecords'],'mode':policy['effectiveMode'],'errors':final['errors'],'unknownReads':final['unknownReadValues'],
        'outcome':final['outcome'],'truncated':final['truncated'],'coreRows':coreRows,'stableBusinessSha256':coreHash,'modelRecordedContext':{k:model.get(k) for k in ['fileName','revision','versionStamp','isModified']},
        'phaseTimings':[{'phase':x['phase'],'elapsedMs':x['elapsedNs']/1e6,'readMs':x['businessCounterDelta']['readActionNs']/1e6,'outputMs':x['businessCounterDelta']['observedOutputNs']/1e6,'stderrMs':x['businessCounterDelta']['stderrPrintlnNs']/1e6,'flushMs':x['businessCounterDelta']['writerFlushNs']/1e6} for x in phases],
        'warnings':warnings}

def analyze(root,obs_path=None):
    issues=[];obs,observation_issues=observations(obs_path);journals=load_journals(root,issues);runs=[];seen={}
    for path in sorted(root.rglob('geometry-sampling-step10-*.jsonl')):
        try:
            run=single_run(path,journals);req=run['requestId']
            if req in seen:
                issues.append('DUPLICATE_REQUEST_NOT_COUNTED:'+req);continue
            seen[req]=True;run['observation']=obs.get(req);runs.append(run)
        except (ValueError,KeyError,TypeError,OSError,StopIteration) as error:
            issues.append('INVALID_BUSINESS:'+path.name+':'+type(error).__name__+':'+str(error))
    runs.sort(key=lambda x:(x['observation']['run_order'] if x['observation'] else 10**9,x['commandEnteredUtc']))
    pending=list(observation_issues)
    if len(runs)<3: pending.append('FEWER_THAN_THREE_DISTINCT_COMPLETED_REQUESTS')
    for x in runs:
        if x['observation'] is None:pending.append('MISSING_OBSERVATION:'+x['requestId'])
        issues.extend(w+':'+x['requestId'] for w in x['warnings'])
    for req in obs:
        if req not in seen: pending.append('OBSERVATION_WITHOUT_COMPLETED_REQUEST:'+req)
    declarations={}
    for key in ['session_id','model_context','configuration_id']:
        values=[x['observation'][key].strip() if x['observation'] else '' for x in runs];declarations[key]=sorted(set(values)-{''})
        if not values or any(not v or v.upper()=='UNKNOWN' for v in values):pending.append('MISSING_DECLARED_CONTEXT:'+key)
        elif len(set(values))!=1:issues.append('DECLARED_CONTEXT_CHANGED:'+key)
    if len({x['mode'] for x in runs})>1:issues.append('STDERR_MODE_CHANGED')
    if len({x['stableBusinessSha256'] for x in runs})>1:issues.append('STABLE_BUSINESS_FIELDS_DIFFER')
    if runs and all(x['observation'] for x in runs):
        orders=[x['observation']['run_order'] for x in runs]
        if orders!=list(range(1,len(runs)+1)):pending.append('RUN_ORDER_NOT_CONTIGUOUS_FROM_ONE')
        for a,b in zip(runs,runs[1:]):
            if instant(b['commandEnteredUtc'])<instant(a['commandExitedUtc']):issues.append('OVERLAPPING_OR_NONCONSECUTIVE_COMMAND_ORDER')
    selected={x['requestId'] for x in runs}
    for journal in journals.values():
        commands=[x for x in journal['rows'] if x['event']=='COMMAND_EXIT']
        positions=[i for i,x in enumerate(commands) if x['requestId'] in selected]
        if len(positions)>1 and any(x['requestId'] not in selected for x in commands[min(positions):max(positions)+1]):
            issues.append('UNPAIRED_COMMAND_WITHIN_SELECTED_SERIES')
    stats={}
    for key in ['elapsedMs','commandCallbackMs','readMs','outputMs','stderrMs','flushMs']:
        values=[x[key] for x in runs]
        stats[key]={'count':len(values),'min':min(values) if values else None,'median':statistics.median(values) if values else None,'max':max(values) if values else None}
    return {'schema':'P1_STEP11_CONTROLLED_STEP10_COMPARISON_V1','distinctCompletedRequests':len(runs),'minimumRequested':3,'status':'INVALID_OR_CHANGED_EVIDENCE' if issues else 'PENDING_REAL_RUNS_OR_OBSERVATIONS' if pending else 'RECORDS_COMPLETE_DECLARED_SAME_CONTEXT',
        'issues':issues,'pending':pending,'declaredContext':declarations,'declaredContextIsIndependentlyAuthenticated':False,'stableBusinessFieldsEqualAcrossProvidedRuns':len(runs)>1 and len({x['stableBusinessSha256'] for x in runs})==1,
        'statistics':stats,'runs':runs,'causalPerformanceGainEstablished':False,'uiHangRootCauseEstablished':False,'materialOccupancyOrFinalWallThicknessAssessed':False,'modelWriteAbsenceIndependentlyProven':False,
        'scope':'READ_ONLY_FILE_ANALYSIS_NO_SDK_NATIVE_MODEL_GUI_OR_NETWORK_CALLS'}

def write_report(report,out):
    out.mkdir(parents=True,exist_ok=False)
    (out/'comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    keys=['run_order','requestId','commandEnteredUtc','commandExitedUtc','elapsedMs','commandCallbackMs','readMs','outputMs','stderrMs','flushMs','mode','unresponsive','observed_command_start','observed_command_end','unresponsive_start','unresponsive_end','observedCommandDurationMs','pauseDurationMs','businessRows','stderrAttempts','outcome','unknownReads']
    with (out/'runs.csv').open('w',encoding='utf-8',newline='') as f:
        w=csv.DictWriter(f,fieldnames=keys,lineterminator='\n');w.writeheader()
        for run in report['runs']:
            obs=run['observation'] or {};row={k:run.get(k,obs.get(k)) for k in keys};w.writerow(row)
    with (out/'phases.csv').open('w',encoding='utf-8',newline='') as f:
        keys=['requestId','phase','elapsedMs','readMs','outputMs','stderrMs','flushMs'];w=csv.DictWriter(f,fieldnames=keys,lineterminator='\n');w.writeheader()
        for run in report['runs']:
            for phase in run['phaseTimings']:w.writerow(dict(phase,requestId=run['requestId']))
    (out/'README.md').write_text('状态：'+report['status']+'\n\n独立完成请求数：'+str(report['distinctCompletedRequests'])+'，要求至少3。外部观察缺失不按“未发生”或0填值。\n\n详见comparison.json中的issues/pending和runs.csv、phases.csv。RECORDS_COMPLETE只表示文件与申报观察齐备；同配置/连续运行由操作者声明，未被独立认证，不是性能因果、未响应根因、零写入或壁厚证明。\n',encoding='utf-8')

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--input',required=True,type=Path);parser.add_argument('--observations',type=Path);parser.add_argument('--out',required=True,type=Path);args=parser.parse_args()
    try:
        if not args.input.is_dir():raise ValueError('input directory not found')
        if args.out.exists():raise ValueError('output directory already exists; choose a new directory')
        report=analyze(args.input,args.observations);write_report(report,args.out)
        print(json.dumps({'status':report['status'],'distinctCompletedRequests':report['distinctCompletedRequests'],'issues':report['issues'],'pending':report['pending']},ensure_ascii=False))
        return 0 if report['status']=='RECORDS_COMPLETE_DECLARED_SAME_CONTEXT' else 2
    except (ValueError,KeyError,OSError,csv.Error) as error:
        print('INPUT_ERROR: '+str(error),file=sys.stderr);return 1

if __name__=='__main__':sys.exit(main())
