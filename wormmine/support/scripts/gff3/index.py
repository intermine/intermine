gff = open('expanded_gff.gff').read().splitlines()
new_gff = open('expandedID_gff.gff', 'w')
tracker = 0
index = 0
for line in gff:
    temp = line.split('\t')
    if temp[2] == 'exon':
        index += 1
        new_line = line.replace('Parent=', 'ID=Exon' + str(index) + ';Parent=')
        new_gff.write(new_line + '\n')
    else:
        new_gff.write(line + '\n')

new_gff.close()
