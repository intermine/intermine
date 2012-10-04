require 'rubygems'
require 'intermine/model'
require "intermine/query"
require "intermine/lists"
require "rexml/document"
require "forwardable"
require "cgi"

module InterMine

    # A Representation of Connection to an InterMine Webservice
    # 
    # == Synopsis
    #
    #   require "intermine/service"
    #   service = Service.new("www.flymine.org/query")
    #
    #   query = service.query("Gene")
    #   list = service.list("My-Favourite-List")
    #   template = service.template("Probe_Gene")
    #   model = service.model
    #
    # == Description
    #
    # The service object is the gateway to all InterMine webservice
    # functionality, and the mechanism by which resources such as 
    # queries, lists, templates and models can be obtained. In itself, it
    # doen't do much, but it facilitates all the operations that can be achieved 
    # with the InterMine API.
    #
    # == Using Queries
    #
    #   service.query("Gene").
    #           select("*", "proteins.proteinDomains.*").
    #           where(:symbol => %{h bib r eve zen}).
    #           order_by(:molecularWeight).
    #           each_result do |gene|
    #             handle_result(gene)
    #           end
    #
    # Queries are arbitrarily complex requests for data over the whole resources
    # of the data-warehouse, similar in scope and design to the SQL queries that
    # they are eventually run as in the webservice. The PathQuery::Query object
    # can be obtained directly from the service with the Service#query constructor.
    # See PathQuery::Query for more details.
    #
    # == Using Templates
    #
    #   service.template("Probe_Genes").each_row(:A => "probeid") do |row|
    #     puts row["gene.primaryIdentifier"]
    #   end
    #
    # Templates are a simpler way of running queries, as they are mostly 
    # predefined, and simply require a parameter or two to be changed to get 
    # different results. They are an effective way of saving and repeating workflows, 
    # and can be precomputed to improve response times for queries you run frequently.
    #
    # See PathQuery::Template
    #
    # == Using Lists
    #
    #   new_list = service.create_list("my/gene_list.txt", "Gene", ["experimentA", "projectB"])
    #   existing_list = service.list("Genes I'm interested in")
    #   intersection = new_list & existing_list
    #   intersection.name = "My genes from experimentA"
    #
    # Lists are saved result-sets and user curated collections of objects 
    # of a particular type. You may have a list of Genes you are particularly
    # interested in, or Pathways that concern your research. Using lists simplifies
    # queries against large groups of objects at once, and allows for 
    # more streamlined analysis. Unlike queries and (to a degree) Templates, 
    # Lists are not just about reading in information - they can be created, renamed, 
    # modified, deleted. You can manage your lists effectively and quickly using the API.
    #
    # == Inspecting the model
    #
    #   model = service.model
    #   puts "Classes in the model:"
    #   model.classes.each do |c|
    #     puts c.name
    #   end
    #
    # The data model, which defines what queries are possible in the data-warehouse, 
    # is fully introspectible and queriable. This allows for sophisticated meta-programming
    # and dynamic query generation. 
    #
    #
    #:include:contact_header.rdoc
    #
    class Service

        extend Forwardable

        VERSION_PATH = "/version"
        RELEASE_PATH = "/version/release"
        MODEL_PATH = "/model/json"
        TEMPLATES_PATH = "/templates"
        QUERY_RESULTS_PATH = "/query/results"
        QUERY_TO_LIST_PATH = "/query/tolist/json"
        QUERY_APPEND_PATH = "/query/append/tolist/json"
        TEMPLATE_RESULTS_PATH = "/template/results"
        TEMPLATE_TO_LIST_PATH = "/template/tolist/json"
        TEMPLATE_APPEND_PATH = "/template/append/tolist/json"
        LISTS_PATH = "/lists/json"
        LIST_APPEND_PATH = "/lists/append/json"
        LIST_RENAME_PATH = "/lists/rename/json"
        LIST_UNION_PATH = "/lists/union/json"
        LIST_DIFFERENCE_PATH = "/lists/diff/json"
        LIST_INTERSECTION_PATH = "/lists/intersect/json"
        LIST_SUBTRACTION_PATH = "/lists/subtract/json"
        LIST_TAG_PATH = "/list/tags/json"
        LIST_ENRICHMENT_PATH = "/list/enrichment"

        # The webservice version. An integer that 
        # supplies information about what features are supported.
        attr_reader :version
        
        # The root of the query. If you supplied an abbreviated version, this
        # attribute will hold the expanded URL.
        attr_reader :root
        
        # The token you supplied to the constructor.
        attr_reader :token
        
        # A collection of the names of any templates that this service was not able to parse,
        # and you will thus not be able to access.
        attr_reader :broken_templates

        def_delegators :@list_manager, 
            :lists, :list, :list_names, :create_list, :delete_lists, 
            :get_lists_with_tags,
            :union_of, :intersection_of, :symmetric_difference_of, 
            :subtract

        # Construct a new service.
        #
        #  service = Service.new("www.flymine.org/query", "TOKEN")
        #
        # Arguments:
        # [+root+] The URL to the root of the webservice. For example, for 
        #          FlyMine this is: "http://www.flymine.org/query/service"
        #          For simplicity's sake, it is possible to omit the scheme
        #          and the "/service" section, and just pass the bare minimum:
        #          "www.flymine.org/query"
        # [+token+] An optional API access token to authenticate your webservice access with. 
        #           If you supply a token, you will have access to your private lists
        #           and templates.
        #
        def initialize(root, token=nil, mock_model=nil)
            u = URI.parse(root)
            unless u.scheme
                root = "http://" + root
            end
            unless root.end_with?("/service") # All webservices must
                root << "/service"
            end
            @root = root
            @token = token
            begin
                @version = fetch(@root + VERSION_PATH).to_i
            rescue => e
                raise ServiceError, "Error fetching version at #{@root + VERSION_PATH}: #{e.message}"
            end
            @model = mock_model
            @_templates = nil
            @broken_templates = []
            @list_manager = InterMine::Lists::ListManager.new(self)
        end

        # Return the release string
        def release
            return @release ||= fetch(@root + RELEASE_PATH)
        end

        # call-seq:
        #   model() => Model
        #
        # Retrieve the model from the service. This contains
        # all the metadata about the data-model which defines what is 
        # queriable.
        def model
            if @model.nil?
                data = fetch(@root + MODEL_PATH)
                @model = InterMine::Metadata::Model.new(data, self)
            end
            @model
        end

        alias get_model model

        # call-seq:
        #   query(rootClass=nil) => PathQuery::Query
        #
        # Create a new query against the data at this webservice
        def query(rootClass=nil)
            return InterMine::PathQuery::Query.new(self.model, rootClass, self)
        end

        # call-seq:
        #   select(*columns) => PathQuery::Query
        #
        # Create a new query with the given view.
        def select(*columns)
            return InterMine::PathQuery::Query.new(self.model, nil, self).select(*columns)
        end

        alias new_query query

        # call-seq:
        #   template(name) => PathQuery::Template
        #
        # Get a Template by name, Returns nil if there is no such template.
        def template(name) 
            return templates[name]
        end

        alias get_template template


        # call-seq:
        #   templates() => Hash{String => Template}
        #
        # Returns all the templates available to query against in the service 
        #
        def templates 
            if @_templates.nil?
                @_templates = {}
                parser = InterMine::PathQuery::Template.parser(model)
                template_xml = fetch(@root + TEMPLATES_PATH)
                doc = REXML::Document.new(template_xml)
                doc.elements.each("template-queries/template") do |t|
                    begin
                        temp = parser.parse(t)
                        @_templates[temp.name] = temp
                    rescue
                        @broken_templates.push(t.attribute("name").value)
                    end
                end
            end
            return @_templates
        end

        # Get all the names of the available templates, in 
        # alphabetical order.
        def template_names
            return templates.keys.sort
        end

        # Get the data used for constructing the lists. Called internally.
        def get_list_data
            return fetch(@root + LISTS_PATH)
        end

        # Get the basic parameters used by all requests. This includes
        # the authorization token.
        def params
            return @token.nil? ? {} : {"token" => @token}
        end

        private

        # Retrieves data from a url with a get request.
        def fetch(url)
            uri = URI.parse(url)
            qs = params.map { |k,v| "#{k}=#{CGI::escape(v.to_s)}" }.join('&')
            return Net::HTTP.get(uri.host, uri.path + (params.empty? ? "" : "?#{qs}"))
        end
    end

    # Errors resulting from Service failures.
    class ServiceError < RuntimeError
    end
end

