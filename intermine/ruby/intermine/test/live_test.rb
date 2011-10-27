require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/service"

class LiveDemoTest <  Test::Unit::TestCase

    def setup
        @service = Service.new("http://localhost/intermine-test", "Z1a3D3U16cicCdS0T6y4bdN1SQh")
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

end
