package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

import junit.framework.AssertionFailedError;

import org.intermine.SummaryAssertionFailedError;
import org.intermine.SummaryException;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.*;
import org.intermine.objectstore.query.*;
import org.intermine.util.DynamicUtil;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * TestCase for testing InterMine Queries
 * To check results:
 * add results to the results mapItemsToNames
 * override executeTest to run query and assert that the result is what is expected
 */

public abstract class ObjectStoreQueryTestCase {
    public static final Object NO_RESULT = new Object() {
        public String toString() {
            return "NO RESULT";
        }
    };

    protected static ObjectStore os;
    protected static ObjectStoreWriter storeDataWriter;

    protected static Map data;
    protected static Map<String, Query> queries = new HashMap<String, Query>();
    protected static Map<String, Object> results = new LinkedHashMap<String, Object>();
    protected boolean strictTestQueries = true;

    public static void oneTimeSetUp(
            String osName, String osWriterName, String modelName, String itemsXmlFilename) throws Exception {

        os = ObjectStoreFactory.getObjectStore(osName);
        storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter(osWriterName);

        // As required clean up any junk other tests have left behind
        // TODO: Really we should just wipe the objectstore between tests or at least test classes, if this is
        // performance feasible

        System.out.println("Deleted " + ObjectStoreTestUtils.deleteAllObjectsInClass(storeDataWriter, Contractor.class) + " " + Contractor.class);
        //System.out.println("Deleted " + ObjectStoreTestUtils.deleteAllObjectsInClass(os, storeDataWriter, Employable.class) + " " + Employable.class);
        System.out.println("Deleted " + ObjectStoreTestUtils.deleteAllObjectsInClass(storeDataWriter, Employee.class) + " " + Employee.class);
        System.out.println("Deleted " + ObjectStoreTestUtils.deleteAllObjectsInClass(storeDataWriter, Secretary.class) + " " + Employee.class);

        data = ObjectStoreTestUtils.getTestData(modelName, itemsXmlFilename);
        ObjectStoreTestUtils.storeData(storeDataWriter, data);

        setUpQueries();
        setUpResults();
    }

    @AfterClass
    public static void oneTimeShutdown() throws Exception {
        storeDataWriter.close();
    }

    /**
     * Test the queries produce the appropriate result
     */
    @Test
    public void testQueries() throws Throwable {
        StringWriter errorMessage = new StringWriter();
        PrintWriter writer = new PrintWriter(errorMessage);
        int failureCount = 0;
        int errorCount = 0;
        for (String type : results.keySet()) {
            // Does this appear in the queries mapItemsToNames;
            if (!(queries.containsKey(type))) {
                writer.println("\n" + type + " does not appear in the queries mapItemsToNames");
                failureCount++;
            } else {
                Object result = results.get(type);
                if (result != NO_RESULT) {
                    long startTime = System.currentTimeMillis();
                    try {
                        executeTest(type);
                    } catch (AssertionFailedError e) {
                        writer.println("\n" + type + " has failed: " + e.getMessage());
                        //e.printStackTrace(writer);
                        failureCount++;
                    } catch (Throwable t) {
                        writer.println("\n" + type + " produced an error:");
                        t.printStackTrace(writer);
                        errorCount++;
                    } finally {
                        System.out.println("Test " + type + " took " + (System.currentTimeMillis() - startTime) + " ms");
                    }
                }
            }
        }
        for (String type : queries.keySet()) {
            Object result = results.get(type);
            if (result == null) {
                if (strictTestQueries) {
                    writer.println("\n" + type + " does not appear in the results mapItemsToNames");
                    failureCount++;
                }
            }
        }
        writer.flush();
        errorMessage.flush();

        if (errorCount > 0) {
            if (failureCount > 0) {
                throw new SummaryException(errorMessage.toString().replace("<", "&amp;amp;lt;"), errorCount + " errors and " + failureCount + " failures present");
            } else {
                throw new SummaryException(errorMessage.toString().replace("<", "&amp;amp;lt;"), errorCount + " errors present");
            }
        } else if (failureCount > 0) {
            throw new SummaryAssertionFailedError(errorMessage.toString().replace("<", "&amp;amp;lt;"), failureCount + " failures present");
        }
    }

