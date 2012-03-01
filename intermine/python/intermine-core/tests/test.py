import time
import unittest
import sys

from intermine.model import *
from intermine.webservice import *
from intermine.query import *
from intermine.constraints import *
from intermine.lists.list import List

from testserver import TestServer

class WebserviceTest(unittest.TestCase): # pragma: no cover
    TEST_PORT = 8000
    MAX_ATTEMPTS = 50
    maxDiff = None

    def get_test_root(self):
        return "http://localhost:" + str(WebserviceTest.TEST_PORT) + "/testservice/service"

    def do_unpredictable_test(self, test, attempts=0, error=None):
        if attempts < WebserviceTest.MAX_ATTEMPTS:
            try:
                test()
            except IOError, e:
                self.do_unpredictable_test(test, attempts + 1, e)
            except:
                e, t = sys.exc_info()[:2]
                if 104 in t: # Handle connection reset errors
                    self.do_unpredictable_test(test, attempts + 1, t)
                else:
                    raise
        else:
            raise RuntimeError("Max error count reached - last error: " + str(error))

class TestInstantiation(WebserviceTest): # pragma: no cover

    def testMakeModel(self):
        """Should be about to make a model, or fail with an appropriate message"""
        m = Model(self.get_test_root() + "/model")
        self.assertTrue(isinstance(m, Model), "Can make a model")
        try:
            bad_m = Model("foo")
            self.fail("No ModelParseError thrown at bad model xml")
        except ModelParseError, ex:
            self.assertEqual(ex.message, "Error parsing model")

    def testMakeService(self):
        """Should be able to make a Service"""
        s = Service(self.get_test_root())
        self.assertTrue(isinstance(s, Service), "Can make a service")

class TestModel(WebserviceTest):# pragma: no cover

    model = None

    def setUp(self):
        if self.model is None:
            self.__class__.model = Model(self.get_test_root() + "/model")

    def testModelClasses(self):
        '''The model should have the correct number of classes, which behave correctly'''
        self.assertEqual(len(self.model.classes.items()), 19)
        for good_class in ["Employee", "Company", "Department"]:
            cd = self.model.get_class(good_class)
            self.assertEqual(cd.name, good_class)
        ceo = self.model.get_class("Employee.department.company.CEO")
        self.assertEqual(ceo.name, "CEO")
        self.assertTrue(ceo.isa("Employee"))
        emp = self.model.get_class("Employee")
        self.assertTrue(ceo.isa(emp))

        try:
            self.model.get_class("Foo")
            self.fail("No ModelError thrown at non existent class")
        except ModelError, ex:
            self.assertEqual(ex.message,
                    "'Foo' is not a class in this model")
        try:
            self.model.get_class("Employee.name")
            self.fail("No ModelError thrown at bad class retrieval")
        except ModelError, ex:
            self.assertEqual(ex.message, "'Employee.name' is not a class")

    def testClassFields(self):
        '''The classes should have the correct fields'''
        ceo = self.model.get_class("CEO")
        for f in ["name", "age", "seniority", "address", "department"]:
            fd = ceo.get_field(f)
            self.assertEqual(fd.name, f)
            self.assertTrue(isinstance(fd, Field))

        try:
            ceo.get_field("foo")
            self.fail("No ModelError thrown at non existent field")
        except ModelError, ex:
            self.assertEqual(ex.message,
                "There is no field called foo in CEO")

    def testFieldTypes(self):
        '''The fields should be of the appropriate type'''
        dep = self.model.get_class("Department")
        self.assertTrue(isinstance(dep.get_field("name"), Attribute))
        self.assertTrue(isinstance(dep.get_field("employees"), Collection))
        self.assertTrue(isinstance(dep.get_field("company"), Reference))

class TestService(WebserviceTest): # pragma: no cover

    def setUp(self):
        self.s = Service(self.get_test_root())

    def testRoot(self):
        """The service should have the right root"""
        self.assertEqual(self.get_test_root(), self.s.root, "it has the right root")

    def testVersion(self):
        """The service should have a version"""
        from numbers import Number
        v = self.s.version
        self.assertTrue(isinstance(v, Number))

    def testRelease(self):
        """The service should have a release"""
        self.assertEqual(self.s.release, "FOO\n")

    def testQueryMaking(self):
        """The service should be able to make a query"""
        q = self.s.new_query()
        self.assertTrue(isinstance(q, Query), "Can make a query")
        self.assertEqual(q.model.name, "testmodel", "and it has the right model")

