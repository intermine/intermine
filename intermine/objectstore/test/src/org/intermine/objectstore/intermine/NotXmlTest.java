package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicBean;

public class NotXmlTest extends TestCase
{
    ObjectStore os;

    public void setUp() throws Exception {
        os = ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public void test1() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = NotXmlRenderer.DELIM + "org.intermine.model.testmodel.Employee"
            + NotXmlRenderer.DELIM + "aage" + NotXmlRenderer.DELIM + "0"
            + NotXmlRenderer.DELIM + "rdepartment" + NotXmlRenderer.DELIM + "5678"
            + NotXmlRenderer.DELIM + "afullTime" + NotXmlRenderer.DELIM + "false"
            + NotXmlRenderer.DELIM + "aid" + NotXmlRenderer.DELIM + "1234"
            + NotXmlRenderer.DELIM + "aname" + NotXmlRenderer.DELIM + "Employee1";

        String got = NotXmlRenderer.render(e);
        assertEquals(got, expected, got);
    }

    public void testParse1() throws Exception {
        String s = NotXmlRenderer.DELIM + "org.intermine.model.testmodel.Employee"
            + NotXmlRenderer.DELIM + "aid" + NotXmlRenderer.DELIM + "1234"
            + NotXmlRenderer.DELIM + "aname" + NotXmlRenderer.DELIM + "Employee1"
            + NotXmlRenderer.DELIM + "rdepartment" + NotXmlRenderer.DELIM + "5678";

        Employee obj1 = (Employee) NotXmlParser.parse(s, os);

        assertEquals("Employee1", obj1.getName());
        assertEquals(new Integer(1234), obj1.getId());
        Class c = Employee.class;
        java.lang.reflect.Field f = c.getDeclaredField("department");
        f.setAccessible(true);
        ProxyReference o = (ProxyReference) f.get(obj1);
        assertNotNull(o);
        assertEquals(new Integer(5678), o.getId());
    }

    public void testParseDynamic() throws Exception {

        String s = NotXmlRenderer.DELIM + "org.intermine.model.testmodel.Company net.sf.cglib.proxy.Factory"
            + NotXmlRenderer.DELIM + "raddress" + NotXmlRenderer.DELIM + "74328"
            + NotXmlRenderer.DELIM + "avatNumber" + NotXmlRenderer.DELIM + "100"
            + NotXmlRenderer.DELIM + "aname" + NotXmlRenderer.DELIM + "CompanyC"
            + NotXmlRenderer.DELIM + "aid" + NotXmlRenderer.DELIM + "74350";

        Company obj1 = (Company) NotXmlParser.parse(s, os);

        assertEquals("CompanyC", obj1.getName());
        assertEquals(100, obj1.getVatNumber());
        assertEquals(new Integer(74350), obj1.getId());
        Map fieldMap = ((DynamicBean) ((net.sf.cglib.proxy.Factory) obj1).getCallback(0)).getMap();
        ProxyReference addressRef = (ProxyReference) fieldMap.get("Address");
        assertNotNull(addressRef);
        assertEquals(new Integer(74328), addressRef.getId());
    }

    public void testHandleDelims() throws Exception {
        Employee e = new Employee();
        e.setId(new Integer(2874));
        e.setName("Flibble $_^ Wotsit");

        String notXml = NotXmlRenderer.render(e);

        Employee reparsed = (Employee) NotXmlParser.parse(notXml, os);

        assertEquals(e.getName(), reparsed.getName());
        assertEquals(e.getId(), reparsed.getId());
    }
}
