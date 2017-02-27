#!/usr/bin/env perl
use strict;
use warnings;

use InterMine::Item::Document;
use InterMine::Model;

my $usage = <<USAGE;
usage: $0 /path/to/datasets.txt /path/to/bio/core/core.xml

This script takes an input text file containing metadata about
DataSources and DataSets and any references to Publications and
converts it to an `intermine-items-xml` file. The XML output of
this script can be set up as a new `static` DataSource which can
be used to attach metadata to the different DataSources and
DataSets defined in your `project.xml`.

The format of the input text file is 6-column, tab-delimited.
Below is the format of the table:

       (1)        (2)          (3)          (4)            (5)           (6)*
    DataType     Name      Description      URL        Reference(s)    Version

The 5th column in the table takes a comma-separated list
of references.  Each reference is a key-value pair separated
by a colon. The first part (key) corresponds to the Reference
name and the 2nd part refers to its value.

The 6th column is optional and may be used to specify the version
or the release date of the DataSet.

For example, a DataSet which has membership in a DataSource
(DataSource:Panther) or a Publication(s) related to a particular
DataSet/Source (Publication:23193289).

As an example, if the input was:
    DataSource  Panther            Orthologue and paralogue relationships based on the inferred speciation and gene duplication events in the phylogenetic tree   https://www.pantherdb.org   Publication:23193289
    DataSet     Panther data set   Panther orthologues from Yeast, Roundworm, Fruit Fly, Zebrafish, Human, Mouse and Rat and paralogues from Arabidopsis          https://www.pantherdb.org   DataSource:Panther	8.1

The resultant XML produced by this script will be like so:
    <item id="0_1" class="Publication" implements="">
       <attribute name="pubMedId" value="23193289" />
    </item>
    <item id="0_2" class="DataSource" implements="">
       <attribute name="name" value="Panther" />
       <attribute name="description" value="Orthologue and paralogue relationships based on the inferred speciation and gene duplication events in the phylogenetic tree" />
       <attribute name="url" value="https://www.pantherdb.org" />
       <collection name="publications">
          <reference ref_id="0_1" />
       </collection>
    </item>
    <item id="0_3" class="DataSet" implements="">
       <attribute name="version" value="8.1" />
       <reference name="dataSource" ref_id="0_2" />
       <attribute name="name" value="Panther data set" />
       <attribute name="description" value="Panther orthologues from Yeast, Roundworm, Fruit Fly, Zebrafish, Human, Mouse and Rat and paralogues from Arabidopsis" />
       <attribute name="url" value="https://www.pantherdb.org" />
    </item>

Normally, the script is invoked like so from the intermine root directory
(please replace the string "YOURMINE" with the name of your mine project):

    perl bio/scripts/make-datasets-xml.pl \
        YOURMINE/integrate/datasets.txt bio/core/core.xml \
        > YOURMINE/integrate/datasets.xml

Once the `datasets.xml` file has been generated, the following set of files
are to be configured like so (please replace the string "YOURMINE" with the
name of your mine project):

(1) Set up a new intermine-items-xml-file data source in YOURMINE/project.xml:

    <source name="YOURMINE-static" type="intermine-items-xml-file">
      <property name="src.data.file" location="datasets.xml"/>
    </source>

(2) Set up a YOURMINE/integrate/resources/YOURMINE-static_keys.properties file:

    DataSet.key_title = name
    DataSource.key_name = name

(3) Update YOURMINE/dbmodel/genomic_priorities.properties to give priority to
    the metadata from the `datasets.xml` file:

    DataSet.dataSource = YOURMINE-static, *
    DataSet.description = YOURMINE-static, *
    DataSet.url = YOURMINE-static, *

Author:
    Vivek Krishnakumar <vkrishna@jcvi.org>
USAGE

# print out usage if datasets txt file and/or genomic_model.xml file are not provided
die $usage if (@ARGV < 2);

my ($datasets_file, $model_file) = @ARGV;

my %data  = ();
my $model = new InterMine::Model(file => $model_file);
my $doc   = new InterMine::Item::Document(model => $model);

open DATASETS, "<", $datasets_file or die "Error: unable to open file: $!\n";
while (<DATASETS>) {
    chomp;
    my @line = split /\t/;

    # process all the publications first
    my @refs = ();
    if (defined $line[4]) {
        @refs = split /,/, $line[4];
        foreach my $ref (@refs) {
            my ($refName, $refValue) = split /:/, $ref;
            next unless ($refName eq "Publication");
            $data{$refName}{$refValue} = make_item($refName => (pubMedId => $refValue))
              if (not defined $data{$refName}{$refValue});
        }
    }

    # process the datasource/set
    $data{ $line[0] }{ $line[1] } = make_item(
        $line[0] => (
            name        => $line[1],
            description => $line[2],
            url         => $line[3],
        ),
    );

    # set all the references/collections
    foreach my $ref (@refs) {
        my ($refName, $refValue) = split /:/, $ref;
        my ($lcRefName, $refId) = (lcfirst $refName, $data{$refName}{$refValue});
        if ($line[0] eq "DataSource" and $refName eq "Publication") {
            $lcRefName .= "s";
            $refId = [$refId];
        }
        $data{ $line[0] }{ $line[1] }->set($lcRefName => $refId);
    }

    # set version number or release data for DataSet, if available
    $data{ $line[0] }{ $line[1] }->set(version => $line[5])
      if (defined $line[5] and $line[0] eq "DataSet");
}
close DATASETS;

$doc->close();    # write the xml
exit(0);

######### helper subroutines:

sub make_item {
    my @args = @_;
    my $item = $doc->add_item(@args);
    return $item;
}
