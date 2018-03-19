# Paulo Nuin February 2018

import pandas as pd
from sqlalchemy import create_engine

db_string = "postgres://postgres:interwormmine@localhost/intermine_prod_263_185_4"
db = create_engine(db_string)
connection = db.connect()


def get_cds_length(cds):

    cds['Length'] = cds['end'] - cds['start'] + 1
    return cds['Length'].sum()


def check_cds_table():

    all = []
    result = connection.execute('select * from cds')
    for row in result:
        all.append(row)

    return(all)

if __name__ == '__main__':

    col_names = ['seqid', 'source', 'type', 'start', 'end', 'score', 'strand', 'phase', 'attributes']
    gff = pd.read_csv('../../../../datadir263/wormbase-gff3/final/c_elegans.PRJNA13758.WS263_index.removed.gff', sep='\t', names = col_names)

    cds = gff.loc[gff['type'] == 'CDS']

    print(len(check_cds_table()))

    cds_length = {}
    print('Reading GFF file')
    for k, g in cds.groupby(cds['attributes'].str.split(';').str[0]):
        print(k)
        cds_length[k.split('=')[-1]] = get_cds_length(g)


    for i in cds_length:
        print(i)
        connection.execute("UPDATE cds SET length = %d WHERE primaryidentifier = '%s'" % (cds_length[i], i))
