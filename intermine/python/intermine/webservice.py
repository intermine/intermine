from urlparse import urlunsplit, urljoin
from xml.dom import minidom
import urllib
import csv
import base64

# Local intermine imports
from .query import Query, Template
from .model import Model
from .util import ReadableException

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
      
    OVERVIEW
    --------
    The two methods the user will be most concerned with are:
      - L{Service.new_query}: constructs a new query to query a service with
      - L{Service.get_template}: gets a template from the service

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

    @see: L{intermine.query}
    """
    QUERY_PATH         = '/query/results'
    MODEL_PATH         = '/model'
    TEMPLATES_PATH     = '/templates/xml'
    TEMPLATEQUERY_PATH = '/template/results'
    VERSION_PATH       = '/version'
    USER_AGENT         = 'WebserviceInterMinePerlAPIClient'
    LIST_PATH          = '/lists/xml'
    SAVEDQUERY_PATH    = '/savedqueries/xml'
    RELEASE_PATH       = '/version/release'
    SCHEME             = 'http://'

    def __init__(self, root, username=None, password=None):
        """
        Constructor
        ===========

        Construct a connection to a webservice::

            service = Service("http://www.flymine.org/query/service")

        @param root: the root url of the webservice (required)
        @param username: your login name (optional)
        @param password: your password (required if a username is given)

        @raise ServiceError: if the version cannot be fetched and parsed
        @raise ValueError:   if a username is supplied, but no password

        @return: Service

        """
        self.root = root
        self._templates = None
        self._model = None
        self._version = None
        self._release = None
        if username:
            if not password:
                raise ValueError("No password supplied")
            self.opener = InterMineURLOpener((username, password))
        else:
            self.opener = InterMineURLOpener()

# This works in the real world, but not in testing...
#       try:
#           self.version
#       except ServiceError:
#           raise ServiceError("Could not validate service - is the root url correct?")

    @property
    def version(self):
        """
        Returns the webservice version
        ==============================

        Service.version S{->} int
            
        May throw: ServiceError, if the version cannot be fetched

        The version specifies what capabilities a
        specific webservice provides. The most current 
        version is 3
        """
        if self._version is None:
            try:
                url = self.root + self.VERSION_PATH
                self._version = int(self.opener.open(url).read())
            except ValueError:
                raise ServiceError("Could not parse a valid webservice version")
        return self._version
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
        """
        if self._release is None:
            self._release = urllib.urlopen(self.root + RELEASE_PATH).read()
        return self._release

    def new_query(self):
        """
        Construct a new Query object for the given webservice
        =====================================================

        This is the standard method for instantiating new Query
        objects. Queries require access to the data model, as well
        as the service itself, so it is easiest to access them through
        this factory method.

        @return: L{intermine.query.Query}
        """
        return Query(self.model, self)

    def get_template(self, name):
        """
        Returns a template of the given name
        ====================================

        Tries to retrieve a template of the given name
        from the webservice. If you are trying to fetch
        a private template (ie. one you made yourself 
        and is not available to others) then you may need to authenticate
        
        @see: L{intermine.service.Service.__init__}

        @param name: the template's name as a string

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

        @type: dict

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

        @type: L{intermine.model.Model}

        """
        if self._model is None:
            model_url = self.root + self.MODEL_PATH
            self._model = Model(model_url)
        return self._model

    def get_results(self, path, params, rowformat, view):
        """
        Return an Iterator over the rows of the results
        ===============================================

        This method is called internally by the query objects
        when they are called to get results. You will not 
        normally need to call it directly

        @param path: The resource path (eg: "/query/results")
        @param params: The query parameters for this request as a dictionary
        @param rowformat: One of "dict", "list", "string"
        @param view: The output columns

        @raise WebserviceError: for failed requests

        @return: L{intermine.webservice.ResultIterator}
        """
        return ResultIterator(self.root, path, params, rowformat, view, self.opener)

    def get_results_list(self, path, params, rowformat, view):
        """
        Return a list of the rows of the results
        ========================================

        This method is called internally by the query objects
        when they are called to get results. You will not 
        normally need to call it directly

        @param path: The resource path (eg: "/query/results")
        @param params: The query parameters for this request as a dictionary
        @param rowformat: One of "dict", "list", "string"
        @param view: The output columns

        @raise WebserviceError: for failed requests

        @return: list(list|dict|string)
        """
        rows = self.get_results(path, params, rowformat, view)
        return [r for r in rows]

class ResultIterator(object):
    
    ROW_FORMATS = frozenset(["string", "list", "dict"])

    def __init__(self, root, path, params, rowformat, view, opener):
        """
        Constructor
        ===========

        Services are responsible for getting result iterators. You will 
        not need to create one manually.

        @param root: The root path (eg: "http://www.flymine.org/query/service")
        @param path: The resource path (eg: "/query/results")
        @param params: The query parameters for this request as a dictionary
        @param rowformat: One of "dict", "list", "string"
        @param view: The output columns
        @param opener: A urllib.URLopener object

        @raise ValueError: if the row format is incorrect
        @raise WebserviceError: if the request is unsuccessful
        """
        if rowformat not in self.ROW_FORMATS:
            raise ValueError("'" + rowformat + "' is not a valid row format:" + self.ROW_FORMATS)

        params.update({"format" : "csv"})
        url  = root + path
        data = urllib.urlencode(params)
        con = opener.open(url + "?" + data)
        self.reader = {
            "string" : lambda: con,
            "list"   : lambda: csv.reader(con),
            "dict"   : lambda: csv.DictReader(con, view)
        }.get(rowformat)()

    def __iter__(self):
        return self.reader

    def next(self):
        """Returns the next row, in the appropriate format"""
        return self.reader.next()

class InterMineURLOpener(urllib.FancyURLopener):
    """
    Specific implementation of urllib.FancyURLOpener for this client
    ================================================================

    Provides user agent and authentication headers, and handling of errors
    """
    version = "InterMine-Python-Client-0.96.00"

    def __init__(self, credentials=None):
        """
        Constructor
        ===========

        InterMineURLOpener((username, password)) S{->} InterMineURLOpener

        Return a new url-opener with the appropriate credentials
        """
        urllib.FancyURLopener.__init__(self)
        if credentials and len(credentials) == 2:
            base64string = base64.encodestring('%s:%s' % credentials)[:-1]
            self.addheader("Authorization", base64string)
            self.using_authentication = True
        else: 
            self.using_authentication = False

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
        raise WebserviceError("There was a problem with our request", errcode, errmsg, content)

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
        raise WebserviceError("Missing resource", errcode, errmsg, content)
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
        raise WebserviceError("Internal server error", errcode, errmsg, content)

class ServiceError(ReadableException):
    """Errors in the creation and use of the Service object"""
    pass
class WebserviceError(IOError):
    """Errors from interaction with the webservice"""
    pass
