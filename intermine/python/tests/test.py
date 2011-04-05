import time
import unittest

from intermine.model import *
from intermine.webservice import *
from intermine.query import *
from intermine.constraints import *

from testserver import TestServer

class WebserviceTest(unittest.TestCase):
    TEST_PORT = 8000

    def get_test_root(self):
        return "http://localhost:" + str(WebserviceTest.TEST_PORT) + "/testservice/service"

class TestInstantiation(WebserviceTest): 

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

class TestModel(WebserviceTest):

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

class TestService(WebserviceTest):
     
    def setUp(self):
        self.s = Service(self.get_test_root())

    def testRoot(self):
        """The service should have the right root"""
        self.assertEqual(self.get_test_root(), self.s.root, "it has the right root")

    def testQueryMaking(self):
        """The service should be able to make a query"""
        q = self.s.new_query()
        self.assertTrue(isinstance(q, Query), "Can make a query")
        self.assertEqual(q.model.name, "testmodel", "and it has the right model")

class TestQuery(WebserviceTest):

    model = None
    expected_unary = '[<UnaryConstraint: Employee.age IS NULL>, <UnaryConstraint: Employee.name IS NOT NULL>]'
    expected_binary = '[<BinaryConstraint: Employee.age > 50000>, <BinaryConstraint: Employee.name = John>, <BinaryConstraint: Employee.end != 0>]'
    expected_multi = "[<MultiConstraint: Employee.name ONE OF ['Tom', 'Dick', 'Harry']>, <MultiConstraint: Manager.name NONE OF ['Sue', 'Jane', 'Helen']>]"
    expected_list = "[<ListConstraint: Employee IN my-list>, <ListConstraint: Manager NOT IN my-list>]"
    expected_loop = '[<LoopConstraint: Employee IS Employee.department.manager>, <LoopConstraint: CEO IS NOT CEO.company.departments.employees>]' 
    expected_ternary = '[<TernaryConstraint: Employee LOOKUP Susan>, <TernaryConstraint: Employee.department.manager LOOKUP John IN Wernham-Hogg>]'
    expected_subclass = "[<SubClassConstraint: Department.employees ISA Manager>]"

    def setUp(self):
        if self.model is None:
            self.__class__.model = Model(self.get_test_root() + "/model")
        self.q = Query(self.model)

    def testAddViews(self):
        """Queries should be able to add legal views, and complain about illegal ones"""
        self.q.add_view("Employee.age")
        self.q.add_view("Employee.name", "Employee.department.company.name")
        self.q.add_view("Employee.department.name Employee.department.company.vatNumber")
        self.q.add_view("Employee.department.manager.name,Employee.department.company.CEO.name")
        self.q.add_view("Employee.department.manager.name, Employee.department.company.CEO.name")
        expected = [
            "Employee.age", "Employee.name", "Employee.department.company.name", 
            "Employee.department.name", "Employee.department.company.vatNumber",
            "Employee.department.manager.name", "Employee.department.company.CEO.name", 
            "Employee.department.manager.name", "Employee.department.company.CEO.name"]
        self.assertEqual(self.q.views, expected)
        try: 
            self.q.add_view("Employee.name", "Employee.age", "Employee.department")
            self.fail("No ConstraintError thrown at non attribute view")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Employee.department' does not represent an attribute")

    def testSortOrder(self):
        """Queries should be able to add sort orders, and complain appropriately"""
        self.q.add_view("Employee.name", "Employee.age", "Employee.fullTime")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.name asc")
        self.q.add_sort_order("Employee.fullTime", "desc")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.fullTime desc")
        self.q.add_sort_order("Employee.age", "asc")
        self.assertEqual(str(self.q.get_sort_order()), "Employee.fullTime desc,Employee.age asc")
        self.assertRaises(ModelError, self.q.add_sort_order, "Foo", "asc")
        self.assertRaises(TypeError,  self.q.add_sort_order, "Employee.name", "up")
        self.assertRaises(QueryError, self.q.add_sort_order, "Employee.id", "desc")


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

    def testAddBinaryConstraints(self):
        """Queries should be able to handle constraints on attribute values"""
        self.q.add_constraint('Employee.age', '>', 50000)
        self.q.add_constraint('Employee.name', '=', 'John')
        self.q.add_constraint('Employee.end', '!=', 0)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_binary)
        try:
            self.q.add_constraint('Department.company', '=', "foo")
            self.fail("No ConstraintError thrown for non attribute BinaryConstraint")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Department.company' does not represent an attribute")

    def testTernaryConstraint(self):
        """Queries should be able to add constraints for LOOKUPs"""
        self.q.add_constraint('Employee', 'LOOKUP', 'Susan')
        self.q.add_constraint('Employee.department.manager', 'LOOKUP', 'John', 'Wernham-Hogg')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_ternary)
        try:
            self.q.add_constraint('Department.company.name', 'LOOKUP', "foo")
            self.fail("No ConstraintError thrown for non object TernaryConstraint")
        except ConstraintError, ex:
            self.assertEqual(ex.message, "'Department.company.name' does not represent a class, or a reference to a class")

    def testMultiConstraint(self):
        """Queries should be ok with multi-value constraints"""
        self.q.add_constraint('Employee.name', 'ONE OF', ['Tom', 'Dick', 'Harry'])
        self.q.add_constraint('Manager.name', 'NONE OF', ['Sue', 'Jane', 'Helen'])
        self.assertEqual(self.q.constraints.__repr__(), self.expected_multi)
        self.assertRaises(TypeError, self.q.add_constraint, "Manager.name", "ONE OF", "Tom, Dick, Harry")
        self.assertRaises(ConstraintError, self.q.add_constraint, "Manager", "ONE OF", ["Tom", "Dick", "Harry"])

    def testListConstraint(self):
        """Queries should be ok with list constraints"""
        self.q.add_constraint('Employee', 'IN', 'my-list')
        self.q.add_constraint('Manager', 'NOT IN', 'my-list')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_list)
        self.assertRaises(ConstraintError, self.q.add_constraint, "Employee.name", "IN", "some list")

    def testLoopConstraint(self):
        """Queries should be ok with loop constraints"""
        self.q.add_constraint('Employee', 'IS', 'Employee.department.manager')
        self.q.add_constraint('CEO', 'IS NOT', 'CEO.company.departments.employees')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_loop)
        self.assertRaises(ConstraintError, self.q.add_constraint, "Employee", "IS", "Employee.department")
        self.assertRaises(ConstraintError, self.q.add_constraint, "Company", "IS", "Company.CEO")

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

    def testLogic(self):
        """Queries should be able to parse good logic strings"""
        self.q.add_constraint("Employee.name", "IS NOT NULL")
        self.q.add_constraint("Employee.age", ">", 10)
        self.q.add_constraint("Employee.department", "LOOKUP", "Sales", "Wernham-Hogg")
        self.q.add_constraint("Employee.department.employees.name", "ONE OF", 
            ["John", "Paul", "Mary"])
        self.q.add_constraint("Employee.department.employees", "Manager")
        self.assertEqual(str(self.q.get_logic()), "A and B and C and D")
        self.q.set_logic("(B or C) and (A or D)")
        self.assertEqual(str(self.q.get_logic()), "(B or C) and (A or D)")
        self.q.set_logic("B and C or A and D")
        self.assertEqual(str(self.q.get_logic()), "B and (C or A) and D")
        self.q.set_logic("(A and B) or (A and C and D)")
        self.assertEqual(str(self.q.get_logic()), "(A and B) or (A and C and D)")
        self.assertRaises(ConstraintError, self.q.set_logic, "E and C or A and D")
        self.assertRaises(QueryError,      self.q.set_logic, "A and B and C")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and B and C not D")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and ((B and C and D)")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and ((B and C) and D))")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and B( and C and D)")
        self.assertRaises(LogicParseError, self.q.set_logic, "A and (B and C and )D")

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

