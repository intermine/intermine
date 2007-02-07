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

public class TruncatedSqlGeneratorTest extends SqlGeneratorTest
{
    public TruncatedSqlGeneratorTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(TruncatedSqlGeneratorTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        SqlGeneratorTest.oneTimeSetUp();
        setUpResults();
        db = DatabaseFactory.getDatabase("db.truncunittest");
    }

    public static void setUpResults() throws Exception {
        results.put("SelectSimpleObject", "SELECT intermine_Alias.OBJECT AS \"intermine_Alias\", intermine_Alias.id AS \"intermine_Aliasid\" FROM InterMineObject AS intermine_Alias WHERE intermine_Alias.class = 'org.intermine.model.testmodel.Company' ORDER BY intermine_Alias.id");
        results2.put("SelectSimpleObject", Collections.singleton("InterMineObject"));
        results.put("SubQuery", "SELECT DISTINCT intermine_All.intermine_Arrayname AS a1_, intermine_All.intermine_Alias AS \"intermine_Alias\" FROM (SELECT intermine_Array.CEOId AS intermine_ArrayCEOId, intermine_Array.addressId AS intermine_ArrayaddressId, intermine_Array.id AS intermine_Arrayid, intermine_Array.name AS intermine_Arrayname, intermine_Array.vatNumber AS intermine_ArrayvatNumber, 5 AS intermine_Alias FROM InterMineObject AS intermine_Array WHERE intermine_Array.class = 'org.intermine.model.testmodel.Company') AS intermine_All ORDER BY intermine_All.intermine_Arrayname, intermine_All.intermine_Alias");
        results2.put("SubQuery", Collections.singleton("InterMineObject"));
        results.put("WhereSimpleEquals", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.vatNumber = 1234 ORDER BY a1_.name");
        results2.put("WhereSimpleEquals", Collections.singleton("InterMineObject"));
        results.put("WhereSimpleNotEquals", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.vatNumber != 1234 ORDER BY a1_.name");
        results2.put("WhereSimpleNotEquals", Collections.singleton("InterMineObject"));
        results.put("WhereSimpleNegEquals", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.vatNumber != 1234 ORDER BY a1_.name");
        results2.put("WhereSimpleNegEquals", Collections.singleton("InterMineObject"));
        results.put("WhereSimpleLike", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.name LIKE 'Company%' ORDER BY a1_.name");
        results2.put("WhereSimpleLike", Collections.singleton("InterMineObject"));
        results.put("WhereEqualsString", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.name = 'CompanyA' ORDER BY a1_.name");
        results2.put("WhereEqualsString", Collections.singleton("InterMineObject"));
        results.put("WhereAndSet", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000 ORDER BY a1_.name");
        results2.put("WhereAndSet", Collections.singleton("InterMineObject"));
        results.put("WhereOrSet", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND (a1_.name LIKE 'CompanyA%' OR a1_.vatNumber > 2000) ORDER BY a1_.name");
        results2.put("WhereOrSet", Collections.singleton("InterMineObject"));
        results.put("WhereNotSet", "SELECT DISTINCT a1_.name AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND (NOT (a1_.name LIKE 'Company%' AND a1_.vatNumber > 2000)) ORDER BY a1_.name");
        results2.put("WhereNotSet", Collections.singleton("InterMineObject"));
        results.put("WhereSubQueryField", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a1_.name IN (SELECT DISTINCT a1_.name FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Department') ORDER BY a1_.name, a1_.id");
        results2.put("WhereSubQueryField", Collections.singleton("InterMineObject"));
        results.put("WhereSubQueryClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id IN (SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereSubQueryClass", Collections.singleton("InterMineObject"));
        results.put("WhereNotSubQueryClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id NOT IN (SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereNotSubQueryClass", Collections.singleton("InterMineObject"));
        results.put("WhereNegSubQueryClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id NOT IN (SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.name = 'CompanyA') ORDER BY a1_.id");
        results2.put("WhereNegSubQueryClass", Collections.singleton("InterMineObject"));
        results.put("WhereClassClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereClassClass", Collections.singleton("InterMineObject"));
        results.put("WhereNotClassClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereNotClassClass", Collections.singleton("InterMineObject"));
        results.put("WhereNegClassClass", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a1_.id != a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("WhereNegClassClass", Collections.singleton("InterMineObject"));
        results.put("WhereClassObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = " + companyAId + " ORDER BY a1_.id");
        results2.put("WhereClassObject", Collections.singleton("InterMineObject"));
        results.put("Contains11", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a2_.class = 'org.intermine.model.testmodel.Manager' AND a1_.managerId = a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("Contains11", Collections.singleton("InterMineObject"));
        results.put("ContainsNot11", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a2_.class = 'org.intermine.model.testmodel.Manager' AND a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNot11", Collections.singleton("InterMineObject"));
        results.put("ContainsNeg11", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a2_.class = 'org.intermine.model.testmodel.Manager' AND a1_.managerId != a2_.id AND a1_.name = 'DepartmentA1' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNeg11", Collections.singleton("InterMineObject"));
        results.put("Contains1N", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a1_.id = a2_.companyId AND a1_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("Contains1N", Collections.singleton("InterMineObject"));
        results.put("ContainsNot1N", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a1_.id != a2_.companyId AND a1_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsNot1N", Collections.singleton("InterMineObject"));
        results.put("ContainsN1", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a1_.companyId = a2_.id AND a2_.name = 'CompanyA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsN1", Collections.singleton("InterMineObject"));
        results.put("ContainsMN", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_, CompanysContractors AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Contractor' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = indirect0.Companys AND indirect0.Contractors = a2_.id AND a1_.name = 'ContractorA' ORDER BY a1_.id, a2_.id");
        results2.put("ContainsMN", new HashSet(Arrays.asList(new String[] {"InterMineObject", "CompanysContractors"})));
        results.put("ContainsDuplicatesMN", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_, OldComsOldContracts AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Contractor' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = indirect0.OldComs AND indirect0.OldContracts = a2_.id ORDER BY a1_.id, a2_.id");
        results2.put("ContainsDuplicatesMN", new HashSet(Arrays.asList(new String[] {"InterMineObject", "OldComsOldContracts"})));
        results.put("SimpleGroupBy", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id, COUNT(*) AS a2_ FROM InterMineObject AS a1_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a3_.class = 'org.intermine.model.testmodel.Department' AND a1_.id = a3_.companyId GROUP BY a1_.OBJECT, a1_.CEOId, a1_.addressId, a1_.id, a1_.name, a1_.vatNumber ORDER BY a1_.id, COUNT(*)");
        results2.put("SimpleGroupBy", Collections.singleton("InterMineObject"));
        results.put("MultiJoin", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id, a4_.OBJECT AS a4_, a4_.id AS a4_id FROM InterMineObject AS a1_, InterMineObject AS a2_, InterMineObject AS a3_, InterMineObject AS a4_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a3_.class = 'org.intermine.model.testmodel.Manager' AND a4_.class = 'org.intermine.model.testmodel.Address' AND a1_.id = a2_.companyId AND a2_.managerId = a3_.id AND a3_.addressId = a4_.id AND a3_.name = 'EmployeeA1' ORDER BY a1_.id, a2_.id, a3_.id, a4_.id");
        results2.put("MultiJoin", Collections.singleton("InterMineObject"));
        results.put("SelectComplex", "SELECT DISTINCT (AVG(a1_.vatNumber) + 20) AS a3_, STDDEV(a1_.vatNumber) AS a4_, a2_.name AS a5_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' GROUP BY a2_.OBJECT, a2_.companyId, a2_.id, a2_.managerId, a2_.name ORDER BY (AVG(a1_.vatNumber) + 20), STDDEV(a1_.vatNumber), a2_.name, a2_.id");
        results2.put("SelectComplex", Collections.singleton("InterMineObject"));
        results.put("SelectClassAndSubClasses", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.name AS orderbyfield0 FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY a1_.name, a1_.id");
        results2.put("SelectClassAndSubClasses", Collections.singleton("InterMineObject"));
        results.put("SelectInterfaceAndSubClasses", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employable' ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses", Collections.singleton("InterMineObject"));
        results.put("SelectInterfaceAndSubClasses2", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.RandomInterface' ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses2", Collections.singleton("InterMineObject"));
        results.put("SelectInterfaceAndSubClasses3", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.ImportantPerson' ORDER BY a1_.id");
        results2.put("SelectInterfaceAndSubClasses3", Collections.singleton("InterMineObject"));
        results.put("OrderByAnomaly", "SELECT DISTINCT 5 AS a2_, a1_.name AS a3_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' ORDER BY a1_.name");
        results2.put("OrderByAnomaly", Collections.singleton("InterMineObject"));
        results.put("SelectClassObjectSubquery", "SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a2_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a1_.id = " + companyAId + " AND a1_.id = a2_.companyId AND a2_.id IN (SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a1_.id = " + departmentA1Id + ") ORDER BY a1_.id");
        results2.put("SelectClassObjectSubquery", Collections.singleton("InterMineObject"));
        results.put("SelectUnidirectionalCollection", "SELECT DISTINCT a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a1_, InterMineObject AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Secretary' AND a1_.name = 'CompanyA' AND a1_.id = indirect0.Secretarys AND indirect0.HasSecretarys = a2_.id ORDER BY a2_.id");
        results2.put("SelectUnidirectionalCollection", new HashSet(Arrays.asList(new String[] {"InterMineObject", "HasSecretarysSecretarys"})));
        results.put("EmptyAndConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND true ORDER BY a1_.id");
        results2.put("EmptyAndConstraintSet", Collections.singleton("InterMineObject"));
        results.put("EmptyOrConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND false ORDER BY a1_.id");
        results2.put("EmptyOrConstraintSet", Collections.singleton("InterMineObject"));
        results.put("EmptyNandConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND false ORDER BY a1_.id");
        results2.put("EmptyNandConstraintSet", Collections.singleton("InterMineObject"));
        results.put("EmptyNorConstraintSet", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND true ORDER BY a1_.id");
        results2.put("EmptyNorConstraintSet", Collections.singleton("InterMineObject"));
        results.put("BagConstraint", "SELECT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM InterMineObject AS Company WHERE Company.class = 'org.intermine.model.testmodel.Company' AND Company.name IN ('CompanyA', 'goodbye', 'hello') ORDER BY Company.id");
        results2.put("BagConstraint", Collections.singleton("InterMineObject"));
        results.put("BagConstraint2", "SELECT Company.OBJECT AS \"Company\", Company.id AS \"Companyid\" FROM InterMineObject AS Company WHERE Company.class = 'org.intermine.model.testmodel.Company' AND Company.id IN (" + companyAId + ") ORDER BY Company.id");
        results2.put("BagConstraint2", Collections.singleton("InterMineObject"));
        results.put("InterfaceField", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employable' AND a1_.name = 'EmployeeA1' ORDER BY a1_.id");
        results2.put("InterfaceField", Collections.singleton("InterMineObject"));
        Set res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1__1.debt AS a2_, a1_.age AS a3_ FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a1__1.debt > 0 AND a1_.age > 0 ORDER BY a1_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.debt AS a2_, a1__1.age AS a3_ FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Employee' AND a1_.debt > 0 AND a1__1.age > 0 ORDER BY a1_.id");
        results.put("DynamicInterfacesAttribute", res);
        results2.put("DynamicInterfacesAttribute", Collections.singleton("InterMineObject"));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Employable' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' ORDER BY a1_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Employable' ORDER BY a1_.id");
        results.put("DynamicClassInterface", res);
        results2.put("DynamicClassInterface", Collections.singleton("InterMineObject"));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a3_.class = 'org.intermine.model.testmodel.Bank' AND a2_.id = a1_.companyId AND a3_.id = a1__1.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Department' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a3_.class = 'org.intermine.model.testmodel.Bank' AND a2_.id = a1__1.companyId AND a3_.id = a1_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef1", res);
        results2.put("DynamicClassRef1", Collections.singleton("InterMineObject"));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a3_.class = 'org.intermine.model.testmodel.Bank' AND a1_.companyId = a2_.id AND a1__1.bankId = a3_.id ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Department' AND a2_.class = 'org.intermine.model.testmodel.Company' AND a3_.class = 'org.intermine.model.testmodel.Bank' AND a1__1.companyId = a2_.id AND a1_.bankId = a3_.id ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef2", res);
        results2.put("DynamicClassRef2", Collections.singleton("InterMineObject"));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Bank' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a3_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a2_.companyId AND a1_.id = a3_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Bank' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a3_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a2_.companyId AND a1_.id = a3_.bankId ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef3", res);
        results2.put("DynamicClassRef3", Collections.singleton("InterMineObject"));
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Bank' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a3_.class = 'org.intermine.model.testmodel.Broke' AND a2_.companyId = a1_.id AND a3_.bankId = a1_.id ORDER BY a1_.id, a2_.id, a3_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a3_ WHERE a1_.class = 'org.intermine.model.testmodel.Bank' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Company' AND a2_.class = 'org.intermine.model.testmodel.Department' AND a3_.class = 'org.intermine.model.testmodel.Broke' AND a2_.companyId = a1_.id AND a3_.bankId = a1_.id ORDER BY a1_.id, a2_.id, a3_.id");
        results.put("DynamicClassRef4", res);
        results2.put("DynamicClassRef4", Collections.singleton("InterMineObject"));
        res = new HashSet();
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a2__1 WHERE a1_.class = 'org.intermine.model.testmodel.Employable' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a2_.class = 'org.intermine.model.testmodel.HasAddress' AND a2_.id = a2__1.id AND a2__1.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a2__1 WHERE a1_.class = 'org.intermine.model.testmodel.Employable' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a2_.class = 'org.intermine.model.testmodel.Broke' AND a2_.id = a2__1.id AND a2__1.class = 'org.intermine.model.testmodel.HasAddress' AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a2__1 WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Employable' AND a2_.class = 'org.intermine.model.testmodel.HasAddress' AND a2_.id = a2__1.id AND a2__1.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a2_.id ORDER BY a1_.id");
        res.add("SELECT DISTINCT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1, InterMineObject AS a2_, InterMineObject AS a2__1 WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Employable' AND a2_.class = 'org.intermine.model.testmodel.Broke' AND a2_.id = a2__1.id AND a2__1.class = 'org.intermine.model.testmodel.HasAddress' AND a1_.id = a2_.id ORDER BY a1_.id");
        results.put("DynamicClassConstraint", res);
        results2.put("DynamicClassConstraint", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.addressId IS NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNull", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintNotNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.addressId IS NOT NULL ORDER BY a1_.id");
        results2.put("ContainsConstraintNotNull", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintObjectRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.departmentId = 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintObjectRefObject", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintNotObjectRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.departmentId != 5 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotObjectRefObject", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND indirect0.class = 'org.intermine.model.testmodel.Employee' AND a1_.id = indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintCollectionRefObject", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintNotCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Department' AND indirect0.class = 'org.intermine.model.testmodel.Employee' AND a1_.id != indirect0.departmentId AND indirect0.id = 11 ORDER BY a1_.id");
        results2.put("ContainsConstraintNotCollectionRefObject", Collections.singleton("InterMineObject"));
        results.put("ContainsConstraintMMCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, CompanysContractors AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND a1_.id = indirect0.Contractors AND indirect0.Companys = 3 ORDER BY a1_.id");
        results2.put("ContainsConstraintMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "CompanysContractors"})));
        //results.put("ContainsConstraintNotMMCollectionRefObject", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, CompanysContractors AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND (a1_.id != indirect0.Contractors AND indirect0.Companys = 3) ORDER BY a1_.id");
        //results2.put("ContainsConstraintNotMMCollectionRefObject", new HashSet(Arrays.asList(new String[] {"InterMineObject", "CompanysContractors"})));
        results.put("SimpleConstraintNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Manager' AND a1_.title IS NULL ORDER BY a1_.id");
        results2.put("SimpleConstraintNull", Collections.singleton("InterMineObject"));
        results.put("SimpleConstraintNotNull", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Manager' AND a1_.title IS NOT NULL ORDER BY a1_.id");
        results2.put("SimpleConstraintNotNull", Collections.singleton("InterMineObject"));
        results.put("TypeCast", "SELECT DISTINCT (a1_.age)::TEXT AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY (a1_.age)::TEXT");
        results2.put("TypeCast", Collections.singleton("InterMineObject"));
        results.put("IndexOf", "SELECT STRPOS(a1_.name, 'oy') AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY STRPOS(a1_.name, 'oy')");
        results2.put("IndexOf", Collections.singleton("InterMineObject"));
        results.put("Substring", "SELECT SUBSTR(a1_.name, 2, 2) AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY SUBSTR(a1_.name, 2, 2)");
        results2.put("Substring", Collections.singleton("InterMineObject"));
        results.put("Substring2", "SELECT SUBSTR(a1_.name, 2) AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY SUBSTR(a1_.name, 2)");
        results2.put("Substring2", Collections.singleton("InterMineObject"));
        results.put("OrderByReference", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.departmentId AS orderbyfield0 FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY a1_.departmentId, a1_.id");
        results2.put("OrderByReference", Collections.singleton("InterMineObject"));

        String largeBagConstraintText = new BufferedReader(new InputStreamReader(TruncatedSqlGeneratorTest.class.getClassLoader().getResourceAsStream("truncatedLargeBag.sql"))).readLine();
        results.put("LargeBagConstraint", largeBagConstraintText);
        results2.put("LargeBagConstraint", Collections.singleton("InterMineObject"));

        String largeBagNotConstraintText = new BufferedReader(new InputStreamReader(TruncatedSqlGeneratorTest.class.getClassLoader().getResourceAsStream("truncatedLargeNotBag.sql"))).readLine();
        results.put("LargeBagNotConstraint", largeBagNotConstraintText);
        results2.put("LargeBagNotConstraint", Collections.singleton("InterMineObject"));

        results.put("LargeBagConstraintUsingTable", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, " + LARGE_BAG_TABLE_NAME + " AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.name = indirect0.value ORDER BY a1_.id");
        results2.put("LargeBagConstraintUsingTable", Collections.singleton("InterMineObject"));

        results.put("LargeBagNotConstraintUsingTable", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND (NOT (a1_.name IN (SELECT value FROM " + LARGE_BAG_TABLE_NAME + "))) ORDER BY a1_.id");
        results2.put("LargeBagNotConstraintUsingTable", Collections.singleton("InterMineObject"));

        results.put("NegativeNumbers", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND a1_.age > -51 ORDER BY a1_.id");
        results2.put("NegativeNumbers", Collections.singleton("InterMineObject"));

        results.put("Lower", "SELECT LOWER(a1_.name) AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY LOWER(a1_.name)");
        results2.put("Lower", Collections.singleton("InterMineObject"));

        results.put("Upper", "SELECT UPPER(a1_.name) AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY UPPER(a1_.name)");
        results2.put("Upper", Collections.singleton("InterMineObject"));
        results.put("CollectionQueryOneMany", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND " + departmentA1Id + " = a1_.departmentId ORDER BY a1_.id");
        results2.put("CollectionQueryOneMany", Collections.singleton("InterMineObject"));
        results.put("CollectionQueryManyMany", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, HasSecretarysSecretarys AS indirect0 WHERE a1_.class = 'org.intermine.model.testmodel.Secretary' AND " + companyBId + " = indirect0.Secretarys AND indirect0.HasSecretarys = a1_.id ORDER BY a1_.id");
        results2.put("CollectionQueryManyMany", new HashSet(Arrays.asList(new String[] {"InterMineObject", "HasSecretarysSecretarys"})));
        results.put("QueryClassBag", "SELECT a2_.departmentId AS a3_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a2_ WHERE a2_.class = 'org.intermine.model.testmodel.Employee' AND a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") ORDER BY a2_.departmentId, a2_.id");
        results2.put("QueryClassBag", Collections.singleton("InterMineObject"));
        results.put("QueryClassBagMM", "SELECT indirect0.Secretarys AS a3_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a2_.class = 'org.intermine.model.testmodel.Secretary' AND indirect0.Secretarys IN (" + companyAId + ", " + companyBId + ", " + employeeB1Id + ") AND indirect0.HasSecretarys = a2_.id ORDER BY indirect0.Secretarys, a2_.id");
        results2.put("QueryClassBagMM", new HashSet(Arrays.asList(new String[] {"InterMineObject", "HasSecretarysSecretarys"})));
        results.put("QueryClassBagDynamic", "SELECT indirect0.Secretarys AS a3_, a2_.OBJECT AS a2_, a2_.id AS a2_id FROM InterMineObject AS a2_, HasSecretarysSecretarys AS indirect0 WHERE a2_.class = 'org.intermine.model.testmodel.Secretary' AND indirect0.Secretarys IN (" + employeeB1Id + ") AND indirect0.HasSecretarys = a2_.id ORDER BY indirect0.Secretarys, a2_.id");
        results2.put("QueryClassBagDynamic", new HashSet(Arrays.asList(new String[] {"InterMineObject", "HasSecretarysSecretarys"})));
        //res = new HashSet()
        //res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Employable' AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND (a1_.id IN (" + employeeB1Id + ")) ORDER BY a1_.id");
        //res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1__1.class = 'org.intermine.model.testmodel.Employable' AND a1_.id = a1__1.id AND (a1_.id IN (" + employeeB1Id + ")) ORDER BY a1_.id");
        //results.put("DynamicBagConstraint", res);
        //results2.put("DynamicBagConstraint", Collections.singleton("InterMineObject")); // See ticket #469
        res = new HashSet();
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.CEO' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.Broke' AND a1_.id IN (" + employeeB1Id + ") ORDER BY a1_.id");
        res.add("SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_, InterMineObject AS a1__1 WHERE a1_.class = 'org.intermine.model.testmodel.Broke' AND a1_.id = a1__1.id AND a1__1.class = 'org.intermine.model.testmodel.CEO' AND a1_.id IN (" + employeeB1Id + ") ORDER BY a1_.id");
        results.put("DynamicBagConstraint2", res);
        results2.put("DynamicBagConstraint2", Collections.singleton("InterMineObject"));
        results.put("QueryClassBagDouble", "SELECT a2_.departmentId AS a4_, a2_.OBJECT AS a2_, a2_.id AS a2_id, a3_.OBJECT AS a3_, a3_.id AS a3_id FROM InterMineObject AS a2_, InterMineObject AS a3_ WHERE a2_.class = 'org.intermine.model.testmodel.Employee' AND a3_.class = 'org.intermine.model.testmodel.Employee' AND a2_.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND a3_.departmentId = a2_.departmentId ORDER BY a2_.departmentId, a2_.id, a3_.id");
        results2.put("QueryClassBagDouble", Collections.singleton("InterMineObject"));
        results.put("QueryClassBagContainsObject", "SELECT indirect0.departmentId AS a2_ FROM InterMineObject AS indirect0 WHERE indirect0.class = 'org.intermine.model.testmodel.Employee' AND indirect0.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND indirect0.id = " + employeeA1Id + " ORDER BY indirect0.departmentId");
        results2.put("QueryClassBagContainsObject", Collections.singleton("InterMineObject"));
        results.put("QueryClassBagContainsObjectDouble", "SELECT indirect0.departmentId AS a2_ FROM InterMineObject AS indirect0, InterMineObject AS indirect1 WHERE indirect0.class = 'org.intermine.model.testmodel.Employee' AND indirect0.departmentId IN (" + departmentA1Id + ", " + departmentB1Id + ") AND indirect0.id = " + employeeA1Id + " AND indirect1.class = 'org.intermine.model.testmodel.Employee' AND indirect1.departmentId = indirect0.departmentId AND indirect1.id = " + employeeA2Id + " ORDER BY indirect0.departmentId");
        results2.put("QueryClassBagContainsObjectDouble", Collections.singleton("InterMineObject"));
        results.put("ObjectContainsObject", "SELECT 'hello' AS a1_ FROM InterMineObject AS indirect0 WHERE indirect0.class = 'org.intermine.model.testmodel.Employee' AND " + departmentA1Id + " = indirect0.departmentId AND indirect0.id = " + employeeA1Id);
        results2.put("ObjectContainsObject", Collections.singleton("InterMineObject"));
        results.put("ObjectContainsObject2", "SELECT 'hello' AS a1_ FROM InterMineObject AS indirect0 WHERE indirect0.class = 'org.intermine.model.testmodel.Employee' AND " + departmentA1Id + " = indirect0.departmentId AND indirect0.id = " + employeeB1Id);
        results2.put("ObjectContainsObject2", Collections.singleton("InterMineObject"));
        results.put("ObjectNotContainsObject", "SELECT 'hello' AS a1_ FROM InterMineObject AS indirect0 WHERE indirect0.class = 'org.intermine.model.testmodel.Employee' AND " + departmentA1Id + " != indirect0.departmentId AND indirect0.id = " + employeeA1Id);
        results2.put("ObjectNotContainsObject", Collections.singleton("InterMineObject"));
        results.put("SubqueryExistsConstraint", "SELECT 'hello' AS a1_ WHERE EXISTS(SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company')");
        results2.put("SubqueryExistsConstraint", Collections.singleton("InterMineObject"));
        results.put("NotSubqueryExistsConstraint", "SELECT 'hello' AS a1_ WHERE (NOT EXISTS(SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company'))");
        results2.put("NotSubqueryExistsConstraint", Collections.singleton("InterMineObject"));
        results.put("SubqueryExistsConstraintNeg", "SELECT 'hello' AS a1_ WHERE EXISTS(SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Bank')");
        results2.put("SubqueryExistsConstraintNeg", Collections.singleton("InterMineObject"));
        //results.put("ObjectPathExpression", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' ORDER BY a1_.id");
        //results2.put("ObjectPathExpression", Collections.singleton("InterMineObject"));
        //results.put("FieldPathExpression", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' ORDER BY a1_.id");
        //results2.put("FieldPathExpression", Collections.singleton("InterMineObject"));
        results.put("ObjectPathExpression", NO_RESULT);
        results.put("FieldPathExpression", NO_RESULT);
        results.put("ForeignKey", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.CEOId AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' ORDER BY a1_.id");
        results2.put("ForeignKey", Collections.singleton("InterMineObject"));
        results.put("ForeignKey2", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id, a1_.CEOId AS a2_ FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' ORDER BY a1_.id");
        results2.put("ForeignKey2", Collections.singleton("InterMineObject"));
        results.put("OrSubquery", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.InterMineObject' AND (a1_.id IN (SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' UNION SELECT a1_.id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Broke')) ORDER BY a1_.id");
        results2.put("OrSubquery", Collections.singleton("InterMineObject"));
        results.put("ScientificNumber", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Types' AND a1_.doubleType < 1.3432E24 AND a1_.floatType > -8.56E-32::REAL ORDER BY a1_.id");
        results2.put("ScientificNumber", Collections.singleton("InterMineObject"));
        results.put("LowerBag", "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee' AND LOWER(a1_.name) IN ('employeea1', 'employeea2', 'employeeb1') ORDER BY a1_.id");
        results2.put("LowerBag", Collections.singleton("InterMineObject"));
    }

    protected DatabaseSchema getSchema() {
        return new DatabaseSchema(model, Collections.singletonList(model.getClassDescriptorByName("org.intermine.model.InterMineObject")), true, Collections.EMPTY_SET);
    }
    public String getRegisterOffset1() {
        return "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' ORDER BY a1_.id";
    }
    public String getRegisterOffset2() {
        return "SELECT a1_.OBJECT AS a1_, a1_.id AS a1_id FROM InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Company' AND ";
    }
    public String getRegisterOffset3() {
        return "InterMineObject AS a1_ WHERE a1_.class = 'org.intermine.model.testmodel.Employee'";
    }
    public String getRegisterOffset4() {
        return "AND";
    }
    public String precompTableString() {
        return "SELECT intermine_Alias.OBJECT AS \"intermine_Alias\", intermine_Alias.CEOId AS \"intermine_Aliasceoid\", intermine_Alias.addressId AS \"intermine_Aliasaddressid\", intermine_Alias.id AS \"intermine_Aliasid\", intermine_Alias.name AS \"intermine_Aliasname\", intermine_Alias.vatNumber AS \"intermine_Aliasvatnumber\" FROM InterMineObject AS intermine_Alias WHERE intermine_Alias.class = 'org.intermine.model.testmodel.Company' ORDER BY intermine_Alias.id";
    }
}
