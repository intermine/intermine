from intermine import query
from interminebio.iterators import *
try:
    import simplejson as json
except ImportError:
    try:
       import json
    except ImportError:
        raise "No JSON module found - please install simplejson"

class SequenceDataQuery(object):
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


class RegionQuery(SequenceDataQuery):
    """
    Class for querying InterMine Webservices for Features in Genomic Intervals
    ==========================================================================

    This module allows you to construct queries that retrieve data about sequences and 
    sequence features in biologically relevant formats, where those features are located
    overlapping genomic intervals.

    The currently supported formats are UCSC-BED, GFF3, and FASTA.

    These queries may also be used to construct lists with.

    """


    LIST_PATH = "/regions/list"
    BED_PATH = "/regions/bed"
    FASTA_PATH = "/regions/fasta"
    GFF3_PATH = "/regions/gff3"

    def __init__(self, service, organism, feature_types, regions, extension=0, is_interbase=False):
        """
        Constructor
        ===========
         
          >>> s = Service("www.flymine.org/query", "API-KEY")
          >>> org = "D. melanogaster"
          >>> regions = ["2L:14614843..14619614"]
          >>> feature_types = ["Exon", "Intron"]
          >>> q = RegionQuery(s, org, feature_types, regions)
          <interminebio.RegionQuery @xxx>

        @param service: The service to connect to.
        @type service: intermine.webservice.Service

        @param organism: The short name of the organism to look within (eg: D. melanogaster)
        @type organism: str

        @param feature_types: The types of features to look for
        @type feature_types: list[str]

        @param regions: The regions to search within, in chrX:start..end or chrX\tstart\tend format
        @type regions: list(str)

        @param extension: A number of base-pairs to extend each region on either side (default: 0)
        @type extension: int

        @param is_interbase: Whether to interpret the co-ordinates as interbase co-ordinates
        @type is_interbase: boolean

        """
        self.service = service
        self.organism = organism
        self.feature_types = set(feature_types)
        self.regions = set(regions)
        self.extension = extension
        self.is_interbase = is_interbase
        self.bed_path = RegionQuery.BED_PATH
        self.fasta_path = RegionQuery.FASTA_PATH
        self.gff3_path = RegionQuery.GFF3_PATH
        self.views = []

    def _get_region_query(self):
        return {
            "organism": self.organism, 
            "featureTypes": list(self.feature_types), 
            "regions": list(self.regions),
            "extension": self.extension,
            "isInterbase": self.is_interbase
            }

    def to_query_params(self):
        """
        Returns the query parameters for this request.
        ==============================================

        This method is a required part of the interface for creating lists.

        @rtype: dict
        """
        return {"query": json.dumps(self._get_region_query())}

    def get_list_upload_uri(self):
        """
        Returns the full url for the list upload service
        ================================================

        This method is a required part of the interface for creating lists.

        @rtype: str
        """
        return self.service.root + RegionQuery.LIST_PATH

    @property
    def query(self):
        return self
        

class SequenceQuery(SequenceDataQuery):
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
            self.service = service_or_query
            self.query = query.Query(self.service.model, self.service, root=root)

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
