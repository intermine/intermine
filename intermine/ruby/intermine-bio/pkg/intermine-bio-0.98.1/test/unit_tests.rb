require File.dirname(__FILE__) + "/test_helper.rb"

require "intermine/bio"
require "test/unit"

class MockQuery
    def service
        return MockService.new
    end
end

class MockService
    def root 
        return "http://www.flymine.org/query/service"
    end
end

module InterMine

    module PathQuery

        class Query

            def results_reader
                return InterMine::Results::ResultsReader.new(MockQuery.new)
            end

        end

    end

    module Results

        class ResultsReader

            attr_reader :uri

            def initialize(mock_query)
                @query = mock_query
            end

            def each_line(params)
                f = File.new(File.dirname(__FILE__) + "/data/test.#{params[:format]}", "r")
                f.each_line {|line| yield line}
            end

            def params(format)
                return {:format => format}
            end

            unless ENV['LIVE_TESTING'] == "1"
                def adjust_path(*args)
                    # disable, as it makes requests
                end
            end

        end
    end
end

class TestBio < Test::Unit::TestCase

    def setup
        @rr = InterMine::Results::ResultsReader.new(MockQuery.new)
    end

    def testServiceResolution
        if ENV['LIVE_TESTING'] == "1"
            @rr.adjust_path("bed")
            assert_equal("/query/service/query/results/bed", @rr.uri.path)
            @rr.adjust_path("gff3")
            assert_equal("/query/service/query/results/gff3", @rr.uri.path)
            @rr.adjust_path("fasta")
            assert_equal("/query/service/query/results/fasta", @rr.uri.path)
        end
    end

    def testBedParsing
        assert_equal(InterMine::PathQuery::Query.new.bed, File.new(File.dirname(__FILE__) + "/data/test.bed", "r").read)

        c = 0
        header = InterMine::PathQuery::Query.new.bed {|b| c += 1}
        assert_equal(4, c)
        assert_match(/Source: FlyMine/, header)
    end

    def testGff3Parsing
        assert_equal(InterMine::PathQuery::Query.new.gff3, File.new(File.dirname(__FILE__) + "/data/test.gff3", "r").read)

        c = 0
        header = InterMine::PathQuery::Query.new.gff3 {|g| c += 1}
        assert_equal(4, c)
        assert_match(/gff-version 3/, header)
    end

    def testFastaParsing
        assert_equal(InterMine::PathQuery::Query.new.fasta, File.new(File.dirname(__FILE__) + "/data/test.fasta", "r").read)

        c = 0
        last = nil
        InterMine::PathQuery::Query.new.fasta {|f| c += 1; last = f}
        assert_equal(4, c)
        assert_match(/BIB_DROME 362030490/, last)
    end

end






