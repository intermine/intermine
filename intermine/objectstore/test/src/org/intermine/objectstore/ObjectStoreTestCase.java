package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.HasSecretarys;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Range;
import org.intermine.model.testmodel.Secretary;
import org.intermine.model.testmodel.SimpleObject;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.proxy.Lazy;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.iql.IqlQueryParser;
import org.intermine.util.DynamicUtil;

/**
 * TestCase for all ObjectStores
 *
 */
public abstract class ObjectStoreTestCase extends StoreDataTestCase
{
    protected static ObjectStore os;

    public ObjectStoreTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
        setUpResults();
    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public static void setUpResults() throws Exception {
        Object[][] r;

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("SelectSimpleObject", toList(r));

        r = new Object[][] { { "CompanyA", new Integer(5) },
                             { "CompanyB", new Integer(5) } };
        results.put("SubQuery", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereSimpleEquals", toList(r));

        r = new Object[][] { { "CompanyB" } };
        results.put("WhereSimpleNotEquals", toList(r));
        results.put("WhereSimpleNegEquals", toList(r));

        r = new Object[][] { { "CompanyA" },
                             { "CompanyB" } };
        results.put("WhereSimpleLike", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereEqualsString", toList(r));

        r = new Object[][] { { "CompanyB" } };
        results.put("WhereAndSet", toList(r));

        r = new Object[][] { { "CompanyA" },
                             { "CompanyB" } };
        results.put("WhereOrSet", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereNotSet", toList(r));

        r = new Object[][] { { data.get("DepartmentA1") },
                             { data.get("DepartmentB1") },
                             { data.get("DepartmentB2") } };
        results.put("WhereSubQueryField", toList(r));

        r = new Object[][] { { data.get("CompanyA") } };
        results.put("WhereSubQueryClass", toList(r));

        r = new Object[][] { { data.get("CompanyB") } };
        results.put("WhereNotSubQueryClass", toList(r));

        r = new Object[][] { { data.get("CompanyB") } };
        results.put("WhereNegSubQueryClass", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyA") },
                             { data.get("CompanyB"), data.get("CompanyB") } };
        results.put("WhereClassClass", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyB") },
                             { data.get("CompanyB"), data.get("CompanyA") } };
        results.put("WhereNotClassClass", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyB") },
                             { data.get("CompanyB"), data.get("CompanyA") } };
        results.put("WhereNegClassClass", toList(r));

        r = new Object[][] { { data.get("CompanyA") } };
        results.put("WhereClassObject", toList(r));

        r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeA1") } };
        results.put("Contains11", toList(r));

        r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeB1") },
                             { data.get("DepartmentA1"), data.get("EmployeeB3") } };
        results.put("ContainsNot11", toList(r));
        results.put("ContainsNeg11", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1") } };
        results.put("Contains1N", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentB1") },
                             { data.get("CompanyA"), data.get("DepartmentB2") } };
        results.put("ContainsNot1N", toList(r));

        r = new Object[][] { { data.get("DepartmentA1"), data.get("CompanyA") } };
        results.put("ContainsN1", toList(r));

        r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
                             { data.get("ContractorA"), data.get("CompanyB") } };
        results.put("ContainsMN", toList(r));

        r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
                             { data.get("ContractorA"), data.get("CompanyB") },
                             { data.get("ContractorB"), data.get("CompanyA") },
                             { data.get("ContractorB"), data.get("CompanyB") } };
        results.put("ContainsDuplicatesMN", toList(r));

        results.put("ContainsNotMN", NO_RESULT); //TODO: Fix this (ticket #445)
        //results.put("ContainsNotMN", Collections.EMPTY_LIST);

        r = new Object[][] { { data.get("CompanyA"), new Long(1) },
                             { data.get("CompanyB"), new Long(2) } };
        results.put("SimpleGroupBy", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1"), ((Employee)data.get("EmployeeA1")).getAddress() } };
        results.put("MultiJoin", toList(r));

        r = new Object[][] { { new BigDecimal("3476.0000000000000000"), new BigDecimal("3142.382535593017"), "DepartmentA1", data.get("DepartmentA1") },
                             { new BigDecimal("3476.0000000000000000"), new BigDecimal("3142.382535593017"), "DepartmentB1", data.get("DepartmentB1") },
                             { new BigDecimal("3476.0000000000000000"), new BigDecimal("3142.382535593017"), "DepartmentB2", data.get("DepartmentB2") } };
        results.put("SelectComplex", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("SelectClassAndSubClasses", toList(r));

        r = new Object[][] { { data.get("ContractorA") },
                             { data.get("ContractorB") },
                             { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("SelectInterfaceAndSubClasses", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") },
                             { data.get("DepartmentA1") },
                             { data.get("DepartmentB1") },
                             { data.get("DepartmentB2") } };
        results.put("SelectInterfaceAndSubClasses2", toList(r));

        r = new Object[][] { { data.get("ContractorA") },
                             { data.get("ContractorB") },
                             { data.get("EmployeeA1") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB3") } };
        results.put("SelectInterfaceAndSubClasses3", toList(r));

        r = new Object[][] { { new Integer(5), "CompanyA" },
                             { new Integer(5), "CompanyB" } };
        results.put("OrderByAnomaly", toList(r));

        r = new Object[][] { { data.get("Secretary1") },
                             { data.get("Secretary2") },
                             { data.get("Secretary3") } };
        results.put("SelectUnidirectionalCollection", toList(r));

        r = new Object[][] { { data.get("CompanyA") } };
        results.put("SelectClassObjectSubquery", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("EmptyAndConstraintSet", toList(r));

        results.put("EmptyOrConstraintSet", Collections.EMPTY_LIST);

        results.put("EmptyNandConstraintSet", Collections.EMPTY_LIST);

        results.put("EmptyNorConstraintSet", toList(r));

        results.put("BagConstraint", Collections.singletonList(Collections.singletonList(data.get("CompanyA"))));

        results.put("BagConstraint2", Collections.singletonList(Collections.singletonList(data.get("CompanyA"))));

        r = new Object[][] { { data.get("EmployeeA1") } };
        results.put("InterfaceField", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") } };
        results.put("InterfaceReference", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("InterfaceCollection", toList(r));

        r = new Object[][] { { data.get("EmployeeB1") } };
        results.put("ContainsConstraintNull", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("ContainsConstraintNotNull", toList(r));

        results.put("ContainsConstraintNullCollection1N", Collections.EMPTY_LIST);

        r = new Object[][] { { data.get("DepartmentA1") },
                { data.get("DepartmentB1") },
                { data.get("DepartmentB2") } };
        results.put("ContainsConstraintNotNullCollection1N", toList(r));

        results.put("ContainsConstraintNullCollectionMN", Collections.EMPTY_LIST);

        r = new Object[][] { { data.get("CompanyA") },
                { data.get("CompanyB") } };
        results.put("ContainsConstraintNotNullCollectionMN", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") } };
        results.put("ContainsConstraintObjectRefObject", toList(r));

        r = new Object[][] { { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("ContainsConstraintNotObjectRefObject", toList(r));

        r = new Object[][] { { data.get("DepartmentB1") } };
        results.put("ContainsConstraintCollectionRefObject", toList(r));

        r = new Object[][] { { data.get("DepartmentA1") },
                             { data.get("DepartmentB2") } };
        results.put("ContainsConstraintNotCollectionRefObject", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("ContainsConstraintMMCollectionRefObject", toList(r));

        results.put("ContainsConstraintNotMMCollectionRefObject", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)")); //TODO: Fix this (ticket #445)
        //results.put("ContainsConstraintNotMMCollectionRefObject", Collections.EMPTY_LIST);

        r = new Object[][] { { data.get("EmployeeA1") }};
        results.put("SimpleConstraintNull", toList(r));

        r = new Object[][] { { data.get("EmployeeB1") },
                             { data.get("EmployeeB3") } };
        results.put("SimpleConstraintNotNull", toList(r));

        r = new Object[][] { { "10" },
            { "20" },
            { "30" },
            { "40" },
            { "50" },
            { "60" } };
        results.put("TypeCast", toList(r));

        r = new Object[][] { { new Integer(5) },
            { new Integer(5) },
            { new Integer(5) },
            { new Integer(5) },
            { new Integer(5) },
            { new Integer(5) } };
        results.put("IndexOf", toList(r));

        r = new Object[][] { { "mp" },
            { "mp" },
            { "mp" },
            { "mp" },
            { "mp" },
            { "mp" } };
        results.put("Substring", toList(r));

        r = new Object[][] { { "mployeeA1" },
            { "mployeeA2" },
            { "mployeeA3" },
            { "mployeeB1" },
            { "mployeeB2" },
            { "mployeeB3" } };
        results.put("Substring2", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("OrderByReference", toList(r));

        results.put("FailDistinctOrder", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));
        results.put("FailDistinctOrder2", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeB2") } };
        results.put("LargeBagConstraint", NO_RESULT);
        results.put("LargeBagConstraintUsingTable", toList(r));

        r = new Object[][] { { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB3") } };
        results.put("LargeBagNotConstraint", NO_RESULT);
        results.put("LargeBagNotConstraintUsingTable", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("NegativeNumbers", toList(r));

        r = new Object[][] { { "employeea1" },
                             { "employeea2" },
                             { "employeea3" },
                             { "employeeb1" },
                             { "employeeb2" },
                             { "employeeb3" } };
        results.put("Lower", toList(r));

        r = new Object[][] { { "EMPLOYEEA1" },
                             { "EMPLOYEEA2" },
                             { "EMPLOYEEA3" },
                             { "EMPLOYEEB1" },
                             { "EMPLOYEEB2" },
                             { "EMPLOYEEB3" } };
        results.put("Upper", toList(r));

        r = new Object[][] { { 2000 },
                { 5678 } };
        results.put("Greatest", toList(r));

        r = new Object[][] { { 1234 },
                { 2000 } };
        results.put("Least", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") } };
        results.put("CollectionQueryOneMany", toList(r));

        r = new Object[][] { { data.get("Secretary1") },
                             { data.get("Secretary2") } };
        results.put("CollectionQueryManyMany", toList(r));

        r = new Object[][] { { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB1") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB2") } };
        results.put("QueryClassBag", toList(r));

        r = new Object[][] { { ((HasSecretarys) data.get("CompanyA")).getId(), data.get("Secretary1") },
                             { ((HasSecretarys) data.get("CompanyA")).getId(), data.get("Secretary2") },
                             { ((HasSecretarys) data.get("CompanyA")).getId(), data.get("Secretary3") },
                             { ((HasSecretarys) data.get("CompanyB")).getId(), data.get("Secretary1") },
                             { ((HasSecretarys) data.get("CompanyB")).getId(), data.get("Secretary2") } };
        results.put("QueryClassBagMM", toList(r));

        r = new Object[][] { { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeB1") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeB2") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeB3") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeA1") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeA2") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeA3") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB3") } };
        results.put("QueryClassBagNot", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));

        results.put("QueryClassBagNotMM", new Failure(RuntimeException.class, "ObjectStore error has occurred (in get)"));

        r = new Object[][] { { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1"), data.get("EmployeeA1") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1"), data.get("EmployeeA2") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA1"), data.get("EmployeeA3") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2"), data.get("EmployeeA1") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2"), data.get("EmployeeA2") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA2"), data.get("EmployeeA3") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3"), data.get("EmployeeA1") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3"), data.get("EmployeeA2") },
                             { ((Department) data.get("DepartmentA1")).getId(), data.get("EmployeeA3"), data.get("EmployeeA3") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB1"), data.get("EmployeeB1") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB1"), data.get("EmployeeB2") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB2"), data.get("EmployeeB1") },
                             { ((Department) data.get("DepartmentB1")).getId(), data.get("EmployeeB2"), data.get("EmployeeB2") } };
        results.put("QueryClassBagDouble", toList(r));
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

        r = new Object[][] { { data.get("EmployeeA1"), data.get("DepartmentA1") },
                             { data.get("EmployeeA2"), data.get("DepartmentA1") },
                             { data.get("EmployeeA3"), data.get("DepartmentA1") },
                             { data.get("EmployeeB1"), data.get("DepartmentB1") },
                             { data.get("EmployeeB2"), data.get("DepartmentB1") },
                             { data.get("EmployeeB3"), data.get("DepartmentB2") } };
        results.put("ObjectPathExpression", toList(r));
        r = new Object[][] { { data.get("EmployeeA1"), data.get("Employee Street, AVille") },
                             { data.get("EmployeeA2"), data.get("Employee Street, AVille") },
                             { data.get("EmployeeA3"), data.get("Employee Street, AVille") },
                             { data.get("EmployeeB1"), null },
                             { data.get("EmployeeB2"), data.get("Employee Street, BVille") },
                             { data.get("EmployeeB3"), data.get("Employee Street, BVille") } };
        results.put("ObjectPathExpression2", toList(r));
        r = new Object[][] { { data.get("EmployeeA1"), data.get("CompanyA") },
                             { data.get("EmployeeA2"), data.get("CompanyA") },
                             { data.get("EmployeeA3"), data.get("CompanyA") },
                             { data.get("EmployeeB1"), data.get("CompanyB") },
                             { data.get("EmployeeB2"), data.get("CompanyB") },
                             { data.get("EmployeeB3"), data.get("CompanyB") } };
        results.put("ObjectPathExpression3", toList(r));
        r = new Object[][] { { data.get("EmployeeA1"), data.get("Company Street, AVille") },
                             { data.get("EmployeeA2"), data.get("Company Street, AVille") },
                             { data.get("EmployeeA3"), data.get("Company Street, AVille") },
                             { data.get("EmployeeB1"), data.get("Company Street, BVille") },
                             { data.get("EmployeeB2"), data.get("Company Street, BVille") },
                             { data.get("EmployeeB3"), data.get("Company Street, BVille") } };
        results.put("ObjectPathExpression4", toList(r));
        r = new Object[][] { { data.get("EmployeeA1"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("Company Street, AVille") },
                             { data.get("EmployeeA2"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("Company Street, AVille") },
                             { data.get("EmployeeA3"), data.get("DepartmentA1"), data.get("CompanyA"), data.get("Company Street, AVille") },
                             { data.get("EmployeeB1"), data.get("DepartmentB1"), data.get("CompanyB"), data.get("Company Street, BVille") },
                             { data.get("EmployeeB2"), data.get("DepartmentB1"), data.get("CompanyB"), data.get("Company Street, BVille") },
                             { data.get("EmployeeB3"), data.get("DepartmentB2"), data.get("CompanyB"), data.get("Company Street, BVille") } };
        results.put("ObjectPathExpression5", toList(r));
        r = new Object[][] { { data.get("CompanyA"), null},
                             { data.get("CompanyB"), "EmployeeB1"} };
        results.put("FieldPathExpression", toList(r));
        r = new Object[][] { { data.get("EmployeeA1"), "Company Street, AVille" },
                             { data.get("EmployeeA2"), "Company Street, AVille" },
                             { data.get("EmployeeA3"), "Company Street, AVille" },
                             { data.get("EmployeeB1"), "Company Street, BVille" },
                             { data.get("EmployeeB2"), "Company Street, BVille" },
                             { data.get("EmployeeB3"), "Company Street, BVille" } };
        results.put("FieldPathExpression2", toList(r));
        r = new Object[][] { { data.get("DepartmentA1"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3")) },
                             { data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2")) },
                             { data.get("DepartmentB2"), Collections.singletonList(data.get("EmployeeB3")) } };
        results.put("CollectionPathExpression", toList(r));
        r = new Object[][] { { data.get("EmployeeA1"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3")) },
                             { data.get("EmployeeA2"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3")) },
                             { data.get("EmployeeA3"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3")) },
                             { data.get("EmployeeB1"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2")) },
                             { data.get("EmployeeB2"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2")) },
                             { data.get("EmployeeB3"), Collections.singletonList(data.get("EmployeeB3")) } };
        results.put("CollectionPathExpression2", toList(r));
        r = new Object[][] { { data.get("CompanyA"), Collections.singletonList(Arrays.asList(data.get("DepartmentA1"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3")))) },
                             { data.get("CompanyB"), Arrays.asList(Arrays.asList(data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"))), Arrays.asList(data.get("DepartmentB2"), Collections.singletonList(data.get("EmployeeB3")))) } };
        results.put("CollectionPathExpression3", toList(r));
        r = new Object[][] { { data.get("CompanyA"), Arrays.asList(data.get("EmployeeA1"), data.get("EmployeeA2"), data.get("EmployeeA3")) },
                             { data.get("CompanyB"), Arrays.asList(data.get("EmployeeB1"), data.get("EmployeeB2"), data.get("EmployeeB3")) } };
        results.put("CollectionPathExpression4", toList(r));
        r = new Object[][] { { data.get("CompanyA"), Collections.singletonList(data.get("DepartmentA1")) },
                             { data.get("CompanyB"), Collections.singletonList(data.get("DepartmentB1")) } };
        results.put("CollectionPathExpression5", toList(r));
        r = new Object[][] { { data.get("DepartmentA1"), data.get("CompanyA"), Arrays.asList(data.get("DepartmentA1")) },
                             { data.get("DepartmentB1"), data.get("CompanyB"), Arrays.asList(data.get("DepartmentB1"), data.get("DepartmentB2")) },
                             { data.get("DepartmentB2"), data.get("CompanyB"), Arrays.asList(data.get("DepartmentB1"), data.get("DepartmentB2")) } };
        results.put("CollectionPathExpression6", toList(r));
        results.put("CollectionPathExpression7", Arrays.asList(
                    Arrays.asList(data.get("EmployeeA1"), Arrays.asList(Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA")))),
                    Arrays.asList(data.get("EmployeeA2"), Arrays.asList(Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA")))),
                    Arrays.asList(data.get("EmployeeA3"), Arrays.asList(Arrays.asList(data.get("DepartmentA1"), data.get("CompanyA")))),
                    Arrays.asList(data.get("EmployeeB1"), Arrays.asList(Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB")))),
                    Arrays.asList(data.get("EmployeeB2"), Arrays.asList(Arrays.asList(data.get("DepartmentB1"), data.get("CompanyB")))),
                    Arrays.asList(data.get("EmployeeB3"), Arrays.asList(Arrays.asList(data.get("DepartmentB2"), data.get("CompanyB"))))));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") },
                             { data.get("EmployeeA1") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB3") } };
        results.put("OrSubquery", toList(r));

        r = new Object[][] { { data.get("Types1") } };
        results.put("ScientificNumber", toList(r));

        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeB1") } };
        results.put("LowerBag", toList(r));
        results.put("FetchBag", Collections.EMPTY_LIST);
        results.put("ObjectStoreBag", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagQueryClass", Collections.EMPTY_LIST);
        r = new Object[][] { { data.get("EmployeeB3") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA1") } };
        results.put("OrderDescending", toList(r));
        results.put("ObjectStoreBagCombination", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagCombination2", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagsForObject", Collections.EMPTY_LIST);
        results.put("ObjectStoreBagsForObject2", Collections.EMPTY_LIST);

        r = new Object[][] { { ((Employee) data.get("EmployeeA1")).getDepartment().getId() },
                             { ((Employee) data.get("EmployeeA2")).getDepartment().getId() },
                             { ((Employee) data.get("EmployeeA3")).getDepartment().getId() },
                             { ((Employee) data.get("EmployeeB1")).getDepartment().getId() },
                             { ((Employee) data.get("EmployeeB2")).getDepartment().getId() },
                             { ((Employee) data.get("EmployeeB3")).getDepartment().getId() } };
        results.put("SelectForeignKey", toList(r));
        r = new Object[][] { { data.get("DepartmentA1"), new Long(3) },
                             { data.get("DepartmentB1"), new Long(2) } };
        results.put("WhereCount", toList(r));
        //r = new Object[][] { { "EmployeeA1" },
        //                     { "EmployeeA2" },
        //                     { "EmployeeA3" } };
        //results.put("LimitedSubquery", toList(r));
        results.put("LimitedSubquery", NO_RESULT); // Gives a random selection of the Employees
        results.put("ObjectStoreBagCombination3", Collections.EMPTY_LIST);

        results.put("TotallyFalse", Collections.EMPTY_LIST);
        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("TotallyTrue", toList(r));
        results.put("MergeFalse", toList(r));
        results.put("MergeTrue", toList(r));
        results.put("EmptyBagConstraint", Collections.EMPTY_LIST);
        int minId = ((Employee) data.get("EmployeeA1")).getId().intValue();
        minId = Math.min(minId, ((Employee) data.get("EmployeeA2")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeA3")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeB1")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeB2")).getId().intValue());
        minId = Math.min(minId, ((Employee) data.get("EmployeeB3")).getId().intValue());
        r = new Object[][] { { new Integer(minId) } };
        results.put("SelectFunctionNoGroup", toList(r));
        r = new Object[][] { { Address.class, new Long(8) },
                             { CEO.class, new Long(1) },
                             { Company.class, new Long(2) },
                             { Contractor.class, new Long(2) },
                             { Department.class, new Long(3) },
                             { Employee.class, new Long(3) },
                             { Manager.class, new Long(2) },
                             { Range.class, new Long(4) },
                             { Secretary.class, new Long(3) },
                             { Types.class, new Long(1) } };
        results.put("SelectClassFromInterMineObject", toList(r));
        r = new Object[][] { { CEO.class, new Long(1) },
                             { Employee.class, new Long(3) },
                             { Manager.class, new Long(2) } };
        results.put("SelectClassFromEmployee", toList(r));
        r = new Object[][] { { data.get("DepartmentA1"), Arrays.asList(data.get("EmployeeA1")) },
                             { data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1")) },
                             { data.get("DepartmentB2"), Collections.singletonList(data.get("EmployeeB3")) } };
        results.put("SubclassCollection", toList(r));
        r = new Object[][] { { data.get("DepartmentA1"), Collections.EMPTY_LIST },
                             { data.get("DepartmentB1"), Arrays.asList(data.get("EmployeeB1")) },
                             { data.get("DepartmentB2"), Collections.EMPTY_LIST } };
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
                    Arrays.asList(data.get("CompanyA")),
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
    public void executeTest(String type) throws Exception {
        if (results.get(type) instanceof Failure) {
            try {
                Results res = os.execute((Query) queries.get(type), 2, true, true, true);
                Iterator iter = res.iterator();
                while (iter.hasNext()) {
                    iter.next();
                }
                fail(type + " was expected to fail");
            } catch (Exception e) {
                assertEquals(type + " was expected to produce a particular exception", results.get(type), new Failure(e));
            }
        } else {
            System.out.println(type + " QUERY: " + queries.get(type));
            Results res = os.execute((Query)queries.get(type), 2, true, true, true);
            List expected = (List) results.get(type);
            if ((expected != null) && (!expected.equals(res))) {
                Set a = new HashSet(expected);
                Set b = new HashSet(res);
                List la = resToNames(expected);
                List lb = resToNames(res);
                if (a.equals(b)) {
                    assertEquals(type + " has failed - wrong order", la, lb);
                }
                fail(type + " has failed. Expected " + la + " but was " + lb + " - " + b);
            }
            //assertEquals(type + " has failed", results.get(type), res);
        }
    }

    public List resToNames(List res) throws Exception {
        List aNames = new ArrayList();
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            List row = (List) resIter.next();
            List toRow = new ArrayList();
            Iterator rowIter = row.iterator();
            while (rowIter.hasNext()) {
                Object o = rowIter.next();
                if (o instanceof List) {
                    List newO = new ArrayList();
                    for (Object p : ((List) o)) {
                        newO.add(objectToName(p));
                    }
                    toRow.add(newO);
                } else {
                    toRow.add(objectToName(o));
                }
            }
            aNames.add(toRow);
        }
        return aNames;
    }

    public void testResults() throws Exception {
        Object[][] r = new Object[][] { { data.get("CompanyA") },
                                        { data.get("CompanyB") } };
        List res = os.execute((Query) queries.get("SelectSimpleObject"));
        assertEquals(toList(r).size(), res.size());
        assertEquals(toList(r), res);
    }

    // estimate tests

    public void testEstimateQueryNotNull() throws Exception {
        ResultsInfo er = os.estimate((Query)queries.get("WhereClassClass"));
        if (er == null) {
            fail("a null ResultsInfo was returned");
        }
    }

    // reference and collection proxy tests

    public void testCEOWhenSearchingForManager() throws Exception {
        // test
        // select manager where manager.name="EmployeeB1" (actually a CEO)
        QueryClass c1 = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("EmployeeB1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        q1.setConstraint(sc1);
        List l1 = os.execute(q1);
        assertEquals(1, l1.size());
        CEO ceo = (CEO) (((ResultsRow) l1.get(0)).get(0));
        assertEquals(ceo.toString(), 45000, ceo.getSalary());
    }

    public void testLazyCollection() throws Exception {
        List r = os.execute((Query) queries.get("ContainsN1"));
        Department d = (Department) ((ResultsRow) r.get(0)).get(0);
        assertTrue("Expected " + d.getEmployees().getClass() + " to be a Lazy object", d.getEmployees() instanceof Lazy);

        Set expected = new HashSet();
        expected.add(data.get("EmployeeA1"));
        expected.add(data.get("EmployeeA2"));
        expected.add(data.get("EmployeeA3"));
        assertEquals(expected, new HashSet(d.getEmployees()));
    }

    public void testLazyCollectionMtoN() throws Exception {
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        q1.setConstraint(sc1);
        Results r  = os.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Company c = (Company) rr.get(0);
        assertTrue("Expected " + c.getContractors().getClass() + " to be a Lazy object", c.getContractors() instanceof Lazy);
        Set contractors = new HashSet(c.getContractors());
        Set expected1 = new HashSet();
        expected1.add(data.get("ContractorA"));
        expected1.add(data.get("ContractorB"));
        assertEquals(expected1, contractors);

        Contractor contractor1 = (Contractor) contractors.iterator().next();
        assertTrue("Expected " + contractor1.getCompanys().getClass() + " to be a Lazy object", contractor1.getCompanys() instanceof Lazy);
        Set expected2 = new HashSet();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        assertEquals(expected2, new HashSet(contractor1.getCompanys()));
    }

    // setDistinct tests

    public void testCountNoGroupByNotDistinct() throws Exception {
        Query q = QueryCloner.cloneQuery((Query) queries.get("ContainsDuplicatesMN"));
        q.setDistinct(false);
        int count = os.count(q, ObjectStore.SEQUENCE_IGNORE);
        assertEquals(4, count);
        assertEquals(4, os.execute(q).size());
    }

    public void testCountNoGroupByDistinct() throws Exception {
        Query q = (Query) queries.get("ContainsDuplicatesMN");
        int count = os.count(q, ObjectStore.SEQUENCE_IGNORE);
        assertEquals(4, count);
        assertEquals(4, os.execute(q).size());
    }

   public void testCountGroupByNotDistinct() throws Exception {
        Query q = QueryCloner.cloneQuery((Query) queries.get("SimpleGroupBy"));
        q.setDistinct(false);
        int count = os.count(q, ObjectStore.SEQUENCE_IGNORE);
        assertEquals(2, count);
        assertEquals(2, os.execute(q).size());
    }

    public void testCountGroupByDistinct() throws Exception {
    // distinct doesn't actually do anything to group by reuslt
        Query q = (Query) queries.get("SimpleGroupBy");
        int count = os.count(q, ObjectStore.SEQUENCE_IGNORE);
        assertEquals(2, count);
        assertEquals(2, os.execute(q).size());
    }

    // getObjectByExample tests

    public void testGetObjectByExampleNull() throws Exception {
        try {
            os.getObjectByExample(null, Collections.singleton("name"));
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetObjectByExampleNonExistent() throws Exception {
        Address a = new Address();
        a.setAddress("10 Downing Street");
        assertNull(os.getObjectByExample(a, Collections.singleton("address")));
    }

    public void testGetObjectByExampleAttribute() throws Exception {
        Address a1 = ((Employee) data.get("EmployeeA1")).getAddress();
        Address a = new Address();
        a.setAddress(a1.getAddress());
        assertEquals(a1, os.getObjectByExample(a, Collections.singleton("address")));
    }

    public void testGetObjectByExampleFields() throws Exception {
        Employee e1 = (Employee) data.get("EmployeeA1");
        Employee e = new Employee();
        e.setName(e1.getName());
        e.setAge(e1.getAge());
        e.setAddress(e1.getAddress());
        assertEquals(e1, os.getObjectByExample(e, new HashSet(Arrays.asList(new String[] {"name", "age", "address"}))));
    }

    public void testDataTypes() throws Exception {
        Types d1 = (Types) data.get("Types1");
        //Types d2 = new Types();
        //d2.setName(d1.getName());
        Query q = new Query();
        QueryClass c = new QueryClass(Types.class);
        q.addFrom(c);
        q.addToSelect(c);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryField name = new QueryField(c, "name");
        QueryField booleanType = new QueryField(c, "booleanType");
        QueryField floatType = new QueryField(c, "floatType");
        QueryField doubleType = new QueryField(c, "doubleType");
        QueryField shortType = new QueryField(c, "shortType");
        QueryField intType = new QueryField(c, "intType");
        QueryField longType = new QueryField(c, "longType");
        QueryField booleanObjType = new QueryField(c, "booleanObjType");
        QueryField floatObjType = new QueryField(c, "floatObjType");
        QueryField doubleObjType = new QueryField(c, "doubleObjType");
        QueryField shortObjType = new QueryField(c, "shortObjType");
        QueryField intObjType = new QueryField(c, "intObjType");
        QueryField longObjType = new QueryField(c, "longObjType");
        QueryField bigDecimalObjType = new QueryField(c, "bigDecimalObjType");
        QueryField stringObjType = new QueryField(c, "stringObjType");
        QueryField dateObjType = new QueryField(c, "dateObjType");
        q.addToSelect(name);
        q.addToSelect(booleanType);
        q.addToSelect(floatType);
        q.addToSelect(doubleType);
        q.addToSelect(shortType);
        q.addToSelect(intType);
        q.addToSelect(longType);
        q.addToSelect(booleanObjType);
        q.addToSelect(floatObjType);
        q.addToSelect(doubleObjType);
        q.addToSelect(shortObjType);
        q.addToSelect(intObjType);
        q.addToSelect(longObjType);
        q.addToSelect(bigDecimalObjType);
        q.addToSelect(stringObjType);
        q.addToSelect(dateObjType);
        cs.addConstraint(new SimpleConstraint(name, ConstraintOp.EQUALS, new QueryValue("Types1")));
        cs.addConstraint(new SimpleConstraint(booleanType, ConstraintOp.EQUALS, new QueryValue(Boolean.TRUE)));
        cs.addConstraint(new SimpleConstraint(floatType, ConstraintOp.EQUALS, new QueryValue(new Float(0.6F))));
        cs.addConstraint(new SimpleConstraint(doubleType, ConstraintOp.EQUALS, new QueryValue(new Double(0.88D))));
        cs.addConstraint(new SimpleConstraint(shortType, ConstraintOp.EQUALS, new QueryValue(new Short((short) 675))));
        cs.addConstraint(new SimpleConstraint(intType, ConstraintOp.EQUALS, new QueryValue(new Integer(267))));
        cs.addConstraint(new SimpleConstraint(longType, ConstraintOp.EQUALS, new QueryValue(new Long(98729353495843l))));
        cs.addConstraint(new SimpleConstraint(booleanObjType, ConstraintOp.EQUALS, new QueryValue(Boolean.TRUE)));
        cs.addConstraint(new SimpleConstraint(floatObjType, ConstraintOp.EQUALS, new QueryValue(new Float(1.6F))));
        cs.addConstraint(new SimpleConstraint(doubleObjType, ConstraintOp.EQUALS, new QueryValue(new Double(1.88D))));
        cs.addConstraint(new SimpleConstraint(shortObjType, ConstraintOp.EQUALS, new QueryValue(new Short((short) 1982))));
        cs.addConstraint(new SimpleConstraint(intObjType, ConstraintOp.EQUALS, new QueryValue(new Integer(369))));
        cs.addConstraint(new SimpleConstraint(longObjType, ConstraintOp.EQUALS, new QueryValue(new Long(38762874323212l))));
        cs.addConstraint(new SimpleConstraint(bigDecimalObjType, ConstraintOp.EQUALS, new QueryValue(new BigDecimal("876323428764587621764532432.8768173432887324123645"))));
        cs.addConstraint(new SimpleConstraint(stringObjType, ConstraintOp.EQUALS, new QueryValue("A test String")));
        cs.addConstraint(new SimpleConstraint(dateObjType, ConstraintOp.EQUALS, new QueryValue(new Date(7777777l))));

        q.setConstraint(cs);
        Results res = os.execute(q);
        List row1 = (List) res.get(0);
        Types d = (Types) row1.get(0);

        //Types d = (Types) (os.getObjectByExample(d2));

        // Go through each attribute to check that it has been set correctly
        assertEquals(d1.getName(), d.getName());
        assertEquals(d1.getName(), row1.get(1));
        assertEquals(d1.getBooleanType(), d.getBooleanType());
        assertEquals(Boolean.class, row1.get(2).getClass());
        assertEquals(d1.getBooleanType(), ((Boolean) row1.get(2)).booleanValue());
        assertEquals(d1.getFloatType(), d.getFloatType(), 0.0);
        assertEquals(Float.class, row1.get(3).getClass());
        assertEquals(d1.getFloatType(), ((Float) row1.get(3)).floatValue(), 0.0);
        assertEquals(d1.getDoubleType(), d.getDoubleType(), 0.0);
        assertEquals(Double.class, row1.get(4).getClass());
        assertEquals(d1.getDoubleType(), ((Double) row1.get(4)).doubleValue(), 0.0);
        assertEquals(d1.getShortType(), d.getShortType());
        assertEquals(Short.class, row1.get(5).getClass());
        assertEquals(d1.getShortType(), ((Short) row1.get(5)).shortValue());
        assertEquals(d1.getIntType(), d.getIntType());
        assertEquals(Integer.class, row1.get(6).getClass());
        assertEquals(d1.getIntType(), ((Integer) row1.get(6)).intValue());
        assertEquals(d1.getLongType(), d.getLongType());
        assertEquals(Long.class, row1.get(7).getClass());
        assertEquals(d1.getLongType(), ((Long) row1.get(7)).longValue());
        assertEquals(d1.getBooleanObjType(), d.getBooleanObjType());
        assertEquals(d1.getBooleanObjType(), row1.get(8));
        assertEquals(d1.getFloatObjType(), d.getFloatObjType());
        assertEquals(d1.getFloatObjType(), row1.get(9));
        assertEquals(d1.getDoubleObjType(), d.getDoubleObjType());
        assertEquals(d1.getDoubleObjType(), row1.get(10));
        assertEquals(d1.getShortObjType(), d.getShortObjType());
        assertEquals(d1.getShortObjType(), row1.get(11));
        assertEquals(d1.getIntObjType(), d.getIntObjType());
        assertEquals(d1.getIntObjType(), row1.get(12));
        assertEquals(d1.getLongObjType(), d.getLongObjType());
        assertEquals(d1.getLongObjType(), row1.get(13));
        assertEquals(d1.getBigDecimalObjType(), d.getBigDecimalObjType());
        assertEquals(d1.getBigDecimalObjType(), row1.get(14));
        assertEquals(d1.getStringObjType(), d.getStringObjType());
        assertEquals(d1.getStringObjType(), row1.get(15));
        assertEquals(d1.getDateObjType(), d.getDateObjType());
        assertEquals(Date.class, row1.get(16).getClass());
        assertEquals(d1.getDateObjType(), row1.get(16));
    }

    public void testGetObjectByNullId() throws Exception {
        try {
            os.getObjectById(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetObjectById() throws Exception {
        Integer id = ((Employee) data.get("EmployeeA1")).getId();
        Employee e = (Employee) os.getObjectById(id, Employee.class);
        assertEquals(data.get("EmployeeA1"), e);
        assertTrue(e == os.getObjectById(id, Employee.class));
    }

    public void testGetObjectMultipleTimes() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select Secretary from Secretary where Secretary.name = 'Secretary1'", "org.intermine.model.testmodel"));
        Secretary a = (Secretary) ((List) os.execute(q).get(0)).get(0);

        Secretary b = (Secretary) os.getObjectById(a.getId(), Secretary.class);
        Secretary c = (Secretary) os.getObjectById(a.getId(), Secretary.class);
        assertEquals(b, c);
        assertTrue(b == c);
        assertEquals(a, b);
        assertTrue(a == b);
    }

    public void testIndirectionTableMultipleCopies() throws Exception {
        Contractor c1 = new Contractor();
        c1.setName("Clippy");
        Company c2 = (Company) DynamicUtil.createObject(Company.class);
        c2.setName("?bersoft");
        c2.addContractors(c1);
        c1.addCompanys(c2);

        storeDataWriter.store(c1);
        storeDataWriter.store(c2);

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("Clippy"));
        ContainsConstraint cc = new ContainsConstraint(new QueryCollectionReference(qc1, "companys"), ConstraintOp.CONTAINS, qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(sc);
        cs.addConstraint(cc);
        q1.setConstraint(cs);
        q1.setDistinct(false);

        try {
            Results r1 = os.execute(q1);
            assertEquals(1, r1.size());

            storeDataWriter.store(c1);
            Results r2 = os.execute(q1);
            assertEquals(1, r1.size());
        } finally {
            storeDataWriter.delete(c1);
            storeDataWriter.delete(c2);
        }
    }

    public void testSimpleObjects() throws Exception {
        SimpleObject so = new SimpleObject();
        so.setName("Albert");
        storeDataWriter.store(so);
        Query q = new Query();
        QueryClass qc = new QueryClass(SimpleObject.class);
        QueryField qf = new QueryField(qc, "name");
        q.addFrom(qc);
        q.addToSelect(qc);
        Constraint con = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("Albert"));
        q.setConstraint(con);
        SingletonResults res = os.executeSingleton(q);
        assertEquals(1, res.size());
        SimpleObject got = (SimpleObject) res.get(0);
        assertEquals("Albert", got.getName());
        assertNull(got.getEmployee());
        storeDataWriter.delete(qc, con);
        res = os.executeSingleton(q);
        assertEquals(0, res.size());
    }

    public void testNullFields() throws Exception {
        Employee ex = new Employee();
        ex.setName("EmployeeB1");
        Employee e = (Employee) os.getObjectByExample(ex, Collections.singleton("name"));
        assertNull(e.proxGetAddress());
        assertNull(e.getAddress());
    }

    public void testEmptySelect() throws Exception {
        try {
            Query q = new Query();
            QueryClass qc = new QueryClass(Employee.class);
            q.addFrom(qc);
            q.setDistinct(false);
            os.execute(q, 0, 100, false, false, ObjectStore.SEQUENCE_IGNORE);
            fail("Expected exception");
        } catch (ObjectStoreException e) {
            assertEquals("SELECT list is empty in Query", e.getMessage());
        }
    }

    // helper method

    protected static List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
    }
}
