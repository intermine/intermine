package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Broke;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.HasAddress;
import org.intermine.model.testmodel.HasSecretarys;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Secretary;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryClassBag;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.XmlBinding;

public abstract class SetupDataTestCase extends ObjectStoreQueriesTestCase
{
    protected static Map data = new LinkedHashMap();
    protected static Model model;
    protected static String alternateDataFile = null;

    public SetupDataTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeSetUp();
        model = Model.getInstanceByName("testmodel");
        Collection col = setUpData();
        setIds(col);
        data = map(col);
        // These queries are here because they require objects with IDs
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

    public static Collection setUpData() throws Exception {
        XmlBinding binding = new XmlBinding(model);
        return (List) binding.unmarshal(SetupDataTestCase.class.getClassLoader().getResourceAsStream(alternateDataFile == null ? "testmodel_data.xml" : alternateDataFile));
    }

    public static void main(String[] args) throws Exception {
        Model testModel = Model.getInstanceByName("testmodel");
        if (testModel == null) {
            throw new Exception("Cannot find testmodel");
        }
        XmlBinding binding = new XmlBinding(testModel);
        Collection col = setUpDataObjects();
        setIds(col);
        binding.marshal(col, new FileWriter(File.createTempFile("testmodel_data", "xml")));
    }

