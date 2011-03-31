import re
import string
from .pathfeatures import PathFeature, PATH_PATTERN
from .util import ReadableException

class Constraint(PathFeature):
    child_type = "constraint"

class LogicNode(object):
    def __add__(self, other):
        if not isinstance(other, LogicNode):
            return NotImplemented
        else:
            return LogicGroup(self, 'AND', other)
    def __and__(self, other):
        if not isinstance(other, LogicNode):
            return NotImplemented
        else:
            return LogicGroup(self, 'AND', other)
    def __or__(self, other):
        if not isinstance(other, LogicNode):
            return NotImplemented
        else:
            return LogicGroup(self, 'OR', other)

class LogicGroup(LogicNode):
    LEGAL_OPS = frozenset(['AND', 'OR'])
    def __init__(self, left, op, right, parent=None):
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
        return '<' + self.__class__.__name__ + ': ' + str(self) + '>'
    def __str__(self):
        core = ' '.join(map(str, [self.left, self.op.lower(), self.right]))
        return '(' + core + ')' if self.parent and self.op != self.parent.op else core
    def get_codes(self):
        codes = []
        for node in [self.left, self.right]:
            if isinstance(node, LogicGroup):
                codes.extend(node.get_codes())
            else:
                codes.append(node.code)
        return codes

class LogicParseError(ReadableException):
    pass

class LogicParser(object):

    def __init__(self, query):
        self._query = query

    def get_constraint(self, code):
        return self._query.get_constraint(code) 

    def get_priority(self, op):
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
        def flatten(l): 
            ret = []
            for item in l:
                if isinstance(item, list):
                    ret.extend(item)
                else:
                    ret.append(item)
            return ret
        logic_str = logic_str.upper()
        tokens = re.split("\s+", logic_str)
        tokens = flatten([self.ops[x] if x in self.ops else re.split("\b", x) for x in tokens])
        tokens = flatten([list(x) if re.search("[()]", x) else x for x in tokens])
        self.check_syntax(tokens)
        postfix_tokens = self.infix_to_postfix(tokens)
        abstract_syntax_tree = self.postfix_to_tree(postfix_tokens)
        return abstract_syntax_tree

    def check_syntax(self, infix_tokens):
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
        stack = []
        for token in postfix_tokens:
            if token not in self.ops:
                stack.append(token)
            else:
                op = token
                right = stack.pop()
                left = stack.pop()
                right = right if isinstance(right, LogicGroup) else self.get_constraint(right)
                left = left if isinstance(left, LogicGroup) else self.get_constraint(left)
                stack.append(LogicGroup(left, op, right))
        assert len(stack) == 1, "Tree doesn't have a unique root"
        return stack.pop()

class CodedConstraint(Constraint, LogicNode):
    OPS = set([])
    def __init__(self, path, op, code="A"):
        if op not in self.OPS:
            raise TypeError(op + " not in " + str(self.OPS))
        self.op = op
        self.code = code
        super(CodedConstraint, self).__init__(path)
    def __str__(self):
        return self.code
    def to_string(self):
        s = super(CodedConstraint, self).to_string()
        return " ".join([s, self.op])
    def to_dict(self):
        d = super(CodedConstraint, self).to_dict()
        d.update(op=self.op, code=self.code)
        return d
    
class UnaryConstraint(CodedConstraint):
    OPS = set(['IS NULL', 'IS NOT NULL'])

class BinaryConstraint(CodedConstraint):
    OPS = set(['=', '!=', '<', '>', '<=', '>=', 'LIKE', 'NOT LIKE'])
    def __init__(self, path, op, value, code="A"):
        self.value = value
        super(BinaryConstraint, self).__init__(path, op, code)

    def to_string(self):
        s = super(BinaryConstraint, self).to_string()
        return " ".join([s, str(self.value)])
    def to_dict(self):
        d = super(BinaryConstraint, self).to_dict()
        d.update(value=str(self.value))
        return d

class ListConstraint(CodedConstraint):
    OPS = set(['IN', 'NOT IN'])
    def __init__(self, path, op, list_name, code="A"):
        self.list_name = list_name
        super(ListConstraint, self).__init__(path, op, code)

    def to_string(self):
        s = super(ListConstraint, self).to_string()
        return " ".join([s, str(self.list_name)])
    def to_dict(self):
        d = super(ListConstraint, self).to_dict()
        d.update(value=str(self.list_name))
        return d

