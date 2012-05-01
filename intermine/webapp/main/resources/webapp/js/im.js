if (typeof intermine == "undefined") {
    intermine = {};
}
if (typeof __ == "undefined") {
    __ = function(x) {return _(x).chain()};
}

_.extend(intermine, (function() {
    var List = function(properties, service) {

        _(this).extend(properties);
        this.service = service;
        this.dateCreated = this.dateCreated ? new Date(this.dateCreated) : null;

        var isFolder = function(t) {
            return t.substr(0, t.indexOf(":")) === '__folder__';
        };
        var getFolderName = function(t) {
            return t.substr(t.indexOf(":") + 1);
        };

        this.folders = __(this.tags).filter(isFolder)
                                    .map(getFolderName)
                                    .value();
        
        this.hasTag = function(t) {
            return _(this.tags).include(t);
        };

        this.del = function(cb) {
            cb = cb || function() {};
            return this.service.makeRequest("lists", 
                {name: this.name}, cb, "DELETE");
        };

        this.contents = function(cb) {
            cb = cb || function() {};
            var query = {select: ["*"], from: this.type, where: {}};
            query.where[this.type] = {IN: this.name};
            return this.service.query(query, function(q) {
                q.records(cb);
            });
        };

        this.enrichment = function(data, cb) {
            data.list = this.name;
            return this.service.enrichment(data, cb);
        };
    };

    return {"List": List};
})());
        
if (typeof intermine == "undefined") {
    intermine = {};
}

if (typeof __ == "undefined") {
    __ = function(x) {return _(x).chain()};
}