    protected static void setIds(Collection c) throws Exception {
        int i=1;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            TypeUtil.setFieldValue(iter.next(), "id", new Integer(i++));
        }
    }

    // Used to re-generate testmodel_data.xml file from java objects, called by main method
    public static Collection setUpDataObjects() throws Exception {
        Company companyA = (Company) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Company.class, Broke.class})));
        companyA.setName("CompanyA");
        companyA.setVatNumber(1234);
        ((Broke) companyA).setDebt(876324);

        Contractor contractorA = (Contractor) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Contractor.class, Broke.class})));
        contractorA.setName("ContractorA");
        contractorA.setSeniority(new Integer(128764));
        ((Broke) contractorA).setDebt(7634);

        Company companyB = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));;
        companyB.setName("CompanyB");
        companyB.setVatNumber(5678);

        Contractor contractorB = new Contractor();
        contractorB.setName("ContractorB");
        contractorB.setSeniority(new Integer(62341));

        Address address1 = new Address();
        address1.setAddress("Contractor Business Street, BVille");

        Address address2 = new Address();
        address2.setAddress("Contractor Personal Street, BVille");

        Address address3 = new Address();
        address3.setAddress("Company Street, BVille");

        Department departmentB1 = new Department();
        departmentB1.setName("DepartmentB1");

        CEO employeeB1 = (CEO) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {CEO.class, Broke.class})));
        employeeB1.setName("EmployeeB1");
        employeeB1.setFullTime(true);
        employeeB1.setAge(40);
        employeeB1.setTitle("Mr.");
        employeeB1.setSalary(45000);
        employeeB1.setSeniority(new Integer(76321));
        ((Broke) employeeB1).setDebt(340);

        Address address4 = new Address();
        address4.setAddress("Employee Street, BVille");

        Employee employeeB2 = new Employee();
        employeeB2.setName("EmployeeB2");
        employeeB2.setFullTime(true);
        employeeB2.setAge(50);

        Department departmentB2 = new Department();
        departmentB2.setName("DepartmentB2");

        Manager employeeB3 = new Manager();
        employeeB3.setName("EmployeeB3");
        employeeB3.setFullTime(true);
        employeeB3.setAge(60);
        employeeB3.setSeniority(new Integer(761231));

        Address address5 = new Address();
        address5.setAddress("Contractor Business Street, AVille");

        Address address6 = new Address();
        address6.setAddress("Contractor Personal Street, AVille");

        Address address7 = new Address();
        address7.setAddress("Company Street, AVille");

        Department departmentA1 = new Department();
        departmentA1.setName("DepartmentA1");

        Manager employeeA1 = new Manager();
        employeeA1.setName("EmployeeA1");
        employeeA1.setFullTime(true);
        employeeA1.setAge(10);
        employeeA1.setSeniority(new Integer(876123));

        Address address8 = new Address();
        address8.setAddress("Employee Street, AVille");

        Employee employeeA2 = new Employee();
        employeeA2.setName("EmployeeA2");
        employeeA2.setFullTime(true);
        employeeA2.setAge(20);

        Employee employeeA3 = new Employee();
        employeeA3.setName("EmployeeA3");
        employeeA3.setFullTime(false);
        employeeA3.setAge(30);

        Secretary secretary1 = new Secretary();
        secretary1.setName("Secretary1");

        Secretary secretary2 = new Secretary();
        secretary2.setName("Secretary2");

        Secretary secretary3 = new Secretary();
        secretary3.setName("Secretary3");

        Types types1 = new Types();
        types1.setName("Types1");
        types1.setBooleanType(true);
        types1.setFloatType(0.6F);
        types1.setDoubleType(0.88D);
        types1.setShortType((short) 675);
        types1.setIntType(267);
        types1.setLongType(98729353495843l);
        types1.setBooleanObjType(Boolean.TRUE);
        types1.setFloatObjType(new Float(1.6F));
        types1.setDoubleObjType(new Double(1.88D));
        types1.setShortObjType(new Short((short) 1982));
        types1.setIntObjType(new Integer(369));
        types1.setLongObjType(new Long(38762874323212l));
        types1.setBigDecimalObjType(new BigDecimal("876323428764587621764532432.8768173432887324123645"));
        types1.setStringObjType("A test String");
        types1.setDateObjType(new Date(7777777l));

        companyA.setAddress(address7);
        companyA.setDepartments(Collections.singleton(departmentA1));
        companyA.setSecretarys(new HashSet(Arrays.asList(new Secretary[] {secretary1, secretary2, secretary3})));
        companyA.setContractors(new HashSet(Arrays.asList(new Contractor[] {contractorA, contractorB})));
        companyA.setOldContracts(new HashSet(Arrays.asList(new Contractor[] {contractorA, contractorB})));

        contractorA.setPersonalAddress(address6);
        contractorA.setBusinessAddress(address5);
        contractorA.setCompanys(new HashSet(Arrays.asList(new Company[] {companyA, companyB})));
        contractorA.setOldComs(new HashSet(Arrays.asList(new Company[] {companyA, companyB})));

        companyB.setAddress(address3);
        companyB.setDepartments(new HashSet(Arrays.asList(new Department[] {departmentB1, departmentB2})));
        companyB.setSecretarys(new HashSet(Arrays.asList(new Secretary[] {secretary1, secretary2})));
        companyB.setContractors(new HashSet(Arrays.asList(new Contractor[] {contractorA, contractorB})));
        companyB.setOldContracts(new HashSet(Arrays.asList(new Contractor[] {contractorA, contractorB})));
        companyB.setCEO(employeeB1);

        contractorB.setPersonalAddress(address2);
        contractorB.setBusinessAddress(address1);
        contractorB.setCompanys(new HashSet(Arrays.asList(new Company[] {companyA, companyB})));
        contractorB.setOldComs(new HashSet(Arrays.asList(new Company[] {companyA, companyB})));

        departmentB1.setCompany(companyB);
        departmentB1.setManager(employeeB1);
        departmentB1.setEmployees(new HashSet(Arrays.asList(new Employee[] {employeeB1, employeeB2})));

        employeeB1.setDepartment(departmentB1);
        employeeB1.setAddress(null);
        employeeB1.setCompany(companyB);

        employeeB2.setDepartment(departmentB1);
        employeeB2.setAddress(address4);

        departmentB2.setCompany(companyB);
        departmentB2.setManager(employeeB3);
        departmentB2.setEmployees(Collections.singleton(employeeB3));

        employeeB3.setDepartment(departmentB2);
        employeeB3.setAddress(address4);

        departmentA1.setCompany(companyA);
        departmentA1.setManager(employeeA1);
        departmentA1.setEmployees(new HashSet(Arrays.asList(new Employee[] {employeeA1, employeeA2, employeeA3})));

        employeeA1.setDepartment(departmentA1);
        employeeA1.setAddress(address8);

        employeeA2.setDepartment(departmentA1);
        employeeA2.setAddress(address8);

        employeeA3.setDepartment(departmentA1);
        employeeA3.setAddress(address8);

        Set set = new LinkedHashSet();
        set.add(companyA);
        set.add(companyB);
        set.add(contractorA);
        set.add(contractorB);
        set.add(departmentA1);
        set.add(departmentB1);
        set.add(departmentB2);
        set.add(employeeA1);
        set.add(employeeA2);
        set.add(employeeA3);
        set.add(employeeB1);
        set.add(employeeB2);
        set.add(employeeB3);
        set.add(secretary1);
        set.add(secretary2);
        set.add(secretary3);
        set.add(address1);
        set.add(address2);
        set.add(address3);
        set.add(address4);
        set.add(address5);
        set.add(address6);
        set.add(address7);
        set.add(address8);
        set.add(types1);
        return set;
    }

    private static Map map(Collection c) throws Exception {
        Map returnData = new LinkedHashMap();
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            returnData.put(objectToName(o), o);
        }
        return returnData;
    }

    public static Object objectToName(Object o) throws Exception {
        Method name = null;
        try {
            name = o.getClass().getMethod("getName", new Class[] {});
        } catch (Exception e) {
            try {
                name = o.getClass().getMethod("getAddress", new Class[] {});
            } catch (Exception e2) {
            }
        }
        if (name != null) {
            return name.invoke(o, new Object[] {});
        } else if (o instanceof InterMineObject) {
            return new Integer(o.hashCode());
        } else {
            return o;
        }
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
        set.add("hello");
        set.add("goodbye");
        set.add("CompanyA");
        set.add(data.get("CompanyA"));
        set.add(new Integer(5));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(HasSecretarys.class, Arrays.asList(new Object[] {data.get("CompanyA"), data.get("CompanyB"), data.get("EmployeeB1")}));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(HasSecretarys.class, Arrays.asList(new Object[] {data.get("CompanyA"), data.get("CompanyB"), data.get("EmployeeB1")}));
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
        QueryClassBag qcb = new QueryClassBag(new HashSet(Arrays.asList(new Class[] {CEO.class, Broke.class})), Collections.singletonList(data.get("EmployeeB1")));
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
        QueryClass qc = new QueryClass(new HashSet(Arrays.asList(new Class[] {Broke.class, CEO.class})));
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, new HashSet(Arrays.asList(new Object[] {data.get("EmployeeA1"), data.get("CompanyA"), new Integer(5), data.get("EmployeeB1")}))));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_.id, a2_, a3_ FROM ?::Department AS a1_, Employee AS a2_, Employee AS a3_ WHERE a1_.employees CONTAINS a2_ AND a1_.employee CONTAINS a3_
     */
    public static Query queryClassBagDouble() throws Exception {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
        QueryClassBag qcb = new QueryClassBag(Department.class, Arrays.asList(new Object[] {data.get("DepartmentA1"), data.get("DepartmentB1")}));
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
