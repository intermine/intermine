require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveDemoTest <  Test::Unit::TestCase

    def setup
        @service = Service.new("http://localhost/intermine-test", "test-user-token")
        @temp_lists = []
    end


    def teardown
        @temp_lists.each do |l| 
            begin
                @service.delete_lists(l)
            rescue
            end
        end
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

    def testGetTemplate
        template = @service.template("ManagerLookup")
        assert_equal("ManagerLookup", template.name)
        assert_equal(1, template.constraints.length)
    end

    def testGetLists
        list = @service.list("My-Favourite-Employees")
        assert_equal(4, list.size)
        assert_match("CURRENT", list.status)
    end

    def testListEnumerability
        list = @service.list("My-Favourite-Employees")
        names = list.map {|emp| emp.name }
        exp = [ "David Brent", "Neil Godwin", "Bernd Stromberg", "Timo Becker"]
        assert_equal(exp, names)

        old = list.select {|emp| emp.age > 43}
        assert_equal(1, old.size)
        assert_equal("Neil Godwin", old.first.name)

        sum = list.reduce(0) {|m, i| m + i.age}
        assert_equal(159, sum)
    end

    def testLazyReferenceFetching
        list = @service.list("My-Favourite-Employees")

        deps = list.map {|emp| emp.department.name }
        exp = ["Sales", "Human Resources", "Schadensregulierung M-Z", "Schadensregulierung"]
        assert_equal(exp, deps)

        comps = list.map {|emp| emp.department.company.name }
        exp = ["Wernham-Hogg", "Wernham-Hogg", "Capitol Versicherung AG", "Capitol Versicherung AG"]
        assert_equal(exp, comps)
    end

    def testLazyCollectionFetching
        list = @service.list("My-Favourite-Employees")
        emps = list.map {|manager| manager.department.employees.map {|employee| employee.age} }
        exp = [
            [34, 36, 41, 55, 61, 61],
            [44, 49, 62],
            [30, 37, 45, 46, 58, 64],
            [36, 37, 39, 49, 57, 59]
        ]
        assert_equal(exp, emps)

        sum = list.reduce(0) {|m, manager| m + manager.department.employees.reduce(0) {|n, emp| n + emp.age}}
        assert_equal(1000, sum)
    end

    def testListCreation
        ids = %{Alex Brenda Carol David Edgar}
        new_list = @service.create_list(ids, "Employee")
        @temp_lists << new_list
        assert_equal(3, new_list.size)
    end

    def testListTagging
        ids = %{Alex Brenda Carol David Edgar}
        new_list = @service.create_list(ids, "Employee")
        @temp_lists << new_list
        assert_equal([], new_list.tags)
        new_list.add_tags("a-tag", "another-tag")
        assert_equal(2, new_list.tags.size)
        assert_equal(["a-tag", "another-tag"].sort, new_list.tags.sort)
    end

    def testListTagRemoval
        ids = %{Alex Brenda Carol David Edgar}
        tags = ["a-tag", "another-tag"]
        new_list = @service.create_list(ids, "Employee", tags)
        @temp_lists << new_list
        assert_equal(2, new_list.tags.size)
        new_list.remove_tags("another-tag")
        assert_equal(["a-tag"], new_list.tags)
        new_list.remove_tags("a-tag")
        assert_equal([], new_list.tags)
        new_list.remove_tags("a-non-existent-tag")
        assert_equal([], new_list.tags)
    end

    def testListTagUpdating
        ids = %{Alex Brenda Carol David Edgar}
        tags = ["a-tag", "another-tag"]
        new_list = @service.create_list(ids, "Employee")
        @temp_lists << new_list
        assert_equal([], new_list.tags)
        manager = @service.instance_variable_get("@list_manager")
        assert_equal(2, manager.add_tags(new_list, tags).size)
        assert_equal([], new_list.tags)
        new_list.update_tags
        assert_equal(tags.sort, new_list.tags.sort)
    end

    def testEnrichment
        l = @service.list("My-Favourite-Employees")
        contractors = l.calculate_enrichment :contractor_enrichment, :maxp => 1.0
        assert_equal("Vikram", contractors.first[:identifier])
    end

end
