from intermine.query import Query
from intermine.model import Model

m = Model('http://www.flymine.org/query/service/model')
q = Query(m)

q.name = 'Foo'
q.description = 'a query made out of pythons'
q.add_view("Gene.name Gene.symbol")
q.add_constraint('Gene', 'LOOKUP', 'eve')
q.add_constraint('Gene.length', '>', 50000)
q.add_constraint('Gene', 'Clone')
q.add_constraint('Gene.symbol', 'ONE OF', ['eve', 'zen'])
q.add_join('Gene.alleles')
q.add_path_description('Gene', 'One of those gene-y things')
print q.to_xml()
print q.to_formatted_xml()
