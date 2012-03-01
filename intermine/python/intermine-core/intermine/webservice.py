from urlparse import urlunsplit, urljoin
from xml.dom import minidom
import urllib
from urlparse import urlparse
import csv
import base64
import httplib
import re
import copy
import UserDict
from itertools import groupby

#class UJsonLibDecoder(object): # pragma: no cover
#    def __init__(self):
#        self.loads = ujson.decode
#
# Use core json for 2.6+, simplejson for <=2.5
#try:
#    import ujson
#    json = UJsonLibDecoder()
#except ImportError: # pragma: no cover
try:
    import simplejson as json # Prefer this as it is faster
except ImportError: # pragma: no cover
    try:
        import json
    except ImportError:
        raise ImportError("Could not find any JSON module to import - "
            + "please install simplejson or jsonlib to continue")

# Local intermine imports
from intermine.query import Query, Template
from intermine.model import Model, Attribute, Reference, Collection, Column
from intermine.util import ReadableException
from intermine.lists.listmanager import ListManager

"""
Webservice Interaction Routines for InterMine Webservices
=========================================================

Classes for dealing with communication with an InterMine
RESTful webservice.

"""

__author__ = "Alex Kalderimis"
__organization__ = "InterMine"
__license__ = "LGPL"
__contact__ = "dev@intermine.org"

class Registry(object, UserDict.DictMixin):
    """
    A Class representing an InterMine registry.
    ===========================================

    Registries are web-services that mines can automatically register themselves
    with, and thus enable service discovery by clients.

    SYNOPSIS
    --------

    example::

        from intermine.webservice import Registry

        # Connect to the default registry service
        # at www.intermine.org/registry
        registry = Registry()

        # Find all the available mines:
        for name, mine in registry.items():
            print name, mine.version

        # Dict-like interface for accessing mines.
        flymine = registry["flymine"]

        # The mine object is a Service
        for gene in flymine.select("Gene.*").results():
            process(gene)

    This class is meant to aid with interoperation between
    mines by allowing them to discover one-another, and
    allow users to always have correct connection information.
    """

    MINES_PATH = "/mines.json"

    def __init__(self, registry_url="http://www.intermine.org/registry"):
        self.registry_url = registry_url
        opener = InterMineURLOpener()
        data = opener.open(registry_url + Registry.MINES_PATH).read()
        mine_data = json.loads(data)
        self.__mine_dict = dict(( (mine["name"], mine) for mine in mine_data["mines"]))
        self.__synonyms = dict(( (name.lower(), name) for name in self.__mine_dict.keys() ))
        self.__mine_cache = {}

    def __contains__(self, name):
        return name.lower() in self.__synonyms

    def __getitem__(self, name):
        lc = name.lower()
        if lc in self.__synonyms:
            if lc not in self.__mine_cache:
                self.__mine_cache[lc] = Service(self.__mine_dict[self.__synonyms[lc]]["webServiceRoot"])
            return self.__mine_cache[lc]
        else:
            raise KeyError("Unknown mine: " + name)

    def __setitem__(self, name, item):
        raise NotImplementedError("You cannot add items to a registry")

    def __delitem__(self, name):
        raise NotImplementedError("You cannot remove items from a registry")

    def keys(self):
        return self.__mine_dict.keys()

