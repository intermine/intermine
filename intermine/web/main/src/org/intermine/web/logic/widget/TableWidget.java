package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.config.TableWidgetConfig;

/**
 * @author "Xavier Watkins"
 *
 */
public class TableWidget extends Widget
{

    private int notAnalysed = 0;
    private InterMineBag bag;
    private ObjectStore os;
    private TableWidgetLdr bagWidgLdr;

    /**
     * @param config configuration for this widget
     * @param interMineBag bag for this widget
     * @param os objecstore
     * @param selectedExtraAttribute not used
     */
    public TableWidget(TableWidgetConfig config, InterMineBag interMineBag, ObjectStore os,
        String selectedExtraAttribute) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        process();
    }

    /**
     * {@inheritDoc}
     */
    public void process() {
            try {
                bagWidgLdr = new TableWidgetLdr(config, bag, os);
                notAnalysed = bag.getSize() - bagWidgLdr.getWidgetTotal();
            } catch (ObjectStoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }


    /**
     * Get the flattened results
     * @return the List of flattened results
     */
    public List getFlattenedResults() {
        return bagWidgLdr.getFlattenedResults();
    }


    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[] selected) throws Exception {
        return bagWidgLdr.getExportResults(selected);
    }


    /**
     * {@inheritDoc}
     */
    public boolean getHasResults() {
        return (bagWidgLdr.getFlattenedResults().size() > 0);
    }


    /**
     * {@inheritDoc}
     */
    public int getNotAnalysed() {
        return notAnalysed;
    }


    /**
     * {@inheritDoc}
      */
     public void setNotAnalysed(int notAnalysed) {
         this.notAnalysed = notAnalysed;
     }


     /**
      * Get the columns
      * @return the columns
      */
     public List getColumns() {
         return bagWidgLdr.getColumns();
     }

}
