InterMine
============

Master: [![Build Status: master][travis-badge-master]][ci]
Beta: [![Build Status: beta][travis-badge-beta]][ci]

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

InterMine is free, open-source software.

For extensive documentation please visit: [InterMine Documentation][readthedocs]

If you run an InterMine, or use one in your research,
in order to improve the chance of continued funding for the
InterMine project it would be appreciated if groups that use
InterMine or parts of InterMine would let us know (email
[info[at]flymine.org](mailto:info flymine.org)).

Getting Started With InterMine
-------------------------------

For a guide on getting started with InterMine, please visit:
[quick start tutorial][tutorial]

3min bootstrap
--------------------------------------

As long as you have the prerequisites installed ([Java][java],
[PostgreSQL][psql]), you can get a working 
data-warehouse and associated web-application by running an
automated bootstap script:

```bash
  # Set up tomcat
  sh config/download_and_configure_tomcat.sh
  # For a genomic application, with test data from Malaria
  sh bio/setup.sh
  # For the testmodel
  sh testmodel/setup.sh
```

This requires that you have all the software dependencies
installed and running with the appropriate user permissions
(postgres, Tomcat, Java SDK). You will need to have set up usernames
and passwords for Tomcat and postgres first, and these can be
provided to the setup scripts as `PSQL_USER`, `PSQL_PWD`,
`TOMCAT_USER`, and `TOMCAT_PWD`.

Copyright and Licence
------------------------

Copyright (C) 2002-2015 FlyMine

See [LICENSE](LICENSE) file for licensing information.

This product includes software developed by the
[Apache Software Foundation][apache]

[travis-badge-master]: https://travis-ci.org/intermine/intermine.svg?branch=master
[travis-badge-beta]: https://travis-ci.org/intermine/intermine.svg?branch=beta
[ci]: https://travis-ci.org/intermine/intermine
[readthedocs]: http://intermine.readthedocs.org/en/latest
[tutorial]: http://intermine.readthedocs.org/en/latest/get-started/tutorial
[psql]: http://www.postgresql.org
[java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[apache]: http://www.apache.org
[tomcat]: http://tomcat.apache.org/download-70.cgi