class TestTemplate(TestQuery):
    
    expected_unary = '[<TemplateUnaryConstraint: Employee.age IS NULL (editable, locked)>, <TemplateUnaryConstraint: Employee.name IS NOT NULL (editable, locked)>]'
    expected_binary = '[<TemplateBinaryConstraint: Employee.age > 50000 (editable, locked)>, <TemplateBinaryConstraint: Employee.name = John (editable, locked)>, <TemplateBinaryConstraint: Employee.end != 0 (editable, locked)>]'
    expected_multi = "[<TemplateMultiConstraint: Employee.name ONE OF ['Tom', 'Dick', 'Harry'] (editable, locked)>, <TemplateMultiConstraint: Manager.name NONE OF ['Sue', 'Jane', 'Helen'] (editable, locked)>]"
    expected_ternary = '[<TemplateTernaryConstraint: Employee LOOKUP Susan (editable, locked)>, <TemplateTernaryConstraint: Employee.department.manager LOOKUP John IN Wernham-Hogg (editable, locked)>]'
    expected_subclass = '[<TemplateSubClassConstraint: Department.employees ISA Manager (editable, locked)>]'
    expected_list = "[<TemplateListConstraint: Employee IN my-list (editable, locked)>, <TemplateListConstraint: Manager NOT IN my-list (editable, locked)>]"
    expected_loop = '[<TemplateLoopConstraint: Employee IS Employee.department.manager (editable, locked)>, <TemplateLoopConstraint: CEO IS NOT CEO.company.departments.employees (editable, locked)>]'

    def setUp(self):
        super(TestTemplate, self).setUp()
        self.q = Template(self.model)