class Service(object):
    """
    A class representing connections to different InterMine WebServices
    ===================================================================

    The intermine.webservice.Service class is the main interface for the user.
    It will provide access to queries and templates, as well as doing the
    background task of fetching the data model, and actually requesting
    the query results.

    SYNOPSIS
    --------

    example::

      from intermine.webservice import Service
      service = Service("http://www.flymine.org/query/service")

      template = service.get_template("Gene_Pathways")
      for row in template.results(A={"value":"zen"}):
        do_something_with(row)
        ...

      query = service.new_query()
      query.add_view("Gene.symbol", "Gene.pathway.name")
      query.add_constraint("Gene", "LOOKUP", "zen")
      for row in query.results():
        do_something_with(row)
        ...

      new_list = service.create_list("some/file/with.ids", "Gene")
      list_on_server = service.get_list("On server")
      in_both = new_list & list_on_server
      in_both.name = "Intersection of these lists"
      for row in in_both:
        do_something_with(row)
        ...

    OVERVIEW
    --------
    The two methods the user will be most concerned with are:
      - L{Service.new_query}: constructs a new query to query a service with
      - L{Service.get_template}: gets a template from the service
      - L{ListManager.create_list}: creates a new list on the service

    For list management information, see L{ListManager}.

    TERMINOLOGY
    -----------
    X{Query} is the term for an arbitrarily complex structured request for
    data from the webservice. The user is responsible for specifying the
    structure that determines what records are returned, and what information
    about each record is provided.

    X{Template} is the term for a predefined "Query", ie: one that has been
    written and saved on the webservice you will access. The definition
    of the query is already done, but the user may want to specify the
    values of the constraints that exist on the template. Templates are accessed
    by name, and while you can easily introspect templates, it is assumed
    you know what they do when you use them

    X{List} is a saved result set containing a set of objects previously identified
    in the database. Lists can be created and managed using this client library.

    @see: L{intermine.query}
    """
    USER_AGENT             = 'WebserviceInterMinePerlAPIClient'
    QUERY_PATH             = '/query/results'
    QUERY_LIST_UPLOAD_PATH = '/query/tolist/json'
    QUERY_LIST_APPEND_PATH = '/query/append/tolist/json'
    MODEL_PATH             = '/model'
    TEMPLATES_PATH         = '/templates/xml'
    TEMPLATEQUERY_PATH     = '/template/results'
    LIST_PATH              = '/lists/json'
    LIST_CREATION_PATH     = '/lists/json'
    LIST_RENAME_PATH       = '/lists/rename/json'
    LIST_APPENDING_PATH    = '/lists/append/json'
    LIST_TAG_PATH          = '/list/tags/json'
    SAVEDQUERY_PATH        = '/savedqueries/xml'
    VERSION_PATH           = '/version/ws'
    RELEASE_PATH           = '/version/release'
    SCHEME                 = 'http://'
    SERVICE_RESOLUTION_PATH = "/check/"

    def __init__(self, root,
            username=None, password=None, token=None,
            prefetch_depth=1, prefetch_id_only=False):
        """
        Constructor
        ===========

        Construct a connection to a webservice::

            url = "http://www.flymine.org/query/service"

            # An unauthenticated connection - access to all public data
            service = Service(url)

            # An authenticated connection - access to private and public data
            service = Service(url, token="ABC123456")


        @param root: the root url of the webservice (required)
        @param username: your login name (optional)
        @param password: your password (required if a username is given)
        @param token: your API access token(optional - used in preference to username and password)

        @raise ServiceError: if the version cannot be fetched and parsed
        @raise ValueError:   if a username is supplied, but no password

        There are two alternative authentication systems supported by InterMine
        webservices. The first is username and password authentication, which
        is supported by all webservices. Newer webservices (version 6+)
        also support API access token authentication, which is the recommended
        system to use. Token access is more secure as you will never have
        to transmit your username or password, and the token can be easily changed
        or disabled without changing your webapp login details.

        """
        o = urlparse(root)
        if not o.scheme: root = "http://" + root
        if not root.endswith("/service"): root = root + "/service"

        self.root = root
        self.prefetch_depth = prefetch_depth
        self.prefetch_id_only = prefetch_id_only
        self._templates = None
        self._model = None
        self._version = None
        self._release = None
        self._list_manager = ListManager(self)
        self.__missing_method_name = None
        if token:
            self.opener = InterMineURLOpener(token=token)
        elif username:
            if token:
                raise ValueError("Both username and token credentials supplied")

            if not password:
                raise ValueError("Username given, but no password supplied")

            self.opener = InterMineURLOpener((username, password))
        else:
            self.opener = InterMineURLOpener()

        try:
            self.version
        except WebserviceError, e:
            raise ServiceError("Could not validate service - is the root url (%s) correct? %s" % (root, e))

        if token and self.version < 6:
            raise ServiceError("This service does not support API access token authentication")

        # Set up sugary aliases
        self.query = self.new_query


    # Delegated list methods

    LIST_MANAGER_METHODS = frozenset(["get_list", "get_all_lists",
        "get_all_list_names",
        "create_list", "get_list_count", "delete_lists", "l"])

    def __getattribute__(self, name):
        return object.__getattribute__(self, name)

    def __getattr__(self, name):
        if name in self.LIST_MANAGER_METHODS:
            method = getattr(self._list_manager, name)
            return method
        raise AttributeError("Could not find " + name)

    def __del__(self):
        try:
            self._list_manager.delete_temporary_lists()
        except ReferenceError:
            pass

    @property
    def version(self):
        """
        Returns the webservice version
        ==============================

        The version specifies what capabilities a
        specific webservice provides. The most current
        version is 3

        may raise ServiceError: if the version cannot be fetched

        @rtype: int
        """
        if self._version is None:
            try:
                url = self.root + self.VERSION_PATH
                self._version = int(self.opener.open(url).read())
            except ValueError, e:
                raise ServiceError("Could not parse a valid webservice version: " + str(e))
        return self._version

    def resolve_service_path(self, variant):
        """Resolve the path to optional services"""
        url = self.root + self.SERVICE_RESOLUTION_PATH + variant
        return self.opener.open(url).read()

    @property
    def release(self):
        """
        Returns the datawarehouse release
        =================================

        Service.release S{->} string

        The release is an arbitrary string used to distinguish
        releases of the datawarehouse. This usually coincides
        with updates to the data contained within. While a string,
        releases usually sort in ascending order of recentness
        (eg: "release-26", "release-27", "release-28"). They can also
        have less machine readable meanings (eg: "beta")

        @rtype: string
        """
        if self._release is None:
            self._release = urllib.urlopen(self.root + self.RELEASE_PATH).read()
        return self._release

    def load_query(self, xml, root=None):
        """
        Construct a new Query object for the given webservice
        =====================================================

        This is the standard method for instantiating new Query
        objects. Queries require access to the data model, as well
        as the service itself, so it is easiest to access them through
        this factory method.

        @return: L{intermine.query.Query}
        """
        return Query.from_xml(xml, self.model, root=root)

    def select(self, *columns, **kwargs):
        """
        Construct a new Query object with the given columns selected.
        =============================================================

        As new_query, except that instead of a root class, a list of
        output column expressions are passed instead.
        """
        if "xml" in kwargs:
            return self.load_query(kwargs["xml"])
        if len(columns) == 1:
            view = columns[0]
            if isinstance(view, Attribute):
                return Query(self.model, self).select("%s.%s" % (view.declared_in.name, view))
            if isinstance(view, Reference):
                return Query(self.model, self).select("%s.%s.*" % (view.declared_in.name, view))
            elif not isinstance(view, Column) and not str(view).endswith("*"):
                path = self.model.make_path(view)
                if not path.is_attribute():
                    return Query(self.model, self).select(str(view) + ".*")
        return Query(self.model, self).select(*columns)

    new_query = select

    def get_template(self, name):
        """
        Returns a template of the given name
        ====================================

        Tries to retrieve a template of the given name
        from the webservice. If you are trying to fetch
        a private template (ie. one you made yourself
        and is not available to others) then you may need to authenticate

        @see: L{intermine.webservice.Service.__init__}

        @param name: the template's name
        @type name: string

        @raise ServiceError: if the template does not exist
        @raise QueryParseError: if the template cannot be parsed

        @return: L{intermine.query.Template}
        """
        try:
            t = self.templates[name]
        except KeyError:
            raise ServiceError("There is no template called '"
                + name + "' at this service")
        if not isinstance(t, Template):
            t = Template.from_xml(t, self.model, self)
            self.templates[name] = t
        return t

    @property
    def templates(self):
        """
        The dictionary of templates from the webservice
        ===============================================

        Service.templates S{->} dict(intermine.query.Template|string)

        For efficiency's sake, Templates are not parsed until
        they are required, and until then they are stored as XML
        strings. It is recommended that in most cases you would want
        to use L{Service.get_template}.

        You can use this property however to test for template existence though::

         if name in service.templates:
            template = service.get_template(name)

        @rtype: dict

        """
        if self._templates is None:
            sock = self.opener.open(self.root + self.TEMPLATES_PATH)
            dom = minidom.parse(sock)
            sock.close()
            templates = {}
            for e in dom.getElementsByTagName('template'):
                name = e.getAttribute('name')
                if name in templates:
                    raise ServiceError("Two templates with same name: " + name)
                else:
                    templates[name] = e.toxml()
            self._templates = templates
        return self._templates

    @property
    def model(self):
        """
        The data model for the webservice you are querying
        ==================================================

        Service.model S{->} L{intermine.model.Model}

        This is used when constructing queries to provide them
        with information on the structure of the data model
        they are accessing. You are very unlikely to want to
        access this object directly.

        raises ModelParseError: if the model cannot be read

        @rtype: L{intermine.model.Model}

        """
        if self._model is None:
            model_url = self.root + self.MODEL_PATH
            self._model = Model(model_url, self)
        return self._model

    def get_results(self, path, params, rowformat, view, cld=None):
        """
        Return an Iterator over the rows of the results
        ===============================================

        This method is called internally by the query objects
        when they are called to get results. You will not
        normally need to call it directly

        @param path: The resource path (eg: "/query/results")
        @type path: string
        @param params: The query parameters for this request as a dictionary
        @type params: dict
        @param rowformat: One of "rr", "object", "count", "dict", "list", "tsv", "csv", "jsonrows", "jsonobjects"
        @type rowformat: string
        @param view: The output columns
        @type view: list

        @raise WebserviceError: for failed requests

        @return: L{intermine.webservice.ResultIterator}
        """
        return ResultIterator(self, path, params, rowformat, view, cld)

