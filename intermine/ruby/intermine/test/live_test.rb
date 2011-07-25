require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveTest < Test::Unit::TestCase

    def setup
        @service = Service.new("http://www.flymine.org/query/service")
    end

    # Tests a number of integrated features:
    #  * Getting a template
    #  * Passing template parameters
    #  * getting counts
    #  * getting rows
    #  * getting records
    #
    def testBigResultSet
        template = @service.template("Chromosome_Gene")
        args = {"A" => {"!=" => '2L'}}
        size = template.count(args)
        i = 0
        template.each_row(args) do |r|
            i += 1
        end
        assert_equal(size, i)
        i = 0
        template.each_result(args) do |r|
            i += 1
        end
        assert_equal(size, i)
    end
end

