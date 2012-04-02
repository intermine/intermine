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
