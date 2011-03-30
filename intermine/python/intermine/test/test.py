import threading
import SimpleHTTPServer
import time
import unittest

from ..model import *
from ..webservice import *
from ..query import *
from ..constraints import *

class ServerThread( threading.Thread ):
    def __init__(self):
        super(ServerThread, self).__init__()
        self.daemon = True
    def run(self):
        SimpleHTTPServer.test()

class TestInstantiation(unittest.TestCase): 

    def testMakeModel(self):
        m = Model("http://localhost:8000/intermine/testservice/service/model")
        self.assertTrue(isinstance(m, Model), "Can make a model")
        try:
            bad_m = Model("foo")
        except ModelParseError as ex:
            self.assertEqual(ex.message, "Error parsing model")

    def testMakeService(self):
        s = Service("http://localhost:8000/intermine/testservice/service")
        self.assertTrue(isinstance(s, Service), "Can make a service")

class TestModel(unittest.TestCase):

    model = None

    def setUp(self):
        if self.model is None: 
            self.model = Model("http://localhost:8000/intermine/testservice/service/model")

    def testModelClasses(self):
        '''The model should have the correct number of classes'''
        self.assertEqual(len(self.model.classes.items()), 19)
        for good_class in ["Employee", "Company", "Department"]:
            cd = self.model.get_class(good_class)
            self.assertEqual(cd.name, good_class)
        dep = self.model.get_class("Employee.department.company.CEO")
        self.assertEqual(dep.name, "CEO") 
        self.assertTrue(dep.isa("Employee"))
        emp = self.model.get_class("Employee")
        self.assertTrue(dep.isa(emp))

        try:
            self.model.get_class("Foo")
        except ModelError as ex:
            self.assertEqual(ex.message, 
                    "'Foo' is not a class in this model")
        try: 
            self.model.get_class("Employee.name")
        except ModelError as ex:
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
        except ModelError as ex:
            self.assertEqual(ex.message, 
                "There is no field called foo in CEO")

    def testFieldTypes(self):
        dep = self.model.get_class("Department")
        self.assertTrue(isinstance(dep.get_field("name"), Attribute))
        self.assertTrue(isinstance(dep.get_field("employees"), Collection))
        self.assertTrue(isinstance(dep.get_field("company"), Reference))

class TestService(unittest.TestCase):

    ROOT = "http://localhost:8000/intermine/testservice/service"
     
    def setUp(self):
        self.s = Service(TestService.ROOT)

    def testRoot(self):
        self.assertEqual(TestService.ROOT, self.s.root, "it has the right root")

    def testQueryMaking(self):
        q = self.s.new_query()
        self.assertTrue(isinstance(q, Query), "Can make a query")
        self.assertEqual(q.model.name, "testmodel", "and it has the right model")

