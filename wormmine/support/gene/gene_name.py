# Paulo Nuin May 2018


from sqlalchemy import create_engine

# db_string = "postgres://postgres:interwormmine@localhost/intermine_prod_264_185_3"
# db = create_engine(db_string)
# connection = db.connect() 


if __name__ == '__main__':


	gene_names = {}
	gene_file = open('gene_names.txt').read().splitlines()

	for line in gene_file:
		temp = line.split('\t')
		gene_names[temp[0]] = temp[1]

	for gene in gene_names:
		connection.execute("UPDATE gene SET name = %d, symbol = %d WHERE primaryidentifier = '%s'" % (gene_names[gene], gene_names[gene], gene))