class TestQuery(WebserviceTest): # pragma: no cover

    model = None
    service = None
    expected_unary = '[<UnaryConstraint: Employee.age IS NULL>, <UnaryConstraint: Employee.name IS NOT NULL>]'
    expected_binary = '[<BinaryConstraint: Employee.age > 50000>, <BinaryConstraint: Employee.name = John>, <BinaryConstraint: Employee.end != 0>]'
    expected_multi = "[<MultiConstraint: Employee.name ONE OF ['Tom', 'Dick', 'Harry']>, <MultiConstraint: Employee.name NONE OF ['Sue', 'Jane', 'Helen']>]"
    expected_list = '[<ListConstraint: Employee IN my-list>, <ListConstraint: Employee.department.manager NOT IN my-list>]'
    expected_loop = '[<LoopConstraint: Employee IS Employee.department.manager>, <LoopConstraint: Employee.department.manager IS NOT Employee.department.company.CEO>]'
    expected_ternary = '[<TernaryConstraint: Employee LOOKUP Susan>, <TernaryConstraint: Employee.department.manager LOOKUP John IN Wernham-Hogg>]'
    expected_subclass = "[<SubClassConstraint: Department.employees ISA Manager>]"

    XML_1 = """
        <query model="testmodel" view="Employee.name Employee.age">
        </query>
        """
    EXPECTED_VIEWS_1 = ["Employee.name", "Employee.age"]

    XML_2 = """
        <query model="testmodel"
            view="Department.employees.name Department.employees.age Department.employees.seniority"
            >
            <constraint path="Department.employees" type="Manager"/>
        </query>
        """

    EXPECTED_VIEWS_2 = ["Department.employees.name",
        "Department.employees.age", "Department.employees.seniority"]

    XML_3 = """
        <query model="testmodel"
            view="Department.employees.name Department.employees.age Department.employees.seniority"
            >
            <constraint path="Department.employees" type="Manager"/>
            <constraint path="Department.name" op="=" value="foo"/>
        </query>
        """

    XML_4 = """
        <query model="testmodel"
            view="Department.employees.name Department.employees.age Department.employees.seniority"
            sortOrder="Department.employees.age asc"
            >
            <constraint path="Department.employees" type="Manager"/>
            <constraint path="Department.name" op="=" value="foo"/>
        </query>
        """

    XML_5 = """
        <query model="testmodel"
            view="Department.employees.name Department.employees.age Department.employees.seniority"
            sortOrder="Department.employees.age asc"
            >
            <pathDescription description="Should be a manager" pathString="Department.employees"/>
            <constraint path="Department.employees" type="Manager"/>
            <constraint path="Department.name" op="=" value="foo"/>
        </query>
        """

    XML_6 = """
        <query model="testmodel"
            view="Department.employees.name Department.employees.age Department.employees.salary"
            sortOrder="Department.employees.salary asc"
            >
            <pathDescription description="The Company" pathString="Department.employees.company"/>
            <pathDescription description="Should be a CEO" pathString="Department.employees"/>
            <constraint path="Department.employees" type="CEO"/>
            <constraint path="Department.employees.company" op="LOOKUP" value="foo"/>
        </query>
        """

    EXPECTED_VIEWS_6 = ["Department.employees.name",
        "Department.employees.age", "Department.employees.salary"]

    XML_7 = """
        <query model="testmodel"
            view="Department.employees.seniority Department.employees.age Department.employees.salary"
            sortOrder="Department.employees.salary asc"
            >
            <pathDescription description="The Company" pathString="Department.employees.company"/>
            <pathDescription description="Should be a CEO" pathString="Department.employees"/>
            <constraint path="Department.employees" type="CEO"/>
            <constraint path="Department.employees.company" op="LOOKUP" value="foo"/>
        </query>
        """

    EXPECTED_VIEWS_7 = ["Department.employees.seniority",
        "Department.employees.age", "Department.employees.salary"]

    CONSTRAINTS_COUNT_3 = 2

    XML_8 = '''
        <query nampe="codesNotInOrder" model="testmodel" view="Employee.name Employee.age">
            <constraint path="Employee.name" op="=" value="foo" code="X"/>
            <constraint path="Employee.name" op="=" value="bar" code="Y"/>
            <constraint path="Employee.name" op="=" value="baz" code="Z"/>
        </query>
          '''

    def setUp(self):
        if self.service is None:
            self.__class__.service = Service(self.get_test_root())
        if self.model is None:
            self.__class__.model = Model(self.get_test_root() + "/model", self.service)
        self.q = Query(self.model, self.service)
        class DummyManager:
            pass
        list_dict = {"service": None, "manager": DummyManager(), "name": "my-list", "title": None, "type": "Employee", "size": 10}
        self.l = List(**list_dict)

    def testFromXML(self):
        q1 = self.service.new_query(xml = TestQuery.XML_1)
        self.assertEqual(q1.views, TestQuery.EXPECTED_VIEWS_1)

        q2 = self.service.new_query(xml = TestQuery.XML_2)
        self.assertEqual(q2.views, TestQuery.EXPECTED_VIEWS_2)

        q3 = self.service.new_query(xml = TestQuery.XML_3)
        self.assertEqual(q3.views, TestQuery.EXPECTED_VIEWS_2)
        self.assertEqual(len(q3.constraints), TestQuery.CONSTRAINTS_COUNT_3)

        q4 = self.service.new_query(xml = TestQuery.XML_4)
        self.assertEqual(q4.views, TestQuery.EXPECTED_VIEWS_2)
        self.assertEqual(len(q4.constraints), TestQuery.CONSTRAINTS_COUNT_3)
        self.assertEqual(str(q4.get_sort_order()), "Department.employees.age asc")

        q5 = self.service.new_query(xml = TestQuery.XML_5)
        self.assertEqual(q5.views, TestQuery.EXPECTED_VIEWS_2)
        self.assertEqual(len(q5.constraints), TestQuery.CONSTRAINTS_COUNT_3)
        self.assertEqual(str(q5.get_sort_order()), "Department.employees.age asc")
        self.assertEqual(len(q5.path_descriptions), 1)

        q6 = self.service.new_query(xml = TestQuery.XML_6)
        self.assertEqual(q6.views, TestQuery.EXPECTED_VIEWS_6)
        self.assertEqual(len(q6.constraints), TestQuery.CONSTRAINTS_COUNT_3)
        self.assertEqual(str(q6.get_sort_order()), "Department.employees.salary asc")
        self.assertEqual(len(q6.path_descriptions), 2)

        q7 = self.service.new_query(xml = TestQuery.XML_7)
        self.assertEqual(q7.views, TestQuery.EXPECTED_VIEWS_7)
        self.assertEqual(len(q7.constraints), TestQuery.CONSTRAINTS_COUNT_3)
        self.assertEqual(str(q7.get_sort_order()), "Department.employees.salary asc")
        self.assertEqual(len(q7.path_descriptions), 2)

        q8 = self.service.new_query(xml = TestQuery.XML_8)
        v = None
        try:
            v = q8.get_constraint("X").value
        except:
            pass

        self.assertIsNotNone(v, "query should have a constraint with the code 'X', but it only has the codes: %s" % map(lambda x: x.code, q8.constraints))
        self.assertEqual("foo", v, "And it has the right value")

    def testAddViews(self):
        """Queries should be able to add legal views, and complain about illegal ones"""
        self.q.add_view("Employee.age")
        self.q.add_view("Employee.name", "Employee.department.company.name")
        self.q.add_view("Employee.department.name Employee.department.company.vatNumber")
        self.q.add_view("Employee.department.manager.name,Employee.department.company.CEO.name")
        self.q.add_view("Employee.department.manager.name, Employee.department.company.CEO.name")
        self.q.add_view("department.*")
        expected = [
            "Employee.age", "Employee.name", "Employee.department.company.name",
            "Employee.department.name", "Employee.department.company.vatNumber",
            "Employee.department.manager.name", "Employee.department.company.CEO.name",
            "Employee.department.manager.name", "Employee.department.company.CEO.name",
            "Employee.department.id", "Employee.department.name"]
        self.assertEqual(self.q.views, expected)
        try:
            self.q.add_view("Employee.name", "Employee.age", "Employee.department")
            self.fail("No ConstraintError thrown at non attribute view")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Employee.department' does not represent an attribute")

    def testIDOnlyExpansion(self):
        """Should be able to expand to a given depth, selecting only the id attribute for IM Object classes"""
        self.q.prefetch_depth = 3
        self.q.prefetch_id_only = True

        self.q.add_view("Employee.*")
        expected = [
            'Employee.age',
            'Employee.end',
            'Employee.fullTime',
            'Employee.id',
            'Employee.name',
            'Employee.address.id',
            'Employee.department.id',
            'Employee.department.company.id',
            'Employee.department.manager.id',
            'Employee.department.employees.id',
            'Employee.department.rejectedEmployees.id',
            'Employee.departmentThatRejectedMe.id',
            'Employee.departmentThatRejectedMe.company.id',
            'Employee.departmentThatRejectedMe.manager.id',
            'Employee.departmentThatRejectedMe.employees.id',
            'Employee.departmentThatRejectedMe.rejectedEmployees.id',
            'Employee.simpleObjects.name',
            'Employee.simpleObjects.employee.id'
        ]
        self.assertEqual(self.q.views, expected)
        exp_joins = ['Employee.address',
            'Employee.department',
            'Employee.department.company',
            'Employee.department.manager',
            'Employee.department.employees',
            'Employee.department.rejectedEmployees',
            'Employee.departmentThatRejectedMe',
            'Employee.departmentThatRejectedMe.company',
            'Employee.departmentThatRejectedMe.manager',
            'Employee.departmentThatRejectedMe.employees',
            'Employee.departmentThatRejectedMe.rejectedEmployees',
            'Employee.simpleObjects',
            'Employee.simpleObjects.employee']
        self.assertEqual([j.path for j in self.q.joins], exp_joins)

    def testDeepViews(self):
        """Should be able to expand to a given prefetch depth"""
        self.q.prefetch_depth = 3
        self.q.add_view("Employee.*")
        expected = [
           'Employee.age',
           'Employee.end',
           'Employee.fullTime',
           'Employee.id',
           'Employee.name',
           'Employee.address.address',
           'Employee.address.id',
           'Employee.department.id',
           'Employee.department.name',
           'Employee.department.company.id',
           'Employee.department.company.name',
           'Employee.department.company.vatNumber',
           'Employee.department.manager.age',
           'Employee.department.manager.end',
           'Employee.department.manager.fullTime',
           'Employee.department.manager.id',
           'Employee.department.manager.name',
           'Employee.department.manager.seniority',
           'Employee.department.manager.title',
           'Employee.department.employees.age',
           'Employee.department.employees.end',
           'Employee.department.employees.fullTime',
           'Employee.department.employees.id',
           'Employee.department.employees.name',
           'Employee.department.rejectedEmployees.age',
           'Employee.department.rejectedEmployees.end',
           'Employee.department.rejectedEmployees.fullTime',
           'Employee.department.rejectedEmployees.id',
           'Employee.department.rejectedEmployees.name',
           'Employee.departmentThatRejectedMe.id',
           'Employee.departmentThatRejectedMe.name',
           'Employee.departmentThatRejectedMe.company.id',
           'Employee.departmentThatRejectedMe.company.name',
           'Employee.departmentThatRejectedMe.company.vatNumber',
           'Employee.departmentThatRejectedMe.manager.age',
           'Employee.departmentThatRejectedMe.manager.end',
           'Employee.departmentThatRejectedMe.manager.fullTime',
           'Employee.departmentThatRejectedMe.manager.id',
           'Employee.departmentThatRejectedMe.manager.name',
           'Employee.departmentThatRejectedMe.manager.seniority',
           'Employee.departmentThatRejectedMe.manager.title',
           'Employee.departmentThatRejectedMe.employees.age',
           'Employee.departmentThatRejectedMe.employees.end',
           'Employee.departmentThatRejectedMe.employees.fullTime',
           'Employee.departmentThatRejectedMe.employees.id',
           'Employee.departmentThatRejectedMe.employees.name',
           'Employee.departmentThatRejectedMe.rejectedEmployees.age',
           'Employee.departmentThatRejectedMe.rejectedEmployees.end',
           'Employee.departmentThatRejectedMe.rejectedEmployees.fullTime',
           'Employee.departmentThatRejectedMe.rejectedEmployees.id',
           'Employee.departmentThatRejectedMe.rejectedEmployees.name',
           'Employee.simpleObjects.name',
           'Employee.simpleObjects.employee.age',
           'Employee.simpleObjects.employee.end',
           'Employee.simpleObjects.employee.fullTime',
           'Employee.simpleObjects.employee.id',
           'Employee.simpleObjects.employee.name'
           ]
        self.assertEqual(self.q.views, expected)
        exp_joins = ['Employee.address',
            'Employee.department',
            'Employee.department.company',
            'Employee.department.manager',
            'Employee.department.employees',
            'Employee.department.rejectedEmployees',
            'Employee.departmentThatRejectedMe',
            'Employee.departmentThatRejectedMe.company',
            'Employee.departmentThatRejectedMe.manager',
            'Employee.departmentThatRejectedMe.employees',
            'Employee.departmentThatRejectedMe.rejectedEmployees',
            'Employee.simpleObjects',
            'Employee.simpleObjects.employee']
        self.assertEqual([j.path for j in self.q.joins], exp_joins)

    def testViewAlias(self):
        """The aliases for add_view should work as well"""
        self.q.select("Employee.age")
        self.q.add_to_select("name")
        self.q.add_column("department.name")
        self.q.add_columns("department.manager.name")
        self.q.add_views("department.company.CEO.name")
        expected = [
            "Employee.age", "Employee.name",
            "Employee.department.name",
            "Employee.department.manager.name", "Employee.department.company.CEO.name"]
        self.assertEqual(self.q.views, expected)
        self.q.add_sort_order("name")
        self.q.add_sort_order("department.name")
        self.q.select("department.*")
        expected = ["Employee.department.id", "Employee.department.name"]
        self.assertEqual(self.q.views, expected)
        self.assertEqual(len(self.q._sort_order_list), 1)

    def testSortOrder(self):
        """Queries should be able to add sort orders, and complain appropriately"""
        self.q.add_view("Employee.name", "Employee.age", "Employee.fullTime")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.name asc")
        self.q.add_sort_order("Employee.fullTime", "desc")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.fullTime desc")
        self.q.add_sort_order("Employee.age", "asc")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.fullTime desc Employee.age asc")
        self.q.add_sort_order("Employee.end", "desc")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.fullTime desc Employee.age asc Employee.end desc")
        self.assertRaises(ModelError, self.q.add_sort_order, "Foo", "asc")
        self.assertRaises(TypeError,  self.q.add_sort_order, "Employee.name", "up")
        self.assertRaises(QueryError, self.q.add_sort_order, "Employee.department.name", "desc")

    def testUCSortOrder(self):
        """Sort-Order directions should be accepted in upper case"""
        self.q.add_view("Employee.name", "Employee.age", "Employee.fullTime")
        self.q.add_sort_order("Employee.age", "ASC")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.age asc")
        self.q.add_sort_order("Employee.fullTime", "DESC")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.age asc Employee.fullTime desc")

    def testConstraintPathProblems(self):
        """Queries should not add constraints with bad paths to themselves"""
        try:
            self.q.add_constraint('Foo', 'IS NULL')
            self.fail("No ModelError thrown at bad path name")
        except ModelError, ex:
            self.assertEqual(ex.message, "'Foo' is not a class in this model")

    def testUnaryConstraints(self):
        """Queries should be fine with NULL/NOT NULL constraints"""
        self.q.add_constraint('Employee.age', 'IS NULL')
        self.q.add_constraint('Employee.name', 'IS NOT NULL')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_unary)

    def testUnaryConstraintsSugar(self):
        """Queries should be fine with NULL/NOT NULL constraints"""
        Employee = self.q.model.table("Employee")

        self.q.add_constraint(Employee.age == None)
        self.q.add_constraint(Employee.name != None)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_unary)

    def testAddBinaryConstraints(self):
        """Queries should be able to handle constraints on attribute values"""
        self.q.add_constraint('Employee.age', '>', 50000)
        self.q.add_constraint('Employee.name', '=', 'John')
        self.q.add_constraint('Employee.end', '!=', 0)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_binary)
        try:
            self.q.add_constraint('Employee.department', '=', "foo")
            self.fail("No ConstraintError thrown for non attribute BinaryConstraint")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Employee.department' does not represent an attribute")

    def testAddBinaryConstraintsSugar(self):
        """Queries should be able to handle constraints on attribute values"""
        Employee = self.q.model.table("Employee")

        self.q.add_constraint(Employee.age > 50000)
        self.q.add_constraint(Employee.name == 'John')
        self.q.add_constraint(Employee.end != 0)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_binary)
        try:
            self.q.add_constraint(Employee.department == "foo")
            self.fail("No ConstraintError thrown for non attribute BinaryConstraint")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Employee.department' does not represent an attribute")

    def testTernaryConstraint(self):
        """Queries should be able to add constraints for LOOKUPs"""
        self.q.add_constraint('Employee', 'LOOKUP', 'Susan')
        self.q.add_constraint('Employee.department.manager', 'LOOKUP', 'John', 'Wernham-Hogg')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_ternary)
        try:
            self.q.add_constraint('Employee.department.name', 'LOOKUP', "foo")
            self.fail("No ConstraintError thrown for non object TernaryConstraint")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Employee.department.name' does not represent a class, or a reference to a class")

    def testMultiConstraint(self):
        """Queries should be ok with multi-value constraints"""
        self.q.add_constraint('Employee.name', 'ONE OF', ['Tom', 'Dick', 'Harry'])
        self.q.add_constraint('Employee.name', 'NONE OF', ['Sue', 'Jane', 'Helen'])
        self.assertEqual(self.q.constraints.__repr__(), self.expected_multi)
        self.assertRaises(TypeError, self.q.add_constraint, "Employee.name", "ONE OF", "Tom, Dick, Harry")
        self.assertRaises(ConstraintError, self.q.add_constraint, "Employee", "ONE OF", ["Tom", "Dick", "Harry"])

    def testMultiConstraintSugar(self):
        """Queries should be ok with multi-value constraints"""
        Employee = self.q.model.table("Employee")

        self.q.add_constraint(Employee.name == ['Tom', 'Dick', 'Harry'])
        self.q.add_constraint(Employee.name != ['Sue', 'Jane', 'Helen'])
        self.assertEqual(self.q.constraints.__repr__(), self.expected_multi)
        self.q.add_constraint(Employee.name == "Tom, Dick, Harry") # This method does not throw an error in this form!!
        self.assertRaises(ConstraintError, self.q.add_constraint, Employee == ["Tom", "Dick", "Harry"])

    def testListConstraint(self):
        """Queries should be ok with list constraints"""
        self.q.add_constraint('Employee', 'IN', 'my-list')
        self.q.add_constraint('Employee.department.manager', 'NOT IN', 'my-list')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_list)
        self.assertRaises(ConstraintError, self.q.add_constraint, "Employee.name", "IN", "some list")

    def testListConstraintSugar(self):
        """Queries should be ok with list constraints"""
        Employee = self.q.model.table("Employee")

        self.q.add_constraint(Employee == self.l)
        self.q.add_constraint(Employee.department.manager != self.l)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_list)
        self.assertRaises(ConstraintError, self.q.add_constraint, Employee.name == self.l)

    def testLoopConstraint(self):
        """Queries should be ok with loop constraints"""
        self.q.add_constraint('Employee', 'IS', 'Employee.department.manager')
        self.q.add_constraint('Employee.department.manager', 'IS NOT', 'Employee.department.company.CEO')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_loop)
        self.assertRaises(ConstraintError, self.q.add_constraint, "Employee", "IS", "Employee.department")

    def testLoopConstraintSugar(self):
        """Queries should be ok with loop constraints made with alchemical sugar"""
        Employee = self.q.model.table("Employee")

        self.q.add_constraint(Employee == Employee.department.manager)
        self.q.add_constraint(Employee.department.manager != Employee.department.company.CEO)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_loop)
        self.assertRaises(ConstraintError, self.q.add_constraint, Employee == Employee.department)

    def testSubclassConstraints(self):
        """Queries should be ok with sub class constraints"""
        self.q.add_constraint('Department.employees', 'Manager')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_subclass)
        try:
           self.q.add_constraint('Department.company.CEO', 'Foo')
           self.fail("No ModelError raised by bad sub class")
        except ModelError, ex:
            self.assertEqual(ex.message, "'Foo' is not a class in this model")
        try:
            self.q.add_constraint('Department.company.CEO', 'Manager')
            self.fail("No ConstraintError raised by bad subclass relationship")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Manager' is not a subclass of 'Department.company.CEO'")

    def testStringLogic(self):
        """Queries should be able to parse good logic strings"""

        a = self.q.add_constraint("Employee.name", "IS NOT NULL")
        b = self.q.add_constraint("Employee.age", ">", 10)
        c = self.q.add_constraint("Employee.department", "LOOKUP", "Sales", "Wernham-Hogg")
        d = self.q.add_constraint("Employee.department.employees.name", "ONE OF",
            ["John", "Paul", "Mary"])
        self.q.add_constraint("Employee.department.employees", "Manager")

        self.assertEqual(str(self.q.get_logic()), "A and B and C and D")
        self.q.set_logic("(B or C) and (A or D)")
        self.assertEqual(str(self.q.get_logic()), "(B or C) and (A or D)")
        self.q.set_logic("B and C or A and D")
        self.assertEqual(str(self.q.get_logic()), "B and (C or A) and D")
        self.q.set_logic("(A and B) or (A and C and D)")
        self.assertEqual(str(self.q.get_logic()), "(A and B) or (A and C and D)")

    def testIrrelevantCodeStripping(self):
        """Should be able to recover from queries that have irrelevant codes in their logic"""

        a = self.q.add_constraint("Employee.name", "IS NOT NULL")
        b = self.q.add_constraint("Employee.age", ">", 10)
        c = self.q.add_constraint("Employee.department", "LOOKUP", "Sales", "Wernham-Hogg")
        d = self.q.add_constraint("Employee.department.employees.name", "ONE OF",
            ["John", "Paul", "Mary"])
        self.q.add_constraint("Employee.department.employees", "Manager")

        self.q._set_questionable_logic("A and B or C or D and E")
        self.assertEqual(str(self.q.get_logic()), "A and (B or C or D)")
        self.q._set_questionable_logic("E and A and B or C or D")
        self.assertEqual(str(self.q.get_logic()), "A and (B or C or D)")
        self.q._set_questionable_logic("A and B and E or C or D")
        self.assertEqual(str(self.q.get_logic()), "A and B and (C or D)")
        self.q._set_questionable_logic("A and B or X and J or Z and E or C or D")
        self.assertEqual(str(self.q.get_logic()), "A and B and (C or D)")
        self.q._set_questionable_logic("A and B or X and (J or Z and E or C) or D")
        self.assertEqual(str(self.q.get_logic()), "(A and B and C) or D")
        self.q._set_questionable_logic("A or (B or X and J or Z and E or C) or D")
        self.assertEqual(str(self.q.get_logic()), "A or (B and C) or D")

    def testObjectLogic(self):
        """Queries should be able to set logic from object methods"""

        a = self.q.add_constraint("Employee.name", "IS NOT NULL")
        b = self.q.add_constraint("Employee.age", ">", 10)
        c = self.q.add_constraint("Employee.department", "LOOKUP", "Sales", "Wernham-Hogg")
        d = self.q.add_constraint("Employee.department.employees.name", "ONE OF",
            ["John", "Paul", "Mary"])
        self.q.add_constraint("Employee.department.employees", "Manager")

        self.q.set_logic(a + b + c + d)
        self.assertEqual(str(self.q.get_logic()), "A and B and C and D")
        self.q.set_logic(a & b & c & d)
        self.assertEqual(str(self.q.get_logic()), "A and B and C and D")
        self.q.set_logic(a | b | c | d)
        self.assertEqual(str(self.q.get_logic()), "A or B or C or D")
        self.q.set_logic(a + b & c | d)
        self.assertEqual(str(self.q.get_logic()), "(A and B and C) or D")

        self.assertEqual(repr(self.q.get_logic()), '<LogicGroup: (A and B and C) or D>')

        self.assertRaises(ConstraintError, self.q.set_logic, "E and C or A and D")
        self.assertRaises(QueryError,      self.q.set_logic, "A and B and C")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and B and C not D")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and ((B and C and D)")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and ((B and C) and D))")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and B( and C and D)")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and (B and C and )D")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and (B and C) D")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and (B and C) (D and E)")
        self.assertRaises(TypeError, lambda: self.q.get_logic() + 1)
        self.assertRaises(TypeError, lambda: self.q.get_logic() & 1)
        self.assertRaises(TypeError, lambda: self.q.get_logic() | 1)
        self.assertRaises(TypeError, lambda: LogicGroup(a, "bar", b))

    def testJoins(self):
        """Queries should be able to add joins"""
        self.assertRaises(TypeError,       self.q.add_join, 'Employee.department', 'foo')
        self.assertRaises(QueryError, self.q.add_join, 'Employee.age', 'inner')
        self.assertRaises(ModelError,      self.q.add_join, 'Employee.foo', 'inner')
        self.q.add_join('Employee.department', 'inner')
        self.q.add_join('Employee.department.company', 'outer')
        expected = "[<Join: Employee.department INNER>, <Join: Employee.department.company OUTER>]"
        self.assertEqual(expected, self.q.joins.__repr__())

    def testXML(self):
        """Queries should be able to serialise themselves to XML"""
        self.q.add_view("Employee.name", "Employee.age", "Employee.department.name")
        self.q.add_constraint("Employee.name", "IS NOT NULL")
        self.q.add_constraint("Employee.age", ">", 10)
        self.q.add_constraint("Employee.department", "LOOKUP", "Sales", "Wernham-Hogg")
        self.q.add_constraint("Employee.department.employees.name", "ONE OF",
            ["John", "Paul", "Mary"])
        self.q.add_constraint("Employee.department.manager", "IS", "Employee")
        self.q.add_constraint("Employee", "IN", "some list of employees")
        self.q.add_constraint("Employee.department.employees", "Manager")
        self.q.add_join("Employee.department", "outer")
        self.q.add_sort_order("Employee.age")
        self.q.set_logic("(A and B) or (A and C and D) and (E or F)")
        expected ='<query constraintLogic="((A and B) or (A and C and D)) and (E or F)" longDescription="" model="testmodel" name="" sortOrder="Employee.age asc" view="Employee.name Employee.age Employee.department.name"><join path="Employee.department" style="OUTER"/><constraint code="A" op="IS NOT NULL" path="Employee.name"/><constraint code="B" op="&gt;" path="Employee.age" value="10"/><constraint code="C" extraValue="Wernham-Hogg" op="LOOKUP" path="Employee.department" value="Sales"/><constraint code="D" op="ONE OF" path="Employee.department.employees.name"><value>John</value><value>Paul</value><value>Mary</value></constraint><constraint code="E" loopPath="Employee" op="=" path="Employee.department.manager"/><constraint code="F" op="IN" path="Employee" value="some list of employees"/><constraint path="Employee.department.employees" type="Manager"/></query>'
        self.assertEqual(expected, self.q.to_xml())
        self.assertEqual(expected, self.q.clone().to_xml()) # Clones must produce identical XML

    def testSugaryQueryConstruction(self):
        """Test use of operation coercion which is similar to SQLAlchemy"""
        model = self.q.model

        Employee = model.table("Employee")
        Manager = model.table("Manager")

        expected = '<query constraintLogic="((A and B) or (A and C and D)) and (E or F)" longDescription="" model="testmodel" name="" sortOrder="Employee.age asc" view="Employee.name Employee.age Employee.department.name"><join path="Employee.department" style="OUTER"/><constraint code="A" op="IS NOT NULL" path="Employee.name"/><constraint code="B" op="&gt;" path="Employee.age" value="10"/><constraint code="C" extraValue="Wernham-Hogg" op="LOOKUP" path="Employee.department" value="Sales"/><constraint code="D" op="ONE OF" path="Employee.department.employees.name"><value>John</value><value>Paul</value><value>Mary</value></constraint><constraint code="E" loopPath="Employee" op="=" path="Employee.department.manager"/><constraint code="F" op="IN" path="Employee" value="my-list"/><constraint path="Employee.department.employees" type="Manager"/></query>'

        # SQL style
        q = Employee.\
                select("name", "age", "department.name").\
                where(Employee.name != None).\
                where(Employee.age > 10).\
                where(Employee.department % ("Sales", "Wernham-Hogg")).\
                where(Employee.department.employees.name == ["John", "Paul", "Mary"]).\
                where(Employee.department.manager == Employee).\
                where(Employee == self.l).\
                where(Employee.department.employees >> Manager).\
                outerjoin(Employee.department).\
                order_by(Employee.age).\
                set_logic("(A and B) or (A and C and D) and (E or F)")

        self.assertEqual(expected, q.to_xml())

        # SQLAlchemy style
        q = self.service.query(Employee).\
                select("name", "age", "department.name").\
                filter(Employee.name != None).\
                filter(Employee.age > 10).\
                filter(Employee.department % ("Sales", "Wernham-Hogg")).\
                filter(Employee.department.employees.name == ["John", "Paul", "Mary"]).\
                filter(Employee.department.manager == Employee).\
                filter(Employee == self.l).\
                filter(Employee.department.employees >> Manager).\
                outerjoin(Employee.department).\
                order_by(Employee.age).\
                set_logic("(A and B) or (A and C and D) and (E or F)")

        self.assertEqual(expected, q.to_xml())

    def testKWCons(self):
        """Test use of constraints provided in kwargs"""

        model = self.q.model

        expected = '<query longDescription="" model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name"><constraint code="A" op="=" path="Employee.age" value="10"/></query>'

        q = model.Employee.select("name").where(age = 10)

        self.assertEqual(expected, q.to_xml())

    def testLogicConstraintTrees(self):
        # Actually SQL-Alchemy-esque
        expected = """
            <query constraintLogic="(A and B) or (C and D)" longDescription="" model="testmodel" name=""
                sortOrder="Employee.age asc" view="Employee.age Employee.end Employee.fullTime
                Employee.id Employee.name Employee.department.name">
                <join path="Employee.department" style="OUTER"/>
                <constraint code="A" op="IS NOT NULL" path="Employee.name"/>
                <constraint code="B" op="&gt;" path="Employee.age" value="10"/>
                <constraint code="C" op="IN" path="Employee" value="my-list"/>
                <constraint code="D" op="LOOKUP" path="Employee.department.manager" value="David"/>
                <constraint path="Employee.department.manager" type="CEO"/>
            </query>
            """
        e = self.service.model.Employee
        CEO = self.service.model.CEO
        q = self.service.query(e, e.department.name).\
                filter(
                        e.department.manager < CEO,
                        (
                            ((e.name != None) & (e.age > 10))
                            | (e.in_(self.l) & (e.department.manager % "David"))
                        )
                    ).\
                outerjoin(e.department).\
                order_by(e.age)

        expected = re.sub(r'\s+', ' ', expected)
        expected = re.sub(r'>\s+<', '><', expected)
        expected = expected.strip()
        self.assertEqual(expected, q.to_xml())

