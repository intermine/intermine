require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveSummaryTest < Test::Unit::TestCase

    def setup
        service = Service.new("http://localhost/intermine-test")
        @query = service.query("Employee").where(:age => {:lt => 50})
    end

    def testIteration
        rr = @query.results_reader
        count = 0
        rr.each_summary("age") {|summary|
            assert_equal("36.3561643835616438", summary["average"])
            assert(summary["max"] < 50)
            count += 1
        }
        assert_equal(1, count)

        rr = @query.results_reader
        count = 0
        rr.each_summary("name") {|summary|
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
        summary = @query.summary_items("department.name")
        assert_equal("Sales", summary[0]["item"])
        assert_equal(11, summary[0]["count"])
    end

end
