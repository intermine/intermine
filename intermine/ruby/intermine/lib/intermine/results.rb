require 'rubygems'
require "json"
require 'stringio'
require 'net/http'
require 'addressable/uri'
require 'rest-open-uri'

module Results

    class ResultsRow

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

        def [](key)
            if key.is_a?(Integer)
                idx = key
            else
                idx = index_for(key)
            end
            if idx.nil?
                raise IndexError, "Bad key: #{key}"
            end
            begin
                result = @results[idx]["value"]
            rescue NoMethodError
                raise IndexError, "Bad key: #{key}"
            end
            return result
        end

        def to_a
            return @results.map {|x| x["value"]}
        end

        def to_h
            hash = {}
            @results.each_index do |x|
                key = @columns[x].to_s
                hash[key] = self[x]
            end
            return hash
        end

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

        def to_csv
            return @results.map {|x| x["value"].to_s}.join("\t")
        end

        private

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
            return @indexes[key]
        end
    end


    class ResultsReader

        def initialize(uri, query)
            @uri = URI(uri)
            @http = Net::HTTP.new(@uri.host, @uri.port)
            @query = query
        end

        def get_each_line
            buffer = StringIO.new
            last = 0
            current = nil
            @http.request_get(@uri.path) do |res|
                res.read_body do |chunk|
                buffer << chunk
                current = buffer.pos
                buffer.seek(last)
                while buffer.pos < current
                    yield buffer.readline
                end
                last = buffer.pos
                buffer.seek(current)
                end
            end
        end

        def get_query_string
            bits = []
            @query.params.each do |k, v|
                if v.is_a?(Array)
                    v.each do |x|
                        bits << [k, x]
                    end
                else
                    bits << [k, v]
                end
            end

            return Addressable::URI.form_encode(bits)
        end

        def get_size
            query = get_query_string + "&format=jsoncount"
            container = @uri.open(:method => :post, :body => query).read
            result_set = check_result_set(container)
            return result_set["count"]
        end

        def each_row
            query = get_query_string + "&format=jsonrows"
            @uri.open(:method => :post, :body => query) do |f|
                container = ''
                f.each_line do |line|
                    if line.start_with?("[")
                        begin
                            row = ResultsRow.new(line.chomp("," + $/), @query.views)
                        rescue => e
                            raise ServiceError, "Error parsing #{line}: #{e.message}"
                        end
                        yield row
                    else
                        container << line
                    end
                end
                check_result_set(container)
            end

        end

        def each_result
            query = get_query_string + "&format=jsonobjects"
            model = @query.model
            @uri.open(:method => :post, :body => query) do |f|
                container = ''
                f.each_line do |line|
                    line.chomp!("," + $/)
                    if line.start_with?("{") and line.end_with?("}")
                        begin
                            data = JSON.parse(line)
                            result = model.make_new(data)
                        rescue JSON::ParserError => e
                            raise ServiceError, "Error parsing #{line}: #{e.message}"
                        rescue => e
                            raise ServiceError, "Could not instantiate this result object: #{e.message}"
                        end
                        yield result
                    else
                        container << line
                    end
                end
                check_result_set(container)
            end

        end

        private

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

    class ServiceError < RuntimeError
    end


end
