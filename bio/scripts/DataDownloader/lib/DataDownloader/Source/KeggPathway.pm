package DataDownloader::Source::KeggPathways;

use Moose;
extends 'DataDownloader::Source::ABC';
use URI;
use autodie qw(open close);
use Ouch;


use Webservice::InterMine 0.9700;
use File::Path qw(mkpath);
use SOAP::Lite;

use constant {
    TITLE       => 'KEGG pathways',
    DESCRIPTION => 'Pathways from KEGG',
    SOURCE_LINK => 'http://www.genome.jp/kegg',
    SOURCE_DIR  => 'kegg',
    SERVICE_URL => 'http://soap.genome.jp/KEGG.wsdl',
    COMPARE     => 1,
};



sub BUILD {
   $wsdl = 'http://soap.genome.jp/KEGG.wsdl';
   $results = SOAP::Lite
             -> service($wsdl)
             -> list_pathways("dem");
   open(my $out, '>:utf8', $destination);
   foreach $path (@{$results}) {
      $out->print($path->{entry_id}\t$path->{definition});
   }
}
