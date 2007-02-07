package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.DatabaseFactory;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.TypeUtil;

public class WithNotXmlSqlGeneratorTest extends SqlGeneratorTest
{
    public WithNotXmlSqlGeneratorTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(WithNotXmlSqlGeneratorTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        SqlGeneratorTest.oneTimeSetUp();
        setUpResults();
        db = DatabaseFactory.getDatabase("db.notxmlunittest");
    }

    public static void setUpResults() throws Exception {
        results.put("SelectSimpleObject", "SELECT intermine_Alias.OBJECT AS \"intermine_Alias\", intermine_Alias.id AS \"intermine_Aliasid\" FROM Company AS intermine_Alias ORDER BY intermine_Alias.id");
        results2.put("SelectSimpleObject", Collections.singleton("Company"));
        results.put("WhereSubQueryField", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name FROM Department AS a1_) ORDER BY a1_.name, a1_.id");
        results2.put("WhereSubQueryField", Collections.singleton("Department"));
        results.put("WhereSubQueryClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id IN (SELECT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereSubQueryClass", Collections.singleton("Company"));
        results.put("WhereNotSubQueryClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id NOT IN (SELECT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereNotSubQueryClass", Collections.singleton("Company"));
        results.put("WhereNegSubQueryClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id NOT IN (SELECT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereNegSubQueryClass", Collections.singleton("Company"));
        results.put("WhereClassClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereClassClass", Collections.singleton("Company"));
        results.put("WhereNotClassClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereNotClassClass", Collections.singleton("Company"));
        results.put("WhereNegClassClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereNegClassClass", Collections.singleton("Company"));
        results.put("WhereClassObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id = " + companyAId + " ORDER BY a1_.id");
        results2.put("WhereClassObject", Collections.singleton("Company"));
        results.put("Contains11", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE a1_.managerId = a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("Contains11", new HashSet(Arrays.asList(new String[] {"Department", "Manager"})));
        results.put("ContainsNot11", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNot11", new HashSet(Arrays.asList(new String[] {"Department", "Manager"})));
        results.put("ContainsNeg11", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNeg11", new HashSet(Arrays.asList(new String[] {"Department", "Manager"})));
        results.put("Contains1N", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ WHERE a1_.id = a2_.companyId AND a1_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("Contains1N", new HashSet(Arrays.asList(new String[] {"Department", "Company"})));
        results.put("ContainsNot1N", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ WHERE a1_.id != a2_.companyId AND a1_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNot1N", new HashSet(Arrays.asList(new String[] {"Department", "Company"})));
        results.put("ContainsN1", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Department AS a1_, Company AS a2_ WHERE a1_.companyId = a2_.id AND a2_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsN1", new HashSet(Arrays.asList(new String[] {"Department", "Company"})));
        results.put("ContainsMN", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, CompanysContractors AS indirect0 WHERE a1_.id = indirect0.Companys AND indirect0.Contractors = a2_.id AND a1_.name = 'ContractorA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsMN", new HashSet(Arrays.asList(new String[] {"Contractor", "Company", "CompanysContractors"})));
        results.put("ContainsDuplicatesMN", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, OldComsOldContracts AS indirect0 WHERE a1_.id = indirect0.OldComs AND indirect0.OldContracts = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("ContainsDuplicatesMN", new HashSet(Arrays.asList(new String[] {"Contractor", "Company", "OldComsOldContracts"})));
        results.put("SimpleGroupBy", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, COUNT(*) AS a2_ FROM Company AS a1_, Department AS a3_ WHERE a1_.id = a3_.companyId GROUP BY a1_.OBJECT, a1_.CEOId, a1_.addressId, a1_.id, a1_.name, a1_.vatNumber ORDER BY a1_.id, COUNT(*)");
        results2.put("SimpleGroupBy", new HashSet(Arrays.asList(new String[] {"Department", "Company"})));
        results.put("MultiJoin", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id, a4_.OBJECT AS a4_, a4_.id AS a4_id FROM Company AS a1_, Department AS a2_, Manager AS a3_, Address AS a4_ WHERE a1_.id = a2_.companyId AND a2_.managerId = a3_.id AND a3_.addressId = a4_.id AND a3_.name = 'EmployeeA1' ORDER BY a1_.id, a2_.id, a3_.id, a4_.id");
        results2.put("MultiJoin", new HashSet(Arrays.asList(new String[] {"Department", "Manager", "Company", "Address"})));
        results.put("SelectComplex", "SELECT DISTINCT (AVG(a1_.vatNumber) + 20) AS a3_, STDDEV(a1_.vatNumber) AS a4_, a2_.name AS a5_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ GROUP BY a2_.OBJECT, a2_.companyId, a2_.id, a2_.managerId, a2_.name ORDER BY (AVG(a1_.vatNumber) + 20), STDDEV(a1_.vatNumber), a2_.name, a2_.id");
        results2.put("SelectComplex", new HashSet(Arrays.asList(new String[] {"Department", "Company"})));
        results.put("SelectClassAndSubClasses", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM Employee AS a1_ ORDER BY a1_.name, a1_.id");
        results2.put("SelectClassAndSubClasses", Collections.singleton("Employee"));
        results.put("SelectInterfaceAndSubClasses", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_ ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses", Collections.singleton("Employable"));
        results.put("SelectInterfaceAndSubClasses2", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM RandomInterface AS a1_ ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses2", Collections.singleton("RandomInterface"));
        results.put("SelectInterfaceAndSubClasses3", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM ImportantPerson AS a1_ ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses3", Collections.singleton("ImportantPerson"));
        results.put("SelectClassObjectSubquery", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_, Department AS a2_ WHERE a1_.id = " + companyAId + " AND a1_.id = a2_.companyId AND a2_.id IN (SELECT a1_.id FROM Department AS a1_ WHERE a1_.id = " + departmentA1Id + ") ORDER BY a1_.id");
        results2.put("SelectClassObjectSubquery", new HashSet(Arrays.asList(new String[] {"Department", "Company"})));
        results.put("SelectUnidirectionalCollection", "SELECT DISTINCT a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Company AS a1_, Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a1_.name = 'CompanyA' AND a1_.id = indirect0.Secretarys AND indirect0.HasSecretarys = a2_.id ORDER BY a2_.id");
        results2.put("SelectUnidirectionalCollection", new HashSet(Arrays.asList(new String[] {"Company", "Secretary", "HasSecretarysSecretarys"})));
        results.put("EmptyAndConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE true ORDER BY a1_.id");
        results2.put("EmptyAndConstraintSet", Collections.singleton("Company"));
        results.put("EmptyOrConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE false ORDER BY a1_.id");
        results2.put("EmptyOrConstraintSet", Collections.singleton("Company"));
        results.put("EmptyNandConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE false ORDER BY a1_.id");
        results2.put("EmptyNandConstraintSet", Collections.singleton("Company"));
        results.put("EmptyNorConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE true ORDER BY a1_.id");
        results2.put("EmptyNorConstraintSet", Collections.singleton("Company"));
        results.put("BagConstraint", "SELECT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM Company AS Company WHERE Company.name IN ('CompanyA', 'goodbye', 'hello') ORDER BY Company.id");
        results2.put("BagConstraint", Collections.singleton("Company"));
        results.put("BagConstraint2", "SELECT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM Company AS Company WHERE Company.id IN (" + companyAId + ") ORDER BY Company.id");
        results2.put("BagConstraint2", Collections.singleton("Company"));
        results.put("InterfaceField", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_ WHERE a1_.name = 'EmployeeA1' ORDER BY a1_.id");
        results2.put("InterfaceField", Collections.singleton("Employable"));
        Set res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1__1.debt AS a2_, a1_.age AS a3_ FROM Employee AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND a1__1.debt > 0 AND a1_.age > 0 ORDER BY a1_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.debt AS a2_, a1__1.age AS a3_ FROM Broke AS a1_, Employee AS a1__1 WHERE a1_.id = a1__1.id AND a1_.debt > 0 AND a1__1.age > 0 ORDER BY a1_.id");
        results.put("DynamicInterfacesAttribute", res);
        results2.put("DynamicInterfacesAttribute", new HashSet(Arrays.asList(new String[] {"Employee", "Broke"})));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id ORDER BY a1_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1 WHERE a1_.id = a1__1.id ORDER BY a1_.id");
        results.put("DynamicClassInterface", res);
        results2.put("DynamicClassInterface", new HashSet(Arrays.asList(new String[] {"Employable", "Broke"})));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Department AS a1_, Broke AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a2_.id = a1_.companyId AND a3_.id = a1__1.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Broke AS a1_, Department AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a2_.id = a1__1.companyId AND a3_.id = a1_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef1", res);
        results2.put("DynamicClassRef1", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank"})));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Department AS a1_, Broke AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a1_.companyId = a2_.id AND a1__1.bankId = a3_.id ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Broke AS a1_, Department AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a1__1.companyId = a2_.id AND a1_.bankId = a3_.id ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef2", res);
        results2.put("DynamicClassRef2", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank"})));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Company AS a1_, Bank AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a1_.id = a2_.companyId AND a1_.id = a3_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Bank AS a1_, Company AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a1_.id = a2_.companyId AND a1_.id = a3_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef3", res);
        results2.put("DynamicClassRef3", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank"})));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Company AS a1_, Bank AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a2_.companyId = a1_.id AND a3_.bankId = a1_.id ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Bank AS a1_, Company AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a2_.companyId = a1_.id AND a3_.bankId = a1_.id ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef4", res);
        results2.put("DynamicClassRef4", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank"})));
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1, HasAddress AS a2_, Broke AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1, Broke AS a2_, HasAddress AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1, HasAddress AS a2_, Broke AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1, Broke AS a2_, HasAddress AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        results.put("DynamicClassConstraint", res);
        results2.put("DynamicClassConstraint", new HashSet(Arrays.asList(new String[] {"Employable", "Broke", "HasAddress"})));
        results.put("ContainsConstraintNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNull", Collections.singleton("Employee"));
        results.put("ContainsConstraintNotNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NOT NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNotNull", Collections.singleton("Employee"));
        results.put("ContainsConstraintObjectRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.departmentId = 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintObjectRefObject", new HashSet(Arrays.asList(new String[] {"Employee"})));
        results.put("ContainsConstraintNotObjectRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.departmentId != 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotObjectRefObject", new HashSet(Arrays.asList(new String[] {"Employee"})));
        results.put("ContainsConstraintCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Department AS a1_, Employee AS indirect0 WHERE a1_.id = indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintCollectionRefObject", new HashSet(Arrays.asList(new String[] {"Department", "Employee"})));
        results.put("ContainsConstraintNotCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Department AS a1_, Employee AS indirect0 WHERE a1_.id != indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotCollectionRefObject", new HashSet(Arrays.asList(new String[] {"Department", "Employee"})));
        results.put("ContainsConstraintMMCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_, CompanysContractors AS indirect0 WHERE a1_.id = indirect0.Contractors AND indirect0.Companys = 3 ORDER BY a1_.id");
        results2.put("ContainsConstraintMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"Company", "CompanysContractors"})));
        //results.put("ContainsConstraintNotMMCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_, CompanysContractors AS indirect0 WHERE a1_.id != indirect0.Contractors AND indirect0.Companys = 3 ORDER BY a1_.id");
        //results2.put("ContainsConstraintNotMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"Company", "CompanysContractors"})));
        results.put("SimpleConstraintNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Manager AS a1_ WHERE a1_.title IS NULL ORDER BY a1_.id");
        results2.put("SimpleConstraintNull", Collections.singleton("Manager"));
        results.put("SimpleConstraintNotNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Manager AS a1_ WHERE a1_.title IS NOT NULL ORDER BY a1_.id");
        results2.put("SimpleConstraintNotNull", Collections.singleton("Manager"));
        results.put("OrderByReference", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.departmentId AS orderbyfield0 FROM Employee AS a1_ ORDER BY a1_.departmentId, a1_.id");
        results2.put("OrderByReference", Collections.singleton("Employee"));

        String largeBagConstraintText = new BufferedReader(new InputStreamReader(TruncatedSqlGeneratorTest.class.getClassLoader().getResourceAsStream("withNotXmlLargeBag.sql"))).readLine();
        results.put("LargeBagConstraint", largeBagConstraintText);
        results2.put("LargeBagConstraint", Collections.singleton("Employee"));

        String largeNotBagConstraintText = new BufferedReader(new InputStreamReader(TruncatedSqlGeneratorTest.class.getClassLoader().getResourceAsStream("withNotXmlLargeNotBag.sql"))).readLine();
        results.put("LargeBagNotConstraint", largeNotBagConstraintText);
        results2.put("LargeBagNotConstraint", Collections.singleton("Employee"));

        results.put("LargeBagConstraintUsingTable", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_, " + LARGE_BAG_TABLE_NAME + " AS indirect0 WHERE a1_.name = indirect0.value ORDER BY a1_.id");
        results2.put("LargeBagConstraintUsingTable", Collections.singleton("Employee"));

        results.put("LargeBagNotConstraintUsingTable", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE (NOT (a1_.name IN (SELECT value FROM " + LARGE_BAG_TABLE_NAME + "))) ORDER BY a1_.id");
        results2.put("LargeBagNotConstraintUsingTable", Collections.singleton("Employee"));

        results.put("NegativeNumbers", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.age > -51 ORDER BY a1_.id");
        results2.put("NegativeNumbers", Collections.singleton("Employee"));
        results.put("CollectionQueryOneMany", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE " + departmentA1Id + " = a1_.departmentId ORDER BY a1_.id");
        results2.put("CollectionQueryOneMany", Collections.singleton("Employee"));
        results.put("CollectionQueryManyMany", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Secretary AS a1_, HasSecretarysSecretarys AS indirect0 WHERE " + companyBId + " = indirect0.Secretarys AND indirect0.HasSecretarys = a1_.id ORDER BY a1_.id");
        results2.put("CollectionQueryManyMany", new HashSet(Arrays.asList(new String[] {"Secretary", "HasSecretarysSecretarys"})));
        results.put("QueryClassBag", "SELECT a2_.departmentId AS a3_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Employee AS a2_ WHERE a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") ORDER BY a2_.departmentId, a2_.id");
        results2.put("QueryClassBag", Collections.singleton("Employee"));
        results.put("QueryClassBagMM", "SELECT indirect0.Secretarys AS a3_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE indirect0.Secretarys IN (" + companyAId + ", " + companyBId + ", " + employeeB1Id + ") AND indirect0.HasSecretarys = a2_.id ORDER BY indirect0.Secretarys, a2_.id");
        results2.put("QueryClassBagMM", new HashSet(Arrays.asList(new String[] {"Secretary", "HasSecretarysSecretarys"})));
        results.put("QueryClassBagDynamic", "SELECT indirect0.Secretarys AS a3_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE indirect0.Secretarys IN (" + employeeB1Id + ") AND indirect0.HasSecretarys = a2_.id ORDER BY indirect0.Secretarys, a2_.id");
        results2.put("QueryClassBagDynamic", new HashSet(Arrays.asList(new String[] {"Secretary", "HasSecretarysSecretarys"})));
        //res = new HashSet()
        //res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND (a1_.id IN (" + employeeB1Id + ")) ORDER BY a1_.id");
        //res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1 WHERE a1_.id = a1__1.id AND (a1_.id IN (" + employeeB1Id + ")) ORDER BY a1_.id");
        //results.put("DynamicBagConstraint", res);
        //results2.put("DynamicBagConstraint", new HashSet(Arrays.asList(new String[] {"Broke", "CEO"}))); // See ticket #469
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM CEO AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND a1_.id IN (" + employeeB1Id + ") ORDER BY a1_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Broke AS a1_, CEO AS a1__1 WHERE a1_.id = a1__1.id AND a1_.id IN (" + employeeB1Id + ") ORDER BY a1_.id");
        results.put("DynamicBagConstraint2", res);
        results2.put("DynamicBagConstraint2", new HashSet(Arrays.asList(new String[] {"Broke", "CEO"})));
        results.put("QueryClassBagDouble", "SELECT a2_.departmentId AS a4_, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM Employee AS a2_, Employee AS a3_ WHERE a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND a3_.departmentId = a2_.departmentId ORDER BY a2_.departmentId, a2_.id, a3_.id");
        results2.put("QueryClassBagDouble", Collections.singleton("Employee"));
        //results.put("ObjectPathExpression", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        //results2.put("ObjectPathExpression", new HashSet(Arrays.asList(new String[] {"Employee", "Department"})));
        //results.put("FieldPathExpression", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        //results2.put("FieldPathExpression", new HashSet(Arrays.asList(new String[] {"Company", "CEO"})));
        results.put("ObjectPathExpression", NO_RESULT);
        results.put("FieldPathExpression", NO_RESULT);
        results.put("ForeignKey", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.CEOId AS a2_ FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("ForeignKey", Collections.singleton("Company"));
        results.put("ForeignKey2", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.CEOId AS a2_ FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("ForeignKey2", Collections.singleton("Company"));
        results.put("ScientificNumber", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Types AS a1_ WHERE a1_.doubleType < 1.3432E24 AND a1_.floatType > -8.56E-32::REAL ORDER BY a1_.id");
        results2.put("ScientificNumber", Collections.singleton("Types"));
        results.put("LowerBag", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Employee AS a1_ WHERE LOWER(a1_.name) IN ('employeea1', 'employeea2', 'employeeb1') ORDER BY a1_.id");
        results2.put("LowerBag", Collections.singleton("Employee"));
    }

    protected DatabaseSchema getSchema() {
        return new DatabaseSchema(model, Collections.EMPTY_LIST, false, Collections.EMPTY_SET);
    }
    public String getRegisterOffset1() {
        return "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id";
    }
    public String getRegisterOffset2() {
        return "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM Company AS a1_ WHERE ";
    }
    public String getRegisterOffset3() {
        return "Employee AS a1_";
    }
    public String getRegisterOffset4() {
        return "WHERE";
    }
    public String precompTableString() {
        return "SELECT intermine_Alias.OBJECT AS \"intermine_Alias\", intermine_Alias.CEOId AS \"intermine_Aliasceoid\", intermine_Alias.addressId AS \"intermine_Aliasaddressid\", intermine_Alias.id AS \"intermine_Aliasid\", intermine_Alias.name AS \"intermine_Aliasname\", intermine_Alias.vatNumber AS \"intermine_Aliasvatnumber\" FROM Company AS intermine_Alias ORDER BY intermine_Alias.id";
    }

    public void testInvalidClassForGetById() throws Exception {
        try {
            SqlGenerator.generateQueryForId(new Integer(5), SqlGeneratorTest.class, getSchema());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("class org.intermine.objectstore.intermine.SqlGeneratorTest is not in the model", e.getMessage());
        }
        try {
            SqlGenerator.tableNameForId(SqlGeneratorTest.class, getSchema());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("class org.intermine.objectstore.intermine.SqlGeneratorTest is not in the model", e.getMessage());
        }
    }
}