    /**
     * Set up the set of queries we are testing
     *
     * @throws Exception if an error occurs
     */
    public static void setUpQueries() throws Exception {
        queries.put("SelectSimpleObject", selectSimpleObject());
        queries.put("SubQuery", subQuery());
        queries.put("WhereSimpleEquals", whereSimpleEquals());
        queries.put("WhereSimpleNotEquals", whereSimpleNotEquals());
        queries.put("WhereSimpleNegEquals", whereSimpleNegEquals());
        queries.put("WhereSimpleLike", whereSimpleLike());
        queries.put("WhereEqualsString", whereEqualString());
        queries.put("WhereAndSet", whereAndSet());
        queries.put("WhereOrSet", whereOrSet());
        queries.put("WhereNotSet", whereNotSet());
        queries.put("WhereSubQueryField", whereSubQueryField());
        queries.put("WhereSubQueryClass", whereSubQueryClass());
        queries.put("WhereNotSubQueryClass", whereNotSubQueryClass());
        queries.put("WhereNegSubQueryClass", whereNegSubQueryClass());
        queries.put("WhereClassClass", whereClassClass());
        queries.put("WhereNotClassClass", whereNotClassClass());
        queries.put("WhereNegClassClass", whereNegClassClass());
        queries.put("Contains11", contains11());
        queries.put("ContainsNot11", containsNot11());
        queries.put("ContainsNeg11", containsNeg11());
        queries.put("Contains1N", contains1N());
        queries.put("ContainsNot1N", containsNot1N());
        queries.put("ContainsN1", containsN1());
        queries.put("ContainsMN", containsMN());
        queries.put("ContainsNotMN", containsNotMN());
        queries.put("ContainsDuplicatesMN", containsDuplicatesMN());
        queries.put("SimpleGroupBy", simpleGroupBy());
        queries.put("MultiJoin", multiJoin());
        queries.put("SelectComplex", selectComplex());
        queries.put("SelectClassAndSubClasses", selectClassAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses", selectInterfaceAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses2", selectInterfaceAndSubClasses2());
        queries.put("SelectInterfaceAndSubClasses3", selectInterfaceAndSubClasses3());
        //queries.put("SelectClassFromSubQuery", selectClassFromSubQuery());
        queries.put("OrderByAnomaly", orderByAnomaly());
        queries.put("SelectUnidirectionalCollection", selectUnidirectionalCollection());
        queries.put("EmptyAndConstraintSet", emptyAndConstraintSet());
        queries.put("EmptyOrConstraintSet", emptyOrConstraintSet());
        queries.put("EmptyNandConstraintSet", emptyNandConstraintSet());
        queries.put("EmptyNorConstraintSet", emptyNorConstraintSet());
        queries.put("BagConstraint", bagConstraint());
        queries.put("InterfaceField", interfaceField());
        queries.put("DynamicInterfacesAttribute", dynamicInterfacesAttribute());
        queries.put("DynamicClassInterface", dynamicClassInterface());
        queries.put("DynamicClassRef1", dynamicClassRef1());
        queries.put("DynamicClassRef2", dynamicClassRef2());
        queries.put("DynamicClassRef3", dynamicClassRef3());
        queries.put("DynamicClassRef4", dynamicClassRef4());
        queries.put("DynamicClassConstraint", dynamicClassConstraint());
        queries.put("ContainsConstraintNull", containsConstraintNull());
        queries.put("ContainsConstraintNotNull", containsConstraintNotNull());
        queries.put("ContainsConstraintNullCollection1N", containsConstraintNullCollection1N());
        queries.put("ContainsConstraintNotNullCollection1N", containsConstraintNotNullCollection1N());
        queries.put("ContainsConstraintNullCollectionMN", containsConstraintNullCollectionMN());
        queries.put("ContainsConstraintNotNullCollectionMN", containsConstraintNotNullCollectionMN());
        queries.put("SimpleConstraintNull", simpleConstraintNull());
        queries.put("SimpleConstraintNotNull", simpleConstraintNotNull());
        queries.put("TypeCast", typeCast());
        queries.put("IndexOf", indexOf());
        queries.put("Substring", substring());
        queries.put("Substring2", substring2());
        queries.put("OrderByReference", orderByReference());
        queries.put("FailDistinctOrder", failDistinctOrder());
        queries.put("FailDistinctOrder2", failDistinctOrder2());
        queries.put("NegativeNumbers", negativeNumbers());
        queries.put("Lower", lower());
        queries.put("Upper", upper());
        queries.put("Greatest", greatest());
        queries.put("Least", least());

        // test 'foo' IN bag
        queries.put("LargeBagConstraint", largeBagConstraint(false));
        // test 'foo' NOT IN bag
        queries.put("LargeBagNotConstraint", largeBagConstraint(true));

        // tests using a temporary table for the bag - 'foo' IN bag
        queries.put("LargeBagConstraintUsingTable", largeBagConstraint(false));

        // tests using a temporary table for the bag - 'foo' NOT IN bag
        queries.put("LargeBagNotConstraintUsingTable", largeBagConstraint(true));
        queries.put("SubqueryExistsConstraint", subqueryExistsConstraint());
        queries.put("NotSubqueryExistsConstraint", notSubqueryExistsConstraint());
        queries.put("SubqueryExistsConstraintNeg", subqueryExistsConstraintNeg());
        queries.put("ObjectPathExpression", objectPathExpression());
        queries.put("ObjectPathExpression2", objectPathExpression2());
        queries.put("ObjectPathExpression3", objectPathExpression3());
        queries.put("ObjectPathExpression4", objectPathExpression4());
        queries.put("ObjectPathExpression5", objectPathExpression5());
        queries.put("FieldPathExpression", fieldPathExpression());
        queries.put("FieldPathExpression2", fieldPathExpression2());
        queries.put("CollectionPathExpression", collectionPathExpression());
        queries.put("CollectionPathExpression2", collectionPathExpression2());
        queries.put("CollectionPathExpression3", collectionPathExpression3());
        queries.put("CollectionPathExpression4", collectionPathExpression4());
        queries.put("CollectionPathExpression5", collectionPathExpression5());
        queries.put("CollectionPathExpression6", collectionPathExpression6());
        queries.put("CollectionPathExpression7", collectionPathExpression7());
        queries.put("OrSubquery", orSubquery());
        queries.put("ScientificNumber", scientificNumber());
        queries.put("LowerBag", lowerBag());
        queries.put("FetchBag", fetchBag());
        queries.put("ObjectStoreBag", objectStoreBag());
        queries.put("ObjectStoreBagQueryClass", objectStoreBagQueryClass());
        queries.put("OrderDescending", orderDescending());
        queries.put("ObjectStoreBagCombination", objectStoreBagCombination());
        queries.put("ObjectStoreBagCombination2", objectStoreBagCombination2());
        queries.put("ObjectStoreBagsForObject", objectStoreBagsForObject());
        queries.put("ObjectStoreBagsForObject2", objectStoreBagsForObject2());
        queries.put("SelectForeignKey", selectForeignKey());
        queries.put("WhereCount", whereCount());
        queries.put("LimitedSubquery", limitedSubquery());
        queries.put("ObjectStoreBagCombination3", objectStoreBagCombination3());
        queries.put("TotallyFalse", totallyFalse());
        queries.put("TotallyTrue", totallyTrue());
        queries.put("MergeFalse", mergeFalse());
        queries.put("MergeTrue", mergeTrue());
        queries.put("EmptyBagConstraint", emptyBagConstraint());
        queries.put("SelectFunctionNoGroup", selectFunctionNoGroup());
        queries.put("SelectClassFromInterMineObject", selectClassFromInterMineObject());
        queries.put("SelectClassFromEmployee", selectClassFromEmployee());
        queries.put("SelectClassFromBrokeEmployable", selectClassFromBrokeEmployable());
        queries.put("SubclassCollection", subclassCollection());
        queries.put("SubclassCollection2", subclassCollection2());
        queries.put("SelectWhereBackslash", selectWhereBackslash());
        queries.put("MultiColumnObjectInCollection", multiColumnObjectInCollection());
        queries.put("RangeOverlaps", rangeOverlaps());
        queries.put("RangeDoesNotOverlap", rangeDoesNotOverlap());
        queries.put("RangeOverlapsValues", rangeOverlapsValues());
        queries.put("ConstrainClass1", constrainClass1());
        queries.put("ConstrainClass2", constrainClass2());
        queries.put("MultipleInBagConstraint1", multipleInBagConstraint1());

        // These queries require objects with IDs
        queries.put("WhereClassObject", whereClassObject());
        queries.put("SelectClassObjectSubquery", selectClassObjectSubquery());
        queries.put("BagConstraint2", bagConstraint2());
        queries.put("InterfaceReference", interfaceReference());
        queries.put("InterfaceCollection", interfaceCollection());
        queries.put("ContainsConstraintObjectRefObject", containsConstraintObjectRefObject());
        queries.put("ContainsConstraintNotObjectRefObject", containsConstraintNotObjectRefObject());
        queries.put("ContainsConstraintCollectionRefObject", containsConstraintCollectionRefObject());
        queries.put("ContainsConstraintNotCollectionRefObject", containsConstraintNotCollectionRefObject());
        queries.put("ContainsConstraintMMCollectionRefObject", containsConstraintMMCollectionRefObject());
        queries.put("ContainsConstraintNotMMCollectionRefObject", containsConstraintNotMMCollectionRefObject());
        queries.put("CollectionQueryOneMany", collectionQueryOneMany());
        queries.put("CollectionQueryManyMany", collectionQueryManyMany());
        queries.put("QueryClassBag", queryClassBag());
        queries.put("QueryClassBagMM", queryClassBagMM());
        queries.put("QueryClassBagNot", queryClassBagNot());
        queries.put("QueryClassBagNotMM", queryClassBagNotMM());
        queries.put("QueryClassBagDynamic", queryClassBagDynamic());
        //queries.put("DynamicBagConstraint", dynamicBagConstraint()); // See ticket #469
        queries.put("DynamicBagConstraint2", dynamicBagConstraint2());
        queries.put("QueryClassBagDouble", queryClassBagDouble());
        queries.put("QueryClassBagContainsObject", queryClassBagContainsObject());
        queries.put("QueryClassBagContainsObjectDouble", queryClassBagContainsObjectDouble());
        queries.put("QueryClassBagNotContainsObject", queryClassBagNotContainsObject());
        queries.put("ObjectContainsObject", objectContainsObject());
        queries.put("ObjectContainsObject2", objectContainsObject2());
        queries.put("ObjectNotContainsObject", objectNotContainsObject());
        queries.put("QueryClassBagNotViaNand", queryClassBagNotViaNand());
        queries.put("QueryClassBagNotViaNor", queryClassBagNotViaNor());
    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public static void setUpResults() throws Exception {
        Object[][] r;

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")}};
        results.put("SelectSimpleObject", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyA", new Integer(5)},
                {"CompanyB", new Integer(5)}};
        results.put("SubQuery", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyA"}};
        results.put("WhereSimpleEquals", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyB"}};
        results.put("WhereSimpleNotEquals", ObjectStoreTestUtils.toList(r));
        results.put("WhereSimpleNegEquals", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyA"},
                {"CompanyB"}};
        results.put("WhereSimpleLike", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyA"}};
        results.put("WhereEqualsString", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyB"}};
        results.put("WhereAndSet", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyA"},
                {"CompanyB"}};
        results.put("WhereOrSet", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"CompanyA"}};
        results.put("WhereNotSet", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("DepartmentA1")},
                {data.get("DepartmentB1")},
                {data.get("DepartmentB2")}};
        results.put("WhereSubQueryField", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")}};
        results.put("WhereSubQueryClass", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyB")}};
        results.put("WhereNotSubQueryClass", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyB")}};
        results.put("WhereNegSubQueryClass", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA"), data.get("CompanyA")},
                {data.get("CompanyB"), data.get("CompanyB")}};
        results.put("WhereClassClass", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA"), data.get("CompanyB")},
                {data.get("CompanyB"), data.get("CompanyA")}};
        results.put("WhereNotClassClass", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA"), data.get("CompanyB")},
                {data.get("CompanyB"), data.get("CompanyA")}};
        results.put("WhereNegClassClass", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")}};
        results.put("WhereClassObject", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("DepartmentA1"), data.get("EmployeeA1")}};
        results.put("Contains11", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("DepartmentA1"), data.get("EmployeeB1")},
                {data.get("DepartmentA1"), data.get("EmployeeB3")}};
        results.put("ContainsNot11", ObjectStoreTestUtils.toList(r));
        results.put("ContainsNeg11", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA"), data.get("DepartmentA1")}};
        results.put("Contains1N", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA"), data.get("DepartmentB1")},
                {data.get("CompanyA"), data.get("DepartmentB2")}};
        results.put("ContainsNot1N", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("DepartmentA1"), data.get("CompanyA")}};
        results.put("ContainsN1", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("ContractorA"), data.get("CompanyA")},
                {data.get("ContractorA"), data.get("CompanyB")}};
        results.put("ContainsMN", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("ContractorA"), data.get("CompanyA")},
                {data.get("ContractorA"), data.get("CompanyB")},
                {data.get("ContractorB"), data.get("CompanyA")},
                {data.get("ContractorB"), data.get("CompanyB")}};
        results.put("ContainsDuplicatesMN", ObjectStoreTestUtils.toList(r));

        results.put("ContainsNotMN", NO_RESULT); //TODO: Fix this (ticket #445)
        //results.put("ContainsNotMN", Collections.EMPTY_LIST);

        r = new Object[][]{{data.get("CompanyA"), new Long(1)},
                {data.get("CompanyB"), new Long(2)}};
        results.put("SimpleGroupBy", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1"), ((Employee) data.get("EmployeeA1")).getAddress()}};
        results.put("MultiJoin", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{new BigDecimal("3476.0000000000000000"), new BigDecimal("3142.382535593017"), "DepartmentA1", data.get("DepartmentA1")},
                {new BigDecimal("3476.0000000000000000"), new BigDecimal("3142.382535593017"), "DepartmentB1", data.get("DepartmentB1")},
                {new BigDecimal("3476.0000000000000000"), new BigDecimal("3142.382535593017"), "DepartmentB2", data.get("DepartmentB2")}};
        results.put("SelectComplex", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("SelectClassAndSubClasses", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("ContractorA")},
                {data.get("ContractorB")},
                {data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("SelectInterfaceAndSubClasses", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")},
                {data.get("DepartmentA1")},
                {data.get("DepartmentB1")},
                {data.get("DepartmentB2")}};
        results.put("SelectInterfaceAndSubClasses2", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("ContractorA")},
                {data.get("ContractorB")},
                {data.get("EmployeeA1")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB3")}};
        results.put("SelectInterfaceAndSubClasses3", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{new Integer(5), "CompanyA"},
                {new Integer(5), "CompanyB"}};
        results.put("OrderByAnomaly", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("Secretary1")},
                {data.get("Secretary2")},
                {data.get("Secretary3")}};
        results.put("SelectUnidirectionalCollection", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")}};
        results.put("SelectClassObjectSubquery", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")}};
        results.put("EmptyAndConstraintSet", ObjectStoreTestUtils.toList(r));

        results.put("EmptyOrConstraintSet", Collections.EMPTY_LIST);

        results.put("EmptyNandConstraintSet", Collections.EMPTY_LIST);

        results.put("EmptyNorConstraintSet", ObjectStoreTestUtils.toList(r));

        results.put("BagConstraint", Collections.singletonList(Collections.singletonList(data.get("CompanyA"))));

        results.put("BagConstraint2", Collections.singletonList(Collections.singletonList(data.get("CompanyA"))));

        r = new Object[][]{{data.get("EmployeeA1")}};
        results.put("InterfaceField", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")}};
        results.put("InterfaceReference", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")}};
        results.put("InterfaceCollection", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeB1"), new Integer(340), new Integer(40)}};
        results.put("DynamicInterfacesAttribute", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("ContractorA")},
                {data.get("EmployeeB1")}};
        results.put("DynamicClassInterface", ObjectStoreTestUtils.toList(r));

        results.put("DynamicClassRef1", Collections.EMPTY_LIST);
        results.put("DynamicClassRef2", Collections.EMPTY_LIST);
        results.put("DynamicClassRef3", Collections.EMPTY_LIST);
        results.put("DynamicClassRef4", Collections.EMPTY_LIST);

        r = new Object[][]{{data.get("EmployeeB1")}};
        results.put("DynamicClassConstraint", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeB1")}};
        results.put("ContainsConstraintNull", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("ContainsConstraintNotNull", ObjectStoreTestUtils.toList(r));

        results.put("ContainsConstraintNullCollection1N", Collections.EMPTY_LIST);

        r = new Object[][]{{data.get("DepartmentA1")},
                {data.get("DepartmentB1")},
                {data.get("DepartmentB2")}};
        results.put("ContainsConstraintNotNullCollection1N", ObjectStoreTestUtils.toList(r));

        results.put("ContainsConstraintNullCollectionMN", Collections.EMPTY_LIST);

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")}};
        results.put("ContainsConstraintNotNullCollectionMN", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")}};
        results.put("ContainsConstraintObjectRefObject", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeB1")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("ContainsConstraintNotObjectRefObject", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("DepartmentB1")}};
        results.put("ContainsConstraintCollectionRefObject", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("DepartmentA1")},
                {data.get("DepartmentB2")}};
        results.put("ContainsConstraintNotCollectionRefObject", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")}};
        results.put("ContainsConstraintMMCollectionRefObject", ObjectStoreTestUtils.toList(r));

        results.put("ContainsConstraintNotMMCollectionRefObject", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)")); //TODO: Fix this (ticket #445)
        //results.put("ContainsConstraintNotMMCollectionRefObject", Collections.EMPTY_LIST);

        r = new Object[][]{{data.get("EmployeeA1")}};
        results.put("SimpleConstraintNull", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeB1")},
                {data.get("EmployeeB3")}};
        results.put("SimpleConstraintNotNull", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"10"},
                {"20"},
                {"30"},
                {"40"},
                {"50"},
                {"60"}};
        results.put("TypeCast", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{new Integer(5)},
                {new Integer(5)},
                {new Integer(5)},
                {new Integer(5)},
                {new Integer(5)},
                {new Integer(5)}};
        results.put("IndexOf", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"mp"},
                {"mp"},
                {"mp"},
                {"mp"},
                {"mp"},
                {"mp"}};
        results.put("Substring", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"mployeeA1"},
                {"mployeeA2"},
                {"mployeeA3"},
                {"mployeeB1"},
                {"mployeeB2"},
                {"mployeeB3"}};
        results.put("Substring2", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("OrderByReference", ObjectStoreTestUtils.toList(r));

        results.put("FailDistinctOrder", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));
        results.put("FailDistinctOrder2", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeB2")}};
        results.put("LargeBagConstraint", NO_RESULT);
        results.put("LargeBagConstraintUsingTable", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB3")}};
        results.put("LargeBagNotConstraint", NO_RESULT);
        results.put("LargeBagNotConstraintUsingTable", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("NegativeNumbers", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"employeea1"},
                {"employeea2"},
                {"employeea3"},
                {"employeeb1"},
                {"employeeb2"},
                {"employeeb3"}};
        results.put("Lower", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{"EMPLOYEEA1"},
                {"EMPLOYEEA2"},
                {"EMPLOYEEA3"},
                {"EMPLOYEEB1"},
                {"EMPLOYEEB2"},
                {"EMPLOYEEB3"}};
        results.put("Upper", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{2000},
                {5678}};
        results.put("Greatest", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{1234},
                {2000}};
        results.put("Least", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")}};
        results.put("CollectionQueryOneMany", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("Secretary1")},
                {data.get("Secretary2")}};
        results.put("CollectionQueryManyMany", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB1")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB2")}};
        results.put("QueryClassBag", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{((HasSecretarys) data.get("CompanyA")).getId(), data.get("Secretary1")},
                {((HasSecretarys) data.get("CompanyA")).getId(), data.get("Secretary2")},
                {((HasSecretarys) data.get("CompanyA")).getId(), data.get("Secretary3")},
                {((HasSecretarys) data.get("CompanyB")).getId(), data.get("Secretary1")},
                {((HasSecretarys) data.get("CompanyB")).getId(), data.get("Secretary2")}};
        results.put("QueryClassBagMM", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeB1")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeB2")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeB3")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeA1")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeA2")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeA3")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB3")}};
        results.put("QueryClassBagNot", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));

        results.put("QueryClassBagNotMM", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));

        results.put("QueryClassBagDynamic", Collections.EMPTY_LIST);

        // results.put("DynamicBagConstraint", Collections.singletonList(Collections.singletonList(data.get("EmployeeB1")))); // See ticket #469
        results.put("DynamicBagConstraint2", Collections.singletonList(Collections.singletonList(data.get("EmployeeB1"))));

        r = new Object[][]{{((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1"), data.get("EmployeeA1")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1"), data.get("EmployeeA2")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1"), data.get("EmployeeA3")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2"), data.get("EmployeeA1")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2"), data.get("EmployeeA2")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2"), data.get("EmployeeA3")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3"), data.get("EmployeeA1")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3"), data.get("EmployeeA2")},
                {((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3"), data.get("EmployeeA3")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB1"), data.get("EmployeeB1")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB1"), data.get("EmployeeB2")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB2"), data.get("EmployeeB1")},
                {((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB2"), data.get("EmployeeB2")}};
        results.put("QueryClassBagDouble", ObjectStoreTestUtils.toList(r));
        results.put("QueryClassBagContainsObject", Collections.singletonList(Collections.singletonList(((Department) data.get("DepartmentA1")).getId())));
        results.put("QueryClassBagContainsObjectDouble", Collections.singletonList(Collections.singletonList(((Department) data.get("DepartmentA1")).getId())));
        results.put("QueryClassBagNotContainsObject", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));
        results.put("ObjectContainsObject", Collections.singletonList(Collections.singletonList("hello")));
        results.put("ObjectContainsObject2", Collections.EMPTY_LIST);
        results.put("ObjectNotContainsObject", Collections.EMPTY_LIST);
        results.put("QueryClassBagNotViaNand", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));
        results.put("QueryClassBagNotViaNor", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));
        results.put("SubqueryExistsConstraint", Collections.singletonList(Collections.singletonList("hello")));
        results.put("NotSubqueryExistsConstraint", Collections.EMPTY_LIST);
        results.put("SubqueryExistsConstraintNeg", Collections.EMPTY_LIST);

        r = new Object[][]{{data.get("EmployeeA1"), data.get("DepartmentA1")},
                {data.get("EmployeeA2"), data.get("DepartmentA1")},
                {data.get("EmployeeA3"), data.get("DepartmentA1")},
                {data.get("EmployeeB1"), data.get("DepartmentB1")},
                {data.get("EmployeeB2"), data.get("DepartmentB1")},
                {data.get("EmployeeB3"), data.get("DepartmentB2")}};
        results.put("ObjectPathExpression", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("EmployeeA1"), data.get("Employee Street, AVille")},
                {data.get("EmployeeA2"), data.get("Employee Street, AVille")},
                {data.get("EmployeeA3"), data.get("Employee Street, AVille")},
                {data.get("EmployeeB1"), null},
                {data.get("EmployeeB2"), data.get("Employee Street, BVille")},
                {data.get("EmployeeB3"), data.get("Employee Street, BVille")}};
        results.put("ObjectPathExpression2", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("EmployeeA1"), data.get("CompanyA")},
                {data.get("EmployeeA2"), data.get("CompanyA")},
                {data.get("EmployeeA3"), data.get("CompanyA")},
                {data.get("EmployeeB1"), data.get("CompanyB")},
                {data.get("EmployeeB2"), data.get("CompanyB")},
                {data.get("EmployeeB3"), data.get("CompanyB")}};
        results.put("ObjectPathExpression3", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("EmployeeA1"), data.get("Company Street, AVille")},
                {data.get("EmployeeA2"), data.get("Company Street, AVille")},
                {data.get("EmployeeA3"), data.get("Company Street, AVille")},
                {data.get("EmployeeB1"), data.get("Company Street, BVille")},
                {data.get("EmployeeB2"), data.get("Company Street, BVille")},
                {data.get("EmployeeB3"), data.get("Company Street, BVille")}};
        results.put("ObjectPathExpression4", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("EmployeeA1"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("Company Street, AVille")},
                {data.get("EmployeeA2"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("Company Street, AVille")},
                {data.get("EmployeeA3"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("Company Street, AVille")},
                {data.get("EmployeeB1"), data.get("DepartmentB1"), data.get("CompanyB"), data.get("Company Street, BVille")},
                {data.get("EmployeeB2"), data.get("DepartmentB1"), data.get("CompanyB"), data.get("Company Street, BVille")},
                {data.get("EmployeeB3"), data.get("DepartmentB2"), data.get("CompanyB"), data.get("Company Street, BVille")}};
        results.put("ObjectPathExpression5", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("CompanyA"), null},
                {data.get("CompanyB"), "EmployeeB1"}};
        results.put("FieldPathExpression", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("EmployeeA1"), "Company Street, AVille"},
                {data.get("EmployeeA2"), "Company Street, AVille"},
                {data.get("EmployeeA3"), "Company Street, AVille"},
                {data.get("EmployeeB1"), "Company Street, BVille"},
                {data.get("EmployeeB2"), "Company Street, BVille"},
                {data.get("EmployeeB3"), "Company Street, BVille"}};
        results.put("FieldPathExpression2", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("DepartmentA1"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3"))},
                {data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"))},
                {data.get("DepartmentB2"), Collections.singletonList(data.get("EmployeeB3"))}};
        results.put("CollectionPathExpression", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("EmployeeA1"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3"))},
                {data.get("EmployeeA2"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3"))},
                {data.get("EmployeeA3"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3"))},
                {data.get("EmployeeB1"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"))},
                {data.get("EmployeeB2"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"))},
                {data.get("EmployeeB3"), Collections.singletonList(data.get("EmployeeB3"))}};
        results.put("CollectionPathExpression2", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("CompanyA"), Collections.singletonList(Arrays.asList(data.get("DepartmentA1"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3"))))},
                {data.get("CompanyB"), Arrays.asList(Arrays.asList(data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"))), Arrays.asList(data.get("DepartmentB2"), Collections.singletonList(data.get("EmployeeB3"))))}};
        results.put("CollectionPathExpression3", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("CompanyA"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3"))},
                {data.get("CompanyB"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"), data.get("EmployeeB3"))}};
        results.put("CollectionPathExpression4", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("CompanyA"), Collections.singletonList(data.get("DepartmentA1"))},
                {data.get("CompanyB"), Collections.singletonList(data.get("DepartmentB1"))}};
        results.put("CollectionPathExpression5", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("DepartmentA1"), data.get("CompanyA"), Arrays.asList(data.get("DepartmentA1"))},
                {data.get("DepartmentB1"), data.get("CompanyB"), Arrays.asList(data.get("DepartmentB1"), data.get("DepartmentB2"))},
                {data.get("DepartmentB2"), data.get("CompanyB"), Arrays.asList(data.get("DepartmentB1"), data.get("DepartmentB2"))}};
        results.put("CollectionPathExpression6", ObjectStoreTestUtils.toList(r));
        results.put("CollectionPathExpression7", Arrays.asList(
                Arrays.asList(data.get("EmployeeA1"), Arrays.asList(Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA")))),
                Arrays.asList(data.get("EmployeeA2"), Arrays.asList(Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA")))),
                Arrays.asList(data.get("EmployeeA3"), Arrays.asList(Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA")))),
                Arrays.asList(data.get("EmployeeB1"), Arrays.asList(Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB")))),
                Arrays.asList(data.get("EmployeeB2"), Arrays.asList(Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB")))),
                Arrays.asList(data.get("EmployeeB3"), Arrays.asList(Arrays.asList(data.get("DepartmentB2"), data.get("CompanyB"))))));

        r = new Object[][]{{data.get("CompanyA")},
                {data.get("CompanyB")},
                {data.get("ContractorA")},
                {data.get("EmployeeB1")}};
        results.put("OrSubquery", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("Types1")}};
        results.put("ScientificNumber", ObjectStoreTestUtils.toList(r));

        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeB1")}};
        results.put("LowerBag", ObjectStoreTestUtils.toList(r));
        results.put("FetchBag", Collections.EMPTY_LIST);
        results.put("ObjectStoreBag", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagQueryClass", Collections.EMPTY_LIST);
        r = new Object[][]{{data.get("EmployeeB3")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA1")}};
        results.put("OrderDescending", ObjectStoreTestUtils.toList(r));
        results.put("ObjectStoreBagCombination", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagCombination2", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagsForObject", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagsForObject2", Collections.EMPTY_LIST);

        r = new Object[][]{{((Employee) data.get("EmployeeA1")).getDepartment().getId()},
                {((Employee) data.get("EmployeeA2")).getDepartment().getId()},
                {((Employee) data.get("EmployeeA3")).getDepartment().getId()},
                {((Employee) data.get("EmployeeB1")).getDepartment().getId()},
                {((Employee) data.get("EmployeeB2")).getDepartment().getId()},
                {((Employee) data.get("EmployeeB3")).getDepartment().getId()}};
        results.put("SelectForeignKey", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("DepartmentA1"), new Long(3)},
                {data.get("DepartmentB1"), new Long(2)}};
        results.put("WhereCount", ObjectStoreTestUtils.toList(r));
        //r = new Object[][] { { "EmployeeA1" },
        //                     { "EmployeeA2" },
        //                     { "EmployeeA3" } };
        //results.put("LimitedSubquery", ObjectStoreTestUtils.toList(r));
        results.put("LimitedSubquery", NO_RESULT); // Gives a random selection of the Employees
        results.put("ObjectStoreBagCombination3", Collections.EMPTY_LIST);

        results.put("TotallyFalse", Collections.EMPTY_LIST);
        r = new Object[][]{{data.get("EmployeeA1")},
                {data.get("EmployeeA2")},
                {data.get("EmployeeA3")},
                {data.get("EmployeeB1")},
                {data.get("EmployeeB2")},
                {data.get("EmployeeB3")}};
        results.put("TotallyTrue", ObjectStoreTestUtils.toList(r));
        results.put("MergeFalse", ObjectStoreTestUtils.toList(r));
        results.put("MergeTrue", ObjectStoreTestUtils.toList(r));
        results.put("EmptyBagConstraint", Collections.EMPTY_LIST);
        int minId = ((Employee) data.get("EmployeeA1")).getId().intValue();
        minId = Math.min(minId, ((Employee) data.get("EmployeeA2")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeA3")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeB1")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeB2")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeB3")).getId().intValue());
        r = new Object[][]{{new Integer(minId)}};
        results.put("SelectFunctionNoGroup", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{Address.class, new Long(8)},
                {DynamicUtil.composeClass(Broke.class, CEO.class), new Long(1)},
                {DynamicUtil.composeClass(Broke.class, Company.class), new Long(1)},
                {DynamicUtil.composeClass(Broke.class, Contractor.class), new Long(1)},
                {Company.class, new Long(1)},
                {Contractor.class, new Long(1)},
                {Department.class, new Long(3)},
                {Employee.class, new Long(3)},
                {Manager.class, new Long(2)},
                {Range.class, new Long(4)},
                {Secretary.class, new Long(3)},
                {Types.class, new Long(1)}};
        results.put("SelectClassFromInterMineObject", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{DynamicUtil.composeClass(Broke.class, CEO.class), new Long(1)},
                {Employee.class, new Long(3)},
                {Manager.class, new Long(2)}};
        results.put("SelectClassFromEmployee", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{DynamicUtil.composeClass(Broke.class, CEO.class), new Long(1)},
                {DynamicUtil.composeClass(Broke.class, Contractor.class), new Long(1)}};
        results.put("SelectClassFromBrokeEmployable", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("DepartmentA1"), Arrays.asList(data.get("EmployeeA1"))},
                {data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1"))},
                {data.get("DepartmentB2"), Collections.singletonList(data.get("EmployeeB3"))}};
        results.put("SubclassCollection", ObjectStoreTestUtils.toList(r));
        r = new Object[][]{{data.get("DepartmentA1"), Collections.EMPTY_LIST},
                {data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1"))},
                {data.get("DepartmentB2"), Collections.EMPTY_LIST}};
        results.put("SubclassCollection2", ObjectStoreTestUtils.toList(r));
        results.put("SelectWhereBackslash", Collections.emptyList());
        results.put("MultiColumnObjectInCollection", Arrays.asList(
                Arrays.asList(data.get("CompanyA"), Arrays.asList(
                        Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA"), Arrays.asList(data.get("ContractorA"), data.get("ContractorB"))))),
                Arrays.asList(data.get("CompanyB"), Arrays.asList(
                        Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB"), Arrays.asList(data.get("ContractorA"), data.get("ContractorB"))),
                        Arrays.asList(data.get("DepartmentB2"), data.get("CompanyB"), Arrays.asList(data.get("ContractorA"), data.get("ContractorB")))))));
        results.put("RangeOverlaps", Arrays.asList(
                Arrays.asList(((InterMineObject) data.get("Range1")).getId(), ((InterMineObject) data.get("Range1")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range1")).getId(), ((InterMineObject) data.get("Range2")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range1")).getId(), ((InterMineObject) data.get("Range4")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range2")).getId(), ((InterMineObject) data.get("Range1")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range2")).getId(), ((InterMineObject) data.get("Range2")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range2")).getId(), ((InterMineObject) data.get("Range3")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range2")).getId(), ((InterMineObject) data.get("Range4")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range3")).getId(), ((InterMineObject) data.get("Range2")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range3")).getId(), ((InterMineObject) data.get("Range3")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range3")).getId(), ((InterMineObject) data.get("Range4")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range4")).getId(), ((InterMineObject) data.get("Range1")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range4")).getId(), ((InterMineObject) data.get("Range2")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range4")).getId(), ((InterMineObject) data.get("Range3")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range4")).getId(), ((InterMineObject) data.get("Range4")).getId())));
        results.put("RangeDoesNotOverlap", Arrays.asList(
                Arrays.asList(((InterMineObject) data.get("Range1")).getId(), ((InterMineObject) data.get("Range3")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range3")).getId(), ((InterMineObject) data.get("Range1")).getId())));
        results.put("RangeOverlapsValues", Arrays.asList(
                Arrays.asList(((InterMineObject) data.get("Range3")).getId()),
                Arrays.asList(((InterMineObject) data.get("Range4")).getId())));

        results.put("ConstrainClass1", Arrays.asList(
                Arrays.asList(data.get("EmployeeA2")),
                Arrays.asList(data.get("EmployeeA3")),
                Arrays.asList(data.get("EmployeeB2"))));

        results.put("ConstrainClass2", Arrays.asList(
                Arrays.asList(data.get("CompanyB")),
                Arrays.asList(data.get("EmployeeA2")),
                Arrays.asList(data.get("EmployeeA3")),
                Arrays.asList(data.get("EmployeeB2"))));
        results.put("MultipleInBagConstraint1", Arrays.asList(
                Arrays.asList(data.get("EmployeeA1")),
                Arrays.asList(data.get("EmployeeA2")),
                Arrays.asList(data.get("EmployeeB1"))));
    }

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * those expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public abstract void executeTest(String type) throws Exception;

    /*
      select Alias
      from Company AS Alias
      NOT DISTINCT
    */
    public static Query selectSimpleObject() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.setDistinct(false);
        q1.alias(c1, "Alias");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        return q1;
    }

    /*
      select All.Array.name, All.alias as Alias
      from (select Array, 5 as Alias from Company AS Array) as All
    */
    public static Query subQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        Query q1 = new Query();
        q1.alias(c1, "Array");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        q1.alias(v1, "Alias");
        q1.addToSelect(v1);
        Query q2 = new Query();
        q2.alias(q1, "All");
        q2.addFrom(q1);
        QueryField f1 = new QueryField(q1, c1, "name");
        QueryField f2 = new QueryField(q1, v1);
        q2.addToSelect(f1);
        q2.alias(f2, "Alias");
        q2.addToSelect(f2);
        return q2;
    }

    /*
      select name
      from Company
      where vatNumber = 1234
    */
    public static Query whereSimpleEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where vatNumber! = 1234
    */
    public static Query whereSimpleNotEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.NOT_EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where vatNumber! = 1234
    */
    public static Query whereSimpleNegEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        sc1.negate();
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name like "Company%"
    */
    public static Query whereSimpleLike() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name = "CompanyA"
    */
    public static Query whereEqualString() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name LIKE "Company%"
      and vatNumber > 2000
    */
    public static Query whereAndSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select name
      from Company
      where name LIKE "CompanyA%"
      or vatNumber > 2000
    */
    public static Query whereOrSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select name
      from Company
      where not (name LIKE "Company%"
      and vatNumber > 2000)
    */
    public static Query whereNotSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        cs1.negate();
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department
      from Department
      where department.name IN (select name from Department)
      order by Department.name
    */
    public static Query whereSubQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f2 = new QueryField(c2, "name");
        SubqueryConstraint sqc1 = new SubqueryConstraint(f2, ConstraintOp.IN, q1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        q2.addToOrderBy(f2);
        return q2;
    }

    /*
      select company
      from Company
      where company IN (select company from Company where name = "CompanyA")
    */
    public static Query whereSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, ConstraintOp.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(c2, ConstraintOp.IN, q1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select company
      from Company
      where company NOT IN (select company from Company where name = "CompanyA")
    */
    public static Query whereNotSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, ConstraintOp.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(c2, ConstraintOp.NOT_IN, q1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select company
      from Company
      where not company IN (select company from Company where name = "CompanyA")
    */
    public static Query whereNegSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, ConstraintOp.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(c2, ConstraintOp.IN, q1);
        sqc1.negate();
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where c1 = c2
    */
    public static Query whereClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where c1 != c2
    */
    public static Query whereNotClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.NOT_EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where not (c1 = c2)
    */
    public static Query whereNegClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2);
        cc1.negate();
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select department, manager
      from Department, Manager
      where department.manager CONTAINS manager
      and department.name = "DepartmentA1"
    */

    public static Query contains11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department, manager
      from Department, Manager
      where department.manager DOES NOT CONTAIN manager
      and department.name = "DepartmentA1"
    */

    public static Query containsNot11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.DOES_NOT_CONTAIN, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department, manager
      from Department, Manager
      where (not department.manager CONTAINS manager)
      and department.name = "DepartmentA1"
    */

    public static Query containsNeg11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        cc1.negate();
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select company, department
      from Company, Department
      where company.departments contains department
      and company.name = "CompanyA"
    */
    public static Query contains1N() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select company, department
      from Company, Department
      where company.departments DOES NOT contain department
      and company.name = "CompanyA"
    */
    public static Query containsNot1N() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.DOES_NOT_CONTAIN, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department, company
      from Department, company
      where department.company CONTAINS company
      and company.name = "CompanyA"
    */
    public static Query containsN1() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "company");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc2, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.companys contains company
      and contractor.name = "ContractorA"
    */
    public static Query containsMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "companys");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("ContractorA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.oldComs CONTAINS company
    */
    public static Query containsDuplicatesMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "oldComs");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.companys DOES NOT CONTAIN company
      and contractor.name = "ContractorA"
    */
    public static Query containsNotMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "companys");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.DOES_NOT_CONTAIN, qc2);
        QueryValue v1 = new QueryValue("ContractorA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select company, count(*)
      from Company, Department
      where company contains department
      group by company
    */
    public static Query simpleGroupBy() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(new QueryFunction());
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.setConstraint(cc1);
        q1.addToGroupBy(qc1);
        return q1;
    }

    /*
      select company, department, manager, address
      from Company, Department, Manager, Address
      where company contains department
      and department.manager CONTAINS manager
      and manager.address CONTAINS address
      and manager.name = "EmployeeA1"
    */
    public static Query multiJoin() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Manager.class);
        QueryClass qc4 = new QueryClass(Address.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        QueryReference qr2 = new QueryObjectReference(qc2, "manager");
        QueryReference qr3 = new QueryObjectReference(qc3, "address");
        QueryField qf1 = new QueryField(qc3, "name");
        QueryValue qv1 = new QueryValue("EmployeeA1");

        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        q1.addToSelect(qc4);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addFrom(qc4);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2));
        cs1.addConstraint(new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qc3));
        cs1.addConstraint(new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qc4));
        cs1.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, qv1));
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select avg(company.vatNumber) + 20, stddev(company.vatNumber), department.name, department
      from Company, Department
      group by department
    */
    public static Query selectComplex() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        QueryField f3 = new QueryField(c2, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addFrom(c2);
        QueryExpression e1 = new QueryExpression(new QueryFunction(f2, QueryFunction.AVERAGE),
                QueryExpression.ADD, new QueryValue(new Integer(20)));
        QueryFunction e2 = new QueryFunction(f2, QueryFunction.STDDEV);
        q1.addToSelect(e1);
        q1.addToSelect(e2);
        q1.addToSelect(f3);
        q1.addToSelect(c2);
        q1.addToGroupBy(c2);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND ALL EMPLOYEES
      select employee
      from Employee
      order by employee.name
    */
    public static Query selectClassAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employee.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        QueryField f1 = new QueryField(qc1, "name");
        q1.addToOrderBy(f1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS, CONTRACTORS AND ALL EMPLOYEES
      select employable
      from Employable
    */
    public static Query selectInterfaceAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE DEPARTMENTS AND COMPANIES
      select randominterface
      from RandomInterface
    */
    public static Query selectInterfaceAndSubClasses2() throws Exception {
        QueryClass qc1 = new QueryClass(RandomInterface.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND CONTRACTORS
      select ImportantPerson
      from ImportantPerson
    */
    public static Query selectInterfaceAndSubClasses3() throws Exception {
        QueryClass qc1 = new QueryClass(ImportantPerson.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      select company
      from (select company from Company) as subquery, Company
      where company = subquery.company
    */
    /*
     * TODO: this currently cannot be done.
    public static Query selectClassFromSubQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Query q2 = new Query();
        q2.addFrom(q1);
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(new ClassConstraint(c1, ConstraintOp.EQUALS, c2));
        return q2;
    }
    */

    /*
     * select 5 as a2_, Company.name as a3_ from Company
     */
    public static Query orderByAnomaly() throws Exception {
        QueryClass c = new QueryClass(Company.class);
        Query q = new Query();
        q.addFrom(c);
        q.addToSelect(new QueryValue(new Integer(5)));
        q.addToSelect(new QueryField(c, "name"));
        return q;
    }

    /*
     * select Secretary from Company, Secretary where Company.name = 'CompanyA' AND Company.secretarys CONTAINS Secretary
     */
    public static Query selectUnidirectionalCollection() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Secretary.class);
        Query q = new Query();
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc2);
        ConstraintSet qs = new ConstraintSet(ConstraintOp.AND);
        qs.addConstraint(new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("CompanyA")));
        qs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "secretarys"), ConstraintOp.CONTAINS, qc2));
        q.setConstraint(qs);
        return q;
    }

    public static Query emptyAndConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.AND));
        return q;
    }

    public static Query emptyOrConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.OR));
        return q;
    }

    public static Query emptyNandConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.NAND));
        return q;
    }

    public static Query emptyNorConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.NOR));
        return q;
    }

    /*
      select Company
      from Company
      where Company.name in ("hello", "goodbye")
    */
    public static Query bagConstraint() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.alias(c1, "Company");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        HashSet set = new LinkedHashSet();
        set.add("hello");
        set.add("goodbye");
        set.add("CompanyA");
        q1.setConstraint(new BagConstraint(new QueryField(c1, "name"), ConstraintOp.IN, set));
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS, CONTRACTORS AND ALL EMPLOYEES
      select employable
      from Employable where Employable.name = "EmployeeA1"
    */
    public static Query interfaceField() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        q1.setConstraint(new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("EmployeeA1")));
        return q1;
    }

    /*
      select a1_, a1_.debt, a1_.age from (Broke, Employee) as a1_
      where a1_.debt > 0 and a1_.age > 0;

      Checks Attributes, and that they are sourced from the correct table
      Checks that two Interfaces can be combined
    */
    public static Query dynamicInterfacesAttribute() throws Exception {
        Set classes = new HashSet();
        classes.add(Broke.class);
        classes.add(Employee.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryField f1 = new QueryField(qc1, "debt");
        QueryField f2 = new QueryField(qc1, "age");
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.addToSelect(f1);
        q1.addToSelect(f2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(f1, ConstraintOp.GREATER_THAN, new QueryValue(new Integer(0))));
        cs.addConstraint(new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, new QueryValue(new Integer(0))));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_ from (Broke, Employable);

      Checks that a Class can be combined with an Interface
    */
    public static Query dynamicClassInterface() throws Exception {
        Set classes = new HashSet();
        classes.add(Broke.class);
        classes.add(Employable.class);
        QueryClass qc1 = new QueryClass(classes);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        return q1;
    }

    /*
      select a1_, a2_, a3_ from (Department, Broke) as a1_, Company as a2_, Bank as a3_
      where a2_.departments contains a1_ and a3_.debtors contains a1_
    */
    public static Query dynamicClassRef1() throws Exception {
        Set classes = new HashSet();
        classes.add(Department.class);
        classes.add(Broke.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryClass qc3 = new QueryClass(Bank.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc2, "departments"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc3, "debtors"), ConstraintOp.CONTAINS, qc1));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_, a2_, a3_ from (Department, Broke) as a1_, Company as a2_, Bank as a3_
      where a1_.company contains a2_ and a1_.bank contains a3_
    */
    public static Query dynamicClassRef2() throws Exception {
        Set classes = new HashSet();
        classes.add(Department.class);
        classes.add(Broke.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryClass qc3 = new QueryClass(Bank.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "company"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "bank"), ConstraintOp.CONTAINS, qc3));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_, a2_, a3_ from (Company, Bank) as a1_, Department as a2_, Broke as a3_
      where a1_.departments contains a2_ and a1_.debtors contains a3_
    */
    public static Query dynamicClassRef3() throws Exception {
        Set classes = new HashSet();
        classes.add(Company.class);
        classes.add(Bank.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Broke.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "departments"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "debtors"), ConstraintOp.CONTAINS, qc3));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_, a2_, a3_ from (Company, Bank) as a1_, Department as a2_, Broke as a3_
      where a2_.company contains a1_ and a3_.bank contains a1_
    */
    public static Query dynamicClassRef4() throws Exception {
        Set classes = new HashSet();
        classes.add(Company.class);
        classes.add(Bank.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Broke.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc2, "company"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc3, "bank"), ConstraintOp.CONTAINS, qc1));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_ from (Broke, Employable) as a1_, (Broke, HasAddress) as a2_ where a1_ = a2_;
    */
    public static Query dynamicClassConstraint() throws Exception {
        Set classes = new HashSet();
        classes.add(Broke.class);
        classes.add(Employable.class);
        QueryClass qc1 = new QueryClass(classes);
        classes = new HashSet();
        classes.add(Broke.class);
        classes.add(HasAddress.class);
        QueryClass qc2 = new QueryClass(classes);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.setConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2));
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.address IS NULL;
     */
    public static Query containsConstraintNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(new QueryObjectReference(qc, "address"),
                ConstraintOp.IS_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.address IS NOT NULL;
     */
    public static Query containsConstraintNotNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(new QueryObjectReference(qc, "address"),
                ConstraintOp.IS_NOT_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Department AS a1_ WHERE a1_.employees IS NULL;
     * Department.employees is a 1:N collection
     */
    public static Query containsConstraintNullCollection1N() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(
                new QueryCollectionReference(qc, "employees"), ConstraintOp.IS_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Department AS a1_ WHERE a1_.employees IS NOT NULL;
     * Department.employees is a 1:N collection
     */
    public static Query containsConstraintNotNullCollection1N() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(
                new QueryCollectionReference(qc, "employees"), ConstraintOp.IS_NOT_NULL);
        q1.setConstraint(c);
        return q1;
    }


    /*
     * SELECT a1_ FROM Company AS a1_ WHERE a1_.contractors IS NULL;
     * Company.contractors is a many to many collection
     */
    public static Query containsConstraintNullCollectionMN() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(
                new QueryCollectionReference(qc, "contractors"), ConstraintOp.IS_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Company AS a1_ WHERE a1_.contractors IS NOT NULL;
     * Company.contractors is a many to many collection
     */
    public static Query containsConstraintNotNullCollectionMN() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(
                new QueryCollectionReference(qc, "contractors"), ConstraintOp.IS_NOT_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Manager AS a1_ WHERE a1_.title IS NULL;
     */
    public static Query simpleConstraintNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Manager.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        SimpleConstraint c = new SimpleConstraint(new QueryField(qc, "title"), ConstraintOp.IS_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Manager AS a1_ WHERE a1_.title IS NOT NULL;
     */
    public static Query simpleConstraintNotNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Manager.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        SimpleConstraint c = new SimpleConstraint(new QueryField(qc, "title"), ConstraintOp.IS_NOT_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_.age::String from Employee AS a1_;
     */
    public static Query typeCast() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "age");
        QueryCast c = new QueryCast(f, String.class);
        q.addToSelect(c);
        return q;
    }

    /*
     * SELECT indexof(a1_.name, 'oy') from Employee AS a1_;
     */
    public static Query indexOf() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(f, QueryExpression.INDEX_OF, new QueryValue("oy"));
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT substr(a1_.name, 2, 2) AS a2_ FROM Employee AS a1_;
     */
    public static Query substring() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(f, new QueryValue(new Integer(2)), new QueryValue(new Integer(2)));
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT substr(a1_.name, 2) AS a2_ FROM Employee AS a1_;
     */
    public static Query substring2() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(f, QueryExpression.SUBSTRING, new QueryValue(new Integer(2)));
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT lower(a1_.name) AS a2_ FROM Employee AS a1_;
     */
    public static Query lower() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(QueryExpression.LOWER, f);
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT upper(a1_.name) AS a2_ FROM Employee AS a1_;
     */
    public static Query upper() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(QueryExpression.UPPER, f);
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT GREATEST(2000, a1_.vatNumber) AS a2_ FROM Company AS a1_;
     */
    public static Query greatest() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "vatNumber");
        QueryExpression e = new QueryExpression(new QueryValue(2000), QueryExpression.GREATEST, f);
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT LEAST(2000, a1_.vatNumber) AS a2_ FROM Company AS a1_;
     */
    public static Query least() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "vatNumber");
        QueryExpression e = new QueryExpression(new QueryValue(2000), QueryExpression.LEAST, f);
        q.addToSelect(e);
        return q;
    }

    /*
     * select a1_ FROM Employee AS a1_ ORDER BY a1_.department;
     */
    public static Query orderByReference() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToOrderBy(new QueryObjectReference(qc, "department"));
        return q;
    }

    /*
     * SELECT DISTINCT a1_.name FROM Employee AS a1_ ORDER BY a1_.age;
     */
    public static Query failDistinctOrder() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "name"));
        q.addToOrderBy(new QueryField(qc, "age"));
        q.setDistinct(true);
        return q;
    }

    /*
     * SELECT DISTINCT a1_ FROM Employee AS a1_, Department AS a2_ WHERE a1_.department CONTAINS a2_ ORDER BY a2_
     */
    public static Query failDistinctOrder2() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc1);
        q.addToOrderBy(qc2);
        q.setConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "department"), ConstraintOp.CONTAINS, qc2));
        q.setDistinct(true);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.name IN (...)
     */
    public static Query largeBagConstraint(boolean makeNotConstraint) throws Exception {
        ConstraintOp constraintOp;

        if (makeNotConstraint) {
            constraintOp = ConstraintOp.NOT_IN;
        } else {
            constraintOp = ConstraintOp.IN;
        }

        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Set bag = new HashSet();
        bag.add("EmployeeA1");
        bag.add("EmployeeB2");
        for (int i = 0; i < 20000; i++) {
            bag.add("a" + i);
        }
        bag.add("a string with quotes: '\"");

        q.setConstraint(new BagConstraint(new QueryField(qc, "name"), constraintOp, bag));
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS 1_ WHERE a1_.age > -50
     */
    public static Query negativeNumbers() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.GREATER_THAN, new QueryValue(new Integer(-51))));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE EXISTS (SELECT a1_ FROM Company AS a1_)
     */
    public static Query subqueryExistsConstraint() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(new QueryValue("hello"));
        Query q2 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q2.addFrom(qc);
        q2.addToSelect(qc);
        q2.setDistinct(false);
        q1.setConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, q2));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE DOES NOT EXIST (SELECT a1_ FROM Company AS a1_)
     */
    public static Query notSubqueryExistsConstraint() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(new QueryValue("hello"));
        Query q2 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q2.addFrom(qc);
        q2.addToSelect(qc);
        q2.setDistinct(false);
        q1.setConstraint(new SubqueryExistsConstraint(ConstraintOp.DOES_NOT_EXIST, q2));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE EXISTS (SELECT a1_ FROM Bank AS a1_)
     */
    public static Query subqueryExistsConstraintNeg() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(new QueryValue("hello"));
        Query q2 = new Query();
        QueryClass qc = new QueryClass(Bank.class);
        q2.addFrom(qc);
        q2.addToSelect(qc);
        q2.setDistinct(false);
        q1.setConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, q2));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_, a1_.department AS a2_ FROM Employee AS a1_
     */
    public static Query objectPathExpression() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryObjectPathExpression(qc, "department"));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.address AS a2_ FROM Employee AS a1_
     */
    public static Query objectPathExpression2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryObjectPathExpression(qc, "address"));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.department.company AS a2_ FROM Employee AS a1_
     */
    public static Query objectPathExpression3() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope1 = new QueryObjectPathExpression(qc, "department");
        qope1.addToSelect(new QueryObjectPathExpression(qope1.getDefaultClass(), "company"));
        q.addToSelect(qope1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.department.company.address AS a2_ FROM Employee AS a1_
     */
    public static Query objectPathExpression4() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope1 = new QueryObjectPathExpression(qc, "department");
        QueryObjectPathExpression qope2 = new QueryObjectPathExpression(qope1.getDefaultClass(), "company");
        qope2.addToSelect(new QueryObjectPathExpression(qope2.getDefaultClass(), "address"));
        qope1.addToSelect(qope2);
        q.addToSelect(qope1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.department AS a2_, a1_.department.company AS a3_, a1_.department.company.address AS a4_ FROM Employee AS a1_
     */
    public static Query objectPathExpression5() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope1 = new QueryObjectPathExpression(qc, "department");
        QueryObjectPathExpression qope2 = new QueryObjectPathExpression(qope1.getDefaultClass(), "company");
        qope2.addToSelect(qope2.getDefaultClass());
        qope2.addToSelect(new QueryObjectPathExpression(qope2.getDefaultClass(), "address"));
        qope1.addToSelect(qope1.getDefaultClass());
        qope1.addToSelect(new PathExpressionField(qope2, 0));
        qope1.addToSelect(new PathExpressionField(qope2, 1));
        q.addToSelect(new PathExpressionField(qope1, 0));
        q.addToSelect(new PathExpressionField(qope1, 1));
        q.addToSelect(new PathExpressionField(qope1, 2));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.CEO.name(DEF: 'fred') AS a2_ FROM Company AS a1_
     */
    public static Query fieldPathExpression() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope1 = new QueryObjectPathExpression(qc, "CEO");
        qope1.addToSelect(new QueryField(qope1.getDefaultClass(), "name"));
        q.addToSelect(qope1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.department.company.address.address(DEF: 'Nowhere') AS a2_ FROM Employee AS a1_
     */
    public static Query fieldPathExpression2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope1 = new QueryObjectPathExpression(qc, "department");
        QueryObjectPathExpression qope2 = new QueryObjectPathExpression(qope1.getDefaultClass(), "company");
        QueryObjectPathExpression qope3 = new QueryObjectPathExpression(qope2.getDefaultClass(), "address");
        qope3.addToSelect(new QueryField(qope3.getDefaultClass(), "address"));
        qope2.addToSelect(qope3);
        qope1.addToSelect(qope2);
        q.addToSelect(qope1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.employees AS a2_ FROM Department AS a1_
     */
    public static Query collectionPathExpression() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryCollectionPathExpression(qc, "employees"));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.department.employees AS a2_ FROM Employee AS a1_
     */
    public static Query collectionPathExpression2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope1 = new QueryObjectPathExpression(qc, "department");
        qope1.addToSelect(new QueryCollectionPathExpression(qope1.getDefaultClass(), "employees"));
        q.addToSelect(qope1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.departments(SELECT default, default.employees) AS a2_ FROM Company AS a1_
     */
    public static Query collectionPathExpression3() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryCollectionPathExpression qcpe = new QueryCollectionPathExpression(qc, "departments");
        qcpe.addToSelect(qcpe.getDefaultClass());
        qcpe.addToSelect(new QueryCollectionPathExpression(qcpe.getDefaultClass(), "employees"));
        q.addToSelect(qcpe);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.departments(SELECT a1_ FROM Employee AS a1_ WHERE default.employees CONTAINS a1_) AS a2_ FROM Company AS a1_
     */
    public static Query collectionPathExpression4() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryCollectionPathExpression qcpe = new QueryCollectionPathExpression(qc, "departments");
        QueryClass qc2 = new QueryClass(Employee.class);
        qcpe.addFrom(qc2);
        qcpe.addToSelect(qc2);
        qcpe.setSingleton(true);
        qcpe.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcpe.getDefaultClass(), "employees"), ConstraintOp.CONTAINS, qc2));
        q.addToSelect(qcpe);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.departments(WHERE default.name LIKE '%1') AS a2_ FROM Company AS a1_
     */
    public static Query collectionPathExpression5() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryCollectionPathExpression qcpe = new QueryCollectionPathExpression(qc, "departments");
        qcpe.setConstraint(new SimpleConstraint(new QueryField(qcpe.getDefaultClass(), "name"), ConstraintOp.MATCHES, new QueryValue("%1")));
        q.addToSelect(qcpe);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a2_.0 AS a3_, a2_.1 AS a4_ FROM Department PATH a1_.company(SELECT default, default.departments) AS a2_
     */
    public static Query collectionPathExpression6() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryObjectPathExpression qope = new QueryObjectPathExpression(qc, "company");
        qope.addToSelect(qope.getDefaultClass());
        qope.addToSelect(new QueryCollectionPathExpression(qope.getDefaultClass(), "departments"));
        q.addToSelect(new PathExpressionField(qope, 0));
        q.addToSelect(new PathExpressionField(qope, 1));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.department(SELECT default, a1_ FROM Company AS a1_ WHERE default.company CONTAINS a1_) AS a2_ FROM Employee AS a1_
     */
    public static Query collectionPathExpression7() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        QueryCollectionPathExpression qcpe = new QueryCollectionPathExpression(qc1, "department");
        QueryClass qc2 = new QueryClass(Company.class);
        qcpe.addFrom(qc2);
        qcpe.addToSelect(qcpe.getDefaultClass());
        qcpe.addToSelect(qc2);
        qcpe.setConstraint(new ContainsConstraint(new QueryObjectReference(qcpe.getDefaultClass(), "company"), ConstraintOp.CONTAINS, qc2));
        q.addToSelect(qcpe);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM InterMineObject AS a1_ WHERE a1_ IN (SELECT a2_ FROM Company AS a2_) OR a1_ IN (SELECT a3_ FROM Broke AS a3_)
     */
    public static Query orSubquery() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
        q.setConstraint(cs);
        Query q2 = new Query();
        q2.setDistinct(false);
        QueryClass qc2 = new QueryClass(Company.class);
        q2.addFrom(qc2);
        q2.addToSelect(qc2);
        cs.addConstraint(new SubqueryConstraint(qc, ConstraintOp.IN, q2));
        Query q3 = new Query();
        q3.setDistinct(false);
        QueryClass qc3 = new QueryClass(Broke.class);
        q3.addFrom(qc3);
        q3.addToSelect(qc3);
        cs.addConstraint(new SubqueryConstraint(qc, ConstraintOp.IN, q3));
        return q;
    }

    /*
     * SELECT a1_ FROM Types AS a1_ WHERE a1_.doubleType < 1.3432E+4
     */
    public static Query scientificNumber() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Types.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "doubleType"), ConstraintOp.LESS_THAN, new QueryValue(new Double(1.3432E+24))));
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "floatType"), ConstraintOp.GREATER_THAN, new QueryValue(new Float(-8.56E-32))));
        return q;
    }

    /*
     * SELECT a1_ FROM Employee WHERE LOWER(a1_.name) IN ?
     */
    public static Query lowerBag() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        q.setConstraint(new BagConstraint(new QueryExpression(QueryExpression.LOWER, new QueryField(qc, "name")), ConstraintOp.IN, Arrays.asList(new String[]{"employeea1", "employeea2", "employeeb1"})));
        return q;
    }

    /*
     * SELECT BAG(5)
     */
    public static Query fetchBag() throws Exception {
        ObjectStoreBag osb = new ObjectStoreBag(5);
        Query q = new Query();
        q.addToSelect(osb);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT Employee FROM Employee WHERE Employee IN BAG(5)
     */
    public static Query objectStoreBag() throws Exception {
        ObjectStoreBag osb = new ObjectStoreBag(5);
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, osb));
        return q;
    }

    /*
     * SELECT Department.id, Employee FROM Employee, BAG(5)::Department WHERE Department.employees CONTAINS Employee
     */
    public static Query objectStoreBagQueryClass() throws Exception {
        ObjectStoreBag osb = new ObjectStoreBag(5);
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        QueryClassBag qcb = new QueryClassBag(Department.class, osb);
        q.addFrom(qc);
        q.addFrom(qcb);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT Employee FROM Employee ORDER BY Employee DESC
     */
    public static Query orderDescending() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToOrderBy(new OrderDescending(qc));
        return q;
    }

    /*
     * SELECT BAG(5) UNION BAG(6)
     */
    public static Query objectStoreBagCombination() throws Exception {
        Query q = new Query();
        ObjectStoreBag osb1 = new ObjectStoreBag(5);
        ObjectStoreBag osb2 = new ObjectStoreBag(6);
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(ObjectStoreBagCombination.UNION);
        osbc.addBag(osb1);
        osbc.addBag(osb2);
        q.addToSelect(osbc);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT BAG(5) INTERSECT BAG(6)
     */
    public static Query objectStoreBagCombination2() throws Exception {
        Query q = new Query();
        ObjectStoreBag osb1 = new ObjectStoreBag(5);
        ObjectStoreBag osb2 = new ObjectStoreBag(6);
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(ObjectStoreBagCombination.INTERSECT);
        osbc.addBag(osb1);
        osbc.addBag(osb2);
        q.addToSelect(osbc);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT BAGS FOR 999
     */
    public static Query objectStoreBagsForObject() throws Exception {
        Query q = new Query();
        ObjectStoreBagsForObject osbfo = new ObjectStoreBagsForObject(new Integer(999));
        q.addToSelect(osbfo);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT BAGS FOR 999 IN BAGS(10, 11, 12)
     */
    public static Query objectStoreBagsForObject2() throws Exception {
        Query q = new Query();
        Collection bags = new LinkedHashSet();
        bags.add(new ObjectStoreBag(10));
        bags.add(new ObjectStoreBag(11));
        bags.add(new ObjectStoreBag(12));
        ObjectStoreBagsForObject osbfo = new ObjectStoreBagsForObject(new Integer(999), bags);
        q.addToSelect(osbfo);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT Employee.department.id FROM Employee
     */
    public static Query selectForeignKey() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(new QueryForeignKey(qc, "department"));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT Department, COUNT(*) FROM Department, Employee WHERE Department.employees CONTAINS Employee AND COUNT(*) > 1 GROUP BY Department
     */
    public static Query whereCount() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc1);
        QueryFunction count = new QueryFunction();
        q.addToSelect(count);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "employees"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new SimpleConstraint(count, ConstraintOp.GREATER_THAN, new QueryValue(new Long(1))));
        q.addToGroupBy(qc1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT DISTINCT a3_.a2_ FROM (SELECT a1_.name AS a2_ FROM Employee AS a1_ LIMIT 3) AS a3_
     */
    public static Query limitedSubquery() throws Exception {
        Query subQ = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        subQ.addFrom(qc);
        QueryField qf = new QueryField(qc, "name");
        subQ.addToSelect(qf);
        subQ.setDistinct(false);
        subQ.setLimit(3);
        Query q = new Query();
        q.addFrom(subQ);
        q.addToSelect(new QueryField(subQ, qf));
        q.setDistinct(true);
        return q;
    }

    /*
     * SELECT BAG(5) ALLBUTINTERSECT BAG(6)
     */
    public static Query objectStoreBagCombination3() throws Exception {
        Query q = new Query();
        ObjectStoreBag osb1 = new ObjectStoreBag(5);
        ObjectStoreBag osb2 = new ObjectStoreBag(6);
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(ObjectStoreBagCombination.ALLBUTINTERSECT);
        osbc.addBag(osb1);
        osbc.addBag(osb2);
        q.addToSelect(osbc);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.age > 3 AND false
     */
    public static Query totallyFalse() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.GREATER_THAN, new QueryValue(new Integer(3))));
        cs1.addConstraint(new ConstraintSet(ConstraintOp.OR));
        q.setConstraint(cs1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.age > 3 OR true
     */
    public static Query totallyTrue() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        cs1.addConstraint(new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.GREATER_THAN, new QueryValue(new Integer(3))));
        cs1.addConstraint(new ConstraintSet(ConstraintOp.AND));
        q.setConstraint(cs1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.age > 3 OR false
     */
    public static Query mergeFalse() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        cs1.addConstraint(new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.GREATER_THAN, new QueryValue(new Integer(3))));
        cs1.addConstraint(new ConstraintSet(ConstraintOp.OR));
        q.setConstraint(cs1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.age > 3 AND true
     */
    public static Query mergeTrue() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.GREATER_THAN, new QueryValue(new Integer(3))));
        cs1.addConstraint(new ConstraintSet(ConstraintOp.AND));
        q.setConstraint(cs1);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.name IN ()
     */
    public static Query emptyBagConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(new QueryField(qc, "name"), ConstraintOp.IN, Collections.EMPTY_SET));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT MIN(a1_.id) FROM Employee AS a1_
     */
    public static Query selectFunctionNoGroup() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(new QueryFunction(new QueryField(qc, "id"), QueryFunction.MIN));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.class, count(*) from InterMineObject AS a1_ GROUP BY a1_.class
     */
    public static Query selectClassFromInterMineObject() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        QueryField qf = new QueryField(qc, "class");
        q.addToSelect(qf);
        q.addToSelect(new QueryFunction());
        q.addToGroupBy(qf);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.class, count(*) from Employable AS a1_ GROUP BY a1_.class
     */
    public static Query selectClassFromEmployee() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField qf = new QueryField(qc, "class");
        q.addToSelect(qf);
        q.addToSelect(new QueryFunction());
        q.addToGroupBy(qf);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.class, count(*) from BrokeEmployable AS a1_ GROUP BY a1_.class
     */
    public static Query selectClassFromBrokeEmployable() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Broke.class, Employable.class);
        q.addFrom(qc);
        QueryField qf = new QueryField(qc, "class");
        q.addToSelect(qf);
        q.addToSelect(new QueryFunction());
        q.addToGroupBy(qf);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.employees::Manager FROM Department AS a1_
     */
    public static Query subclassCollection() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryCollectionPathExpression(qc, "employees", Manager.class));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.employees::(Broke, Employee) FROM Department AS a1_
     */
    public static Query subclassCollection2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryCollectionPathExpression(qc, "employees", Broke.class, Employee.class));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee WHERE a1_.name = "Fred\Blog's"
     */
    public static Query selectWhereBackslash() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Fred\\Blog's")));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.departments(SELECT default, a1_.0, a1_.1 PATH default.company(SELECT default, default.contractors) AS a1_) AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name
     */
    public static Query multiColumnObjectInCollection() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryCollectionPathExpression qcpe = new QueryCollectionPathExpression(qc, "departments");
        qcpe.addToSelect(qcpe.getDefaultClass());
        QueryObjectPathExpression qope = new QueryObjectPathExpression(qcpe.getDefaultClass(), "company");
        qope.addToSelect(qope.getDefaultClass());
        qope.addToSelect(new QueryCollectionPathExpression(qope.getDefaultClass(), "contractors"));
        qcpe.addToSelect(new PathExpressionField(qope, 0));
        qcpe.addToSelect(new PathExpressionField(qope, 1));
        q.addToSelect(qcpe);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a2_ FROM Range AS a1_, Range AS a2_ WHERE RANGE(a1_.rangeStart, a1_.rangeEnd, a1_.parent) OVERLAPS RANGE(a2_.rangeStart, a2_.rangeEnd, a2_.parent)
     */
    public static Query rangeOverlaps() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Range.class);
        QueryClass qc2 = new QueryClass(Range.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(new QueryField(qc1, "id"));
        q.addToSelect(new QueryField(qc2, "id"));
        OverlapRange r1 = new OverlapRange(new QueryField(qc1, "rangeStart"), new QueryField(qc1, "rangeEnd"), new QueryObjectReference(qc1, "parent"));
        OverlapRange r2 = new OverlapRange(new QueryField(qc2, "rangeStart"), new QueryField(qc2, "rangeEnd"), new QueryObjectReference(qc2, "parent"));
        q.setConstraint(new OverlapConstraint(r1, ConstraintOp.OVERLAPS, r2));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a2_ FROM Range AS a1_, Range AS a2_ WHERE RANGE(a1_.rangeStart, a1_.rangeEnd, a1_.parent) DOES NOT OVERLAP RANGE(a2_.rangeStart, a2_.rangeEnd, a2_.parent)
     */
    public static Query rangeDoesNotOverlap() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Range.class);
        QueryClass qc2 = new QueryClass(Range.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(new QueryField(qc1, "id"));
        q.addToSelect(new QueryField(qc2, "id"));
        OverlapRange r1 = new OverlapRange(new QueryField(qc1, "rangeStart"), new QueryField(qc1, "rangeEnd"), new QueryObjectReference(qc1, "parent"));
        OverlapRange r2 = new OverlapRange(new QueryField(qc2, "rangeStart"), new QueryField(qc2, "rangeEnd"), new QueryObjectReference(qc2, "parent"));
        q.setConstraint(new OverlapConstraint(r1, ConstraintOp.DOES_NOT_OVERLAP, r2));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a2_ FROM Range AS a1_, Range AS a2_ WHERE RANGE(a1_.rangeStart, a1_.rangeEnd, a1_.parent) OVERLAPS RANGE(1000, 2000, a2_.parent)
     */
    public static Query rangeOverlapsValues() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Range.class);
        q.addFrom(qc1);
        q.addToSelect(new QueryField(qc1, "id"));
        OverlapRange r1 = new OverlapRange(new QueryField(qc1, "rangeStart"), new QueryField(qc1, "rangeEnd"), new QueryObjectReference(qc1, "parent"));
        OverlapRange r2 = new OverlapRange(new QueryValue(35), new QueryValue(45), new QueryObjectReference(qc1, "parent"));
        q.setConstraint(new OverlapConstraint(r1, ConstraintOp.OVERLAPS, r2));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM InterMineObject WHERE a1_.class = Employee.class
     */
    public static Query constrainClass1() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(InterMineObject.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        QueryField qf = new QueryField(qc1, "class");
        q.setConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(Employee.class)));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM InterMineObject WHERE a1_.class IN (Employee.class, Company.class)
     */
    public static Query constrainClass2() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(InterMineObject.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        QueryField qf = new QueryField(qc1, "class");
        q.setConstraint(new BagConstraint(qf, ConstraintOp.IN, Arrays.asList(Employee.class, Company.class)));
        q.setDistinct(false);
        return q;
    }

    public static Query multipleInBagConstraint1() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        List<QueryField> fields = new ArrayList<QueryField>();
        fields.add(new QueryField(qc1, "end"));
        fields.add(new QueryField(qc1, "name"));
        Collection<String> bag = Arrays.asList("1", "2", "EmployeeA1", "EmployeeB1");
        q.setConstraint(new MultipleInBagConstraint(bag, fields));
        q.setDistinct(false);
        return q;
    }

    /*
      select company,
      from Company
      where c1 = <company object>
    */
    public static Query whereClassObject() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Company obj = (Company) data.get("CompanyA");
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select company,
      from Company, Department
      where c1 = <company object>
      and Company.departments = Department
      and Department CONTAINS (select department
                               from Department
                               where department = <department object>)
    */
    public static Query selectClassObjectSubquery() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        Company obj1 = (Company) data.get("CompanyA");
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, obj1);
        cs1.addConstraint(cc1);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint con1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        cs1.addConstraint(con1);

        Query subquery = new Query();
        QueryClass qc3 = new QueryClass(Department.class);
        Department obj2 = (Department) data.get("DepartmentA1");
        ClassConstraint cc2 = new ClassConstraint(qc3, ConstraintOp.EQUALS, obj2);
        subquery.addFrom(qc3);
        subquery.addToSelect(qc3);
        subquery.setConstraint(cc2);
        SubqueryConstraint sc1 = new SubqueryConstraint(qc2, ConstraintOp.IN, subquery);
        cs1.addConstraint(sc1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select Company
      from Company
      where Company in ("hello", "goodbye")
    */
    public static Query bagConstraint2() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.alias(c1, "Company");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Set set = new LinkedHashSet();
        set.add(data.get("CompanyA"));
        q1.setConstraint(new BagConstraint(c1, ConstraintOp.IN, set));
        return q1;
    }

    /*
     * select HasAddress from HasAddress, Address where HasAddress.address CONTAINS Address AND Address = <address>
     */
    public static Query interfaceReference() throws Exception {
        QueryClass qc1 = new QueryClass(HasAddress.class);
        QueryClass qc2 = new QueryClass(Address.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "address"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new ClassConstraint(qc2, ConstraintOp.EQUALS, (Address) data.get("Employee Street, AVille")));
        q1.setConstraint(cs);
        return q1;
    }

    /*
     * select HasSecretarys from HasSecretarys, Secretary where HasSecretarys.secretarys CONTAINS Secretary AND Secretary = <secretary>
     */
    public static Query interfaceCollection() throws Exception {
        QueryClass qc1 = new QueryClass(HasSecretarys.class);
        QueryClass qc2 = new QueryClass(Secretary.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "secretarys"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new ClassConstraint(qc2, ConstraintOp.EQUALS, (Secretary) data.get("Secretary1")));
        q1.setConstraint(cs);
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.department CONTAINS <deptA>
     */
    public static Query containsConstraintObjectRefObject() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "department"),
                ConstraintOp.CONTAINS, (InterMineObject) data.get("DepartmentA1")));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.department DOES NOT CONTAIN <deptA>
     */
    public static Query containsConstraintNotObjectRefObject() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "department"),
                ConstraintOp.DOES_NOT_CONTAIN, (InterMineObject) data.get("DepartmentA1")));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Department AS a1_ WHERE a1_.employees CONTAINS <empB1>
     */
    public static Query containsConstraintCollectionRefObject() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryCollectionReference(qc, "employees"),
                ConstraintOp.CONTAINS, (InterMineObject) data.get("EmployeeB1")));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Department AS a1_ WHERE a1_.employees DOES NOT CONTAIN <empB1>
     */
    public static Query containsConstraintNotCollectionRefObject() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryCollectionReference(qc, "employees"),
                ConstraintOp.DOES_NOT_CONTAIN, (InterMineObject) data.get("EmployeeB1")));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Company AS a1_ WHERE a1_.contractors CONTAINS <cont1>
     */
    public static Query containsConstraintMMCollectionRefObject() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryCollectionReference(qc, "contractors"),
                ConstraintOp.CONTAINS, (InterMineObject) data.get("ContractorA")));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Company AS a1_ WHERE a1_.contractors DOES NOT CONTAIN <cont1>
     */
    public static Query containsConstraintNotMMCollectionRefObject() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryCollectionReference(qc, "contractors"),
                ConstraintOp.DOES_NOT_CONTAIN, (InterMineObject) data.get("ContractorA")));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE <deptA1>.employees CONTAINS a1_
     */
    public static Query collectionQueryOneMany() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryCollectionReference((InterMineObject) data.get("DepartmentA1"), "employees"), ConstraintOp.CONTAINS, qc));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_ FROM Secretary AS a1_ WHERE <CompanyB>.secretarys CONTAINS a1_
     */
    public static Query collectionQueryManyMany() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Secretary.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        q1.setConstraint(new ContainsConstraint(new QueryCollectionReference((InterMineObject) data.get("CompanyB"), "secretarys"), ConstraintOp.CONTAINS, qc));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::Department AS a1_, Employee AS a2_ WHERE a1_.employees CONTAINS a2_
     */
    public static Query queryClassBag() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::HasSecretarys AS a1_, Secretary AS a2_ WHERE a1_.secretarys CONTAINS a2_
     */
    public static Query queryClassBagMM() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(HasSecretarys.class, Arrays.asList(new Object[]{data.get("CompanyA"), data.get("CompanyB"), data.get("EmployeeB1")}));
        QueryClass qc = new QueryClass(Secretary.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "secretarys"), ConstraintOp.CONTAINS, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::Department AS a1_, Employee AS a2_ WHERE a1_.employees DOES NOT CONTAIN a2_
     */
    public static Query queryClassBagNot() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.DOES_NOT_CONTAIN, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::HasSecretarys AS a1_, Secretary AS a2_ WHERE a1_.secretarys DOES NOT CONTAIN a2_
     */
    public static Query queryClassBagNotMM() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(HasSecretarys.class, Arrays.asList(new Object[]{data.get("CompanyA"), data.get("CompanyB"), data.get("EmployeeB1")}));
        QueryClass qc = new QueryClass(Secretary.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "secretarys"), ConstraintOp.DOES_NOT_CONTAIN, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::(CEO, Broke) AS a1_, Secretary AS a2_ WHERE a1_.secretarys CONTAINS a2_
     */
    public static Query queryClassBagDynamic() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(new HashSet(Arrays.asList(new Class[]{CEO.class, Broke.class})), Collections.singletonList(data.get("EmployeeB1")));
        QueryClass qc = new QueryClass(Secretary.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "secretarys"), ConstraintOp.CONTAINS, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM (Broke, Employable) AS a1_ WHERE a1_ IN ?
     */
    /* See ticket #469
    public static Query dynamicBagConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(new HashSet(Arrays.asList(new Class[] {Broke.class, Employable.class})));
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, new HashSet(Arrays.asList(new Object[] {data.get("EmployeeA1"), data.get("CompanyA"), new Integer(5), data.get("EmployeeB1")}))));
        q.setDistinct(false);
        return q;
    }*/

    /*
     * SELECT a1_ FROM (Broke, CEO) AS a1_ WHERE a1_ IN ?
     */
    public static Query dynamicBagConstraint2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(new HashSet(Arrays.asList(new Class[]{Broke.class, CEO.class})));
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, new HashSet(Arrays.asList(new Object[]{data.get("EmployeeB1")}))));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_, a3_ FROM ?::Department AS a1_, Employee AS a2_, Employee AS a3_ WHERE a1_.employees CONTAINS a2_ AND a1_.employee CONTAINS a3_
     */
    public static Query queryClassBagDouble() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        q.addFrom(qcb);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc1);
        q.addToSelect(qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc2));
        q.setConstraint(cs);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id FROM ?::Department AS a1_ WHERE a1_.employees CONTAINS ?
     */
    public static Query queryClassBagContainsObject() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        q.addFrom(qcb);
        q.addToSelect(new QueryField(qcb));
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, (Employee) data.get("EmployeeA1")));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id FROM ?::Department AS a1_ WHERE a1_.employees CONTAINS ? AND a1_.employees CONTAINS ?
     */
    public static Query queryClassBagContainsObjectDouble() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        q.addFrom(qcb);
        q.addToSelect(new QueryField(qcb));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, (Employee) data.get("EmployeeA1")));
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, (Employee) data.get("EmployeeA2")));
        q.setConstraint(cs);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id FROM ?::Department AS a1_ WHERE a1_.employees DOES NOT CONTAIN ?
     */
    public static Query queryClassBagNotContainsObject() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        q.addFrom(qcb);
        q.addToSelect(new QueryField(qcb));
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.DOES_NOT_CONTAIN, (Employee) data.get("EmployeeA1")));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE ?.employees CONTAINS ?
     */
    public static Query objectContainsObject() throws Exception {
        Query q = new Query();
        q.addToSelect(new QueryValue("hello"));
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference((InterMineObject) data.get("DepartmentA1"), "employees"), ConstraintOp.CONTAINS, (InterMineObject) data.get("EmployeeA1")));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE ?.employees CONTAINS ?
     */
    public static Query objectContainsObject2() throws Exception {
        Query q = new Query();
        q.addToSelect(new QueryValue("hello"));
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference((InterMineObject) data.get("DepartmentA1"), "employees"), ConstraintOp.CONTAINS, (InterMineObject) data.get("EmployeeB1")));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE ?.employees DOES NOT CONTAIN ?
     */
    public static Query objectNotContainsObject() throws Exception {
        Query q = new Query();
        q.addToSelect(new QueryValue("hello"));
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference((InterMineObject) data.get("DepartmentA1"), "employees"), ConstraintOp.DOES_NOT_CONTAIN, (InterMineObject) data.get("EmployeeA1")));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::Department AS a1_, Employee AS a2_ WHERE NOT (a1_.employees CONTAINS a2_ AND 1 = 1)
     */
    public static Query queryClassBagNotViaNand() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.NAND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc));
        cs.addConstraint(new SimpleConstraint(new QueryValue(new Integer(1)), ConstraintOp.EQUALS, new QueryValue(new Integer(1))));
        q.setConstraint(cs);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_ FROM ?::Department AS a1_, Employee AS a2_ WHERE NOT (a1_.employees CONTAINS a2_ OR 1 = 1)
     */
    public static Query queryClassBagNotViaNor() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[]{data.get("DepartmentA1"), data.get("DepartmentB1")}));
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qcb);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.NOR);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc));
        cs.addConstraint(new SimpleConstraint(new QueryValue(new Integer(1)), ConstraintOp.EQUALS, new QueryValue(new Integer(1))));
        q.setConstraint(cs);
        q.setDistinct(false);
        return q;
    }
}
