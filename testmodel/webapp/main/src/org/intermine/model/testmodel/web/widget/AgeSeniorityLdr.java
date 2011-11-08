package org.intermine.model.testmodel.web.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;

import org.intermine.api.query.MainHelper;

import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.DataSetLdr;
import org.jfree.data.xy.CategoryTableXYDataset;
import org.jfree.data.xy.XYDataset;

public class AgeSeniorityLdr implements DataSetLdr {

    private final CategoryTableXYDataset dataset = new CategoryTableXYDataset();
    private final ObjectStore os;
    private Results results;
    private int total = 0;

    public AgeSeniorityLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        this.os = os;
        Query q = getQuery(bag);
        results = os.execute(q);
        Iterator<?> it = results.iterator();
        while (it.hasNext()) {
            ResultsRow<?> row = (ResultsRow<?>) it.next();
            Manager manager = (Manager) row.get(0);
            dataset.add(manager.getAge(), manager.getSeniority(), "Seniority");
            total++;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Query getQuery(InterMineBag bag) {
        PathQuery pq = new PathQuery(os.getModel());
        pq.addViews("Manager.age", "Manager.seniority");
        pq.addConstraint(Constraints.in("Manager", bag.getName()));
        Map<String, InterMineBag> bags = new HashMap<String, InterMineBag>();
        bags.put(bag.getName(), bag);
        try {
            return MainHelper.makeQuery(pq, bags, new HashMap(), null, new HashMap());
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error running query", e);
        }
    }

    @Override
    public XYDataset getDataSet() {
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