_.extend(intermine, (function() {

    var PathInfo = function(info) {
        _(this).extend(info);
        this.end = _(this.descriptors).last();
    };
    PathInfo.prototype.constructor = PathInfo;

    PathInfo.prototype.isRoot = function() {
        return this.descriptors.length == 0;
    };

    PathInfo.prototype.getEndClass = function() {
        if (this.isRoot()) {
            return this.root;
        }
        if (this.isClass()) {
            return this.model.classes[this.end.referencedType];
        }
        return null;
    };

    PathInfo.prototype.getParent = function() {
        if (this.isRoot()) {
            throw "Root paths do not have a parent";
        }
        var data = _.extend({}, this, {descriptors: _(this.descriptors).initial()});
        return new PathInfo(data);
    };

    PathInfo.prototype.append = function(attr) {
        var type = this.getType();
        if (_(attr).isString()) {
            attr = type.fields[attr];
        }
        var data = _.extend({}, this, {descriptors: this.descriptors.concat([attr])});
        return new PathInfo(data);
    };

    PathInfo.prototype.isa = function(clazz) {
        var className = (clazz.name) ? clazz.name : clazz + "";
        var type = this.getType();
        var ancestors;
        if (className === type.name) {
            return true;
        } else {
            ancestors = this.model.getAncestorsOf(type);
            return _(ancestors).include(className);
        }
    };

    PathInfo.prototype.getChildNodes = function() {
        var cls, flds, childNodes, i, l;
        childNodes = [];
        var self = this;
        if (!this.isAttribute()) {
            cls = this.getEndClass();
            flds = cls.fields;
            _.each(flds, function(fld, name) {
                childNodes.push(self.append(fld));
            });
        } 
        return childNodes;
    };

    PathInfo.prototype.toPathString = function() {
        var str = this.root.name;
        return _(this.descriptors).reduce(function(a, b) {return a + "." + b.name}, this.root.name);
    };

    PathInfo.prototype.isAttribute = function() {
        if (this.isRoot()) {
            return false;
        }
        return this.end && !this.end.referencedType;
    };

    PathInfo.prototype.isClass = function() {
        if (this.isRoot()) {
            return true;
        }
        return this.end && this.end.referencedType;
    };

    PathInfo.prototype.containsCollection = function() {
        if (this.isRoot()) {
            return false;
        }
        return _(this.descriptors).any(function(d) {
            return d.isCollection;
        });
    };

    var Table = function(o) {
        _(this).extend(o);
        _(this.collections).each(function(coll) {
            coll.isCollection = true;
        });
        this.fields = _({}).extend(this.attributes, this.references, this.collections);
        this.allReferences = _({}).extend(this.references, this.collections);
    };

    // TODO: write unit tests.
    /**
        * Get the type of an attribute path. If the path represents a class or a reference, 
        * the class itself is returned, otherwise the name of the attribute type is returned, 
        * minus any "java.lang." prefix.
        *
        * @param path The path to get the type of
        * @return A class-descriptor, or an attribute type name.
        */
    PathInfo.prototype.getType = function() {
        return this.getEndClass() || this.end.type.replace(/java\.lang\./, "");
    };

    Table.prototype = {
        constructor: Table,
    };

    var Model = function(model) {
        _(this).extend(model);

        // Promote classes to tables.
        var classes = this.classes;
        _(classes).each(function(cd, name) {
            classes[name] = new Table(cd);
        });

    };

    Model.prototype.constructor = Model;

    /**
    * Get the ClassDescriptor for a path. If the path is a root-path, it 
    * returns the class descriptor for the class named, otherwise it returns 
    * the class the last part resolves to. If the last part is an attribute, this
    * function returns "undefined".
    *
    * @param path The path to resolve.
    * @return A class descriptor object, or undefined.
    */
    Model.prototype.getCdForPath = function(path) {
        var parts = path.split(".");
        var cd = this.classes[parts.shift()];
        return _(parts).reduce(_(function (memo, fieldName) {
            var fields = _({}).extend(
                memo.attributes, memo.references, memo.collections);
            return this.classes[fields[fieldName].referencedType];
        }).bind(this), cd);
    };

    // TODO: write unit tests
    /**
        * Get an object describing the path defined by the arguments.
        *
        * @param path The path to be described.
        * @param subclasses An object mapping path {Str} -> type {Str}
        */
    Model.prototype.getPathInfo = function(path, subclasses) {
        var self = this;
        subclasses = subclasses || {};
        var pathInfo = {};
        var parts = path.split(".");
        var cd = this.classes[parts.shift()];
        var keyPath = cd.name;
        pathInfo.root = cd;
        pathInfo.model = this;
        pathInfo.descriptors = _(parts).map(function(fieldName) {
            var fields = _({}).extend(cd.attributes, cd.references, cd.collections);
            if (!fields[fieldName]) {
                cd = self.classes[subclasses[keyPath]];
                fields = _({}).extend(cd.attributes, cd.references, cd.collections);
            }
            var fd = fields[fieldName];
            cd = fd.referencedType ? self.classes[fd.referencedType] : null;
            return fd;
        });
        return new PathInfo(pathInfo);
    };



    // TODO: write unit tests.
    // TODO - move all uses to PathInfo
    /**
        * Determine if there are any collections mentioned in the given path. 
        * eg: 
        *   Department.employees.name -> true
        *   Department.company.name -> false
        *
        * @param path {String} The path to examine.
        * @return {Boolean} Whether or not there is any collection in the path.
        */
    Model.prototype.hasCollection = function(path) {
        var paths = []
            ,parts = path.split(".")
            ,bit, parent, cd;
        while (bit = parts.pop()) {
            parent = parts.join(".");
            if ((parent) && (cd = this.getCdForPath(parent))) {
                if (cd.collections[bit]) {
                    return true;
                }
            }
        }
        return false;
    };

    var _subclass_map = {};

    /**
        * Return the subclasses of a given class. The subclasses of a class
        * includes the class itself, and is thus equivalent to 
        * 'isAssignableTo' in java.
        */
    Model.prototype.getSubclassesOf = function(cls) {
        var self = this;
        if (cls in _subclass_map) {
            return _subclass_map[cls];
        }
        var ret = [cls];
        _(this.classes).each(function(c) {
            if (_(c["extends"]).include(cls)) {
                ret = ret.concat(self.getSubclassesOf(c.name));
            }
        });
        _subclass_map[cls] = ret;
        return ret;
    };

    /**
    * Get the full ancestry of a particular class.
    *
    * The returned ancestry never includes the root InterMineObject base class.
    */
    Model.prototype.getAncestorsOf = function(clazz) {
        clazz = (clazz && clazz.name) ? clazz : this.classes[clazz + ""];
        var ancestors = clazz["extends"].slice();
        _(ancestors).each(_(function(a) {
            if (!a.match(/InterMineObject$/)) {
                ancestors = _.union(ancestors, this.getAncestorsOf(a));
            }
        }).bind(this));
        return ancestors;
    }


    /**
    * Return the common type of two model classes, or null if there isn't one.
    */
    Model.prototype.findCommonTypeOf = function(classA, classB) {
        if (classB == null || classA == null || classA == classB) {
            return classA;
        }
        var allAncestorsOfA = this.getAncestorsOf(classA);
        var allAncestorsOfB = this.getAncestorsOf(classB);
        // If one is a superclass of the other, return it.
        if (_(allAncestorsOfA).include(classB)) {
            return classB;
        }
        if (_(allAncestorsOfB).include(classA)) {
            return classA;
        }
        // Return the first common ancestor

        return _.intersection(allAncestorsOfA, allAncestorsOfB).shift();
    };

    /**
    * Return the common type of 0 or more model classes, or null if there is none.
    *
    * @param model The data model for this service.
    * @classes {String[]} classes the model classes to try and get a common type of.
    */
    Model.prototype.findCommonTypeOfMultipleClasses = function(classes) {
        return _.reduce(classes, _(this.findCommonTypeOf).bind(this), classes.pop());
    };
    Model.NUMERIC_TYPES = ["int", "Integer", "double", "Double", "float", "Float"];
    Model.INTEGRAL_TYPES = ["int", "Integer"]
    Model.BOOLEAN_TYPES = ["boolean", "Boolean"];

    return {"Model": Model};
})());

if (typeof intermine == "undefined") {
    intermine = {};
}
if (typeof __ == "undefined") {
    __ = function(x) {return _(x).chain()};
}
if (typeof console == "undefined") {
    console = {log: function() {}}
}

