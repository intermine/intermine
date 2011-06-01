import unittest
from test import WebserviceTest

from intermine.webservice import *

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