class ResultObject(object):
    """
    An object used to represent result records as returned in jsonobjects format
    ============================================================================

    These objects are backed by a row of data and the class descriptor that
    describes the object. They allow access in standard object style:

        >>> for gene in query.results():
        ...    print gene.symbol
        ...    print map(lambda x: x.name, gene.pathways)

    All objects will have "id" and "type" properties. The type refers to the
    actual type of this object: if it is a subclass of the one requested, the
    subclass name will be returned. The "id" refers to the internal database id
    of the object, and is a guarantor of object identity.

    """

    def __init__(self, data, cld, view=[]):
        stripped = [v[v.find(".") + 1:] for v in view]
        self.selected_attributes = [v for v in stripped if "." not in v]
        self.reference_paths = dict(((k, list(i)) for k, i in groupby(stripped, lambda x: x[:x.find(".") + 1])))
        self._data = data
        self._cld = cld if "class" not in data or cld.name == data["class"] else cld.model.get_class(data["class"])
        self._attr_cache = {}

    def __str__(self):
        dont_show = set(["objectId", "class"])
        return "%s(%s)" % (self._cld.name, ",  ".join("%s = %r" % (k, v) for k, v in self._data.items()
            if not isinstance(v, dict) and not isinstance(v, list) and k not in dont_show))

    def __repr__(self):
        dont_show = set(["objectId", "class"])
        return "%s(%s)" % (self._cld.name, ", ".join("%s = %r" % (k, getattr(self, k)) for k in self._data.keys()
            if k not in dont_show))

    def __getattr__(self, name):
        if name in self._attr_cache:
            return self._attr_cache[name]

        if name == "type":
            return self._data["class"]

        fld = self._cld.get_field(name)
        attr = None
        if isinstance(fld, Attribute):
            if name in self._data:
                attr = self._data[name]
            if attr is None:
                attr = self._fetch_attr(fld)
        elif isinstance(fld, Reference):
            ref_paths = self._get_ref_paths(fld)
            if name in self._data:
                data = self._data[name]
            else:
                data = self._fetch_reference(fld)
            if isinstance(fld, Collection):
                if data is None:
                    attr = []
                else:
                    attr = map(lambda x: ResultObject(x, fld.type_class, ref_paths), data)
            else:
                if data is None:
                    attr = None
                else:
                    attr = ResultObject(data, fld.type_class, ref_paths)
        else:
            raise WebserviceError("Inconsistent model - This should never happen")
        self._attr_cache[name] = attr
        return attr

    def _get_ref_paths(self, fld):
        if fld.name + "." in self.reference_paths:
            return self.reference_paths[fld.name + "."]
        else:
            return []

    @property
    def id(self):
        """Return the internal DB identifier of this object. Or None if this is not an InterMine object"""
        return self._data.get('objectId')

    def _fetch_attr(self, fld):
        if fld.name in self.selected_attributes:
            return None # Was originally selected - no point asking twice
        c = self._cld
        if "id" not in c:
            return None # Cannot reliably fetch anything without access to the objectId.
        q = c.model.service.query(c, fld).where(id = self.id)
        r = q.first()
        return r._data[fld.name] if fld.name in r._data else None

    def _fetch_reference(self, ref):
        if ref.name + "." in self.reference_paths:
            return None # Was originally selected - no point asking twice.
        c = self._cld
        if "id" not in c:
            return None # Cannot reliably fetch anything without access to the objectId.
        q = c.model.service.query(ref).outerjoin(ref).where(id = self.id)
        r = q.first()
        return r._data[ref.name] if ref.name in r._data else None

