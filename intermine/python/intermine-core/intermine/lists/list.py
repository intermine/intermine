import weakref
import urllib

from intermine.results import JSONIterator, EnrichmentLine

class List(object):
    """
    Class for representing a List on an InterMine Webservice
    ========================================================

    Lists represent stored collections of data and saved result
    sets in an InterMine data warehouse. This class is an abstraction
    of this information, and provides mechanisms for managing the
    data.

    SYNOPSIS
    --------

    example::

        >>> from intermine.webservice import Service
        >>>
        >>> flymine = Service("www.flymine.org/query", "SOMETOKEN")
        >>> new_list = flymine.create_list(["h", "zen", "eve", "bib"], "Gene", name="My New List")
        >>>
        >>> another_list = flymine.get_list("Some other list")
        >>> combined_list = new_list | another_list # Same syntax as for sets
        >>> combined_list.name = "Union of the other lists"
        >>>
        >>> print "The combination of the two lists has %d elements" % combined_list.size
        >>> print "The combination of the two lists has %d elements" % len(combined_list)
        >>>
        >>> for row in combined_list:
        ...     print row

    OVERVIEW
    --------

    Lists are created from a webservice, and can be manipulated in various ways.
    The operations are::
        * Union: this | that
        * Intersection: this & that
        * Symmetric Difference: this ^ that
        * Asymmetric Difference (subtraction): this - that
        * Appending: this += that

    Lists can be created from a list of identifiers that could be::
        * stored in a file
        * held in a list or set
        * contained in a string
    In all these cases the syntax is the same:

        >>> new_list = service.create_list(content, type, name="Some name", description="Some description", tags=["some", "tags"])

    Lists can also be created from a query's result with the exact
    same syntax. In the case of queries, the type is not required,
    but the query should have just one view, and it should be an id.

        >>> query = service.new_query()
        >>> query.add_view("Gene.id")
        >>> query.add_constraint("Gene.length", "<", 100)
        >>> new_list = service.create_list(query, name="Short Genes")

    """

    def __init__(self, **args):
        """
        Constructor
        ===========

        Do not construct these objects yourself. They should be
        fetched from a service or constructed using the "create_list"
        method.
        """
        try:
            self._service = args["service"]
            self._manager = weakref.proxy(args["manager"])
            self._name = args["name"]
            self._title = args["title"]
            self._description = args.get("description")
            self._list_type = args["type"]
            self._size = int(args["size"])
            self._date_created = args.get("dateCreated")
            self._is_authorized = args.get("authorized")
            self._status = args.get("status")

            if self._is_authorized is None: self._is_authorized = True

            if "tags" in args:
                tags = args["tags"]
            else:
                tags = []

            self._tags = frozenset(tags)
        except KeyError:
            raise ValueError("Missing argument")
        self.unmatched_identifiers = set([])

    @property
    def date_created(self):
        """When this list was originally created"""
        return self._date_created

    @property
    def tags(self):
        """The tags associated with this list"""
        return self._tags

    @property
    def description(self):
        """The human readable description of this list"""
        return self._description

    @property
    def title(self):
        """The fixed title of this list"""
        return self._title

    @property
    def status(self):
        """The upgrade status of this list"""
        return self._status

    @property
    def is_authorized(self):
        """Whether or not the current user is authorised to make changes to this list"""
        return self._is_authorized

    @property
    def list_type(self):
        """The type of the InterMine objects this list can contain"""
        return self._list_type

    def get_name(self):
        """The name of the list used to access it programmatically"""
        return self._name

    def set_name(self, new_name):
        """
        Set the name of the list
        ========================

        Setting the list's name causes the list's name to be updated on the server.
        """
        if self._name == new_name:
            return
        uri = self._service.root + self._service.LIST_RENAME_PATH
        params = {
            "oldname": self._name,
            "newname": new_name
        }
        uri += "?" + urllib.urlencode(params)
        resp = self._service.opener.open(uri)
        data = resp.read()
        resp.close()
        new_list = self._manager.parse_list_upload_response(data)
        self._name = new_name

    def del_name(self):
        """Raises an error - lists must always have a name"""
        raise AttributeError("List names cannot be deleted, only changed")

    @property
    def size(self):
        """Return the number of elements in the list. Also available as len(obj)"""
        return self._size

    @property
    def count(self):
        """Alias for obj.size. Also available as len(obj)"""
        return self.size

    def __len__(self):
        """Returns the number of elements in the object"""
        return self.size

    name = property(get_name, set_name, del_name, "The name of this list")

    def _add_failed_matches(self, ids):
        if ids is not None:
            self.unmatched_identifiers.update(ids)

    def __str__(self):
        string = self.name + " (" + str(self.size) + " " + self.list_type + ")"
        if self.date_created:
            string += " " + self.date_created
        if self.description:
            string += " " + self.description
        return string

    def delete(self):
        """
        Delete this list from the webservice
        ====================================

        Calls the webservice to delete this list immediately. This
        object should not be used after this method is called - attempts
        to do so will raise errors.
        """
        self._manager.delete_lists([self])

    def to_query(self):
        """
        Construct a query to fetch the items in this list
        =================================================

        Return a new query constrained to the objects in this list,
        and with a single view column of the objects ids.

        @rtype: intermine.query.Query
        """
        q = self._service.new_query(self.list_type)
        q.add_constraint(self.list_type, "IN", self.name)
        return q

    def __iter__(self):
        """Return an iterator over the objects in this list, with all attributes selected for output"""
        return iter(self.to_query())

    def __getitem__(self, index):
        """Get a member of this list by index"""
        if not isinstance(index, int):
            raise IndexError("Expected an integer key - got %s" % (index))
        if index < 0: # handle negative indices.
            i = self.size + index
        else:
            i = index

        if i not in range(self.size):
            raise IndexError("%d is not a valid index for a list of size %d" % (index, self.size))

        return self.to_query().first(start=i, row="jsonobjects")

    def __and__(self, other):
        """
        Intersect this list and another
        """
        return self._manager.intersect([self, other])

    def __iand__(self, other):
        """
        Intersect this list and another, and replace this list with the result of the
        intersection
        """
        intersection = self._manager.intersect([self, other], description=self.description, tags=self.tags)
        self.delete()
        intersection.name = self.name
        return intersection

    def __or__(self, other):
        """
        Return the union of this list and another
        """
        return self._manager.union([self, other])

    def __add__(self, other):
        """
        Return the union of this list and another
        """
        return self._manager.union([self, other])

    def __iadd__(self, other):
        """
        Append other to this list.
        """
        return self.append(other)

    def _do_append(self, content):
        name = self.name
        data = None

        try:
            ids = open(content).read()
        except (TypeError, IOError):
            if isinstance(content, basestring):
                ids = content
            else:
                try:
                    ids = "\n".join(map(lambda x: '"' + x + '"', iter(content)))
                except TypeError:
                    content = self._manager._get_listable_query(content)
                    uri = content.get_list_append_uri()
                    params = content.to_query_params()
                    params["listName"] = name
                    params["path"] = None
                    form = urllib.urlencode(params)
                    resp = self._service.opener.open(uri, form)
                    data = resp.read()

        if data is None:
            uri = self._service.root + self._service.LIST_APPENDING_PATH
            query_form = {'name': name}
            uri += "?" + urllib.urlencode(query_form)
            data = self._service.opener.post_plain_text(uri, ids)

        new_list = self._manager.parse_list_upload_response(data)
        self.unmatched_identifiers.update(new_list.unmatched_identifiers)
        self._size = new_list.size
        return self

    def append(self, appendix):
        """Append the arguments to this list"""
        try:
            return self._do_append(self._manager.union(appendix))
        except:
            return self._do_append(appendix)

    def calculate_enrichment(self, widget, background = None, correction = "Holm-Bonferroni", maxp = 0.05, filter = ''):
        """Perform an enrichment calculation on this list"""
        params = dict(list = self.name, widget = widget, correction = correction, maxp = maxp, filter = filter)
        if background is not None:
            params["population"] = background
        form = urllib.urlencode(params)
        uri = self._service.root + self._service.LIST_ENRICHMENT_PATH
        resp = self._service.opener.open(uri, form)
        return JSONIterator(resp, EnrichmentLine)

    def __xor__(self, other):
        """Calculate the symmetric difference of this list and another"""
        return self._manager.xor([self, other])

    def __ixor__(self, other):
        """Calculate the symmetric difference of this list and another and replace this list with the result"""
        diff = self._manager.xor([self, other], description=self.description, tags=self.tags)
        self.delete()
        diff.name = self.name
        return diff

    def __sub__(self, other):
        """Subtract the other from this list"""
        return self._manager.subtract([self], [other])

    def __isub__(self, other):
        """Replace this list with the subtraction of the other from this list"""
        subtr = self._manager.subtract([self], [other], description=self.description, tags=self.tags)
        self.delete()
        subtr.name = self.name
        return subtr

    def add_tags(self, *tags):
        """
        Tag this list with one or more categories
        =========================================

        Calls the server to add these tags, and updates this lists tags.
        """
        self._tags = frozenset(self._manager.add_tags(self, tags))

    def remove_tags(self, *tags):
        """
        Remove tags associated with this list.
        ======================================

        Calls the server to remove these tags, and updates this lists tags.
        """
        self._tags = frozenset(self._manager.remove_tags(self, tags))

    def update_tags(self, *tags):
        """
        Remove tags associated with this list.
        ======================================

        Calls the server to remove these tags, and updates this lists tags.
        """
        self._tags = frozenset(self._manager.get_tags(self))

