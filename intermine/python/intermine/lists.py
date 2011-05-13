import weakref

# Use core json for 2.6+, simplejson for <=2.5
try:
    import json
except ImportError:
    import simplejson as json

import webservice
from query import Query
import urllib
import codecs

class ListManager(object):

    DEFAULT_LIST_NAME = "my_list_"
    DEFAULT_DESCRIPTION = "List created with Python client library"

    INTERSECTION_PATH = '/lists/intersect/json'
    UNION_PATH = '/lists/union/json'
    DIFFERENCE_PATH = '/lists/diff/json'
    SUBTRACTION_PATH = '/lists/subtract/json'

    def __init__(self, service):
        self.service = weakref.proxy(service)
        self.lists = None
        self._temp_lists = set()

    def refresh_lists(self):
        self.lists = {}
        url = self.service.root + self.service.LIST_PATH
        sock = self.service.opener.open(url)
        data = sock.read()
        sock.close()
        list_info = json.loads(data)
        if not list_info.get("wasSuccessful"):
            raise webservice.WebserviceError(list_info.get("error"))
        for l in list_info["lists"]:
            self.lists[l["name"]] = List(service=self.service, manager=self, **l)

    def get_list(self, name):
        if self.lists is None:
            self.refresh_lists()
        return self.lists.get(name)

    def get_all_lists(self):
        if self.lists is None:
            self.refresh_lists()
        return self.lists.values()

    def get_all_list_names(self):
        if self.lists is None:
            self.refresh_lists()
        return self.lists.keys()

    def get_list_count(self):
        return len(self.get_all_list_names())

    def get_unused_list_name(self):
        list_names = self.get_all_list_names()
        counter = 1
        name = self.DEFAULT_LIST_NAME + str(counter)
        while name in list_names:
            counter += 1
            name = self.DEFAULT_LIST_NAME + str(counter)
        self._temp_lists.add(name)
        return name

    def create_list(self, content, list_type="", name=None, description=None):
        if description is None:
            description = self.DEFAULT_DESCRIPTION

        if name is None:
            name = self.get_unused_list_name()

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
                        uri = content.get_list_upload_uri()
                    except:
                        content = content.to_query()
                        uri = content.get_list_upload_uri()
                    params = content.to_query_params()
                    params["listName"] = name
                    params["description"] = description
                    form = urllib.urlencode(params)
                    resp = self.service.opener.open(uri, form)
                    data = resp.read()
                    resp.close()
                    return self.parse_list_upload_response(data) 

        uri = self.service.root + self.service.LIST_CREATION_PATH
        query_form = {'name': name, 'type': list_type, 'description': description}
        uri += "?" + urllib.urlencode(query_form)
        data = self.service.opener.post_plain_text(uri, ids)
        return self.parse_list_upload_response(data)
        

    def parse_list_upload_response(self, response):
        try:
            response_data = json.loads(response)
        except ValueError:
            raise webservice.WebserviceError("Error parsing response: " + response)
        if not response_data.get("wasSuccessful"):
            raise webservice.WebserviceError(response_data.get("error"))
        self.refresh_lists()
        new_list = self.get_list(response_data["listName"])
        failed_matches = response_data.get("unmatchedIdentifiers")
        new_list._add_failed_matches(failed_matches)
        return new_list

    def delete_lists(self, lists):
        for l in lists:
            if isinstance(l, List):
                name = l.name
            else:
                name = str(l)
            if name not in self.get_all_list_names():
                continue
            uri = self.service.root + self.service.LIST_PATH
            query_form = {'name': name}
            uri += "?" + urllib.urlencode(query_form)
            response = self.service.opener.delete(uri)
            response_data = json.loads(response)
            if not response_data.get("wasSuccessful"):
                raise webservice.WebserviceError(response_data.get("error"))
        self.refresh_lists()

    def delete_temporary_lists(self):
        self.delete_lists(self._temp_lists)
        self._temp_lists = set()

    def intersect(self, lists, name=None, description=None):
        return self._do_operation(self.INTERSECTION_PATH, "Intersection", lists, name, description)

    def union(self, lists, name=None, description=None):
        return self._do_operation(self.UNION_PATH, "Union", lists, name, description)

    def xor(self, lists, name=None, description=None):
        return self._do_operation(self.DIFFERENCE_PATH, "Difference", lists, name, description)

    def subtract(self, lefts, rights, name=None, description=None):
        left_names = self.make_list_names(lefts)
        right_names = self.make_list_names(rights)
        if description is None:
            description = "Subtraction of " + ' and '.join(right_names) + " from " + ' and '.join(left_names)
        if name is None:
            name = self.get_unused_list_name()
        uri = self.service.root + self.SUBTRACTION_PATH
        uri += '?' + urllib.urlencode({
            "name": name,
            "description": description,
            "references": ';'.join(left_names),
            "subtract": ';'.join(right_names)
            })
        resp = self.service.opener.open(uri)
        data = resp.read()
        resp.close()
        return self.parse_list_upload_response(data)

    def _do_operation(self, path, operation, lists, name, description):
        list_names = self.make_list_names(lists)
        if description is None:
            description = operation + " of " + ' and '.join(list_names)
        if name is None:
            name = self.get_unused_list_name()
        uri = self.service.root + path
        uri += '?' + urllib.urlencode({
            "name": name,
            "lists": ';'.join(list_names),
            "description": description
            })
        resp = self.service.opener.open(uri)
        data = resp.read()
        resp.close()
        return self.parse_list_upload_response(data)


    def make_list_names(self, lists):
        list_names = []
        for l in lists:
            try:
                t = l.list_type
                list_names.append(l.name)
            except AttributeError:
                try: 
                    m = l.model
                    list_names.append(self.create_list(l).name)
                except AttributeError:
                    list_names.append(str(l))

        return list_names

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
        except KeyError:
            raise ValueError("Missing argument") 
        self.unmatched_identifiers = set([])

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, new_name):
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
        name = self.name
        if isinstance(appendix, basestring):
            return self._do_append(appendix)
        if isinstance(appendix, List):
            return self._do_append(appendix)
        if isinstance(appendix, Query):
            return self._do_append(appendix)
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
        subtr = self.manager.subtract([self], [other], description=self.description)
        self.delete()
        subtr.name = self.name
        return subtr

