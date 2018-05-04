# Paulo Nuin March 2018

import pandas as pd
from sqlalchemy import create_engine

db_string = "postgres://postgres:interwormmine@localhost/intermine_prod_264_185_3"
db = create_engine(db_string)
connection = db.connect()


def get_cds_length(cds):

    cds['Length'] = cds['end'] - cds['start'] + 1
    return cds['Length'].sum()


def get_start_end(cds):

    return(cds['start'].min(), cds['end'].max())

def check_cds_table():

    all = []
    # result = connection.execute('select * from cds full join intermine_sequence on cds.sequenceid = intermine_sequence.id;')
    result = connection.execute('select * from cds full join intermine_sequence on cds.sequenceid = intermine_sequence.id;')
    for row in result:
        all.append(row)

    return(all)

if __name__ == '__main__':


    for i in check_cds_table():
        print(i)
        if i[-1] == 'org.intermine.model.bio.Sequence' and i[2] != None:
            sequenceid = i[-3]
            residue = i[-2]
            result = connection.execute('select * from intermine_sequence where id = %d' % (sequenceid))
            sequenceid = i[-3]
            residue = i[-2]
            length = i[4]
            result = connection.execute('select * from intermine_sequence where id = %d' % (sequenceid))
            for j in result:
                temp = j[3].split(',')
                temp[2] = str(length)
                new_residue = ','.join(temp)
                connection.execute("UPDATE intermine_sequence SET residues = '%s', length = %d WHERE id = %d" % (new_residue, length, sequenceid))
                print(sequenceid, new_residue)
