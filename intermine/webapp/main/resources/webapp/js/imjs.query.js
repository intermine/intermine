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
            unwanted = __(unwanted).map(_(adjustPath).bind(this))
                                   .map(_(expandStar).bind(this))
                                   .flatten()
                                   .value();

            this.views = _(this.views).difference(unwanted);
            this.trigger("remove:view", unwanted);
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

        var _getPaths = function(root, model, cd, depth) {
            var ret = [root];
            if (depth > 0) {
                return ret.concat(__(getAllFields(cd))
                    .map(function(r) {return root + "." + r.name})
                    .map(function(p) { 
                        var cls = model.getCdForPath(p);
                        if (cls) {
                            return _getPaths(p, model, cls, depth - 1);
                        } else {
                            return p;
                        }
                    }).flatten().value());
            } 
            return ret;
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
                var cd = this.service.model.classes[this.root]; //getCdForPath(this.root);
                possiblePaths[depth] = 
                    _getPaths(this.root, this.service.model, cd, depth);
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
            toRun.select(["id"]);
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

        /**
         * @triggers a "add:sortorder" event.
         */
        this.addSortOrder = function(so) {
            var adjuster = _(adjustPath).bind(this);
            if (_.isString(so)) {
                so = {path: so, direction: "ASC"};
            } else if (! so.path) {
                var k = _.keys(so)[0];
                var v = _.values(so)[0];
                so = {path: k, direction: v};
            }
            so.path = adjuster(so.path);
            so.direction = so.direction.toUpperCase();
            this.sortOrder.push(so);
            this.trigger("add:sortorder", so);
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
            return this.service.root + "query/results?" + $.param(req);
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
                return this.service.root + "query/results/" + f + "?" + $.param(req);
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
            return this.service.root + "query/code?" + $.param(req);
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