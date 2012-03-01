import re
import string
from intermine.pathfeatures import PathFeature, PATH_PATTERN
from intermine.util import ReadableException

class Constraint(PathFeature):
    """
    A class representing constraints on a query
    ===========================================

    All constraints inherit from this class, which
    simply defines the type of element for the 
    purposes of serialisation.
    """
    child_type = "constraint"

class LogicNode(object):
    """
    A class representing nodes in a logic graph
    ===========================================

    Objects which can be represented as nodes 
    in the AST of a constraint logic graph should
    inherit from this class, which defines 
    methods for overloading built-in operations.
    """

    def __add__(self, other):
        """
        Overloads +
        ===========

        Logic may be defined by using addition to sum
        logic nodes::

            > query.set_logic(con_a + con_b + con_c)
            > str(query.logic)
            ... A and B and C

        """
        if not isinstance(other, LogicNode):
            return NotImplemented
        else:
            return LogicGroup(self, 'AND', other)

    def __and__(self, other):
        """
        Overloads &
        ===========

        Logic may be defined by using the & operator::

            > query.set_logic(con_a & con_b)
            > sr(query.logic)
            ... A and B

        """
        if not isinstance(other, LogicNode):
            return NotImplemented
        else:
            return LogicGroup(self, 'AND', other)

    def __or__(self, other):
        """
        Overloads |
        ===========

        Logic may be defined by using the | operator::

            > query.set_logic(con_a | con_b)
            > str(query.logic)
            ... A or B

        """
        if not isinstance(other, LogicNode):
            return NotImplemented
        else:
            return LogicGroup(self, 'OR', other)

class LogicGroup(LogicNode):
    """
    A logic node that represents two sub-nodes joined in some way
    =============================================================

    A logic group is a logic node with two child nodes, which are
    either connected by AND or by OR logic.
    """

    LEGAL_OPS = frozenset(['AND', 'OR'])

    def __init__(self, left, op, right, parent=None):
        """
        Constructor
        ===========

        Makes a new node composes of two nodes (left and right),
        and some operator.

        Groups may have a reference to their parent.
        """
        if not op in self.LEGAL_OPS:
            raise TypeError(op + " is not a legal logical operation")
        self.parent = parent
        self.left = left
        self.right = right
        self.op = op
        for node in [self.left, self.right]:
            if isinstance(node, LogicGroup):
                node.parent = self

    def __repr__(self):
        """
        Provide a sensible representation of a node
        """
        return '<' + self.__class__.__name__ + ': ' + str(self) + '>'

    def __str__(self):
        """
        Provide a human readable version of the group. The 
        string version should be able to be parsed back into the
        original logic group.
        """
        core = ' '.join(map(str, [self.left, self.op.lower(), self.right]))
        if self.parent and self.op != self.parent.op:
            return '(' + core + ')' 
        else:
            return core

    def get_codes(self):
        """
        Get a list of all constraint codes used in this group.
        """
        codes = []
        for node in [self.left, self.right]:
            if isinstance(node, LogicGroup):
                codes.extend(node.get_codes())
            else:
                codes.append(node.code)
        return codes

class LogicParseError(ReadableException):
    """
    An error representing problems in parsing constraint logic.
    """
    pass

class EmptyLogicError(ValueError):
    """
    An error representing the fact that an the logic string to be parsed was empty
    """
    pass