_.extend(intermine, (function() {
    var log = _(console.log).bind(console);

    var Query = function(properties, service) {
        
        var adjustPath, constructor;

        var JOIN_STYLES = ["INNER", "OUTER"];
        var NULL_OPS = ["IS NULL", "IS NOT NULL"];
        var OP_DICT  = {
            "=" : "=",
            "==": "=",
            "eq": "=",
            "!=": "!=",
            "ne": "!=",
            ">" : ">",
            "gt" : ">",
            ">=": ">=",
            "ge": ">=",
            "<": "<",
            "lt": "<",
            "<=": "<=",
            "le": "<=",
            "contains": "CONTAINS",
            "like": "LIKE", 
            "lookup": "LOOKUP",
            "IS NULL": "IS NULL",
            "is null": "IS NULL",
            "IS NOT NULL": "IS NOT NULL",
            "is not null": "IS NOT NULL",
            "ONE OF": "ONE OF",
            "one of": "ONE OF",
            "in": "IN",
            "not in": "IN",
            "IN": "IN",
            "NOT IN": "NOT IN"
        };

        /**
         * Allow others to listed to events on this query.
         *
         * Straight copy of Backbone events.
         */
        this.on = function(events, callback, context) {
            var ev;
            events = events.split(/\s+/);
            var calls = this._callbacks || (this._callbacks = {});
            while (ev = events.shift()) {
                var list = calls[ev] || (calls[ev] = {});
                var tail = list.tail || (list.tail = list.next = {});
                tail.callback = callback;
                tail.context = context;
                list.tail = tail.next = {};
            }

            return this;
        }
        
        this.bind = this.on;

        // Trigger an event, firing all bound callbacks. Callbacks are passed the
        // same arguments as `trigger` is, apart from the event name.
        // Listening for `"all"` passes the true event name as the first argument.
        this.trigger = function(events) {
            var event, node, calls, tail, args, all, rest;
            if (!(calls = this._callbacks)) return this;
            all = calls['all'];
            (events = events.split(/\s+/)).push(null);
            // Save references to the current heads & tails.
            while (event = events.shift()) {
                if (all) events.push({next: all.next, tail: all.tail, event: event});
                if (!(node = calls[event])) continue;
                events.push({next: node.next, tail: node.tail});
            }
            // Traverse each list, stopping when the saved tail is reached.
            rest = Array.prototype.slice.call(arguments, 1);
            while (node = events.pop()) {
                tail = node.tail;
                args = node.event ? [node.event].concat(rest) : rest;
                while ((node = node.next) !== tail) {
                node.callback.apply(node.context || this, args);
                }
            }
            return this;
        };

        var get_canonical_op = function(orig) {
            var canonical = _(orig).isString() ? OP_DICT[orig.toLowerCase()] : null;
            if (canonical == null) {
                throw "Illegal constraint operator: " + orig;
            }
            return canonical;
        }

        constructor = _.bind(function(properties, service) {
            _.defaults(this, {
                constraints: [], 
                views: [], 
                joins: {}, 
                constraintLogic: "",
                sortOrder: []
            });
            this.service = service || {};
            this.model = properties.model || {};
            this.summaryFields = properties.summaryFields || {};
            this.root = properties.root || properties.from;
            this.select(properties.views || properties.select || []);
            this.addConstraints(properties.constraints || properties.where || []);
            this.addJoins(properties.joins || properties.join || []);
            this.constraintLogic = properties.constraintLogic || this.constraintLogic;
            this.orderBy(properties.sortOrder || properties.orderBy || []);
            this.maxRows = properties.size || properties.limit;
            this.start = properties.start || properties.offset || 0;
        }, this);

        this.removeFromSelect = function(unwanted) {
            unwanted = _(unwanted).isString() ? [unwanted] : unwanted || [];
            var mapFn = _.compose.apply(this, _.map([expandStar, adjustPath], function (f) {
                return _(f).bind(this)
            }));
            unwanted = _.flatten([_(unwanted).map(mapFn)]);
            console.log(unwanted);
            
            this.sortOrder = _(this.sortOrder).filter(function(so) {return !_(unwanted).include(so.path);});

            this.views = _(this.views).difference(unwanted);
            this.trigger("remove:view", unwanted);
            this.trigger("change:views", this.views);
            return this;
        };

        this.removeConstraint = function(con) {
            var reduced = []
                , orig = this.constraints;
            if (typeof con == 'string') {
                // If we have a code, remove the constraint with that code.
                reduced = _(orig).reject(function(c) {
                    return c.code === con;
                });
            } else {
                // Perform object comparison.
                reduced = _(orig).reject(function(c) {
                    return con.path === c.path
                           && con.op === c.op
                           && con.value === c.value
                           && con.extraValue === c.extraValue
                           && con.type === c.type
                           && (con.values ? con.values.join("%%%") : "") === (c.values ? c.values.join("%%%") : "");
                });
            }
            if (reduced.length != orig.length - 1) {
                throw "Did not remove a single constraint. orig=" 
                    + orig + ", reduced=" + reduced + ", argument=" + con;
            }
            this.constraints = reduced;
            this.trigger("change:constraints");
            this.trigger("removed:constraints", _.difference(orig, reduced));
            return this;
        };

        this.addToSelect = function(views) {
            var self = this;
            views = _(views).isString() ? [views] : views || [];
            var toAdd  = __(views).map(_(adjustPath).bind(this))
                     .map(_(expandStar).bind(this))
                     .value();

            _.chain([toAdd]).flatten().each(function(p) { self.views.push(p) });
            this.trigger("add:view", toAdd);
            this.trigger("change:views", toAdd);
            return this;
        };

        this.select = function(views) {
            this.views = [];
            _(views).each(_(this.addToSelect).bind(this));
            return this;
        };

        var adjustPath = function(path) {
            // Allow descriptors to be passed in.
            path = (path && path.name) ? path.name : "" + path;
            if (!this.root) {
                this.root = path.split(".")[0];
            } else if (path.indexOf(this.root) != 0) {
                path = this.root + "." + path;
            }
            return path;
        };

        var possiblePaths = {};

        var getAllFields = function(table) {
            var attrs = _(table.attributes).values();
            var refs = _(table.references).values();
            var cols = _(table.collections).values();
            return _.union(attrs, refs, cols);
        };

        // TODO: unit tests
        this._getPaths = function(root, cd, depth) {
            var that = this;
            var ret = [root];
            var others = [];
            if (cd && depth > 0) {
                with (_) {
                    others = flatten(map(cd.fields, function(r) {
                        var p = root + "." + r.name;
                        var pi = that.getPathInfo(p);
                        var cls = pi.getEndClass();
                        return that._getPaths(p, cls, depth - 1);
                    }));
                }
            } 
            return ret.concat(others);
        };

        /**
         * Get a list of valid paths for this query, given
         * the model and the query's starting root class.
         * The lists generated are cached.
         *
         * @param depth The number of levels of fields to traverse.
         *              The minimum value is 1, and the default is 3.
         *
         * @return A list of paths
         */
        this.getPossiblePaths = function(depth) {
            depth = depth || 3;
            if (!possiblePaths[depth]) {
                var cd = this.service.model.classes[this.root];
                possiblePaths[depth] = _.flatten(this._getPaths(this.root, cd, depth)); 
            }
            return possiblePaths[depth];
        };

        this.getPathInfo = function(path) {
            var adjusted = adjustPath.call(this, path);
            return this.service.model.getPathInfo(adjusted, this.getSubclasses());
        };

        this.getSubclasses = function() {
            return _(this.constraints)
                    .reduce(function(a, c) {c.type && (a[c.path] = c.type); return a}, {})
        };

        this.getType = function(path) {
            return this.getPathInfo(path).getType();
        };

        this.canHaveMultipleValues = function(path) {
            var adjusted = adjustPath.call(this, path);
            return this.service.model.hasCollection(adjusted);
        };

        this.getViewNodes = function() {
            var self = this;
            var toParentNode = function(v) {return self.getPathInfo(v).getParent()};
            var toPathString = function(node) {return node.toPathString();};
            return _.uniq(_.map(self.views, toParentNode), false, toPathString);
        };

        this.getQueryNodes = function() {
            var self = this;
            var viewNodes = self.getViewNodes();
            var constrainedNodes = _.map(self.constraints, function(c) {
                var pi  = self.getPathInfo(c.path);
                if (pi.isAttribute()) {
                    return pi.getParent();
                } else {
                    return pi;
                }
            });
            return _.uniq(viewNodes.concat(constrainedNodes), false, function(node) {
                return node.toPathString();
            });
        };

        var decapitate = function(x) {return x.substr(x.indexOf("."))};
        var expandStar = function(path) {
            var self = this;
            if (/\*$/.test(path)) {
                var pathStem = path.substr(0, path.lastIndexOf("."));
                var expand   = function(x) {return pathStem + x};
                var cd = this.model.getCdForPath(pathStem);
                if (/\.\*$/.test(path)) {
                    if (cd && this.summaryFields[cd.name]) {
                        return __(this.summaryFields[cd.name])
                                .reject(this.hasView)
                                .map(_.compose(expand, decapitate))
                                .value();
                    }
                } 
                if (/\.\*\*$/.test(path)) {
                    var str = function(a) {return "." + a.name};
                    return __(_(expandStar).bind(this)(pathStem + ".*"))
                            .union(_(cd.attributes).map(_.compose(expand, str)))
                            .unique()
                            .value();
                } 
            }
            return path;
        }

        /**
         * Return true if this path
         * is declared to be an outer join.
         *
         * @param p The path to enquire about.
         * @return Whether this path is declared to be on an outer join.
         */
        this.isOuterJoin = function(p) {
            var expanded = adjustPath.call(this, p);
            return this.joins[expanded] === "OUTER";
        };


        this.hasView = function(v) {
            return this.views && _(this.views).include(v);
        };

        this.count = function(cont) {
            if (this.service.count) {
                return this.service.count(this, cont);
            } else {
                throw "This query has no service. It cannot request a count";
            }
        };

        var getListResponseHandler = function(service, cb) { return function(data) {
            cb = cb || function() {};
            var name = data.listName;
            return service.fetchLists(function(ls) {
                cb(_(ls).find(function(l) {return l.name === name}));
            });
        }};

        // TODO: unit tests
        this.appendToList = function(target, cb) {
            var name = (target && target.name) ? target.name : "" + target;
            var toRun  = this.clone();
            if (toRun.views.length != 1 || !toRun.views[0].match(/\.id$/)) {
                toRun.select(["id"]);
            }
            var req = {
                "listName": name,
                "query": toRun.toXML()
            };
            var wrappedCb;
            if (target && target.name) {
                wrappedCb = function(list) {
                    target.size = list.size;
                    cb(list);
                };
            } else {
                wrappedCb = cb;
            }

            return service.makeRequest("query/append/tolist", 
                    req, getListResponseHandler(this.service, wrappedCb), "POST");
        };

        this.saveAsList = function(options, cb) {
            var toRun  = this.clone();
            if (toRun.views.length != 1 || toRun.views[0] == null || !toRun.views[0].match(/\.id$/)) {
                toRun.select(["id"]);
            }
            var req = _.clone(options);
            req.listName = req.listName || req.name;
            req.query = toRun.toXML();
            if (options.tags) {
                req.tags = options.tags.join(';');
            }
            var service = this.service;
            return service.makeRequest("query/tolist", req, getListResponseHandler(this.service, cb), "POST");
        };

        this.summarise = function(path, limit, cont) {
            if (_.isFunction(limit) && !cont) {
                cont = limit;
                limit = null;
            };
            cont = cont || function() {};
            path = adjustPath.call(this, path);
            var toRun = this.clone();
            if (!_(toRun.views).include(path)) {
                toRun.views.push(path);
            }
            var req = {query: toRun.toXML(), format: "jsonrows", summaryPath: path};
            if (limit) {
                req.size = limit;
            }
            return this.service.makeRequest("query/results", req, function(data) {cont(data.results, data.uniqueValues)});
        };

        this.summarize = this.summarise;

        this._get_data_fetcher = function(serv_fn) { 
            return function(page, cb) {
                var self = this;
                cb = cb || page;
                page = (_(page).isFunction() || !page) ? {} : page;
                if (self.service[serv_fn]) {
                    _.defaults(page, {start: self.start, size: self.maxRows});
                    return self.service[serv_fn](self, page, cb);
                } else {
                    throw "This query has no service. It cannot request results";
                }
            };
        };

        this.records = this._get_data_fetcher("records");
        this.rows = this._get_data_fetcher("rows");
        this.table = this._get_data_fetcher("table");

        this.clone = function(cloneEvents) {
            // Not the fastest, but it does make isolated clones.
            var clone = jQuery.extend(true, {}, this);
            if (!cloneEvents) {
                clone._callbacks = {};
            }
            return clone;
        };

        this.next = function() {
            var clone = this.clone();
            if (this.maxRows) {
                clone.start = this.start + this.maxRows;
            }
            return clone;
        };

        this.previous = function() {
            var clone = this.clone();
            if (this.maxRows) {
                clone.start = this.start - this.maxRows;
            } else {
                clone.start = 0;
            }
            return clone;
        };

        this.getSortDirection = function(path) {
            path = adjustPath.call(this, path);
            var i = 0, l = this.sortOrder.length;
            for (i = 0; i < l; i++) {
                if (this.sortOrder[i].path === path) {
                    return this.sortOrder[i].direction;
                }
            }
            return null;
        };

        /**
         * @return true if the path given is on an outerjoined group.
         */
        this.isOuterJoined = function(path) {
            path = adjustPath.call(this, path);
            var outer = "OUTER";
            return _.any(this.joins, function(d, p) {return d === outer && path.indexOf(p) === 0;});
        };

        var parseSortOrder = function(input, adjuster) {
            var so = input;
            with (_) {
                if (isString(input)) {
                    so = {path: input, direction: "ASC"};
                } else if (! input.path) {
                    var k = keys(input)[0];
                    var v = values(input)[0];
                    so = {path: k, direction: v};
                } 
            }
            so.path = adjuster(so.path);
            so.direction = so.direction.toUpperCase();
            return so;
        };

        /**
         * Either add a sort order element to the end of the sortOrder, if no
         * direction is defined for that path, or if there is already a direction set for this
         * path then that direction is updated with the supplied one.
         */
        this.addOrSetSortOrder = function(so) {
            var adjuster = _(adjustPath).bind(this);
            var so = parseSortOrder(so, adjuster);
            var currentDirection = this.getSortDirection(so.path);
            if (currentDirection == null) {
                this.addSortOrder(so);
            } else if (currentDirection != so.direction) {
                _(this.sortOrder).each(function(oe) {
                    if (oe.path === so.path) {
                        oe.direction = so.direction;
                    }
                });
                this.trigger("change:sortorder", this.sortOrder);
            }
        };

        /**
         * @triggers a "add:sortorder" event.
         */
        this.addSortOrder = function(so) {
            var adjuster = _(adjustPath).bind(this);
            var so = parseSortOrder(so, adjuster);
            this.sortOrder.push(so);
            this.trigger("add:sortorder", so);
            this.trigger("change:sortorder", this.sortOrder);
        };

        /**
         * @triggers a "set:sortorder" event.
         */
        this.orderBy = function(sort_orders) {
            this.sortOrder = [];
            _(sort_orders).each(_(this.addSortOrder).bind(this));
            this.trigger("set:sortorder", this.sortOrder);
            return this;
        };

        this.addJoins = function(joins) {
            _(joins).each(_(this.addJoin).bind(this));
            return this;
        };

        this.addJoin = function(join) {
            if (_.isString(join)) {
                join = {path: join, style: "OUTER"};
            }
            join.path = _(adjustPath).bind(this)(join.path);
            join.style = join.style ? join.style.toUpperCase() : join.style;
            if (!_(JOIN_STYLES).include(join.style)) {
                throw "Invalid join style: " + join.style;
            }
            this.joins[join.path] = join.style;
            return this;
        };

        this.setJoinStyle = function(path, style) {
            style = style || "OUTER";
            path = adjustPath.call(this, path);
            if (this.joins[path] !== style) {
                this.joins[path] = style;
                this.trigger("change:joins", {path: path, style: style});
            }
            return this;
        };

        this.addConstraints = function(constraints) {
            this.__silent__ = true;
            if (_.isArray(constraints)) {
                _(constraints).each(_(this.addConstraint).bind(this));
            } else {
                var that = this;
                _(constraints).each(function(val, key) {
                    var constraint = {path: key};
                    if (_.isArray(val)) {
                        constraint.op = "ONE OF";
                        constraint.values = val;
                    } else if (_.isString(val) || _.isNumber(val)) {
                        if (_.isString(val) && _(NULL_OPS).include(val.toUpperCase())) {
                            constraint.op = val;
                        } else {
                            constraint.op = "=";
                            constraint.value = val;
                        }
                    } else {
                        var k = _.keys(val)[0];
                        var v = _.values(val)[0];
                        if (k == "isa") {
                            constraint.type = v;
                        } else {
                            constraint.op = k;
                            constraint.value = v;
                        }
                    }
                    that.addConstraint(constraint);
                });
            }
            this.__silent__ = false;
            this.trigger("add:constraint");
            this.trigger("change:constraints");
            return this;
        };

        /**
         * Triggers an "add:constraint" event.
         */
        this.addConstraint = function(constraint) {
            var that = this;
            if (_.isArray(constraint)) {
                var conArgs = constraint.slice();
                var constraint = {path: conArgs.shift()};
                if (conArgs.length == 1) {
                    if (_(NULL_OPS).include(conArgs[0].toUpperCase())) {
                        constraint.op = conArgs[0];
                    } else {
                        constraint.type = conArgs[0];
                    }
                } else if (conArgs.length >= 2) {
                    constraint.op = conArgs[0];
                    var v = conArgs[1];
                    if (_.isArray(v)) {
                        constraint.values = v;
                    } else {
                        constraint.value = v;
                    }
                    if (conArgs.length == 3) {
                        constraint.extraValue = conArgs[2];
                    }
                }
            }

            constraint.path = _(adjustPath).bind(this)(constraint.path);
            if (!constraint.type) {
                try {
                    constraint.op = get_canonical_op(constraint.op);
                } catch(er) {
                    throw "Could not make constraint on " + constraint.path + ": " + er;
                }
            }
            this.constraints.push(constraint);
            if (!this.__silent__) {
                this.trigger("add:constraint", constraint);
                this.trigger("change:constraints");
            }
            return this;
        };

        this.getSorting = function() {
            return _(this.sortOrder).map(function(x) {return x.path + " " + x.direction}).join(" ");
        };

        this.getConstraintXML = function() {
            var xml = "";
            __(this.constraints).filter(function(c) {return c.type != null}).each(function(c) {
                xml += '<constraint path="' + c.path + '" type="' + c.type + '"/>';
            });
            __(this.constraints).filter(function(c) {return c.type == null}).each(function(c) {
                xml += '<constraint path="' + c.path + '" op="' + _.escape(c.op) + '"';
                if (c.value) {
                    xml += ' value="' + _.escape(c.value) + '"';
                }
                if (c.values) {
                    xml += '>';
                    _(c.values).each(function(v) {xml += '<value>' + _.escape(v) + '</value>'});
                    xml += '</constraint>';
                } else {
                    xml += '/>';
                }
            });
            return xml;
        };

        this.toXML = function() {
            var xml = "<query ";
            xml += 'model="' + this.model.name + '"';
            xml += ' ';
            xml += 'view="' + this.views.join(" ") + '"';
            if (this.sortOrder.length) {
                xml += ' sortOrder="' + this.getSorting() + '"';
            }
            if (this.constraintLogic) {
                xml += ' constraintLogic="' + this.constraintLogic + '"';
            }
            xml += ">";
            _(this.joins).each(function(style, j_path) {
                xml += '<join path="' + j_path + '" style="' + style + '"/>';
            });
            xml += this.getConstraintXML();
            xml += '</query>';

            return xml;
        };

        this.fetchCode = function(lang, cb) {
            cb = cb || function() {};
            var req = {
                query: this.toXML(),
                lang: lang,
                format: "json"
            };
            return this.service.makeRequest("query/code", req, function(data) {
                cb(data.code);
            });
        };

        var BIO_FORMATS = ["gff3", "fasta", "bed"];

        this.getExportURI = function(format) {
            format = format || "tab";
            if (_(BIO_FORMATS).include(format)) {
                var meth = "get" + format.toUpperCase() + "URI";
                return this[meth]();
            }
            var req = {
                query: this.toXML(),
                format: format
            };
            if (this.service && this.service.token) {
                req.token = this.service.token;
            }
            return this.service.root + "query/results?" + jQuery.param(req);
        };

        var cls = this;

        _(BIO_FORMATS).each(function(f) {
            var reqMeth = "_" + f + "_req";
            var getMeth = "get" + f.toUpperCase();
            var uriMeth = getMeth + "URI";
            cls[getMeth] = function(cb) {
                var req = this[reqMeth]();
                cb = cb || function() {};
                return this.service.makeRequest("query/results/" + f, req, cb, "POST");
            };

            cls[uriMeth] = function() {
                var req = this[reqMeth]();
                if (this.service.token) {
                    req.token = this.service.token;
                }
                return this.service.root + "query/results/" + f + "?" + jQuery.param(req);
            };
        });

        this._fasta_req = function() {
            var self = this;
            var toRun = this.clone();
            var currentViews = toRun.views;
            var newView = _(currentViews).chain()
                            .map(function(v) {return self.getPathInfo(v).getParent()})
                            .filter(function(p) {return p.isa("SequenceFeature") || p.isa("Protein") })
                            .map(function(p) {return p.append("primaryIdentifier").toPathString()})
                            .value();
            toRun.views = [newView.shift()];
            var req = {query: toRun.toXML()};
            return req;
        };

        this._gff3_req = function() {
            var self = this;
            var toRun = this.clone();
            var currentViews = toRun.views;
            var newView = _(currentViews).chain()
                            .map(function(v) {return self.getPathInfo(v).getParent()})
                            .uniq(function(p) {return p.toPathString()})
                            .filter(function(p) {return p.isa("SequenceFeature") })
                            .map(function(p) {return p.append("primaryIdentifier").toPathString()})
                            .value();
            toRun.views = newView;
            var req = {query: toRun.toXML()};
            return req;
        };

        this._bed_req = this._gff3_req;

        this.getCodeURI = function(lang) {
            var req = {
                query: this.toXML(),
                lang: lang,
                format: "text"
            };
            if (this.service && this.service.token) {
                req.token = this.service.token;
            }
            return this.service.root + "query/code?" + jQuery.param(req);
        };

        constructor(properties || {}, service);
    };

    Query.ATTRIBUTE_VALUE_OPS = ["=", "!=", ">", ">=", "<", "<=", "CONTAINS"];
    Query.MULTIVALUE_OPS = ["ONE OF", "NONE OF"];
    Query.NULL_OPS = ["IS NULL", "IS NOT NULL"];
    Query.ATTRIBUTE_OPS = _.union(Query.ATTRIBUTE_VALUE_OPS, Query.MULTIVALUE_OPS, Query.NULL_OPS);

    Query.TERNARY_OPS = ["LOOKUP"];
    Query.LOOP_OPS = ["=", "!="];
    Query.LIST_OPS = ["IN", "NOT IN"];
    Query.REFERENCE_OPS = _.union(Query.TERNARY_OPS, Query.LOOP_OPS, Query.LIST_OPS);

    return {"Query": Query};
})());
if (typeof intermine == "undefined") {
    intermine = {};
}

