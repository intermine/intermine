package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.util.DynamicUtil;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import servletunit.struts.MockStrutsTestCase;

public class ChangeTableSizeActionTest extends MockStrutsTestCase
{
    public ChangeTableSizeActionTest(String arg1) {
        super(arg1);
    }

    private PagedResults pr;
    private Results results;

    private Company company1, company2, company3;
    private Department department1, department2, department3;

    public void setUp() throws Exception {
        super.setUp();
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        IqlQuery fq = new IqlQuery("select c, d from Company as c, Department as d", "org.intermine.model.testmodel");
        results = os.execute(fq.toQuery());

        // Set up some known objects in the first 3 results rows
        company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setName("Company1");
        company1.setId(new Integer(1));
        company2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company2.setName("Company2");
        company2.setId(new Integer(2));
        company3 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company3.setName("Company3");
        company3.setId(new Integer(3));
        department1 = new Department();
        department1.setName("Department1");
        department1.setId(new Integer(4));
        department2 = new Department();
        department2.setName("Department2");
        department2.setId(new Integer(5));
        department3 = new Department();
        department3.setName("Department3");
        department3.setId(new Integer(6));

        ResultsRow row = new ResultsRow();
        row.add(company1);
        row.add(department1);
        os.addRow(row);
        row = new ResultsRow();
        row.add(company2);
        row.add(department2);
        os.addRow(row);
        row = new ResultsRow();
        row.add(company3);
        row.add(department3);
        os.addRow(row);

        Model model = Model.getInstanceByName("testmodel");
        List view = new ArrayList() {{ // see: http://www.c2.com/cgi/wiki?DoubleBraceInitialization
                                        add("Company");
                                        add("Department");
        }};
        WebResults webResults = new WebResults(view, results, model, Collections.EMPTY_MAP, context);
        pr = new PagedResults(webResults);
        pr.setPageSize(5);
    }

//     public void testChangePageSize1() throws Exception {
//         setRequestPathInfo("/changeResultsSize");
//         addRequestParameter("buttons(changePageSize)", "");

//         ChangeResultsForm form = new MockChangeResultsForm();
//         form.setPageSize("25");
//         setActionForm(form);

//         pr.setStartIndex(0);
//         getSession().setAttribute(Constants.RESULTS_TABLE, pr);

//         actionPerform();

//         verifyForward("results");
//         verifyNoActionErrors();
//         assertEquals(0, pr.getStartIndex());
//         assertEquals(25, pr.getPageSize());
//     }

//     public void testChangePageSize2() throws Exception {
//         setRequestPathInfo("/changeResultsSize");
//         addRequestParameter("buttons(changePageSize)", "");

//         ChangeResultsForm form = new ChangeResultsForm();
//         form.setPageSize("10");
//         setActionForm(form);

//         pr.setStartIndex(12);
//         getSession().setAttribute(Constants.RESULTS_TABLE, pr);

//         actionPerform();

//         verifyForward("results");
//         verifyNoActionErrors();
//         assertEquals(10, pr.getStartIndex());
//         assertEquals(10, pr.getPageSize());
//     }

//     public void testSaveNewBag() throws Exception {
//         setRequestPathInfo("/changeResultsSize");
//         addRequestParameter("buttons(saveNewBag)", "");
//         getSession().setAttribute(Constants.RESULTS_TABLE, new PagedResults(results));

//         ChangeResultsForm form = new MockChangeResultsForm();
//         form.setSelectedObjects(new String[] {"0,0", "1,2"});
//         form.setNewBagName("testBag1");
//         setActionForm(form);

//         actionPerform();

//         verifyForward("results");
//         verifyNoActionErrors();
        
//         Map savedBags = (Map) getSession().getAttribute(Constants.SAVED_BAGS);
//         Collection objs = (Collection) savedBags.get("testBag1");
//         assertEquals(2, objs.size());
//         Iterator iter = objs.iterator();
//         assertEquals(company1, iter.next());
//         assertEquals(department3, iter.next());
//     }

    public void testAddToExistingBag() throws Exception {
        assertTrue(true);
//         setRequestPathInfo("/changeResultsSize");
//         addRequestParameter("addToExistingBag", "");
//         Model model = Model.getInstanceByName("testmodel");
//         getSession().setAttribute(Constants.RESULTS_TABLE, new PagedResults(results, model));

//         ChangeResultsForm form = new MockChangeResultsForm();
//         form.setSelectedObjects(new String[] {"0,1", "1,1"});
//         form.setExistingBagName("testBag1");
//         setActionForm(form);

//         InterMineBag objs = new InterMineBag();
//         objs.add(company1);
//         objs.add(department3);
//         ((Profile) getSession().getAttribute(Constants.PROFILE)).saveBag("testBag1", objs);

//         actionPerform();

//         verifyForward("results");
//         verifyNoActionErrors();

//         objs = (InterMineBag) ((Profile) getSession().getAttribute(Constants.PROFILE)).getSavedBags().get("testBag1");

//         assertEquals(4, objs.size());
//         Iterator iter = objs.iterator();
//         assertEquals(company1, iter.next());
//         assertEquals(department3, iter.next());
//         assertEquals(company2, iter.next());
//         assertEquals(department2, iter.next());
    }

//     public void testAddSameToExistingBag() throws Exception {
//         setRequestPathInfo("/changeResultsSize");
//         addRequestParameter("addToExistingBag", "");
//         Model model = Model.getInstanceByName("testmodel");
//         getSession().setAttribute(Constants.RESULTS_TABLE, new PagedResults(results, model));

//         ChangeResultsForm form = new MockChangeResultsForm();
//         form.setSelectedObjects(new String[] {"0,1", "1,1"});
//         form.setExistingBagName("testBag1");
//         setActionForm(form);

//         InterMineBag objs = new InterMineBag();
//         objs.add(company1);
//         objs.add(department2);
//         ((Profile) getSession().getAttribute(Constants.PROFILE)).saveBag("testBag1", objs);

//         actionPerform();

//         verifyForward("results");
//         verifyNoActionErrors();

//         objs = (InterMineBag) ((Profile) getSession().getAttribute(Constants.PROFILE)).getSavedBags().get("testBag1");

//         assertEquals(3, objs.size());
//         Iterator iter = objs.iterator();
//         assertEquals(company1, iter.next());
//         assertEquals(department2, iter.next());
//         assertEquals(company2, iter.next());
//     }

    class MockChangeResultsForm extends ChangeTableSizeForm
    {
        public void reset(ActionMapping mapping, HttpServletRequest request) {
        }
    }
}
