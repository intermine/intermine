require "query"
require "model"

query = PathQuery::Query.new("Employee")
query.name = "Ruby Query"
query.model = "testmodel"
query.title = "A query made in ruby"
query.sort_order = "Employee asc"
query.add_views("Employee.name", "Employee.age")
query.add_views("fullTime", "department.name")

bin_params = {:path => "Employee.name", :op => "=", :value => "Foo"}
bin2_params = {:path => "age", :op => ">", :value => "26"}
unary_params = {:path => "Employee.name", :op => "IS NULL"}
sub_params = {:path => "Employee", :sub_class => "Manager"}
lookup_params = {:path => "Employee", :op => "LOOKUP", :extra_value => "Foo", :value => "bar" }
lookup_params2 = {:path => "Employee", :op => "LOOKUP", :value => "bar" }
multi_params = {:path => "Employee.name", :op => "ONE OF", :values => ["one", "two", "three"]}
loop_params = {:path => "Employee.name", :op => "IS", :loopPath => "Manager"}
list_params = {:path => "Employee.name", :op => "IN", :value => "a list"}

query.add_constraint(bin_params)
query.add_constraint(bin2_params)
query.add_constraint(sub_params)
query.add_constraint(lookup_params)
query.add_constraint(lookup_params2)
query.add_constraint(multi_params)
query.add_constraint(unary_params)
query.add_constraint(loop_params)
query.add_constraint(list_params)

query.add_join("Employee.department", "OUTER")
query.add_join("Employee.department.company")
query.add_join("Employee.department.company", "INNER")

puts query.to_xml

file = File.new("model.json", "r")

data = file.read
model = Model.new(data)

p model.get_class("Employee").get_field("department").referencedType
p model.get_class("Employee").get_field("department").reverseReference
p model.get_class("Employee").get_field("department").referencedType.get_field("company").referencedType
p model.get_class("Employee").get_field("department").referencedType.get_field("company").referencedType.name

path = Path.new("Employee.name", model)
puts path, path.length

path = Path.new("Employee.department.company.departments", model)
puts path, path.length

path = Path.new("Employee.department.company.departments.employees.address.address", model)
puts path, path.length

path = Path.new("Department.employees.seniority", model, {"Department.employees" => "Manager"})
puts path, path.length

begin
    path = Path.new("Department.employees.foo", model, {"Department.employees" => "Manager"})
rescue Exception => e
    p e
end

begin
    path = Path.new("Department.employees.seniority", model, {"Department.employees" => "Foo"})
rescue Exception => e
    p e
end

begin
    path = Path.new("Employee.department.name.departments", model)
rescue Exception => e
    p e
end

begin
    path = Path.new("Foo.bar", model)
rescue Exception => e
    p e
end


