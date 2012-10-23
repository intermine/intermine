require "rexml/document"
require "rexml/streamlistener"
require "stringio"
require "intermine/model"
require "intermine/results"
require "intermine/service"
require "intermine/lists"

include InterMine

unless String.instance_methods.include?(:start_with?)

    class String
     
        def start_with?(prefix)
            prefix = Regexp.escape(prefix.to_s)
            return self.match("^#{prefix}")
        end

        def end_with?(suffix)
            suffix = Regexp.escape(suffix.to_s)
            return self.match("#{suffix}$")
        end

    end
end

class Array
  def every(count)
    chunks = []
    each_with_index do |item, index|
      chunks << [] if index % count == 0
      chunks.last << item
    end
    chunks
  end
  alias / every
end


module InterMine::PathQuery

    include REXML

    class QueryLoader

        attr_reader :model

        def initialize(model)
            @model = model
        end

        def get_handler
            return QueryBuilder.new(@model)
        end

        def parse(xml)
            xml = StringIO.new(xml.to_s)
            handler = get_handler
            REXML::Document.parse_stream(xml, handler)
            return handler.query
        end
            
    end

    class TemplateLoader < QueryLoader
        
        def get_handler
            return TemplateBuilder.new(@model)
        end
    end

    class QueryBuilder
        include REXML::StreamListener

        def initialize(model)
            @model = model
            @query_attributes = {}
            @subclass_constraints = []
            @coded_constraints = []
            @joins = []
        end

        def query
            q = create_query
            # Add first, in case other bits depend on them
            @subclass_constraints.each do |sc|
                q.add_constraint(sc)
            end
            @joins.each do |j|
                q.add_join(*j)
            end
            @coded_constraints.each do |con|
                q.add_constraint(con)
            end
            @query_attributes.sort_by {|k, v| k}.reverse.each do |k,v|
                begin
                    q.send(k + "=", v)
                rescue
                end
            end
            return q
        end

        def tag_start(name, attrs)
            @in_value = false
            if name == "query"
                attrs.each do |a|
                    @query_attributes[a.first] = a.last if a.first != "model"
                end
            elsif name=="constraint"
                process_constraint(attrs)
            elsif name=="value"
                @in_value = true
            elsif name=="join"
                @joins.push([attrs["path"], attrs["style"]])
            end
        end

        def process_constraint(attrs)
            if attrs.has_key?("type")
                @subclass_constraints.push({:path => attrs["path"], :sub_class => attrs["type"]})
            else
                args = {}
                args[:path] = attrs["path"]
                args[:op] = attrs["op"]
                args[:value] = attrs["value"] if attrs.has_key?("value")
                args[:loopPath] = attrs["loopPath"] if attrs.has_key?("loopPath")
                args[:extra_value] = attrs["extraValue"] if attrs.has_key?("extraValue")
                args[:code] = attrs["code"]
                if MultiValueConstraint.valid_ops.include?(attrs["op"])
                    args[:values] = [] # actual values will be pushed on later
                end
                if attrs.has_key?("loopPath")
                    LoopConstraint.xml_ops.each do |k,v|
                        args[:op] = k if v == args[:op]
                    end
                end
                @coded_constraints.push(args)
            end 
        end

        def text(t)
            @coded_constraints.last[:values].push(t)
        end

        private

        def create_query
            return Query.new(@model)
        end

    end

    class TemplateBuilder < QueryBuilder

        def initialize(model)
            super
            @template_attrs = {}
        end

        def tag_start(name, attrs)
            super
            if name == "template"
                attrs.each do |a|
                    @template_attrs[a.first] = a.last
                end
            end
        end

        def query
            template = super
            @template_attrs.each do |k,v|
                template.send(k + '=', v)
            end
            return template
        end

        def process_constraint(attrs)
            super
            unless attrs.has_key? "type"
                if attrs.has_key?("editable") and attrs["editable"].downcase == "false"
                    @coded_constraints.last[:editable] = false
                else
                    @coded_constraints.last[:editable] = true
                end
                @coded_constraints.last[:switchable] = attrs["switchable"] || "locked"
            end
        end

        private

        def create_query
            return Template.new(@model, nil, @model.service)
        end

    end

    # == A class representing a structured query against an InterMine Data-Warehouse
    #
    # Queries represent structured requests for data from an InterMine data-warehouse. They
    # consist basically of output columns you select, and a set of constraints on the results 
    # to return. These are known as the "view" and the "constraints". In a nod to the SQL-origins 
    # of the queries, and to the syntax of ActiveRecord, there is both a method-chaining SQL-ish
    # DSL, and a more isolating common InterMine DSL. 
    #
    #   query = service.query("Gene").select("*").where("proteins.molecularWeight" => {">" => 10000})
    #   query.each_result do |gene|
    #     puts gene.symbol
    #   end
    #
    # OR:
    #
    #   query = service.query("Gene")
    #   query.add_views("*")
    #   query.add_constraint("proteins.molecularWeight", ">", 10000)
    #   ...
    #
    # The main differences from SQL are that the joining between tables is implicit
    # and automatic. Simply by naming the column "Gene.proteins.molecularWeight" we have access
    # to the protein table joined onto the gene table. (A consequence of this is that all queries must have a
    # unique root that all paths descend from, and we do not permit right outer joins.)
    #
    # You can define the following features of a query:
    #  * The output column
    #  * The filtering constraints (what values certain columns must or must not have)
    #  * The sort order of the results
    #  * The way constraints are combined (AND or OR)
    #
    # In processing results, there are two powerful result formats available, depending on
    # whether you want to process results row by row, or whether you would like the information grouped into
    # logically coherent records. The latter is more similar to the ORM model, and can be seen above.
    # The mechanisms we offer for row access allow accessing cell values of the result table
    # transparently by index or column-name. 
    #
    #:include:contact_header.rdoc
    #
    class Query

        # The first possible constraint code
        LOWEST_CODE = "A"

        # The last possible constraint code
        HIGHEST_CODE = "Z"

        # The (optional) name of the query. Used in automatic access (eg: "query1")
        attr_accessor :name
        
        # A human readable title of the query (eg: "Gene --> Protein Domain")
        attr_accessor :title
        
        # The root class of the query.
        attr_accessor :root

        # The data model associated with the query
        attr_reader :model
        
        # All the current Join objects on the query
        attr_reader :joins
        
        # All the current constraints on the query
        attr_reader :constraints
        
        # All the columns currently selected for output.
        attr_reader :views
        
        # The current sort-order.
        attr_reader :sort_order
        
        # The current logic (as a LogicGroup)
        attr_reader :logic
        
        # The service this query is associated with
        attr_reader :service
        
        # URLs for internal consumption.
        attr_reader :list_upload_uri, :list_append_uri

        # The number of rows to return - defaults to nil (all rows)
        attr_accessor :size

        # The index of the first row to return - defaults to 0 (first row)
        attr_accessor :start

        # Construct a new query object. You should not use this directly.
        # Instead use the factory methods in Service. 
        #
        #   query = service.query("Gene")
        #
        def initialize(model, root=nil, service=nil)
            @model = model
            @service = service
            @url = (@service.nil?) ? nil : @service.root + Service::QUERY_RESULTS_PATH
            @list_upload_uri = (@service.nil?) ? nil : @service.root + Service::QUERY_TO_LIST_PATH
            @list_append_uri = (@service.nil?) ? nil : @service.root + Service::QUERY_APPEND_PATH
            if root
                @root = InterMine::Metadata::Path.new(root, model).rootClass
            end
            @size = nil
            @start = 0
            @constraints = []
            @joins = []
            @views = []
            @sort_order = []
            @used_codes = []
            @logic_parser = LogicParser.new(self)
            @constraint_factory = ConstraintFactory.new(self)
        end

        # Return a parser for deserialising queries. 
        #
        #   parser = Query.parser(service.model)
        #   query = parser.parse(string)
        #   query.each_row |r|
        #     puts r.to_h
        #   end
        #   
        def self.parser(model)
            return QueryLoader.new(model)
        end

        # Return all the constraints that have codes and can thus
        # participate in logic.
        def coded_constraints
            return @constraints.select {|x| !x.is_a?(SubClassConstraint)}
        end
        
        # Return all the constraints that restrict the class of 
        # paths in the query.
        def subclass_constraints
            return @constraints.select {|x| x.is_a?(SubClassConstraint)}
        end

        # Return an XML document node representing the XML form of the query.
        #
        # This is the canonical serialisable form of the query. 
        #
        def to_xml
            doc = REXML::Document.new

            if @sort_order.empty?
                so = SortOrder.new(@views.first, "ASC")
            else
                so = @sort_order.join(" ")
            end

            query = doc.add_element("query", {
                "name" => @name, 
                "model" => @model.name, 
                "title" => @title, 
                "sortOrder" => so,
                "view" => @views.join(" "),
                "constraintLogic" => @logic
            }.delete_if { |k, v | !v })
            @joins.each { |join| 
                query.add_element("join", join.attrs) 
            }
            subclass_constraints.each { |con|
                query.add_element(con.to_elem) 
            }
            coded_constraints.each { |con|
                query.add_element(con.to_elem) 
            }
            return doc
        end

        # Return true if the other query has exactly
        # the same configuration, and belongs to the same
        # service.
        def eql?(other)
            if other.is_a? Query
                return self.service == other.service && self.to_xml_to_s == other.to_xml.to_s
            else
                return false
            end
        end

        # Get your own result reader for handling the results at a low level.
        # If no columns have been selected for output before requesting results, 
        # all attribute columns will be selected.
        def results_reader(start=0, size=nil)
            if @views.empty?
                select("*")
            end
            return Results::ResultsReader.new(@url, self, start, size)
        end

        # Set the maximum number of rows this query will return. 
        #
        # This method can be used to set a default maximum size for 
        # a query. Set to nil for all rows. The value given here 
        # will be overridden by any value supplied by #each_row
        # or #each_result, unless that value is nil, in which 
        # case this value will be used. If unset, the query will return all results.
        #
        # Returns self for method chaining.
        #
        # See also #size= and #offset
        #
        def limit(size)
            @size = size
            return self
        end

        # Set the index of the first row of results this query will return. 
        #
        # This method can be used to set a value for the query offset. 
        # The value given here will be overridden by any value supplied by #each_row
        # or #each_result. If unset, results will start from the first row.
        #
        # Returns self for method chaining.
        #
        # See also #start= and #limit
        #
        def offset(start) 
            @start = start
            return self
        end

        # Iterate over the results of this query one row at a time.
        #
        # Rows support both array-like index based access as well as 
        # hash-like key based access. For key based acces you can use
        # either the full path or the headless short version:
        #
        #   query.each_row do |row|
        #     puts r["Gene.symbol"], r["proteins.primaryIdentifier"]
        #     puts r[0]
        #     puts r.to_a # Materialize the row an an Array
        #     puts r.to_h # Materialize the row an a Hash
        #   end
        #
        # This method is now deprecated and will be removed in version 1
        # Please use Query#rows
        #
        def each_row(start=nil, size=nil)
            start = start.nil? ? @start : start
            size  = size.nil? ? @size : size
            results_reader(start, size).each_row {|row|
                yield row
            }
        end

        # Iterate over the results, one record at a time.
        #
        #   query.each_result do |gene|
        #     puts gene.symbol
        #     gene.proteins.each do |prot|
        #       puts prot.primaryIdentifier
        #     end
        #   end
        #
        # This method is now deprecated and will be removed in version 1
        # Please use Query#results
        #
        def each_result(start=nil, size=nil)
            start = start.nil? ? @start : start
            size  = size.nil? ? @size : size
            results_reader(start, size).each_result {|row|
                yield row
            }
        end

        # Return the number of result rows this query will return in its current state. 
        # This makes a very small request to the webservice, and is the most efficient 
        # method of getting the size of the result set.
        def count
            return results_reader.get_size
        end

        # Returns an Enumerable of ResultRow objects containing the 
        # data returned by running this query, starting at the given offset and 
        # containing up to the given maximum size.
        #
        # The webservice enforces a maximum page-size of 10,000,000 rows,
        # independent of any size you specify - this can be obviated with paging
        # for large result sets.
        #
        #   rows = query.rows
        #   rows.last["symbol"]
        #   => "eve"
        #
        def rows(start=nil, size=nil)
            start = start.nil? ? @start : start
            size  = size.nil? ? @size : size
            return Results::RowReader.new(@url, self, start, size)
        end

        # Return objects corresponding to the type of data requested, starting 
        # at the given row offset. Returns an Enumerable of InterMineObject,
        # where each value is read one at a time from the connection. 
        #
        #  genes = query.results
        #  genes.last.symbol
        #  => "eve"
        #
        def results(start=nil, size=nil)
            start = start.nil? ? @start : start
            size  = size.nil? ? @size : size
            return Results::ObjectReader.new(@url, self, start, size)
        end

        # Return an Enumerable of summary items starting at the given offset.
        #
        #  summary = query.summary_items("chromosome.primaryIdentifier")
        #  top_chromosome = summary[0]["item"]
        #  no_in_top_chrom = summary[0]["count"]
        #
        # This can be made more efficient by passing in a size - ie, if 
        # you only want the top item, pass in an offset of 0 and a size of 1
        # and only that row will be fetched.
        #
        def summaries(path, start=0, size=nil)
            q = self.clone
            q.add_views(path)
            return Results::SummaryReader.new(@url, q, start, size, path)
        end

        # Return a summary for a column as a Hash
        #
        # For numeric values the hash has four keys: "average", "stdev", "min", and "max". 
        #
        #  summary = query.summarise("length")
        #  puts summary["average"]
        #
        # For non-numeric values, the hash will have each possible value as a key, and the
        # count of the occurrences of that value in this query's result set as the 
        # corresponding value:
        # 
        #  summary = query.summarise("chromosome.primaryIdentifier")
        #  puts summary["2L"]
        #
        # To limit the size of the result set you can use start and size as per normal 
        # queries - this has no real effect with numeric queries, which always return the 
        # same information.
        #
        def summarise(path, start=0, size=nil)
            t = make_path(add_prefix(path)).end_type
            if InterMine::Metadata::Model::NUMERIC_TYPES.include? t
                return Hash[summaries(path, start, size).first.map {|k, v| [k, v.to_f]}]
            else
                return Hash[summaries(path, start, size).map {|sum| [sum["item"], sum["count"]]}]
            end
        end

        # Return all result record objects returned by running this query.
        def all
            return self.results
        end

        # Return all the rows returned by running the query
        def all_rows
            return self.rows
        end

        # Get the first result record from the query, starting at the 
        # given offset. If the offset is large, then this is not an efficient
        # way to retrieve this data, and you may with to consider a looping approach
        # or row based access instead.
        def first(start=0)
            current_row = 0
            # Have to iterate as start refers to row count
            results_reader.each_result { |r|
                if current_row == start
                    return r
                end
                current_row += 1
            }
            return nil
        end

        # Get the first row of results from the query, starting at the given offset.
        def first_row(start = 0)
            return self.results(start, 1).first
        end
        
        # Get the constraint on the query with the given code.
        # Raises an error if there is no such constraint.
        def get_constraint(code)
            @constraints.each do |x|
                if x.respond_to?(:code) and x.code == code
                    return x
                end
            end
            raise ArgumentError, "#{code} not in query"
        end


        # Remove the constraint with the given code from the query.
        # If no such constraint exists, no error will be raised.
        def remove_constraint(code)
            @constraints.reject! do |x|
                x.respond_to?(:code) and x.code == code
            end
        end

        # Add the given views (output columns) to the query. 
        #
        # Any columns ending in "*" will be interpreted as a request to add
        # all attribute columns from that table to the query
        #
        # Any columns that name a class or reference will add the id of that
        # object to the query. This is helpful for creating lists and other 
        # specialist services.
        #
        #   query = service.query("Gene")
        #   query.add_views("*")
        #   query.add_to_select("*")
        #   query.add_views("proteins.*")
        #   query.add_views("pathways.*", "organism.shortName")
        #   query.add_views("proteins", "exons")
        #
        def add_views(*views)
            views.flatten.map do |x| 
                y = add_prefix(x)
                if y.end_with?("*")
                    prefix = y.chomp(".*")
                    path = make_path(prefix)
                    add_views(path.end_cd.attributes.map {|x| prefix + "." + x.name})
                else
                    path = make_path(y)
                    path = make_path(y.to_s + ".id") unless path.is_attribute?
                    if @root.nil?
                        @root = path.rootClass
                    end
                    @views << path
                end
            end
            return self
        end

        def make_path(path)
            return InterMine::Metadata::Path.new(path, @model, subclasses)
        end

        alias add_to_select add_views

        # Replace any currently existing views with the given view list. 
        # If the view is not already an Array, it will be split by commas and whitespace.
        #
        def view=(*view)
            @views = []
            view.each do |v|
                if v.is_a?(Array)
                    views = v
                else
                    views = v.to_s.split(/(?:,\s*|\s+)/)
                end
                add_views(*views)
            end
            return self
        end

        alias select view=

        # Get the current sub-class map for this query. 
        #
        # This contains information about which fields of this query have 
        # been declared to be restricted to contain only a subclass of their 
        # normal type.
        #
        #   > query = service.query("Gene")
        #   > query.where(:microArrayResults => service.model.table("FlyAtlasResult"))
        #   > query.subclasses
        #   => {"Gene.microArrayResults" => "FlyAtlasResult"}
        #
        def subclasses
            subclasses = {}
            @constraints.each do |con|
                if con.is_a?(SubClassConstraint)
                    subclasses[con.path.to_s] = con.sub_class.to_s
                end
            end
            return subclasses
        end

        # Declare how a particular join should be treated. 
        #
        # The default join style is for an INNER join, but joins can 
        # optionally be declared to be LEFT OUTER joins. The difference is 
        # that with an inner join, each join in the query implicitly constrains
        # the values of that path to be non-null, whereas an outer-join allows
        # null values in the joined path. If the path passed to the constructor 
        # has a chain of joins, the last section is the one the join is applied to.
        #
        #   query = service.query("Gene")
        #   # Allow genes without proteins
        #   query.add_join("proteins") 
        #   # Demand the results contain only those genes that have interactions that have interactingGenes, 
        #   # but allow those interactingGenes to not have any proteins.
        #   query.add_join("interactions.interactingGenes.proteins")
        #
        # The valid join styles are OUTER and INNER (case-insensitive). There is never
        # any need to declare a join to be INNER, as it is inner by default. Consider 
        # using Query#outerjoin which is more explicitly declarative.
        #
        def add_join(path, style="OUTER")
            p = InterMine::Metadata::Path.new(add_prefix(path), @model, subclasses)
            if @root.nil?
                @root = p.rootClass
            end
            @joins << Join.new(p, style)
            return self
        end

        alias join add_join

        # Explicitly declare a join to be an outer join.
        def outerjoin(path)
            return add_join(path)
        end

        # Add a sort order element to sort order information. 
        # A sort order consists of the name of an output column and 
        # (optionally) the direction to sort in. The default direction is "ASC".
        # The valid directions are "ASC" and "DESC" (case-insensitive).
        #
        #   query.add_sort_order("length")
        #   query.add_sort_order("proteins.primaryIdentifier", "desc")
        #
        def add_sort_order(path, direction="ASC") 
            p = self.path(path)
            if !@views.include? p
                raise ArgumentError, "Sort order (#{p}) not in view (#{@views.map {|v| v.to_s}.inspect} in #{self.name || 'unnamed query'})"
            end
            @sort_order << SortOrder.new(p, direction)
            return self
        end

        # Set the sort order completely, replacing the current sort order.
        #
        #  query.sortOrder = "Gene.length asc Gene.proteins.length desc"
        #
        # The sort order expression will be parsed and checked for conformity with the
        # current state of the query.
        def sortOrder=(so)
            if so.is_a?(Array)
                sos = so
            else
                sos = so.split(/(ASC|DESC|asc|desc)/).map {|x| x.strip}.every(2)
            end
            sos.each do |args|
                add_sort_order(*args)
            end
        end

        alias order_by add_sort_order
        alias order add_sort_order

        # Add a constraint to the query matching the given parameters, and 
        # return the created constraint.
        #
        #   con = query.add_constraint("length", ">", 500)
        #
        # Note that (at least for now) the style of argument used by where and 
        # add_constraint is not compatible. This is on the TODO list.
        def add_constraint(*parameters)
            con = @constraint_factory.make_constraint(parameters)
            @constraints << con
            return con
        end

        # Returns a Path object constructed from the given path-string, 
        # taking the current state of the query into account (its data-model 
        # and subclass constraints).
        def path(pathstr)
            return InterMine::Metadata::Path.new(add_prefix(pathstr), @model, subclasses)
        end

        # Add a constraint clause to the query. 
        #
        #   query.where(:symbol => "eve")
        #   query.where(:symbol => %{eve h bib zen})
        #   query.where(:length => {:le => 100}, :symbol => "eve*")
        #
        # Interprets the arguments in a style similar to that of 
        # ActiveRecord constraints, and adds them to the query.
        # If multiple constraints are supplied in a single hash (as
        # in the third example), then the order in which they are 
        # applied to the query (and thus the codes they will receive) is
        # not predictable. To determine the order use chained where clauses
        # or use multiple hashes:
        #
        #   query.where({:length => {:le => 100}}, {:symbol => "eve*"})
        #
        # Returns self to support method chaining
        # 
        def where(*wheres)
           if @views.empty?
               self.select('*')
           end
           wheres.each do |w|
             w.each do |k,v|
                if v.is_a?(Hash)
                    parameters = {:path => k}
                    v.each do |subk, subv|
                        normalised_k = subk.to_s.upcase.gsub(/_/, " ")
                        if subk == :with
                            parameters[:extra_value] = subv
                        elsif subk == :sub_class
                            parameters[subk] = subv
                        elsif subk == :code
                            parameters[:code] = subv
                        elsif LoopConstraint.valid_ops.include?(normalised_k)
                            parameters[:op] = normalised_k
                            parameters[:loopPath] = subv
                        else
                            if subv.nil?
                                if subk == "="
                                    parameters[:op] = "IS NULL"
                                elsif subk == "!="
                                    parameters[:op] = "IS NOT NULL"
                                else
                                    parameters[:op] = normalised_k
                                end
                            elsif subv.is_a?(Range) or subv.is_a?(Array)
                                if subk == "="
                                    parameters[:op] = "ONE OF"
                                elsif subk == "!="
                                    parameters[:op] = "NONE OF"
                                else
                                    parameters[:op] = normalised_k
                                end
                                parameters[:values] = subv.to_a
                            elsif subv.is_a?(Lists::List)
                                if subk == "="
                                    parameters[:op] = "IN"
                                elsif subk == "!="
                                    parameters[:op] = "NOT IN"
                                else
                                    parameters[:op] = normalised_k
                                end
                                parameters[:value] = subv.name
                            else
                                parameters[:op] = normalised_k
                                parameters[:value] = subv
                            end
                        end
                    end
                    add_constraint(parameters)
                elsif v.is_a?(Range) or v.is_a?(Array)
                    add_constraint(k.to_s, 'ONE OF', v.to_a)
                elsif v.is_a?(InterMine::Metadata::ClassDescriptor)
                    add_constraint(:path => k.to_s, :sub_class => v.name)
                elsif v.is_a?(InterMine::Lists::List)
                    add_constraint(k.to_s, 'IN', v.name)
                elsif v.nil?
                    add_constraint(k.to_s, "IS NULL")
                else
                    if path(k.to_s).is_attribute?
                        add_constraint(k.to_s, '=', v)
                    else
                        add_constraint(k.to_s, 'LOOKUP', v)
                    end
                end
             end
           end
           return self
        end

        # Set the logic to the given value.
        #
        # The value will be parsed for consistency is it is a logic
        # string. 
        #
        # Returns self to support chaining.
        def set_logic(value)
            if value.is_a?(LogicGroup)
                @logic = value
            else
                @logic = @logic_parser.parse_logic(value)
            end
            return self
        end

        alias constraintLogic= set_logic

        # Get the next available code for the query.
        def next_code
            c = LOWEST_CODE
            while Query.is_valid_code(c)
                return c unless used_codes.include?(c)
                c = c.next
            end
            raise RuntimeError, "Maximum number of codes reached - all 26 have been allocated"
        end

        # Return the list of currently used codes by the query.
        def used_codes
            if @constraints.empty?
                return []
            else
                return @constraints.select {|x| !x.is_a?(SubClassConstraint)}.map {|x| x.code}
            end
        end

        # Whether or not the argument is a valid constraint code.
        #
        # to be valid, it must be a one character string between A and Z inclusive.
        def self.is_valid_code(str)
            return (str.length == 1) && (str >= LOWEST_CODE) && (str <= HIGHEST_CODE)
        end

        # Adds the root prefix to the given string.
        #
        # Arguments: 
        # [+x+] An object with a #to_s method
        #
        # Returns the prefixed string.
        def add_prefix(x)
            x = x.to_s
            if @root && !x.start_with?(@root.name)
                return @root.name + "." + x
            else 
                return x
            end
        end

        # Return the parameter hash for running this query in its current state.
        def params
            hash = {"query" => self.to_xml}
            if @service and @service.token
                hash["token"] = @service.token
            end
            return hash
        end

        # Return the textual representation of the query. Here it returns the Query XML
        def to_s
            return to_xml.to_s
        end

        # Return an informative textual representation of the query.
        def inspect
            return "<#{self.class.name} query=#{self.to_s.inspect}>"
        end
    end


    class ConstraintFactory

        def initialize(query)
            @classes = [
                SingleValueConstraint, 
                SubClassConstraint, 
                LookupConstraint, MultiValueConstraint, RangeConstraint,
                UnaryConstraint, LoopConstraint, ListConstraint]

            @query = query
        end

        def make_constraint(args)
            case args.length 
            when 2
                parameters = {:path => args[0], :op => args[1]}
            when 3
                if args[2].is_a?(Array)
                    parameters = {:path => args[0], :op => args[1], :values => args[2]}
                elsif LoopConstraint.valid_ops.include?(args[1])
                    parameters = {:path => args[0], :op => args[1], :loopPath => args[2]}
                else
                    parameters = {:path => args[0], :op => args[1], :value => args[2]}
                end
            when 4
                parameters = {:path => args[0], :op => args[1], :value => args[2], :extra_value => args[3]}
            else
                parameters = args.first
            end

            attr_keys = parameters.keys
            suitable_classes = @classes.select { |cls| 
                attr_keys.reduce(true) do |suitable, k|
                    suitable && cls.method_defined?(k) && (k.to_s != "op" or cls.valid_ops.include?(parameters[k]))
                end
            }
            if suitable_classes.size > 1
                raise ArgumentError, "More than one class found for #{parameters.inspect}"
            elsif suitable_classes.size < 1
                raise ArgumentError, "No suitable classes found for #{parameters.inspect}"
            end

            cls = suitable_classes.first
            con = cls.new
            parameters.each_pair { |key, value|
                if key == :path || key == :loopPath
                    value = @query.path(value)
                end
                if key == :sub_class
                    value = InterMine::Metadata::Path.new(value, @query.model)
                end
                con.send(key.to_s + '=', value)
            }
            con.validate
            if con.respond_to?(:code)
                code = con.code
                if code.nil?
                    con.code = @query.next_code
                else
                    code = code.to_s
                    unless Query.is_valid_code(code)
                        raise ArgumentError, "Coded must be between A and Z, got: #{code}"
                    end
                    if @query.used_codes.include?(code)
                        con.code = @query.next_code
                    end
                end
            end

            return con
        end


    end

    class TemplateConstraintFactory < ConstraintFactory
        
        def initialize(query)
            super
            @classes =  [
                TemplateSingleValueConstraint, 
                SubClassConstraint, 
                TemplateLookupConstraint, TemplateMultiValueConstraint, 
                TemplateUnaryConstraint, TemplateLoopConstraint, TemplateListConstraint]
        end

    end

    module PathFeature
        attr_accessor :path

        def validate
        end
    end

    module TemplateConstraint

        attr_accessor :editable, :switchable

        def to_elem
            attributes = {"editable" => @editable, "switchable" => @switchable}
            elem = super
            elem.add_attributes(attributes)
            return elem
        end

        def template_param_op
            return @op
        end

    end

    module Coded
        attr_accessor :code, :op

        def self.valid_ops
            return []
        end

        def to_elem
            attributes = {
                "path" => @path,
                "op" => @op,
                "code" => @code
            }.delete_if {|k,v| !v}
            elem = REXML::Element.new("constraint")
            elem.add_attributes(attributes)
            return elem
        end
    end

    class SubClassConstraint
        include PathFeature
        attr_accessor :sub_class

        def to_elem
            attributes = {
                "path" => @path,
                "type" => @sub_class
            }
            elem = REXML::Element.new("constraint")
            elem.add_attributes(attributes)
            return elem
        end

        def validate 
            if @path.elements.last.is_a?(InterMine::Metadata::AttributeDescriptor)
                raise ArgumentError, "#{self.class.name}s must be on objects or references to objects"
            end
            if @sub_class.length > 1
                raise ArgumentError, "#{self.class.name} expects sub-classes to be named as bare class names"
            end
            model = @path.model
            cdA = model.get_cd(@path.end_type)
            cdB = model.get_cd(@sub_class.end_type)
            unless ((cdB == cdA) or cdB.subclass_of?(cdA))
                raise ArgumentError, "The subclass in a #{self.class.name} must be a subclass of its path, but #{cdB} is not a subclass of #{cdA}"
            end

        end

    end

    module ObjectConstraint
        def validate
            if @path.elements.last.is_a?(InterMine::Metadata::AttributeDescriptor)
                raise ArgumentError, "#{self.class.name}s must be on objects or references to objects, got #{@path}"
            end
        end
    end

    module AttributeConstraint
        def validate
            if !@path.elements.last.is_a?(InterMine::Metadata::AttributeDescriptor)
                raise ArgumentError, "Attribute constraints must be on attributes, got #{@path}"
            end
        end

        def coerce_value(val)
            nums = ["Float", "Double", "float", "double"]
            ints = ["Integer", "int"]
            bools = ["Boolean", "boolean"]
            dataType = @path.elements.last.dataType.split(".").last
            coerced = val
            if nums.include?(dataType)
                if !val.is_a?(Numeric)
                    coerced = val.to_f
                end
            end
            if ints.include?(dataType)
                coerced = val.to_i
            end
            if bools.include?(dataType)
                if !val.is_a?(TrueClass) && !val.is_a?(FalseClass)
                    if val == 0 or val == "0" or val.downcase == "yes" or val.downcase == "true" or val.downcase == "t"
                        coerced = true
                    elsif val == 1 or val == "1" or val.downcase == "no" or val.downcase == "false" or val.downcase == "f"
                        coerced = false
                    end
                end
            end
            if coerced == 0 and not val.to_s.start_with?("0")
               raise ArgumentError, "cannot coerce #{val} to a #{dataType}"
            end
            return coerced
        end

        def validate_value(val)
            nums = ["Float", "Double", "float", "double"]
            ints = ["Integer", "int"]
            bools = ["Boolean", "boolean"]
            dataType = @path.elements.last.dataType.split(".").last
            if nums.include?(dataType)
                if !val.is_a?(Numeric)
                    raise ArgumentError, "value #{val} is not numeric for #{@path}"
                end
            end
            if ints.include?(dataType)
                val = val.to_i
                if !val.is_a?(Integer)
                    raise ArgumentError, "value #{val} is not an integer for #{@path}"
                end
            end
            if bools.include?(dataType)
                if !val.is_a?(TrueClass) && !val.is_a?(FalseClass)
                    raise ArgumentError, "value #{val} is not a boolean value for #{@path}"
                end
            end
        end
    end

    class SingleValueConstraint
        include PathFeature
        include Coded
        include AttributeConstraint
        attr_accessor :value

        CANONICAL_OPS = {
            "EQ" => "=",
            "==" => "=",
            "NE" => "!=",
            "LT" => "<",
            "GT" => ">",
            "LE" => "<=",
            "GE" => ">="
        }

        def self.valid_ops 
            return %w{= == > < >= <= != CONTAINS LIKE EQ NE GT LT LE GE}
        end

        def to_elem
            elem = super
            attributes = {"value" => @value}
            elem.add_attributes(attributes)
            return elem
        end

        def validate 
            super
            @op = SingleValueConstraint::CANONICAL_OPS[@op] || @op
            @value = coerce_value(@value)
            validate_value(@value)
        end

    end

    class TemplateSingleValueConstraint < SingleValueConstraint
        include TemplateConstraint

        def template_param_op
            case @op
            when '='
                return 'eq'
            when '!='
                return 'ne'
            when '<'
                return 'lt'
            when '<='
                return 'le'
            when '>'
                return 'gt'
            when '>='
                return 'ge'
            else
                return @op
            end
        end
    end

    class ListConstraint < SingleValueConstraint
        include ObjectConstraint
        
        def self.valid_ops
            return ["IN", "NOT IN"]
        end
    end

    class TemplateListConstraint < ListConstraint
        include TemplateConstraint
    end

    class LoopConstraint
        include PathFeature
        include Coded
        attr_accessor :loopPath

        def self.valid_ops
            return ["IS", "IS NOT"]
        end

        def self.xml_ops
            return { "IS" => "=", "IS NOT" => "!=" }
        end

        def to_elem
            elem = super
            elem.add_attribute("op", LoopConstraint.xml_ops[@op])
            elem.add_attribute("loopPath", @loopPath)
            return elem
        end

        def validate
            if @path.elements.last.is_a?(InterMine::Metadata::AttributeDescriptor)
                raise ArgumentError, "#{self.class.name}s must be on objects or references to objects"
            end
            if @loopPath.elements.last.is_a?(InterMine::Metadata::AttributeDescriptor)
                raise ArgumentError, "loopPaths on #{self.class.name}s must be on objects or references to objects"
            end
            model = @path.model
            cdA = model.get_cd(@path.end_type)
            cdB = model.get_cd(@loopPath.end_type)
            if !(cdA == cdB) && !cdA.subclass_of?(cdB) && !cdB.subclass_of?(cdA)
                raise ArgumentError, "Incompatible types in #{self.class.name}: #{@path} -> #{cdA} and #{@loopPath} -> #{cdB}"
            end
        end

    end

    class TemplateLoopConstraint < LoopConstraint
        include TemplateConstraint
        def template_param_op
            case @op
            when 'IS'
                return 'eq'
            when 'IS NOT'
                return 'ne'
            end
        end
    end

    class UnaryConstraint
        include PathFeature
        include Coded
        
        def self.valid_ops
            return ["IS NULL", "IS NOT NULL"]
        end

    end

    class TemplateUnaryConstraint < UnaryConstraint
        include TemplateConstraint
    end

    class LookupConstraint < ListConstraint
        attr_accessor :extra_value

        def self.valid_ops
            return ["LOOKUP"]
        end

        def to_elem
            elem = super
            if @extra_value
                elem.add_attribute("extraValue", @extra_value)
            end
            return elem
        end

    end

    class TemplateLookupConstraint < LookupConstraint
        include TemplateConstraint
    end

    class MultiValueConstraint 
        include PathFeature
        include Coded
        include AttributeConstraint
        
        def self.valid_ops 
            return ["ONE OF", "NONE OF"]
        end

        attr_accessor :values
        def to_elem 
            elem = super
            @values.each { |x|
                value = REXML::Element.new("value")
                value.add_text(x.to_s)
                elem.add_element(value)
            }
            return elem
        end

        def validate
            super
            @values.map! {|val| coerce_value(val)}
            @values.each do |val|
                validate_value(val)
            end
        end
    end

    class RangeConstraint < MultiValueConstraint

        VALID_OPS = [
            "WITHIN", "OUTSIDE",
            "CONTAINS", "DOES NOT CONTAIN",
            "OVERLAPS", "DOES NOT OVERLAP"
        ]

        def self.valid_ops
            return VALID_OPS
        end

        def validate
            # no-op
        end
    end

    class TemplateMultiValueConstraint < MultiValueConstraint 
        include TemplateConstraint
    end

    class TemplateRangeConstraint < RangeConstraint
        include TemplateConstraint
    end

    class SortOrder 
        include PathFeature
        attr_accessor :direction
        class << self;  attr_accessor :valid_directions end
        @valid_directions = %w{ASC DESC}

        def initialize(path, direction) 
            direction = direction.to_s.upcase
            unless SortOrder.valid_directions.include? direction
                raise ArgumentError, "Illegal sort direction: #{direction}"
            end
            self.path = path
            self.direction = direction
        end

        def to_s
            return @path.to_s + " " + @direction
        end
    end

    class Join 
        include PathFeature
        attr_accessor :style
        class << self;  attr_accessor :valid_styles end
        @valid_styles = %{INNER OUTER}

        def initialize(path, style)
            unless Join.valid_styles.include?(style)
                raise ArgumentError, "Invalid style: #{style}"
            end
            self.path = path
            self.style = style
        end

        def attrs
            attributes = {
                "path" => @path, 
                "style" => @style
            }
            return attributes
        end
    end

    class LogicNode
    end

    class LogicGroup < LogicNode

        attr_reader :left, :right, :op
        attr_accessor :parent

        def initialize(left, op, right, parent=nil)
            if !["AND", "OR"].include?(op)
                raise ArgumentError, "#{op} is not a legal logical operator"
            end
            @parent = parent
            @left = left
            @op = op
            @right = right
            [left, right].each do |node|
                if node.is_a?(LogicGroup)
                    node.parent = self
                end
            end
        end

        def to_s
            core = [@left.code, @op.downcase, @right.code].join(" ")
            if @parent && @op != @parent.op
                return "(#{core})"
            else
                return core
            end
        end

        def code
            return to_s
        end

    end

    class LogicParseError < ArgumentError
    end

    class LogicParser

        class << self;  attr_accessor :precedence, :ops end
        @precedence = {
            "AND" => 2,
            "OR"  => 1,
            "("   => 3, 
            ")"   => 3
        }

        @ops = {
            "AND" => "AND",
            "&"   => "AND",
            "&&"  => "AND",
            "OR"  => "OR",
            "|"   => "OR",
            "||"  => "OR",
            "("   => "(",
            ")"   => ")"
        }

        def initialize(query)
            @query = query
        end

        def parse_logic(str)
            tokens = str.upcase.split(/(?:\s+|\b)/).map do |x| 
                LogicParser.ops.fetch(x, x.split(//))
            end
            tokens.flatten!

            check_syntax(tokens)
            postfix_tokens = infix_to_postfix(tokens)
            ast = postfix_to_tree(postfix_tokens)
            return ast
        end

        private

        def infix_to_postfix(tokens)
            stack = []
            postfix_tokens = []
            tokens.each do |x|
                if !LogicParser.ops.include?(x)
                    postfix_tokens << x
                else
                    case x
                    when "("
                        stack << x
                    when ")"
                        while !stack.empty?
                            last_op = stack.pop
                            if last_op == "("
                                if !stack.empty?
                                    previous_op = stack.pop
                                    if previous_op != "("
                                        postfix_tokens << previous_op
                                        break
                                    end
                                end
                            else 
                                postfix_tokens << last_op
                            end
                        end
                    else
                        while !stack.empty? and LogicParser.precedence[stack.last] <= LogicParser.precedence[x]
                            prev_op = stack.pop
                            if prev_op != "("
                                postfix_tokens << prev_op
                            end
                        end
                        stack << x
                    end
                end
            end
            while !stack.empty?
                postfix_tokens << stack.pop
            end
            return postfix_tokens
        end

        def check_syntax(tokens)
            need_op = false
            need_bin_op_or_bracket = false
            processed = []
            open_brackets = 0
            tokens.each do |x|
                if !LogicParser.ops.include?(x)
                    if need_op
                        raise LogicParseError, "Expected an operator after '#{processed.join(' ')}', but got #{x}"
                    elsif need_bin_op_or_bracket
                        raise LogicParseError, "Logic grouping error after '#{processed.join(' ')}', expected an operator or closing bracket, but got #{x}"
                    end
                    need_op = true
                else
                    need_op = false
                    case x
                    when "("
                        if !processed.empty? && !LogicParser.ops.include?(processed.last)
                            raise LogicParseError, "Logic grouping error after '#{processed.join(' ')}', got #{x}"
                        elsif need_bin_op_or_bracket
                            raise LogicParseError, "Logic grouping error after '#{processed.join(' ')}', got #{x}"
                        end
                        open_brackets += 1
                    when ")"
                        need_bin_op_or_bracket = true
                        open_brackets -= 1
                    else
                        need_bin_op_or_bracket = false
                    end
                end
                processed << x
            end
            if open_brackets < 0
                raise LogicParseError, "Unmatched closing bracket in #{tokens.join(' ')}"
            elsif open_brackets > 0
                raise LogicParseError, "Unmatched opening bracket in #{tokens.join(' ')}"
            end
        end

        def postfix_to_tree(tokens)
            stack = []
            tokens.each do |x|
                if !LogicParser.ops.include?(x)
                    stack << x
                else
                    right = stack.pop
                    left = stack.pop
                    right = (right.is_a?(LogicGroup)) ? right : @query.get_constraint(right)
                    left = (left.is_a?(LogicGroup)) ? left : @query.get_constraint(left)
                    stack << LogicGroup.new(left, x, right)
                end
            end
            if stack.size != 1
                raise LogicParseError, "Tree does not have a unique root"
            end
            return stack.pop
        end

        def precedence_of(op)
            return LogicParser.precedence[op]
        end

    end

    class Template < Query

        attr_accessor :longDescription, :comment

        def initialize(model, root=nil, service=nil)
            super
            @constraint_factory = TemplateConstraintFactory.new(self)
            @url = (@service.nil?) ? nil : @service.root + Service::TEMPLATE_RESULTS_PATH
        end

        def self.parser(model)
            return TemplateLoader.new(model)
        end

        def to_xml
            doc = REXML::Document.new
            t = doc.add_element 'template', {"name" => @name, "title" => @title, "longDescription" => @longDescription, "comment" => @comment}.reject {|k,v| v.nil?}
            t.add_element super
            return t
        end

        def editable_constraints
            return coded_constraints.select {|con| con.editable}
        end

        def active_constraints
            return coded_constraints.select {|con| con.switchable != "off"}
        end

        def params 
            p = {"name" => @name}
            actives = active_constraints
            actives.each_index do |idx|
                con = actives[idx]
                count = (idx + 1).to_s
                p["constraint" + count] = con.path.to_s
                p["op" + count] = con.template_param_op
                if con.respond_to? :value
                    p["value" + count] = con.value
                elsif con.respond_to? :values
                    p["value" + count] = con.values
                elsif con.respond_to? :loopPath
                    p["loopPath" + count] = con.loopPath.to_s
                end
                if con.respond_to? :extra_value and !con.extra_value.nil?
                    p["extra" + count] = con.extra_value
                end
            end
            return p
        end

        # Returns an Enumerable of ResultRow objects containing the 
        # data returned by running this query, starting at the given offset and 
        # containing up to the given maximum size.
        #
        # The webservice enforces a maximum page-size of 10,000,000 rows,
        # independent of any size you specify - this can be obviated with paging
        # for large result sets.
        #
        #   names = template.rows("A" => "eve").map {|r| r["name"]}
        #
        def rows(params = {}, start=0, size=nil)
            runner = (params.empty?) ? self : get_adjusted(params)
            return self.class.superclass.instance_method(:rows).bind(runner).call(start, size)
        end

        def each_row(params = {}, start=0, size=nil)
            runner = (params.empty?) ? self : get_adjusted(params)
            runner.results_reader(start, size).each_row {|r| yield r}
        end

        # Return objects corresponding to the type of data requested, starting 
        # at the given row offset. Returns an Enumerable of InterMineObject,
        # where each value is read one at a time from the connection. 
        #
        #  template.results("A" => "eve").each {|gene|
        #      puts gene.symbol
        #  }
        #
        def results(params = {}, start=0, size=nil)
            runner = (params.empty?) ? self : get_adjusted(params)
            return self.class.superclass.instance_method(:results).bind(runner).call(start, size)
        end

        def each_result(params = {}, start=0, size=nil) 
            runner = (params.empty?) ? self : get_adjusted(params)
            runner.results_reader(start, size).each_result {|r| yield r}
        end

        # Return an Enumerable of summary items starting at the given offset.
        #
        #  summary = template.summary_items("chromosome.primaryIdentifier", {"A" => "Pentose Phosphate Pathway"})
        #  top_chromosome = summary[0]["item"]
        #  no_in_top_chrom = summary[0]["count"]
        #
        # This can be made more efficient by passing in a size - ie, if 
        # you only want the top item, pass in an offset of 0 and a size of 1
        # and only that row will be fetched.
        #
        def summaries(path, params = {}, start=0, size=nil)
            runner = (params.empty?) ? self : get_adjusted(params)
            return self.class.superclass.instance_method(:summaries).bind(runner).call(path, start, size)
        end

        # Return a summary for a column as a Hash
        #
        # For numeric values the hash has four keys: "average", "stdev", "min", and "max". 
        #
        #  summary = template.summarise("length", {"A" => "eve"})
        #  puts summary["average"]
        #
        # For non-numeric values, the hash will have each possible value as a key, and the
        # count of the occurrences of that value in this query's result set as the 
        # corresponding value:
        # 
        #  summary = template.summarise("chromosome.primaryIdentifier", {"A" => "eve"})
        #  puts summary["2L"]
        #
        # To limit the size of the result set you can use start and size as per normal 
        # queries - this has no real effect with numeric queries, which always return the 
        # same information.
        #
        def summarise(path, params={}, start=0, size=nil)
            t = make_path(add_prefix(path)).end_type
            if InterMine::Metadata::Model::NUMERIC_TYPES.include? t
                return Hash[summaries(path, params, start, size).first.map {|k, v| [k, v.to_f]}]
            else
                return Hash[summaries(path, params, start, size).map {|sum| [sum["item"], sum["count"]]}]
            end
        end

        # Return the number of result rows this query will return in its current state. 
        # This makes a very small request to the webservice, and is the most efficient 
        # method of getting the size of the result set.
        def count(params = {})
            runner = (params.empty?) ? self : get_adjusted(params)
            runner.results_reader.get_size
        end

        # Return a clone of the current template. Changes made to the clone will not effect
        # the cloned query.
        def clone
            other = super
            other.instance_variable_set(:@constraints, @constraints.map {|c| c.clone})
            return other
        end

        private 

        def get_adjusted(params)
            adjusted = clone
            params.each do |k,v| 
                con = adjusted.get_constraint(k)
                raise ArgumentError, "There is no constraint with code #{k} in this query" unless con
                path = con.path.to_s
                adjusted.remove_constraint(k)
                adjusted.where(path => v)
                adjusted.constraints.last.code = k
            end
            return adjusted
        end
    end

    class TemplateConstraintFactory < ConstraintFactory
    end

end
