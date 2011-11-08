package org.intermine.model.testmodel.web.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.Factory;

import org.apache.commons.collections.map.LazyMap;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.MainHelper;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.DataSetLdr;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;

public class AgeGroupLdr implements DataSetLdr
{

    private enum ColumnGroup {ACTUAL, EXPECTED};
    private static final String GROUP_0 = "0 to 19";
    private static final String GROUP_1 = "20 to 29";
    private static final String GROUP_2 = "30 to 39";
    private static final String GROUP_3 = "40 to 49";
    private static final String GROUP_4 = "50 to 59";
    private static final String GROUP_5 = "over 60";
    private static final Map<String, Integer> GROUP_LIMITS;
    static {
        Map<String, Integer> aMap = new LinkedHashMap<String, Integer>();
        aMap.put(GROUP_0, 20);
        aMap.put(GROUP_1, 30);
        aMap.put(GROUP_2, 40);
        aMap.put(GROUP_3, 50);
        aMap.put(GROUP_4, 60);
        aMap.put(GROUP_5, 1000);
        GROUP_LIMITS = aMap;
    }

    private final DefaultCategoryDataset dataset;
    private Results results;
    private final ObjectStore os;
    private final int items;

    public AgeGroupLdr(InterMineBag bag, ObjectStore os, String extra) {
        super();
        this.os = os;
        dataset = new DefaultCategoryDataset();
        Factory valueFactory = new Factory() {
            public Object create() {
                return new int[] {0, 0, 0};
            }
        };

        Map<String, int[]> resultTable
            = LazyMap.decorate(new HashMap<String, Integer>(), valueFactory);

        int total = addExpected(resultTable, bag);
        items = addActual(resultTable, bag);

        double ratio = Double.valueOf(items) / Double.valueOf(total);

        for (String ageGroup: GROUP_LIMITS.keySet()) {
            int[] vals = resultTable.get(ageGroup);
            dataset.addValue(vals[0], "Actual", ageGroup);
            dataset.addValue(Double.valueOf(vals[1]) * ratio, "Expected", ageGroup);
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Query getQuery(ColumnGroup col, InterMineBag bag) {
        PathQuery pq = new PathQuery(os.getModel());
        pq.addView(bag.getType() + ".age");
        if (ColumnGroup.ACTUAL == col) {
            pq.addConstraint(Constraints.in(bag.getType(), bag.getName()));
        }
        Map<String, InterMineBag> bags = new HashMap<String, InterMineBag>();
        bags.put(bag.getName(), bag);
        try {
            return MainHelper.makeQuery(pq, bags, new HashMap(), null, new HashMap());
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int addActual(Map<String, int[]> resultTable, InterMineBag bag) {
        Query q = getQuery(ColumnGroup.ACTUAL, bag);
        return doAdd(resultTable, q, 0);
    }

    private int addExpected(Map<String, int[]> resultTable, InterMineBag bag) {
        Query q = getQuery(ColumnGroup.EXPECTED, bag);
        return doAdd(resultTable, q, 1);
    }

    private int doAdd(Map<String, int[]> resultTable, Query q, int index) {
        results = os.execute(q);
        int grandTotal = 0;
        for (Iterator<?> it = results.iterator();it.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) it.next();
            Employee emp = (Employee) row.get(0);
            int age = emp.getAge();
            grandTotal++;
            for (Entry<String, Integer> pair: GROUP_LIMITS.entrySet()) {
                if (age < pair.getValue()) {
                    int[] vals = resultTable.get(pair.getKey());
                    vals[index]++;
                    break;
                }
            }
        }
        return grandTotal;
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
        return items;
    }
}
