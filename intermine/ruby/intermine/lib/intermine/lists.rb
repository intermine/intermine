require 'rubygems'
require 'json'

module Lists
    class List

        attr_reader :name, :title, :description, :type, :size, 
            :dateCreated, :tags

        def initialize(details, manager=nil)
            @manager = manager
            details.each {|k,v| instance_variable_set('@' + k, v)}
            @tags ||= []
        end

        def is_authorized?
            return @authorized.nil? ? true : @authorized
        end

        def [](index)
            if index < 0
                index = @size + index
            end
            unless index < @size && index > 0
                raise IndexError, "#{index} is not a suitable index for this list"
            end
            return query.first(index)
        end

        def each
            query.each_result {|r| yield r}
            return self
        end

        def map 
            ret = []
            query.each_result {|r|
                ret.push(yield r)
            }
            return ret
        end

        def query
            return @manager.service.query(@type).where(@type => self)
        end

        def to_s
            return "#{@name}: #{@size} #{@type}s"
        end

        def inspect
            return "<#{self.class.name} @name=#{@name.inspect} @size=#{@size} @type=#{@type.inspect} @description=#{@description.inspect} @title=#{@title.inspect} @dateCreated=#{@dateCreated.inspect} @authorized=#{@authorized.inspect} @tags=#{@tags.inspect}>"
        end

    end

    class ListManager

        attr_reader :service

        def initialize(service)
            @service = service
        end

        def lists
            refresh_lists
            return @lists.values
        end

        def list(name)
            refresh_lists
            return @lists[name]
        end

        def refresh_lists
            lists = JSON.parse(@service.get_list_data)
            @lists = {}
            lists["lists"].each {|h| 
                l = List.new(h, self)
                @lists[l.name] = l
            }
        end

    end
end
