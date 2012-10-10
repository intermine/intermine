# Classes that represent the data model of an InterMine data-warehouse,
# and elements within that data model. 
#

require 'rubygems'
require 'json'

module InterMine
module Metadata

    # 
    # == Description
    #
    # A representation of the data model of an InterMine data warehouse. 
    # This class contains access to all aspects of the model, including the tables
    # of data stored, and the kinds of data in those tables. It is also the
    # mechanism for creating objects which are representations of data within 
    # the data model, including records, paths and columns.
    #
    #   model = Model.new(data)
    #
    #   model.classes.each do |c|
    #       puts "#{c.name} has #{c.fields.size} fields"
    #   end
    #
    #:include:contact_header.rdoc
    #
    class Model

        FLOAT_TYPES = ["Float", "Double", "float", "double"]
        INT_TYPES = ["Integer", "int",  "long", "Long", "short", "Short"]
        BOOL_TYPES = ["Boolean", "boolean"]
        NUMERIC_TYPES = FLOAT_TYPES | INT_TYPES

        # The name of the model
        attr_reader :name

        # The classes within this model
        attr_reader :classes

        # The Service this model belongs to
        attr_reader :service

        # Construct a new model from its textual json representation
        #
        # Arguments:
        # [+model_data+] The JSON serialization of the model
        # [+service+] The Service this model belongs to
        #
        #   model = Model.new(json)
        #
        def initialize(model_data, service=nil) 
            result = JSON.parse(model_data)
            @model = result["model"]
            @service = service
            @name = @model["name"]
            @classes = {}
            @model["classes"].each do |k, v| 
                @classes[k] = ClassDescriptor.new(v, self)
            end
            @classes.each do |name, cld| 
                cld.fields.each do |fname, fd|
                    if fd.respond_to?(:referencedType)
                        refCd = self.get_cd(fd.referencedType)
                        fd.referencedType = refCd
                    end
                end
            end

        end

        # call-seq:
        #   get_cd(name) => ClassDescriptor
        #
        # Get a ClassDescriptor from the model by name.
        #
        # If a ClassDescriptor itself is passed as the argument,
        # it will be passed through.
        #
        def get_cd(cls)
            if cls.is_a?(ClassDescriptor)
                return cls
            else
                return @classes[cls.to_s]
            end
        end

        alias cd get_cd
        alias table get_cd

        # call-seq:
        #   make_new(name=nil, opts={}) => InterMineObject
        #
        # Make a new InterMineObject which is an instantiation of a class a ClassDescriptor represents
        #
        # Arguments:
        # [+name+] The name of the class to instantiate
        # [+opts+] The values to assign to the new object
        #
        #   gene = model.make_new 'Gene', {
        #       "symbol" => "zen",
        #       "name" => "zerknullt",
        #       "organism => {
        #           "shortName" => "D. melanogaster",
        #           "taxonId" => 7217
        #       }
        #   }
        #
        #   puts gene.organism.taxonId
        #   >>> 7217
        #
        def make_new(class_name=nil, opts={})
            # Support calling with just opts
            if class_name.is_a?(Hash)
                opts = class_name
                class_name = nil
            end
            if class_name && opts["class"] && (class_name != opts["class"]) && !get_cd(opts["class"]).subclass_of?(class_name)
                raise ArgumentError, "class name in options hash is not compatible with passed class name: #{opts["class"]} is not a subclass of #{class_name}"
            end
            # Prefer the options value to the passed value
            cd_name = opts["class"] || class_name
            cls = get_cd(cd_name).to_class
            obj = cls.new(opts)
            obj.send(:__cd__=, get_cd(cd_name))
            return obj
        end

        # === Resolve the value referred to by a path on an object
        #
        # The path may be either a string such as "Department.employees[2].name", 
        # or a Path object
        def resolve_path(obj, path)
            return obj._resolve(path)
        end

        private

        # For mocking in tests
        def set_service(service)
            @service = service
        end

    end

    # == A base class for all objects instantiated from a ClassDescriptor
    #
    # This class described the common behaviour for all objects instantiated 
    # as representations of classes defined by a ClassDescriptor. It is not intended
    # to be instantiated directly, but inherited from.
    #
    #:include:contact_header.rdoc
    #
    class InterMineObject

        # The database internal id in the originating mine. Serves as a guarantor
        # of object identity.
        attr_reader :objectId

        # The ClassDescriptor for this object
        attr_reader :__cd__

        # Arguments:
        # hash:: The properties of this object represented as a Hash. Nested 
        #        Arrays and Hashes are expected for collections and references.
        #
        def initialize(hash=nil)
            hash ||= {}
            hash.each do |key, value|
                if key.to_s != "class"
                    self.send(key.to_s + "=", value)
                end
            end
        end

        # call-seq:
        #   is_a?(other) => bool
        #
        # Determine if this class is a subclass of other.
        #
        # Overridden to provide support for querying against ClassDescriptors and Strings.
        #
        def is_a?(other)
            if other.is_a?(ClassDescriptor)
                return is_a?(other.to_module)
            elsif other.is_a?(String)
                return is_a?(@__cd__.model.cd(other))
            else
                return super
            end
        end

        # call-seq:
        #   to_s() => human-readable string
        #
        # Serialise to a readable representation
        def to_s
            parts = [@__cd__.name + ':' + self.objectId.to_s]
            self.instance_variables.reject{|var| var.to_s.end_with?("objectId")}.each do |var|
                parts << "#{var}=#{self.instance_variable_get(var).inspect}"
            end
            return "<#{parts.join(' ')}>"
        end

        # call-seq:
        #   [key] => value
        #
        # Alias property fetches as item retrieval, so the following are equivalent:
        #
        #   organism = gene.organism
        #   organism = gene["organism"]
        #
        def [](key)
            if @__cd__.has_field?(key):
                return self.send(key)
            end
            raise IndexError, "No field #{key} found for #{@__cd__.name}"
        end

        # call-seq:
        #   _resolve(path) => value
        #
        # Resolve a path represented as a String or as a Path into a value
        #
        # This is designed to automate access to values in deeply nested objects. So:
        #
        #   name = gene._resolve('Gene.organism.name')
        #
        # Array indices are supported:
        #  
        #   symbol = gene._resolve('Gene.alleles[3].symbol')
        #
        def _resolve(path)
            begin
                parts = path.split(/(?:\.|\[|\])/).reject {|x| x.empty?}
            rescue NoMethodError
                parts = path.elements.map { |x| x.name }
            end
            root = parts.shift
            if !is_a?(root)
                raise ArgumentError, "Incompatible path '#{path}': #{self} is not a #{root}"
            end
            begin
                res = parts.inject(self) do |memo, part| 
                    part = part.to_i if (memo.is_a?(Array) and part.to_i.to_s == part)
                    begin
                        new = memo[part]
                    rescue TypeError
                        raise ArgumentError, "Incompatible path '#{path}' for #{self}, expected an index"
                    end
                    new
                end
            rescue IndexError => e
                raise ArgumentError, "Incompatible path '#{path}' for #{self}, #{e}"
            end
            return res
        end

        alias inspect to_s

        private 

        def __cd__=(cld)
            @__cd__ = cld
        end
        
        def objectId=(val)
            @objectId = val
        end
    end

    # == A base module that provides helpers for setting up classes bases on the contents of a Hash
    #
    #  ClassDescriptors and FieldDescriptors are instantiated 
    #  with hashes that provide their properties. This module
    #  makes sure that the appropriate instance variables are set
    #
    #:include:contact_header.rdoc
    #
    module SetHashKey 

        # call-seq:
        #   set_key_value(key, value)
        #
        # Set up instance variables based on the contents of a hash
        def set_key_value(k, v)
            if (k == "type")
                k = "dataType"
            end
            ## create and initialize an instance variable for this 
            ## key/value pair
            self.instance_variable_set("@#{k}", v) 
            ## create the getter that returns the instance variable
            self.class.send(:define_method, k, 
                proc{self.instance_variable_get("@#{k}")})  
            ## create the setter that sets the instance variable
            self.class.send(:define_method, "#{k}=", 
                proc{|v| self.instance_variable_set("@#{k}", v)})  
            return
        end

        # call-seq: 
        #   inspect() => readable-string
        #
        # Produce a readable string
        def inspect
            parts = []
            self.instance_variables.each do |x|
                var = self.instance_variable_get(x)
                if var.is_a?(ClassDescriptor) || var.is_a?(Model)
                    parts << x.to_s + "=" + var.to_s
                else
                    parts << x.to_s + "=" + var.inspect
                end
            end
            return "<#{parts.join(' ')}>"
        end
    end

    # == A class representing a table in the InterMine data model
    #
    # A class descriptor represents a logical abstraction of a table in the 
    # InterMine model, and contains information about the columns in the table
    # and the other tables that are referenced by this table.
    #
    # It can be used to construct queries directly, when obtained from a webservice.
    #
    #   cld = service.model.table('Gene')
    #   cld.where(:symbol => 'zen').each_row {|row| puts row}       
    #
    #:include:contact_header.rdoc
    #
    class ClassDescriptor
        include SetHashKey

        # The InterMine Model
        attr_reader :model

        # The Hash containing the fields of this model
        attr_reader :fields

        # ClassDescriptors are constructed automatically when the model itself is 
        # parsed. They should not be constructed on their own.
        #
        # Arguments:
        # [+opts+] A Hash containing the information to initialise this ClassDescriptor.
        # [+model+] The model this ClassDescriptor belongs to.
        #
        def initialize(opts, model)
            @model = model
            @fields = {}
            @klass = nil
            @module = nil

            field_types = {
                "attributes" => AttributeDescriptor,
                "references" => ReferenceDescriptor,
                "collections" => CollectionDescriptor
            }

            opts.each do |k,v|
                if (field_types.has_key?(k))
                    v.each do |name, field| 
                        @fields[name] = field_types[k].new(field, model)
                    end
                else
                    set_key_value(k, v)
                end
            end
        end

        # call-seq: 
        #   new_query => PathQuery::Query
        #
        # Construct a new query for the service this ClassDescriptor belongs to
        # rooted on this table.
        #
        #   query = model.table('Gene').new_query
        #
        def new_query
            q = @model.service.new_query(self.name)
            return q
        end

        alias query new_query

        # call-seq:
        #   select(*columns) => PathQuery::Query
        #
        # Construct a new query on this table in the originating
        # service with given columns selected for output.
        #
        #   query = model.table('Gene').select(:symbol, :name, "organism.name", "alleles.*")
        #
        #   query.each_result do |gene|
        #       puts "#{gene.symbol} (#{gene.organism.name}): #{gene.alleles.size} Alleles"
        #   end
        #
        def select(*cols)
            q = new_query
            q.add_views(cols)
            return q
        end

        # call-seq: 
        #   where(*constraints) => PathQuery::Query
        #
        # Returns a new query on this table in the originating
        # service will all attribute columns selected for output 
        # and the given constraints applied.
        #
        #   zen = model.table('Gene').where(:symbol => 'zen').one
        #   puts "Zen is short for #{zen.name}, and has a length of #{zen.length}"
        #
        def where(*args)
            q = new_query
            q.select("*")
            q.where(*args)
            return q
        end

        # call-seq:
        #   get_field(name) => FieldDescriptor
        #
        # Returns the field of the given name if it exists in the
        # referenced table.
        #
        def get_field(name)
            return @fields[name]
        end

        alias field get_field

        # call-seq:
        #   has_field?(name) => bool
        #
        # Returns true if the table has a field of the given name.
        #
        def has_field?(name)
            return @fields.has_key?(name)
        end

        # call-seq:
        #   attributes => Array[AttributeDescriptor]
        #
        # Returns an Array of all fields in the current table that represent 
        # attributes (ie. columns that can hold values, rather than references to 
        # other tables.)
        #
        # The array returned will be sorted in alphabetical order by field-name.
        #
        def attributes
            return @fields.
                select {|_, v| v.is_a?(AttributeDescriptor)}.
                sort   {|(k0, _), (k1, _)| k0 <=> k1}.
                map    {|(_, v)| v}
        end

        # Returns a human readable string
        def to_s
            return "#{@model.name}.#{@name}"
        end

        # Return a fuller string representation.
        def inspect 
            return "<#{self.class.name}:#{self.object_id} #{to_s}>"
        end

        # call-seq:
        #   subclass_of?(other) => bool
        #
        # Returns true if the class this ClassDescriptor describes is a
        # subclass of the class the other element evaluates to. The other
        # may be a ClassDescriptor, or a Path, or a String describing a path.
        #
        #   model.table('Gene').subclass_of?(model.table('SequenceFeature'))
        #   >>> true
        #
        #   model.table('Gene').subclass_of?(model.table('Protein'))
        #   >>> false
        #
        def subclass_of?(other)
            path = Path.new(other, @model)
            if @extends.include? path.end_type
                return true
            else
                @extends.each do |x|
                    superCls = @model.get_cd(x)
                    if superCls.subclass_of?(path)
                        return true
                    end
                end
            end
            return false
        end

        # call-seq: 
        #   to_module => Module
        #
        # Produces a module containing the logic this ClassDescriptor represents,
        # suitable for including into a class definition.
        #
        # The use of modules enables multiple inheritance, which is supported in 
        # the InterMine data model, to be represented in the classes instantiated
        # in the client.
        #
        def to_module
            if @module.nil?
                nums = Model::FLOAT_TYPES
                ints = Model::INT_TYPES
                bools = Model::BOOL_TYPES

                supers = @extends.map { |x| @model.get_cd(x).to_module }

                klass = Module.new
                fd_names = @fields.values.map { |x| x.name }
                attr_names = @fields.values.select { |x| x.is_a?(AttributeDescriptor)}.map {|x| x.name}
                klass.class_eval do
                    include *supers
                    attr_reader *attr_names
                end

                @fields.values.each do |fd|
                    if fd.is_a?(CollectionDescriptor)
                        klass.class_eval do
                            define_method("add" + fd.name.capitalize) do |*vals|
                                type = fd.referencedType
                                instance_var = instance_variable_get("@" + fd.name)
                                instance_var ||= []
                                vals.each do |item|
                                    if item.is_a?(Hash)
                                        item = type.model.make_new(type.name, item)
                                    end
                                    if !item.is_a?(type)
                                        raise ArgumentError, "Arguments to #{fd.name} in #{@name} must be #{type.name}s"
                                    end
                                    instance_var << item
                                end
                                instance_variable_set("@" + fd.name, instance_var)
                            end
                        end
                    end
                    
                    if fd.is_a?(ReferenceDescriptor)
                        klass.class_eval do 
                            define_method(fd.name) do 
                                if instance_variable_get("@" + fd.name).nil?
                                    q = __cd__.select(fd.name + ".*").where(:id => objectId)
                                    instance_var = q.results.first[fd.name]
                                    instance_variable_set("@" + fd.name, instance_var)
                                end
                                return instance_variable_get("@" + fd.name)
                            end
                        end
                    end

                    klass.class_eval do
                        define_method(fd.name + "=") do |val|
                            if fd.is_a?(AttributeDescriptor)
                                type = fd.dataType
                                if nums.include?(type)
                                    if !val.is_a?(Numeric)
                                        raise ArgumentError, "Arguments to #{fd.name} in #{@name} must be numeric"
                                    end
                                elsif ints.include?(type)
                                    if !val.is_a?(Integer)
                                        raise ArgumentError,  "Arguments to #{fd.name} in #{@name} must be integers"
                                    end
                                elsif bools.include?(type)
                                    if !val.is_a?(TrueClass) && !val.is_a?(FalseClass)
                                        raise ArgumentError,   "Arguments to #{fd.name} in #{@name} must be booleans"
                                    end
                                end
                                instance_variable_set("@" + fd.name, val)
                            else
                                type = fd.referencedType
                                if fd.is_a?(CollectionDescriptor)
                                    instance_var = []
                                    val.each do |item|
                                        if item.is_a?(Hash)
                                            item = type.model.make_new(type.name, item)
                                        end
                                        if !item.is_a?(type)
                                            raise ArgumentError, "Arguments to #{fd.name} in #{@name} must be #{type.name}s"
                                        end
                                        instance_var << item
                                    end
                                    instance_variable_set("@" + fd.name, instance_var)
                                else
                                    if val.is_a?(Hash)
                                        val = type.model.make_new(type.name, val)
                                    end
                                    if !val.is_a?(type)
                                        raise ArgumentError, "Arguments to #{fd.name} in #{@name} must be #{type.name}s"
                                    end
                                    instance_variable_set("@" + fd.name, val)
                                end
                            end
                        end

                    end
                end
                @module = klass
            end
            return @module
        end

        # call-seq:
        #   to_class => Class
        #
        # Returns a Class that can be used to instantiate new objects
        # representing rows of data in the InterMine database.
        #
        def to_class
            if @klass.nil?
                mod = to_module
                kls = Class.new(InterMineObject)
                cd = self
                kls.class_eval do
                    include mod
                    @__cd__ = cd
                end
                @klass = kls
            end
            return @klass
        end

    end

    # A representation of a database column. The characteristics of 
    # these classes are defined by the model information received 
    # from the webservice
    class FieldDescriptor
        include SetHashKey

        # The data model this field descriptor belongs to.
        attr_accessor :model

        # Constructor. 
        #
        # [+opts+]  The hash of parameters received from the webservice
        # [+model+] The parental data model
        #
        def initialize(opts, model) 
            @model = model
            opts.each do |k, v|
                set_key_value(k, v)
            end
        end

    end

    # A class representing columns that contain data.
    class AttributeDescriptor < FieldDescriptor
    end

    # A class representing columns that reference other tables.
    class ReferenceDescriptor < FieldDescriptor
    end

    # A class representing a virtual column that contains multiple references.
    class CollectionDescriptor < ReferenceDescriptor
    end

    # A representation of a path through the data model, starting at a table/class,
    # and descending ultimately to an attribute. A path represents a valid 
    # sequence of joins and column accesses according to the webservice's database schema.
    #
    # In string format, a path can be represented using dotted notation:
    #
    #   Gene.proteins.proteinDomains.name
    #
    # Which is a valid path through the data-model, starting in the gene table, following
    # a reference to the protein table (via x-to-many relationship) and then to the 
    # protein-domain table and then finally to the name column in the protein domain table.
    # Joins are implicitly implied.
    #
    #:include:contact_header.rdoc
    #
    class Path

        # The data model that this path describes.
        attr_reader :model

        # The objects represented by each section of the path. The first is always a ClassDescriptor.
        attr_reader :elements
        
        # The subclass information used to create this path. 
        attr_reader :subclasses
        
        # The root class of this path. This is the same as the first element.
        attr_reader :rootClass

        # Construct a Path
        #
        # The standard mechanism is to parse a string representing a path 
        # with information about the model and the subclasses that are in force. 
        # However, it is also possible to clone a path by passing a Path through
        # as the first element, and also to construct a path from a ClassDescriptor. 
        # In both cases the new Path will inherit the model of the object used to
        # construct it, this avoid the need for a model in these cases.
        #
        def initialize(pathstring, model=nil, subclasses={})
            @model = model
            @subclasses = subclasses
            @elements = []
            @rootClass = nil
            parse(pathstring)
        end

        # call-seq:
        #   end_type => String
        #
        # Return the string that describes the kind of thing this path represents.
        # eg:
        # [+Gene+] "Gene"
        # [+Gene.symbol+] "java.lang.String"
        # [+Gene.proteins+] "Protein"
        #
        def end_type
            last = @elements.last
            if last.is_a?(ClassDescriptor)
                return last.name
            elsif last.respond_to?(:referencedType)
                return last.referencedType.name
            else
                return last.dataType
            end
        end

        # call-seq:
        #   end_cd => ClassDescriptor
        #
        # Return the last ClassDescriptor mentioned in this path. 
        # eg:
        # [+Gene+] Gene
        # [+Gene.symbol+] Gene
        # [+Gene.proteins+] Protein
        # [+Gene.proteins.name+] Protein
        #
        def end_cd
            last = @elements.last
            if last.is_a?(ClassDescriptor)
                return last
            elsif last.respond_to?(:referencedType)
                return last.referencedType
            else
                penult = @elements[-2]
                if penult.is_a?(ClassDescriptor)
                    return penult
                else
                    return penult.referencedType
                end
            end
        end

        # Two paths can be said to be equal when they stringify to the same representation.
        def ==(other)
            return self.to_s == other.to_s
        end

        # Get the number of elements in the path
        def length
            return @elements.length
        end

        # Return the string representation of this path, eg: "Gene.proteins.name"
        def to_s 
            return @elements.map {|x| x.name}.join(".")
        end

        # Returns a string as to_s without the first element. eg: "proteins.name"
        def to_headless_s
            return @elements[1, @elements.size - 1].map {|x| x.name}.join(".")
        end

        # Return true if the Path ends in an attribute
        def is_attribute?
            return @elements.last.is_a?(AttributeDescriptor)
        end

        # Return true if the last element is a class (ie. a path of length 1)
        def is_class?
            return @elements.last.is_a?(ClassDescriptor)
        end

        # Return true if the last element is a reference.
        def is_reference?
            return @elements.last.is_a?(ReferenceDescriptor)
        end

        # Return true if the last element is a collection
        def is_collection?
            return @elements.last.is_a?(CollectionDescriptor)
        end

        private

        # Perform the parsing of the input into a sequence of elements.
        def parse(pathstring)
            if pathstring.is_a?(ClassDescriptor)
                @rootClass = pathstring
                @elements << pathstring
                @model = pathstring.model
                return
            elsif pathstring.is_a?(Path)
                @rootClass = pathstring.rootClass
                @elements = pathstring.elements
                @model = pathstring.model
                @subclasses = pathstring.subclasses
                return
            end

            bits = pathstring.split(".")
            rootName = bits.shift
            @rootClass = @model.get_cd(rootName)
            if @rootClass.nil?
                raise PathException.new(pathstring, subclasses, "Invalid root class '#{rootName}'")
            end

            @elements << @rootClass
            processed = [rootName]

            current_cd = @rootClass

            while (bits.length > 0)
                this_bit = bits.shift
                fd = current_cd.get_field(this_bit)
                if fd.nil?
                    subclassKey = processed.join(".")
                    if @subclasses.has_key?(subclassKey)
                        subclass = model.get_cd(@subclasses[subclassKey])
                        if subclass.nil?
                            raise PathException.new(pathstring, subclasses,
    "'#{subclassKey}' constrained to be a '#{@subclasses[subclassKey]}', but that is not a valid class in the model")
                        end
                        current_cd = subclass
                        fd = current_cd.get_field(this_bit)
                    end
                    if fd.nil?
                        raise PathException.new(pathstring, subclasses,
    "giving up at '#{subclassKey}.#{this_bit}'. Could not find '#{this_bit}' in '#{current_cd}'")
                    end
                end
                @elements << fd
                if fd.respond_to?(:referencedType)
                    current_cd = fd.referencedType
                elsif bits.length > 0
                    raise PathException.new(pathstring, subclasses, 
    "Attributes must be at the end of the path. Giving up at '#{this_bit}'")
                else
                    current_cd = nil
                end
                processed << this_bit
            end
        end
    end

    # An exception class for handling path parsing errors.
    class PathException < RuntimeError

        attr_reader :pathstring, :subclasses

        def initialize(pathstring=nil, subclasses={}, message=nil)
            @pathstring = pathstring
            @subclasses = subclasses
            @message = message
        end

        # The string representation.
        def to_s
            if @pathstring.nil?
                if @message.nil?
                    return self.class.name
                else
                    return @message
                end
            end
            preamble = "Unable to resolve '#{@pathstring}': "
            footer = " (SUBCLASSES => #{@subclasses.inspect})"
            if @message.nil?
                return preamble + footer
            else
                return preamble + @message + footer
            end
        end
    end
end
end