class LogicParser(object):
    """
    Parses logic strings into logic groups
    ======================================

    Instances of this class are used to parse logic strings into
    abstract syntax trees, and then logic groups. This aims to provide
    robust parsing of logic strings, with the ability to identify syntax 
    errors in such strings.
    """

    def __init__(self, query):
        """
        Constructor
        ===========

        Parsers need access to the query they are parsing for, in
        order to reference the constraints on the query.

        @param query: The parent query object
        @type query: intermine.query.Query
        """
        self._query = query

    def get_constraint(self, code):
        """
        Get the constraint with the given code
        ======================================

        This method fetches the constraint from the
        parent query with the matching code.

        @see: intermine.query.Query.get_constraint
        @rtype: intermine.constraints.CodedConstraint
        """
        return self._query.get_constraint(code) 

    def get_priority(self, op):
        """
        Get the priority for a given operator
        =====================================

        Operators have a specific precedence, from highest
        to lowest:
          - () 
          - AND 
          - OR

        This method returns an integer which can be 
        used to compare operator priorities. 

        @rtype: int
        """
        return {
        "AND": 2,
        "OR" : 1,
        "("  : 3,
        ")"  : 3
        }.get(op)

    ops = {
        "AND" : "AND",
        "&"   : "AND",
        "&&"  : "AND",
        "OR"  : "OR",
        "|"   : "OR",
        "||"  : "OR",
        "("   : "(",
        ")"   : ")"
    }

    def parse(self, logic_str):
        """
        Parse a logic string into an abstract syntax tree
        =================================================

        Takes a string such as "A and B or C and D", and parses it
        into a structure which represents this logic as a binary
        abstract syntax tree. The above string would parse to
        "(A and B) or (C and D)", as AND binds more tightly than OR.

        Note that only singly rooted trees are parsed.

        @param logic_str: The logic defininition as a string
        @type logic_str: string

        @rtype: LogicGroup

        @raise LogicParseError: if there is a syntax error in the logic
        """
        def flatten(l):
            """Flatten out a list which contains both values and sublists"""
            ret = []
            for item in l:
                if isinstance(item, list):
                    ret.extend(item)
                else:
                    ret.append(item)
            return ret
        def canonical(x, d):
            if x in d:
                return d[x]
            else:
                return re.split("\b", x)
        def dedouble(x):
            if re.search("[()]", x):
                return list(x)
            else:
                return x

        logic_str = logic_str.upper()
        tokens = [t for t in re.split("\s+", logic_str) if t]
        if not tokens:
            raise EmptyLogicError()
        tokens = flatten([canonical(x, self.ops) for x in tokens])
        tokens = flatten([dedouble(x) for x in tokens])
        self.check_syntax(tokens)
        postfix_tokens = self.infix_to_postfix(tokens)
        abstract_syntax_tree = self.postfix_to_tree(postfix_tokens)
        return abstract_syntax_tree

    def check_syntax(self, infix_tokens):
        """
        Check the syntax for errors before parsing
        ==========================================

        Syntax is checked before parsing to provide better errors, 
        which should hopefully lead to more informative error messages.

        This checks for:
         - correct operator positions (cannot put two codes next to each other without intervening operators)
         - correct grouping (all brackets are matched, and contain valid expressions)

        @param infix_tokens: The input parsed into a list of tokens.
        @type infix_tokens: iterable

        @raise LogicParseError: if there is a problem.
        """
        need_an_op = False
        need_binary_op_or_closing_bracket = False
        processed = []
        open_brackets = 0
        for token in infix_tokens:
            if token not in self.ops:
                if need_an_op:
                    raise LogicParseError("Expected an operator after: '" + ' '.join(processed) + "'"
                                          + " - but got: '" + token + "'")
                if need_binary_op_or_closing_bracket:
                    raise LogicParseError("Logic grouping error after: '" + ' '.join(processed) + "'"
                                          + " - expected an operator or a closing bracket")

                need_an_op = True
            else:
                need_an_op = False
                if token == "(":
                    if processed and processed[-1] not in self.ops:
                        raise LogicParseError("Logic grouping error after: '" + ' '.join(processed) + "'"
                                          + " - got an unexpeced opening bracket")
                    if need_binary_op_or_closing_bracket:
                        raise LogicParseError("Logic grouping error after: '" + ' '.join(processed) + "'"
                                          + " - expected an operator or a closing bracket")

                    open_brackets += 1
                elif token == ")":
                    need_binary_op_or_closing_bracket = True
                    open_brackets -= 1
                else:
                    need_binary_op_or_closing_bracket = False
            processed.append(token)
        if open_brackets != 0:
            if open_brackets < 0:
                message = "Unmatched closing bracket in: "
            else:
                message = "Unmatched opening bracket in: "
            raise LogicParseError(message + '"' + ' '.join(infix_tokens) + '"')
        
    def infix_to_postfix(self, infix_tokens):
        """
        Convert a list of infix tokens to postfix notation
        ==================================================

        Take in a set of infix tokens and return the set parsed 
        to a postfix sequence.

        @param infix_tokens: The list of tokens
        @type infix_tokens: iterable

        @rtype: list
        """
        stack = []
        postfix_tokens = []
        for token in infix_tokens:
            if token not in self.ops:
                postfix_tokens.append(token)
            else:
                op = token
                if op == "(":
                    stack.append(token)
                elif op == ")":
                    while stack:
                        last_op = stack.pop()
                        if last_op == "(":
                            if stack:
                                previous_op = stack.pop()
                                if previous_op != "(": postfix_tokens.append(previous_op)
                                break
                        else: 
                            postfix_tokens.append(last_op)
                else:
                    while stack and self.get_priority(stack[-1]) <= self.get_priority(op):
                        prev_op = stack.pop()
                        if prev_op != "(": postfix_tokens.append(prev_op)
                    stack.append(op)
        while stack: postfix_tokens.append(stack.pop())
        return postfix_tokens

    def postfix_to_tree(self, postfix_tokens):
        """
        Convert a set of structured tokens to a single LogicGroup
        =========================================================

        Convert a set of tokens in postfix notation to a single
        LogicGroup object.

        @param postfix_tokens: A list of tokens in postfix notation.
        @type postfix_tokens: list

        @rtype: LogicGroup

        @raise AssertionError: is the tree doesn't have a unique root.
        """
        stack = []
        try:
            for token in postfix_tokens:
                if token not in self.ops:
                    stack.append(self.get_constraint(token))
                else:
                    op = token
                    right = stack.pop()
                    left = stack.pop()
                    stack.append(LogicGroup(left, op, right))
            assert len(stack) == 1, "Tree doesn't have a unique root"
            return stack.pop()
        except IndexError:
            raise EmptyLogicError()

