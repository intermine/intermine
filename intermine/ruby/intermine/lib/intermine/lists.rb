require 'rubygems'
require 'net/http'
require 'uri'
require "stringio"
require "cgi"
require 'json'
require "intermine/service"

include InterMine

# == List Management Tools
#
# Classes that deal with the creation and management
# of a user's saved lists in an individual webservice.
#
module InterMine::Lists

    #
    # == Synopsis
    #
    #  list = service.create_list(%{h eve H bib zen}, "Gene")
    #  list.name = "My new list of genes" # Updates name on server
    #  puts list.size                     # 5
    #  list.each do |gene|                # Inspect the contents
    #    puts gene.name
    #  end
    #
    #  list << "Hox"                      # Append an element
    #  puts list.size
    #
    # == Description 
    #
    # A representation of a saved list in the account of an
    # individual user of an InterMine service. Lists represent
    # homogenous collections of objects, which are themselves
    # linked to records in the data-warehouse. A list behaves
    # much as a normal Array would: it has a size, and can be 
    # processed with each and map, and allows for positional
    # access with list[idx]. In addition, as this list is 
    # backed by its representation in the webapp, it has a name, 
    # and description, as well as a type. Any changes to the list,
    # either in its contents or by renaming, are reflected in the 
    # stored object.
    #
    #:include:contact_header.rdoc
    #
    class List

        include Enumerable

        # The name of the list. This can be changed at any time.
        attr_reader :name

        # The title of the list. This is fixed.
        attr_reader :title
        
        # An informative description.
        attr_reader :description
        
        # The kind of object this list holds
        attr_reader :type
        
        # The number of elements in the list
        attr_reader :size
        
        # The date that this list was originally created
        attr_reader :dateCreated

        # The upgrade status of this list
        # Anything other than current means this list
        # needs to be manually curated.
        attr_reader :status
        
        # The categories associated with this list
        attr_reader :tags
        
        # Any ids used to construct this list that did not match any objects in the database
        attr_reader :unmatched_identifiers

        # Construct a new list with details from the webservice.
        #
        # This method is called internally. You will not need to construct
        # new list objects directly.
        #
        # Arguments:
        # [+details+] The information about this list received from the webservice.
        # [+manager+] The object responsible for keeping track of all the known lists
        #
        #   list = List.new({"name" => "Foo"}, manager)
        #
        def initialize(details, manager=nil)
            @manager = manager
            details.each {|k,v| instance_variable_set('@' + k, v)}
            @unmatched_identifiers = []
            @tags ||= []
        end

        # True if the list has no elements.
        def empty?
            @size == 0
        end

        # True if the list can be changed by the current user.
        #
        #  if list.is_authorized?
        #    list.remove("h")
        #  end
        #
        def is_authorized?
            return @authorized.nil? ? true : @authorized
        end

        # Returns the first element in the list. The order elements
        # are returned in depends on the fields that its class has.
        # It is not related to the order of the identifiers given at creation.
        #
        #   puts list.first.symbol
        #
        def first
            if @size > 0
                return self[0]
            else
                return nil
            end
        end

        # Retrieve an element at a given position. Negative indices 
        # count from the end of the list. 
        #
        #   puts list[2].length
        #   puts list[-1].length
        #
        def [](index)
            if index < 0
                index = @size + index
            end
            unless index < @size && index >= 0
                return nil
            end
            return query.first(index)
        end

        # Retrieve the object at the given index, or raise an IndexError, unless
        # a default is supplied, in which case that is returned instead.
        #
        #   gene = list.fetch(6)  # Blows up if the list has only 6 elements or less
        #
        def fetch(index, default=nil)
            if index < 0
                index = @size + index
            end
            unless index < @size && index >= 0
                if default
                    return default
                else
                    raise IndexError, "#{index} is not a suitable index for this list"
                end
            end
            return query.first(index)
        end

        # Apply the given block to each element in the list. Return the list.
        #  
        #   list.each do |gene|
        #     puts gene.symbol
        #   end
        #  
        def each
            query.results.each {|r| yield r}
            return self
        end

        # Used to create a new list from the contents of this one. This can be used
        # to define a sub-list
        #
        #   sub_list = service.create_list(list.list_query.where(:length => {"<" => 500}))
        #
        def list_query
            return @manager.service.query(@type).select(@type + '.id').where(@type => self)
        end

        # A PathQuery::Query with all attributes selected for output, and restricted
        # to the content of this list. This object is used to fetch elements for other 
        # methods. This can be used for composing further filters on a list, or for
        # adding other attributes for output.
        #
        #   list.query.select("pathways.*").each_result do |gene|
        #     puts "#{gene.symbol}: #{gene.pathways.map {|p| p.identifier}.inspect}"
        #   end
        #
        def query
            return @manager.service.query(@type).where(@type => self)
        end

        # Returns a simple, readable representation of the list
        #
        #   puts list
        #   => "My new list: 5 genes"
        #
        def to_s
            return "#{@name}: #{@size} #{@type}s"
        end

        # Returns a detailed representation of the list, useful for debugging.
        def inspect
            return "<#{self.class.name} @name=#{@name.inspect} @size=#{@size} @type=#{@type.inspect} @description=#{@description.inspect} @title=#{@title.inspect} @dateCreated=#{@dateCreated.inspect} @authorized=#{@authorized.inspect} @tags=#{@tags.inspect}>"
        end

        # Update the name of the list, making sure that the name is also 
        # changed in the respective service.
        #
        #   list.name = "My new list"
        #
        # If a list is created without a name, it will be considered as a "temporary"
        # list until it is given one. All temporary lists are deleted from the webapp 
        # when the program exits.
        #
        def name=(new_name)
            return if (@name == new_name)
            uri = URI.parse(@manager.service.root + Service::LIST_RENAME_PATH)
            params = @manager.service.params.merge("oldname" => @name, "newname" => new_name)
            res = Net::HTTP.post_form(uri, params)
            @manager.process_list_creation_response(res)
            @name = new_name
        end

        # Delete this list from the webservice. After this method is called this object
        # should not be used again, and any attempt to do so will cause errors.
        def delete
            @manager.delete_lists(self)
            @size = 0
            @name = nil
        end

        # Add other items to this list. The other items can be identifiers in the same
        # form as were used to create this list orginally (strings, or arrays or files).
        # Or other lists or queries can be used to add items to the list. Any combination of these
        # elements is possible.
        #
        #   list.add("Roughened", other_list, a_query)
        #
        def add(*others)
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

        # Add the other item to the list, exactly as in List#add
        def <<(other)
            return add(other)
        end

        # Remove items as specified by the arguments from this list. As in 
        # List#add these others can be identifiers specified by strings or arrays
        # or files, or other lists or queries. 
        #
        #  list.remove("eve", sub_list)
        #
        # If the items were not in the list in the first place, no error will be raised, 
        # and the size of this list will simply not change.
        #
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

        # Add tags to the list
        #
        # Updates the current tags by adding tags to the list.
        #
        def add_tags(*tags)
            @tags = @manager.add_tags(self, tags)
        end

        # Remove one or more tags from the list
        #
        # If the tags are not currently associated with the list, 
        # they will be ignored. 
        #
        def remove_tags(*tags)
            to_remove = tags.select {|t| @tags.include? t}
            unless to_remove.empty?
                @tags = @manager.remove_tags(self, to_remove)
            end
        end

        # Update this lists tags with the current tags on the server.
        def update_tags
            @tags = @manager.tags_for(self)
        end

        ENRICHMENT_DEFAULTS = {:correction => "Holm-Bonferroni", :maxp => 0.05}

        # Retrieve the results of an enrichment calculation
        #
        # Get the results of an enrichment calculate_enrichment with the default parameter values:
        #
        #  calculate_enrichment(widget)
        #
        # Pass optional parameters to the enrichment service:
        #
        #  calculate_enrichment(widget, :correction => "Benjamini-Hochberg", :maxp => 0.1)
        #
        # The optional parameters are: :correction, :maxp, :filter
        #
        # Each result returned by the enrichment service is a hash with the following
        # keys: "p-value", "identifier", "matches", "description". The Hash returned also allows
        # key access with symbols.
        #
        def calculate_enrichment(widget, opts = {})
            s = @manager.service
            params = s.params.merge(ENRICHMENT_DEFAULTS).merge(opts).merge(:widget => widget, :list => @name)
            uri = URI.parse(s.root + Service::LIST_ENRICHMENT_PATH)
            res = Net::HTTP.post_form(uri, params)
            return case res
            when Net::HTTPSuccess
                JSON.parse(res.body)["results"].map {|row| SymbolAcceptingHash[row] }
            else
                begin
                    container = JSON.parse(res.body)
                    raise ServiceError, container["error"]
                rescue
                    res.error!
                end
            end
        end

        class SymbolAcceptingHash < Hash

            def [](key)
                return case key
                when Symbol
                    return super(key.to_s.gsub(/[_]/, '-'))
                else
                    return super(key)
                end
            end
        end

        private

        # Used to interpret arguments to add and remove
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

        # Used to handle the responses returned by queries used to update the list 
        # by add and remove
        def handle_response(res)
            new_list = @manager.process_list_creation_response(res)
            @unmatched_identifiers += new_list.unmatched_identifiers
            @size = new_list.size
        end

        # Add a List to this List
        def append_list(list)
            q = (list.is_a?(List)) ? list.list_query : list
            params = q.params.merge(@manager.service.params).merge("listName" => @name)
            uri = URI.parse(q.list_append_uri)
            res = Net::HTTP.post_form(uri, params)
            handle_response(res)
        end

        # Add a collection of lists and queries to this List
        def append_lists(lists)
            addendum = @manager.union_of(lists)
            append_list(addendum)
        end

        # Add items defined by ids to this list
        def append_ids(ids)
            uri = URI.parse(@manager.service.root + Service::LIST_APPEND_PATH)
            params = {"name" => @name}
            if ids.is_a?(File)
                f = ids
            elsif ids.is_a?(Array)
                f = StringIO.new(ids.map {|x| '"' + x.gsub(/"/, '\"') + '"'}.join(" "))
            elsif File.readable?(ids.to_s)
                f = File.open(ids, "r")
            else
                f = StringIO.new(ids.to_s)
            end
            req = Net::HTTP::Post.new(uri.path + "?" + @manager.params_to_query_string(params))
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

    # == Synopsis 
    #
    # An internal class for managing lists throughout the lifetime of a program. 
    # The main Service object delegates list functionality to this class.
    # 
    #   # Creation
    #   list = service.create_list("path/to/some/file.txt", "Gene", "my-favourite-genes")
    #   # Retrieval
    #   other_list = service.list("my-previously-saved-list")
    #   # Combination
    #   intersection = service.intersection_of([list, other_list])
    #   # Deletion
    #   service.delete_lists(list, other_list)
    #
    # == Description 
    #
    # This class contains logic for reading and updating the lists available 
    # to a given user at a webservice. This class in particular is responsible for 
    # parsing list responses, and performing the operations that combine lists into 
    # new result sets (intersection, union, symmetric difference, subtraction).
    #
    #:include:contact_header.rdoc
    #
    class ListManager

        # The name given by default to all lists you do not explicitly name
        #
        #  l = service.create_list("genes.txt", "Gene")
        #  puts l.name
        #  => "my_list_1"
        #
        DEFAULT_LIST_NAME = "my_list"

        # The description given by default to all new lists for which you 
        # do not provide a description explicitly. The purpose of this
        # is to help you identify automatically created lists in you profile.
        DEFAULT_DESCRIPTION = "Created with InterMine Ruby Webservice Client"

        # The service this manager belongs to
        attr_reader :service
        
        # The temporary lists created in this session. These will be deleted 
        # at program exit.
        attr_reader :temporary_lists

        # Construct a new ListManager. 
        #
        # You will never need to call this constructor yourself.
        #
        def initialize(service)
            @service = service
            @lists = {}
            @temporary_lists = []
            do_at_exit(self)
        end


        # Get the lists currently available in the webservice. 
        def lists
            refresh_lists
            return @lists.values
        end

        # Get the names of the lists currently available in the webservice
        def list_names
            refresh_lists
            return @lists.keys.sort
        end

        # Get a list by name. Returns nil if the list does not exist.
        def list(name)
            refresh_lists
            return @lists[name]
        end

        # Gets all lists with the given tags. If more than one tag is supplied, 
        # then a list must have all given tags to be returned.
        #
        #   tagged = service.get_lists_with_tags("tagA", "tagB")
        #
        def get_lists_with_tags(*tags)
            return lists.select do |l|
                union = l.tags | tags
                union.size == l.tags.size
            end
        end

        # Update the stored record of lists. This method is 
        # called before all list retrieval methods.
        def refresh_lists
            lists = JSON.parse(@service.get_list_data)
            @lists = {}
            lists["lists"].each {|hash| 
                l = List.new(hash, self)
                @lists[l.name] = l
            }
        end
        
        # === Create a new List with the given content.
        #
        # Creates a new List and stores it on the appropriate 
        # webservice:
        #
        # Arguments:
        # [+content+] Can be a string with delimited identifiers, an Array of 
        #             identifiers, a File object containing identifiers, or 
        #             a name of an unopened readable file containing identifiers.
        #             It can also be another List (in which case the list is cloned)
        #             or a query that describes a result set.
        # [+type+]    Required when identifiers are being given (but not when the 
        #             content is a PathQuery::Query or a List. This should be the kind
        #             of object to look for (such as "Gene").
        # [+tags+]    An Array of tags to apply to the new list. If a list is supplied as
        #             the content, these tags will be added to the existing tags.
        # [+name+]    The name of the new list. One will be generated if none is provided.
        #             Lists created with generated names are considered temporary and will
        #             be deleted upon program exit.
        # [+description+] An informative description of the list
        #
        #   # With Array of Ids
        #   list = service.create_list(%{eve bib zen}) 
        #
        #   # From a file
        #   list = service.create_list("path/to/some/file.txt", "Gene", [], "my-stored-genes")
        #
        #   # With a query
        #   list = service.create_list(service.query("Gene").select(:id).where(:length => {"<" => 500}))
        #
        #
        def create_list(content, type=nil, tags=[], name=nil, description=nil)
            name ||= get_unused_list_name
            description ||= DEFAULT_DESCRIPTION

            if content.is_a?(List) 
                tags += content.tags
                response = create_list_from_query(content.list_query, tags, name, description)
            elsif content.respond_to?(:list_upload_uri)
                response = create_list_from_query(content, tags, name, description)
            else 
                response = create_list_from_ids(content, type, tags, name, description)
            end

            return process_list_creation_response(response)
        end

        # Deletes the given lists from the webservice. The lists can be supplied
        # as List objects, or as their names as Strings.
        #
        # Raises errors if problems occur with the deletion of these lists, including 
        # if the lists do not exist.
        #
        def delete_lists(*lists)
            lists.map {|x| x.is_a?(List) ? x.name : x.to_s}.uniq.each do |name|
                uri = URI.parse(@service.root + Service::LISTS_PATH)
                params = {"name" => name}
                req = Net::HTTP::Delete.new(uri.path + "?" + params_to_query_string(params))
                res = Net::HTTP.start(uri.host, uri.port) do |http|
                    http.request(req)
                end
                check_response_for_error(res)
            end
            refresh_lists
        end

        # Create a new list in the webservice from the symmetric difference of two 
        # or more lists, and return a List object that represents it.
        #
        # See ListManager#create_list for an explanation of tags, name and description
        def symmetric_difference_of(lists=[], tags=[], name=nil, description=nil)
            do_commutative_list_operation(Service::LIST_DIFFERENCE_PATH, "Symmetric difference", lists, tags, name, description)
        end

        # Create a new list in the webservice from the union of two
        # or more lists, and return a List object that represents it.
        #
        # See ListManager#create_list for an explanation of tags, name and description
        def union_of(lists=[], tags=[], name=nil, description=nil)
            do_commutative_list_operation(Service::LIST_UNION_PATH, "Union", lists, tags, name, description)
        end

        # Create a new list in the webservice from the intersection of two
        # or more lists, and return a List object that represents it.
        #
        # See ListManager#create_list for an explanation of tags, name and description
        def intersection_of(lists=[], tags=[], name=nil, description=nil)
            do_commutative_list_operation(Service::LIST_INTERSECTION_PATH, "Intersection", lists, tags, name, description)
        end

        # Create a new list in the webservice by subtracting all the elements in the 
        # 'delenda' lists from all the elements in the 'reference' lists,
        # and return a List object that represents it.
        #
        # See ListManager#create_list for an explanation of tags, name and description
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

        # Common code to all list requests for interpreting the response 
        # from the webservice. 
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

        # Add tags to a list. 
        #
        # Returns the current tags
        #
        def add_tags(list, *tags)
            uri = URI.parse(@service.root + Service::LIST_TAG_PATH)
            params = @service.params.merge("name" => list.name, "tags" => tags.join(";"))
            res = Net::HTTP.post_form(uri, params)
            check_response_for_error(res)
            return JSON.parse(res.body)["tags"]
        end

        # Remove tags from a list
        #
        # Returns the current tags
        #
        def remove_tags(list, *tags)
            uri = URI.parse(@service.root + Service::LIST_TAG_PATH)
            params = @service.params.merge(
                "name" => list.name, 
                "tags" => tags.join(";")
            )
            req_path = uri.path + "?" + params_to_query_string(params)
            req = Net::HTTP::Delete.new(req_path)
            res = Net::HTTP.start(uri.host, uri.port) do |http|
                http.request(req)
            end
            check_response_for_error(res)
            return JSON.parse(res.body)["tags"]
        end

        # Get the current tags for a list
        def tags_for(list)
            uri = URI.parse(@service.root + Service::LIST_TAG_PATH)
            params = @service.params.merge(
                "name" => list.name
            )
            req_path = uri.path + "?" + params_to_query_string(params)
            res = Net::HTTP.start(uri.host, uri.port) {|http|
                  http.get(req_path)
            }
            check_response_for_error(res)
            return JSON.parse(res.body)["tags"]
        end

        # only handles single value keys!
        def params_to_query_string(p)
            return @service.params.merge(p).map { |k,v| "#{k}=#{CGI::escape(v.to_s)}" }.join('&')
        end

        private 

        # Clean up after itself by deleting 
        # any temporary lists left lying about
        def do_at_exit(this)
            at_exit do 
                unless this.temporary_lists.empty?
                    this.lists.each do |x|
                        begin
                            x.delete if this.temporary_lists.include?(x.name)
                        rescue
                            # Ignore errors here.
                        end
                    end
                end
            end
        end

        # Transform a collection of objects containing Lists, Queries and Strings
        # into a collection of Strings with list names.
        #
        # Raises errors if an object cannot be resolved to an accessible
        # list.
        def make_list_names(objs)
            current_names = list_names
            return objs.map do |x|
                case x
                when List
                    x.name
                when x.respond_to?(:list_upload_uri)
                    create_list(x).name
                when current_names.include?(x.to_s)
                    x.to_s
                else
                    raise ArgumentError, "#{x} is not a list you can access"
                end
            end
        end

        # Common code behind the operation of union, intersection and symmetric difference operations.
        def do_commutative_list_operation(path, operation, lists, tags=[], name=nil, description=nil)
            list_names = make_list_names(lists)
            name ||= get_unused_list_name
            description ||= "#{operation} of #{list_names[0 .. -2].join(", ")} and #{list_names.last}"

            uri = URI.parse(@service.root + path)
            params = @service.params.merge(
                "name" => name, "lists" => list_names.join(";"), 
                "description" => description, "tags" => tags.join(';')
            )
            res = Net::HTTP::post_form(uri, params)
            return process_list_creation_response(res)
        end

        # Error checking routine.
        def check_response_for_error(response)
            case response
            when Net::HTTPSuccess
                # Ok
            else
                begin
                    container = JSON.parse(response.body)
                    raise ServiceError, container["error"]
                rescue
                    response.error!
                end
            end
        end

        # Routine for creating a list from a PathQuery::Query
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

        # Routine for creating a List in a webservice from a list of Ids.
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
            if ids.is_a?(File)
                f = ids
            elsif ids.is_a?(Array)
                f = StringIO.new(ids.map {|x| '"' + x.gsub(/"/, '\"') + '"'}.join(" "))
            elsif File.readable?(ids.to_s)
                f = File.open(ids, "r")
            else
                f = StringIO.new(ids.to_s)
            end
            req = Net::HTTP::Post.new(uri.path + "?" + params_to_query_string(list_params))
            req.body_stream = f
            req.content_type = "text/plain"
            req.content_length = f.size

            res = Net::HTTP.start(uri.host, uri.port) do |http|
                http.request(req)
            end
            f.close
            return res
        end

        # Helper to get an unused default name
        def get_unused_list_name(no=1)
            name = DEFAULT_LIST_NAME + "_" + no.to_s
            names = list_names
            while names.include?(name)
                no += 1
                name = DEFAULT_LIST_NAME + "_" + no.to_s
            end
            @temporary_lists.push(name)
            return name
        end

    end
end
