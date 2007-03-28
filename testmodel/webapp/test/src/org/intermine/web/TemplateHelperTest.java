package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.struts.action.ActionErrors;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constraint;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateQueryBinding;
import org.intermine.web.struts.TemplateForm;

public class TemplateHelperTest extends TestCase
{
    private Map templates, classKeys;

    public void setUp() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Properties classKeyProps = new Properties();
        classKeyProps.load(getClass().getClassLoader()
                           .getResourceAsStream("class_keys.properties"));
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new InputStreamReader(TemplateHelper.class.getClassLoader().getResourceAsStream("WEB-INF/classes/default-template-queries.xml"));
        templates = binding.unmarshal(reader, new HashMap(), classKeys);
    }
    
    public void testPrecomputeQuery() throws Exception {
        Iterator i = templates.keySet().iterator();
        TemplateQuery t = (TemplateQuery) templates.get("employeeByName");
        String expIql =
            "SELECT DISTINCT a1_, a1_.name AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name, a1_.age";
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee Employee.name\"><node path=\"Employee\" type=\"Employee\"></node></query>";
        Map pathToQueryNode = new HashMap();
        MainHelper.makeQuery(PathQuery.fromXml(queryXml, new HashMap(), classKeys),
                             new HashMap(), pathToQueryNode);
        List indexes = new ArrayList();
        String precomputeQuery = TemplateHelper.getPrecomputeQuery(t, indexes).toString();
        assertEquals(expIql, precomputeQuery);
        assertTrue(indexes.size() == 2);
        System.out.println("pathToQueryNode: " + pathToQueryNode);
        List expIndexes = Arrays.asList(new Object[] {pathToQueryNode.get("Employee"), pathToQueryNode.get("Employee.name")});
        assertEquals(expIndexes.toString(), indexes.toString());
    }
    
    public void testTemplateFormToTemplateQuerySimple() throws Exception {
        // Set EmployeeName != "EmployeeA1"
        TemplateQuery template = (TemplateQuery) templates.get("employeeByName");

        TemplateForm tf = new TemplateForm();
        tf.setAttributeOps("1", "" + ConstraintOp.NOT_EQUALS.getIndex());
        tf.setAttributeValues("1", "EmployeeA1");
        tf.parseAttributeValues(template, null, new ActionErrors(), false);

        TemplateQuery expected = (TemplateQuery) template.clone();
        PathNode tmpNode = (PathNode) expected.getEditableNodes().get(0);
        PathNode node = (PathNode) expected.getNodes().get(tmpNode.getPath());
        Constraint c = node.getConstraint(0);
        node.getConstraints().set(0, new Constraint(ConstraintOp.NOT_EQUALS,
                "EmployeeA1", true, c.getDescription(), c.getCode(), c.getIdentifier()));
        expected.setEdited(true);
        
        TemplateQuery actual = TemplateHelper.templateFormToTemplateQuery(tf, template, new HashMap());
        assertEquals(expected.toXml(), actual.toXml());
    }
    
    public void testTemplateFormToTemplateQueryIdBag() throws Exception {
        TemplateQuery template = (TemplateQuery) templates.get("employeeByName");

        TemplateForm tf = new TemplateForm();
        tf.setUseBagConstraint("1", true);
        tf.setBagOp("1", "" + ConstraintOp.IN.getIndex());
        tf.setBag("1", "bag1");
        tf.parseAttributeValues(template, null, new ActionErrors(), false);
        InterMineBag bag1 = new InterMineBag(new Integer(101), "bag1", "Employee", 1, null, null);
        Map savedBags = new HashMap();
        savedBags.put("bag1", bag1);
        
        TemplateQuery expected = (TemplateQuery) template.clone();
        PathNode tmpNode = (PathNode) expected.getEditableNodes().get(0);
        PathNode node = (PathNode) expected.getNodes().get(tmpNode.getPath());
        PathNode parent = (PathNode) expected.getNodes().get(node.getParent().getPath());
        Constraint c = node.getConstraint(0);
        Constraint cc = new Constraint(ConstraintOp.IN,
                "bag1", true, c.getDescription(), c.getCode(), c.getIdentifier());
        expected.getNodes().remove(node.getPath());
        parent.getConstraints().add(cc);
        expected.setEdited(true);
        TemplateQuery actual = TemplateHelper.templateFormToTemplateQuery(tf, template, savedBags);
        assertEquals(expected.toXml(), actual.toXml());
    }

    public void testGetPrecomputeQuery() throws Exception {
        TemplateQuery t = (TemplateQuery) templates.get("employeesFromCompanyAndDepartment");
        assertEquals("SELECT DISTINCT a1_, a3_, a2_, a3_.name AS a4_, a2_.name AS a5_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_) ORDER BY a1_.name, a1_.age, a3_.name, a2_.name", TemplateHelper.getPrecomputeQuery(t, new ArrayList()).toString());
    }

    public void testBugWhereTrue() throws Exception {
        TemplateQueryBinding binding = new TemplateQueryBinding();
        /*Reader reader = new StringReader("<template name=\"InterProDomain_ProteinInteractingProtein\" title=\"Protein Domain --&gt; Proteins + Interacting proteins.\" longDescription=\"For proteins with a particular domain, search for proteins they have been shown to interact with. Display the confidence of the interaction and the PubMed id of the paper describing the experiment.\" comment=\"\" important=\"false\">"
                + "<query name=\"InterProDomain_ProteinInteractingProtein\" model=\"genomic\" view=\"Protein.proteinFeatures.interproId Protein.proteinFeatures.name Protein.identifier Protein.primaryAccession Protein.interactionRoles.interaction.interactors.protein.identifier Protein.interactionRoles.interaction.interactors.protein.primaryAccession Protein.interactionRoles.interaction.evidence.confidence Protein.interactionRoles.interaction.evidence.confidenceDesc Protein.interactionRoles.interaction.evidence.analysis.publication.pubMedId\" constraintLogic=\"A and B and C and D\">"
                + "<node path=\"Protein\" type=\"Protein\"></node>"
                + "<node path=\"Protein.interactionRoles\" type=\"ProteinInteractor\"></node>"
                + "<node path=\"Protein.interactionRoles.interaction\" type=\"ProteinInteraction\"></node>"
                + "<node path=\"Protein.interactionRoles.interaction.interactors\" type=\"ProteinInteractor\"></node>"
                + "<node path=\"Protein.interactionRoles.interaction.interactors.protein\" type=\"Protein\">"
                + "<constraint op=\"!=\" value=\"Protein\" description=\"\" identifier=\"\" code=\"A\"></constraint></node>"
                + "<node path=\"Protein.interactionRoles.interaction.evidence\" type=\"ExperimentalResult\"></node>"
                + "<node path=\"Protein.organism\" type=\"Organism\"></node>"
                + "<node path=\"Protein.organism.shortName\" type=\"String\">"
                + "<constraint op=\"=\" value=\"D. melanogaster\" description=\"Search for proteins from organism:\" identifier=\"\" editable=\"true\" code=\"B\"></constraint></node>"
                + "<node path=\"Protein.proteinFeatures\" type=\"ProteinFeature\"></node>"
                + "<node path=\"Protein.proteinFeatures.evidence\" type=\"Evidence\"></node>"
                + "<node path=\"Protein.proteinFeatures.identifier\" type=\"String\">"
                + "<constraint op=\"LIKE\" value=\"IPR%\" description=\"\" identifier=\"\" code=\"C\"></constraint></node>"
                + "<node path=\"Protein.proteinFeatures.name\" type=\"String\">"
                + "<constraint op=\"LIKE\" value=\"%leucine%\" description=\"Constrain the name of the domain, use '*' for wildcards:\" identifier=\"\" editable=\"true\" code=\"D\"></constraint></node>"
                + "</query></template>");*/
        Reader reader = new StringReader("<template name=\"flibble\" title=\"flobble\" longDescription=\"wurble\" comment=\"wibble\" important=\"false\">"
                + "<query name=\"flibble\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"A and B and C and D\">"
                + "<node path=\"Employee\" type=\"Employee\"></node>"
                + "<node path=\"Employee.age\" type=\"Integer\">"
                + "    <constraint op=\"!=\" value=\"10\" description=\"a\" identifier=\"\" code=\"A\"></constraint>"
                + "    <constraint op=\"!=\" value=\"20\" description=\"b\" identifier=\"\" code=\"B\" editable=\"true\"></constraint>"
                + "    <constraint op=\"!=\" value=\"30\" description=\"c\" identifier=\"\" code=\"C\"></constraint>"
                + "    <constraint op=\"!=\" value=\"40\" description=\"d\" identifier=\"\" code=\"D\" editable=\"true\"></constraint>"
                + "</node></query></template>");
        TemplateQuery t = 
            (TemplateQuery) binding.unmarshal(reader, new HashMap(), classKeys).values().iterator().next();
        TemplateQuery tc = t.cloneWithoutEditableConstraints();
        System.out.println(t.getConstraintLogic() + " -> " + tc.getConstraintLogic());
        assertEquals("SELECT DISTINCT a1_, a1_.age AS a2_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age != 10 AND a1_.age != 30) ORDER BY a1_.name, a1_.age", TemplateHelper.getPrecomputeQuery(t, new ArrayList()).toString());
    }
}
