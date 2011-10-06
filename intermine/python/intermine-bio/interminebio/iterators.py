class BedIterator(object):

    BED_PATHS = {}

    def __init__(self, service, query, ucsc_compatible=True):
        if service.root not in BedIterator.BED_PATHS:
            BedIterator.BED_PATHS[service.root] = service.resolve_service_path("query.bed")
        self.path = BedIterator.BED_PATHS[service.root] 
        self.service = service
        self.query = query
        self.ucsc_compatible = ucsc_compatible
        self._header = []
        self.it = self._get_iter()

    def header(self):
        return "\n".join(self._header)

    def _get_iter(self):
        params = self.query.to_query_params()
        if not self.ucsc_compatible:
            params["ucscCompatible"] = "no"
        try:
            path = self.query.bed_path
        except: 
            path = self.path
        i = self.service.get_results(path, params, "tsv", self.query.views)
        return i

    def __str__(self):
        lines = [line for line in self]
        return "\n".join(self._header + lines)

    def __iter__(self):
        return self

    def next(self):
        line = self.it.next()
        while line and line.startswith("#") or line.startswith("track"):
            self._header.append(line)
            line = self.it.next()
        if line:
            return line
        raise StopIteration

class GFF3Iterator(object):

    GFF3_PATHS = {}

    def __init__(self, service, query):
        if service.root not in GFF3Iterator.GFF3_PATHS:
            GFF3Iterator.GFF3_PATHS[service.root] = service.resolve_service_path("query.gff3")
        self.path = GFF3Iterator.GFF3_PATHS[service.root] 
        self.service = service
        self.query = query
        self._header = []
        self.it = self._get_iter()

    def header(self):
        return "\n".join(self._header)

    def _get_iter(self):
        params = self.query.to_query_params()
        try:
            path = self.query.gff3_path
        except: 
            path = self.path
        i = self.service.get_results(path, params, "tsv", self.query.views)
        return i

    def __str__(self):
        lines = [line for line in self]
        return "\n".join(self._header + lines)

    def __iter__(self):
        return self

    def next(self):
        line = self.it.next()
        while line and line.startswith("#"):
            self._header.append(line)
            line = self.it.next()
        if line:
            return line
        raise StopIteration

class FastaIterator(object):

    FASTA_PATHS = {}

    def __init__(self, service, query):
        if service.root not in FastaIterator.FASTA_PATHS:
            FastaIterator.FASTA_PATHS[service.root] = service.resolve_service_path("query.fasta")
        self.path = FastaIterator.FASTA_PATHS[service.root] 
        self.service = service
        self.query = query
        self.it = self._get_iter()
        self._holdover = None

    def _get_iter(self):
        params = self.query.to_query_params()
        try:
            path = self.query.fasta_path
        except: 
            path = self.path
        i = self.service.get_results(path, params, "tsv", self.query.views)
        return i

    def __str__(self):
        records = [rec for rec in self]
        return "\n".join(records)

    def __iter__(self):
        return self

    def next(self):
        lines = []
        if self._holdover is not None:
            lines.append(self._holdover)
            self._holdover = None
        else:
            try:
                lines.append(self.it.next())
            except StopIteration:
                pass

        try: 
            while True:
                line = self.it.next()
                if line.startswith(">"):
                    self._holdover = line
                    break
                lines.append(line)
        except StopIteration:
            pass

        if len(lines):
            return "\n".join(lines)

        if self._holdover:
            ret = self._holdover
            self._holdover = None
            return self._holdover

        raise StopIteration


