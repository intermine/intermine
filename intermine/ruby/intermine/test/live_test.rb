require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveDemoTest <  Test::Unit::TestCase

    def setup
        @service = Service.new("http://localhost:8080/intermine-test")
    end

    def testVersion
        assert(@service.version >= 6)
    end

    def testRelease
        assert_match(/test/i, @service.release)
    end

    def testCount
        assert_equal(7, @service.query("Company").count)
        assert_equal(3, @service.query("Department").where(:name => "Sales").count)
    end

end

class LiveFlyMineTest < Test::Unit::TestCase

    def setup
        @service = Service.new("www.flymine.org/query")
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

