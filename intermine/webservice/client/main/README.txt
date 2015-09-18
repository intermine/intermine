InterMine Webservice Client 2.0
===============================
07-04-2011

This Java client library is for use with Web services
attached to InterMine data-warehouses. It abstracts
and simplifies techniques for making structured
queries on remote databases using the InterMine
REST interface.


Overview
--------

This package contains the InterMine code to connect to
webservices, as well as all dependencies. It also includes
documentation and samples to help you get started. 

For further documentation, please visit:
  - http://extrac.sysbiol.ac.uk/wiki/JavaClient
  - http://www.flymine.org/download/docs/java-client-docs/

Running A Query
---------------

We provide a convenience script to make running your queries
easier - called "compile-run.sh". To run a query, 
saved as "examplequery/QueryClient.java" you can use either:

  ./compile-run.sh examplequery/QueryClient.java

  or

  ./compile-run.sh examplequery.QueryClient


Samples Overview
----------------

We provide the following sample programs to demonstrate ways to use the services:

 * Query client:
 ~~~~~~~~~~~~~~~
   The QueryClient demonstrates the use of the InterMine Query Web Service.
   This resource can be used to perform arbitrary, complex queries over
   the full set of data in the InterMine data warehouse.

   This example displays the first 100 Drosophila melanogaster genes sorted by the FlyBase identifier.

 * Template client:
 ~~~~~~~~~~~~~~~~~~
   The TemplateClient demonstrates the use of the InterMine Template Web Service. 
   This resource can be used to automate access to the predefined queries 
   which an InterMine data warehouse has made available.
   
   This example returns the first 100 predicted orthologues between two organisms sorted by FlyBase gene identifier.

 * List client:
 ~~~~~~~~~~~~~~
   The ListClient demonstrates the use of the InterMine List Web Service. 

   This example fetches all public lists containing the FBgn0000606 gene.

 * GenesFinder client
 ~~~~~~~~~~~~~~~~~~~~
   The GenesFinder demonstrates the use of command line arguments,
   with a query client fetching Genes or
   SequenceFeatures located at specific positions with some tolerance. You can use
   the included sample-input.txt to try this client:

   ./compile-run.sh GenesFinder --file absolute_path_to_GenesFinder_directory/sample-input.txt --tolerance tolerance_value --type Gene