class EmptyLogicError (IndexError):
    pass

class CodedConstraint(Constraint, LogicNode):
    """
    A parent class for all constraints that have codes
    ==================================================

    Constraints that have codes are the principal logical 
    filters on queries, and need to be refered to individually
    (hence the codes). They will all have a logical operation they
    embody, and so have a reference to an operator.

    This class is not meant to be instantiated directly, but instead
    inherited from to supply default behaviour.
    """

    OPS = set([])

    def __init__(self, path, op, code="A"):
        """
        Constructor
        ===========

        @param path: The path to constrain
        @type path: string

        @param op: The operation to apply - must be in the OPS set
        @type op: string
        """
        if op not in self.OPS:
            raise TypeError(op + " not in " + str(self.OPS))
        self.op = op
        self.code = code
        super(CodedConstraint, self).__init__(path)

    def get_codes(self):
        return [self.code]

    def __str__(self):
        """
        Stringify to the code they are refered to by.
        """
        return self.code
    def to_string(self):
        """
        Provide a human readable representation of the logic. 
        This method is called by repr.
        """
        s = super(CodedConstraint, self).to_string()
        return " ".join([s, self.op])

    def to_dict(self):
        """
        Return a dict object which can be used to construct a 
        DOM element with the appropriate attributes.
        """
        d = super(CodedConstraint, self).to_dict()
        d.update(op=self.op, code=self.code)
        return d
    
class UnaryConstraint(CodedConstraint):
    """
    Constraints which have just a path and an operator
    ==================================================

    These constraints are simple assertions about the 
    object/value refered to by the path. The set of valid 
    operators is:
     - IS NULL
     - IS NOT NULL

    """
    OPS = set(['IS NULL', 'IS NOT NULL'])

class BinaryConstraint(CodedConstraint):
    """
    Constraints which have an operator and a value
    ==============================================

    These constraints assert a relationship between the
    value represented by the path (it must be a representation
    of a value, ie an Attribute) and another value - ie. the 
    operator takes two parameters.

    In all case the 'left' side of the relationship is the path,
    and the 'right' side is the supplied value.

    Valid operators are:
     - =        (equal to)
     - !=       (not equal to)
     - <        (less than)
     - >        (greater than)
     - <=       (less than or equal to)
     - >=       (greater than or equal to)
     - LIKE     (same as equal to, but with implied wildcards)
     - CONTAINS (same as equal to, but with implied wildcards)
     - NOT LIKE (same as not equal to, but with implied wildcards)

    """
    OPS = set(['=', '!=', '<', '>', '<=', '>=', 'LIKE', 'NOT LIKE', 'CONTAINS'])
    def __init__(self, path, op, value, code="A"):
        """
        Constructor
        ===========

        @param path: The path to constrain
        @type path: string

        @param op: The relationship between the value represented by the path and the value provided (must be a valid operator)
        @type op: string

        @param value: The value to compare the stored value to
        @type value: string or number

        @param code: The code for this constraint (default = "A")
        @type code: string
        """
        self.value = value
        super(BinaryConstraint, self).__init__(path, op, code)

    def to_string(self):
        """
        Provide a human readable representation of the logic. 
        This method is called by repr.
        """
        s = super(BinaryConstraint, self).to_string()
        return " ".join([s, str(self.value)])
    def to_dict(self):
        """
        Return a dict object which can be used to construct a 
        DOM element with the appropriate attributes.
        """
        d = super(BinaryConstraint, self).to_dict()
        d.update(value=str(self.value))
        return d

