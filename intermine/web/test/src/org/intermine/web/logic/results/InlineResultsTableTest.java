package org.intermine.web.logic.results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;

/**
 * InlineResultsTable (used on object details and bag upload confirm) testing
 * @author radek
 *
 */
public class InlineResultsTableTest extends TestCase
{

    private Model model;
    private WebConfig webConfig;
    private Properties classKeyProps;
    private ObjectStore os;

    private Company company;
    private CEO ceo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // InterMine Objects
        company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(new Integer(1));
        company.setName("Weyland Yutani");

        ceo = (CEO) DynamicUtil.createObject(Collections.singleton(CEO.class));
        ceo.setId(new Integer(2));
        ceo.setName("Radek");

        ceo.setCompany(company);
        company.setcEO(ceo);

        // Model
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        model = os.getModel();

        // WebConfig
        webConfig = new WebConfig();

        // Types & FieldConfig
        Type type1  = new Type();
        type1.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig fc1 = new FieldConfig();
        fc1.setFieldExpr("name");
        type1.addFieldConfig(fc1);
        FieldConfig fc2 = new FieldConfig();
        fc2.setFieldExpr("CEO");
        type1.addFieldConfig(fc2);
        webConfig.addType(type1);

        Type type2  = new Type();
        type2.setClassName("org.intermine.model.testmodel.CEO");
        FieldConfig fc3 = new FieldConfig();
        fc3.setFieldExpr("name");
        type2.addFieldConfig(fc3);
        FieldConfig fc4 = new FieldConfig();
        fc4.setFieldExpr("company");
        type2.addFieldConfig(fc4);
        webConfig.addType(type2);

