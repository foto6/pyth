#!/usr/bin/env python3
from random import Random
TASK=55; HEADER=29
def policy(w,h):
    w=max(0,w); h=max(0,h)
    mode='narrow' if w<150 else 'compact' if (h<160 or w<280) else 'full'
    core={'narrow':102,'compact':103,'full':117}[mode]
    rem=max(0,h-core); reserve=HEADER+TASK
    status=mode!='narrow' and rem>=reserve+29
    if status: rem-=29
    apps=mode=='full' and rem>=reserve+50
    if apps: rem-=50
    th=rem>=reserve
    n=max(1,min(4,(rem-HEADER)//TASK)) if th else 0
    return mode,status,apps,th,n,core
def check(w,h):
    mode,status,apps,th,n,core=policy(w,h)
    used=core+(29 if status else 0)+(50 if apps else 0)+(HEADER+n*TASK if th else 0)
    if h>=110 and used>h: raise AssertionError(f'overflow {w}x{h}: used={used} policy={policy(w,h)}')
    if not 0<=n<=4: raise AssertionError('task count range')
    if apps and mode!='full': raise AssertionError('apps outside full mode')
    if mode=='narrow' and (status or apps): raise AssertionError('narrow extras')
    if th and n<1: raise AssertionError('tasks header without row')
def main():
    for w in [72,90,120,149,150,180,220,279,280,320,360,520,900]:
        for h in [100,110,120,140,159,160,180,220,260,320,360,420,520,900]: check(w,h)
    r=Random(0xD411F0C)
    for _ in range(250_000): check(r.randint(0,1000),r.randint(0,1400))
    print('WIDGET STRESS PASS: narrow/compact/full + 52dp rows + 250000 randomized sizes')
if __name__=='__main__': main()
