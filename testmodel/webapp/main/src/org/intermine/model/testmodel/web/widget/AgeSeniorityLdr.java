package org.intermine.model.testmodel.web.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;

import org.intermine.api.query.MainHelper;

import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.DataSetLdr;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class AgeSeniorityLdr implements DataSetLdr {

    private final ObjectStore os;
    private Results results;
    private int total = 0;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();

    public AgeSeniorityLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        this.os = os;
        Query q = getQuery(bag);
        results = os.execute(q);
        Iterator<?> it = results.iterator();
        List<Object> headers = new LinkedList<Object>();
        headers.add("");
        headers.add("Seniority");
        headers.add("Trend");
        resultTable.add(headers);
        List<List<Double>> points = new LinkedList<List<Double>>();
        while (it.hasNext()) {
            ResultsRow<?> row = (ResultsRow<?>) it.next();
            Manager manager = (Manager) row.get(0);
            List<Object> rowList = new LinkedList<Object>();
            List<Double> point = new LinkedList<Double>();
            rowList.add(new Double(manager.getAge()));
            point.add(new Double(manager.getAge()));
            rowList.add(new Double(manager.getSeniority()));
            point.add(new Double(manager.getSeniority()));
            points.add(point);
            resultTable.add(rowList);
            total++;
        }

        /*LinearRegression regression = new LinearRegression(points);
        for (int i = 1; i < resultTable.size(); i++) {
            resultTable.get(i).add(regression.regress((Double) resultTable.get(i).get(0)));
        }*/
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
    public Results getResults() {
        return results;
    }

    @Override
    public int getWidgetTotal() {
        return total;
    }

    @Override
    public List<List<Object>> getResultTable() {
        return resultTable;
    }

}
