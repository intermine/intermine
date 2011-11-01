import sys
import os
sys.path.insert(0, os.getcwd())

from intermine.webservice import Service
import time

s = Service("localhost/intermine-test")
lazy = s.select("Department.*")
eager = s.select("Department.*", "Department.employees.*")

def do_work(q):
    res = q.results()
    age_sum = reduce(lambda x, y: x + reduce(lambda a, b: a + b.age, y.employees, 0), res, 0)
    assert(age_sum == 5798)

if __name__ == '__main__':
    tests = {"Lazy": lazy, "Eager": eager}
    n = 10
    print "Benchmarking %d iterations of " % n + ", ".join(tests.keys())
    for label, q in tests.iteritems():
        total = 0
        for t in range(n):
            a = time.time()
            do_work(q)
            b = time.time()
            total += b - a
        print "%s: total %s, avg: %s" % (label, total, total/n)

