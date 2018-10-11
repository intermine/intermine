package org.intermine.web.logic.results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;

/**
 * ReportObject testing
 * @author radek
 *
 */
public class ReportObjectTest extends InterMineAPITestCase
{

    private WebConfig webConfig;

    private Company company;
    private Address address;

    public ReportObjectTest() throws Exception {
        super(null);

        // InterMine Objects
        company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(new Integer(1));
        company.setName("Weyland Yutani");
        company.setVatNumber(101);

        address = (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        address.setId(new Integer(1));
        address.setAddress("Space");

        company.setAddress(address);

        // WebConfig
        webConfig = new WebConfig();

        Type type  = new Type();
        type.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        type.addFieldConfig(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("vatNumber");
        type.addFieldConfig(df2);
        FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("address.address");
        type.addFieldConfig(df3);
        webConfig.addType(type);

        // ClassKeys
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void testFieldConfigsNoConfig() throws Exception {
        // setup the object we are testing
        WebConfig newWebConfig = new WebConfig();
        ReportObject reportObject = new ReportObject(company, newWebConfig, im, null);

        // test
        assertEquals(new ArrayList<FieldConfig>(), reportObject.getFieldConfigs());
    }

    public void testGetFieldConfigs() throws Exception {
        // setup the object we are testing
        ReportObject reportObject = new ReportObject(company, webConfig, im, null);

        List<FieldConfig> fieldConfigs = new ArrayList<FieldConfig>();
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        fieldConfigs.add(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("vatNumber");
        fieldConfigs.add(df2);
        FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("address.address");
        fieldConfigs.add(df3);

        // test
        assertEquals(fieldConfigs, new ArrayList<FieldConfig>(reportObject.getFieldConfigs()));
    }

    public void testGetFieldValue() throws Exception {
        // setup the object we are testing
        ReportObject reportObject = new ReportObject(company, webConfig, im, null);

        // test
        assertEquals("Weyland Yutani", reportObject.getFieldValue("name"));
        assertEquals(101, reportObject.getFieldValue("vatNumber"));
        assertEquals("Space", reportObject.getFieldValue("address.address"));
    }

    public void testGetSummaryFields() throws Exception {
        // setup the object we are testing
        webConfig = new WebConfig();

        Type type  = new Type();
        type.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        type.addFieldConfig(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("vatNumber");
        df2.setShowInSummary(false); // hiding from a summary...
        type.addFieldConfig(df2);
        FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("address.address");
        type.addFieldConfig(df3);
        webConfig.addType(type);

        ReportObject reportObject = new ReportObject(company, webConfig, im, null);

        // test
        assertEquals(3, reportObject.getObjectSummaryFields().size());
    }

    public void testFieldDisplayer() throws Exception {
        // setup the object we are testing
        webConfig = new WebConfig();

        Type type  = new Type();
        type.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        df1.setDisplayer("Lorem ipsum dolor sit"); // setting a displayer
        type.addFieldConfig(df1);
        webConfig.addType(type);

        ReportObject reportObject = new ReportObject(company, webConfig, im, null);

        // test
        assertEquals(true, reportObject.getObjectSummaryFields().get(0).getValueHasDisplayer());
        assertEquals("Lorem ipsum dolor sit", reportObject.getObjectSummaryFields().get(0).getDisplayerPage());
    }

    public void testFieldPathString() throws Exception {
        // setup the object we are testing
        webConfig = new WebConfig();

        Type type  = new Type();
        type.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        type.addFieldConfig(df1);
        webConfig.addType(type);

        ReportObject reportObject = new ReportObject(company, webConfig, im, null);

        // test
        assertEquals("Company.name", reportObject.getObjectSummaryFields().get(0).getPathString());
    }

    /**
     * Test our References and Collections from a ReportObject
     * @throws Exception
     */
    public void testGetRefsAndCols() throws Exception {
        // setup the object we are testing
        webConfig = new WebConfig();

        Type type  = new Type();
        type.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        type.addFieldConfig(df1);
        webConfig.addType(type);

        ReportObject reportObject = new ReportObject(company, webConfig, im, null);

        // build the map of DisplayField(s) that we want to see in the result
        Map<String, DisplayField> m = new HashMap<String, DisplayField>();
        for (FieldDescriptor fd : im.getModel().getClassDescriptorByName(
                "org.intermine.model.testmodel.Company").getAllFieldDescriptors()) {
            // Reference
            if (fd.isReference()) {
                ReferenceDescriptor ref = (ReferenceDescriptor) fd;
                DisplayReference dr = new DisplayReference(null, ref, webConfig, im.getClassKeys(), null);
                m.put(fd.getName(), dr);
            }
            // Collection
            if (fd.isCollection()) {
                Object fieldValue = company.getFieldValue(fd.getName());
                DisplayCollection dc = new DisplayCollection((Collection<?>) fieldValue,
                        (CollectionDescriptor) fd, webConfig, null, im.getClassKeys(), null);
                m.put(fd.getName(), dc);
            }
        }

        // this is what we got
        Map<String, DisplayField> r = reportObject.getRefsAndCollections();

        // size match
        assertEquals(m.size(), r.size());

        // traverse what we got against what we should see
        for (String key : r.keySet()) {
            // keys match
            assertTrue(m.containsKey(key));
            // type match
            assertEquals(r.get(key).getClass().getName(), m.get(key).getClass().getName());
        }
    }

}
