import re
from copy import deepcopy
from xml.dom import minidom, getDOMImplementation

from .util import openAnything, ReadableException
from .pathfeatures import PathDescription, Join, SortOrder, SortOrderList
import constraints

"""
Classes representing queries against webservices
================================================

Representations of queries, and templates.

"""

__author__ = "Alex Kalderimis"
__organization__ = "InterMine"
__license__ = "LGPL"
__contact__ = "dev@intermine.org"


class Query(object):
    """
    A Class representing a structured database query
    ================================================

    Objects of this class have properties that model the
    attributes of the query, and methods for performing
    the request.

    SYNOPSIS
    --------

    example:

       >>> service = Service("http://www.flymine.org/query/service")
       >>> query = service.new_query()
       >>>
       >>> query.add_view("Gene.symbol", "Gene.pathways.name", "Gene.proteins.symbol")
       >>> query.add_sort_order("Gene.pathways.name")
       >>>
       >>> query.add_constraint("Gene", "LOOKUP", "eve")
       >>> query.add_constraint("Gene.pathways.name", "=", "Phosphate*")
       >>>
       >>> query.set_logic("A or B")
       >>>
       >>> for row in query.results():
       ...     handle_row(row)
    
    Query objects represent structured requests for information over the database
    housed at the datawarehouse whose webservice you are querying. They utilise 
    some of the concepts of relational databases, within an object-related 
    ORM context. If you don't know what that means, don't worry: you
    don't need to write SQL, and the queries will be fast.

    PRINCIPLES
    ----------
    
    The data model represents tables in the databases as classes, with records
    within tables as instances of that class. The columns of the database are the
    fields of that object::

      The Gene table - showing two records/objects
      +---------------------------------------------------+
      | id  | symbol  | length | cyto-location | organism |
      +----------------------------------------+----------+
      | 01  | eve     | 1539   | 46C10-46C10   |  01      |
      +----------------------------------------+----------+
      | 02  | zen     | 1331   | 84A5-84A5     |  01      |
      +----------------------------------------+----------+
      ...

      The organism table - showing one record/object
      +----------------------------------+
      | id  | name            | taxon id |
      +----------------------------------+
      | 01  | D. melanogaster | 7227     |
      +----------------------------------+
    
    Columns that contain a meaningful value are known as 'attributes' (in the tables above, that is 
    everything except the id columns). The other columns (such as "organism" in the gene table)
    are ones that reference records of other tables (ie. other objects), and are called 
    references. You can refer to any field in any class, that has a connection, 
    however tenuous, with a table, by using dotted path notation::

      Gene.organism.name -> the name column in the organism table, referenced by a record in the gene table

    These paths, and the connections between records and tables they represent,
    are the basis for the structure of InterMine queries.

    THE STUCTURE OF A QUERY
    -----------------------

    A query has two principle sets of properties:
      - its view: the set of output columns
      - its constraints: the set of rules for what to include

    A query must have at least one output column in its view, but constraints
    are optional - if you don't include any, you will get back every record
    from the table (every object of that type)

    In addition, the query must be coherent: if you have information about
    an organism, and you want a list of genes, then the "Gene" table
    should be the basis for your query, and as such the Gene class, which
    represents this table, should be the root of all the paths that appear in it:

    So, to take a simple example::
    
        I have an organism name, and I want a list of genes:

    The view is the list of things I want to know about those genes:

        >>> query.add_view("Gene.name")
        >>> query.add_view("Gene.length")
        >>> query.add_view("Gene.proteins.sequence.length")

    Note I can freely mix attributes and references, as long as every view ends in
    an attribute (a meaningful value). As a short-cut I can also write:

        >>> query.add_view("Gene.name", "Gene.length", "Gene.proteins.sequence.length")

    or:

        >>> query.add_view("Gene.name Gene.length Gene.proteins.sequence.length") 

    They are all equivalent.

    Now I can add my constraints. As, we mentioned, I have information about an organism, so:

        >>> query.add_constraint("Gene.organism.name", "=", "D. melanogaster")

    If I run this query, I will get literally millions of results - 
    it needs to be filtered further:

        >>> query.add_constraint("Gene.proteins.sequence.length", "<", 500)
    
    If that doesn't restrict things enough I can add more filters:

        >>> query.add_constraint("Gene.symbol", "ONE OF", ["eve", "zen", "h"])

    Now I am guaranteed to get only information on genes I am interested in. 

    Note, though, that because I have included the link (or "join") from Gene -> Protein,
    this, by default, means that I only want genes that have protein information associated 
    with them. If in fact I want information on all genes, and just want to know the 
    protein information if it is available, then I can specify that with:

        >>> query.add_join("Gene.proteins", "OUTER")

    And if perhaps my query is not as simple as a strict cumulative filter, but I want all 
    D. mel genes that EITHER have a short protein sequence OR come from one of my favourite genes 
    (as unlikely as that sounds), I can specify the logic for that too:

        >>> query.set_logic("A and (B or C)")

    Each letter refers to one of the constraints - the codes are assigned in the order you add
    the constraints. If you want to be absolutely certain about the constraints you mean, you
    can use the constraint objects themselves:

      >>> gene_is_eve = query.add_constraint("Gene.symbol", "=", "eve")
      >>> gene_is_zen = query.add_constraint("Gene.symbol", "=", "zne")
      >>>
      >>> query.set_logic(gene_is_eve | gene_is_zen)

    By default the logic is a straight cumulative filter (ie: A and B and C and D  and ...)

    Putting it all together:

       >>> query.add_view("Gene.name", "Gene.length", "Gene.proteins.sequence.length")
       >>> query.add_constraint("Gene.organism.name", "=", "D. melanogaster")
       >>> query.add_constraint("Gene.proteins.sequence.length", "<", 500)
       >>> query.add_constraint("Gene.symbol", "ONE OF", ["eve", "zen", "h"])
       >>> query.add_join("Gene.proteins", "OUTER")
       >>> query.set_logic("A and (B or C)")

    And the query is defined.

    Result Processing
    -----------------

    calling ".results()" on a query will return an iterator of rows, where each row 
    is a list of values, one for each field in the output columns (view) you selected.

    To process these simply use normal iteration syntax:

        >>> for row in query.results():
        ...     for column in row:
        ...         do_something(column)

    Here each row will have a gene name, a gene length, and a sequence length, eg:

        >>> print row
        ["even skipped", "1359", "376"]

    To make that clearer, you can ask for a dictionary instead of a list:

        >>> for row in query.result("dict")
        ...       print row
        {"Gene.name":"even skipped","Gene.length":"1359","Gene.proteins.sequence.length":"376"}

    Which means you can refer to columns by name:
        
        >>> for row in query.result("dict")
        ...     print "name is", row["Gene.name"]
        ...     print "length is", row["Gene.length"]

    If you just want the raw results, for printing to a file, or for piping to another program, 
    you can request strings instead:

        >>> for row in query.result("string")
        ...     print(row)


    Getting us to Generate your Code 
    --------------------------------

    Not that you have to actually write any of this! The webapp will happily
    generate the code for any query (and template) you can build in it. A good way to get
    started is to use the webapp to generate your code, and then run it as scripts
    to speed up your queries. You can always tinker with and edit the scripts you download.

    To get generated queries, look for the "python" link at the bottom of query-builder and
    template form pages, it looks a bit like this::

      . +=====================================+=============
        |                                     |
        |    Perl  |  Python  |  Java [Help]  |
        |                                     |
        +==============================================

    """
    def __init__(self, model, service=None, validate=True):
        """
        Construct a new Query
        =====================

        Construct a new query for making database queries
        against an InterMine data warehouse. 

        Normally you would not need to use this constructor
        directly, but instead use the factory method on 
        intermine.webservice.Service, which will handle construction
        for you.

        @param model: an instance of L{intermine.model.Model}. Required
        @param service: an instance of l{intermine.service.Service}. Optional, 
            but you will not be able to make requests without one.
        @param validate: a boolean - defaults to True. If set to false, the query
            will not try and validate itself. You should not set this to false.

        """
        self.model = model
        self.name = ''
        self.description = ''
        self.service = service
        self.do_verification = validate
        self.path_descriptions = []
        self.joins = []
        self.constraint_dict = {}
        self.uncoded_constraints = []
        self.views = []
        self._sort_order_list = SortOrderList()
        self._logic_parser = constraints.LogicParser(self)
        self._logic = None
        self.constraint_factory = constraints.ConstraintFactory()

    @classmethod
    def from_xml(cls, xml, *args, **kwargs):
        """
        Deserialise a query serialised to XML
        =====================================

        This method is used to instantiate serialised queries.
        It is used by intermine.webservice.Service objects
        to instantiate Template objects and it can be used
        to read in queries you have saved to a file. 

        @param xml: The xml as a file name, url, or string

        @raise QueryParseError: if the query cannot be parsed
        @raise ModelError: if the query has illegal paths in it
        @raise ConstraintError: if the constraints don't make sense

        @rtype: L{Query}
        """
        obj = cls(*args, **kwargs)
        obj.do_verification = False
        f = openAnything(xml)
        doc = minidom.parse(f)
        f.close()

        queries = doc.getElementsByTagName('query')
        assert len(queries) == 1, "wrong number of queries in xml"
        q = queries[0]
        obj.name = q.getAttribute('name')
        obj.description = q.getAttribute('description')
        obj.add_view(q.getAttribute('view'))
        for p in q.getElementsByTagName('pathDescription'):
            path = p.getAttribute('pathString')
            description = p.getAttribute('description')
            obj.add_path_description(path, description)
        for j in q.getElementsByTagName('join'):
            path = j.getAttribute('path')
            style = j.getAttribute('style')
            obj.add_join(path, style)
        for c in q.getElementsByTagName('constraint'):
            args = {}
            args['path'] = c.getAttribute('path')
            if args['path'] is None:
                if c.parentNode.tagName != "node":
                    msg = "Constraints must have a path"
                    raise QueryParseError(msg)
                args['path'] = c.parentNode.getAttribute('path')
            args['op'] = c.getAttribute('op')
            args['value'] = c.getAttribute('value')
            args['code'] = c.getAttribute('code')
            args['subclass'] = c.getAttribute('type')
            args['editable'] = c.getAttribute('editable')
            args['optional'] = c.getAttribute('switchable')
            args['extra_value'] = c.getAttribute('extraValue')
            args['loopPath'] = c.getAttribute('loopPath')
            values = []
            for val_e in c.getElementsByTagName('value'):
                texts = []
                for node in val_e.childNodes:
                    if node.nodeType == node.TEXT_NODE: texts.append(node.data)
                values.append(' '.join(texts))
            if len(values) > 0: args["values"] = values
            for k, v in args.items():
                if v is None or v == '': del args[k]
            if "loopPath" in args:
                args["op"] = {
                    "=" : "IS",
                    "!=": "IS NOT"
                }.get(args["op"])
            con = obj.add_constraint(**args)
            if not con:
                raise ConstraintError("error adding constraint with args: " + args)
        obj.verify()        

        return obj

    def verify(self):
        """
        Validate the query
        ==================

        Invalid queries will fail to run, and it is not always
        obvious why. The validation routine checks to see that 
        the query will not cause errors on execution, and tries to
        provide informative error messages.

        This method is called immediately after a query is fully 
        deserialised.

        @raise ModelError: if the paths are invalid
        @raise QueryError: if there are errors in query construction
        @raise ConstraintError: if there are errors in constraint construction
        
        """
        self.verify_views()
        self.verify_constraint_paths()
        self.verify_join_paths()
        self.verify_pd_paths()
        self.validate_sort_order()
        self.do_verification = True

    def add_view(self, *paths):
        """
        Add one or more views to the list of output columns
        ===================================================

        example::
            
            query.add_view("Gene.name Gene.organism.name")

        This is the main method for adding views to the list
        of output columns. As well as appending views, it
        will also split a single, space or comma delimited
        string into multiple paths, and flatten out lists, or any
        combination. It will also immediately try to validate 
        the views.

        Output columns must be valid paths according to the 
        data model, and they must represent attributes of tables

        @see: intermine.model.Model 
        @see: intermine.model.Path 
        @see: intermine.model.Attribute
        """
        views = []
        for p in paths:
            if isinstance(p, (set, list)):
                views.extend(list(p))
            else:
                views.extend(re.split("(?:,?\s+|,)", p))
        if self.do_verification: self.verify_views(views)
        self.views.extend(views)

    def verify_views(self, views=None):
        """
        Check to see if the views given are valid
        =========================================

        This method checks to see if the views:
          - are valid according to the model
          - represent attributes

        @see: L{intermine.model.Attribute}

        @raise intermine.model.ModelError: if the paths are invalid
        @raise ConstraintError: if the paths are not attributes
        """
        if views is None: views = self.views
        for path in views:
            path = self.model.make_path(path, self.get_subclass_dict())
            if not path.is_attribute():
                raise ConstraintError("'" + str(path) 
                        + "' does not represent an attribute")

    def add_constraint(self, *args, **kwargs):
        """
        Add a constraint (filter on records)
        ====================================
    
        example::
            
            query.add_constraint("Gene.symbol", "=", "zen")

        This method will try to make a constraint from the arguments
        given, trying each of the classes it knows of in turn 
        to see if they accept the arguments. This allows you 
        to add constraints of different types without having to know
        or care what their classes or implementation details are.
        All constraints derive from intermine.constraints.Constraint, 
        and they all have a path attribute, but are otherwise diverse.

        Before adding the constraint to the query, this method
        will also try to check that the constraint is valid by 
        calling Query.verify_constraint_paths()

        @see: L{intermine.constraints}

        @rtype: L{intermine.constraints.Constraint}
        """
        con = self.constraint_factory.make_constraint(*args, **kwargs)
        if self.do_verification: self.verify_constraint_paths([con])
        if hasattr(con, "code"): 
            self.constraint_dict[con.code] = con
        else:
            self.uncoded_constraints.append(con)
        
        return con

    def verify_constraint_paths(self, cons=None):
        """
        Check that the constraints are valid
        ====================================

        This method will check the path attribute of each constraint.
        In addition it will:
          - Check that BinaryConstraints and MultiConstraints have an Attribute as their path
          - Check that TernaryConstraints have a Reference as theirs
          - Check that SubClassConstraints have a correct subclass relationship
          - Check that LoopConstraints have a valid loopPath, of a compatible type
          - Check that ListConstraints refer to an object

        @param cons: The constraints to check (defaults to all constraints on the query)

        @raise ModelError: if the paths are not valid
        @raise ConstraintError: if the constraints do not satisfy the above rules

        """
        if cons is None: cons = self.constraints
        for con in cons:
            pathA = self.model.make_path(con.path, self.get_subclass_dict())
            if isinstance(con, constraints.TernaryConstraint):
                if pathA.get_class() is None:
                    raise ConstraintError("'" + str(pathA) + "' does not represent a class, or a reference to a class")
            elif isinstance(con, constraints.BinaryConstraint) or isinstance(con, constraints.MultiConstraint):
                if not pathA.is_attribute():
                    raise ConstraintError("'" + str(pathA) + "' does not represent an attribute")
            elif isinstance(con, constraints.SubClassConstraint):
                pathB = self.model.make_path(con.subclass, self.get_subclass_dict())
                if not pathB.get_class().isa(pathA.get_class()):
                    raise ConstraintError("'" + con.subclass + "' is not a subclass of '" + con.path + "'")
            elif isinstance(con, constraints.LoopConstraint):
                pathB = self.model.make_path(con.loopPath, self.get_subclass_dict())
                for path in [pathA, pathB]:
                    if not path.get_class():
                        raise ConstraintError("'" + str(path) + "' does not refer to an object")
                (classA, classB) = (pathA.get_class(), pathB.get_class())
                if not classA.isa(classB) and not classB.isa(classA):
                    raise ConstraintError("the classes are of incompatible types: " + str(classA) + "," + str(classB))
            elif isinstance(con, constraints.ListConstraint):
                if not pathA.get_class():
                    raise ConstraintError("'" + str(pathA) + "' does not refer to an object")

    @property
    def constraints(self):
        """
        Returns the constraints of the query
        ====================================

        Query.constraints S{->} list(intermine.constraints.Constraint)

        Constraints are returned in the order of their code (normally
        the order they were added to the query) and with any
        subclass contraints at the end.

        @rtype: list(Constraint)
        """
        ret = sorted(self.constraint_dict.values(), key=lambda con: con.code)
        ret.extend(self.uncoded_constraints)
        return ret

    def get_constraint(self, code):
        """
        Returns the constraint with the given code
        ==========================================

        Returns the constraint with the given code, if if exists.
        If no such constraint exists, it throws a ConstraintError

        @return: the constraint corresponding to the given code
        @rtype: L{intermine.constraints.CodedConstraint}
        """
        if code in self.constraint_dict: 
            return self.constraint_dict[code]
        else:
            raise ConstraintError("There is no constraint with the code '"  
                                    + code + "' on this query")
        
    def add_join(self, *args ,**kwargs):
        """
        Add a join statement to the query
        =================================

        example::

         query.add_join("Gene.proteins", "OUTER")

        A join statement is used to determine if references should
        restrict the result set by only including those references
        exist. For example, if one had a query with the view::
        
          "Gene.name", "Gene.proteins.name"

        Then in the normal case (that of an INNER join), we would only 
        get Genes that also have at least one protein that they reference.
        Simply by asking for this output column you are placing a 
        restriction on the information you get back. 
        
        If in fact you wanted all genes, regardless of whether they had  
        proteins associated with them or not, but if they did 
        you would rather like to know _what_ proteins, then you need
        to specify this reference to be an OUTER join::

         query.add_join("Gene.proteins", "OUTER")

        Now you will get many more rows of results, some of which will
        have "null" values where the protein name would have been,

        This method will also attempt to validate the join by calling
        Query.verify_join_paths(). Joins must have a valid path, the 
        style can be either INNER or OUTER (defaults to OUTER,
        as the user does not need to specify inner joins, since all
        references start out as inner joins), and the path 
        must be a reference.

        @raise ModelError: if the path is invalid
        @raise TypeError: if the join style is invalid

        @rtype: L{intermine.pathfeatures.Join}
        """
        join = Join(*args, **kwargs)
        if self.do_verification: self.verify_join_paths([join])
        self.joins.append(join)
        return join

    def verify_join_paths(self, joins=None):
        """
        Check that the joins are valid
        ==============================

        Joins must have valid paths, and they must refer to references.

        @raise ModelError: if the paths are invalid
        @raise QueryError: if the paths are not references
        """
        if joins is None: joins = self.joins
        for join in joins:
            path = self.model.make_path(join.path, self.get_subclass_dict())
            if not path.is_reference():
                raise QueryError("'" + join.path + "' is not a reference")

    def add_path_description(self, *args ,**kwargs):
        """
        Add a path description to the query
        ===================================

        example::
            
            query.add_path_description("Gene.symbol", "The symbol for this gene")

        If you wish you can add annotations to your query that describe
        what the component paths are and what they do - this is only really
        useful if you plan to keep your query (perhaps as xml) or store it
        as a template.

        @rtype: L{intermine.pathfeatures.PathDescription}

        """
        path_description = PathDescription(*args, **kwargs)
        if self.do_verification: self.verify_pd_paths([path_description])
        self.path_descriptions.append(path_description)
        return path_description

    def verify_pd_paths(self, pds=None):
        """
        Check that the path of the path description is valid
        ====================================================

        Checks for consistency with the data model

        @raise ModelError: if the paths are invalid
        """
        if pds is None: pds = self.path_descriptions
        for pd in pds: 
            self.model.validate_path(pd.path, self.get_subclass_dict())

    @property
    def coded_constraints(self):
        """
        Returns the list of constraints that have a code
        ================================================

        Query.coded_constraints S{->} list(intermine.constraints.CodedConstraint)

        This returns an up to date list of the constraints that can
        be used in a logic expression. The only kind of constraint 
        that this excludes, at present, is SubClassConstraints

        @rtype: list(L{intermine.constraints.CodedConstraint})
        """
        return sorted(self.constraint_dict.values(), key=lambda con: con.code)

    def get_logic(self):
        """
        Returns the logic expression for the query
        ==========================================

        This returns the up to date logic expression. The default
        value is the representation of all coded constraints and'ed together.

        The LogicGroup object stringifies to a string that can be parsed to 
        obtain itself (eg: "A and (B or C or D)").

        @rtype: L{intermine.constraints.LogicGroup}
        """
        if self._logic is None:
            return reduce(lambda x, y: x+y, self.coded_constraints)
        else:
            return self._logic

    def set_logic(self, value):
        """
        Sets the Logic given the appropriate input
        ==========================================

        example::

          Query.set_logic("A and (B or C)")

        This sets the logic to the appropriate value. If the value is
        already a LogicGroup, it is accepted, otherwise
        the string is tokenised and parsed.

        The logic is then validated with a call to validate_logic()

        raise LogicParseError: if there is a syntax error in the logic
        """
        if isinstance(value, constraints.LogicGroup):
            logic = value
        else: 
            logic = self._logic_parser.parse(value)
        if self.do_verification: self.validate_logic(logic)
        self._logic = logic

    def validate_logic(self, logic=None):
        """
        Validates the query logic
        =========================

        Attempts to validate the logic by checking
        that every coded_constraint is included
        at least once

        @raise QueryError: if not every coded constraint is represented
        """
        if logic is None: logic = self._logic
        logic_codes = set(logic.get_codes())
        for con in self.coded_constraints:
            if con.code not in logic_codes:
                raise QueryError("Constraint " + con.code + repr(con) 
                        + " is not mentioned in the logic: " + str(logic))

    def get_default_sort_order(self):
        """
        Gets the sort order when none has been specified
        ================================================

        This method is called to determine the sort order if
        none is specified

        @raise QueryError: if the view is empty

        @rtype: L{intermine.pathfeatures.SortOrderList}
        """
        try:
            return SortOrderList((self.views[0], SortOrder.ASC))
        except IndexError:
            raise QueryError("Query view is empty")

    def get_sort_order(self):
        """
        Return a sort order for the query
        =================================

        This method returns the sort order if set, otherwise
        it returns the default sort order

        @raise QueryError: if the view is empty

        @rtype: L{intermine.pathfeatures.SortOrderList}
        """
        if self._sort_order_list.is_empty():
            return self.get_default_sort_order()         
        else:
            return self._sort_order_list

    def add_sort_order(self, path, direction=SortOrder.ASC):
        """
        Adds a sort order to the query
        ==============================

        example::

          Query.add_sort_order("Gene.name", "DESC")

        This method adds a sort order to the query. 
        A query can have multiple sort orders, which are 
        assessed in sequence. 
        
        If a query has two sort-orders, for example, 
        the first being "Gene.organism.name asc",
        and the second being "Gene.name desc", you would have 
        the list of genes grouped by organism, with the
        lists within those groupings in reverse alphabetical
        order by gene name.

        This method will try to validate the sort order
        by calling validate_sort_order()
        """
        so = SortOrder(path, direction)
        if self.do_verification: self.validate_sort_order(so)
        self._sort_order_list.append(so)

    def validate_sort_order(self, *so_elems):
        """
        Check the validity of the sort order
        ====================================
        
        Checks that the sort order paths are:
          - valid paths
          - in the view

        @raise QueryError: if the sort order is not in the view
        @raise ModelError: if the path is invalid

        """
        if not so_elems:
            so_elems = self._sort_order_list
        
        for so in so_elems:
            self.model.validate_path(so.path, self.get_subclass_dict())
            if so.path not in self.views:
                raise QueryError("Sort order element is not in the view: " + so.path)

    def get_subclass_dict(self):
        """
        Return the current mapping of class to subclass
        ===============================================

        This method returns a mapping of classes used 
        by the model for assessing whether certain paths are valid. For 
        intance, if you subclass MicroArrayResult to be FlyAtlasResult, 
        you can refer to the .presentCall attributes of fly atlas results. 
        MicroArrayResults do not have this attribute, and a path such as::

          Gene.microArrayResult.presentCall

        would be marked as invalid unless the dictionary is provided. 

        Users most likely will not need to ever call this method.

        @rtype: dict(string, string)
        """
        subclass_dict = {}
        for c in self.constraints:
            if isinstance(c, constraints.SubClassConstraint):
                subclass_dict[c.path] = c.subclass
        return subclass_dict

    def results(self, row="list"):
        """
        Return an iterator over result rows
        ===================================

        Usage::

          for row in query.results():
            do_sth_with(row)
        
        @param row: the format for the row. Defaults to "list". Valid options are 
            "dict", "list", "jsonrows", "jsonobject", "tsv", "csv". 
        @type row: string

        @rtype: L{intermine.webservice.ResultIterator}

        @raise WebserviceError: if the request is unsuccessful
        """
        path = self.get_results_path()
        params = self.to_query_params()
        view = self.views
        return self.service.get_results(path, params, row, view)

    def get_results_path(self):
        """
        Returns the path section pointing to the REST resource
        ======================================================

        Query.get_results_path() -> str

        Internally, this just calls a constant property
        in intermine.service.Service

        @rtype: str
        """
        return self.service.QUERY_PATH

    def get_results_list(self, rowformat="list"):
        """
        Get a list of result rows
        =========================

        This method is a shortcut so that you do not have to
        do a list comprehension yourself on the iterator that 
        is normally returned. If you have a very large result 
        set (in the millions of rows) you will not want to
        have the whole list in memory at once, but there may 
        be other circumstances when you might want to keep the whole
        list in one place.

        @param rowformat: the format for the row. Defaults to "list". Valid options are 
            "dict", "list", "jsonrows", "jsonobject", "tsv", "csv". 
        @type rowformat: string

        @rtype: list

        @raise WebserviceError: if the request is unsuccessful

        """
        return self.service.get_results_list(
                self.get_results_path(),
                self.to_query_params(),
                rowformat,
                self.views)

    def children(self):
        """
        Returns the child objects of the query
        ======================================

        This method is used during the serialisation of queries
        to xml. It is unlikely you will need access to this as a whole.
        Consider using "path_descriptions", "joins", "constraints" instead

        @see: Query.path_descriptions
        @see: Query.joins
        @see: Query.constraints

        @return: the child element of this query
        @rtype: list
        """
        return sum([self.path_descriptions, self.joins, self.constraints], [])
        
    def to_query_params(self):
        """
        Returns the parameters to be passed to the webservice
        =====================================================

        The query is responsible for producing its own query 
        parameters. These consist simply of:
         - query: the xml representation of the query

        @rtype: dict

        """
        xml = self.to_xml()
        params = {'query' : xml }
        return params
        
    def to_Node(self):
        """
        Returns a DOM node representing the query
        =========================================

        This is an intermediate step in the creation of the 
        xml serialised version of the query. You probably 
        won't need to call this directly.

        @rtype: xml.minidom.Node
        """
        impl  = getDOMImplementation()
        doc   = impl.createDocument(None, "query", None)
        query = doc.documentElement
        
        query.setAttribute('name', self.name)
        query.setAttribute('model', self.model.name)
        query.setAttribute('view', ' '.join(self.views))
        query.setAttribute('sortOrder', str(self.get_sort_order()))
        query.setAttribute('longDescription', self.description)
        if len(self.coded_constraints) > 1:
            query.setAttribute('constraintLogic', str(self.get_logic()))

        for c in self.children():
            element = doc.createElement(c.child_type)
            for name, value in c.to_dict().items():
                if isinstance(value, (set, list)):
                    for v in value:
                        subelement = doc.createElement(name)
                        text = doc.createTextNode(v)
                        subelement.appendChild(text)
                        element.appendChild(subelement)
                else:
                    element.setAttribute(name, value)
            query.appendChild(element)
        return query

    def to_xml(self):
        """
        Return an XML serialisation of the query
        ========================================

        This method serialises the current state of the query to an 
        xml string, suitable for storing, or sending over the 
        internet to the webservice.

        @return: the serialised xml string
        @rtype: string
        """
        n = self.to_Node()
        return n.toxml()
    def to_formatted_xml(self):
        """
        Return a readable XML serialisation of the query
        ================================================

        This method serialises the current state of the query to an 
        xml string, suitable for storing, or sending over the 
        internet to the webservice, only more readably.

        @return: the serialised xml string
        @rtype: string
        """
        n = self.to_Node()
        return n.toprettyxml()

    def clone(self):
        """
        Performs a deep clone
        =====================

        This method will produce a clone that is independent, 
        and can be altered without affecting the original, 
        but starts off with the exact same state as it.

        The only shared elements should be the model
        and the service, which are shared by all queries 
        that refer to the same webservice.

        @return: same class as caller
        """
        newobj = self.__class__(self.model)
        for attr in ["joins", "views", "_sort_order_list", "_logic", "path_descriptions", "constraint_dict"]:
            setattr(newobj, attr, deepcopy(getattr(self, attr)))

        for attr in ["name", "description", "service", "do_verification", "constraint_factory"]:
            setattr(newobj, attr, getattr(self, attr))
        return newobj

