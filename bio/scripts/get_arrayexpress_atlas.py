#!/usr/bin/python

import sys
import urllib2
from urllib2 import URLError
import os
import time

try:
    import simplejson
except ImportError:
    print "You need to install simplejson to run this script.  Try: > easy_install simplejson"
    sys.exit(1)

if len(sys.argv) != 4:
    print "Useage:"
    print "\t", sys.argv[0], "experiment_id ensembl_ids_file output_dir"
    print "Where <ensembl_ids_file> is a single column text file with ensembl gene ids to retrieve."
    print "For example:"
    print "\t", sys.argv[0], "E-MTAB-62 ensembl_ids.txt /data/array-express"
    sys.exit(1)

ids_file = None
try:
    ids_file = open(sys.argv[2])
except IOError:
    print "Could not open file: %s" % sys.argv[2]
    sys.exit(1)

output_dir = sys.argv[3]
if not os.path.exists(output_dir):
    print "Output directory doesn't exist: %s" % output_dir
    sys.exit(1)

not_found = open("/".join([output_dir, "not_found.txt"]), "w")

# TODO fetch experiments to check experiment exists and get pubmed ids
# http://www.ebi.ac.uk:80/gxa/api/v1?experiment=listAll&experimentInfoOnly&indent

geneStats = 'geneExpressionStatistics'
keys_to_keep = ['experimentInfo', 'arrayDesign', geneStats]
count = 0
found = 0

for id in ids_file:
    id = id.strip()
    url = 'http://www.ebi.ac.uk:80/gxa/api/vx?experiment=%s&geneIs=%s&format=json&indent' % (sys.argv[1], id)
    print 'URL: %s' % url
    obj = simplejson.load(urllib2.urlopen(url))
    count += 1

    gene_results = {}
    results = obj['results'][0]
    if geneStats in results:
        print "Found - %s" % id
        found += 1
        for key in keys_to_keep:
            gene_results[key] = results[key]
    
        # reform as the original JSON object
        gene = {'results': [gene_results]}
        outfile = open(output_dir + "/" + id + ".json", "w")
        outfile.write(simplejson.dumps(gene, sort_keys=True, indent=4 * ' '))
        outfile.close()
    else:
        not_found.write(id)

    if count % 100 == 0:
        print "Found %.1f%% of %d ids" % (float(found)/count * 100, count)

    time.sleep(1)

not_found.close()
