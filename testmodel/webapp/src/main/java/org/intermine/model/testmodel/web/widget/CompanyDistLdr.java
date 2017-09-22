package org.intermine.model.testmodel.web.widget;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;

import org.apache.commons.collections.map.LazyMap;

import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.InterMineBag;

import org.intermine.api.query.MainHelper;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.DataSetLdr;

public class CompanyDistLdr implements DataSetLdr {

    private final ObjectStore os;
    private Results results;
    private int total = 0;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();

    private static final Factory defaultValueFactory = new Factory() {
        public Object create() {
            return new Integer(0);
        }
    };

    public CompanyDistLdr(InterMineBag bag, ObjectStore os,
            String extra) {
        super();
        this.os = os;
        Query q = getQuery(bag);
        results = os.execute(q);
        Iterator<?> it = results.iterator();
        @SuppressWarnings("unchecked")
        Map<String, Integer> pieMap = (Map<String, Integer>)
            LazyMap.decorate(new HashMap<String, Integer>(), defaultValueFactory);
        while (it.hasNext()) {
            ResultsRow<?> row = (ResultsRow<?>) it.next();
            Employee emp = (Employee) row.get(0);
            Company company = (Company) row.get(1);
            total++;
            pieMap.put(company.getName(),
                    pieMap.get(company.getName()) + 1);
        }
        List<Object> headerRow = new LinkedList<Object>();
        headerRow.add("Company Name");
        headerRow.add("No. of employees");
        resultTable.add(headerRow);
        for (String companyName: pieMap.keySet()) {
            List<Object> row = new LinkedList<Object>();
            row.add(companyName);
            row.add(pieMap.get(companyName).doubleValue());
            resultTable.add(row);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Query getQuery(InterMineBag bag) {
        PathQuery pq = new PathQuery(os.getModel());
        String type;
        if ("CEO".equals(bag.getType())) {
            type = "CEO";
            pq.addViews("CEO.id", "CEO.company.name");
        } else {
            type = "Employee";
            pq.addViews("Employee.id", "Employee.department.company.name");
        }
        pq.addConstraint(Constraints.in(type, bag.getName()));
        Map<String, InterMineBag> bags = new HashMap<String, InterMineBag>();
        bags.put(bag.getName(), bag);
        try {
            return MainHelper.makeQuery(pq, bags, new HashMap<String, QuerySelectable>(), null, new HashMap<String, BagQueryResult>());
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error running query", e);
        }
    }

    @Override
    public List<List<Object>> getResultTable() {
        return resultTable;
    }

    @Override
    public Results getResults() {
        return results;
    }

    @Override
    public int getWidgetTotal() {
        return total;
    }
}
