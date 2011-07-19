require 'rubygems'
require 'rest-open-uri'
require 'intermine/model'
require "intermine/query"

class Service

    VERSION_PATH = "/version"
    MODEL_PATH = "/model/json"
    QUERY_RESULTS_PATH = "/query/results"

    attr_reader :version, :root, :token

    def initialize(root, token=nil, mock_model=nil)
        @root = root
        @token = token
        @version = fetch(@root + VERSION_PATH).to_i
        @model = mock_model
    end

    def model
        if @model.nil?
            data = fetch(@root + MODEL_PATH)
            @model = Model.new(data, self)
        end
        @model
    end

    def new_query(rootClass=nil)
        return PathQuery::Query.new(self.model, rootClass, self)
    end

    private

    def prepare_uri(url)
        uri = URI(url)
        if uri.query
            uri.query += "&token=#{@token}" if @token
        else
            uri.query = "token=#{@token}" if @token
        end
        return uri
    end

    def fetch(url)
        uri = prepare_uri(url)
        return uri.open.read
    end
end


