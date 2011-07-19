require 'rubygems'
require 'json'

class Model

    attr_reader :name, :classes, :service

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

    def table(name)
        return get_cd(name)
    end


    def get_cd(cls)
        if cls.is_a?(ClassDescriptor)
            return cls
        else
            return @classes[cls.to_s]
        end
    end

    def make_new(class_name=nil, opts={})
        # Support calling with just opts
        if class_name.is_a?(Hash)
            opts = class_name
            class_name = nil
        end
        if class_name && opts["class"] && (class_name != opts["class"]) && !get_cd(opts["class"]).subclass_of(class_name)
            raise ArgumentError, "class name in options hash is not compatible with passed class name: #{opts["class"]} is not a subclass of #{class_name}"
        end
        # Prefer the options value to the passed value
        cd_name = opts["class"] || class_name
        cls = get_cd(cd_name).to_class
        return cls.new(opts)
    end

    def resolve_path(obj, path)
        begin
            parts = path.split(".")
        rescue NoMethodError
            parts = path.elements.map { |x| x.name }
        end
        root = parts.shift
        if !obj.is_a?(get_cd(root).to_module)
            raise ArgumentError, "Incompatible path '#{path}': #{obj} is not a #{root}"
        end
        begin
            res = parts.inject(obj) do |memo, part| 
                args = part.split(/[\[\]]/)
                if args.length == 2
                    args[0] = "[]"
                    args[1] = args[1].to_i
                end
                memo.send(*args) 
            end
        rescue NoMethodError => e
            raise ArgumentError, "Incompatible path '#{path}' for #{obj}, #{e}"
        end
        return res
    end

    private

    # For mocking in tests
    def set_service(service)
        @service = service
    end

end

class InterMineObject
    attr_reader :objectId

    def InterMineObject.class_name
        return @class_name
    end

    def initialize(hash=nil)
        hash ||= {}
        hash.each do |key, value|
            if key.to_s != "class"
                self.send(key.to_s + "=", value)
            end
        end
    end

    def is_a?(other)
        if other.is_a?(ClassDescriptor)
            return is_a?(other.to_module)
        else
            return super
        end
    end

    def to_s
        parts = [self.class.class_name + ':' + self.objectId.to_s]
        self.instance_variables.reject{|var| var.to_s.end_with?("objectId")}.each do |var|
            parts << "#{var}=#{self.instance_variable_get(var).inspect}"
        end
        return "<#{parts.join(' ')}>"
    end

    alias inspect to_s

    private 
    
    def objectId=(val)
        @objectId = val
    end
end

module SetHashKey 

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
    end

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

class ClassDescriptor
    include SetHashKey

    attr_accessor :model, :fields

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

    def new_query
        q = @model.service.new_query(self.name)
        return q
    end

    def select(*cols)
        q = new_query
        q.add_views(cols)
        return q
    end

    def where(*args)
        q = new_query
        q.select("*")
        q.where(*args)
        return q
    end

    def get_field(name)
        return @fields[name]
    end

    def attributes
        return @fields.select {|k, v| v.is_a?(AttributeDescriptor)}.map {|pair| pair[1]}
    end

    def to_s 
        return "<#{self.class.name}:#{self.object_id} #{self.model.name}.#{@name}>"
    end

    def subclass_of(other)
        path = Path.new(other, @model)
        if @extends.include? path.end_type
            return true
        else
            @extends.each do |x|
                superCls = @model.get_cd(x)
                if superCls.subclass_of(path)
                    return true
                end
            end
        end
        return false
    end

    def to_module
        if @module.nil?
            nums = ["Float", "Double", "float", "double"]
            ints = ["Integer", "int"]
            bools = ["Boolean", "boolean"]

            supers = @extends.map { |x| @model.get_cd(x).to_module }

            klass = Module.new
            fd_names = @fields.values.map { |x| x.name }
            klass.class_eval do
                include *supers
                attr_reader *fd_names

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

    def to_class
        if @klass.nil?
            mod = to_module
            kls = Class.new(InterMineObject)
            class_n = self.name
            kls.class_eval do
                include mod
                @class_name = class_n
            end
            @klass = kls
        end
        return @klass
    end

end

class FieldDescriptor
    include SetHashKey

    attr_accessor :model

    def initialize(opts, model) 
        @model = model
        opts.each do |k, v|
            set_key_value(k, v)
        end
    end

end

class AttributeDescriptor < FieldDescriptor
end

class ReferenceDescriptor < FieldDescriptor
end

class CollectionDescriptor < ReferenceDescriptor
end

class Path

    attr_accessor :model, :elements, :subclasses, :rootClass

    def initialize(pathstring, model=nil, subclasses={})
        @model = model
        @subclasses = subclasses
        @elements = []
        @rootClass = nil
        parse(pathstring)
    end

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

    def ==(other)
        return self.to_s == other.to_s
    end

    def length
        return @elements.length
    end

    def to_s 
        return @elements.map {|x| x.name}.join(".")
    end

    def to_headless_s
        return @elements[1, @elements.size - 1].map {|x| x.name}.join(".")
    end

    def is_attribute
        return @elements.last.is_a(AttributeDescriptor)
    end

    def is_class
        return @elements.last.is_a(ClassDescriptor)
    end

    def is_reference
        return @elements.last.is_a(ReferenceDescriptor)
    end

    def is_collection
        return @elements.last.is_a(CollectionDescriptor)
    end

    private

    def parse(pathstring)
        if pathstring.is_a?(ClassDescriptor)
            @rootClass = pathstring
            @elements << pathstring
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

class PathException < RuntimeError

    attr_reader :pathstring, :subclasses

    def initialize(pathstring=nil, subclasses={}, message=nil)
        @pathstring = pathstring
        @subclasses = subclasses
        @message = message
    end

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




