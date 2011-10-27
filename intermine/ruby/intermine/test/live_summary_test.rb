require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveSummaryTest < Test::Unit::TestCase

    def setup
        @service = Service.new("http://localhost/intermine-test")
        @query = @service.query("Employee").where(:age => {:lt => 50})
    end

    def testIteration
        count = 0
        @query.summaries("age").each {|summary|
            assert_equal("36.3561643835616438", summary["average"])
            assert(summary["max"] < 50)
            count += 1
        }
        assert_equal(1, count)

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
        assert(summary["average"] > 36.356)
        assert(summary["average"] < 36.3562)
    end

    def testNonNumericSummary
        summary = @query.summarise("name")
        assert_equal(1, summary["Tim Canterbury"])
    end

    def testTop
        summary = @query.summaries("department.name")
        top = summary.first
        assert_equal("Sales", top["item"])
        assert_equal(11, top["count"])
    end

    def testTemplateSummary
        template = @service.template("CEO_Rivals")
        template_params = {"A" => {"!=" => "Charles Miner"}}

        summary = template.summarise("salary", template_params)
        assert_equal(6813474.8, summary["average"])

        summary = template.summarise("company.name", template_params)
        assert_equal(1, summary["Gogirep"])
    end

end
