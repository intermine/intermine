#!/usr/bin/env perl

use strict;

die("Usage: $0 <host> <mine>\n") unless @ARGV==2;

my $minehost = shift @ARGV;
my $mine = shift @ARGV;

my $list = `psql -h $minehost -t $mine -c "select proteomeId from organism"`;

my @list = split(/\n/,$list);

map { $_ =~ s/\s+//g } @list;

my $inList = '('.join(',',@list).')';

print "Extracted ".scalar(@list)." proteome ids\n";

my $pac_out = `mysql -h dbcompgen PAC2_0 --skip-column-names -e "select id,jBrowsename from proteome where id in $inList"`;

open(FIL,">web.properties.links");

print FIL "## created by makeWebPropertiesLinks.pl at ".localtime(time)."\n";

my @lines = split(/\n/,$pac_out);

foreach my $line (@lines) {
  my ($id,$name) = split(/\s+/,$line);
  print FIL "attributelink.JBrowse.Gene.".$id.
            ".chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $name."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";
  print FIL "attributelink.JBrowse.Transcript.".$id.
            ".chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $name."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";
  print FIL "attributelink.JBrowse.Protein.".$id.
            ".transcripts.chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $name."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";

  print FIL "attributelink.Phytozome.Gene.".$id.
            ".primaryIdentifier.url=/pz/portal.html#!gene?organism=".$name.
            "&searchText=locusName:<<attributeValue>>\n";
  print FIL "attributelink.Phytozome.Transcript.".$id.
            ".primaryIdentifier.url=/pz/portal.html#!gene?organism=".$name.
            "&searchText=transcriptName:<<attributeValue>>\n";
  print FIL "attributelink.Phytozome.Protein.".$id.
            ".transcript.primaryIdentifier.url=/pz/portal.html#!gene?organism=".$name.
            "&searchText=peptideName:<<attributeValue>>\n";
}

print FIL "## end of section created by makeWebPropertiesLinks.pl\n";

close(FIL);