if (typeof __ == "undefined") {
    __ = function(x) {return _(x).chain()};
}

_.extend(intermine, (function() {

    var MODELS = {};
    var SUMMARY_FIELDS = {};
    var slice = Array.prototype.slice;

    var Service = function(properties) {
        var DEFAULT_PROTOCOL = "http://";
        var VERSION_PATH = "version";
        var TEMPLATES_PATH = "templates";
        var LISTS_PATH = "lists";
        var MODEL_PATH = "model";
        var SUMMARYFIELDS_PATH = "summaryfields";
        var QUERY_RESULTS_PATH = "query/results";
        var QUICKSEARCH_PATH = "search";
        var WIDGETS_PATH = "widgets";
        var ENRICHMENT_PATH = "list/enrichment";
        var WITH_OBJ_PATH = "listswithobject";

        var LIST_OPERATION_PATHS = {
            merge: "lists/union",
            intersect: "lists/intersect",
            diff: "lists/diff"
        };

        var getResulteriser = function(cb) { return function(data) {
            cb = cb || function() {};
            cb(data.results, data);
        }};

        var getFormat = function(def) {
            var format = def || "json";
            if (!jQuery.support.cors) {
                format = format.replace("json", "jsonp");
            }
            return format;
        };

        /**
         * Performs a get request for data against a url. 
         * This method makes use of jsonp where available.
         */
        this.makeRequest = function(path, data, cb, method) {
            var url = this.root + path;
            data = data || {};
            cb = cb || function() {};
            if (this.token) {
                data.token = this.token;
            }
            var dataType = "json";
            data.format = getFormat(data.format);
            if (!jQuery.support.cors) {
                data.method = method;
                method = false; 
                url += "?callback=?";
                dataType = "jsonp";
                console.log("No CORS support: going for jsonp");
            }

            if (method) {
                if (method === "DELETE") {
                    // grumble grumble struts grumble grumble...
                    url += "?" + jQuery.param(data);
                }
                return jQuery.ajax({
                    data: data,
                    dataType: "json",
                    success: cb,
                    url: url,
                    type: method
                });
            } else {
                return jQuery.getJSON(url, data, cb);
            }
        };

        this.widgets = function(cb) {
            cb = cb || _.identity;
            return this.makeRequest(WIDGETS_PATH, null, function(data) {
                cb(data.widgets);
            });
        };

        this.enrichment = function(req, cb) {
            cb = cb || _.identity;
            _.defaults(req, {maxp: 0.05});
            return this.makeRequest(ENRICHMENT_PATH, req, function(data) {cb(data.results)});
        };

        this.search = function(options, cb) {
            if (_(options).isString()) {
                options = {term: options};
            }
            if (!cb && _(options).isFunction()) {
                cb = options;
                options = {};
            }
            options = options || {};
            cb      = cb      || function() {};
            _.defaults(options, {term: "", facets: {}});
            var req = {q: options.term, start: options.start, size: options.size};
            if (options.facets) {
                _(options.facets).each(function(v, k) {
                    req["facet_" + k] = v;
                });
            }
            return this.makeRequest(QUICKSEARCH_PATH, req, function(data) {
                cb(data.results, data.facets);
            }, "POST");
        };

        this.count = function(q, cont) {
            var req = {
                query: q.toXML(),
                format: jQuery.support.cors ? "jsoncount" : "jsonpcount",
            };
            var promise = jQuery.Deferred();
            this.makeRequest(QUERY_RESULTS_PATH, req, function(data) {
                cont(data.count);
                promise.resolve(data.count);
            }).fail(promise.reject);
            return promise;
        };

        this.findById = function(table, objId, cb) {
            this.query({from: table, select: ["**"], where: {"id": objId}}, function(q) {
                q.records(function(rs) {
                    cb(rs[0]);
                });
            });
        };

        this.whoami = function(cb) {
            cb = cb || function() {};
            var self = this;
            var promise = jQuery.Deferred();
            self.fetchVersion(function(v) {
                if (v < 9) {
                    var msg = "The who-am-i service requires version 9, this is only version " + v;
                    promise.reject("not available", msg);
                } else {
                    self.makeRequest("user/whoami", null, function(resp) {cb(resp.user)})
                        .then(promise.resolve, promise.reject);
                }
            });
            return promise;
        };

        this.table = function(q, page, cb) {
            page = page || {};
            var req = _(page).extend({
                query: q.toXML(), 
                format: "jsondatatable"
            });
            return this.makeRequest(QUERY_RESULTS_PATH, req, getResulteriser(cb), "POST");
        };

        this.records = function(q, page, cb) {
            // Allow calling as records(q, cb)
            if (_(cb).isUndefined() && _(page).isFunction()) {
                cb = page;
                page = {};
            }
            page = page || {};
            var req = _(page).extend({query: q.toXML(), format: jQuery.support.cors ? "jsonobjects" : "jsonpobjects"});
            return this.makeRequest(QUERY_RESULTS_PATH, req, getResulteriser(cb));
        };

        this.rows = function(q, page, cb) {
            // Allow calling as rows(q, cb)
            if (_(cb).isUndefined() && _(page).isFunction()) {
                cb = page;
                page = {};
            }
            page = page || {};
            var req = _(page).extend({query: q.toXML()});
            return this.makeRequest(QUERY_RESULTS_PATH, req, getResulteriser(cb));
        };

        var constructor = _.bind(function(properties) {
            var root = properties.root;
            if (root && !/^https?:\/\//i.test(root)) {
                root = DEFAULT_PROTOCOL + root;
            }
            if (root && !/service\/?$/i.test(root)) {
                root = root + "/service/";
            }
            this.root = root;
            this.token = properties.token

            _.bindAll(this, "fetchVersion", "rows", "records", "fetchTemplates", "fetchLists", 
                "count", "makeRequest", "fetchModel", "fetchSummaryFields", "combineLists", 
                "merge", "intersect", "diff", "query", "whoami");

        }, this);

        this.fetchVersion = function(cb) {
            var self = this;
            var promise = jQuery.Deferred();
            if (typeof this.version === "undefined") {
                this.makeRequest(VERSION_PATH, null, function(data) {
                    this.version = data.version;
                    cb(this.version);
                }).fail(promise.reject);
            } else {
                cb(this.version);
                promise.resolve(this.version);
            }
            return promise;
        };

        this.fetchTemplates = function(cb) {
            var promise = jQuery.Deferred();
            this.makeRequest(TEMPLATES_PATH, null, function(data) {
                cb(data.templates);
                promise.resolve(data.templates);
            }).fail(promise.reject);
            return promise;
        };

        this.fetchLists = function(cb) {
            var self = this;
            var promise = jQuery.Deferred();
            this.makeRequest(LISTS_PATH, null, function(data) {
                var lists = _(data.lists).map(function (l) {return new intermine.List(l, self)});
                cb(lists);
                promise.resolve(lists);
            }).fail(promise.reject);
            return promise;
        };

        this.combineLists = function(operation) {
            var self = this;
            return function(options, cb) {
                var promise = jQuery.Deferred();
                var path = LIST_OPERATION_PATHS[operation];
                var params = {
                    name: options.name,
                    tags: options.tags.join(';'),
                    lists: options.lists.join(";"),
                    description: options.description
                };
                self.makeRequest(path, params, function(data) {
                    var name = data.listName;
                    self.fetchLists(function(ls) {
                        var l = _(ls).find(function(l) {return l.name === name});
                        cb(l);
                        promise.resolve(l);
                    }).fail(promise.reject);
                }).fail(promise.reject);
                return promise;
            };
        };

        this.merge = this.combineLists("merge");
        this.intersect = this.combineLists("intersect");
        this.diff = this.combineLists("diff");

        this.fetchModel = function(cb) {
            var self = this;
            var promise = jQuery.Deferred();
            if (MODELS[self.root]) {
                self.model = MODELS[self.root];
            }
            if (self.model) {
                cb(self.model);
                promise.resolve(self.model);
            } else {
                this.makeRequest(MODEL_PATH, null, function(data) {
                    if (intermine.Model) {
                        self.model = new intermine.Model(data.model);
                    } else {
                        self.model = data.model;
                    }
                    MODELS[self.root] = self.model;
                    cb(self.model);
                    promise.resolve(self.model);
                }).fail(promise.reject);
            }
            return promise;
        };

        this.fetchSummaryFields = function(cb) {
            var self = this;
            var promise = jQuery.Deferred();
            if (SUMMARY_FIELDS[self.root]) {
                self.summaryFields = SUMMARY_FIELDS[self.root];
            }
            if (self.summaryFields) {
                cb(self.summaryFields);
                promise.resolve(self.summaryFields);
            } else {
                self.makeRequest(SUMMARYFIELDS_PATH, null, function(data) {
                    self.summaryFields = data.classes;
                    SUMMARY_FIELDS[self.root] = data.classes;
                    cb(self.summaryFields);
                    promise.resolve(self.summaryFields);
                });
            }
            return promise;
        };

        /**
         * Fetch lists containing an item.
         *
         * @param options Options should contain: 
         *  - either:
         *    * id: The internal id of the object in question
         *  - or: 
         *    * publicId: An identifier
         *    * type: The type of object (eg. "Gene")
         *    * extraValue: (optional) A domain to help resolve the object (eg an organism for a gene).
         *
         *  @param cb function of the type: [List] -> ()
         *  @return A promise
         */
        this.fetchListsContaining = function(opts, cb) {
            cb = cb || function() {};
            return this.makeRequest(WITH_OBJ_PATH, opts, function(data) {cb(data.lists)});
        };


        this.query = function(options, cb) {
            var service = this;
            var promise = jQuery.Deferred();
            service.fetchModel(function(m) {
                service.fetchSummaryFields(function(sfs) {
                    _.defaults(options, {model: m, summaryFields: sfs});
                    var q = new intermine.Query(options, service);
                    cb(q);
                    promise.resolve(q);
                }).fail(promise.reject);
            }).fail(promise.reject);
            return promise;
        };

        constructor(properties || {});
    };

    return {"Service": Service};
})());

        
