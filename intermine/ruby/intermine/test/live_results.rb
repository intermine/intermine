require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"
require "intermine/results"

class LiveResultsTest < Test::Unit::TestCase

    def setup
        @service = Service.new("http://localhost/intermine-test")
        @max = 50
        @q = @service.query("Employee").select("*", "department.*").where(:age => {:lt => @max})
        @empty = @service.query("Employee").where(:name => "some-non-existant-value")
        @exp_count = 82
        @expected_last = "Didier Legu\303\251lec"
        @expected_last_summary = "Vincent"
        @expected_last_obj = "Both Quotes '\""
        @expected_target = 37.02439
        @expected_two_vowels = 31
        @exp_trideps = 5
        @exp_s_deps = 29
        @exp_row_size = 7
        @rows_args = []
        @results_args = []
        @summary_args = []
        @count_args = []
        @dep_size = 3
    end

    def testRowIteration
        count = 0
        @q.rows(*@rows_args).each do |row|
            assert_equal(@exp_row_size, row.size)
            assert(row["age"].is_a? Fixnum)
            assert(row["age"] < @max)
            assert(row["name"].is_a? String)
            count += 1
        end
        assert_equal(@exp_count, count)
    end

    def testEmptyRows
        count = 0
        @empty.rows.each do
            count += 1
        end
        assert_equal(0, count)
    end

    def testRowMapping
        names = @q.rows(*@rows_args).map {|r| r["name"]}
        assert_equal(@exp_count, names.length)
        assert_equal(@expected_last, names.last)
    end

    def testRowFolding
        average = @q.rows(*@rows_args).reduce(0.0) {|m, i| m + i["age"]} / @q.count(*@count_args)
        target = @expected_target
        assert(average > target * 0.999999)
        assert(average < target * 1.000001)
    end
        
    def testRowFiltering
        two_vowels = @q.rows(*@rows_args).select {|r| r["name"].match(/[aeiou]{2}/) }
        assert_equal(@expected_two_vowels, two_vowels.length)
    end

    def testObjectIteration
        count = 0
        @q.results(*@results_args).each do |emp|
            assert(emp.is_a? @q.model.table("Employee"))
            assert(emp.age.is_a? Fixnum)
            assert(emp.age < @max)
            assert(emp.name.is_a? String)
            count += 1
        end
        assert_equal(@exp_count, count)
    end

    def testEmptyObjects
        count = 0
        @empty.results.each do
            count += 1
        end
        assert_equal(0, count)
    end

    def testObjectMapping
        names = @q.results(*@results_args).map {|r| r.name}
        assert_equal(@exp_count, names.length)
        assert_equal(@expected_last_obj, names.last)
    end

    def testObjectFolding
        average = @q.results(*@results_args).reduce(0.0) {|m, i| m + i.age} / @q.count(*@count_args)
        target = @expected_target
        assert(average > target * 0.999999, "Checking lower bound on #{average}")
        assert(average < target * 1.000001, "Checking upper bound on #{average}")
    end
        
    def testObjectFiltering
        emps_in_s_deps = @q.results(*@results_args).select {|o| o.department.name.start_with? "S"}
        assert_equal(@exp_s_deps, emps_in_s_deps.length)
    end

    def testSummaryIteration
        count = 0
        @q.summaries("name", *@summary_args).each do |summary|
            assert_equal(1, summary["count"])
            count += 1
        end
        assert_equal(@exp_count, count)
    end

    def testEmptySummaries
        count = 0
        @empty.summaries("name").each do
            count += 1
        end
        assert_equal(0, count)
    end

    def testSummaryMapping
        names = @q.summaries("name", *@summary_args).map {|s| s["item"]}
        assert_equal(@exp_count, names.length)
        assert_equal(@expected_last_summary, names.last)
    end

    def testSummaryFolding
        sum = @q.summaries("name", *@summary_args).reduce(0) {|m, i| m + i["count"]}
        assert_equal(@q.count(*@count_args), sum)
    end
        
    def testSummaryFiltering
        trideps = @q.summaries("department.name", *@summary_args).select {|s| s["count"] == @dep_size}
        assert_equal(@exp_trideps, trideps.length)
    end
end

class LiveTemplateResultsTest < LiveResultsTest 

    def setup
        @service = Service.new("http://localhost/intermine-test")
        @q = @service.template("employeesOverACertainAgeFromDepartmentA")
        @empty = @q
        @exp_count = 18
        @expected_last = "Tim Canterbury"
        @expected_last_summary = "Tim Canterbury"
        @expected_last_obj = "Fatou"
        @expected_target = 47.277778
        @expected_two_vowels = 4
        @dep_size = 18
        @exp_trideps = 1
        @exp_s_deps = 18
        @max = 69
        @rows_args = [{"B" => "Sales"}]
        @results_args = [{"B" => "Sales"}]
        @summary_args = [{"B" => "Sales"}]
        @count_args = [{"B" => "Sales"}]
        @exp_row_size = 3

    end
end

