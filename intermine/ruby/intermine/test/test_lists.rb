require File.dirname(__FILE__) + "/test_helper.rb"

require "test/unit"
require "intermine/lists"
require "intermine/model"
require "intermine/query"

include Lists

class FakeService

    attr_reader :get_list_data, :root

    def initialize(data, model)
        @get_list_data = data
        @model = model
        @root = "FAKE_ROOT"
    end

    def query(rootClass=nil?)
        return PathQuery::Query.new(@model, rootClass, self)
    end

end

class TestList < Test::Unit::TestCase

    def initialize(name)
        super
        d = File.dirname(__FILE__) + "/data"
        lf = File.new(d + "/lists.json", "r")
        mf = File.new(d + "/model.json", "r")
        model = Model.new(mf.read)
        @service = FakeService.new(lf.read, model)
    end
    
    def setup
        @manager = ListManager.new(@service)
    end

    def testParse
        assert_equal(@manager.lists.size, 3)

        list_a = @manager.lists.first
        assert_equal(list_a.name, "test-list-1")
        assert_equal(list_a.title, "test1")
        assert_equal(list_a.description, "An example test list")
        assert_equal(list_a.size, 42)
        assert_equal(list_a.dateCreated, "2011-05-07T19:52:03")
        assert_equal(list_a.tags, %w{tag1 tag2 tag3})
        assert_equal(list_a.is_authorized?, true)

        list_b = @manager.lists[1]
        assert_equal(list_b.tags, [])
        assert_equal(list_b.is_authorized?, false)

        list_c = @manager.lists.last
        assert_equal(list_c.is_authorized?, true)
    end

    def testListToQuery
        list_a = @manager.lists.first
        q = list_a.query
        expected = %q!<query model='testmodel' view='Employee.name Employee.end Employee.id Employee.fullTime Employee.age' sortOrder='Employee.name ASC'><constraint op='IN' code='A' value='test-list-1' path='Employee'/></query>!
        assert_equal(q.to_xml.to_s, expected)
    end
end
    
