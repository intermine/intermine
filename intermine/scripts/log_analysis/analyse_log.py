#/usr/bin/python
# encoding: UTF-8

import re
import sys
from collections import defaultdict

'''
Read an intermine.log resulting from running a build (from integrate directory) and extract the time
spent running ANALYSE. Print the analyse time for each table and total time spent running analyse.
Requires python 2.7+ to run.
'''

def heading(s):
	print '\n%s\n%s' % (s, '-' * len(s))

def analyse_log(log_file):
	ANALYSE = re.compile('Analysing table ([a-z,A-Z]+) took (\d+)')
	MS_IN_MIN = 60000.0

	analyse_total = 0
	analyse_tables = defaultdict(int)

	with open(log_file) as f:
		for line in f:
			match = ANALYSE.search(line)
			if match:
				analyse_total += int(match.group(2))
				analyse_tables[match.group(1)] += int(match.group(2))

	heading('Total Analyse time')
	print 'total: %dms - %.2fm' % (analyse_total, analyse_total / MS_IN_MIN)
	heading('Analyse time by table')
	for table in sorted(analyse_tables):
		took = analyse_tables[table]
		print '%s: %dms - %.2fm' % (table, took, took / MS_IN_MIN)


if __name__ == '__main__':
	if len(sys.argv) != 2:
		print '\nUsage: %s log_file' % sys.argv[0]
		exit(1)

	log_file = sys.argv[1]
	analyse_log(log_file)


