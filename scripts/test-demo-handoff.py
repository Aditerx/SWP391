"""Verify the handoff index against an existing pristine disposable local fixture. SELECT only."""
from pathlib import Path
import argparse
import contextlib
import copy
import io
import re
import runpy
import sys
import unittest
import uuid

from demo_handoff import catalog, render

ROOT=Path(__file__).resolve().parents[1]


class HandoffAcceptance(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.output=ROOT/'artifacts/runs'/('handoff-check-'+uuid.uuid4().hex[:10])/'SYSTEM-DATA.md'
        old=sys.argv
        try:
            sys.argv=['build-system-data.py','--anchor',ARGS.anchor,'--from-local-pg',ARGS.database,'--output',str(cls.output)]
            with contextlib.redirect_stdout(io.StringIO()):ns=runpy.run_path(str(ROOT/'scripts/build-system-data.py'))
        finally:sys.argv=old
        cls.rows=ns['exported_rows'];cls.design=ns['tables'];cls.data=cls.output.read_bytes()

    def test_catalog_matches_approved_fresh_scenarios(self):
        index=catalog(self.rows,self.design);classes={c['key']:c for c in index['classes']}
        self.assertEqual([f'N{i:02}' for i in range(1,13)],list(classes))
        self.assertEqual((1,2,3),(classes['N01']['paidRegistered'],classes['N05']['paidRegistered'],classes['N11']['paidRegistered']))
        self.assertEqual(7,len(index['refunds']));self.assertEqual(2,len(index['proposals']))
        rf02=next(f for f in index['refunds'] if f['key']=='RF02')
        self.assertEqual(('M04','N06','AwaitingReceiver',1200000),
                         (rf02['memberKey'],rf02['classKey'],rf02['state'],rf02['amount']))

    def test_actual_ids_and_foreign_keys_survive_nonsequential_remapping(self):
        before=catalog(self.rows,self.design);data=copy.deepcopy(self.rows)
        for u in data['users']:u['user_id']+=1000
        for c in data['classes']:
            c['class_id']+=2000
            if c['coach_id'] is not None:c['coach_id']+=1000
        for room in data['rooms']:room['room_id']+=3000
        for s in data['sessions']:
            s['session_id']+=4000;s['class_id']+=2000;s['room_id']+=3000
            if s['coach_id'] is not None:s['coach_id']+=1000
        for e in data['class_enrollments']:e['class_id']+=2000;e['member_id']+=1000
        for i in data['invoices']:
            i['invoice_id']+=6000;i['member_id']+=1000
            for field in ['class_id','entitlement_class_id']:
                if i.get(field) is not None:i[field]+=2000
        for p in data['normal_proposals']:
            p['id']+=5000;p['class_id']+=2000
            if p['target_class_id'] is not None:p['target_class_id']+=2000
        for r in data['payment_resolutions']:r['id']+=7000;r['invoice_id']+=6000
        for f in data['normal_refunds']:f['id']+=7000;f['class_id']+=2000
        for rows in data.values():rows.reverse()
        after=catalog(data,self.design)
        for a,b in zip(before['classes'],after['classes']):
            self.assertEqual(a['key'],b['key']);self.assertEqual(a['classId']+2000,b['classId'])
            self.assertEqual(a['paidRegistered'],b['paidRegistered'])
            self.assertEqual(a['firstSessionId']+4000,b['firstSessionId'])
            self.assertEqual(a['firstRoom']['room_id']+3000,b['firstRoom']['room_id'])
            self.assertEqual(a['classCoach']['user_id']+1000,b['classCoach']['user_id'])
        for a,b in zip(before['refunds'],after['refunds']):
            self.assertEqual(a['key'],b['key']);self.assertEqual(a['refundId']+7000,b['refundId'])
            self.assertEqual(a['invoiceId']+6000,b['invoiceId'])

    def test_missing_class_or_ambiguous_actor_cannot_render(self):
        data=copy.deepcopy(self.rows);data['classes']=[c for c in data['classes'] if not c['class_name'].endswith(' N05')]
        with self.assertRaisesRegex(ValueError,'N05'):render(data,self.design)
        data=copy.deepcopy(self.rows);data['users'].append(copy.deepcopy(data['users'][0]))
        with self.assertRaisesRegex(ValueError,'fixture binding'):render(data,self.design)

    def test_refund_cannot_silently_bind_another_invoice(self):
        index=catalog(self.rows,self.design);rf02=next(f for f in index['refunds'] if f['key']=='RF02')
        data=copy.deepcopy(self.rows)
        next(r for r in data['payment_resolutions'] if r['id']==rf02['refundId'])['invoice_id']=-391
        with self.assertRaisesRegex(ValueError,'RF02'):catalog(data,self.design)

    def test_index_does_not_mutate_exported_rows_or_include_proof_fields(self):
        before=copy.deepcopy(self.rows);text=render(self.rows,self.design)
        self.assertEqual(before,self.rows)
        self.assertNotIn('password_hash',text);self.assertNotIn('bank_reference',text)
        self.assertNotIn('accountNumber=',text)
        for anchor in ['normal-class-index','demo-actor-index','demo-financial-index']:
            self.assertIn('id="'+anchor+'"',text)

    def test_same_snapshot_exports_identically_again(self):
        old=sys.argv
        try:
            sys.argv=['build-system-data.py','--anchor',ARGS.anchor,'--from-local-pg',ARGS.database,'--output',str(self.output),'--check']
            with contextlib.redirect_stdout(io.StringIO()):runpy.run_path(str(ROOT/'scripts/build-system-data.py'))
        finally:sys.argv=old
        self.assertEqual(self.data,self.output.read_bytes())


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--anchor',required=True);p.add_argument('--database',required=True)
    ARGS=p.parse_args()
    if not re.fullmatch(r'scms_schema_demo_phase1_[a-z0-9_]+',ARGS.database):p.error('Pristine disposable local fixture only')
    sys.argv=[sys.argv[0]];unittest.main(verbosity=2)
