package org.intermine.webservice.client.live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.TemplateService;
import org.intermine.webservice.client.template.TemplateParameter;
import org.junit.Test;

/**
 * This is a Java program to run a query from TestMine-Alex.
 * It was automatically generated at Fri Dec 23 14:53:40 GMT 2011
 *
 * @author TestMine-Alex
 *
 */
public class LiveGeneratedTemplateTest
{
    private static final String ROOT = "http://squirrel.flymine.org/intermine-test/service";

    /**
     * Perform the query and print the rows of results.
     * @param args command line arguments
     * @throws IOException
     */
    @Test
    public void run() throws IOException {
        ServiceFactory factory = new ServiceFactory(ROOT);
        // Edit the template parameter values to get different results
        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        parameters.add(new TemplateParameter("Employee.name", "CONTAINS", "Employee", null, "D"));
        parameters.add(new TemplateParameter("Employee.age", "<", "10", null, "B"));
        parameters.add(new TemplateParameter("Employee.age", ">", "30", null, "C"));
        parameters.add(new TemplateParameter("Employee.fullTime", "=", "true", null, "A"));

        // Name of template
        String name = "fourConstraints";
        // Template Service - use this object to fetch results.
        TemplateService service = factory.getTemplateService();

        System.out.printf("%-22.22s | %-22.22s | %-22.22s | %-22.22s\n", "Employee.name", "Employee.age", "Employee.end", "Employee.fullTime");
        Iterator<List<Object>> rows = service.getRowListIterator(name, parameters);
        while (rows.hasNext()) {
            System.out.printf("%-22.22s | %-22.22s | %-22.22s | %-22.22s\n", rows.next().toArray());
        }
        System.out.printf("%d rows\n", service.getCount(name, parameters));
    }

}

