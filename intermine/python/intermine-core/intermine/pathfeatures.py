import re

PATTERN_STR = "^(?:\w+\.)*\w+$"
PATH_PATTERN = re.compile(PATTERN_STR)

class PathFeature(object):
    def __init__(self, path):
        if not PATH_PATTERN.match(path):
            raise TypeError(
                "Path '" + path + "' does not match expected pattern" + PATTERN_STR)
        self.path = path
    def __repr__(self):
        return "<" + self.__class__.__name__ + ": " + self.to_string() + ">"
    def to_string(self):
        return str(self.path)
    def to_dict(self):
        return { 'path' : self.path }
    @property
    def child_type(self):
        raise AttributeError()

class Join(PathFeature):
    valid_join_styles = ['OUTER', 'INNER']
    INNER = "INNER"
    OUTER = "OUTER"
    child_type = 'join'
    def __init__(self, path, style='OUTER'):
        if style.upper() not in Join.valid_join_styles:
            raise TypeError("Unknown join style: " + style)
        self.style = style.upper()
        super(Join, self).__init__(path)
    def to_dict(self):
        d = super(Join, self).to_dict()
        d.update(style=self.style)
        return d
    def __repr__(self):
        return('<' + self.__class__.__name__
                + ' '.join([':', self.path, self.style]) + '>')

class PathDescription(PathFeature):
    child_type = 'pathDescription'
    def __init__(self, path, description):
        self.description = description
        super(PathDescription, self).__init__(path)
    def to_dict(self):
        d = super(PathDescription, self).to_dict()
        d.update(description=self.description)
        return d

class SortOrder(PathFeature):
    ASC = "asc"
    DESC = "desc"
    DIRECTIONS = frozenset(["asc", "desc"])
    def __init__(self, path, order):
        try:
            order = order.lower()
        except:
            pass

        if not order in self.DIRECTIONS:
            raise TypeError("Order must be one of " + str(self.DIRECTIONS)
                + " - not " + order)
        self.order = order
        super(SortOrder, self).__init__(path)
    def __str__(self):
        return self.path + " " + self.order
    def to_string(self):
        return str(self)

class SortOrderList(object):
    """
    A container implementation for holding sort orders
    ==================================================

    This class exists to hold the sort order information for a
    query. It handles appending elements, and the stringification
    of the sort order.
    """
    def __init__(self, *sos):
        self.sort_orders = []
        self.append(*sos)
    def append(self, *sos):
        """
        Add sort order elements to the sort order list.
        ===============================================

        Elements can be provided as a SortOrder object or
        as a tuple of arguments (path, direction).
        """
        for so in sos:
            if isinstance(so, SortOrder):
                self.sort_orders.append(so)
            elif isinstance(so, tuple):
                self.sort_orders.append(SortOrder(*so))
            else:
                raise TypeError(
                        "Sort orders must be either SortOrder instances,"
                        + " or tuples of arguments: I got:" + so + sos)
    def __repr__(self):
        return '<' + self.class__.__name__ + ': [' + str(self) + ']>'
    def __str__(self):
        return " ".join(map(str, self.sort_orders))
    def clear(self):
        self.sort_orders = []
    def is_empty(self):
        return len(self.sort_orders) == 0
    def __len__(self):
        return len(self.sort_orders)
    def next(self):
        return self.sort_orders.next()
    def __iter__(self):
        return iter(self.sort_orders)

