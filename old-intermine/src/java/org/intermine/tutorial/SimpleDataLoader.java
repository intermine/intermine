package org.intermine.tutorial;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;

import org.intermine.model.tutorial.Company;
import org.intermine.model.tutorial.Address;
import org.intermine.model.tutorial.Department;

/**
 * Simple demonstration of loading data, where data may already exist
 * in the ObjectStore. This is demonstrating using the
 * ObjectStoreWriter.store() method. In real life, lots of this work
 * can done by using one of the InterMine IntegrationWriter or
 * DataLoader classes.
 *
 * @author Andrew Varley
 */
public class SimpleDataLoader
{

    /**
     * main method
     * @param args command line parameters
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        SimpleDataLoader sdl = new SimpleDataLoader();
        sdl.storeCompany();
    }

    /**
     * Store a Company object in the database
     *
     * @throws Exception if anything goes wrong
     */
    public void storeCompany() throws Exception {

        // Get an ObjectStoreWriter to use for storing the object. The
        // alias "osw.tutorial" refers to one set up in the
        // intermine.properties file
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.tutorial");

        // Set up a Company object to store - this is a dynamic class
        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));

        // Set the primary keys of the company object
        Address address = new Address();
        address.setAddress("1 The Street, A Town");
        company.setName("CompanyA");
        company.setAddress(address);

        // Try and get the Company object from the database, if it exists
        // Failure to do this lookup stage will mean that the objectstore
        // will perform an insert rather than an update
        Company companyFromDb = (Company) (osw.getObjectStore().getObjectByExample(company,
                    Collections.singleton("name")));
        if (companyFromDb != null) {
            // It did exist. Any attributes we alter must be done on
            // this object, then the ObjectStore will know which to
            // change in the store.
            company = companyFromDb;
        }

        // Set some non-primary key attributes
        company.setVatNumber(12345678);

        // Set up a collection of departments
        List departments = new ArrayList(company.getDepartments());

        // Add some new departments. Here we are assuming they are
        // completely new. If we weren't sure, we would have to set
        // primary keys and do getObjectByExample() etc, exactly as
        // for the company
        Department department1 = new Department();
        department1.setName("Department1");
        department1.setCompany(company);
        departments.add(department1);

        Department department2 = new Department();
        department2.setName("Department2");
        department2.setCompany(company);
        departments.add(department2);

        // Set company's departments to this new list
        company.setDepartments(departments);

        // Store the company in the database
        osw.store(company);

        // Store the address object
        osw.store(address);

        // Store the new departments
        osw.store(department1);
        osw.store(department2);

    }

}
