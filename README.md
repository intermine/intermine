InterMine
============

Master: [![Build Status: master][travis-badge-master]][ci]
Dev: [![Build Status: dev][travis-badge-dev]][ci]
[![Version](http://img.shields.io/badge/version-1.6.6-blue.svg?style=flat)](https://github.com/intermine/intermine/releases/tag/intermine-1.6.6)
[![License](http://img.shields.io/badge/license-LGPL_2.1-blue.svg?style=flat)](https://github.com/intermine/intermine/blob/master/LICENSE)
[![Research software impact](http://depsy.org/api/package/pypi/intermine/badge.svg)](http://depsy.org/package/python/intermine)

A powerful open source data warehouse system. InterMine allows users
to integrate diverse data sources with a minimum of effort, providing
powerful web-services and an elegant web-application with minimal
configuration. InterMine powers some of the largest data-warehouses in
the life sciences, including:
  * [FlyMine](http://www.flymine.org)
  * [MouseMine](http://www.mousemine.org)
  * [YeastMine](http://yeastmine.yeastgenome.org)
  * [ZebrafishMine](http://zebrafishmine.org)
  * [RatMine](http://ratmine.mcw.edu/ratmine/begin.do)
  * [TargetMine](http://targetmine.nibio.go.jp/)
  * [ThaleMine](https://apps.araport.org/thalemine)
  * [PhytoMine](http://phytozome.jgi.doe.gov/phytomine)

For details, please visit: [InterMine Documentation][readthedocs]

If you run an InterMine, or use one in your research, in order to improve the chance of continued funding for the
InterMine project it would be appreciated if groups that use InterMine or parts of InterMine would let us know (email
[info[at]flymine.org](mailto:info intermine.org)).

Getting Started With InterMine
-------------------------------

For a guide on getting started with InterMine, please visit:
[quick start tutorial][tutorial]

3min bootstrap
--------------------------------------

As long as you have the prerequisites installed ([Java][java],
[PostgreSQL][psql]), you can get a working 
data-warehouse and associated web-application by running an
automated bootstrap script:

```bash
  # Set up tomcat
./config/download_and_configure_tomcat.sh
  # For a genomic application, with test data from Malaria
./biotestmine/setup.sh
  # For the testmodel
./testmodel/setup.sh
```

This requires that you have all the software dependencies
installed and running with the appropriate user permissions
(postgres, Tomcat, Java SDK). You will need to have set up usernames
and passwords for Tomcat and postgres first, and these can be
provided to the setup scripts as `PSQL_USER`, `PSQL_PWD`,
`TOMCAT_USER`, and `TOMCAT_PWD`.

Copyright and Licence
------------------------

Copyright (C) 2002-2016 FlyMine

See [LICENSE](LICENSE) file for licensing information.

This product includes software developed by the
[Apache Software Foundation][apache]

Please cite
------------------------

**InterMine: a flexible data warehouse system for the integration and analysis of heterogeneous biological data.**<br/>
*Smith RN, Aleksic J, Butano D, Carr A, Contrino S, Hu F, Lyne M, Lyne R, Kalderimis A, Rutherford K, Stepan R, Sullivan J, Wakeling M, Watkins X, Micklem G.* <br/>
[Bioinformatics (2012) 28 (23): 3163-3165.](http://bioinformatics.oxfordjournals.org/content/28/23/3163.abstract) <br/>
[![doi](http://img.shields.io/badge/doi-10.1093%2Fbioinformatics%2Fbts577-blue.svg?style=flat)](http://bioinformatics.oxfordjournals.org/content/28/23/3163.abstract) [![pubmed](http://img.shields.io/badge/pubmed-    23023984-blue.svg?style=flat)](http://www.ncbi.nlm.nih.gov/pubmed/23023984)


[travis-badge-master]: https://travis-ci.org/intermine/intermine.svg?branch=master
[travis-badge-dev]: https://travis-ci.org/intermine/intermine.svg?branch=dev
[ci]: https://travis-ci.org/intermine/intermine
[readthedocs]: http://intermine.readthedocs.org/en/latest
[tutorial]: http://intermine.readthedocs.org/en/latest/get-started/tutorial
[psql]: http://www.postgresql.org
[java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[apache]: http://www.apache.org
[tomcat]: http://tomcat.apache.org/download-70.cgi
