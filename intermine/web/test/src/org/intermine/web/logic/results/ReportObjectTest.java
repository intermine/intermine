package org.intermine.web.logic.results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;

/**
 * ReportObject testing
 * @author radek
 *
 */
public class ReportObjectTest extends TestCase
{

    private Model model;
    private WebConfig webConfig;
    private Map<String, List<FieldDescriptor>> classKeys;

    private Company company;
    private Address address;

    protected void setUp() throws Exception {
        super.setUp();

        // InterMine Objects
        company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(new Integer(1));
        company.setName("Weyland Yutani");
        company.setVatNumber(101);

        address = (Address) DynamicUtil.createObject(Collections.singleton(Address.class));
        address.setId(new Integer(1));
        address.setAddress("Space");

        company.setAddress(address);

        // Model
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        model = os.getModel();

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
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
    }

    @SuppressWarnings("unchecked")
    public void testFieldConfigsNoConfig() throws Exception {
        // setup the object we are testing
        WebConfig newWebConfig = new WebConfig();
        ReportObject reportObject = new ReportObject(company, model, newWebConfig, classKeys);

        // test
        assertEquals(new ArrayList(), reportObject.getFieldConfigs());
    }

    @SuppressWarnings("unchecked")
    public void testGetFieldConfigs() throws Exception {
        // setup the object we are testing
        ReportObject reportObject = new ReportObject(company, model, webConfig, classKeys);

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
        assertEquals(Collections.unmodifiableCollection(new ArrayList()).getClass(), reportObject.getFieldConfigs().getClass());
        assertEquals(fieldConfigs, new ArrayList(reportObject.getFieldConfigs()));
    }

    public void testGetFieldValue() throws Exception {
        // setup the object we are testing
        ReportObject reportObject = new ReportObject(company, model, webConfig, classKeys);

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

        ReportObject reportObject = new ReportObject(company, model, webConfig, classKeys);

        // test
        assertEquals(2, reportObject.getObjectSummaryFields().size());
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

        ReportObject reportObject = new ReportObject(company, model, webConfig, classKeys);

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

        ReportObject reportObject = new ReportObject(company, model, webConfig, classKeys);

        // test
        assertEquals("Company.name", reportObject.getObjectSummaryFields().get(0).getPathString());
    }

}