class ResultRow(object):
    """
    An object for representing a row of data received back from the server.
    =======================================================================

    ResultRows provide access to the fields of the row through index lookup. However,
    for convenience both list indexes and dictionary keys can be used. So the
    following all work:

        >>> # Assuming the view is "Gene.symbol", "Gene.organism.name":
        >>> row[0] == row["symbol"] == row["Gene.symbol"]
        ... True

    """

    def __init__(self, data, views):
        self.data = data
        self.views = views
        self.index_map = None

    def __len__(self):
        """Return the number of cells in this row"""
        return len(self.data)

    def __iter__(self):
        """Return the list view of the row, so each cell can be processed"""
        return iter(self.to_l())

    def _get_index_for(self, key):
        if self.index_map is None:
            self.index_map = {}
            for i in range(len(self.views)):
                view = self.views[i]
                headless_view = re.sub("^[^.]+.", "", view)
                self.index_map[view] = i
                self.index_map[headless_view] = i

        return self.index_map[key]

    def __str__(self):
        root = re.sub("\..*$", "", self.views[0])
        parts = [root + ":"]
        for view in self.views:
           short_form = re.sub("^[^.]+.", "", view)
           value = self[view]
           parts.append(short_form + "=" + repr(value))
        return " ".join(parts)

    def __getitem__(self, key):
        if isinstance(key, int):
            return self.data[key]
        elif isinstance(key, slice):
            return self.data[key]
        else:
            index = self._get_index_for(key)
            return self.data[index]

    def to_l(self):
        """Return a list view of this row"""
        return [x for x in self.data]


    def to_d(self):
        """Return a dictionary view of this row"""
        d = {}
        for view in self.views:
            d[view] = self[view]

        return d

    def items(self):
        return [(view, self[view]) for view in self.views]

    def iteritems(self):
        for view in self.views:
            yield (view, self[view])

    def keys(self):
        return copy.copy(self.views)

    def values(self):
        return self.to_l()

    def itervalues(self):
        return iter(self.to_l())

    def iterkeys(self):
        return iter(self.views)

    def has_key(self, key):
        try:
            self._get_index_for(key)
            return True
        except KeyError:
           return False

