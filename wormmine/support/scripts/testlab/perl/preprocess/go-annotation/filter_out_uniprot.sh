# Filters out UniProtKB records

grep -vE "^UniProtKB" $1 > $2

