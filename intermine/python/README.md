The InterMine Python Webservice Client
=====================================

> An implementation of a webservice client 
> for InterMine webservices, written in Python

Who should use this software?
-----------------------------

This software is intended for people who make 
use of InterMine datawarehouses (ie. Biologists)
and who want a quicker, more automated way 
to perform queries. Some examples of sites that
are powered by InterMine software, and thus offer
a compatible webservice API are:

* FlyMine
* MetabolicMine
* modMine
* RatMine
* YeastMine

Queries here refer to database queries over the 
integrated datawarehouse. Instead of using 
SQL, InterMine services use a flexible and 
powerful sub-set of database query language
to enable wide-ranging and arbitrary queries.

Downloading:
------------

The easiest way to install is to use easy_install:

  sudo easy_install intermine

The source code can be downloaded from a variety of places:

* From InterMine

  wget http://www.intermine.org/lib/python-webservice-client-0.96.00.tar.gz

* From PyPi

  wget http://pypi.python.org/packages/source/i/intermine/intermine-0.96.00.tar.gz

* From Github

  git clone git://github.com/alexkalderimis/intermine-ws-python.git


Running the Tests:
------------------

If you would like to run the test suite, you can do so by executing
the following command: (from the source directory)

  python setup.py test

Installation:
-------------

Once downloaded, you can install the module with the command (from the source directory):

  python setup.py install

Further documentation:
----------------------

Extensive documentation is available by using the "pydoc" command, eg:

  pydoc intermine.query.Query

Also see:

* Documentation on PyPi: http://packages.python.org/intermine/
* Documentation at InterMine: http://www.flymine.org/download/python-docs http://www.intermine.org/wiki/PythonClient

License:
--------

All InterMine code is freely available under the LGPL license: http://www.gnu.org/licenses/lgpl.html 


