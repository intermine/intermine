package org.intermine.model.testmodel.web.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Factory;

import org.apache.commons.collections.map.LazyMap;

import org.intermine.api.profile.InterMineBag;

import org.intermine.api.query.MainHelper;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.DataSetLdr;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;

public class CompanyDistLdr implements DataSetLdr {

    private final DefaultPieDataset dataset = new DefaultPieDataset();
    private final ObjectStore os;
    private Results results;
    private int total = 0;
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
        Map<String, Integer> pieMap = 
            LazyMap.decorate(new HashMap<String, Integer>(), defaultValueFactory);
        while (it.hasNext()) {
            ResultsRow<?> row = (ResultsRow<?>) it.next();
            Employee emp = (Employee) row.get(0);
            Company company = (Company) row.get(1);
            total++;
            pieMap.put(company.getName(), 
                    pieMap.get(company.getName()) + 1);
        }
        for (String companyName: pieMap.keySet()) {
            dataset.setValue(companyName, pieMap.get(companyName));
        }
    }

    private Query getQuery(InterMineBag bag) {
        PathQuery pq = new PathQuery(os.getModel());
        String type;
        if ("CEO".equals(bag.getType())) {
            type = "CEO";
            pq.addViews("CEO.id", "CEO.company.name");
        } else {
            type = "Employee";
            pq.addViews("Employee.id", 
                    "Employee.department.company.name");
        }
        pq.addConstraint(Constraints.in(type, bag.getName()));
        Map<String, InterMineBag> bags = new HashMap<String, InterMineBag>();
        bags.put(bag.getName(), bag);
        try {
            return MainHelper.makeQuery(pq, bags, new HashMap(), 
                    null, new HashMap());
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error running query", e);
        }
    }

    @Override
    public Dataset getDataSet() {
        return dataset;
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