class TableResultRow(ResultRow):
    """
    A class for parsing results from the jsonrows data format.
    """

    def __getitem__(self, key):
        if isinstance(key, int):
            return self.data[key]["value"]
        elif isinstance(key, slice):
            vals = map(lambda x: x["value"], self.data[key])
            return vals
        else:
            index = self._get_index_for(key)
            return self.data[index]["value"]

    def to_l(self):
        """Return a list view of this row"""
        return map(lambda x: x["value"], self.data)

class ResultIterator(object):
    """
    A facade over the internal iterator object
    ==========================================

    These objects handle the iteration over results
    in the formats requested by the user. They are responsible
    for generating an appropriate parser,
    connecting the parser to the results, and delegating
    iteration appropriately.
    """

    PARSED_FORMATS = frozenset(["rr", "list", "dict"])
    STRING_FORMATS = frozenset(["tsv", "csv", "count"])
    JSON_FORMATS = frozenset(["jsonrows", "jsonobjects", "json"])
    ROW_FORMATS = PARSED_FORMATS | STRING_FORMATS | JSON_FORMATS

    def __init__(self, service, path, params, rowformat, view, cld=None):
        """
        Constructor
        ===========

        Services are responsible for getting result iterators. You will
        not need to create one manually.

        @param root: The root path (eg: "http://www.flymine.org/query/service")
        @type root: string
        @param path: The resource path (eg: "/query/results")
        @type path: string
        @param params: The query parameters for this request
        @type params: dict
        @param rowformat: One of "rr", "object", "count", "dict", "list", "tsv", "csv", "jsonrows", "jsonobjects", "json"
        @type rowformat: string
        @param view: The output columns
        @type view: list
        @param opener: A url opener (user-agent)
        @type opener: urllib.URLopener

        @raise ValueError: if the row format is incorrect
        @raise WebserviceError: if the request is unsuccessful
        """
        if rowformat.startswith("object"): # Accept "object", "objects", "objectformat", etc...
            rowformat = "jsonobjects" # these are synonymous
        if rowformat not in self.ROW_FORMATS:
            raise ValueError("'%s' is not one of the valid row formats (%s)"
                    % (rowformat, repr(list(self.ROW_FORMATS))))

        self.row = ResultRow if service.version >= 8 else TableResultRow

        if rowformat in self.PARSED_FORMATS:
            if service.version >= 8:
                params.update({"format": "json"})
            else:
                params.update({"format" : "jsonrows"})
        else:
            params.update({"format" : rowformat})

        self.url  = service.root + path
        self.data = urllib.urlencode(params)
        self.view = view
        self.opener = service.opener
        self.cld = cld
        self.rowformat = rowformat
        self._it = None

    def __len__(self):
        """
        Return the number of items in this iterator
        ===========================================

        Note that this requires iterating over the full result set.
        """
        c = 0
        for x in self:
            c += 1
        return c

    def __iter__(self):
        """
        Return an iterator over the results
        ===================================

        Returns the internal iterator object.
        """
        con = self.opener.open(self.url, self.data)
        identity = lambda x: x
        flat_file_parser = lambda: FlatFileIterator(con, identity)
        simple_json_parser = lambda: JSONIterator(con, identity)

        try:
            reader = {
                "tsv"         : flat_file_parser,
                "csv"         : flat_file_parser,
                "count"       : flat_file_parser,
                "json"        : simple_json_parser,
                "jsonrows"    : simple_json_parser,
                "list"        : lambda: JSONIterator(con, lambda x: self.row(x, self.view).to_l()),
                "rr"          : lambda: JSONIterator(con, lambda x: self.row(x, self.view)),
                "dict"        : lambda: JSONIterator(con, lambda x: self.row(x, self.view).to_d()),
                "jsonobjects" : lambda: JSONIterator(con, lambda x: ResultObject(x, self.cld, self.view))
            }.get(self.rowformat)()
        except Exception, e:
            raise Exception("Couldn't get iterator for "  + self.rowformat + str(e))
        return reader

    def next(self):
        """
        Returns the next row, in the appropriate format

        @rtype: whatever the rowformat was determined to be
        """
        if self._it is None:
            self._it = iter(self)
        try:
            return self._it.next()
        except StopIteration:
            self._it = None
            raise StopIteration