class Template(Query):
    """
    A Class representing a predefined query
    =======================================

    Templates are ways of saving queries 
    and allowing others to run them 
    simply. They are the main interface
    to querying in the webapp

    SYNOPSIS
    --------

    example::

      service = Service("http://www.flymine.org/query/service")
      template = service.get_template("Gene_Pathways")
      for row in template.results(A={"value":"eve"}):
        process_row(row)
        ...

    A template is a subclass of query that comes predefined. They 
    are typically retrieved from the webservice and run by specifying
    the values for their existing constraints. They are a concise 
    and powerful way of running queries in the webapp.

    Being subclasses of query, everything is true of them that is true
    of a query. They are just less work, as you don't have to design each
    one. Also, you can store your own templates in the web-app, and then
    access them as a private webservice method, from anywhere, making them
    a kind of query in the cloud - for this you will need to authenticate
    by providing log in details to the service.

    The most significant difference is how constraint values are specified
    for each set of results.
        
    @see: L{Template.results}

    """
    def __init__(self, *args, **kwargs):
        """
        Constructor
        ===========

        Instantiation is identical that of queries. As with queries,
        these are best obtained from the intermine.webservice.Service
        factory methods. 
        
        @see: L{intermine.webservice.Service.get_template}
        """
        super(Template, self).__init__(*args, **kwargs)
        self.constraint_factory = constraints.TemplateConstraintFactory()
    @property
    def editable_constraints(self):
        """
        Return the list of constraints you can edit
        ===========================================

        Template.editable_constraints -> list(intermine.constraints.Constraint)

        Templates have a concept of editable constraints, which
        is a way of hiding complexity from users. An underlying query may have 
        five constraints, but only expose the one that is actually
        interesting. This property returns this subset of constraints
        that have the editable flag set to true.
        """
        isEditable = lambda x: x.editable
        return filter(isEditable, self.constraints)

    def to_query_params(self):
        """
        Returns the query parameters needed for the webservice
        ======================================================

        Template.to_query_params() -> dict(string, string)

        Overrides the method of the same name in query to provide the 
        parameters needed by the templates results service. These
        are slightly more complex:
            - name: The template's name
            - for each constraint: (where [i] is an integer incremented for each constraint)
                - constraint[i]: the path
                - op[i]:         the operator
                - value[i]:      the value
                - code[i]:       the code
                - extra[i]:      the extra value for ternary constraints (optional)

        """
        p = {'name' : self.name}
        i = 1
        for c in self.editable_constraints:
            if not c.switched_on: next
            for k, v in c.to_dict().items():
                k = "extra" if k == "extraValue" else k
                k = "constraint" if k == "path" else k
                p[k + str(i)] = v
            i += 1
        return p

    def get_results_path(self):
        """
        Returns the path section pointing to the REST resource
        ======================================================

        Template.get_results_path() S{->} str

        Internally, this just calls a constant property
        in intermine.service.Service

        This overrides the method of the same name in Query
        
        @return: the path to the REST resource
        @rtype: string
        """
        return self.service.TEMPLATEQUERY_PATH

    def get_adjusted_template(self, con_values):
        """
        Gets a template to run 
        ======================

        Template.get_adjusted_template(con_values) S{->} Template

        When templates are run, they are first cloned, and their 
        values are changed to those desired. This leaves the original 
        template unchanged so it can be run again with different
        values. This method does the cloning and changing of constraint
        values

        @raise ConstraintError: if the constraint values specify values for a non-editable constraint.

        @rtype: L{Template}
        """
        clone = self.clone()
        for code, options in con_values.items():
            con = clone.get_constraint(code)
            if not con.editable:
                raise ConstraintError("There is a constraint '" + code 
                                       + "' on this query, but it is not editable")
            for key, value in options.items():
                setattr(con, key, value)
        return clone

    def results(self, row="list", **con_values):
        """
        Get an iterator over result rows
        ================================

        This method returns the same values with the 
        same options as the method of the same name in 
        Query (see intermine.query.Query). The main difference in in the
        arguments. 

        The template result methods also accept a key-word pair
        set of arguments that are used to supply values
        to the editable constraints. eg::

          template.results(
            A = {"value": "eve"},
            B = {"op": ">", "value": 5000}
          )

        The keys should be codes for editable constraints (you can inspect these
        with Template.editable_constraints) and the values should be a dictionary
        of constraint properties to replace. You can replace the values for
        "op" (operator), "value", and "extra_value" and "values" in the case of 
        ternary and multi constraints.

        @rtype: L{intermine.webservice.ResultIterator}
        """
        clone = self.get_adjusted_template(con_values)
        return super(Template, clone).results(row)

    def get_results_list(self, row="list", **con_values):
        """
        Get a list of result rows
        =========================

        This method performs the same as the method of the 
        same name in Query, and it shares the semantics of 
        Template.results().

        @see: L{intermine.query.Query.get_results_list}
        @see: L{intermine.query.Template.results}

        @rtype: list

        """
        clone = self.get_adjusted_template(con_values)
        return super(Template, clone).get_results_list(row)

class QueryError(ReadableException):
    pass

class ConstraintError(QueryError):
    pass

class QueryParseError(QueryError):
    pass