class TestTemplate(TestQuery): # pragma: no cover

    expected_unary = '[<TemplateUnaryConstraint: Employee.age IS NULL (editable, locked)>, <TemplateUnaryConstraint: Employee.name IS NOT NULL (editable, locked)>]'
    expected_binary = '[<TemplateBinaryConstraint: Employee.age > 50000 (editable, locked)>, <TemplateBinaryConstraint: Employee.name = John (editable, locked)>, <TemplateBinaryConstraint: Employee.end != 0 (editable, locked)>]'
    expected_multi = "[<TemplateMultiConstraint: Employee.name ONE OF ['Tom', 'Dick', 'Harry'] (editable, locked)>, <TemplateMultiConstraint: Employee.name NONE OF ['Sue', 'Jane', 'Helen'] (editable, locked)>]"
    expected_ternary = '[<TemplateTernaryConstraint: Employee LOOKUP Susan (editable, locked)>, <TemplateTernaryConstraint: Employee.department.manager LOOKUP John IN Wernham-Hogg (editable, locked)>]'
    expected_subclass = '[<TemplateSubClassConstraint: Department.employees ISA Manager (editable, locked)>]'
    expected_list = '[<TemplateListConstraint: Employee IN my-list (editable, locked)>, <TemplateListConstraint: Employee.department.manager NOT IN my-list (editable, locked)>]'
    expected_loop = '[<TemplateLoopConstraint: Employee IS Employee.department.manager (editable, locked)>, <TemplateLoopConstraint: Employee.department.manager IS NOT Employee.department.company.CEO (editable, locked)>]'

    def setUp(self):
        super(TestTemplate, self).setUp()
        self.q = Template(self.model)