class FlatFileIterator(object):
    """
    An iterator for handling results returned as a flat file (TSV/CSV).
    ===================================================================

    This iterator can be used as the sub iterator in a ResultIterator
    """

    def __init__(self, connection, parser):
        """
        Constructor
        ===========

        @param connection: The source of data
        @type connection: socket.socket
        @param parser: a handler for each row of data
        @type parser: Parser
        """
        self.connection = connection
        self.parser = parser

    def __iter__(self):
        return self

    def next(self):
        """Return a parsed line of data"""
        line = self.connection.next().strip()
        if line.startswith("[ERROR]"):
            raise WebserviceError(line)
        return self.parser(line)

class JSONIterator(object):
    """
    An iterator for handling results returned in the JSONRows format
    ================================================================

    This iterator can be used as the sub iterator in a ResultIterator
    """

    def __init__(self, connection, parser):
        """
        Constructor
        ===========

        @param connection: The source of data
        @type connection: socket.socket
        @param parser: a handler for each row of data
        @type parser: Parser
        """
        self.connection = connection
        self.parser = parser
        self.header = ""
        self.footer = ""
        self.parse_header()
        self._is_finished = False

    def __iter__(self):
        return self

    def next(self):
        """Returns a parsed row of data"""
        if self._is_finished:
            raise StopIteration
        return self.get_next_row_from_connection()

    def parse_header(self):
        """Reads out the header information from the connection"""
        try:
            line = self.connection.next().strip()
            self.header += line
            if not line.endswith('"results":['):
                self.parse_header()
        except StopIteration:
            raise WebserviceError("The connection returned a bad header" + self.header)

    def check_return_status(self):
        """
        Perform status checks
        =====================

        The footer containts information as to whether the result
        set was successfully transferred in its entirety. This
        method makes sure we don't silently accept an
        incomplete result set.

        @raise WebserviceError: if the footer indicates there was an error
        """
        container = self.header + self.footer
        info = None
        try:
            info = json.loads(container)
        except:
            raise WebserviceError("Error parsing JSON container: " + container)

        if not info["wasSuccessful"]:
            raise WebserviceError(info["statusCode"], info["error"])

    def get_next_row_from_connection(self):
        """
        Reads the connection to get the next row, and sends it to the parser

        @raise WebserviceError: if the connection is interrupted
        """
        next_row = None
        try:
            line = self.connection.next()
            if line.startswith("]"):
                self.footer += line;
                for otherline in self.connection:
                    self.footer += line
                self.check_return_status()
            else:
                line = line.strip().strip(',')
                if len(line) > 0:
                    try:
                        row = json.loads(line)
                    except json.decoder.JSONDecodeError, e:
                        raise WebserviceError("Error parsing line from results: '"
                                + line + "' - " + str(e))
                    next_row = self.parser(row)
        except StopIteration:
            raise WebserviceError("Connection interrupted")

        if next_row is None:
            self._is_finished = True
            raise StopIteration
        else:
            return next_row

