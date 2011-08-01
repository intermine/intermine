require File.dirname(__FILE__) + "/test_helper.rb"
require "intermine/model"

require "test/unit"

class TestModel < Test::Unit::TestCase

    def initialize(name)
        super
        file = File.new(
            File.dirname(__FILE__) + "/data/model.json", "r")
        data = file.read
        file.close
        @model = InterMine::Metadata::Model.new(data)
    end

    def test_parse
        assert_equal(@model.classes.size, 19)

        dept = @model.get_cd("Department")
        assert_equal("Department", dept.name)
        assert_equal(false, dept.isInterface)
        assert_equal(6, dept.fields.size)
        assert_equal(dept.fields.keys.sort, ["company", "employees", "id", "manager", "name", "rejectedEmployee"])

        assert_equal(dept.get_field("company").referencedType, @model.get_cd("Company"))

        manager = @model.get_cd("Manager")
        assert(manager.subclass_of?(@model.get_cd("Employee")))
        assert(manager.subclass_of?("Employee"))
        assert(manager.subclass_of?("HasAddress"))
        assert(!manager.subclass_of?("Company"))
        assert(manager.subclass_of?("Company.departments.employees"))
        assert(!manager.subclass_of?("Company.name"))
        assert_raise(InterMine::Metadata::PathException) {manager.subclass_of?("Foo")}
    end

    def test_attributes
        dept = @model.get_cd("Department")
        assert_equal(2, dept.attributes.size)

        manager = @model.get_cd("Manager")
        assert_equal(7, manager.attributes.size)
    end

    def test_sugar
        dept = @model.get_cd("Department")

        table = @model.table("Department")

        assert_equal(dept, table)
    end


    def test_good_paths

        path = InterMine::Metadata::Path.new("Employee.name", @model)
        assert_equal(2, path.length)
        assert_equal("java.lang.String", path.end_type)

        path = InterMine::Metadata::Path.new("Employee.department.company.departments", @model)
        assert_equal(4, path.length)
        assert_equal("Department", path.end_type)

        path = InterMine::Metadata::Path.new("Employee.department.company.departments.employees.address.address", @model)
        assert_equal(7, path.length)
        assert_equal("java.lang.String", path.end_type)

        path = InterMine::Metadata::Path.new("Department.employees.seniority", @model, {"Department.employees" => "Manager"})
        assert_equal(3, path.length)
        assert_equal("java.lang.Integer", path.end_type)

        path = InterMine::Metadata::Path.new("Department.employees.id", @model)
        assert_equal(3, path.length)
        assert_equal("java.lang.Integer", path.end_type)

    end

    def test_bad_paths

        assert_raise(InterMine::Metadata::PathException) do
            InterMine::Metadata::Path.new("Foo.bar", @model)
        end

        assert_raise(InterMine::Metadata::PathException) do
            InterMine::Metadata::Path.new("Department.employees.foo", @model, {"Department.employees" => "Manager"})
        end

        assert_raise(InterMine::Metadata::PathException) do
            InterMine::Metadata::Path.new("Department.employees.seniority", @model, {"Department.employees" => "Foo"})
        end

        assert_raise(InterMine::Metadata::PathException) do
            InterMine::Metadata::Path.new("Employee.department.name.departments", @model)
        end

    end

    def test_item_creation_bean_style
        cd = @model.get_cd("Employee")
        emp_kls = cd.to_class

        emp = emp_kls.new
        emp.name = "John Doe"
        emp.age = 42
        emp.fullTime = false

        dep = @model.make_new("Department")
        dep.name = "Sales"

        emp.department = dep

        assert_equal(emp.name, "John Doe")
        assert_equal(emp.age, 42)
        assert_equal(emp.fullTime, false)
        assert_equal(emp.department.name, "Sales")
    end

    def test_item_creation_with_arguments
        emp_kls = @model.get_cd("Employee").to_class
        emp = emp_kls.new({
            "name" => "John Doe",
            "age" => 25,
            "fullTime" => true
        })
        assert_equal(emp.name, "John Doe")
        assert_equal(emp.age, 25)
        assert_equal(emp.fullTime, true)

        dep_kls = @model.get_cd("Department").to_class
        dep = dep_kls.new({
            "name" => "Sales"
        })

        emp.department = dep

        assert_equal(emp.department.name, "Sales")

        dep.addEmployees(emp)

        assert_equal(dep.employees.first, emp)

        emp2 = emp_kls.new({
            "name" => "Jane Doe",
            "age" => 26,
            "fullTime" => false
        })

        dep.employees = [emp, emp2]
        assert_equal(dep.employees.map { |x| x.name }, ["John Doe", "Jane Doe"])

        emp3 = emp_kls.new({
            "name" => "Jill Doe",
            "age" => 26,
            "fullTime" => false
        })
        emp4 = emp_kls.new({
            "name" => "Jonas Doe",
            "age" => 26,
            "fullTime" => false
        })

        dep.addEmployees(emp3, emp4)
        assert_equal(dep.employees.map { |x| x.name }, ["John Doe", "Jane Doe", "Jill Doe", "Jonas Doe"])
    end

    def test_item_creation_with_nested_coercion

        emp_kls = @model.get_cd("Employee").to_class

        emp5 = emp_kls.new({
            "name" => "Jonas Doe",
            "age" => 26,
            "fullTime" => false,
            "department" => { "name" => "Marketing" , "company" => { "name" => "Aperture Science", "vatNumber" => 12345}}
        })

        assert_equal(emp5.department.name, "Marketing")
        assert_equal(emp5.department.company.name, "Aperture Science")
        assert_equal(emp5.department.company.vatNumber, 12345)

        dep = @model.get_cd("Department").to_class.new({ "name" => "Sales" })
        dep.addEmployees({ "name" => "Jeremiah Doe", "age" => 42}, { "name" => "Jodie Doe" })
        assert_equal(dep.employees.map { |x| x.name }, ["Jeremiah Doe", "Jodie Doe"])

        assert_equal(dep.employees.first.age, 42)
        assert_equal(dep.employees.last.age, nil)
    end

    def test_item_creation_from_model

        manager = @model.make_new("Manager", {
            "name" => "David Brent",
            "seniority" => 42,
            "age" => 39,
            "fullTime" => true,
            "department" => { "name" => "Sales" }
        })

        assert_equal(manager.name, "David Brent")
        assert_equal(manager.seniority, 42)
        assert_equal(manager.age, 39)
        assert_equal(manager.fullTime, true)
        assert_equal(manager.department.name, "Sales")

        dep = @model.make_new("Department", {
            "name" => "Janitorial", 
            "employees" => [
                { "name" => "A" },
                { "name" => "B" }
            ]
        })

        assert_equal(dep.name, "Janitorial")
        assert_equal(dep.employees[0].name, "A")
        assert_equal(dep.employees[1].name, "B")

        dep.addEmployees({ "name" => "C" })
        assert_equal(dep.employees[2].name, "C")

    end

    def test_inheritance

        manager = @model.make_new("Manager", { "name" => "David Brent" })
        employee = @model.make_new("Employee", { "name" => "Tim Canterbury" })

        dep = @model.make_new("Department", { "name" => "Sales" })

        dep.addEmployees(manager, employee)

        assert_equal(dep.employees[0].name, "David Brent")
        assert_equal(dep.employees[1].name, "Tim Canterbury")

        mod = @model.get_cd("Employee").to_module
        dep.employees.each do |emp|
            assert_kind_of(mod, emp)
        end

        assert_kind_of(@model.get_cd("HasAddress").to_module, manager)

        comp = @model.make_new("Company")
        assert_raise ArgumentError do
            dep.addEmployees(comp)
        end
    end

    def test_creation_from_hash_alone

        emp = @model.make_new({
            "class" => "Employee",
            "name" => "John Doe",
            "age" => 25
        })

        assert_kind_of(@model.get_cd("Employee").to_module, emp)
        assert_equal(emp.name, "John Doe")
        assert_equal(emp.age, 25)
    end

    def test_prefer_hash_class

        emp = @model.make_new("Employee", {
            "class" => "Manager",
            "name" => "John Doe",
            "age" => 25
        })

        assert_kind_of(@model.get_cd("Employee").to_module, emp)
        assert_kind_of(@model.get_cd("Manager").to_module, emp)
        assert_equal(emp.name, "John Doe")
        assert_equal(emp.age, 25)
    end

    def test_subclass_coercion

        dep = @model.make_new("Department")

        dep.addEmployees(
            { "name" => "A" },
            { "class" => "Manager", "name" => "B" },
            { "class" => "CEO", "name" => "C" }
        )

        assert_equal(dep.employees[0].name, "A")
        assert_equal(dep.employees[1].name, "B")
        assert_equal(dep.employees[2].name, "C")

        dep.employees.each do |emp|
            assert_kind_of(@model.get_cd("Employee").to_module, emp)
        end

        dep.employees.slice(1, 2).each do |manager|
            assert_kind_of(@model.get_cd("Manager").to_module, manager)
        end

        assert_kind_of(@model.get_cd("Manager").to_module, dep.employees.last)
    end

    def test_overridden_isa

        manager = @model.make_new("Manager")

        assert(manager.is_a?(@model.get_cd("Employee")))
    end

    def test_refuse_to_make_objects_with_conflicting_class_names

        assert_raise ArgumentError do
            @model.make_new("Employee", { "class" => "Company", "name" => "Aperture Science" })
        end
    end

    def test_item_ids

        dep = @model.make_new("Department", {
            "name" => "Sales",
            "objectId" => 12345
        })

        assert_equal(dep.objectId, 12345)
    end

    def test_item_creation_problems

        emp_kls = @model.get_cd("Employee").to_class

        assert_raise ArgumentError do
            emp_kls.new({
                "name" => "John Doe",
                "age" => "foo",
            })
        end

        assert_raise ArgumentError do
            emp_kls.new({
                "name" => "John Doe",
                "age" => 14.75
            })
        end

        assert_raise ArgumentError do
            emp = emp_kls.new
            emp.age = 13.5
        end

        assert_raise ArgumentError do
            emp_kls.new({
                "name" => "John Doe",
                "fullTime" => "foo",
            })
        end

        assert_raise NoMethodError do
            emp_kls.new({
                "name" => "Not for this world",
                "foo" => "bar"
            })
        end

    end

    def test_path_resolution

        dep = @model.make_new("Department", {
            "name" => "Sales",
            "company" => { 
                "name" => "Werhnam-Hogg",
                "CEO" => {
                    "name" => "Jennifer",
                    "address" => {
                        "address" => "42 Some st"
                    }
                }
            },
            "employees" => [
                { "name" => "A" },
                { "name" => "B" }
            ]
        })

        # Test chained path resolution
        pathstr = "Department.company.CEO.address.address"
        obj = @model.resolve_path(dep, pathstr)
        assert_equal(obj, "42 Some st")
        assert_equal(dep._resolve(pathstr), "42 Some st")

        # Test resolving using Path objects
        path = InterMine::Metadata::Path.new(pathstr, @model)
        obj = @model.resolve_path(dep, path)
        assert_equal(obj, "42 Some st")
        assert_equal(dep._resolve(path), "42 Some st")

        # Test resolving items in collections
        pathstr_with_index = "Department.employees[1].name"
        obj = @model.resolve_path(dep, pathstr_with_index)
        assert_equal(obj, "B")
        assert_equal(dep._resolve(pathstr_with_index), "B")

        # Check bad paths
        assert_raise ArgumentError do
            @model.resolve_path(dep, "Department.company.foo")
        end

        # Check legal but irrelevant paths
        assert_raise ArgumentError do
            @model.resolve_path(dep, "Employee.department.name")
        end

        # Check non-indexed collection lookup
        assert_raise ArgumentError do
            @model.resolve_path(dep, "Department.employees.name")
        end

    end

end
