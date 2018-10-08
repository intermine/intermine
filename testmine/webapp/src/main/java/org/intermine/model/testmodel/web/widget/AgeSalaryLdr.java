package org.intermine.model.testmodel.web.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;

import org.intermine.api.query.MainHelper;

import org.intermine.model.testmodel.CEO;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.DataSetLdr;

public class AgeSalaryLdr implements DataSetLdr {

    private final ObjectStore os;
    private Results results;
    private int total = 0;
    private List<List<Object>> resultTable = new LinkedList<List<Object>>();

    public AgeSalaryLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        this.os = os;
        Query q = getQuery(bag);
        results = os.execute(q);
        Iterator<?> it = results.iterator();
        List<Object> headers = new LinkedList<Object>();
        headers.add("");
        headers.add("Salary");
        headers.add("Trend");
        resultTable.add(headers);
        List<List<Double>> points = new LinkedList<List<Double>>();
        while (it.hasNext()) {
            ResultsRow<?> row = (ResultsRow<?>) it.next();
            CEO ceo = (CEO) row.get(0);

            List<Object> rowList = new LinkedList<Object>();
            List<Double> point = new LinkedList<Double>();
            rowList.add(new Double(ceo.getAge()));
            point.add(new Double(ceo.getAge()));
            rowList.add(new Double(ceo.getSalary()));
            point.add(new Double(ceo.getSalary()));
            points.add(point);
            resultTable.add(rowList);

            total++;
        }

/*        LinearRegression regression = new LinearRegression(points);
        for (int i = 1; i < resultTable.size(); i++) {
            resultTable.get(i).add(regression.regress((Double) resultTable.get(i).get(0)));
        }
        */
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Query getQuery(InterMineBag bag) {
        PathQuery pq = new PathQuery(os.getModel());
        pq.addViews("CEO.age", "CEO.salary");
        Map<String, InterMineBag> bags = new HashMap<String, InterMineBag>();
        if (bag != null) {
            pq.addConstraint(Constraints.in("CEO", bag.getName()));
            bags.put(bag.getName(), bag);
        }
        try {
            return MainHelper.makeQuery(pq, bags, new HashMap(), null, new HashMap());
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
        return null;
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