class InterMineURLOpener(urllib.FancyURLopener):
    """
    Specific implementation of urllib.FancyURLOpener for this client
    ================================================================

    Provides user agent and authentication headers, and handling of errors
    """
    version = "InterMine-Python-Client-0.96.00"

    def __init__(self, credentials=None, token=None):
        """
        Constructor
        ===========

        InterMineURLOpener((username, password)) S{->} InterMineURLOpener

        Return a new url-opener with the appropriate credentials
        """
        urllib.FancyURLopener.__init__(self)
        self.token = token
        self.plain_post_header = {
            "Content-Type": "text/plain; charset=utf-8",
            "UserAgent": Service.USER_AGENT
        }
        if credentials and len(credentials) == 2:
            base64string = base64.encodestring('%s:%s' % credentials)[:-1]
            self.addheader("Authorization", base64string)
            self.plain_post_header["Authorization"] = base64string
            self.using_authentication = True
        else:
            self.using_authentication = False

    def post_plain_text(self, url, body):
        url = self.prepare_url(url)
        o = urlparse(url)
        con = httplib.HTTPConnection(o.hostname, o.port)
        con.request('POST', url, body, self.plain_post_header)
        resp = con.getresponse()
        content = resp.read()
        con.close()
        if resp.status != 200:
            raise WebserviceError(resp.status, resp.reason, content)
        return content

    def open(self, url, data=None):
        url = self.prepare_url(url)
        return urllib.FancyURLopener.open(self, url, data)

    def prepare_url(self, url):
        if self.token:
            token_param = "token=" + self.token
            o = urlparse(url)
            if o.query:
                url += "&" + token_param
            else:
                url += "?" + token_param

        return url

    def delete(self, url):
        url = self.prepare_url(url)
        o = urlparse(url)
        con = httplib.HTTPConnection(o.hostname, o.port)
        con.request('DELETE', url, None, self.plain_post_header)
        resp = con.getresponse()
        content = resp.read()
        con.close()
        if resp.status != 200:
            raise WebserviceError(resp.status, resp.reason, content)
        return content

    def http_error_default(self, url, fp, errcode, errmsg, headers):
        """Re-implementation of http_error_default, with content now supplied by default"""
        content = fp.read()
        fp.close()
        raise WebserviceError(errcode, errmsg, content)

    def http_error_400(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Handle 400 HTTP errors, attempting to return informative error messages
        =======================================================================

        400 errors indicate that something about our request was incorrect

        @raise WebserviceError: in all circumstances

        """
        content = fp.read()
        fp.close()
        try:
            message = json.loads(content)["error"]
        except:
            message = content
        raise WebserviceError("There was a problem with our request", errcode, errmsg, message)

    def http_error_401(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Handle 401 HTTP errors, attempting to return informative error messages
        =======================================================================

        401 errors indicate we don't have sufficient permission for the resource
        we requested - usually a list or a tempate

        @raise WebserviceError: in all circumstances

        """
        content = fp.read()
        fp.close()
        if self.using_authentication:
            raise WebserviceError("Insufficient permissions", errcode, errmsg, content)
        else:
            raise WebserviceError("No permissions - not logged in", errcode, errmsg, content)

    def http_error_403(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Handle 403 HTTP errors, attempting to return informative error messages
        =======================================================================

        401 errors indicate we don't have sufficient permission for the resource
        we requested - usually a list or a tempate

        @raise WebserviceError: in all circumstances

        """
        content = fp.read()
        fp.close()
        try:
            message = json.loads(content)["error"]
        except:
            message = content
        if self.using_authentication:
            raise WebserviceError("Insufficient permissions", errcode, errmsg, message)
        else:
            raise WebserviceError("No permissions - not logged in", errcode, errmsg, message)

    def http_error_404(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Handle 404 HTTP errors, attempting to return informative error messages
        =======================================================================

        404 errors indicate that the requested resource does not exist - usually
        a template that is not longer available.

        @raise WebserviceError: in all circumstances

        """
        content = fp.read()
        fp.close()
        try:
            message = json.loads(content)["error"]
        except:
            message = content
        raise WebserviceError("Missing resource", errcode, errmsg, message)
    def http_error_500(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Handle 500 HTTP errors, attempting to return informative error messages
        =======================================================================

        500 errors indicate that the server borked during the request - ie: it wasn't
        our fault.

        @raise WebserviceError: in all circumstances

        """
        content = fp.read()
        fp.close()
        try:
            message = json.loads(content)["error"]
        except:
            message = content
        raise WebserviceError("Internal server error", errcode, errmsg, message)

class UnimplementedError(Exception):
    pass

class ServiceError(ReadableException):
    """Errors in the creation and use of the Service object"""
    pass
class WebserviceError(IOError):
    """Errors from interaction with the webservice"""
    pass
