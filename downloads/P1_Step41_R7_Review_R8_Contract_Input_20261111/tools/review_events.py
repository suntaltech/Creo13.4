"""Recompute callback/probe facts from exact JSONL event names; no Creo calls."""
import argparse
import json
from pathlib import Path


def review(rows):
    start = max(i for i, r in enumerate(rows) if r['event'] == 'INIT_ENTER')
    init = rows[start]
    seg = [r for r in rows[start:] if (r['pid'], r['load_token']) == (init['pid'], init['load_token'])]
    # REGISTER_BEGIN is not a callback entry. Never use substring matching here.
    callback = [r['event'] for r in seg if r['event'] in ('BODY_FACE_COUNT_ENTER', 'BODY_FACE_COUNT_RETURN')]
    probes = [r['facts'] for r in seg if r['event'] == 'MESSAGE_BUFFER_PROBE_RETURN']
    codes = [r['facts']['pro_error'] for r in seg if r['event'].endswith(('_ACTION_ADD_RETURN', '_DESIGNATE_RETURN'))]
    ids = [r['facts']['command_id'] for r in seg if r['event'] == 'CMD_IDENTITY_BEFORE_DESIGNATE']
    return {'pid': init['pid'], 'load_token': init['load_token'],
            'body_callback_enter_observed': 'BODY_FACE_COUNT_ENTER' in callback,
            'body_callback_return_observed': 'BODY_FACE_COUNT_RETURN' in callback,
            'callback_events_observed': callback,
            'six_registration_codes': codes,
            'six_registration_codes_zero': codes == [0] * 6,
            'three_distinct_ids': len(ids) == len(set(ids)) == 3,
            'probe_results': probes,
            'precise_resource_path_proven': False}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--log', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    rows = [json.loads(s) for s in a.log.read_text(encoding='utf-8-sig').splitlines() if s.strip()]
    result = review(rows)
    # Exclusive creation: never overwrite an existing report.
    with a.output.open('x', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write('\n')


if __name__ == '__main__':
    main()
