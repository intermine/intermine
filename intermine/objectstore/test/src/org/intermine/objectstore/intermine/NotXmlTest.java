package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2012 FlyMine
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
    ObjectStoreInterMineImpl os;

    public void setUp() throws Exception {
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public void test1() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = NotXmlParser.DELIM + "org.intermine.model.testmodel.Employee"
            + NotXmlParser.DELIM + "aage" + NotXmlParser.DELIM + "0"
            + NotXmlParser.DELIM + "rdepartment" + NotXmlParser.DELIM + "5678"
            + NotXmlParser.DELIM + "afullTime" + NotXmlParser.DELIM + "false"
            + NotXmlParser.DELIM + "aid" + NotXmlParser.DELIM + "1234"
            + NotXmlParser.DELIM + "aname" + NotXmlParser.DELIM + "Employee1";

        String got = NotXmlRenderer.render(e).toString();
        assertEquals(got, expected, got);
    }

    public void testParse1() throws Exception {
        String s = NotXmlParser.DELIM + "org.intermine.model.testmodel.Employee"
            + NotXmlParser.DELIM + "aid" + NotXmlParser.DELIM + "1234"
            + NotXmlParser.DELIM + "aname" + NotXmlParser.DELIM + "Employee1"
            + NotXmlParser.DELIM + "rdepartment" + NotXmlParser.DELIM + "5678";

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

        String s = NotXmlParser.DELIM + "org.intermine.model.testmodel.Company net.sf.cglib.proxy.Factory"
            + NotXmlParser.DELIM + "raddress" + NotXmlParser.DELIM + "74328"
            + NotXmlParser.DELIM + "avatNumber" + NotXmlParser.DELIM + "100"
            + NotXmlParser.DELIM + "aname" + NotXmlParser.DELIM + "CompanyC"
            + NotXmlParser.DELIM + "aid" + NotXmlParser.DELIM + "74350";

        Company obj1 = (Company) NotXmlParser.parse(s, os);

        assertEquals("CompanyC", obj1.getName());
        assertEquals(100, obj1.getVatNumber());
        assertEquals(new Integer(74350), obj1.getId());
        Map fieldMap = ((DynamicBean) ((net.sf.cglib.proxy.Factory) obj1).getCallback(0)).getMap();
        ProxyReference addressRef = (ProxyReference) fieldMap.get("address");
        assertNotNull(addressRef);
        assertEquals(new Integer(74328), addressRef.getId());
    }

    public void testHandleDelims() throws Exception {
        Employee e = new Employee();
        e.setId(new Integer(2874));
        e.setName("Flibble $_^ Wotsit");

        String notXml = NotXmlRenderer.render(e).toString();

        Employee reparsed = (Employee) NotXmlParser.parse(notXml, os);

        assertEquals(e.getName(), reparsed.getName());
        assertEquals(e.getId(), reparsed.getId());
    }

    public void testSplitPerformance() throws Exception {
        StringBuilder sb = new StringBuilder(49999997);
        for (int i = 0; i < 1000000; i++) {
            if (i > 0) {
                sb.append(NotXmlParser.DELIM);
            }
            sb.append("kjakjhsdlkfgjhfjklslkjalkjdfkjalkaskdjhfjkslkjh");
        }
        String s = sb.toString();
        sb = null;
        assertEquals(49999997, s.length());
        long time = System.currentTimeMillis();
        for (int o = 0; o < 10; o++) {
            NotXmlParser.SPLITTER.split(s);
        }
        System.out.println("SPLIT took " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        for (int o = 0; o < 10; o++) {
            String res[] = new String[1000000];
            for (int i = 0; i < 1000000; i++) {
                res[i] = s.substring(i * 50, i * 50 + 47);
            }
        }
        System.out.println("SUBSTRING took " + (System.currentTimeMillis() - time) + " ms");
    }
}