class TestQueryResults(WebserviceTest): # pragma: no cover

    model = None
    service = None

    class MockService(object):

        QUERY_PATH = '/QUERY-PATH'
        TEMPLATEQUERY_PATH = '/TEMPLATE-PATH'
        root = 'ROOT'
        prefetch_depth = 1
        prefetch_id_only = False

        def get_results(self, *args):
            return args

    def setUp(self):
        if self.service is None:
            self.__class__.service = Service(self.get_test_root())
        if self.model is None:
            self.__class__.model = Model(self.get_test_root() + "/model")

        q = Query(self.model, self.service)
        q.add_view("Employee.name", "Employee.age", "Employee.id")
        self.query = q
        t = Template(self.model, self.service)
        t.add_view("Employee.name", "Employee.age", "Employee.id")
        t.add_constraint("Employee.name", '=', "Fred")
        t.add_constraint("Employee.age", ">", 25)
        self.template = t

    def testURLs(self):
        """Should be able to produce the right information for opening urls"""
        q = Query(self.model, self.MockService())
        q.add_view("Employee.name", "Employee.age", "Employee.id")
        q.add_constraint("Employee.name", '=', "Fred")
        q.add_constraint("Employee.age", ">", 25)

        t = Template(self.model, self.MockService())
        t.name = "TEST-TEMPLATE"
        t.add_view("Employee.name", "Employee.age", "Employee.id")
        t.add_constraint("Employee.name", '=', "Fred")
        t.add_constraint("Employee.age", ">", 25)

        expectedQ = (
            '/QUERY-PATH',
            {
                'query': '<query constraintLogic="A and B" longDescription="" model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name Employee.age Employee.id"><constraint code="A" op="=" path="Employee.name" value="Fred"/><constraint code="B" op="&gt;" path="Employee.age" value="25"/></query>',
                'start': 0
            },
            'object',
            ['Employee.name', 'Employee.age', 'Employee.id'],
            self.model.get_class("Employee")
        )
        self.assertEqual(expectedQ, q.results())
        self.assertEqual(list(expectedQ), q.get_results_list())

        expectedQ = (
            '/QUERY-PATH',
            {
                'query': '<query constraintLogic="A and B" longDescription="" model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name Employee.age Employee.id"><constraint code="A" op="=" path="Employee.name" value="Fred"/><constraint code="B" op="&gt;" path="Employee.age" value="25"/></query>',
                'start': 0
            },
            'rr',
            ['Employee.name', 'Employee.age', 'Employee.id'],
            self.model.get_class("Employee")
        )
        self.assertEqual(expectedQ, q.rows())
        self.assertEqual(list(expectedQ), q.get_row_list())

        expectedQ = (
            '/QUERY-PATH',
            {
                'query': '<query constraintLogic="A and B" longDescription="" model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name Employee.age Employee.id"><constraint code="A" op="=" path="Employee.name" value="Fred"/><constraint code="B" op="&gt;" path="Employee.age" value="25"/></query>',
                'start': 10,
                'size': 200
            },
            'object',
            ['Employee.name', 'Employee.age', 'Employee.id'],
            self.model.get_class("Employee")
        )
        self.assertEqual(expectedQ, q.results(start=10, size=200))
        self.assertEqual(list(expectedQ), q.get_results_list(start=10, size=200))

        expected1 = (
            '/TEMPLATE-PATH',
            {
             'name': 'TEST-TEMPLATE',
             'code1': 'A',
             'code2': 'B',
             'constraint1': 'Employee.name',
             'constraint2': 'Employee.age',
             'op1': '=',
             'op2': '>',
             'value1': 'Fred',
             'value2': '25',
             'start': 0
            },
           'object',
           ['Employee.name', 'Employee.age', 'Employee.id'],
           self.model.get_class("Employee")
           )
        self.assertEqual(expected1, t.results())
        self.assertEqual(list(expected1), t.get_results_list())

        expected1 = (
            '/TEMPLATE-PATH',
            {
             'name': 'TEST-TEMPLATE',
             'code1': 'A',
             'code2': 'B',
             'constraint1': 'Employee.name',
             'constraint2': 'Employee.age',
             'op1': '=',
             'op2': '>',
             'value1': 'Fred',
             'value2': '25',
             'start': 0
            },
           'rr',
           ['Employee.name', 'Employee.age', 'Employee.id'],
           self.model.get_class("Employee")
           )
        self.assertEqual(expected1, t.rows())
        self.assertEqual(list(expected1), t.get_row_list())

        expected2 = (
            '/TEMPLATE-PATH',
            {
             'name': 'TEST-TEMPLATE',
             'code1': 'A',
             'code2': 'B',
             'constraint1': 'Employee.name',
             'constraint2': 'Employee.age',
             'op1': '<',
             'op2': '>',
             'value1': 'Tom',
             'value2': '55',
             'start': 0
            },
           'object',
           ['Employee.name', 'Employee.age', 'Employee.id'],
           self.model.get_class("Employee")
           )
        self.assertEqual(expected2, t.results(
            A = {"op": "<", "value": "Tom"},
            B = {"value": 55}
        ))

        expected2 = (
            '/TEMPLATE-PATH',
            {
             'name': 'TEST-TEMPLATE',
             'code1': 'A',
             'code2': 'B',
             'constraint1': 'Employee.name',
             'constraint2': 'Employee.age',
             'op1': '<',
             'op2': '>',
             'value1': 'Tom',
             'value2': '55',
             'start': 10,
             'size': 200
            },
           'object',
           ['Employee.name', 'Employee.age', 'Employee.id'],
           self.model.get_class("Employee")
           )
        self.assertEqual(expected2, t.results(
            start = 10,
            size = 200,
            A = {"op": "<", "value": "Tom"},
            B = {"value": 55}
        ))
        self.assertEqual(list(expected2), t.get_results_list(
            start = 10,
            size = 200,
            A = {"op": "<", "value": "Tom"},
            B = {"value": 55}
        ))

        # Check that we can just use strings for simple value replacement.
        expected3 = (
            '/TEMPLATE-PATH',
            {
             'name': 'TEST-TEMPLATE',
             'code1': 'A',
             'code2': 'B',
             'constraint1': 'Employee.name',
             'constraint2': 'Employee.age',
             'op1': '=',
             'op2': '>',
             'value1': 'Foo',
             'value2': 'Bar',
             'start': 10,
             'size': 200
            },
           'object',
           ['Employee.name', 'Employee.age', 'Employee.id'],
           self.model.get_class("Employee")
           )
        self.assertEqual(list(expected3), t.get_results_list(
            start = 10,
            size  = 200,
            A = "Foo",
            B = "Bar"
            ))
        # Check that these contraint values have not been applied to the actual template
        self.assertEqual(expected1, t.rows())
        expected1 = (
            '/TEMPLATE-PATH',
            {
             'name': 'TEST-TEMPLATE',
             'code1': 'A',
             'code2': 'B',
             'constraint1': 'Employee.name',
             'constraint2': 'Employee.age',
             'op1': '=',
             'op2': '>',
             'value1': 'Fred',
             'value2': '25',
             'start': 0
            },
           'object',
           ['Employee.name', 'Employee.age', 'Employee.id'],
           self.model.get_class("Employee")
           )

        self.assertEqual(expected1, t.results())

    def testResultsList(self):
        """Should be able to get results as one list per row"""
        def logic():
            expected = [['foo', 'bar', 'baz'], [123, 1.23, -1.23], [True, False, None]]
            self.assertEqual(self.query.get_results_list("list"), expected)
            self.assertEqual(self.template.get_results_list("list"), expected)

        self.do_unpredictable_test(logic)

    def testResultRows(self):
        """Should be able to get results as result rows"""
        def logic():
            assertEqual = self.assertEqual
            q_res = self.query.all("rr")
            t_res = self.template.all("rr")
            for results in [q_res, t_res]:
                assertEqual(results[0]["age"], 'bar') # Can index by short path
                assertEqual(results[1]["Employee.age"], 1.23) # or by full path
                assertEqual(results[2][0], True) # or by numerical index
                assertEqual(len(results), 3)
                for row in results:
                    assertEqual(len(row), 3)
        self.do_unpredictable_test(logic)

    def testResultRowIterability(self):
        """Result rows should iterate as lists"""
        def logic():
            q_res = self.query.all("rr")
            t_res = self.template.all("rr")
            for results in [q_res, t_res]:
                # 'in' as test of iterability
                self.assertTrue("bar" in results[0])
                self.assertTrue(1.23 in results[1])
                self.assertTrue(True in results[2])
                # Should be able to actually iterate
                count = 0
                for val in results[0]:
                    count += 1
                    self.assertTrue(0 < count < 4)

        self.do_unpredictable_test(logic)

    def testResultRowDictBehaviour(self):
        """Result rows should allow iteration using items() and iterkeys()"""
        def logic():
            q_res = self.query.all("rr")
            t_res = self.template.all("rr")
            for results in [q_res, t_res]:
                r = results[0]
                count = 0
                for (k, v) in r.items():
                    count += 1
                    self.assertTrue(0 < count < 4)
                count = 0
                self.assertEqual([("Employee.name", 'foo'), ("Employee.age", 'bar'), ("Employee.id", 'baz')], r.items())
                for (k, v) in r.iteritems():
                    count += 1
                    self.assertTrue(0 < count < 4)
                self.assertEqual([pair for pair in r.iteritems()], r.items())
                self.assertEqual(r.keys(), self.query.views)
                self.assertEqual(r.values(), r.to_l())
                self.assertEqual(zip(r.values(), r.keys()), zip(r.itervalues(), r.iterkeys()))
                self.assertTrue(r.has_key("age"))
                self.assertTrue(r.has_key("Employee.age"))
                self.assertTrue(not r.has_key("Employee.foo"))

        self.do_unpredictable_test(logic)

    def testResultsDict(self):
        """Should be able to get results as one dictionary per row"""
        expected = [
            {'Employee.age': u'bar', 'Employee.id': u'baz', 'Employee.name': u'foo'},
            {'Employee.age': 1.23, 'Employee.id': -1.23, 'Employee.name': 123},
            {'Employee.age': False, 'Employee.id': None, 'Employee.name': True}
        ]
        def logic():
            self.assertEqual(self.query.get_results_list("dict"), expected)
            self.assertEqual(self.template.get_results_list("dict"), expected)

        self.do_unpredictable_test(logic)

