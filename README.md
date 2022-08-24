InterMine
============

Master: [![InterMine CI](https://github.com/intermine/intermine/workflows/InterMine%20CI/badge.svg?branch=master)](https://github.com/intermine/intermine/actions?query=workflow%3A%22InterMine+CI%22)
Dev: [![InterMine CI](https://github.com/intermine/intermine/workflows/InterMine%20CI/badge.svg?branch=dev)](https://github.com/intermine/intermine/actions?query=workflow%3A%22InterMine+CI%22)
[![Version](http://img.shields.io/badge/version-5.1.0-blue.svg?style=flat)](https://github.com/intermine/intermine/releases)
[![License](http://img.shields.io/badge/license-LGPL_2.1-blue.svg?style=flat)](https://github.com/intermine/intermine/blob/master/LICENSE)
[![Research software impact](http://depsy.org/api/package/pypi/intermine/badge.svg)](http://depsy.org/package/python/intermine)
[![Conda](https://anaconda.org/anaconda/conda-build/badges/installer/conda.svg)](https://anaconda.org/bioconda/intermine)
[![Documentation Status](https://readthedocs.org/projects/intermine/badge/?version=latest)](https://intermine.readthedocs.io/en/latest/?badge=latest)
f
A powerful open source data warehouse system. InterMine allows users
to integrate diverse data sources with a minimum of effort, providing
powerful web-services and an elegant web-application with minimal
configuration. InterMine powers some of the largest data-warehouses in
the life sciences, including:
  * [FlyMine](https://www.flymine.org)
  * [HumanMine](https://www.humanmine.org)
  * [MouseMine](http://www.mousemine.org)
  * [YeastMine](https://yeastmine.yeastgenome.org)
  * [ZebrafishMine](http://zebrafishmine.org)
  * [RatMine](http://ratmine.mcw.edu/ratmine/begin.do)
  * [TargetMine](http://targetmine.mizuguchilab.org/)
  * [ThaleMine](https://apps.araport.org/thalemine)
  * [PhytoMine](https://phytozome.jgi.doe.gov/phytomine)

For the full list of InterMines, please see the [registry](http://registry.intermine.org)

For details, please visit: [InterMine Documentation][readthedocs]

If you run an InterMine, or use one in your research, in order to improve the chance of continued funding for the
project it would be appreciated if groups that use InterMine or parts of InterMine would let us know.

Getting Started With InterMine
-------------------------------

For a guide on getting started with InterMine, please visit:
[tutorial][tutorial]

3min bootstrap
--------------------------------------

As long as you have the prerequisites installed ([Java][java], [PostgreSQL][psql]), you can get a working data-warehouse and associated web-application by running an automated bootstrap script:

```bash
  # For the testmodel
./testmine/setup.sh
```
For a genomic application, with test data from Malaria, see [BioTestMine](https://github.com/intermine/biotestmine)


Docker
--------------------------------------
You can build InterMine using Docker. See https://github.com/intermine/docker-intermine-gradle

Copyright and Licence
------------------------

Copyright (C) 2002-2022 FlyMine

See [LICENSE](LICENSE) file for licensing information.

This product includes software developed by the
[Apache Software Foundation][apache]

InterMine Development Roadmap
-----------------------------

For more information about the upcoming releases, please visit the [InterMine Development Roadmap](http://intermine.org/im-docs/docs/intermine/roadmap/). For the roadmap, please see [here](https://github.com/intermine/intermine/projects/7).

Please cite
------------------------

**InterMine: extensive web services for modern biology.**<br/>
*Kalderimis A, Lyne R, Butano D, Contrino S, Lyne M, Heimbach J, Hu F, Smith R,
Stěpán R, Sullivan J, Micklem G.* <br/>
[Nucleic Acids Res. 2014 Jul; 42 (Web Server issue): W468-72](https://academic.oup.com/nar/article/42/W1/W468/2435235) <br/>
[![doi](http://img.shields.io/badge/doi-10.1093%2Fnar%2Fgku301-blue.svg?style=flat)](https://academic.oup.com/nar/article/42/W1/W468/2435235) 
[![pubmed](http://img.shields.io/badge/pubmed-24753429-blue.svg?style=flat)](http://www.ncbi.nlm.nih.gov/pubmed/24753429)

**InterMine: a flexible data warehouse system for the integration and analysis of heterogeneous biological data.**<br/>
*Smith RN, Aleksic J, Butano D, Carr A, Contrino S, Hu F, Lyne M, Lyne R, Kalderimis A, Rutherford K, Stepan R, Sullivan J, Wakeling M, Watkins X, Micklem G.* <br/>
[Bioinformatics (2012) 28 (23): 3163-3165.](http://bioinformatics.oxfordjournals.org/content/28/23/3163.abstract) <br/>
[![doi](http://img.shields.io/badge/doi-10.1093%2Fbioinformatics%2Fbts577-blue.svg?style=flat)](http://bioinformatics.oxfordjournals.org/content/28/23/3163.abstract) 
[![pubmed](http://img.shields.io/badge/pubmed-23023984-blue.svg?style=flat)](http://www.ncbi.nlm.nih.gov/pubmed/23023984)

See [zotero](https://www.zotero.org/groups/2117194/intermine/) for the full list of InterMine publications.

[travis-badge-master]: https://travis-ci.org/intermine/intermine.svg?branch=master
[ci]: https://travis-ci.org/intermine/intermine
[readthedocs]: http://intermine.readthedocs.org/en/latest
[tutorial]: http://intermine.readthedocs.org/en/latest/get-started/tutorial
[psql]: http://www.postgresql.org
[java]: http://openjdk.java.net
[apache]: http://www.apache.org
[tomcat]: http://tomcat.apache.org/download-70.cgi
