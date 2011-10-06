import sys
import os
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__) + "/.."))

from intermine.webservice import Service
from interminebio import RegionQuery, SequenceQuery
s = Service("squirrel.flymine.org/flymine", token="C1o3t1e0d4V06ep8xb47DdlFVMr")
q = RegionQuery(s, "D. melanogaster", ["Exon", "Intron"], ["2L:14614843..14619614", "Foo"])

print q.bed()
print q.fasta()
print q.gff3()

l = s.create_list(q)

print str(l)

sq = SequenceQuery(s, "Gene")

sq.add_sequence_features("Gene").where("symbol", "ONE OF", ["eve", "zen", "r"])

print sq.fasta()

sq.add_sequence_features("exons")

print sq.bed()
print sq.gff3()

