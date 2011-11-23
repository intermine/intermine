package org.intermine.web.logic.widget;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;

public class GraphWidgetLoader {



    private String categoryPath;
    private String countPath;
    private String bagType;
    private String bagPath;
    private String series1;
    private String series2;
    private ObjectStore os;

    // TODO the parameters need to be updated
    public void GraphWidgetLoader(ObjectStore os, String categoryPath, String countPath,
            String bagType, String bagPath, String series1, String series2) {
        this.os = os;
        this.categoryPath = categoryPath;
        this.countPath = countPath;
        this.bagType = bagType;
        this.bagPath = bagPath;
        this.series1 = series1;
        this.series2 = series2;
    }

    public Results getResultTable(InterMineBag bag) {
        // TODO check the bag type

        Query q = makeQuery(bag);
        return os.execute(q);
    }

    public Query makeQuery(InterMineBag bag) {
        Query q = new Query();

        // Create a Path object for each path in configuration

        // Create QueryClasses

        // Add ContainsConstraints

        // Add attributes to select


        return q;
    }

}
