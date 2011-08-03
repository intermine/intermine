from intermine.webservice import Service
import itertools

def lines_of(x):
    return chunker(0, x)

def chunker(i, x):
    d = {"accum": i}
    def func(y):
        i = d["accum"]
        grp = i / x
        i += 1
        d["accum"] = i
        return grp
    return func

col_width = 15
cols = 8
sep = '| '
ellipsis = '...'
line_width = col_width * cols + (cols - 1) * len(sep)
fit_to_cell = lambda a: a.ljust(col_width) if len(a) <= col_width else a[:col_width - len(ellipsis)] + ellipsis
hrule = "-" * line_width
summary = "\n%s: %d Alleles"

s = Service("www.flymine.org/query")

Gene = s.model.Gene

q = s.query(Gene).\
      add_columns("name", "symbol", "alleles.*").\
      filter(Gene.symbol == ["zen", "eve", "bib", "h"]).\
      filter(Gene.alleles.symbol == "*hs*").\
      outerjoin(Gene.alleles).\
      order_by("symbol")

for row in q.rows():
   print row

for gene in s.query(s.model.Gene).filter(s.model.Gene.symbol == ["zen", "eve", "bib", "h"]).add_columns(s.model.Gene.alleles):

    print summary % (gene.symbol, len(gene.alleles))
    print hrule

    for k, line_of_alleles in itertools.groupby(sorted(map(lambda a: a.symbol, gene.alleles)), lines_of(cols)):
        print sep.join(map(fit_to_cell, line_of_alleles))
    
    print "\nAllele Classes:"
    allele_classes = [(key, len(list(group))) for key, group in itertools.groupby(sorted(map(lambda x: x.alleleClass, gene.alleles)))]
    for pair in reversed(sorted(allele_classes, key=lambda g: g[1])):
        print "%s (%d)" % pair

    print hrule