class ListConstraint(CodedConstraint):
    """
    Constraints which refer to an objects membership of lists
    =========================================================

    These constraints assert a membership relationship between the
    object represented by the path (it must always be an object, ie.
    a Reference or a Class) and a List. Lists are collections of 
    objects in the database which are stored in InterMine 
    datawarehouses. These lists must be set up before the query is run, either
    manually in the webapp or by using the webservice API list 
    upload feature.

    Valid operators are:
     - IN 
     - NOT IN

     """
    OPS = set(['IN', 'NOT IN'])
    def __init__(self, path, op, list_name, code="A"):
        if hasattr(list_name, 'to_query'):
            q = list_name.to_query()
            l = q.service.create_list(q)
            self.list_name = l.name
        elif hasattr(list_name, "name"):
            self.list_name = list_name.name
        else:
            self.list_name = list_name
        super(ListConstraint, self).__init__(path, op, code)

    def to_string(self):
        """
        Provide a human readable representation of the logic. 
        This method is called by repr.
        """
        s = super(ListConstraint, self).to_string()
        return " ".join([s, str(self.list_name)])
    def to_dict(self):
        """
        Return a dict object which can be used to construct a 
        DOM element with the appropriate attributes.
        """
        d = super(ListConstraint, self).to_dict()
        d.update(value=str(self.list_name))
        return d

class LoopConstraint(CodedConstraint):
    """
    Constraints with refer to object identity
    =========================================

    These constraints assert that two paths refer to the same
    object. 

    Valid operators:
     - IS
     - IS NOT

    The operators IS and IS NOT map to the ops "=" and "!=" when they
    are used in XML serialisation.

    """
    OPS = set(['IS', 'IS NOT'])
    SERIALISED_OPS = {'IS':'=', 'IS NOT':'!='}
    def __init__(self, path, op, loopPath, code="A"):
        """
        Constructor
        ===========

        @param path: The path to constrain
        @type path: string

        @param op: The relationship between the path and the path provided (must be a valid operator)
        @type op: string

        @param loopPath: The path to check for identity against
        @type loopPath: string

        @param code: The code for this constraint (default = "A")
        @type code: string
        """
        self.loopPath = loopPath
        super(LoopConstraint, self).__init__(path, op, code)

    def to_string(self):
        """
        Provide a human readable representation of the logic. 
        This method is called by repr.
        """
        s = super(LoopConstraint, self).to_string()
        return " ".join([s, self.loopPath])
    def to_dict(self):
        """
        Return a dict object which can be used to construct a 
        DOM element with the appropriate attributes.
        """
        d = super(LoopConstraint, self).to_dict()
        d.update(loopPath=self.loopPath, op=self.SERIALISED_OPS[self.op])
        return d
    
class TernaryConstraint(BinaryConstraint):
    """
    Constraints for broad, general searching over all fields
    ========================================================

    These constraints request a wide-ranging search for matching
    fields over all aspects of an object, including up to coercion
    from related classes.

    Valid operators:
     - LOOKUP

    To aid disambiguation, Ternary constaints accept an extra_value as 
    well as the main value.
    """
    OPS = set(['LOOKUP'])
    def __init__(self, path, op, value, extra_value=None, code="A"):
        """
        Constructor
        ===========

        @param path: The path to constrain. Here is must be a class, or a reference to a class.
        @type path: string

        @param op: The relationship between the path and the path provided (must be a valid operator)
        @type op: string

        @param value: The value to check other fields against.
        @type value: string

        @param extra_value: A further value for disambiguation. The meaning of this value varies by class
                            and configuration. For example, if the class of the object is Gene, then
                            extra_value will refer to the Organism. 
        @type extra_value: string

        @param code: The code for this constraint (default = "A")
        @type code: string
        """
        self.extra_value = extra_value
        super(TernaryConstraint, self).__init__(path, op, value, code)

    def to_string(self):
        """
        Provide a human readable representation of the logic. 
        This method is called by repr.
        """
        s = super(TernaryConstraint, self).to_string()
        if self.extra_value is None:
            return s
        else:
            return " ".join([s, 'IN', self.extra_value])
    def to_dict(self):
        """
        Return a dict object which can be used to construct a 
        DOM element with the appropriate attributes.
        """
        d = super(TernaryConstraint, self).to_dict()
        if self.extra_value is not None:
            d.update(extraValue=self.extra_value)
        return d

