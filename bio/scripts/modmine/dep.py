#!/usr/bin/env python
#
# python dep.py < deprecations > new_file
#
# deals with multiple deprecations e.g.
#
#4057 2675
#4057 2675 146
#4057 2675 2269
#4057 2675 266
#4057 2675 35
#4057 2675 36
#4057 2675 37
#4057 2675 744
#4057, 2967
#
# is transformed in
#
# 4057,2675,146,2269,266,35,36,37,744,2967
#
# TODO: finish, and do the grep here as well
#

import os

live = {}

import sys
for line in sys.stdin:
    
    kk = line.split(',')[0:1]
    k = kk[0]
    vv = line.split(',')[1:]
#    v = vv[0]
    
    if k in live:
        if len(live[k]) < len(vv):
            live[k] = vv
        #print "KK", k 
    else:
        live[k] = vv
    for e in vv:
        #print "???", vv, "|", e
        if e in live:
           print "AAA", e
        else:
           if e not in live[k]: 
              #print "ADD ", e
              live[k].append(e) 

for kp,vp in live.items():
    pp=""
    for u in vp:
        pp = pp + u.replace('\n', '').lstrip() + ","
    print kp+","+pp.rstrip(',')
      
