from xml.dom import minidom
import urllib
from urlparse import urlparse
import base64
import UserDict

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
from intermine.lists.listmanager import ListManager
from intermine.errors import ServiceError, WebserviceError
from intermine.results import InterMineURLOpener

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
    QUERY_PATH             = '/query/results'
    LIST_ENRICHMENT_PATH   = '/list/enrichment'
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