class MultiConstraint(CodedConstraint):
    """
    Constraints for checking membership of a set of values
    ======================================================

    These constraints require the value they constrain to be
    either a member of a set of values, or not a member.

    Valid operators:
     - ONE OF
     - NONE OF

    These constraints are similar in use to List constraints, with
    the following differences:
      - The list in this case is a defined set of values that is passed
        along with the query itself, rather than anything stored
        independently on a server.
      - The object of the constaint is the value of an attribute, rather
        than an object's identity.
    """
    OPS = set(['ONE OF', 'NONE OF'])
    def __init__(self, path, op, values, code="A"):
        """
        Constructor
        ===========

        @param path: The path to constrain. Here it must be an attribute of some object.
        @type path: string

        @param op: The relationship between the path and the path provided (must be a valid operator)
        @type op: string

        @param values: The set of values which the object of the constraint either must or must not belong to.
        @type values: set or list

        @param code: The code for this constraint (default = "A")
        @type code: string
        """
        if not isinstance(values, (set, list)):
            raise TypeError("values must be a set or a list, not " + str(type(values)))
        self.values = values
        super(MultiConstraint, self).__init__(path, op, code)

    def to_string(self):
        """
        Provide a human readable representation of the logic. 
        This method is called by repr.
        """
        s = super(MultiConstraint, self).to_string()
        return ' '.join([s, str(self.values)])
    def to_dict(self):
        """
        Return a dict object which can be used to construct a 
        DOM element with the appropriate attributes.
        """
        d = super(MultiConstraint, self).to_dict()
        d.update(value=self.values)
        return d

class SubClassConstraint(Constraint):
    """
    Constraints on the class of a reference
    =======================================

    If an object has a reference X to another object of type A, 
    and type B extends type A, then any object of type B may be 
    the value of the reference X. If you only want to see X's
    which are B's, this may be achieved with subclass constraints, 
    which allow the type of an object to be limited to one of the 
    subclasses (at any depth) of the class type required 
    by the attribute.

    These constraints do not use operators. Since they cannot be 
    conditional (eg. "A is a B or A is a C" would not be possible
    in an InterMine query), they do not have codes 
    and cannot be referenced in logic expressions.
    """
    def __init__(self, path, subclass):
        """
        Constructor
        ===========

        @param path: The path to constrain. This must refer to a class or a reference to a class.
        @type path: str

        @param subclass: The class to subclass the path to. This must be a simple class name (not a dotted name)
        @type subclass: str
        """
        if not PATH_PATTERN.match(subclass):
             raise TypeError
        self.subclass = subclass
        super(SubClassConstraint, self).__init__(path)
    def to_string(self):
       """
       Provide a human readable representation of the logic. 
       This method is called by repr.
       """
       s = super(SubClassConstraint, self).to_string()
       return s + ' ISA ' + self.subclass
    def to_dict(self):
       """
       Return a dict object which can be used to construct a 
       DOM element with the appropriate attributes.
       """
       d = super(SubClassConstraint, self).to_dict()
       d.update(type=self.subclass) 
       return d