class MinimalResultsTest(TestQueryResults):

    PATH = "/testservice/legacyjsonrows"

class TestTSVResults(WebserviceTest): # pragma: no cover

    model = None
    service = None
    PATH = "/testservice/tsvservice"
    FORMAT = "tsv"
    EXPECTED_RESULTS = ['foo\tbar\tbaz', '123\t1.23\t-1.23']

    def get_test_root(self):
        return "http://localhost:" + str(self.TEST_PORT) + self.PATH

    def setUp(self):
        if self.service is None:
            self.__class__.service = Service(self.get_test_root())
        if self.model is None:
            self.__class__.model = Model(self.get_test_root() + "/service/model")

        q = Query(self.model, self.service)
        q.add_view("Employee.name", "Employee.age", "Employee.id")
        self.query = q
        t = Template(self.model, self.service)
        t.add_view("Employee.name", "Employee.age", "Employee.id")
        t.add_constraint("Employee.name", '=', "Fred")
        t.add_constraint("Employee.age", ">", 25)
        self.template = t

    def testResults(self):
        """Should be able to get results as one string per row"""
        def logic():
            self.assertEqual(self.query.get_results_list(self.FORMAT), self.EXPECTED_RESULTS)
            self.assertEqual(self.template.get_results_list(self.FORMAT), self.EXPECTED_RESULTS)
        self.do_unpredictable_test(logic)


