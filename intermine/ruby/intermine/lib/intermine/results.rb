require 'rubygems'
require "json"
require 'stringio'
require 'net/http'

# == Code for handling results
#
# The classes in this module are used to process
# the results of queries. They include mechanisms 
# for reading from the connection in a record 
# orientated format, and for interpreting the values 
# received.
#
module InterMine::Results

    # == Synopsis
    #
    #   query.each_row do |row|
    #     puts row[0], row[1], row[2]
    #     puts row["symbol"], row["proteins.name"]
    #   end
    #
    # == Description
    #
    # A dual-faced object, these representations of rows 
    # of results are intended to provide a single object that 
    # allows Array-like and Hash-like access to an individual
    # row of data. The primary means of access for this is
    # the use of item access with ResultsRow#[], which accepts
    # Array like index arguments, as well as Hash like key arguments.
    #
    # As well as value retrieval via indices and keys, iteration is also supported
    # with ResultsRow#each and the aliases each_pair and each_value. If
    # one parameter is requested the iteration will be by value, if 
    # two are requested it will be by pair. 
    #
    # There is no attempt to fully emulate the complete Hash and Array 
    # interfaces - that would make little sense as it does not make any 
    # sense to insert values into a row, or to re-sort it in place. If you 
    # want to do such things, call ResultsRow#to_a or ResultsRow#to_h to
    # get real Hash or Array values you are free to manipulate.
    #
    #:include:contact_header.rdoc
    #
    class ResultsRow

        # Construct a new result row. 
        #
        # You will not need to do this - ResultsRow objects are created for
        # you when the results are parsed.
        #
        def initialize(results, columns)
            @results = results.is_a?(Array) ? results : JSON.parse(results)
            unless @results.is_a?(Array)
                raise ArgumentError, "Bad results format: #{results}"
            end
            unless @results.size == columns.size
                raise ArgumentError, "Column size (#{columns.size}) != results size (#{@results.size})"
            end

            @columns = columns
        end

        # == Retrieve values from the row
        #
        #   row[0]              # first element
        #   row[-1]             # last element
        #   row[2, 3]           # Slice of three cells, starting at cell no. 3 (index 2)
        #   row[1..3]           # Slice of three cells, starting at cell no. 2 (index 1)
        #   row["length"]       # Get a cell's value by column name
        #   row["Gene.length"]  # Use of the root class is optional
        #   row[:length]        # For bare attributes, a symbol may be used.
        #
        # This method emulated both the array and hash style of access, based
        # on argument type. Passing in integer indexes or ranges retrieves
        # values in a manner that treats the row as a list of values. Passing in 
        # string or symbols retrieves values in a manner that treates the 
        # row as a Hash. It is possible to access the values in the row by using 
        # either the short or long forms of the column name.
        #
        # Unlike the corresponding method in either Hash or Array, this method
        # throws errors when trying to access single elements (not when requesting
        # array slices) and the result cannot be found.
        #
        def [](arg, length=nil)
            unless length.nil?
                raise ArgumentError, "when providing a length, the first argument must be an Integer" unless arg.is_a? Integer
                return @results[arg, length].map {|x| x["value"]}
            end

            case arg
            when Range
                return @results[arg].map {|x| x["value"]}
            when Integer
                idx = arg
            else
                idx = index_for(arg)
            end

            raise ArgumentError, "Bad argument: #{arg}" if idx.nil?

            cell = @results[idx]

            raise IndexError, "Argument out of range" if cell.nil?

            return cell["value"]
        end

        # Returns the first value in this row
        def first
            return @results[0]["value"]
        end

        # Returns the last value in this row
        def last
            return @results.last["value"]
        end

        # Iterate over the values in this row in the
        # order specified by the query's view.
        # 
        #   row.each do |value|
        #     puts value
        #   end
        #
        #   row.each do |column, value|
        #     puts "#{column} is #{value}"
        #   end
        #
        # If one parameter is specified, then the iteration 
        # simply includes values, if more than one is specified,
        # then it will be by column/value pairs.
        #
        def each(&block) 
            if block
                if block.arity == 1
                    @results.each {|x| block.call(x["value"])}
                else block.arity == 2
                    (0 ... @results.size).to_a.each {|i| block.call(@columns[i], @results[i]["value"])}
                end
            end
            return self
        end

        alias each_value each
        alias each_pair each

        # Return the number of cells in this row.
        def size
            return @results.size
        end

        alias length size

        # Return an Array version of the row.
        def to_a
            return @results.map {|x| x["value"]}
        end

        # Return a Hash version of the row. All keys in this
        # hash will be full length column headers.
        def to_h
            hash = {}
            @results.each_index do |x|
                key = @columns[x].to_s
                hash[key] = self[x]
            end
            return hash
        end

        # Return a readable representation of the information in this row
        def to_s 
            bufs = []
            @results.each_index do |idx|
                buffer = ""
                buffer << @columns[idx].to_headless_s
                buffer << "="
                buffer << self[idx].to_s
                bufs << buffer
            end
            return @columns.first.rootClass.name + ": " + bufs.join(",\t")
        end

        # Return the information in this row as a line suitable for prining in a 
        # CSV or TSV file. The optional separator argument will be used to delimit
        # columns
        def to_csv(sep="\t")
            return @results.map {|x| x["value"].inspect}.join(sep)
        end

        private

        # Return the index for a string or symbol key.
        def index_for(key)
            if @indexes.nil?
                @indexes = {}
                @results.each_index do |idx|
                    idx_key = @columns[idx]
                    @indexes[idx_key.to_s] = idx

                    ## Include root-less paths as aliases
                    # But allow for string columns
                    if idx_key.respond_to?(:to_headless_s)
                        @indexes[idx_key.to_headless_s] = idx
                    end
                end
            end
            return @indexes[key.to_s]
        end
    end

    # The class responsible for retrieving results and processing them
    #
    #   query.each_row do |row|
    #     puts row[2..3]
    #   end
    #
    # Queries delegate their handling of results to these objects, which
    # are responsible for creating the ResultsRow objects or model objects 
    # that the results represent.
    #
    #:include:contact_header.rdoc
    #
    class ResultsReader

        # Construct a new ResultsReader.
        #
        # You will not need to do this yourself. It is handled by 
        # queries themselves.
        #
        def initialize(uri, query, start, size)
            @uri = URI(uri)
            @query = query
            @start = start
            @size = size
        end

        # Run a request to get the size of the result set.
        def get_size
            query = params("jsoncount")
            res = Net::HTTP.post_form(@uri, query)
            case res
            when Net::HTTPSuccess
                return check_result_set(res.body)["count"]
            else
                check_result_set(res.body)
                res.error!
            end
        end

        # Iterate over the result set one ResultsRow at a time
        def each_row
            processor = lambda {|line|
                x = line.chomp.chomp(",")
                x.empty? ? nil : ResultsRow.new(x, @query.views)
            }
            read_result_set(params("jsonrows"), processor) {|x|
                yield x
            }
        end

        # Iterate over the resultset, one object at a time, where the
        # object is the instantiation of the type of object specified as the 
        # query's root type. The actual type returned depends on the query
        # itself, and any subclass information returned by the webservice.
        #
        #   query = service.query("Gene").select("*")
        #   query.each_result do |gene|
        #     puts gene.symbol, gene.length
        #   end
        #
        #   query = service.query("Organism").select("*")
        #   query.each_result do |organism|
        #     puts organism.shortName, organism.taxonId
        #   end
        #
        def each_result
            model = @query.model
            processor = lambda {|line|
                x = line.chomp.chomp(",")
                x.empty? ? nil : model.make_new(JSON.parse(x))
            }
            read_result_set(params("jsonobjects"), processor) {|x|
                yield x
            }
        end

        def each_summary(summary_path)
            extra = {"summaryPath" => @query.add_prefix(summary_path)}
            p = params("jsonrows").merge(extra)
            processor = lambda {|line|
                x = line.chomp.chomp(",")
                x.empty? ? nil : JSON.parse(x)
            }
            read_result_set(p, processor) {|x|
                yield x
            }
        end

        def read_result_set(parameters, processor)
            container = ''
            in_results = false
            each_line(parameters) do |line|
                if line.start_with?("]")
                    in_results = false
                end
                if in_results
                    begin
                        row = processor.call(line)
                    rescue => e
                        raise ServiceError, "Error parsing '#{line}': #{e.message}"
                    end
                    unless row.nil?
                        yield row
                    end
                else
                    container << line
                    if line.chomp($/).end_with?("[")
                        in_results = true
                    end
                end
            end
            check_result_set(container)
        end

        private

        # Retrieve the results from the webservice, one line at a time. 
        # This method has responsibility for ensuring that the lines are 
        # complete, and not split over two or more chunks.
        def each_line(data)
            req = Net::HTTP::Post.new(@uri.path)
            req.set_form_data(data)
            Net::HTTP.new(@uri.host, @uri.port).start {|http|
                http.request(req) {|resp|
                    holdover = ""
                    resp.read_body {|chunk|
                        sock = StringIO.new(holdover + chunk)
                        sock.each_line {|line|
                            if sock.eof?
                                holdover = line
                            else
                                unless line.empty?
                                    yield line
                                end
                            end
                        }
                        sock.close
                    }
                    yield holdover
                }
            }
        end


        # Get the parameters for this request, given the specified format.
        def params(format="tab")
            p = @query.params.merge("start" => @start, "format" => format)
            p["size"] = @size unless @size.nil?
            return p
        end

        # Check that the request was successful according the metadata
        # passed with the result.
        def check_result_set(container)
            begin
                result_set = JSON.parse(container)
            rescue JSON::ParserError => e
                raise "Error parsing container: #{container}, #{e.message}"
            end
            unless result_set["wasSuccessful"]
                raise ServiceError, result_set["error"]
            end
            result_set
        end
    end

    class RowReader < ResultsReader

        include Enumerable

        alias :each :each_row
    end

    class ObjectReader < ResultsReader

        include Enumerable

        alias :each :each_result

    end

    class SummaryReader < ResultsReader

        include Enumerable

        def initialize(uri, query, start, size, path)
            super(uri, query, start, size)
            @path = path
        end

        def each
            each_summary(@path) {|x| yield x}
        end
    end

    # Errors if there are problems retrieving results from the webservice.
    class ServiceError < RuntimeError
    end


end