class TemplateConstraint(object):
    """
    A mixin to supply the behaviour and state of constraints on templates
    =====================================================================

    Constraints on templates can also be designated as "on", "off" or "locked", which refers
    to whether they are active or not. Inactive constraints are still configured, but behave
    as if absent for the purpose of results. In addition, template constraints can be
    editable or not. Only values for editable constraints can be provided when requesting results,
    and only constraints that can participate in logic expressions can be editable.
    """
    REQUIRED = "locked"
    OPTIONAL_ON = "on"
    OPTIONAL_OFF = "off"
    def __init__(self, editable=True, optional="locked"):
        """
        Constructor
        ===========

        @param editable: Whether or not this constraint should accept new values.
        @type editable: bool
        
        @param optional: Whether a value for this constraint must be provided when running.
        @type optional: "locked", "on" or "off"
        """
        self.editable = editable
        if optional == TemplateConstraint.REQUIRED:
            self.optional = False
            self.switched_on = True
        else:
            self.optional = True
            if optional == TemplateConstraint.OPTIONAL_ON:
                self.switched_on = True
            elif optional == TemplateConstraint.OPTIONAL_OFF:
                self.switched_on = False
            else:
                raise TypeError("Bad value for optional")

    @property
    def required(self):
        """
        True if a value must be provided for this constraint.

        @rtype: bool
        """
        return not self.optional

    @property
    def switched_off(self):
        """
        True if this constraint is currently inactive.

        @rtype: bool
        """
        return not self.switched_on

    def get_switchable_status(self):
        """
        Returns either "locked", "on" or "off".
        """
        if not self.optional:
            return "locked"
        else:
            if self.switched_on:
                return "on"
            else:
                return "off"
    
    def switch_on(self):
        """
        Make sure this constraint is active
        ===================================

        @raise ValueError: if the constraint is not editable and optional
        """
        if self.editable and self.optional:
            self.switched_on = True
        else:
            raise ValueError, "This constraint is not switchable"

    def switch_off(self):
        """
        Make sure this constraint is inactive
        =====================================

        @raise ValueError: if the constraint is not editable and optional
        """
        if self.editable and self.optional:
            self.switched_on = False
        else:
            raise ValueError, "This constraint is not switchable"

    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        if self.editable:
            editable = "editable" 
        else:
            editable = "non-editable"
        return '(' + editable + ", " + self.get_switchable_status() + ')'
    def separate_arg_sets(self, args):
        """
        A static function to use when building template constraints. 
        ============================================================

        dict -> (dict, dict)

        Splits a dictionary of arguments into two separate dictionaries, one with
        arguments for the main constraint, and one with arguments for the template
        portion of the behaviour
        """
        c_args = {}
        t_args = {}
        for k, v in args.items():
            if k == "editable": 
                t_args[k] = v == "true"
            elif k == "optional": 
                t_args[k] = v
            else:
                c_args[k] = v
        return (c_args, t_args)

class TemplateUnaryConstraint(UnaryConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        UnaryConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(UnaryConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateBinaryConstraint(BinaryConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        BinaryConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(BinaryConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateListConstraint(ListConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        ListConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(ListConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateLoopConstraint(LoopConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        LoopConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(LoopConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateTernaryConstraint(TernaryConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        TernaryConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(TernaryConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateMultiConstraint(MultiConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        MultiConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(MultiConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateSubClassConstraint(SubClassConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        SubClassConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        """
        Provide a template specific human readable representation of the 
        constraint. This method is called by repr.
        """
        return(SubClassConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class ConstraintFactory(object):
    """
    A factory for creating constraints from a set of arguments.
    ===========================================================

    A constraint factory is responsible for finding an appropriate
    constraint class for the given arguments and instantiating the 
    constraint.
    """
    CONSTRAINT_CLASSES = set([
        UnaryConstraint, BinaryConstraint, TernaryConstraint, 
        MultiConstraint, SubClassConstraint, LoopConstraint,
        ListConstraint])

    def __init__(self):
        """
        Constructor
        ===========

        Creates a new ConstraintFactory
        """
        self._codes = iter(string.ascii_uppercase)
    
    def get_next_code(self):
        """
        Return the available constraint code.

        @return: A single uppercase character
        @rtype: str
        """
        return self._codes.next()

    def make_constraint(self, *args, **kwargs):
        """
        Create a constraint from a set of arguments.
        ============================================

        Finds a suitable constraint class, and instantiates it.

        @rtype: Constraint
        """
        for CC in self.CONSTRAINT_CLASSES:
            try:
                c = CC(*args, **kwargs)
                if hasattr(c, "code") and c.code == "A":
                    c.code = self.get_next_code()
                return c
            except TypeError, e:
                pass
        raise TypeError("No matching constraint class found for " 
            + str(args) + ", " + str(kwargs))
    
class TemplateConstraintFactory(ConstraintFactory):
    """
    A factory for creating constraints with template specific characteristics.
    ==========================================================================

    A constraint factory is responsible for finding an appropriate
    constraint class for the given arguments and instantiating the 
    constraint. TemplateConstraintFactories make constraints with the 
    extra set of TemplateConstraint qualities.
    """
    CONSTRAINT_CLASSES = set([
        TemplateUnaryConstraint, TemplateBinaryConstraint, 
        TemplateTernaryConstraint, TemplateMultiConstraint,
        TemplateSubClassConstraint, TemplateLoopConstraint, 
        TemplateListConstraint
    ])
