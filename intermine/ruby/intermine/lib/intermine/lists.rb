require 'rubygems'
require 'json'
require 'net/http'
require 'uri'
require "stringio"
require "intermine/service"
require 'addressable/uri'

module Lists
    class List

        attr_reader :name, :title, :description, :type, :size, 
            :dateCreated, :tags, :unmatched_identifiers

        def initialize(details, manager=nil)
            @manager = manager
            details.each {|k,v| instance_variable_set('@' + k, v)}
            @unmatched_identifiers = []
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

        def list_query
            return @manager.service.query(@type).select(@type + '.id').where(@type => self)
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

        def name=(new_name)
            return if (@name == new_name)
            uri = URI.parse(@manager.service.root + Service::LIST_RENAME_PATH)
            params = @manager.service.params.merge("oldname" => @name, "newname" => new_name)
            res = Net::HTTP.post_form(uri, params)
            @manager.process_list_creation_response(res)
            @name = new_name
        end

        def delete
            @manager.delete_lists(self)
            @size = 0
            @name = nil
        end

        def append(*others)
            unionables, ids = classify_others(others)
            unless unionables.empty?
                if unionables.size == 1
                    append_list(unionables.first)
                else
                    append_lists(unionables)
                end
            end
            unless ids.empty?
                ids.each {|x| append_ids(x)}
            end
            return self
        end

        def <<(other)
            return append(other)
        end

        def remove(*others)
            unionables, ids = classify_others(others)
            unless ids.empty?
                unionables += ids.map {|x| @manager.create_list(x, @type)}
            end
            unless unionables.empty?
                myname = @name
                new_list = @manager.subtract([self], unionables, @tags, nil, @description)
                self.delete
                @size = new_list.size
                @name = new_list.name
                @description = new_list.description
                @dateCreated = new_list.dateCreated
                @tags = new_list.tags
                self.name = myname
            end
            return self
        end

        private

        def classify_others(others)
            unionables = []
            ids = []
            others.each do |o|
                case o
                when List
                    unionables << o
                when o.respond_to?(:list_upload_uri)
                    unionables << o
                else 
                    ids << o
                end
            end
            return [unionables, ids]
        end

        def handle_response(res)
            new_list = @manager.process_list_creation_response(res)
            @unmatched_identifiers += new_list.unmatched_identifiers
            @size = new_list.size
        end

        def append_list(list)
            q = (list.is_a?(List)) ? list.list_query : list
            params = q.params.merge(@manager.service.params).merge("listName" => @name)
            uri = URI.parse(q.list_append_uri)
            res = Net::HTTP.post_form(uri, params)
            handle_response(res)
        end

        def append_lists(lists)
            addendum = @manager.union_of(lists)
            append_list(addendum)
        end

        def append_ids(ids)
            uri = URI.parse(@manager.service.root + Service::LIST_APPEND_PATH)
            params = @manager.service.params.merge("name" => @name)
            if ids.is_a?(File)
                f = ids
            elsif ids.is_a?(Array)
                f = StringIO.new(ids.map {|x| '"' + x.gsub(/"/, '\"') + '"'}.join(" "))
            elsif File.readable?(ids.to_s)
                f = File.open(ids, "r")
            else
                f = StringIO.new(ids.to_s)
            end
            req = Net::HTTP::Post.new(uri.path + "?" + Addressable::URI.form_encode(params))
            req.body_stream = f
            req.content_type = "text/plain"
            req.content_length = f.size

            res = Net::HTTP.start(uri.host, uri.port) do |http|
                http.request(req)
            end
            handle_response(res)
            f.close
        end

    end

    class ListManager

        DEFAULT_LIST_NAME = "my_list"
        DEFAULT_DESCRIPTION = "Created with InterMine Ruby Webservice Client"

        attr_reader :service, :temporary_lists

        def initialize(service)
            @service = service
            @lists = {}
            @temporary_lists = []
            do_at_exit(self)
        end

        def do_at_exit(this)
            at_exit do 
                unless this.temporary_lists.empty?
                    this.lists.each do |x|
                        if this.temporary_lists.include?(x.name)
                            x.delete
                        end
                    end
                end
            end
        end

        def lists
            refresh_lists
            return @lists.values
        end

        def list_names
            refresh_lists
            return @lists.keys
        end

        def list(name)
            refresh_lists
            return @lists[name]
        end

        def refresh_lists
            lists = JSON.parse(@service.get_list_data)
            @lists = {}
            lists["lists"].each {|hash| 
                l = List.new(hash, self)
                @lists[l.name] = l
            }
        end
        
        def create_list(content, type=nil, tags=[], name=nil, description=nil)
            name ||= get_unused_list_name
            description ||= DEFAULT_DESCRIPTION

            if content.is_a?(List) 
                response = create_list_from_query(content.list_query, tags, name, description)
            elsif content.respond_to?(:list_upload_uri)
                response = create_list_from_query(content, tags, name, description)
            else 
                response = create_list_from_ids(content, type, tags, name, description)
            end

            return process_list_creation_response(response)
        end

        def delete_lists(*lists)
            lists.map {|x| x.is_a?(List) ? x.name : x.to_s}.uniq.each do |name|
                uri = URI.parse(@service.root + Service::LISTS_PATH)
                params = @service.params.merge({"name" => name})
                req = Net::HTTP::Delete.new(uri.path + "?" + Addressable::URI.form_encode(params))
                res = Net::HTTP.start(uri.host, uri.port) do |http|
                    http.request(req)
                end
                check_response_for_error(res)
            end
            refresh_lists
        end

        def symmetric_difference_of(lists=[], tags=[], name=nil, description=nil)
            do_commutative_list_operation(Service::LIST_DIFFERENCE_PATH, "Symmetric difference", lists, tags, name, description)
        end

        def union_of(lists=[], tags=[], name=nil, description=nil)
            do_commutative_list_operation(Service::LIST_UNION_PATH, "Union", lists, tags, name, description)
        end

        def intersection_of(lists=[], tags=[], name=nil, description=nil)
            do_commutative_list_operation(Service::LIST_INTERSECTION_PATH, "Intersection", lists, tags, name, description)
        end

        def subtract(references=[], delenda=[], tags=[], name=nil, description=nil)
            ref_names = make_list_names(references)
            del_names = make_list_names(delenda)
            name ||= get_unused_list_name
            description ||= "Subtraction of #{del_names[0 .. -2].join(", ")} and #{del_names.last} from #{ref_names[0 .. -2].join(", ")} and #{ref_names.last}"
            uri = URI.parse(@service.root + Service::LIST_SUBTRACTION_PATH)
            params = @service.params.merge("name" => name, "description" => description, "references" => ref_names.join(';'),
                                           "subtract" => del_names.join(';'), "tags" => tags.join(';'))
            res = Net::HTTP.post_form(uri, params)
            return process_list_creation_response(res)
        end

        def process_list_creation_response(response)
            check_response_for_error(response)
            new_list = JSON.parse(response.body)
            new_name = new_list["listName"]
            failed_matches = new_list["unmatchedIdentifiers"] || []
            refresh_lists
            ret = list(new_name)
            ret.unmatched_identifiers.replace(failed_matches)
            return ret
        end

        private 

        def make_list_names(lists)
            return lists.map do |x|
                case x
                when List
                    x.name
                when x.respond_to?(:list_upload_uri)
                    create_list(x).name
                else
                    x.to_s
                end
            end
        end

        def do_commutative_list_operation(path, operation, lists, tags=[], name=nil, description=nil)
            list_names = make_list_names(lists)
            name ||= get_unused_list_name
            description ||= "#{operation} of #{list_names[0 .. -2].join(", ")} and #{list_names.last}"

            uri = URI.parse(@service.root + path)
            params = @service.params.merge("name" => name, "lists" => list_names.join(";"), "description" => description)
            res = Net::HTTP::post_form(uri, params)
            return process_list_creation_response(res)
        end

        def check_response_for_error(response)
            case response
            when Net::HTTPSuccess
                # Ok
            else
                begin
                    container = JSON.parse(response.body)
                    raise ServiceError, container.error
                rescue
                    response.error!
                end
            end
        end

        def create_list_from_query(query, tags=[], name=nil, description=nil)
            uri = query.list_upload_uri
            list_params = {
                "listName" => name,
                "description" => description,
                "tags" => tags.join(";")
            }
            service_params = @service.params
            params = query.params.merge(list_params).merge(service_params)
            return Net::HTTP.post_form(URI.parse(uri), params)
        end


        def create_list_from_ids(ids, type, tags=[], name=nil, description=nil)
            if @service.model.get_cd(type).nil?
                raise ArgumentError, "Invalid type (#{type.inspect})"
            end
            uri = URI.parse(@service.root + Service::LISTS_PATH)
            list_params = {
                "name" => name,
                "description" => description,
                "tags" => tags.join(";"),
                "type" => type
            }
            params = @service.params.merge(list_params)
            if ids.is_a?(File)
                f = ids
            elsif ids.is_a?(Array)
                f = StringIO.new(ids.map {|x| '"' + x.gsub(/"/, '\"') + '"'}.join(" "))
            elsif File.readable?(ids.to_s)
                f = File.open(ids, "r")
            else
                f = StringIO.new(ids.to_s)
            end
            req = Net::HTTP::Post.new(uri.path + "?" + Addressable::URI.form_encode(params))
            req.body_stream = f
            req.content_type = "text/plain"
            req.content_length = f.size

            res = Net::HTTP.start(uri.host, uri.port) do |http|
                http.request(req)
            end
            f.close
            return res
        end

        def get_unused_list_name(no=1, names=nil)
            name = DEFAULT_LIST_NAME + "_" + no.to_s
            names ||= list_names
            if names.include?(name)
                return get_unused_list_name(no + 1, names)
            else
                @temporary_lists.push(name)
                return name
            end
        end

    end
end