class TestQueryResults(WebserviceTest):

    model = None
    service = None

    class MockService(object):
        
        QUERY_PATH = '/QUERY-PATH'
        TEMPLATEQUERY_PATH = '/TEMPLATE-PATH'
        root = 'ROOT'

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
                'query': '<query constraintLogic="A and B" longDescription="" model="testmodel" name="" sortOrder="Employee.name asc" view="Employee.name Employee.age Employee.id"><constraint code="A" op="=" path="Employee.name" value="Fred"/><constraint code="B" op="&gt;" path="Employee.age" value="25"/></query>'
            }, 
            'list', 
            ['Employee.name', 'Employee.age', 'Employee.id']
        )
        self.assertEqual(expectedQ, q.results())

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
             'value2': '25'
            }, 
           'list', 
           ['Employee.name', 'Employee.age', 'Employee.id'])
        self.assertEqual(expected1, t.results())

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
             'value2': '55'
            }, 
           'list', 
           ['Employee.name', 'Employee.age', 'Employee.id'])
        self.assertEqual(expected2, t.results(
            A = {"op": "<", "value": "Tom"},
            B = {"value": 55} 
        ))

        self.assertEqual(expected1, t.results()) 

    def testResultsList(self):
        """Should be able to get results as one list per row"""
        expected = [['foo', 'bar', 'baz'], [123, 1.23, -1.23], [True, False, None]] 
        attempts = 0
        def do_tests(error=None):
            if attempts < 5:
                try:
                    self.assertEqual(self.query.get_results_list(), expected)
                    self.assertEqual(self.template.get_results_list(), expected)
                except IOError, e:
                    do_tests(e)
            else:
                raise RuntimeError("Error connecting to " + self.query.service.root, error)

        do_tests()

    def testResultsDict(self):
        """Should be able to get results as one dictionary per row"""
        expected = [
            {'Employee.age': u'bar', 'Employee.id': u'baz', 'Employee.name': u'foo'}, 
            {'Employee.age': 1.23, 'Employee.id': -1.23, 'Employee.name': 123}, 
            {'Employee.age': False, 'Employee.id': None, 'Employee.name': True}
        ] 
        attempts = 0
        def do_tests(error=None):
            if attempts < 5:
                try:
                    self.assertEqual(self.query.get_results_list("dict"), expected)
                    self.assertEqual(self.template.get_results_list("dict"), expected)
                except IOError, e:
                    do_tests(e)
            else:
                raise RuntimeError("Error connecting to " + self.query.service.root, error)

        do_tests()