class TestCSVResults(TestTSVResults): # pragma: no cover

    PATH = "/testservice/csvservice"
    FORMAT = "csv"
    EXPECTED_RESULTS = ['"foo","bar","baz"', '"123","1.23","-1.23"']

class TestResultObjects(WebserviceTest): # pragma: no cover
    model = None
    service = None

    def get_test_root(self):
        return "http://localhost:" + str(self.TEST_PORT) + "/testservice/testresultobjs"

    def setUp(self):
        if self.service is None:
            self.__class__.service = Service(self.get_test_root())
        if self.model is None:
            self.__class__.model = self.service.model

        q = Query(self.model, self.service)
        q.add_view("Department.name", "Department.employees.name", "Department.employees.age", "Department.company.vatNumber")
        self.query = q
        t = Template(self.model, self.service)
        q.add_view("Department.name", "Department.employees.name", "Department.employees.age", "Department.company.vatNumber")
        t.add_constraint("Department.manager.name", '=', "Fred")
        self.template = t

    def testResultObjs(self):
        """Should be able to get results as result objects"""
        def logic():
            assertEqual = self.assertEqual
            q_res = self.query.all("jsonobjects")
            t_res = self.template.all("jsonobjects")
            for departments in [q_res, t_res]:
                assertEqual(departments[0].name, 'Sales')
                assertEqual(departments[0].company.vatNumber, 665261)
                assertEqual(departments[0].employees[2].name, "Tim Canterbury")
                assertEqual(departments[0].employees[3].age, 58)
                assertEqual(len(departments[0].employees), 6)

                assertEqual(departments[-1].name, 'Slashes')
                assertEqual(departments[-1].company.vatNumber, 764575)
                assertEqual(departments[-1].employees[2].name, "Double forward Slash //")
                assertEqual(departments[-1].employees[2].age, 62)
                assertEqual(len(departments[-1].employees), 5)

                for idx in [0, -1]:
                    self.assertRaises(ModelError, lambda: departments[idx].foo) # Model errors are thrown for illegal field access
                    self.assertRaises(ModelError, lambda: departments[idx].company.foo)

                assertEqual(len(departments), 8)

        self.do_unpredictable_test(logic)

class TestCountResults(TestTSVResults): # pragma: no cover

    PATH = "/testservice/countservice"
    FORMAT = "count"
    EXPECTED_RESULTS = ['25']
    EXPECTED_COUNT = 25

    def testCount(self):
        """Should be able to get count as an integer"""
        def logic():
            self.assertEqual(self.query.count(), self.EXPECTED_COUNT)
            self.assertEqual(self.template.count(), self.EXPECTED_COUNT)
        self.do_unpredictable_test(logic)

if __name__ == '__main__': # pragma: no cover
    server = TestServer()
    server.start()
    time.sleep(0.1) # Avoid race conditions with the server
    unittest.main()
    server.shutdown()