        // ClassKeys
        classKeyProps = new Properties();
        try {
            classKeyProps.load(getClass().getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Reference types are resolved internally either from Object or ProxyReference
     * @throws Exception
     */
    public void testReferenceTypes() throws Exception {
        // Collection
        List<Object> collection = new ArrayList<Object>();
        collection.add(ceo);

        // InlineResultsTable
        InlineResultsTable resultsTable = new InlineResultsTable(collection, model, webConfig,
                null, new Integer(1), new Boolean(false), null);

        assertEquals(new Integer(1), (Integer) resultsTable.getListOfTypes().size());
        assertEquals(new Boolean(false), resultsTable.getHasMoreThanOneType());
        assertEquals(DynamicUtil.getSimpleClass(ceo), resultsTable.getListOfTypes().get(0));
    }

    /**
     * Collection passed to an InlineList requires us to pass in a list of Types, resolved
     *  higher up using PathQueryResultHelper.queryForTypesInCollection() working over OS
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testCollectionTypes() throws Exception {
        // Collection
        List<Object> collection = new ArrayList<Object>();
        collection.add(company);
        collection.add(ceo);

        // Types for a Collection from WebConfig
        List<Class<?>> typeClasses = new ArrayList<Class<?>>();
        Map<String, Type> typesMap = webConfig.getTypes();
        // for all types
        for (Type type : typesMap.values()) {
            // String to Class
            typeClasses.add(Class.forName(type.getClassName()));
        }

        // InlineResultsTable
        InlineResultsTable resultsTable = new InlineResultsTable(collection, model, webConfig,
                null, new Integer(2), new Boolean(false), typeClasses);

        assertEquals(new Integer(2), (Integer) resultsTable.getListOfTypes().size());
        assertEquals(new Integer(3), new Integer(resultsTable.getColumnsSize()));
        assertEquals(new Boolean(true), resultsTable.getHasMoreThanOneType());
        assertEquals(
                new ArrayList<Class<?>>(Arrays.asList(
                        DynamicUtil.getSimpleClass(ceo), DynamicUtil.getSimpleClass(company))),
                        resultsTable.getListOfTypes());
    }

    /**
     * Returns a list of all the possible FieldConfigs (columns) for a given table, takes care
     *  of retrieving FC from different Class Objects that can reside in the table
     * @throws Exception
     */
    public void testTableFieldConfigs() throws Exception {
        // Collection
        List<Object> collection = new ArrayList<Object>();
        collection.add(company);
        collection.add(ceo);

        // FieldConfigs list (+ Types for a Collection)
        List<FieldConfig> listOfFC = new ArrayList<FieldConfig>();
        List<Class<?>> typeClasses = new ArrayList<Class<?>>();
        Map<String, Type> typesMap = webConfig.getTypes();
        // for all types
        for (Type type : typesMap.values()) {
            // instead of using addAll, we want only unique FC, Company and CEO both have "name"
            for (FieldConfig fc: type.getFieldConfigs()) {
                if (! listOfFC.contains(fc)) {
                    listOfFC.add(fc);
                }
            }
            // String to Class
            typeClasses.add(Class.forName(type.getClassName()));
        }

        // InlineResultsTable
        InlineResultsTable resultsTable = new InlineResultsTable(collection, model, webConfig,
                new HashMap<String, List<FieldDescriptor>>(), 2, false, typeClasses);

        assertEquals(new Integer(3), new Integer(resultsTable.getTableFieldConfigs().size()));
        assertEquals(new Integer(3), new Integer(resultsTable.getColumnsSize()));
        assertEquals(new Boolean(true), resultsTable.getHasMoreThanOneType());
        assertEquals(listOfFC, resultsTable.getTableFieldConfigs());
    }

    /**
     * Check the elements saved in the table
     * @throws Exception
     */
    public void testTableElementRows() throws Exception {
        // Collection
        List<Object> collection = new ArrayList<Object>();
        collection.add(company);
        collection.add(ceo);

        // Types for a Collection from WebConfig
        List<Class<?>> typeClasses = new ArrayList<Class<?>>();
        Map<String, Type> typesMap = webConfig.getTypes();
        // for all types
        for (Type type : typesMap.values()) {
            // String to Class
            typeClasses.add(Class.forName(type.getClassName()));
        }

        // InlineResultsTable
        InlineResultsTable resultsTable = new InlineResultsTable(collection, model, webConfig,
                new HashMap<String, List<FieldDescriptor>>(), new Integer(2), new Boolean(false),
                typeClasses);

        // List<InlineResultsTableRow>
        List<Object> rows = resultsTable.getResultElementRows();
        assertEquals(new Integer(2), new Integer(rows.size()));
        // traverse and create a map to match
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object rowObject : rows) {
            Map<String, Object> resultRow = new LinkedHashMap<String, Object>();

            // assert Class equality
            assertEquals(InlineResultsTableRow.class, rowObject.getClass());
            InlineResultsTableRow row = (InlineResultsTableRow) rowObject;

            // assert the two Class names contained in a Collection
            assertTrue("Company".equals(row.getClassName()) || "CEO".equals(row.getClassName()));

            // assert the (not implemented) object IDs contained in a Collection
            assertTrue("Not implemented".equals(row.getObjectId()));

            // traverse the ResultElements
            for (Object resultElementObject : row.getItems()) {
                // some columns will be InlineTableResultElement(s)
                if (resultElementObject instanceof InlineTableResultElement) {
                    // save into map
                    resultRow.put(
                            ((InlineTableResultElement) resultElementObject)
                            .getFieldConfig().getFieldExpr(),
                            ((ResultElement) resultElementObject).getField());
                }
            }
            result.add(resultRow);
        }

        // [{name=Weyland Yutani}, {name=Radek}] as id & references do not show in FieldExpr()
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        Map<String, Object> m1 = new LinkedHashMap<String, Object>();
        m1.put("name", "Weyland Yutani");
        l.add(m1);
        Map<String, Object> m2 = new LinkedHashMap<String, Object>();
        m2.put("name", "Radek");
        l.add(m2);

        assertEquals(l, result);
    }

}
