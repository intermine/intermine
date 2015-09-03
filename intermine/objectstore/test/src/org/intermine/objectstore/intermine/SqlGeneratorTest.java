package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.Failure;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.SetupDataTestCase;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.precompute.BestQueryStorer;
import org.intermine.sql.precompute.PrecomputedTable;
import org.intermine.sql.precompute.QueryOptimiser;
import org.intermine.testing.MustBeDifferentMap;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.DynamicUtil;

public class SqlGeneratorTest extends SetupDataTestCase
{
    protected static Database db;
    protected static Map<String, Object> results2;
    private static Map<String, Map<String, String>> rangeQueries;

    public SqlGeneratorTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(SqlGeneratorTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        setUpResults();
        db = DatabaseFactory.getDatabase("db.unittest");
    }

    public static Integer companyAId;
    public static Integer companyBId;
    public static Integer departmentA1Id;
    public static Integer departmentB1Id;
    public static Integer employeeA1Id;
    public static Integer employeeA2Id;
    public static Integer employeeB1Id;

    public static void setUpResults() throws Exception {
        companyAId = ((Company) data.get("CompanyA")).getId();
        companyBId = ((Company) data.get("CompanyB")).getId();
        departmentA1Id = ((Department) data.get("DepartmentA1")).getId();
        departmentB1Id = ((Department) data.get("DepartmentB1")).getId();
        employeeA1Id = ((Employee) data.get("EmployeeA1")).getId();
        employeeA2Id = ((Employee) data.get("EmployeeA2")).getId();
        employeeB1Id = ((Employee) data.get("EmployeeB1")).getId();
        results = new MustBeDifferentMap(new HashMap());
        results2 = new MustBeDifferentMap(new HashMap());
        results.put("SelectSimpleObject", "SELECT intermine_Alias.id AS \"intermine_Aliasid\" FROM Company AS intermine_Alias ORDER BY intermine_Alias.id");
        results2.put("SelectSimpleObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("SubQuery", "SELECT DISTINCT intermine_All.intermine_Arrayname AS a1_, intermine_All.intermine_Alias AS \"intermine_Alias\" FROM (SELECT intermine_Array.CEOId AS intermine_ArrayCEOId, intermine_Array.addressId AS intermine_ArrayaddressId, intermine_Array.bankId AS intermine_ArraybankId, intermine_Array.id AS intermine_Arrayid, intermine_Array.name AS intermine_Arrayname, intermine_Array.vatNumber AS intermine_ArrayvatNumber, 5 AS intermine_Alias FROM Company AS intermine_Array) AS intermine_All ORDER BY intermine_All.intermine_Arrayname, intermine_All.intermine_Alias");
        results2.put("SubQuery", Collections.singleton("Company"));
        results.put("WhereSimpleEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber = 1234 ORDER BY a1_.name");
        results2.put("WhereSimpleEquals", Collections.singleton("Company"));
        results.put("WhereSimpleNotEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 1234 ORDER BY a1_.name");
        results2.put("WhereSimpleNotEquals", Collections.singleton("Company"));
        results.put("WhereSimpleNegEquals", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 1234 ORDER BY a1_.name");
        results2.put("WhereSimpleNegEquals", Collections.singleton("Company"));
        results.put("WhereSimpleLike", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name LIKE 'Company%' ORDER BY a1_.name");
        results2.put("WhereSimpleLike", Collections.singleton("Company"));
        results.put("WhereEqualsString", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name = 'CompanyA' ORDER BY a1_.name");
        results2.put("WhereEqualsString", Collections.singleton("Company"));
        results.put("WhereAndSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000 ORDER BY a1_.name");
        results2.put("WhereAndSet", Collections.singleton("Company"));
        results.put("WhereOrSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000) ORDER BY a1_.name");
        results2.put("WhereOrSet", Collections.singleton("Company"));
        results.put("WhereNotSet", "SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)) ORDER BY a1_.name");
        results2.put("WhereNotSet", Collections.singleton("Company"));
        results.put("WhereSubQueryField", "SELECT a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name FROM Department AS a1_) ORDER BY a1_.name, a1_.id");
        results2.put("WhereSubQueryField", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Department"})));
        results.put("WhereSubQueryClass", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id IN (SELECT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereSubQueryClass", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("WhereNotSubQueryClass", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id NOT IN (SELECT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereNotSubQueryClass", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("WhereNegSubQueryClass", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id NOT IN (SELECT a1_.id FROM Company AS a1_ WHERE a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereNegSubQueryClass", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("WhereClassClass", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereClassClass", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("WhereNotClassClass", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereNotClassClass", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("WhereNegClassClass", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Company AS a1_, Company AS a2_ WHERE a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereNegClassClass", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("WhereClassObject", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE a1_.id = " + companyAId + " ORDER BY a1_.id");
        results2.put("WhereClassObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("Contains11", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE a1_.managerId = a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("Contains11", new HashSet(Arrays.asList(new String[] {"Department", "Manager", "InterMineObject"})));
        results.put("ContainsNot11", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNot11", new HashSet(Arrays.asList(new String[] {"Department", "Manager", "InterMineObject"})));
        results.put("ContainsNeg11", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Department AS a1_, Manager AS a2_ WHERE a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNeg11", new HashSet(Arrays.asList(new String[] {"Department", "Manager", "InterMineObject"})));
        results.put("Contains1N", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ WHERE a1_.id = a2_.companyId AND a1_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("Contains1N", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("ContainsNot1N", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ WHERE a1_.id != a2_.companyId AND a1_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNot1N", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("ContainsN1", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Department AS a1_, Company AS a2_ WHERE a1_.companyId = a2_.id AND a2_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsN1", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("ContainsMN", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, CompanysContractors AS indirect0 WHERE a1_.id = indirect0.Contractors AND indirect0.Companys = a2_.id AND a1_.name = 'ContractorA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsMN", new HashSet(Arrays.asList(new String[] {"Contractor", "Company", "CompanysContractors", "InterMineObject"})));
        results.put("ContainsDuplicatesMN", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, OldComsOldContracts AS indirect0 WHERE a1_.id = indirect0.OldContracts AND indirect0.OldComs = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("ContainsDuplicatesMN", new HashSet(Arrays.asList(new String[] {"Contractor", "Company", "OldComsOldContracts", "InterMineObject"})));
        results.put("ContainsNotMN", new Failure(ObjectStoreException.class, "Cannot represent many-to-many collection DOES NOT CONTAIN in SQL")); //TODO: Fix this (ticket #445)
        results2.put("ContainsNotMN", NO_RESULT);
        results.put("SimpleGroupBy", "SELECT DISTINCT a1_.id AS a1_id, COUNT(*) AS a2_ FROM Company AS a1_, Department AS a3_ WHERE a1_.id = a3_.companyId GROUP BY a1_.CEOId, a1_.addressId, a1_.bankId, a1_.id, a1_.name, a1_.vatNumber ORDER BY a1_.id, a2_");
        results2.put("SimpleGroupBy", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("MultiJoin", "SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id, a4_.id AS a4_id FROM Company AS a1_, Department AS a2_, Manager AS a3_, Address AS a4_ WHERE a1_.id = a2_.companyId AND a2_.managerId = a3_.id AND a3_.addressId = a4_.id AND a3_.name = 'EmployeeA1' ORDER BY a1_.id, a2_.id, a3_.id, a4_.id");
        results2.put("MultiJoin", new HashSet(Arrays.asList(new String[] {"Department", "Manager", "Company", "Address", "InterMineObject"})));
        results.put("SelectComplex", "SELECT DISTINCT (AVG(a1_.vatNumber) + 20) AS a3_, STDDEV(a1_.vatNumber) AS a4_, a2_.name AS a5_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ GROUP BY a2_.companyId, a2_.id, a2_.managerId, a2_.name ORDER BY (AVG(a1_.vatNumber) + 20), a4_, a2_.name, a2_.id");
        results2.put("SelectComplex", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("SelectClassAndSubClasses", "SELECT a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM Employee AS a1_ ORDER BY a1_.name, a1_.id");
        results2.put("SelectClassAndSubClasses", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("SelectInterfaceAndSubClasses", "SELECT a1_.id AS a1_id FROM Employable AS a1_ ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employable"})));
        results.put("SelectInterfaceAndSubClasses2", "SELECT a1_.id AS a1_id FROM RandomInterface AS a1_ ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses2", new HashSet(Arrays.asList(new String[] {"InterMineObject", "RandomInterface"})));
        results.put("SelectInterfaceAndSubClasses3", "SELECT a1_.id AS a1_id FROM ImportantPerson AS a1_ ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses3", new HashSet(Arrays.asList(new String[] {"InterMineObject", "ImportantPerson"})));
        results.put("OrderByAnomaly", "SELECT DISTINCT 5 AS a2_, a1_.name AS a3_ FROM Company AS a1_ ORDER BY a1_.name");
        results2.put("OrderByAnomaly", Collections.singleton("Company"));
        results.put("SelectClassObjectSubquery", "SELECT DISTINCT a1_.id AS a1_id FROM Company AS a1_, Department AS a2_ WHERE a1_.id = " + companyAId + " AND a1_.id = a2_.companyId AND a2_.id IN (SELECT a1_.id FROM Department AS a1_ WHERE a1_.id = " + departmentA1Id + ") ORDER BY a1_.id");
        results2.put("SelectClassObjectSubquery", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("SelectUnidirectionalCollection", "SELECT DISTINCT a2_.id AS a2_id FROM Company AS a1_, Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a1_.name = 'CompanyA' AND a1_.id = indirect0.HasSecretarys AND indirect0.Secretarys = a2_.id ORDER BY a2_.id");
        results2.put("SelectUnidirectionalCollection", new HashSet(Arrays.asList(new String[] {"Company", "Secretary", "HasSecretarysSecretarys", "InterMineObject"})));
        results.put("EmptyAndConstraintSet", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("EmptyAndConstraintSet", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("EmptyOrConstraintSet", new Failure(CompletelyFalseException.class, null));
        results2.put("EmptyOrConstraintSet", Collections.EMPTY_SET);
        results.put("EmptyNandConstraintSet", new Failure(CompletelyFalseException.class, null));
        results2.put("EmptyNandConstraintSet", Collections.EMPTY_SET);
        results.put("EmptyNorConstraintSet", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("EmptyNorConstraintSet", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("BagConstraint", "SELECT Company.id AS \"Companyid\" FROM Company AS Company WHERE Company.name IN ('CompanyA', 'goodbye', 'hello') ORDER BY Company.id");
        results2.put("BagConstraint", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("BagConstraint2", "SELECT Company.id AS \"Companyid\" FROM Company AS Company WHERE Company.id IN (" + companyAId + ") ORDER BY Company.id");
        results2.put("BagConstraint2", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("InterfaceField", "SELECT a1_.id AS a1_id FROM Employable AS a1_ WHERE a1_.name = 'EmployeeA1' ORDER BY a1_.id");
        results2.put("InterfaceField", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employable"})));
        results.put("InterfaceReference", NO_RESULT);
        results.put("InterfaceCollection", NO_RESULT);
        Set res = new HashSet();
        results.put("ContainsConstraintNull", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNull", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintNotNull", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NOT NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNotNull", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintNullCollection1N", "SELECT a1_.id AS a1_id FROM Department AS a1_ WHERE (NOT EXISTS(SELECT 1 FROM Employee AS indirect0 WHERE indirect0.departmentId = a1_.id)) ORDER BY a1_.id");
        results2.put("ContainsConstraintNullCollection1N", new HashSet(Arrays.asList(new String[] {"Employee", "Department", "InterMineObject"})));
        results.put("ContainsConstraintNotNullCollection1N", "SELECT a1_.id AS a1_id FROM Department AS a1_ WHERE EXISTS(SELECT 1 FROM Employee AS indirect0 WHERE indirect0.departmentId = a1_.id) ORDER BY a1_.id");
        results2.put("ContainsConstraintNotNullCollection1N", new HashSet(Arrays.asList(new String[] {"Employee", "Department", "InterMineObject"})));
        results.put("ContainsConstraintNullCollectionMN", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE (NOT EXISTS(SELECT 1 FROM CompanysContractors AS indirect0 WHERE indirect0.Companys = a1_.id)) ORDER BY a1_.id");
        results2.put("ContainsConstraintNullCollectionMN", new HashSet(Arrays.asList(new String[] {"Company", "CompanysContractors", "InterMineObject"})));
        results.put("ContainsConstraintNotNullCollectionMN", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE EXISTS(SELECT 1 FROM CompanysContractors AS indirect0 WHERE indirect0.Companys = a1_.id) ORDER BY a1_.id");
        results2.put("ContainsConstraintNotNullCollectionMN", new HashSet(Arrays.asList(new String[] {"Company", "CompanysContractors", "InterMineObject"})));

        results.put("ContainsConstraintObjectRefObject", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.departmentId = 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintObjectRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintNotObjectRefObject", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.departmentId != 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotObjectRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintCollectionRefObject", "SELECT a1_.id AS a1_id FROM Department AS a1_, Employee AS indirect0 WHERE a1_.id = indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Department", "Employee"})));
        results.put("ContainsConstraintNotCollectionRefObject", "SELECT a1_.id AS a1_id FROM Department AS a1_, Employee AS indirect0 WHERE a1_.id != indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Department", "Employee"})));
        results.put("ContainsConstraintMMCollectionRefObject", "SELECT a1_.id AS a1_id FROM Company AS a1_, CompanysContractors AS indirect0 WHERE a1_.id = indirect0.Companys AND indirect0.Contractors = 3 ORDER BY a1_.id");
        results2.put("ContainsConstraintMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company", "CompanysContractors"})));
        results.put("ContainsConstraintNotMMCollectionRefObject", new Failure(ObjectStoreException.class, "Cannot represent many-to-many collection DOES NOT CONTAIN in SQL")); //TODO: Fix this (ticket #445)
        results2.put("ContainsConstraintNotMMCollectionRefObject", NO_RESULT);
        results.put("SimpleConstraintNull", "SELECT a1_.id AS a1_id FROM Manager AS a1_ WHERE a1_.title IS NULL ORDER BY a1_.id");
        results2.put("SimpleConstraintNull", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Manager"})));
        results.put("SimpleConstraintNotNull", "SELECT a1_.id AS a1_id FROM Manager AS a1_ WHERE a1_.title IS NOT NULL ORDER BY a1_.id");
        results2.put("SimpleConstraintNotNull", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Manager"})));
        results.put("TypeCast", "SELECT DISTINCT (a1_.age)::TEXT AS a2_ FROM Employee AS a1_ ORDER BY (a1_.age)::TEXT");
        results2.put("TypeCast", Collections.singleton("Employee"));
        results.put("IndexOf", "SELECT STRPOS(a1_.name, 'oy') AS a2_ FROM Employee AS a1_ ORDER BY STRPOS(a1_.name, 'oy')");
        results2.put("IndexOf", Collections.singleton("Employee"));
        results.put("Substring", "SELECT SUBSTR(a1_.name, 2, 2) AS a2_ FROM Employee AS a1_ ORDER BY SUBSTR(a1_.name, 2, 2)");
        results2.put("Substring", Collections.singleton("Employee"));
        results.put("Substring2", "SELECT SUBSTR(a1_.name, 2) AS a2_ FROM Employee AS a1_ ORDER BY SUBSTR(a1_.name, 2)");
        results2.put("Substring2", Collections.singleton("Employee"));
        results.put("OrderByReference", "SELECT a1_.id AS a1_id, a1_.departmentId AS orderbyfield0 FROM Employee AS a1_ ORDER BY a1_.departmentId, a1_.id");
        results2.put("OrderByReference", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("FailDistinctOrder", new Failure(ObjectStoreException.class, "Field a1_.age in the ORDER BY list must be in the SELECT list, or the whole QueryClass org.intermine.model.testmodel.Employee must be in the SELECT list, or the query made non-distinct"));
        results2.put("FailDistinctOrder", NO_RESULT);
        results.put("FailDistinctOrder2", new Failure(ObjectStoreException.class, "Class a2_ in the ORDER BY list must be in the SELECT list, or its id, or the query made non-distinct"));
        results2.put("FailDistinctOrder2", NO_RESULT);

        String largeBagConstraintText = new BufferedReader(new InputStreamReader(TruncatedSqlGeneratorTest.class.getClassLoader().getResourceAsStream("largeBag.sql"))).readLine();
        results.put("LargeBagConstraint", largeBagConstraintText);
        results2.put("LargeBagConstraint", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));

        String largeNotBagConstraintText = new BufferedReader(new InputStreamReader(TruncatedSqlGeneratorTest.class.getClassLoader().getResourceAsStream("largeNotBag.sql"))).readLine();
        results.put("LargeBagNotConstraint", largeNotBagConstraintText);
        results2.put("LargeBagNotConstraint", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));

        results.put("LargeBagConstraintUsingTable", "SELECT a1_.id AS a1_id FROM Employee AS a1_, " + LARGE_BAG_TABLE_NAME + " AS indirect0 WHERE a1_.name = indirect0.value ORDER BY a1_.id");
        results2.put("LargeBagConstraintUsingTable", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));

        results.put("LargeBagNotConstraintUsingTable", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE (NOT (a1_.name IN (SELECT value FROM " + LARGE_BAG_TABLE_NAME + "))) ORDER BY a1_.id");
        results2.put("LargeBagNotConstraintUsingTable", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));

        results.put("NegativeNumbers", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.age > -51 ORDER BY a1_.id");
        results2.put("NegativeNumbers", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));

        results.put("Lower", "SELECT LOWER(a1_.name) AS a2_ FROM Employee AS a1_ ORDER BY LOWER(a1_.name)");
        results2.put("Lower", Collections.singleton("Employee"));

        results.put("Greatest", "SELECT GREATEST(2000,a1_.vatNumber) AS a2_ FROM Company AS a1_ ORDER BY GREATEST(2000,a1_.vatNumber)");
        results2.put("Greatest", Collections.singleton("Company"));

        results.put("Least", "SELECT LEAST(2000,a1_.vatNumber) AS a2_ FROM Company AS a1_ ORDER BY LEAST(2000,a1_.vatNumber)");
        results2.put("Least", Collections.singleton("Company"));

        results.put("Upper", "SELECT UPPER(a1_.name) AS a2_ FROM Employee AS a1_ ORDER BY UPPER(a1_.name)");
        results2.put("Upper", Collections.singleton("Employee"));
        results.put("CollectionQueryOneMany", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE " + departmentA1Id + " = a1_.departmentId ORDER BY a1_.id");
        results2.put("CollectionQueryOneMany", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("CollectionQueryManyMany", "SELECT a1_.id AS a1_id FROM Secretary AS a1_, HasSecretarysSecretarys AS indirect0 WHERE " + companyBId + " = indirect0.HasSecretarys AND indirect0.Secretarys = a1_.id ORDER BY a1_.id");
        results2.put("CollectionQueryManyMany", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Secretary", "HasSecretarysSecretarys"})));
        results.put("QueryClassBag", "SELECT a2_.departmentId AS a3_, a2_.id AS a2_id FROM Employee AS a2_ WHERE a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") ORDER BY a2_.departmentId, a2_.id");
        results2.put("QueryClassBag", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("QueryClassBagMM", "SELECT indirect0.HasSecretarys AS a3_, a2_.id AS a2_id FROM Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE indirect0.HasSecretarys IN (" + companyAId + ", " + companyBId + ", " + employeeB1Id + ") AND indirect0.Secretarys = a2_.id ORDER BY indirect0.HasSecretarys, a2_.id");
        results2.put("QueryClassBagMM", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Secretary", "HasSecretarysSecretarys"})));
        results.put("QueryClassBagNot", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results2.put("QueryClassBagNot", NO_RESULT);
        results.put("QueryClassBagNotMM", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results2.put("QueryClassBagNotMM", NO_RESULT);
        results.put("QueryClassBagDouble", "SELECT a2_.departmentId AS a4_, a2_.id AS a2_id, a3_.id AS a3_id FROM Employee AS a2_, Employee AS a3_ WHERE a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND a3_.departmentId = a2_.departmentId ORDER BY a2_.departmentId, a2_.id, a3_.id");
        results2.put("QueryClassBagDouble", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("QueryClassBagContainsObject", "SELECT indirect0.departmentId AS a2_ FROM Employee AS indirect0 WHERE indirect0.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND indirect0.id = " + employeeA1Id + " ORDER BY indirect0.departmentId");
        results2.put("QueryClassBagContainsObject", Collections.singleton("Employee"));
        results.put("QueryClassBagContainsObjectDouble", "SELECT indirect0.departmentId AS a2_ FROM Employee AS indirect0, Employee AS indirect1 WHERE indirect0.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND indirect0.id = " + employeeA1Id + " AND indirect1.departmentId = indirect0.departmentId AND indirect1.id = " + employeeA2Id + " ORDER BY indirect0.departmentId");
        results2.put("QueryClassBagContainsObjectDouble", Collections.singleton("Employee"));
        results.put("QueryClassBagNotContainsObject", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results2.put("QueryClassBagNotContainsObject", NO_RESULT);
        results.put("ObjectContainsObject", "SELECT 'hello' AS a1_ FROM Employee AS indirect0 WHERE " + departmentA1Id + " = indirect0.departmentId AND indirect0.id = " + employeeA1Id );
        results2.put("ObjectContainsObject", Collections.singleton("Employee"));
        results.put("ObjectContainsObject2", "SELECT 'hello' AS a1_ FROM Employee AS indirect0 WHERE " + departmentA1Id + " = indirect0.departmentId AND indirect0.id = " + employeeB1Id);
        results2.put("ObjectContainsObject2", Collections.singleton("Employee"));
        results.put("ObjectNotContainsObject", "SELECT 'hello' AS a1_ FROM Employee AS indirect0 WHERE " + departmentA1Id + " != indirect0.departmentId AND indirect0.id = " + employeeA1Id);
        results2.put("ObjectNotContainsObject", Collections.singleton("Employee"));
        results.put("QueryClassBagNotViaNand", new Failure(ObjectStoreException.class, "Invalid constraint: QueryClassBag ContainsConstraint cannot be inside an OR ConstraintSet"));
        results2.put("QueryClassBagNotViaNand", NO_RESULT);
        results.put("QueryClassBagNotViaNor", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results2.put("QueryClassBagNotViaNor", NO_RESULT);
        results.put("SubqueryExistsConstraint", "SELECT 'hello' AS a1_ WHERE EXISTS(SELECT a1_.id FROM Company AS a1_)");
        results2.put("SubqueryExistsConstraint", Collections.singleton("Company"));
        results.put("NotSubqueryExistsConstraint", "SELECT 'hello' AS a1_ WHERE (NOT EXISTS(SELECT a1_.id FROM Company AS a1_))");
        results2.put("NotSubqueryExistsConstraint", Collections.singleton("Company"));
        results.put("SubqueryExistsConstraintNeg", "SELECT 'hello' AS a1_ WHERE EXISTS(SELECT a1_.id FROM Bank AS a1_)");
        results2.put("SubqueryExistsConstraintNeg", Collections.singleton("Bank"));
        results.put("ObjectPathExpression", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("ObjectPathExpression", new HashSet(Arrays.asList("InterMineObject", "Employee")));
        results.put("ObjectPathExpression2", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("ObjectPathExpression2", new HashSet(Arrays.asList("InterMineObject", "Employee")));
        results.put("ObjectPathExpression3", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("ObjectPathExpression3", new HashSet(Arrays.asList("InterMineObject", "Employee", "Department")));
        results.put("ObjectPathExpression4", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("ObjectPathExpression4", new HashSet(Arrays.asList("InterMineObject", "Employee", "Department", "Company")));
        results.put("ObjectPathExpression5", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("ObjectPathExpression5", new HashSet(Arrays.asList("InterMineObject", "Employee", "Department", "Company")));
        results.put("FieldPathExpression", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("FieldPathExpression", new HashSet(Arrays.asList("InterMineObject", "Company", "CEO")));
        results.put("FieldPathExpression2", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("FieldPathExpression2", new HashSet(Arrays.asList("InterMineObject", "Employee", "Department", "Company", "Address")));
        results.put("CollectionPathExpression", "SELECT a1_.id AS a1_id FROM Department AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression", new HashSet(Arrays.asList("InterMineObject", "Employee", "Department")));
        results.put("CollectionPathExpression2", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression2", new HashSet(Arrays.asList("InterMineObject", "Employee", "Department")));
        results.put("CollectionPathExpression3", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression3", new HashSet(Arrays.asList("InterMineObject", "Company", "Department", "Employee")));
        results.put("CollectionPathExpression4", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression4", new HashSet(Arrays.asList("InterMineObject", "Company", "Department", "Employee")));
        results.put("CollectionPathExpression5", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression5", new HashSet(Arrays.asList("InterMineObject", "Company", "Department")));
        results.put("CollectionPathExpression6", "SELECT a1_.id AS a1_id FROM Department AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression6", new HashSet(Arrays.asList("InterMineObject", "Company", "Department")));
        results.put("CollectionPathExpression7", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("CollectionPathExpression7", new HashSet(Arrays.asList("InterMineObject", "Company", "Department", "Employee")));
        results.put("OrSubquery", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE (a1_.id IN (SELECT a1_.id FROM Company AS a1_ UNION SELECT a1_.id FROM Manager AS a1_)) ORDER BY a1_.id");
        results2.put("OrSubquery", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company", "Manager"})));
        results.put("ScientificNumber", "SELECT a1_.id AS a1_id FROM Types AS a1_ WHERE a1_.doubleType < 1.3432E24 AND a1_.floatType > -8.56E-32::REAL ORDER BY a1_.id");
        results2.put("ScientificNumber", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Types"})));
        results.put("LowerBag", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE LOWER(a1_.name) IN ('employeea1', 'employeea2', 'employeeb1') ORDER BY a1_.id");
        results2.put("LowerBag", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("FetchBag", "SELECT value AS a1_ FROM osbag_int WHERE bagid = 5 ORDER BY value");
        results2.put("FetchBag", Collections.singleton("osbag_int"));
        results.put("ObjectStoreBag", "SELECT a1_.id AS a1_id FROM Employee AS a1_, osbag_int AS indirect0 WHERE a1_.id = indirect0.value AND indirect0.bagid = 5 ORDER BY a1_.id");
        results2.put("ObjectStoreBag", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee", "osbag_int"})));
        results.put("ObjectStoreBagQueryClass", NO_RESULT);
        results.put("OrderDescending", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id DESC");
        results2.put("OrderDescending", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ObjectStoreBagCombination", "SELECT DISTINCT value AS a1_ FROM osbag_int WHERE bagid IN (5, 6)");
        results2.put("ObjectStoreBagCombination", Collections.singleton("osbag_int"));
        results.put("ObjectStoreBagCombination2", "SELECT value AS a1_ FROM osbag_int WHERE bagid = 5 INTERSECT SELECT value AS a1_ FROM osbag_int WHERE bagid = 6 ORDER BY a1_");
        results2.put("ObjectStoreBagCombination2", Collections.singleton("osbag_int"));
        results.put("ObjectStoreBagsForObject", "SELECT bagid AS a1_ FROM osbag_int WHERE value = 6 ORDER BY bagid");
        results2.put("ObjectStoreBagsForObject", Collections.singleton("osbag_int"));
        results.put("ObjectStoreBagsForObject2", "SELECT bagid AS a1_ FROM osbag_int WHERE value = 6 AND bagid IN (10, 11, 12) ORDER BY bagid");
        results2.put("ObjectStoreBagsForObject2", Collections.singleton("osbag_int"));
        results.put("SelectForeignKey", "SELECT a1_.departmentId AS a2_ FROM Employee AS a1_ ORDER BY a1_.departmentId");
        results2.put("SelectForeignKey", Collections.singleton("Employee"));
        results.put("WhereCount", "SELECT a1_.id AS a1_id, COUNT(*) AS a3_ FROM Department AS a1_, Employee AS a2_ WHERE a1_.id = a2_.departmentId GROUP BY a1_.companyId, a1_.id, a1_.managerId, a1_.name HAVING COUNT(*) > 1 ORDER BY a1_.id, a3_");
        results2.put("WhereCount", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Department", "Employee"})));
        results.put("LimitedSubquery", "SELECT DISTINCT a1_.a2_ AS a2_ FROM (SELECT a1_.name AS a2_ FROM Employee AS a1_ LIMIT 3) AS a1_ ORDER BY a1_.a2_");
        results2.put("LimitedSubquery", Collections.singleton("Employee"));
        results.put("ObjectStoreBagCombination3", "SELECT value AS a1_ FROM osbag_int WHERE bagid IN (5, 6) GROUP BY value HAVING COUNT(*) < 2 ORDER BY value");
        results2.put("ObjectStoreBagCombination3", Collections.singleton("osbag_int"));
        results.put("TotallyFalse", new Failure(CompletelyFalseException.class, null));
        results2.put("TotallyFalse", Collections.EMPTY_SET);
        results.put("TotallyTrue", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        results2.put("TotallyTrue", new HashSet(Arrays.asList("InterMineObject", "Employee")));
        results.put("MergeFalse", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE (a1_.age > 3) ORDER BY a1_.id");
        results2.put("MergeFalse", new HashSet(Arrays.asList("InterMineObject", "Employee")));
        results.put("MergeTrue", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.age > 3 ORDER BY a1_.id");
        results2.put("MergeTrue", new HashSet(Arrays.asList("InterMineObject", "Employee")));
        results.put("EmptyBagConstraint", new Failure(CompletelyFalseException.class, null));
        results2.put("EmptyBagConstraint", Collections.EMPTY_SET);
        results.put("SelectFunctionNoGroup", "SELECT MIN(a1_.id) AS a2_ FROM Employee AS a1_");
        results2.put("SelectFunctionNoGroup", Collections.singleton("Employee"));
        results.put("SelectClassFromInterMineObject", "SELECT a1_.class AS a2_, COUNT(*) AS a3_ FROM InterMineObject AS a1_ GROUP BY a1_.class ORDER BY a1_.class, a3_");
        results2.put("SelectClassFromInterMineObject", Collections.singleton("InterMineObject"));
        results.put("SelectClassFromEmployee", "SELECT a1_.class AS a2_, COUNT(*) AS a3_ FROM Employee AS a1_ GROUP BY a1_.class ORDER BY a1_.class, a3_");
        results2.put("SelectClassFromEmployee", Collections.singleton("Employee"));
        results.put("SubclassCollection", "SELECT a1_.id AS a1_id FROM Department AS a1_ ORDER BY a1_.id");
        results2.put("SubclassCollection", new HashSet(Arrays.asList("InterMineObject", "Department", "Manager")));
        results.put("SelectWhereBackslash", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.name = E'Fred\\\\Blog\\'s' ORDER BY a1_.id");
        results2.put("SelectWhereBackslash", new HashSet(Arrays.asList("Employee", "InterMineObject")));
        results.put("MultiColumnObjectInCollection", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("MultiColumnObjectInCollection", new HashSet(Arrays.asList("Company", "InterMineObject", "Department", "Contractor", "CompanysContractors")));
        results.put("ConstrainClass1", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY a1_.id");
        results2.put("ConstrainClass1", new HashSet(Arrays.asList("InterMineObject")));
        results.put("ConstrainClass2", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class IN ('org.intermine.model.testmodel.Company', 'org.intermine.model.testmodel.Employee') ORDER BY a1_.id");
        results2.put("ConstrainClass2", new HashSet(Arrays.asList("InterMineObject")));
        results.put("MultipleInBagConstraint1", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE (a1_.intermine_end IN ('1', '2', 'EmployeeA1', 'EmployeeB1') OR a1_.name IN ('1', '2', 'EmployeeA1', 'EmployeeB1')) ORDER BY a1_.id");
        results2.put("MultipleInBagConstraint1", new HashSet(Arrays.asList("Employee", "InterMineObject")));

        // results for range queries depend on capabilities of the database, each variant is also tested in testOverlapQueries
        DatabaseSchema schema = ((ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest")).getSchema();
        String method = "default";
        if (schema.useRangeTypes()) {
            method = "int4range";
        } else if (schema.hasBioSeg()) {
            method = "bioseg";
        }
        results.put("RangeOverlaps", getOverlapQuery(method, "RangeOverlaps"));
        results2.put("RangeOverlaps", new HashSet(Arrays.asList("intermine_Range")));
        results.put("RangeDoesNotOverlap", getOverlapQuery(method, "RangeDoesNotOverlap"));
        results2.put("RangeDoesNotOverlap", new HashSet(Arrays.asList("intermine_Range")));
        results.put("RangeOverlapsValues", getOverlapQuery(method, "RangeOverlapsValues"));
        results2.put("RangeOverlapsValues", new HashSet(Arrays.asList("intermine_Range")));
    }

    final static String LARGE_BAG_TABLE_NAME = "large_string_bag_table";


    // expected SQL for overlap queries depends on capabilities of the database,
    // each variant is also tested in testOverlapQueries below.
    private static String getOverlapQuery(String method, String queryName) {
       if (rangeQueries == null) {
           rangeQueries = new HashMap<String, Map<String, String>>();

           // int4range
           rangeQueries.put("int4range", new HashMap<String, String>());
           rangeQueries.get("int4range").put("RangeOverlaps", "SELECT a1_.id AS a3_, a2_.id AS a4_ FROM intermine_Range AS a1_, intermine_Range AS a2_ WHERE a1_.parentId = a2_.parentId AND int4range(a1_.rangeStart, a1_.rangeEnd) && int4range(a2_.rangeStart, a2_.rangeEnd) ORDER BY a1_.id, a2_.id");
           rangeQueries.get("int4range").put("RangeDoesNotOverlap", "SELECT a1_.id AS a3_, a2_.id AS a4_ FROM intermine_Range AS a1_, intermine_Range AS a2_ WHERE (NOT (a1_.parentId = a2_.parentId AND int4range(a1_.rangeStart, a1_.rangeEnd) && int4range(a2_.rangeStart, a2_.rangeEnd))) ORDER BY a1_.id, a2_.id");
           rangeQueries.get("int4range").put("RangeOverlapsValues", "SELECT a1_.id AS a2_ FROM intermine_Range AS a1_ WHERE a1_.parentId = a1_.parentId AND int4range(a1_.rangeStart, a1_.rangeEnd) && int4range(35, 45) ORDER BY a1_.id");

           // bioseg
           rangeQueries.put("bioseg", new HashMap<String, String>());
           rangeQueries.get("bioseg").put("RangeOverlaps", "SELECT a1_.id AS a3_, a2_.id AS a4_ FROM intermine_Range AS a1_, intermine_Range AS a2_ WHERE a1_.parentId = a2_.parentId AND bioseg_create(a1_.rangeStart, a1_.rangeEnd) && bioseg_create(a2_.rangeStart, a2_.rangeEnd) ORDER BY a1_.id, a2_.id");
           rangeQueries.get("bioseg").put("RangeDoesNotOverlap", "SELECT a1_.id AS a3_, a2_.id AS a4_ FROM intermine_Range AS a1_, intermine_Range AS a2_ WHERE (NOT (a1_.parentId = a2_.parentId AND bioseg_create(a1_.rangeStart, a1_.rangeEnd) && bioseg_create(a2_.rangeStart, a2_.rangeEnd))) ORDER BY a1_.id, a2_.id");
           rangeQueries.get("bioseg").put("RangeOverlapsValues", "SELECT a1_.id AS a2_ FROM intermine_Range AS a1_ WHERE a1_.parentId = a1_.parentId AND bioseg_create(a1_.rangeStart, a1_.rangeEnd) && bioseg_create(35, 45) ORDER BY a1_.id");

           // default
           rangeQueries.put("default", new HashMap<String, String>());
           rangeQueries.get("default").put("RangeOverlaps", "SELECT a1_.id AS a3_, a2_.id AS a4_ FROM intermine_Range AS a1_, intermine_Range AS a2_ WHERE a1_.parentId = a2_.parentId AND a1_.rangeStart <= a2_.rangeEnd AND a1_.rangeEnd >= a2_.rangeStart ORDER BY a1_.id, a2_.id");
           rangeQueries.get("default").put("RangeDoesNotOverlap", "SELECT a1_.id AS a3_, a2_.id AS a4_ FROM intermine_Range AS a1_, intermine_Range AS a2_ WHERE (NOT (a1_.parentId = a2_.parentId AND a1_.rangeStart <= a2_.rangeEnd AND a1_.rangeEnd >= a2_.rangeStart)) ORDER BY a1_.id, a2_.id");
           rangeQueries.get("default").put("RangeOverlapsValues", "SELECT a1_.id AS a2_ FROM intermine_Range AS a1_ WHERE a1_.parentId = a1_.parentId AND a1_.rangeStart <= 45 AND a1_.rangeEnd >= 35 ORDER BY a1_.id");
       }

       return rangeQueries.get(method).get(queryName);
    }

    public void executeTest(String type) throws Exception {
        Query q = (Query) queries.get(type);
        Object expected = results.get(type);

        if (expected instanceof Failure) {
            try {
                SqlGenerator.generate(q, 0, Integer.MAX_VALUE, getSchema(), db, new HashMap());
                fail(type + " was expected to fail");
            } catch (Exception e) {
                assertEquals(type + " was expected to produce a particular exception", expected, new Failure(e));
            }
        } else {
            Map bagTableNames = new HashMap();

            if (type.matches("LargeBag.*UsingTable")) {
                // special case - the Map will tell generate() what table to use to find the values
                // of large bags
                Query largeBagQuery = (Query) queries.get(type);
                BagConstraint largeBagConstraint = (BagConstraint) largeBagQuery.getConstraint();
                bagTableNames.put(largeBagConstraint, "large_string_bag_table");
            }

            String generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, getSchema(), db,
                                                     bagTableNames);
            if (expected instanceof String) {
                assertEquals("", results.get(type), generated);
            } else if (expected instanceof Collection) {
                boolean hasEqual = false;
                Iterator expectedIter = ((Collection) expected).iterator();
                while ((!hasEqual) && expectedIter.hasNext()) {
                    Object expectedStringObj = expectedIter.next();
                    if (expectedStringObj instanceof String) {
                        hasEqual = expectedStringObj.equals(generated);
                    } else {
                        throw new ClassCastException("Expected string, but was " + expectedStringObj.getClass() + " - \"" + expectedStringObj + "\"");
                    }
                }
                assertTrue(generated, hasEqual);
            } else {
                fail("No result found for " + type);
            }

            // Sql containing sub-queries or range constraints can't be parsed by the optimisier,
            // we don't want to test precomputing for these.

            if (!("SubqueryExistsConstraint".equals(type)
                    || "NotSubqueryExistsConstraint".equals(type)
                    || "SubqueryExistsConstraintNeg".equals(type)
                    || "ObjectStoreBagCombination2".equals(type)
                    || "ContainsConstraintNullCollection1N".equals(type)
                    || "ContainsConstraintNotNullCollection1N".equals(type)
                    || "ContainsConstraintNullCollectionMN".equals(type)
                    || "ContainsConstraintNotNullCollectionMN".equals(type)
                    || "RangeDoesNotOverlap".equals(type)
                    || "RangeOverlapsValues".equals(type)
                    || "RangeOverlaps".equals(type))) {

                // And check that the SQL generated is high enough quality to be parsed by the
                // optimiser.
                org.intermine.sql.query.Query sql = new org.intermine.sql.query.Query(generated);
                if (!"LargeBagNotConstraintUsingTable".equals(type)) {
                    // Also check to see that the optimiser doesn't barf on them.
                    //PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
                    //assertFalse(ptm.getPrecomputedTables().isEmpty());
                    String ptsql = "SELECT a1_.id AS id, a1_.name AS name FROM Employee AS a1_ ORDER BY a1_.id";
                    Connection c = null;
                    try {
                        c = db.getConnection();
                        PrecomputedTable pt = new PrecomputedTable(new org.intermine.sql.query.Query(ptsql), ptsql, "fred", "bob", c);
                        QueryOptimiser.recursiveOptimise(Collections.singleton(pt), sql, new BestQueryStorer(), sql);
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
        if (results2.get(type) != NO_RESULT) {
            assertEquals(results2.get(type), SqlGenerator.findTableNames(q, getSchema(), false));
        }
    }

    public void testSelectQueryValue() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue("Hello");
        QueryValue v3 = new QueryValue(new Date(1046275720000l));
        QueryValue v4 = new QueryValue(Boolean.TRUE);

        StringBuffer buffer = new StringBuffer();

        SqlGenerator.State state = new SqlGenerator.State();

        SqlGenerator.queryEvaluableToString(buffer, v1, null, state);
        SqlGenerator.queryEvaluableToString(buffer, v2, null, state);
        SqlGenerator.queryEvaluableToString(buffer, v3, null, state);
        SqlGenerator.queryEvaluableToString(buffer, v4, null, state);
        assertEquals("5'Hello'1046275720000'true'", buffer.toString());
    }

    /** Expect Underscores to not be escaped in LIKE queries **/
    public void testQueryValueUnderscoreInMatch() throws Exception {
        QueryValue right = new QueryValue("%Hello_World");
        QueryValue left  = new QueryValue("Hello-World");
        SimpleConstraint con = new SimpleConstraint(left, ConstraintOp.MATCHES, right);

        SqlGenerator.State state = new SqlGenerator.State();
        StringBuffer buffer = state.getWhereBuffer();
        SqlGenerator.simpleConstraintToString(state, buffer, con, null);
        assertEquals("'Hello-World' LIKE '%Hello_World'", buffer.toString());
    }

    /** Expect Underscores to not be escaped in LIKE queries **/
    public void testQueryValueEscapedOpsInMatch() throws Exception {
        QueryValue right = new QueryValue("\\%Hello_Under\\_Score%");
        QueryValue left  = new QueryValue("%Hello-Under_Score!");
        SimpleConstraint con = new SimpleConstraint(left, ConstraintOp.MATCHES, right);

        SqlGenerator.State state = new SqlGenerator.State();
        StringBuffer buffer = state.getWhereBuffer();
        SqlGenerator.simpleConstraintToString(state, buffer, con, null);
        String expected = "'%Hello-Under_Score!' LIKE E'\\\\%Hello_Under\\\\_Score%'";
        assertEquals(expected, buffer.toString());
    }

    /** Expect underscores to be unescaped in other queries **/
    public void testQueryValueUnderscoreInEquals() throws Exception {
        QueryValue right = new QueryValue("Hello_World");
        QueryValue left  = new QueryValue("HelloXWorld");
        SimpleConstraint con = new SimpleConstraint(left, ConstraintOp.EQUALS, right);

        SqlGenerator.State state = new SqlGenerator.State();

        StringBuffer buffer = state.getWhereBuffer();
        SqlGenerator.simpleConstraintToString(state, buffer, con, null);
        assertEquals("'HelloXWorld' = 'Hello_World'", buffer.toString());
    }

    public void testSelectQueryExpression() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue(new Integer(7));
        QueryExpression e1 = new QueryExpression(v1, QueryExpression.ADD, v2);
        QueryExpression e2 = new QueryExpression(v1, QueryExpression.SUBTRACT, v2);
        QueryExpression e3 = new QueryExpression(v1, QueryExpression.MULTIPLY, v2);
        QueryExpression e4 = new QueryExpression(v1, QueryExpression.DIVIDE, v2);
        StringBuffer buffer = new StringBuffer();

        SqlGenerator.State state = new SqlGenerator.State();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, state);
        SqlGenerator.queryEvaluableToString(buffer, e2, null, state);
        SqlGenerator.queryEvaluableToString(buffer, e3, null, state);
        SqlGenerator.queryEvaluableToString(buffer, e4, null, state);
        assertEquals("(5 + 7)(5 - 7)(5 * 7)(5 / 7)", buffer.toString());
    }

    public void testSelectQuerySubstringExpression() throws Exception {
        QueryValue v1 = new QueryValue("Hello");
        QueryValue v2 = new QueryValue(new Integer(3));
        QueryValue v3 = new QueryValue(new Integer(5));
        QueryExpression e1 = new QueryExpression(v1, v2, v3);
        StringBuffer buffer = new StringBuffer();

        SqlGenerator.State state = new SqlGenerator.State();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, state);
        assertEquals("SUBSTR('Hello', 3, 5)", buffer.toString());
    }

    public void testSelectQueryExpressionGreatestLeast() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue(new Integer(7));
        QueryExpression e1 = new QueryExpression(v1, QueryExpression.GREATEST, v2);
        QueryExpression e2 = new QueryExpression(v1, QueryExpression.LEAST, v2);
        StringBuffer buffer = new StringBuffer();

        SqlGenerator.State state = new SqlGenerator.State();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, state);
        buffer.append(", ");
        SqlGenerator.queryEvaluableToString(buffer, e2, null, state);
        assertEquals("GREATEST(5,7), LEAST(5,7)", buffer.toString());
    }

    /* TODO
    public void testSelectQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, f1, q1);
        assertEquals("a1_.name", buffer.toString());
    }

    public void testSelectQueryFunction() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryField v1 = new QueryField(c1, "vatNumber");
        QueryFunction f1 = new QueryFunction();
        QueryFunction f2 = new QueryFunction(v1, QueryFunction.SUM);
        QueryFunction f3 = new QueryFunction(v1, QueryFunction.AVERAGE);
        QueryFunction f4 = new QueryFunction(v1, QueryFunction.MIN);
        QueryFunction f5 = new QueryFunction(v1, QueryFunction.MAX);
        Query q1 = new Query();
        q1.addFrom(c1);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, f1, q1);
        SqlGenerator.queryEvaluableToString(buffer, f2, q1);
        SqlGenerator.queryEvaluableToString(buffer, f3, q1);
        SqlGenerator.queryEvaluableToString(buffer, f4, q1);
        SqlGenerator.queryEvaluableToString(buffer, f5, q1);
        assertEquals("COUNT(*)SUM(a1_.vatNumber)AVG(a1_.vatNumber)MIN(a1_.vatNumber)MAX(a1_.vatNumber)", buffer.toString());
    }
*/

    public void testInvalidClass() throws Exception {
        Query q = new Query();
        QueryClass c1 = new QueryClass(Company.class);
        q.addFrom(c1);
        q.addToSelect(c1);
        DatabaseSchema s = new DatabaseSchema(new Model("nothing", "", new HashSet()), Collections.EMPTY_LIST, false, Collections.EMPTY_SET, 1, false, false);
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, s, db, new HashMap());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("interface org.intermine.model.testmodel.Company is not in the model", e.getMessage());
        }
        try {
            SqlGenerator.findTableNames(q, s, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("interface org.intermine.model.testmodel.Company is not in the model", e.getMessage());
        }
    }

    public void testInvalidFromElement() throws Exception {
        Query q = new Query();
        FromElement fe = new FromElement() {};
        q.addFrom(fe);
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, getSchema(), db, new HashMap());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertTrue(e.getMessage().startsWith("Unknown FromElement: "));
        }
        try {
            SqlGenerator.findTableNames(q, getSchema(), false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertTrue(e.getMessage().startsWith("Unknown FromElement: "));
        }
    }

    public void testInvalidConstraintType() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new Constraint() {});
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, getSchema(), db, new HashMap());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Unrecognised object "));
        }
        try {
            SqlGenerator.findTableNames(q, getSchema(), false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Unknown constraint "));
        }
    }

    public void testInvalidClassConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Company c = (Company) DynamicUtil.createObject(Company.class);
        q.setConstraint(new ClassConstraint(qc, ConstraintOp.EQUALS, c));
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, getSchema(), db, new HashMap());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("ClassConstraint cannot contain an InterMineObject without an ID set", e.getMessage());
        }
    }

    public void testInvalidClassInContainsConstraint() throws Exception {
        Employee emp = new Employee() {
            private Set extras;
            public Set getExtras() {
                return extras;
            }
            public void setExtras(Set extras) {
                this.extras = extras;
            }
            public void addExtras(Employee e) {
                extras.add(e);
            }
        };
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(emp, "extras"), ConstraintOp.CONTAINS, qc));
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, getSchema(), db, new HashMap());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Reference ?.extras is not in the model - fields available in class org.intermine.objectstore.intermine.SqlGeneratorTest$3 are [fullTime, age, end, department, departmentThatRejectedMe, employmentPeriod, simpleObjects, address, id, name]", e.getMessage());
        }
    }


    public void testRegisterOffset() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = new Query();
        QueryClass c1 = new QueryClass(Company.class);
        q.addFrom(c1);
        q.addToSelect(c1);
        assertEquals("SQL incorrect.", getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        SqlGenerator.registerOffset(q, 5, schema, db, new Integer(10), new HashMap());
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, new HashMap()));
        SqlGenerator.registerOffset(q, 11000, schema, db, new Integer(20), new HashMap());
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 20 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 11005, Integer.MAX_VALUE, schema, db, new HashMap()));
        SqlGenerator.registerOffset(q, 21000, schema, db, new Integer(30), new HashMap());
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 11000", SqlGenerator.generate(q, 11005, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 30 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 21005, Integer.MAX_VALUE, schema, db, new HashMap()));
        SqlGenerator.registerOffset(q, 21005, schema, db, new Integer(31), new HashMap());
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 11000", SqlGenerator.generate(q, 11005, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 30 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 21005, Integer.MAX_VALUE, schema, db, new HashMap()));
        SqlGenerator.registerOffset(q, 11002, schema, db, new Integer(29), new HashMap());
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 11000", SqlGenerator.generate(q, 11005, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 30 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 21005, Integer.MAX_VALUE, schema, db, new HashMap()));
        SqlGenerator.registerOffset(q, 101000, schema, db, new Integer(40), new HashMap());
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 11000", SqlGenerator.generate(q, 11005, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 10 ORDER BY a1_.id OFFSET 21000", SqlGenerator.generate(q, 21005, Integer.MAX_VALUE, schema, db, new HashMap()));
        assertEquals(getRegisterOffset2() + "a1_.id > 40 ORDER BY a1_.id OFFSET 5", SqlGenerator.generate(q, 101005, Integer.MAX_VALUE, schema, db, new HashMap()));
    }

    public void testRegisterOffset2() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        q.addToSelect(f);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM " + getRegisterOffset3() + " ORDER BY a1_.name", SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
        SqlGenerator.registerOffset(q, 5, schema, db, "flibble", Collections.EMPTY_MAP);
        String expected = "SELECT DISTINCT a1_.name AS a2_ FROM "
            + getRegisterOffset3() + " " + getRegisterOffset4()
            + " (a1_.name > 'flibble' OR a1_.name IS NULL) ORDER BY a1_.name OFFSET 5";
        assertEquals(expected,
                     SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));

        q = new Query();
        qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        f = new QueryField(qc, "age");
        q.addToSelect(f);
        assertEquals("SELECT DISTINCT a1_.age AS a2_ FROM " + getRegisterOffset3() + " ORDER BY a1_.age", SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
        SqlGenerator.registerOffset(q, 5, schema, db, new Integer(34), Collections.EMPTY_MAP);
        assertEquals("SELECT DISTINCT a1_.age AS a2_ FROM " + getRegisterOffset3() + " " + getRegisterOffset4() + " a1_.age > 34 ORDER BY a1_.age OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
    }

    // Large offset code adds constraint that value > x and is not null to deal with ordering in postgres
    // check that is doesn't break the query by adding 'value is null' when the query already contains
    // 'value is null'
    public void testRegisterOffset3() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        q.addToSelect(f);
        SimpleConstraint sc = new SimpleConstraint(f, ConstraintOp.IS_NOT_NULL);
        q.setConstraint(sc);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM " + getRegisterOffset3() + " " + getRegisterOffset4() + " a1_.name IS NOT NULL ORDER BY a1_.name", SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
        SqlGenerator.registerOffset(q, 5, schema, db, "flibble", Collections.EMPTY_MAP);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM " + getRegisterOffset3() + " " + getRegisterOffset4() + " a1_.name IS NOT NULL AND a1_.name > 'flibble' ORDER BY a1_.name OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
    }

    public void testRegisterOffset4() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        q.addToSelect(f);
        q.addToOrderBy(new OrderDescending(f));
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM " + getRegisterOffset3() + " ORDER BY a1_.name DESC", SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
        SqlGenerator.registerOffset(q, 5, schema, db, "flibble", Collections.EMPTY_MAP);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM " + getRegisterOffset3() + " " + getRegisterOffset4() + " a1_.name < 'flibble' ORDER BY a1_.name DESC OFFSET 5", SqlGenerator.generate(q, 10, Integer.MAX_VALUE, schema, db, Collections.EMPTY_MAP));
    }

    public void testForPrecomp() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = (Query) queries.get("SelectSimpleObject");
        assertEquals("SQL incorrect.", precompTableString(), SqlGenerator.generate(q, schema, db, null, SqlGenerator.QUERY_FOR_PRECOMP, Collections.EMPTY_MAP));
    }

    public void testInvalidSafenesses() throws Exception {
        try {
            SqlGenerator.constraintToString(null, null, null, null, null, 3, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Unknown ContainsConstraint safeness: 3", e.getMessage());
        }
        try {
            SqlGenerator.constraintSetToString(null, null, null, null, null, 3, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Unknown ContainsConstraint safeness: 3", e.getMessage());
        }
        try {
            SqlGenerator.containsConstraintToString(null, null, null, null, null, 3, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Unknown ContainsConstraint safeness: 3", e.getMessage());
        }
    }

    public void testWhereHavingSafe() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryField qf1 = new QueryField(qc1, "end");
        QueryField qf2 = new QueryField(qc1, "name");
        QueryFunction qf3 = new QueryFunction();
        QueryField qf4 = new QueryField(qc1, "age");
        q.addFrom(qc1);
        q.addToSelect(qf1);
        q.addToGroupBy(qf1);
        assertArrayEquals(new boolean[] {true, true}, SqlGenerator.whereHavingSafe(qf1, q));
        assertArrayEquals(new boolean[] {true, false}, SqlGenerator.whereHavingSafe(qf2, q));
        assertArrayEquals(new boolean[] {false, true}, SqlGenerator.whereHavingSafe(qf3, q));
        assertArrayEquals(new boolean[] {true, false}, SqlGenerator.whereHavingSafe(qf4, q));
        assertArrayEquals(new boolean[] {true, false}, SqlGenerator.whereHavingSafe(new SimpleConstraint(qf1, ConstraintOp.EQUALS, qf2), q));
        assertArrayEquals(new boolean[] {false, false}, SqlGenerator.whereHavingSafe(new SimpleConstraint(qf3, ConstraintOp.EQUALS, new QueryCast(qf4, Long.class)), q));

    }

    public void testIrrelevantBag() throws Exception {
        QueryClass qc = new QueryClass(Employee.class);
        QueryField qf = new QueryField(qc, "name");
        Set bag = new HashSet();
        bag.add(new Integer(3));
        BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, bag);
        try {
            SqlGenerator.completelyFalse(bc);
            fail("Expected exception");
        } catch (ObjectStoreException e) {
        }
    }

    // range query results are different depending on available features in database schema so
    // need to test separately.
    public void testOverlapQueries() throws Exception {
        DatabaseSchema schema = getSchema();
        boolean originalUseRangeTypes = schema.useRangeTypes();
        boolean originalBioSeg = schema.hasBioSeg();

        try {
            Query q1 = rangeDoesNotOverlap();
            String generated1 = SqlGenerator.generate(q1, 0, Integer.MAX_VALUE, getSchema(), db, new HashMap());

            // 1. We can use Postgres built in range types
            Query q = rangeOverlaps();
            String generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            String expected = getOverlapQuery("int4range", "RangeOverlaps");
            assertEquals(expected, generated);

            q = rangeDoesNotOverlap();
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            expected = getOverlapQuery("int4range", "RangeDoesNotOverlap");
            assertEquals(expected, generated);

            q = rangeOverlapsValues();
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            expected = getOverlapQuery("int4range", "RangeOverlapsValues");
            assertEquals(expected, generated);

            // 2. if no range column but we have bioseg then use that
            schema.useRangeTypes = false;
            schema.hasBioSeg = true;

            // here we can use queries from map
            q = rangeOverlaps();
            expected = getOverlapQuery("bioseg", "RangeOverlaps");
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            assertEquals(expected, generated);

            q = rangeDoesNotOverlap();
            expected = getOverlapQuery("bioseg", "RangeDoesNotOverlap");
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            assertEquals(expected, generated);

            q = rangeOverlapsValues();
            expected = getOverlapQuery("bioseg", "RangeOverlapsValues");
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            assertEquals(expected, generated);

            // 3. not range column or bioseg so fall back to simple constraints on start/end
            schema.useRangeTypes = false;
            schema.hasBioSeg = false;
            q = rangeOverlaps();
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            expected = getOverlapQuery("default", "RangeOverlaps");
            assertEquals(expected, generated);

            q = rangeDoesNotOverlap();
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            expected = getOverlapQuery("default", "RangeDoesNotOverlap");
            assertEquals(expected, generated);

            q = rangeOverlapsValues();
            generated = SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap());
            expected = getOverlapQuery("default", "RangeOverlapsValues");
            assertEquals(expected, generated);
        } finally {
            // reset schema rangeDefs & bioseg when finished
            schema.useRangeTypes = originalUseRangeTypes;
            schema.hasBioSeg = originalBioSeg;
        }
    }

    private void assertArrayEquals(boolean arg1[], boolean arg2[]) {
        String s1 = "(" + arg1[0] + ", " + arg1[1] + ")";
        String s2 = "(" + arg2[0] + ", " + arg2[1] + ")";
        assertEquals(s1, s2);
    }

    protected DatabaseSchema getSchema() throws Exception {
        return ((ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest")).getSchema();
    }

    public String getRegisterOffset1() {
        return "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id";
    }
    public String getRegisterOffset2() {
        return "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE ";
    }
    public String getRegisterOffset3() {
        return "Employee AS a1_";
    }
    public String getRegisterOffset4() {
        return "WHERE";
    }
    public String precompTableString() {
        return "SELECT intermine_Alias.CEOId AS \"intermine_Aliasceoid\", intermine_Alias.addressId AS \"intermine_Aliasaddressid\", intermine_Alias.bankId AS \"intermine_Aliasbankid\", intermine_Alias.id AS \"intermine_Aliasid\", intermine_Alias.name AS \"intermine_Aliasname\", intermine_Alias.vatNumber AS \"intermine_Aliasvatnumber\" FROM Company AS intermine_Alias ORDER BY intermine_Alias.id";
    }
}
