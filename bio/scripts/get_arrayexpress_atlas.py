#!/usr/bin/python

import sys
import urllib2
from urllib2 import URLError, HTTPError
import os
import time

try:
    import simplejson
    from simplejson.decoder import JSONDecodeError
except ImportError:
    print "You need to install simplejson to run this script.  Try: > easy_install simplejson"
    sys.exit(1)

if len(sys.argv) != 4:
    print "Usage:"
    print "\t", sys.argv[0], "experiment_id ensembl_ids_file output_dir"
    print "Where <ensembl_ids_file> is a single column text file with ensembl gene ids to retrieve."
    print "For example:"
    print "\t", sys.argv[0], "E-MTAB-62 ensembl_ids.txt /data/array-express"
    sys.exit(1)

output_dir = sys.argv[3]
if not os.path.exists(output_dir):
    print "Output directory doesn't exist: %s" % output_dir
    sys.exit(1)

def read_done_ids(filename):
    f = open("/".join([output_dir, filename]), "r")
    ids = []
    for id in f:
        ids.append(id.strip())
    f.close()
    return ids

found_file = open("/".join([output_dir, "found.txt"]), "a")
not_found_file = open("/".join([output_dir, "not_found.txt"]), "a")
failed_file = open("/".join([output_dir, "failed.txt"]), "a")
found_ids = read_done_ids("found.txt")
not_found_ids = read_done_ids("not_found.txt")
failed_ids = read_done_ids("failed.txt")

print "Already found ids: %d" % len(found_ids)
print "Already not found ids: %d" % len(not_found_ids)
print "Already failed ids: %d" % len(failed_ids)

def found_id(id):
    found_ids.append(id)
    found_file.write(id + "\n")
    found_file.flush()

def not_found_id(id):
    not_found_ids.append(id)
    not_found_file.write(id + "\n")
    not_found_file.flush()

def failed_id(id):
    failed_ids.append(id)
    failed_file.write(id + "\n")
    failed_file.flush()

def already_done(id):
    return (id in found_ids) or (id in not_found_ids) or (id in failed_ids)

ids_file = None
try:
    ids_file = open(sys.argv[2])
except IOError:
    print "Could not open file: %s" % sys.argv[2]
    sys.exit(1)


# TODO fetch experiments to check experiment exists and get pubmed ids
# http://www.ebi.ac.uk:80/gxa/api/v1?experiment=listAll&experimentInfoOnly&indent

geneStats = 'geneExpressionStatistics'
keys_to_keep = ['experimentInfo', 'arrayDesign', geneStats]
count = 0
found = 0

def readUrl(url):
    tries = 0
    while (tries < 10):
        try:
            tries += 1
            return simplejson.load(urllib2.urlopen(url))
        except HTTPError as err:
            print "Failed to open URL with code %s, waiting 2 mins before trying again, id: %s." % (err.code, id) 
            time.sleep(120)
            continue
        except JSONDecodeError:
            print "Failed to parse JSON, waiting 2 mins before trying again, id: %s." % id 
            time.sleep(120)
            continue
    raise ValueError


for id in ids_file:
    id = id.strip()

    if already_done(id):
        continue

    url = 'http://www.ebi.ac.uk:80/gxa/api/vx?experiment=%s&geneIs=%s&format=json&indent' % (sys.argv[1], id)
    print 'URL: %s' % url
    count += 1

    try:
        obj = readUrl(url)
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
            found_id(id)
        else:
            not_found_id(id)

            if count % 100 == 0:
                print "Found %.1f%% of %d ids" % (float(found)/count * 100, count)
    
    except ValueError:
        print "Failed to fetch %s" % id
        failed_id(id)

not_found_file.close()
found_file.close()
failed_file.close()
