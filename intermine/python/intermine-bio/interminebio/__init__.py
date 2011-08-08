from intermine import query
from interminebio.iterators import *

class SequenceQuery(object):
    """
    Class for querying InterMine Webservices for Sequence based data
    ================================================================

    This module allows you to construct queries that retrieve data about sequences and 
    sequence features in biologically relevant formats.

    The currently supported formats are UCSC-BED, GFF3, and FASTA.

    """

    def __init__(self, service_or_query, root=None):
        """
        Constructor
        ===========
         
          >>> s = Service("www.flymine.org/query")
          >>> bio_query = SequenceQuery(s, "Gene")
          <interminebio.SequenceQuery xxx>
          >>> q = s.new_query("Gene").where(s.model.Gene.symbol == ["h", "r", "eve", "zen"])
          >>> bio_query = SequenceQuery(q)
          <interminebio.SequenceQuery yyy>

        @param service_or_query: The service to connect to, or a query to wrap.
        @type service_or_query: intermine.webservice.Service or intermine.query.Query

        @param root: The root class of the query
        @type root: str

        """
        if isinstance(service_or_query, query.Query):
            self.service = service_or_query.service
            self.query = service_or_query
        else:
            self.service = service
            self.query = query.Query(service.model, service, root=root)

        # Set up delegations
        self.add_constraint = self.query.add_constraint
        self.filter = self.where

        self.to_xml = self.query.to_xml

        self.get_logic = self.query.get_logic
        self.set_logic = self.query.set_logic

        self.select_sequence = self.set_sequence
        self.select_sequences = self.add_sequence_feature
        self.add_sequence_features = self.add_sequence_feature

    def add_sequence_feature(self, *features):
        """
        Add an arbitrarily long list of sequence features to the query.
        ===============================================================

        Fasta, GFF3 and BED queries all can read information from SequenceFeatures. 
        For Fasta you are advised to use the set_sequence method instead, 
        as unlike the GFF3 and BED services, the Fasta service can only handle 
        queries with one output column.
        """
        for f in features:
            p = self.query.column(f)._path
            if p.is_attribute() or not p.get_class().isa("SequenceFeature"):
                raise ValueError("%s is not a Sequence Feature" % (f))
            self.query.add_view(str(p) + ".id")

        return self

    def where(self, *args, **kwargs):
        """
        Add a constraint to the query, and return self for chaining.
        """
        self.query.where(*args, **kwargs)
        return self

    def set_sequence(self, f):
        """
        Set the sequence column to retrieve.
        ====================================

        Add a sequence holding object to the query. It can be a SequenceFeature, Protein
        or Sequence object.

        Fasta queries, which read sequences rather than sequence features, 
        currently only permit one output column.
        """
        self.query.views = []
        p = self.query.column(f)._path
        if p.is_attribute() or not (p.get_class().isa("SequenceFeature") or
                p.get_class().isa("Protein") or
                p.get_class().isa("Sequence")):
            raise ValueError("%s has no sequence information" % (f))
        self.query.add_view(str(p) + ".id")

        return self

    def bed(self, ucsc_compatible=True):
        """
        Get results as BED
        ==================

        Return a BedIterator object, which stringifies to the BED results, 
        and works as an iterator over the lines. After iteration the header
        information is accessible with the iter.header() method
        """
        return BedIterator(self.service, self.query, ucsc_compatible)

    def fasta(self):
        """
        Get results as FASTA
        ====================

        Return a FastaIterator object, which stringifies to the Fasta results, 
        and works as an iterator over the records (not the lines).

        When attempting to get results as FASTA the query may only have a single
        output column. Errors will be raised otherwise.
        """
        return FastaIterator(self.service, self.query)

    def gff3(self):
        """
        Get results as GFF3
        ===================

        Return a GFF3Iterator object, which stringifies to the GFF3 results, 
        and works as an iterator over the lines. After iteration the header
        information is accessible with the iter.header() method
        """
        return GFF3Iterator(self.service, self.query)

