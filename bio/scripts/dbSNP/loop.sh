#!/bin/bash
i=1
while [ $i -le 24 ]; do
	perl dbSNP.pl $i $i
	i=$((i+1))
done
