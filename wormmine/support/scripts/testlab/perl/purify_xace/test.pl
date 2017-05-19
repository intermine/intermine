$str="<Txt>Initial conversion from geneace</Txt>";

$str =~ s/(<Text>|<Txt>)(.*?)(<)/$1<![CDATA[$2]]>$3/msg;

print $str."\n";