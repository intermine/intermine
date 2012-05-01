package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.api.profile.InterMineBag;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
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
    @SuppressWarnings("rawtypes")
    private List bagContent = new Vector();

    /**
     * @param config configuration for this widget
     * @param interMineBag bag for this widget
     * @param os objecstore
     */
    public TableWidget(TableWidgetConfig config, InterMineBag interMineBag, ObjectStore os) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        createBagContent();
        process();
    }


    @SuppressWarnings("unchecked")
    private void createBagContent() {

        List<Integer> bagResults = bag.getContentsAsIds();
        Iterator<Integer> it = bagResults.iterator();

        while (it.hasNext()) {
            InterMineObject oj = null;
            try {
                oj = os.getObjectById(it.next());
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ObjectStoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String[] tmp = oj.toString().split(",");
            String identifier = null;
            Pattern primaryIdentifier = Pattern.compile("P\\w+(I)\\w+(r)\\=\\w*");
            for (int i = 0; i < tmp.length; i++) {
                Matcher matcher = primaryIdentifier.matcher(tmp[i].trim());
                if (matcher.find()) {
                    identifier = tmp[i];
                    break;
                }
            }
            if (identifier != null) {
                String[] tmp1 = identifier.split("\"");
                if (tmp1.length > 1) {
                    bagContent.add(tmp1[1]);
                }
            }
        }
    }

    /**
     * checks if elem is in bag
     * @return true if elem is in bag
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List getElementInList() {
        return bagContent;
    }

    /**
     * {@inheritDoc}
     */
    public void process() {
        try {
            bagWidgLdr = new TableWidgetLdr(config, bag, os);
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
    @SuppressWarnings({ "rawtypes" })
    public List getColumns() {
        return bagWidgLdr.getColumns();
    }

    @Override
    public List<List<Object>> getResults() {
        return bagWidgLdr.getFlattenedResults();
    }

    public String getType() {
        return bagWidgLdr.getType();
    }

    public PathQuery getPathQuery() {
        return bagWidgLdr.createPathQuery();
    }

}