class LoopConstraint(CodedConstraint):
    OPS = set(['IS', 'IS NOT'])
    SERIALISED_OPS = {'IS':'=', 'IS NOT':'!='}
    def __init__(self, path, op, loopPath, code="A"):
        self.loopPath = loopPath
        super(LoopConstraint, self).__init__(path, op, code)

    def to_string(self):
        s = super(LoopConstraint, self).to_string()
        return " ".join([s, self.loopPath])
    def to_dict(self):
        d = super(LoopConstraint, self).to_dict()
        d.update(loopPath=self.loopPath, op=self.SERIALISED_OPS[self.op])
        return d
    
class TernaryConstraint(BinaryConstraint):
    OPS = set(['LOOKUP'])
    def __init__(self, path, op, value, extra_value=None, code="A"):
        self.extra_value = extra_value
        super(TernaryConstraint, self).__init__(path, op, value, code)

    def to_string(self):
        s = super(TernaryConstraint, self).to_string()
        if self.extra_value is None:
            return s
        else:
            return " ".join([s, 'IN', self.extra_value])
    def to_dict(self):
        d = super(TernaryConstraint, self).to_dict()
        if self.extra_value is not None:
            d.update(extraValue=self.extra_value)
        return d

class MultiConstraint(CodedConstraint):
    OPS = set(['ONE OF', 'NONE OF'])
    def __init__(self, path, op, values, code="A"):
        if not isinstance(values, list):
            raise TypeError("values must be a list, not " + str(type(values)))
        self.values = values
        super(MultiConstraint, self).__init__(path, op, code)

    def to_string(self):
        s = super(MultiConstraint, self).to_string()
        return ' '.join([s, str(self.values)])
    def to_dict(self):
        d = super(MultiConstraint, self).to_dict()
        d.update(value=self.values)
        return d

class SubClassConstraint(Constraint):
    def __init__(self, path, subclass):
       if not PATH_PATTERN.match(subclass):
            raise TypeError
       self.subclass = subclass
       super(SubClassConstraint, self).__init__(path)
    def to_string(self):
       s = super(SubClassConstraint, self).to_string()
       return s + ' ISA ' + self.subclass
    def to_dict(self):
       d = super(SubClassConstraint, self).to_dict()
       d.update(type=self.subclass) 
       return d


class TemplateConstraint(object):
    REQUIRED = "locked"
    OPTIONAL_ON = "on"
    OPTIONAL_OFF = "off"
    def __init__(self, editable=True, optional="locked"):
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
        return not self.optional

    @property
    def switched_off(self):
        return not self.switched_on

    def get_switchable_status(self):
        if not self.optional:
            return "locked"
        else:
            switch = "on" if self.switched_on else "off"
            return switch

    def to_string(self):
        editable = "editable" if self.editable else "non-editable"
        return '(' + editable + ", " + self.get_switchable_status() + ')'
    def separate_arg_sets(self, args):
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
        return(UnaryConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateBinaryConstraint(BinaryConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        BinaryConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        return(BinaryConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateListConstraint(ListConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        ListConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        return(ListConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateLoopConstraint(LoopConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        LoopConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        return(LoopConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateTernaryConstraint(TernaryConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        TernaryConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        return(TernaryConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateMultiConstraint(MultiConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        MultiConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        return(MultiConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class TemplateSubClassConstraint(SubClassConstraint, TemplateConstraint):
    def __init__(self, *a, **d):
        (c_args, t_args) = self.separate_arg_sets(d)
        SubClassConstraint.__init__(self, *a, **c_args)
        TemplateConstraint.__init__(self, **t_args)
    def to_string(self):
        return(SubClassConstraint.to_string(self) 
                + " " + TemplateConstraint.to_string(self))

class ConstraintFactory(object):

    CONSTRAINT_CLASSES = set([
        UnaryConstraint, BinaryConstraint, TernaryConstraint, 
        MultiConstraint, SubClassConstraint, LoopConstraint,
        ListConstraint])

    def __init__(self):
        self._codes = iter(string.ascii_uppercase)
    
    def get_next_code(self):
        return self._codes.next()

    def make_constraint(self, *args, **kwargs):
        for CC in self.CONSTRAINT_CLASSES:
            try:
                c = CC(*args, **kwargs)
                if hasattr(c, "code"): c.code = self.get_next_code()
                return c
            except TypeError, e:
                pass
        raise TypeError("No matching constraint class found for " 
            + str(args) + ", " + str(kwargs))
    
class TemplateConstraintFactory(ConstraintFactory):
    CONSTRAINT_CLASSES = set([
        TemplateUnaryConstraint, TemplateBinaryConstraint, 
        TemplateTernaryConstraint, TemplateMultiConstraint,
        TemplateSubClassConstraint, TemplateLoopConstraint, 
        TemplateListConstraint
    ])
