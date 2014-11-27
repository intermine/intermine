package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.CompanyShadow;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.MockServletContext;


/**
 * A TestCase that sets up a webconfig for use in TestCases that extend this class.  The
 * setUp() method creates a new webconfig from  WebConfigTest.xml.
 * @author dbutano
 *
 */
public class WidgetConfigTestCase extends InterMineAPITestCase {
    protected WebConfig webConfig;

    public WidgetConfigTestCase() {
        super(null);
    }

    public void setUp() throws Exception {
        super.setUp();
        MockServletContext context = new MockServletContext();
        final Properties p = new Properties();
        p.setProperty("web.config.classname.mappings", "CLASS_NAME_MAPPINGS");
        p.setProperty("web.config.fieldname.mappings", "FIELD_NAME_MAPPINGS");
        SessionMethods.setWebProperties(context, p);

        final InputStream is = getClass().getClassLoader().getResourceAsStream("WebConfigTest.xml");
        final InputStream classesIS = getClass().getClassLoader().getResourceAsStream("testClassMappings.properties");
        final InputStream fieldsIS = getClass().getClassLoader().getResourceAsStream("testFieldMappings.properties");
        context.addInputStream("/WEB-INF/webconfig-model.xml", is);
        context.addInputStream("/WEB-INF/CLASS_NAME_MAPPINGS", classesIS);
        context.addInputStream("/WEB-INF/FIELD_NAME_MAPPINGS", fieldsIS);
        webConfig = WebConfig.parse(context, os.getModel());
    }
    
    protected InterMineBag createEmployeeList() throws Exception {
        ObjectStoreWriter osw = null;
        try {
            Profile superUser = im.getProfileManager().getSuperuserProfile();
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
            Employee e1 = new Employee();
            e1.setName("Employee1");
            Employee e2 = new Employee();
            e2.setName("Employee2");
            Department d1 = new Department();
            d1.setName("department");
            Company company = new CompanyShadow();
            company.setName("company");
            Contractor contractor = new Contractor();
            contractor.setName("contractor");
            osw.store(contractor);
            company.addContractors(contractor);
            osw.store(company);
            d1.setCompany(company);
            osw.store(d1);
            e1.setDepartment(d1);
            e2.setDepartment(d1);
            osw.store(e1);
            osw.store(e2);
            InterMineBag list = superUser.createBag("employeeList", "Employee", "", im.getClassKeys());
            Collection<Integer> ids = new ArrayList<Integer>();
            ids.add(e1.getId()); ids.add(e2.getId());
            list.addIdsToBag(ids, "Employee");
            return list;
        } finally {
            osw.close();
        }
    }
    
    protected InterMineBag createCompanyList() throws Exception {
        ObjectStoreWriter osw = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
            Profile superUser = im.getProfileManager().getSuperuserProfile();
            Company c1 = new CompanyShadow();
            c1.setName("CompanyA");
            Company c2 = new CompanyShadow();
            c2.setName("CompanyB");
            osw.store(c1);
            osw.store(c2);
            InterMineBag list = superUser.createBag("companyList", "Company", "", im.getClassKeys());
            Collection<Integer> ids = new ArrayList<Integer>();
            ids.add(c1.getId()); ids.add(c2.getId());
            list.addIdsToBag(ids, "Company");
            return list;
        } finally {
            osw.close();
        }
    }
}
