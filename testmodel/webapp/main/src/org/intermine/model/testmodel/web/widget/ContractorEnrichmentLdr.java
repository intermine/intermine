package org.intermine.model.testmodel.web.widget;

import java.util.LinkedList;
import java.util.List;

import org.intermine.api.profile.InterMineBag;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;

import org.intermine.objectstore.ObjectStore;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

public class ContractorEnrichmentLdr extends EnrichmentWidgetLdr
{
    private final InterMineBag bag;

    public ContractorEnrichmentLdr(InterMineBag bag, ObjectStore os, String extra) {
        this.bag = bag;
    }

    @Override
    public Query getQuery(String action, List<String> keys) {

        Query q = new Query();
        q.setDistinct(true);

        // From classes
        QueryClass qcEmployee = new QueryClass(Employee.class);
        QueryClass qcContractor = new QueryClass(Contractor.class);
        QueryClass qcDep = new QueryClass(Department.class);
        QueryClass qcCom = new QueryClass(Company.class);

        // Fields for select
        QueryField qfEmpId = new QueryField(qcEmployee, "id");
        QueryField qfEmpName = new QueryField(qcEmployee, "name");
        QueryField qfContractorId = new QueryField(qcContractor, "id");
        QueryField qfContractorName = new QueryField(qcContractor, "name");

        q.addFrom(qcEmployee);
        q.addFrom(qcContractor);
        q.addFrom(qcDep);
        q.addFrom(qcCom);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(qfEmpId, ConstraintOp.IS_NOT_NULL));

        if (keys != null) {
            List<Integer> keysAsInts = new LinkedList<Integer>();
            for (String s: keys) {
                keysAsInts.add(Integer.valueOf(s));
            }
            cs.addConstraint(new BagConstraint(qfContractorId, ConstraintOp.IN, keysAsInts));
        }

        if (!action.startsWith("population")) {
            cs.addConstraint(new BagConstraint(qfEmpId, ConstraintOp.IN, bag.getOsb()));
        }

        // Employee.department.company.contractors = constractor
        QueryObjectReference depRef = new QueryObjectReference(qcEmployee, "department");
        cs.addConstraint(new ContainsConstraint(depRef, ConstraintOp.CONTAINS, qcDep));

        QueryObjectReference comRef = new QueryObjectReference(qcDep, "company");
        cs.addConstraint(new ContainsConstraint(comRef, ConstraintOp.CONTAINS, qcCom));

        QueryCollectionReference contrRef = new QueryCollectionReference(qcCom, "contractors");
        cs.addConstraint(new ContainsConstraint(contrRef, ConstraintOp.CONTAINS, qcContractor));

        q.setConstraint(cs);

        if ("analysed".equals(action)) {
            q.addToSelect(qfEmpId);
        } else if ("export".equals(action)) {
            q.addToSelect(qfContractorId);
            q.addToSelect(qfEmpName);
            q.addToOrderBy(qfContractorId);
        } else if (action.endsWith("Total")) {
            q.addToSelect(qfEmpId);
            Query subQ = q;
            q = new Query();
            q.addFrom(subQ);
            q.addToSelect(new QueryFunction()); // emp count
        } else {
            q.addToSelect(qfContractorId);
            q.addToGroupBy(qfContractorId);
            q.addToSelect(new QueryFunction());
            if ("sample".equals(action)) {
                q.addToSelect(qfContractorName);
                q.addToGroupBy(qfContractorName);
            }
        }

        return q;
    }
}