class TestQuery(unittest.TestCase):

    model = None
    expected_unary = '[<UnaryConstraint: Employee.age IS NULL>, <UnaryConstraint: Employee.name IS NOT NULL>]'
    expected_binary = '[<BinaryConstraint: Employee.age > 50000>, <BinaryConstraint: Employee.name = John>, <BinaryConstraint: Employee.end != 0>]'
    expected_multi = "[<MultiConstraint: Employee.name ONE OF ['Tom', 'Dick', 'Harry']>, <MultiConstraint: Manager.name NONE OF ['Sue', 'Jane', 'Helen']>]"
    expected_ternary = '[<TernaryConstraint: Employee LOOKUP Susan>, <TernaryConstraint: Employee.department.manager LOOKUP John IN Wernham-Hogg>]'
    expected_subclass = "[<SubClassConstraint: Department.employees ISA Manager>]"

    def setUp(self):
        if self.model is None:
            self.__class__.model = Model("http://localhost:8000/intermine/testservice/service/model") 
        self.q = Query(self.model)

    def testAddViews(self):
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
        except ConstraintError as ex:
            self.assertEqual(ex.message, "'Employee.department' does not represent an attribute")

    def testSortOrder(self):
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
        try:
            self.q.add_constraint('Foo', 'IS NULL')
        except ModelError as ex:
            self.assertEqual(ex.message, "'Foo' is not a class in this model")

    def testUnaryConstraints(self):
        self.q.add_constraint('Employee.age', 'IS NULL')
        self.q.add_constraint('Employee.name', 'IS NOT NULL')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_unary)

    def testAddBinaryConstraints(self):
        self.q.add_constraint('Employee.age', '>', 50000)
        self.q.add_constraint('Employee.name', '=', 'John')
        self.q.add_constraint('Employee.end', '!=', 0)
        self.assertEqual(self.q.constraints.__repr__(), self.expected_binary)
        try:
            self.q.add_constraint('Department.company', '=', "foo")
        except ConstraintError as ex:
            self.assertEqual(ex.message, "'Department.company' does not represent an attribute")

    def testTernaryConstraint(self):
        self.q.add_constraint('Employee', 'LOOKUP', 'Susan')
        self.q.add_constraint('Employee.department.manager', 'LOOKUP', 'John', 'Wernham-Hogg')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_ternary)
        try:
            self.q.add_constraint('Department.company.name', 'LOOKUP', "foo")
        except ConstraintError as ex:
            self.assertEqual(ex.message, "'Department.company.name' does not represent a class, or a reference to a class")

    def testMultiConstraint(self):
        self.q.add_constraint('Employee.name', 'ONE OF', ['Tom', 'Dick', 'Harry'])
        self.q.add_constraint('Manager.name', 'NONE OF', ['Sue', 'Jane', 'Helen'])
        self.assertEqual(self.q.constraints.__repr__(), self.expected_multi)

    def testSubclassConstraints(self):
        self.q.add_constraint('Department.employees', 'Manager')
        self.assertEqual(self.q.constraints.__repr__(), self.expected_subclass)
        try:
           self.q.add_constraint('Department.company.CEO', 'Foo')
        except ModelError as ex:
            self.assertEqual(ex.message, "'Foo' is not a class in this model")
        try:
            self.q.add_constraint('Department.company.CEO', 'Manager')
        except ConstraintError as ex:
            self.assertEqual(ex.message, "'Manager' is not a subclass of 'Department.company.CEO'")

    def testLogic(self):
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
        self.assertRaises(TypeError,       self.q.add_join, 'Employee.department', 'foo')
        self.assertRaises(QueryError, self.q.add_join, 'Employee.age', 'inner')
        self.assertRaises(ModelError,      self.q.add_join, 'Employee.foo', 'inner')
        self.q.add_join('Employee.department', 'inner')
        self.q.add_join('Employee.department.company', 'outer')
        expected = "[<Join: Employee.department INNER>, <Join: Employee.department.company OUTER>]"
        self.assertEqual(expected, self.q.joins.__repr__())

    def testXML(self):
        self.q.add_view("Employee.name", "Employee.age", "Employee.department.name")
        self.q.add_constraint("Employee.name", "IS NOT NULL")
        self.q.add_constraint("Employee.age", ">", 10)
        self.q.add_constraint("Employee.department", "LOOKUP", "Sales", "Wernham-Hogg")
        self.q.add_constraint("Employee.department.employees.name", "ONE OF", 
            ["John", "Paul", "Mary"])
        self.q.add_constraint("Employee.department.employees", "Manager")
        self.q.add_join("Employee.department", "outer")
        self.q.add_sort_order("Employee.age")
        self.q.set_logic("(A and B) or (A and C and D)")
        expected = '<query constraintLogic="(A and B) or (A and C and D)" longDescription="" model="testmodel" name="" sortOrder="Employee.age asc" view="Employee.name Employee.age Employee.department.name"><join path="Employee.department" style="OUTER"/><constraint code="A" op="IS NOT NULL" path="Employee.name"/><constraint code="B" op="&gt;" path="Employee.age" value="10"/><constraint code="C" extraValue="Wernham-Hogg" op="LOOKUP" path="Employee.department" value="Sales"/><constraint code="D" op="ONE OF" path="Employee.department.employees.name"><value>John</value><value>Paul</value><value>Mary</value></constraint><constraint path="Employee.department.employees" type="Manager"/></query>'
        self.assertEqual(expected, self.q.to_xml())

