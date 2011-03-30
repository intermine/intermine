from distutils.core import setup
setup(
        name = "intermine",
        packages = ["intermine"],
        version = "0.96.00",
        description = "InterMine WebService client",
        author = "Alex Kalderimis",
        author_email = "dev@intermine.org",
        url = "http://www.intermine.org/",
        download_url = "http://www.intermine.org/lib/python-webservice-client-0.96.00.tar.gz",
        keywords = ["webservice", "genomic", "bioinformatics"],
        classifiers = [
            "Programming Language :: Python",
            "Development Status :: 3 - Alpha",
            "Intended Audience :: Science/Research",
            "Intended Audience :: Developers",
            "License :: OSI Approved :: GNU Library or Lesser General Public License (LGPL)",
            "Topic :: Software Development :: Libraries :: Python Modules",
            "Topic :: Internet :: WWW/HTTP",
            "Topic :: Scientific/Engineering :: Bio-Informatics",
            "Topic :: Scientific/Engineering :: Information Analysis",
            "Operating System :: OS Independent",
            ],
        license = "LGPL",
        long_description = """\
InterMine Webservice Client
----------------------------

Provides access routines to datawarehouses powered 
by InterMine technology.

""",
)
