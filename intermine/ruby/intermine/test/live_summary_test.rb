require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveSummaryTest < Test::Unit::TestCase

    def setup
        @service = Service.new("http://localhost/intermine-test")
        @query = @service.query("Employee").where(:age => {:lt => 50})
    end

    def testIteration
        @query.summaries("age").each {|summary|
            assert_equal("37.0243902439024390", summary["average"])
            assert(summary["max"] < 50)
        }

        count = 0
        @query.summaries("name").each {|summary|
            assert_equal(1, summary["count"])
            count += 1
        }
        assert_equal(count, @query.count)

    end

    def testNumericSummary
        summary = @query.summarise("age")
        assert(summary["max"] < 50)
        # Work around float comparison errors, grrr
        assert(summary["average"] > 37.024)
        assert(summary["average"] < 37.025)
    end

    def testNonNumericSummary
        summary = @query.summarise("name")
        assert_equal(1, summary["Tim Canterbury"])
    end

    def testTop
        summary = @query.summaries("department.name")
        top = summary.first
        assert_equal("Warehouse", top["item"])
        assert_equal(12, top["count"])
    end

    def testTemplateSummary
        template = @service.template("CEO_Rivals")
        template_params = {"A" => {"!=" => "Charles Miner"}}

        summary = template.summarise("salary", template_params)
        assert_equal(529119.4, summary["average"])

        summary = template.summarise("company.name", template_params)
        assert_equal(1, summary["Gogirep"])
    end

end
