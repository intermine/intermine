package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.TableWidgetConfig;

/**
 * A table widget gets the values for a given path for all the items in a list.
 * @author "Xavier Watkins"
 * @author Daniela Butano
 *
 */
public class TableWidget extends Widget
{
    private TableWidgetLdr bagWidgLdr;

    /**
     * @param config configuration for this widget
     * @param interMineBag bag for this widget
     * @param ids intermine IDs, required if bag is NULL
     * @param os objecstore
     */
    public TableWidget(TableWidgetConfig config, InterMineBag interMineBag, ObjectStore os,
        String ids) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        this.ids = ids;
    }

    /**
     * {@inheritDoc}
     */
    public void process() {
        checkNotProcessed();
        try {
            bagWidgLdr = new TableWidgetLdr(config, bag, os, ids);
            notAnalysed = bag.getSize() - bagWidgLdr.getWidgetTotal();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Get the flattened results
     * @return the List of flattened results
     */
    @SuppressWarnings("rawtypes")
    public List getFlattenedResults() {
        checkProcessed();
        return bagWidgLdr.getFlattenedResults();
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[] selected) throws Exception {
        checkProcessed();
        return bagWidgLdr.getExportResults(selected);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getHasResults() {
        checkProcessed();
        return (bagWidgLdr.getFlattenedResults().size() > 0);
    }

    /**
     * Get the columns
     * @return the columns
     */
    @SuppressWarnings("rawtypes")
    public List getColumns() {
        checkProcessed();
        return bagWidgLdr.getColumns();
    }

    private void checkProcessed() {
        if (bagWidgLdr == null) {
            throw new IllegalStateException("This widget has not been processed yet.");
        }
    }

    private void checkNotProcessed() {
        if (bagWidgLdr != null) {
            throw new IllegalStateException("This widget has already bveen processed.");
        }
    }

    @Override
    public List<List<Object>> getResults() {
        checkProcessed();
        return bagWidgLdr.getFlattenedResults();
    }

    @Override
    public PathQuery getPathQuery() {
        checkProcessed();
        return bagWidgLdr.createPathQuery();
    }
}
