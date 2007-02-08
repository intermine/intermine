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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.sql.Connection;

import junit.framework.Test;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.Failure;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.SetupDataTestCase;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.precompute.BestQueryStorer;
import org.intermine.sql.precompute.PrecomputedTable;
import org.intermine.sql.precompute.PrecomputedTableManager;
import org.intermine.sql.precompute.QueryOptimiser;
import org.intermine.testing.MustBeDifferentMap;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

public class SqlGeneratorTest extends SetupDataTestCase
{
    protected static Database db;
    protected static Map results2;

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
        results.put("SubQuery", "SELECT DISTINCT intermine_All.intermine_Arrayname AS a1_, intermine_All.intermine_Alias AS \"intermine_Alias\" FROM (SELECT intermine_Array.CEOId AS intermine_ArrayCEOId, intermine_Array.addressId AS intermine_ArrayaddressId, intermine_Array.id AS intermine_Arrayid, intermine_Array.name AS intermine_Arrayname, intermine_Array.vatNumber AS intermine_ArrayvatNumber, 5 AS intermine_Alias FROM Company AS intermine_Array) AS intermine_All ORDER BY intermine_All.intermine_Arrayname, intermine_All.intermine_Alias");
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
        results.put("ContainsMN", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, CompanysContractors AS indirect0 WHERE a1_.id = indirect0.Companys AND indirect0.Contractors = a2_.id AND a1_.name = 'ContractorA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsMN", new HashSet(Arrays.asList(new String[] {"Contractor", "Company", "CompanysContractors", "InterMineObject"})));
        results.put("ContainsDuplicatesMN", "SELECT a1_.id AS a1_id, a2_.id AS a2_id FROM Contractor AS a1_, Company AS a2_, OldComsOldContracts AS indirect0 WHERE a1_.id = indirect0.OldComs AND indirect0.OldContracts = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("ContainsDuplicatesMN", new HashSet(Arrays.asList(new String[] {"Contractor", "Company", "OldComsOldContracts", "InterMineObject"})));
        results.put("ContainsNotMN", new Failure(ObjectStoreException.class, "Cannot represent many-to-many collection DOES NOT CONTAIN in SQL")); //TODO: Fix this (ticket #445)
        results.put("SimpleGroupBy", "SELECT DISTINCT a1_.id AS a1_id, COUNT(*) AS a2_ FROM Company AS a1_, Department AS a3_ WHERE a1_.id = a3_.companyId GROUP BY a1_.CEOId, a1_.addressId, a1_.id, a1_.name, a1_.vatNumber ORDER BY a1_.id, COUNT(*)");
        results2.put("SimpleGroupBy", new HashSet(Arrays.asList(new String[] {"Department", "Company", "InterMineObject"})));
        results.put("MultiJoin", "SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id, a4_.id AS a4_id FROM Company AS a1_, Department AS a2_, Manager AS a3_, Address AS a4_ WHERE a1_.id = a2_.companyId AND a2_.managerId = a3_.id AND a3_.addressId = a4_.id AND a3_.name = 'EmployeeA1' ORDER BY a1_.id, a2_.id, a3_.id, a4_.id");
        results2.put("MultiJoin", new HashSet(Arrays.asList(new String[] {"Department", "Manager", "Company", "Address", "InterMineObject"})));
        results.put("SelectComplex", "SELECT DISTINCT (AVG(a1_.vatNumber) + 20) AS a3_, STDDEV(a1_.vatNumber) AS a4_, a2_.name AS a5_, a2_.id AS a2_id FROM Company AS a1_, Department AS a2_ GROUP BY a2_.companyId, a2_.id, a2_.managerId, a2_.name ORDER BY (AVG(a1_.vatNumber) + 20), STDDEV(a1_.vatNumber), a2_.name, a2_.id");
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
        results.put("SelectUnidirectionalCollection", "SELECT DISTINCT a2_.id AS a2_id FROM Company AS a1_, Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a1_.name = 'CompanyA' AND a1_.id = indirect0.Secretarys AND indirect0.HasSecretarys = a2_.id ORDER BY a2_.id");
        results2.put("SelectUnidirectionalCollection", new HashSet(Arrays.asList(new String[] {"Company", "Secretary", "HasSecretarysSecretarys", "InterMineObject"})));
        results.put("EmptyAndConstraintSet", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE true ORDER BY a1_.id");
        results2.put("EmptyAndConstraintSet", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("EmptyOrConstraintSet", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE false ORDER BY a1_.id");
        results2.put("EmptyOrConstraintSet", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("EmptyNandConstraintSet", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE false ORDER BY a1_.id");
        results2.put("EmptyNandConstraintSet", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("EmptyNorConstraintSet", "SELECT a1_.id AS a1_id FROM Company AS a1_ WHERE true ORDER BY a1_.id");
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
        res.add("SELECT a1_.id AS a1_id, a1__1.debt AS a2_, a1_.age AS a3_ FROM Employee AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND a1__1.debt > 0 AND a1_.age > 0 ORDER BY a1_.id");
        res.add("SELECT a1_.id AS a1_id, a1_.debt AS a2_, a1__1.age AS a3_ FROM Broke AS a1_, Employee AS a1__1 WHERE a1_.id = a1__1.id AND a1_.debt > 0 AND a1__1.age > 0 ORDER BY a1_.id");
        results.put("DynamicInterfacesAttribute", res);
        results2.put("DynamicInterfacesAttribute", new HashSet(Arrays.asList(new String[] {"Employee", "Broke", "InterMineObject"})));
        res = new HashSet();
        res.add("SELECT a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id ORDER BY a1_.id");
        res.add("SELECT a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1 WHERE a1_.id = a1__1.id ORDER BY a1_.id");
        results.put("DynamicClassInterface", res);
        results2.put("DynamicClassInterface", new HashSet(Arrays.asList(new String[] {"Employable", "Broke", "InterMineObject"})));
        res = new HashSet();
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Department AS a1_, Broke AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a2_.id = a1_.companyId AND a3_.id = a1__1.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Broke AS a1_, Department AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a2_.id = a1__1.companyId AND a3_.id = a1_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef1", res);
        results2.put("DynamicClassRef1", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank", "InterMineObject"})));
        res = new HashSet();
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Department AS a1_, Broke AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a1_.companyId = a2_.id AND a1__1.bankId = a3_.id ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Broke AS a1_, Department AS a1__1, Company AS a2_, Bank AS a3_ WHERE a1_.id = a1__1.id AND a1__1.companyId = a2_.id AND a1_.bankId = a3_.id ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef2", res);
        results2.put("DynamicClassRef2", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank", "InterMineObject"})));
        res = new HashSet();
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Company AS a1_, Bank AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a1_.id = a2_.companyId AND a1_.id = a3_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Bank AS a1_, Company AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a1_.id = a2_.companyId AND a1_.id = a3_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef3", res);
        results2.put("DynamicClassRef3", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank", "InterMineObject"})));
        res = new HashSet();
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Company AS a1_, Bank AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a2_.companyId = a1_.id AND a3_.bankId = a1_.id ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.id AS a1_id, a2_.id AS a2_id, a3_.id AS a3_id FROM Bank AS a1_, Company AS a1__1, Department AS a2_, Broke AS a3_ WHERE a1_.id = a1__1.id AND a2_.companyId = a1_.id AND a3_.bankId = a1_.id ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef4", res);
        results2.put("DynamicClassRef4", new HashSet(Arrays.asList(new String[] {"Department", "Broke", "Company", "Bank", "InterMineObject"})));
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1, HasAddress AS a2_, Broke AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1, Broke AS a2_, HasAddress AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1, HasAddress AS a2_, Broke AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1, Broke AS a2_, HasAddress AS a2__1 WHERE a1_.id = a1__1.id AND a2_.id = a2__1.id AND a1_.id = a2_.id ORDER BY a1_.id");
        results.put("DynamicClassConstraint", res);
        results2.put("DynamicClassConstraint", new HashSet(Arrays.asList(new String[] {"Employable", "Broke", "HasAddress", "InterMineObject"})));
        results.put("ContainsConstraintNull", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNull", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintNotNull", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.addressId IS NOT NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNotNull", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintObjectRefObject", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.departmentId = 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintObjectRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintNotObjectRefObject", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE a1_.departmentId != 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotObjectRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("ContainsConstraintCollectionRefObject", "SELECT a1_.id AS a1_id FROM Department AS a1_, Employee AS indirect0 WHERE a1_.id = indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Department", "Employee"})));
        results.put("ContainsConstraintNotCollectionRefObject", "SELECT a1_.id AS a1_id FROM Department AS a1_, Employee AS indirect0 WHERE a1_.id != indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Department", "Employee"})));
        results.put("ContainsConstraintMMCollectionRefObject", "SELECT a1_.id AS a1_id FROM Company AS a1_, CompanysContractors AS indirect0 WHERE a1_.id = indirect0.Contractors AND indirect0.Companys = 3 ORDER BY a1_.id");
        results2.put("ContainsConstraintMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company", "CompanysContractors"})));
        results.put("ContainsConstraintNotMMCollectionRefObject", new Failure(ObjectStoreException.class, "Cannot represent many-to-many collection DOES NOT CONTAIN in SQL")); //TODO: Fix this (ticket #445)
        //results.put("ContainsConstraintNotMMCollectionRefObject", "SELECT a1_.id AS a1_id FROM Company AS a1_, CompanysContractors AS indirect0 WHERE a1_.id != indirect0.Contractors AND indirect0.Companys = 3 ORDER BY a1_.id");
        //results2.put("ContainsConstraintNotMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company", "CompanysContractors"})));
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

        results.put("Upper", "SELECT UPPER(a1_.name) AS a2_ FROM Employee AS a1_ ORDER BY UPPER(a1_.name)");
        results2.put("Upper", Collections.singleton("Employee"));
        results.put("CollectionQueryOneMany", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE " + departmentA1Id + " = a1_.departmentId ORDER BY a1_.id");
        results2.put("CollectionQueryOneMany", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("CollectionQueryManyMany", "SELECT a1_.id AS a1_id FROM Secretary AS a1_, HasSecretarysSecretarys AS indirect0 WHERE " + companyBId + " = indirect0.Secretarys AND indirect0.HasSecretarys = a1_.id ORDER BY a1_.id");
        results2.put("CollectionQueryManyMany", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Secretary", "HasSecretarysSecretarys"})));
        results.put("QueryClassBag", "SELECT a2_.departmentId AS a3_, a2_.id AS a2_id FROM Employee AS a2_ WHERE a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") ORDER BY a2_.departmentId, a2_.id");
        results2.put("QueryClassBag", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("QueryClassBagMM", "SELECT indirect0.Secretarys AS a3_, a2_.id AS a2_id FROM Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE indirect0.Secretarys IN (" + companyAId + ", " + companyBId + ", " + employeeB1Id + ") AND indirect0.HasSecretarys = a2_.id ORDER BY indirect0.Secretarys, a2_.id");
        results2.put("QueryClassBagMM", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Secretary", "HasSecretarysSecretarys"})));
        results.put("QueryClassBagNot", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        //results.put("QueryClassBagNot", "SELECT a2_.departmentId AS a3_, a2_.id AS a2_id FROM Employee AS a2_ WHERE  NOT (a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ")) ORDER BY a2_.departmentId, a2_.id");
        //results2.put("QueryClassBagNot", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("QueryClassBagNotMM", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results.put("QueryClassBagDynamic", "SELECT indirect0.Secretarys AS a3_, a2_.id AS a2_id FROM Secretary AS a2_, HasSecretarysSecretarys AS indirect0 WHERE indirect0.Secretarys IN (" + employeeB1Id + ") AND indirect0.HasSecretarys = a2_.id ORDER BY indirect0.Secretarys, a2_.id");
        results2.put("QueryClassBagDynamic", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Secretary", "HasSecretarysSecretarys"})));
        //res = new HashSet()
        //res.add("SELECT a1_.id AS a1_id FROM Employable AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND (a1_.id IN (" + employeeB1Id + ")) ORDER BY a1_.id");
        //res.add("SELECT a1_.id AS a1_id FROM Broke AS a1_, Employable AS a1__1 WHERE a1_.id = a1__1.id AND (a1_.id IN (" + employeeB1Id + ")) ORDER BY a1_.id");
        //results.put("DynamicBagConstraint", res);
        //results2.put("DynamicBagConstraint", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Broke", "CEO"}))); // See ticket #469
        res = new HashSet();
        res.add("SELECT a1_.id AS a1_id FROM CEO AS a1_, Broke AS a1__1 WHERE a1_.id = a1__1.id AND a1_.id IN (" + employeeB1Id + ") ORDER BY a1_.id");
        res.add("SELECT a1_.id AS a1_id FROM Broke AS a1_, CEO AS a1__1 WHERE a1_.id = a1__1.id AND a1_.id IN (" + employeeB1Id + ") ORDER BY a1_.id");
        results.put("DynamicBagConstraint2", res);
        results2.put("DynamicBagConstraint2", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Broke", "CEO"})));
        results.put("QueryClassBagDouble", "SELECT a2_.departmentId AS a4_, a2_.id AS a2_id, a3_.id AS a3_id FROM Employee AS a2_, Employee AS a3_ WHERE a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND a3_.departmentId = a2_.departmentId ORDER BY a2_.departmentId, a2_.id, a3_.id");
        results2.put("QueryClassBagDouble", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("QueryClassBagContainsObject", "SELECT indirect0.departmentId AS a2_ FROM Employee AS indirect0 WHERE indirect0.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND indirect0.id = " + employeeA1Id + " ORDER BY indirect0.departmentId");
        results2.put("QueryClassBagContainsObject", Collections.singleton("Employee"));
        results.put("QueryClassBagContainsObjectDouble", "SELECT indirect0.departmentId AS a2_ FROM Employee AS indirect0, Employee AS indirect1 WHERE indirect0.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND indirect0.id = " + employeeA1Id + " AND indirect1.departmentId = indirect0.departmentId AND indirect1.id = " + employeeA2Id + " ORDER BY indirect0.departmentId");
        results2.put("QueryClassBagContainsObjectDouble", Collections.singleton("Employee"));
        results.put("QueryClassBagNotContainsObject", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results.put("ObjectContainsObject", "SELECT 'hello' AS a1_ FROM Employee AS indirect0 WHERE " + departmentA1Id + " = indirect0.departmentId AND indirect0.id = " + employeeA1Id );
        results2.put("ObjectContainsObject", Collections.singleton("Employee"));
        results.put("ObjectContainsObject2", "SELECT 'hello' AS a1_ FROM Employee AS indirect0 WHERE " + departmentA1Id + " = indirect0.departmentId AND indirect0.id = " + employeeB1Id);
        results2.put("ObjectContainsObject2", Collections.singleton("Employee"));
        results.put("ObjectNotContainsObject", "SELECT 'hello' AS a1_ FROM Employee AS indirect0 WHERE " + departmentA1Id + " != indirect0.departmentId AND indirect0.id = " + employeeA1Id);
        results2.put("ObjectNotContainsObject", Collections.singleton("Employee"));
        results.put("QueryClassBagNotViaNand", new Failure(ObjectStoreException.class, "Invalid constraint: QueryClassBag ContainsConstraint cannot be inside an OR ConstraintSet"));
        results.put("QueryClassBagNotViaNor", new Failure(ObjectStoreException.class, "Invalid constraint: DOES NOT CONTAINS cannot be applied to a QueryClassBag"));
        results.put("SubqueryExistsConstraint", "SELECT 'hello' AS a1_ WHERE EXISTS(SELECT a1_.id FROM Company AS a1_)");
        results2.put("SubqueryExistsConstraint", Collections.singleton("Company"));
        results.put("NotSubqueryExistsConstraint", "SELECT 'hello' AS a1_ WHERE (NOT EXISTS(SELECT a1_.id FROM Company AS a1_))");
        results2.put("NotSubqueryExistsConstraint", Collections.singleton("Company"));
        results.put("SubqueryExistsConstraintNeg", "SELECT 'hello' AS a1_ WHERE EXISTS(SELECT a1_.id FROM Bank AS a1_)");
        results2.put("SubqueryExistsConstraintNeg", Collections.singleton("Bank"));
        //results.put("ObjectPathExpression", "SELECT a1_.id AS a1_id FROM Employee AS a1_ ORDER BY a1_.id");
        //results2.put("ObjectPathExpression", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee", "Department"})));
        //results.put("FieldPathExpression", "SELECT a1_.id AS a1_id FROM Company AS a1_ ORDER BY a1_.id");
        //results2.put("FieldPathExpression", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company", "CEO"})));
        results.put("ObjectPathExpression", NO_RESULT);
        results.put("FieldPathExpression", NO_RESULT);
        results.put("ForeignKey", "SELECT a1_.id AS a1_id, a1_.CEOId AS a2_ FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("ForeignKey", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("ForeignKey2", "SELECT a1_.id AS a1_id, a1_.CEOId AS a2_ FROM Company AS a1_ ORDER BY a1_.id");
        results2.put("ForeignKey2", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company"})));
        results.put("OrSubquery", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE (a1_.id IN (SELECT a1_.id FROM Company AS a1_ UNION SELECT a1_.id FROM Broke AS a1_)) ORDER BY a1_.id");
        results2.put("OrSubquery", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Company", "Broke"})));
        results.put("ScientificNumber", "SELECT a1_.id AS a1_id FROM Types AS a1_ WHERE a1_.doubleType < 1.3432E24 AND a1_.floatType > -8.56E-32::REAL ORDER BY a1_.id");
        results2.put("ScientificNumber", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Types"})));
        results.put("LowerBag", "SELECT a1_.id AS a1_id FROM Employee AS a1_ WHERE LOWER(a1_.name) IN ('employeea1', 'employeea2', 'employeeb1') ORDER BY a1_.id");
        results2.put("LowerBag", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee"})));
        results.put("FetchBag", "SELECT " + ObjectStoreInterMineImpl.BAGVAL_COLUMN + " AS a1_ FROM " + ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME + " WHERE " + ObjectStoreInterMineImpl.BAGID_COLUMN + " = 5 ORDER BY " + ObjectStoreInterMineImpl.BAGVAL_COLUMN);
        results2.put("FetchBag", Collections.singleton(ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME));
        results.put("ObjectStoreBag", "SELECT a1_.id AS a1_id FROM Employee AS a1_, " + ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME + " AS indirect0 WHERE a1_.id = indirect0." + ObjectStoreInterMineImpl.BAGVAL_COLUMN + " AND indirect0." + ObjectStoreInterMineImpl.BAGID_COLUMN + " = 5 ORDER BY a1_.id");
        results2.put("ObjectStoreBag", new HashSet(Arrays.asList(new String[] {"InterMineObject", "Employee", ObjectStoreInterMineImpl.INT_BAG_TABLE_NAME})));
    }

    final static String LARGE_BAG_TABLE_NAME = "large_string_bag_table";

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
                assertTrue("No result found for " + type, false);
            }

            assertEquals(results2.get(type), SqlGenerator.findTableNames(q, getSchema()));

            // TODO: extend sql so that it can represent these
            if (!(type.startsWith("Empty") || "SubqueryExistsConstraint".equals(type) || "NotSubqueryExistsConstraint".equals(type) || "SubqueryExistsConstraintNeg".equals(type))) {
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
    }

    public void testSelectQueryValue() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue("Hello");
        QueryValue v3 = new QueryValue(new Date(1046275720000l));
        QueryValue v4 = new QueryValue(Boolean.TRUE);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, v1, null, null);
        SqlGenerator.queryEvaluableToString(buffer, v2, null, null);
        SqlGenerator.queryEvaluableToString(buffer, v3, null, null);
        SqlGenerator.queryEvaluableToString(buffer, v4, null, null);
        assertEquals("5'Hello'1046275720000'true'", buffer.toString());
    }

    public void testSelectQueryExpression() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue(new Integer(7));
        QueryExpression e1 = new QueryExpression(v1, QueryExpression.ADD, v2);
        QueryExpression e2 = new QueryExpression(v1, QueryExpression.SUBTRACT, v2);
        QueryExpression e3 = new QueryExpression(v1, QueryExpression.MULTIPLY, v2);
        QueryExpression e4 = new QueryExpression(v1, QueryExpression.DIVIDE, v2);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, null);
        SqlGenerator.queryEvaluableToString(buffer, e2, null, null);
        SqlGenerator.queryEvaluableToString(buffer, e3, null, null);
        SqlGenerator.queryEvaluableToString(buffer, e4, null, null);
        assertEquals("(5 + 7)(5 - 7)(5 * 7)(5 / 7)", buffer.toString());
    }

    public void testSelectQuerySubstringExpression() throws Exception {
        QueryValue v1 = new QueryValue("Hello");
        QueryValue v2 = new QueryValue(new Integer(3));
        QueryValue v3 = new QueryValue(new Integer(5));
        QueryExpression e1 = new QueryExpression(v1, v2, v3);
        StringBuffer buffer = new StringBuffer();
        SqlGenerator.queryEvaluableToString(buffer, e1, null, null);
        assertEquals("SUBSTR('Hello', 3, 5)", buffer.toString());
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
        DatabaseSchema s = new DatabaseSchema(new Model("nothing", "http://www.intermine.org/model/testmodel", new HashSet()), Collections.EMPTY_LIST, false, Collections.EMPTY_SET);
        try {
            SqlGenerator.generate(q, 0, Integer.MAX_VALUE, s, db, new HashMap());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("interface org.intermine.model.testmodel.Company is not in the model", e.getMessage());
        }
        try {
            SqlGenerator.findTableNames(q, s);
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
            SqlGenerator.findTableNames(q, getSchema());
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
            assertTrue(e.getMessage().startsWith("Unknown constraint type: "));
        }
        try {
            SqlGenerator.findTableNames(q, getSchema());
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertTrue(e.getMessage().startsWith("Unknown constraint type: "));
        }
    }

    public void testInvalidClassConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
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
            assertEquals("Reference ?.extras is not in the model", e.getMessage());
        }
    }


    public void testRegisterOffset() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = new Query();
        QueryClass c1 = new QueryClass(Company.class);
        q.addFrom(c1);
        q.addToSelect(c1);
        assertEquals(getRegisterOffset1(), SqlGenerator.generate(q, 0, Integer.MAX_VALUE, schema, db, new HashMap()));
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


    public void testForPrecomp() throws Exception {
        DatabaseSchema schema = getSchema();
        Query q = (Query) queries.get("SelectSimpleObject");
        assertEquals(precompTableString(), SqlGenerator.generate(q, schema, db, null, SqlGenerator.QUERY_FOR_PRECOMP, Collections.EMPTY_MAP));
    }

    public void testInvalidSafenesses() throws Exception {
        try {
            SqlGenerator.constraintToString(null, null, null, null, 3, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Unknown ContainsConstraint safeness: 3", e.getMessage());
        }
        try {
            SqlGenerator.constraintSetToString(null, null, null, null, 3, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Unknown ContainsConstraint safeness: 3", e.getMessage());
        }
        try {
            SqlGenerator.containsConstraintToString(null, null, null, null, 3, false);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
            assertEquals("Unknown ContainsConstraint safeness: 3", e.getMessage());
        }
    }

    protected DatabaseSchema getSchema() throws Exception {
        return new DatabaseSchema(model, Collections.EMPTY_LIST, true, Collections.EMPTY_SET);
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
        return "SELECT intermine_Alias.CEOId AS \"intermine_Aliasceoid\", intermine_Alias.addressId AS \"intermine_Aliasaddressid\", intermine_Alias.id AS \"intermine_Aliasid\", intermine_Alias.name AS \"intermine_Aliasname\", intermine_Alias.vatNumber AS \"intermine_Aliasvatnumber\" FROM Company AS intermine_Alias ORDER BY intermine_Alias.id";
    }
}
