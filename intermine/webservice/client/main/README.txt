InterMine Webservice Client 1.0
6.2.2009

Use this with FlyMine release 16 and newer.  For contacting FlyMine release 15 please use older client
implementation (flymine-release-15.0-client.zip), template service changed in particular. You can make
sure that you are using right version just pointing your browser to http://connected_server/query/service/version
It displays version of InterMine deployed on the server. If the page on this server doesn't exist it
means that the server doesn't have web services or the version of the web services is 0.

For all the issues with versions please see documentation at http://intermine.org/wiki/WebService#version

Overview
========
There is javadoc documentation and samples in doc directory. Javadoc documentation to the
intermine-client library is in javadoc-client directory. javadoc-pathquery directory contains
javadoc that is useful when you want to create pathquery java object in programmatic way
adding adding constraints.
There is sample for each service showing how to write client using the service. Samples can be run
like ./compile-run.sh ListClient

Samples Overview
==================

List client
ListClient demonstrates using of InterMine list web service. This example
fetches all public lists containing FBgn0000606 gene.

Query client
The QueryClient demonstrates using of InterMine query web service.
This examples displays first 100 Drosophila  melanogaster genes sorted by the FlyBase identifier.

Template client
The TemplateClient demonstrates using of InterMine template web service. This example returns
first 100 predicted orthologues between two organisms sorted by FlyBase gene identifier.

GenesFinder client
The GenesFinder demonstrates command line query client fetching Genes or
SequenceFeatures located at specific positions with some tolerance. You can use
included sample-input.txt to try this client:
./compile-run.sh GenesFinder --file absolute_path_to_GenesFinder_directory/sample-input.txt --tolerance tolerance_value --type Gene

