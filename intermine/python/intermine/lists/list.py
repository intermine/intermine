import weakref

class List(object):

    def __init__(self, **args):
        try: 
            self.service = args["service"]
            self.manager = weakref.proxy(args["manager"])
            self._name = args["name"]
            self.title = args["title"]
            self.description = args.get("description")
            self.list_type = args["type"]
            self.size = int(args["size"])
            self.date_created = args.get("dateCreated")
            self.is_authorized = args.get("authorized")
            if self.is_authorized is None:
                self.is_authorized = True
            tags = args["tags"] if "tags" in args else []
            self.tags = frozenset(tags)
        except KeyError:
            raise ValueError("Missing argument") 
        self.unmatched_identifiers = set([])

    def get_name(self):
        return self._name

    def set_name(self, new_name):
        if self._name == new_name:
            return
        uri = self.service.root + self.service.LIST_RENAME_PATH
        params = {
            "oldname": self._name,
            "newname": new_name
        }
        uri += "?" + urllib.urlencode(params)
        resp = self.service.opener.open(uri)
        data = resp.read()
        resp.close()
        new_list = self.manager.parse_list_upload_response(data)
        self._name = new_name

    def del_name(self):
        raise AttributeError("List names cannot be deleted, only changed")

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
        self.manager.delete_lists([self])

    def to_query(self):
        q = self.service.new_query()
        q.add_view(self.list_type + ".id")
        q.add_constraint(self.list_type, "IN", self.name)
        return q

    def to_attribute_query(self):
        q = self.to_query()
        attributes = q.model.get_class(self.list_type).attributes
        q.clear_view()
        q.add_view(map(lambda x: self.list_type + "." + x.name, attributes))
        return q

    def __and__(self, other):
        return self.manager.intersect([self, other])

    def __iand__(self, other):
        intersection = self.manager.intersect([self, other], description=self.description)
        self.delete()
        intersection.name = self.name
        return intersection

    def __or__(self, other):
        return self.manager.union([self, other])

    def __add__(self, other):
        return self.manager.union([self, other])

    def __iadd__(self, other):
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
                    try:
                        uri = content.get_list_append_uri()
                    except:
                        content = content.to_query()
                        uri = content.get_list_append_uri()
                    params = content.to_query_params()
                    params["listName"] = name
                    params["path"] = None
                    form = urllib.urlencode(params)
                    resp = self.service.opener.open(uri, form)
                    data = resp.read()

        if data is None:
            uri = self.service.root + self.service.LIST_APPENDING_PATH
            query_form = {'name': name}
            uri += "?" + urllib.urlencode(query_form)
            data = self.service.opener.post_plain_text(uri, ids)

        new_list = self.manager.parse_list_upload_response(data)
        self.unmatched_identifiers.update(new_list.unmatched_identifiers)
        self.size = new_list.size
        return self

    def append(self, appendix):
        try:
            return self._do_append(self.manager.union(appendix))
        except:
            return self._do_append(appendix)

    def __xor__(self, other):
        return self.manager.xor([self, other])

    def __ixor__(self, other):
        diff = self.manager.xor([self, other], description=self.description)
        self.delete()
        diff.name = self.name
        return diff

    def __sub__(self, other):
        return self.manager.subtract([self], [other])

    def __isub__(self, other):
        subtr = self.manager.subtract([self], [other], description=self.description, tags=self.tags)
        self.delete()
        subtr.name = self.name
        return subtr

