$LOAD_PATH << File.expand_path( File.dirname(__FILE__) + '/../lib' )
require "intermine/model"
require "intermine/query"
require "intermine/results"
include PathQuery

d = File.new(File.dirname(__FILE__) + "/data/model.json").read
m = Model.new(d)
q = Query.new(m, "Employee")
q.add_views("name", "age")
q.add_constraint({:path => "age", :op => ">", :value => 40})
q.add_sort_order("age")
params = {"query" => q.to_xml, "format" => "jsonrows"}
uri = "http://squirrel.flymine.org/intermine-test/service/query/results"
rr = Results::ResultsReader.new(uri, params, q.views)
sum, total = 0, 0
rr.each_row {|emp| 
    puts emp
    sum += emp["age"]
    total += 1
}
puts "Average age: #{sum/total} years"
