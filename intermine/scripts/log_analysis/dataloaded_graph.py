#/usr/bin/python
# encoding: UTF-8

import re
import sys
from datetime import datetime
from collections import defaultdict

import matplotlib.pyplot as plt


'''
Read an intermine.log resulting from running a build (from integrate directory) and extract the objects
per minute for each data source. Render the obj/min times on a graph. This requires matplotlib to be
installed, e.g. with 'sudo pip install matplotlib'.
'''

OBJECTS = re.compile('Finished dataloading (\d+) objects at (\d+) objects per minute \(\d+ ms total\) for source (\w+(-\w+)*)')

def graph_times(log_file, title):

	with open(log_file) as f:
		sources = defaultdict(list)
		for line in f:
			# find 'Finished dataloading...' lines
			m = OBJECTS.search(line)
			if m:
				print line.strip()
				timestamp = line.split('INFO')[0].strip()
				t = datetime.strptime(timestamp, '%Y-%m-%d %H:%M:%S')
				print timestamp, t, m.group(1), m.group(2), m.group(3)
				source = m.group(3)

				# store tuples of timestamp, total objs and objs per minute for each source
				sources[source].append((t, int(m.group(1)), int(m.group(2))))

	# now we only want the most recent entry for a particular source
	objs = {}
	per_min = {}
	for source in sources:
		latest = None
		for t, total, rate in sources[source]:
			if not latest or t > latest:
				objs[t] = total
				per_min[t] = rate
				latest = t


	x = []
	y = []
	s = []
	for i, t in enumerate(sorted(objs)):
		print t, objs[t], per_min[t]
		x.append(i)
		y.append(per_min[t])
		# make each marker size proportional to number of objects
		s.append(objs[t] / 10000)

	colour = '#6B7F01'
	plt.scatter(x, y, s=s, facecolor=colour, edgecolor=colour)
	plt.plot(x, y, color=colour)
	plt.ylim(ymin=0)
	plt.xlim(xmin=-1)
	plt.xlabel('Data source number')
	plt.ylabel('Objects per Minute')
	plt.title(title)
	plt.show()


if __name__ == '__main__':
	if len(sys.argv) != 3:
		print '\nUsage: %s log_file chart_title' % sys.argv[0]
		exit(1)

	log_file = sys.argv[1]
	title = sys.argv[2]
	graph_times(log_file, title)