class TestTSVResults(WebserviceTest):

    model = None
    service = None
    PATH = "/testservice/tsvservice"
    FORMAT = "tsv"
    EXPECTED_RESULTS = ['foo\tbar\tbaz\n', '123\t1.23\t-1.23\n']

    def get_test_root(self):
        return "http://localhost:" + str(self.TEST_PORT) + self.PATH

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

    def testResults(self):
        """Should be able to get results as one string per row"""
        attempts = 0
        def do_tests(error=None):
            if attempts < 5:
                try:
                    self.assertEqual(self.query.get_results_list(self.FORMAT), self.EXPECTED_RESULTS)
                    self.assertEqual(self.template.get_results_list(self.FORMAT), self.EXPECTED_RESULTS)
                except IOError, e:
                    do_tests(e)
            else:
                raise RuntimeError("Error connecting to " + self.query.service.root, error)

        do_tests()

class TestCSVResults(TestTSVResults):

    PATH = "/testservice/csvservice"
    FORMAT = "csv"
    EXPECTED_RESULTS = ['"foo","bar","baz"\n', '"123","1.23","-1.23"\n']

class TestTemplates(WebserviceTest):

    def setUp(self):
        self.service = Service(self.get_test_root())

    def testGetTemplate(self):
        """Should be able to get a template from the webservice, if it exists"""
        self.assertEqual(len(self.service.templates), 12)
        t = self.service.get_template("MultiValueConstraints")
        self.assertTrue(isinstance(t, Template))
        expected = "[<TemplateMultiConstraint: Employee.name ONE OF [u'Dick', u'Jane', u'Timmy, the Loyal German-Shepherd'] (editable, locked)>]"
        self.assertEqual(t.editable_constraints.__repr__(), expected)
        expected = [[u'foo', u'bar', u'baz'], [123, 1.23, -1.23], [True, False, None]] 
        attempts = 0
        def do_tests(error=None):
            if attempts < 5:
                try:
                    self.assertEqual(t.get_results_list(), expected)
                except IOError, e:
                    do_tests(e)
            else:
                raise RuntimeError("Error connecting to " + self.query.service.root, error)

        do_tests()
        try:
            self.service.get_template("Non_Existant")
            self.fail("No ServiceError raised by non-existant template")
        except ServiceError, ex:
            self.assertEqual(ex.message, "There is no template called 'Non_Existant' at this service")
    
    def testTemplateConstraintParsing(self):
        """Should be able to parse template constraints"""
        t = self.service.get_template("UneditableConstraints")
        self.assertEqual(len(t.constraints), 2)
        self.assertEqual(len(t.editable_constraints), 1)
        expected = '[<TemplateBinaryConstraint: Company.name = Woolies (editable, locked)>]'
        self.assertEqual(expected, t.editable_constraints.__repr__())

        t2 = self.service.get_template("SwitchableConstraints")
        self.assertEqual(len(t2.editable_constraints), 3)
        con = t2.get_constraint("A")
        self.assertTrue(con.editable and con.required and con.switched_on)
        con = t2.get_constraint("B")
        self.assertTrue(con.editable and con.optional and con.switched_on)
        con = t2.get_constraint("C")
        self.assertTrue(con.editable and con.optional and con.switched_off)
         

if __name__ == '__main__':
    server = TestServer()
    server.start()
    time.sleep(0.1) # Avoid race conditions with the server
    unittest.main()
    server.shutdown()
