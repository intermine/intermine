s/$/;/
s/^/EXPLAIN ANALYSE /
p
p
s/;$/ LIMIT 500;/
p