class TestTemplate(TestQuery):
    
    expected_unary = '[<TemplateUnaryConstraint: Employee.age IS NULL (editable, locked)>, <TemplateUnaryConstraint: Employee.name IS NOT NULL (editable, locked)>]'
    expected_binary = '[<TemplateBinaryConstraint: Employee.age > 50000 (editable, locked)>, <TemplateBinaryConstraint: Employee.name = John (editable, locked)>, <TemplateBinaryConstraint: Employee.end != 0 (editable, locked)>]'
    expected_multi = "[<TemplateMultiConstraint: Employee.name ONE OF ['Tom', 'Dick', 'Harry'] (editable, locked)>, <TemplateMultiConstraint: Manager.name NONE OF ['Sue', 'Jane', 'Helen'] (editable, locked)>]"
    expected_ternary = '[<TemplateTernaryConstraint: Employee LOOKUP Susan (editable, locked)>, <TemplateTernaryConstraint: Employee.department.manager LOOKUP John IN Wernham-Hogg (editable, locked)>]'
    expected_subclass = '[<TemplateSubClassConstraint: Department.employees ISA Manager (editable, locked)>]'

    def setUp(self):
        super(TestTemplate, self).setUp()
        self.q = Template(self.model)

class TestQueryResults(unittest.TestCase):

    model = None
    service = Service("http://localhost:8000/intermine/testservice/service")

    class MockService(object):
        
        QUERY_PATH = '/QUERY-PATH'
        TEMPLATEQUERY_PATH = '/TEMPLATE-PATH'
        root = 'ROOT'

        def get_results(self, *args):
            return args
    
    def setUp(self):
        if self.model is None:
            self.__class__.model = Model("http://localhost:8000/intermine/testservice/service/model") 
        q = Query(self.model, self.service)
        q.add_view("Employee.name", "Employee.age", "Employee.id")
        self.query = q
        t = Template(self.model, self.service)
        t.add_view("Employee.name", "Employee.age", "Employee.id")
        t.add_constraint("Employee.name", '=', "Fred")
        t.add_constraint("Employee.age", ">", 25)
        self.template = t

    def testURLs(self):
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
        expected = [['foo', 'bar', 'baz'],['quux','fizz','fop']]
        self.assertEqual(self.query.get_results_list(), expected)
        self.assertEqual(self.template.get_results_list(), expected)

    def testResultsDict(self):
        expected = [
            {'Employee.name':'foo', 'Employee.age':'bar', 'Employee.id':'baz'},
            {'Employee.name':'quux', 'Employee.age':'fizz', 'Employee.id':'fop'}
            ]
        self.assertEqual(self.query.get_results_list("dict"), expected)
        self.assertEqual(self.template.get_results_list("dict"), expected)

    def testResultsString(self):
        expected = [
            '"foo","bar","baz"\n',
            '"quux","fizz","fop"\n'
            ]
        self.assertEqual(self.query.get_results_list("string"), expected)
        self.assertEqual(self.template.get_results_list("string"), expected)

class TestTemplates(unittest.TestCase):

    def setUp(self):
        self.service = Service("http://localhost:8000/intermine/testservice/service")

    def testGetTemplate(self):
        self.assertEqual(len(self.service.templates), 12)
        t = self.service.get_template("MultiValueConstraints")
        self.assertTrue(isinstance(t, Template))
        expected = "[<TemplateMultiConstraint: Employee.name ONE OF [u'Dick', u'Jane', u'Timmy, the Loyal German-Shepherd'] (editable, locked)>]"
        self.assertEqual(t.editable_constraints.__repr__(), expected)
        expected = [['foo', 'bar', 'baz'],['quux','fizz','fop']]
        self.assertEqual(t.get_results_list(), expected)
        try:
            self.service.get_template("Non_Existant")
        except ServiceError as ex:
            self.assertEqual(ex.message, "There is no template called 'Non_Existant' at this service")
    
    def testTemplateConstraintParsing(self):
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
    server = ServerThread()
    server.start()
    time.sleep(0.1) # Avoid race conditions with the server
    unittest.main()
