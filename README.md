InterMine
============

A powerful open source data warehouse system.

[InterMine Documentation](http://intermine.readthedocs.org/en/latest/)

In order to improve the chance of continued funding for the
InterMine project it would be appreciated if groups that use
InterMine or parts of InterMine would let us know (email
[info[at]flymine.org](mailto:info flymine.org)).

Getting Started With InterMine
-------------------------------

For a guide on getting started with InterMine, please visit:
[quick start
tutorial](http://intermine.readthedocs.org/en/latest/get-started/tutorial/)

Deploying a Demonstration Application
--------------------------------------

To deploy one of the demonstration applications without going
through a tutorial, please enter either the `testmodel` or
`biotestmine` directory and run the set-up script:

  sh setup.sh

This requires that you have all the software dependencies
installed and running with the appropriate user permissions
(postgres, Tomcat, Java SDK). You will need to have set up usernames
and passwords for Tomcat and postgres first, and these can be
provided to the setup scripts as `PSQL_USER`, `PSQL_PWD`,
`TOMCAT_USER`, and `TOMCAT_PWD`.

Copyright and Licence
------------------------

Copyright (C) 2002-2014 FlyMine

See [LICENSE](LICENSE) file for licensing information.

This product includes software developed by the [Apache Software Foundation](http://www.apache.org/).
