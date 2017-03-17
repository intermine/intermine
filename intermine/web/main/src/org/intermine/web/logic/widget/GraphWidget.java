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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

/**
 * @author Xavier Watkins
 * @author Alex Kalderimis
 * @author Daniela Butano
 */
public class GraphWidget extends Widget
{
    private static final String NOT_ACCEPTABLE
        = "The %s chart widget only accepts lists of %s, but you provided a list of %s";
    private static final String WIDGET_TYPE_NOT_IN_MODEL
        = "This widget is configured incorrectly. Its type is not in the model: ";
    private static final String NOT_IN_MODEL
        = "This bag has a type not found in the current model: ";
    private static final Logger LOG = Logger.getLogger(GraphWidget.class);
    private GraphWidgetLoader grapgWidgetLdr = null;
    private String filter;

    /**
     * @param config config for widget
     * @param interMineBag bag for widget
     * @param os objectstore
     * @param ids intermine IDs, required if bag is NULL
     * @param options The options for this widget.
     */
    public GraphWidget(GraphWidgetConfig config, InterMineBag interMineBag, ObjectStore os,
                       WidgetOptions options, String ids) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        this.ids = ids;
        this.filter = options.getFilter();
        validateBagType();
    }

    /** @param filter Set the filter to something else **/
    public void setFilter(String filter) {
        checkNotProcessed();
        this.filter = filter;
    }

    private void checkNotProcessed() {
        if (grapgWidgetLdr != null) {
            throw new IllegalStateException("This widget has already been processed.");
        }
    }
    private void checkProcessed() {
        if (grapgWidgetLdr == null) {
            throw new IllegalStateException("This widget has not been processed yet.");
        }
    }

    /**
     * Validate the bag type using the attribute typeClass set in the config file.
     * Throws a ResourceNotFoundException if it's not valid
     */
    private void validateBagType() {
        final String type = bag.getType();
        final String accepts = config.getTypeClass();
        ClassDescriptor bagType = os.getModel().getClassDescriptorByName(type);
        if (bagType == null) {
            throw new IllegalArgumentException(NOT_IN_MODEL + type);
        }
        ClassDescriptor accepted = os.getModel().getClassDescriptorByName(accepts);
        if (accepted == null) {
            throw new IllegalStateException(WIDGET_TYPE_NOT_IN_MODEL + accepts);
        }
        if ("InterMineObject".equals(accepted.getUnqualifiedName())) {
            return; // This widget accepts anything, however useless.
        } else if (bagType.equals(accepted)) {
            return; // Exact match.
        } else if (bagType.getAllSuperDescriptors().contains(accepted)) {
            return; // Sub-class.
        }
        throw new IllegalArgumentException(
                String.format(NOT_ACCEPTABLE, config.getId(), accepts, type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process() {
        checkNotProcessed();
        grapgWidgetLdr = new GraphWidgetLoader(bag, os, (GraphWidgetConfig) config, filter, ids);
        if (grapgWidgetLdr == null || grapgWidgetLdr.getResults() == null) {
            LOG.warn("No data found for graph widget");
            return;
        }
        try {
            notAnalysed = bag.getSize() - grapgWidgetLdr.getWidgetTotal();
        } catch (Exception err) {
            LOG.warn("Error rendering graph widget.", err);
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<String>> getExportResults(String[] selected)
        throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getHasResults() {
        checkProcessed();
        return (grapgWidgetLdr != null
                && grapgWidgetLdr.getResults() != null
                && grapgWidgetLdr.getResults().size() > 0);
    }

    @Override
    public List<List<Object>> getResults() {
        checkProcessed();
        return grapgWidgetLdr.getResultTable();
    }

    /**
     * Returns the pathquery based on the views set in config file and the bag constraint.
     * Executed when the user click on 'View results' button in the graph widget.
     * @return the query generated
     */
    public PathQuery getPathQuery() {
        PathQuery q = createPathQueryView(os, config);

        // bag constraint
        if (((GraphWidgetConfig) config).isListPathSet()) {
            q.addConstraint(Constraints.in(((GraphWidgetConfig) config).getListPath(),
                                           bag.getName()));
        } else {
            q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        }

        String prefix = config.getStartClass() + ".";
        //category constraint
        q.addConstraint(Constraints.eq(prefix + ((GraphWidgetConfig) config).getCategoryPath(),
                                      "%category"));
        if (!((GraphWidgetConfig) config).comparesActualToExpected()
                && ((GraphWidgetConfig) config).hasSeries()) {
            //series constraint
            q.addConstraint(Constraints.eq(prefix + ((GraphWidgetConfig) config).getSeriesPath(),
                                          "%series"));
        }

        //constraints set in the constraints attribute
        List<PathConstraint> pathConstraints = config.getPathConstraints();
        for (PathConstraint pc : pathConstraints) {
            if (!WidgetConfigUtil.isFilterConstraint(config, pc)) {
                if (pc.getOp().equals(ConstraintOp.EQUALS)) {
                    q.addConstraint(Constraints.eq(prefix + pc.getPath(),
                            PathConstraint.getValue(pc)));
                } else if (pc.getOp().equals(ConstraintOp.NOT_EQUALS)) {
                    q.addConstraint(Constraints.neq(prefix + pc.getPath(),
                            PathConstraint.getValue(pc)));
                }
            }
        }
        return q;
    }

    /**
     * Returns the pathquery based on the classkey.
     * Executed when the user selects any columns in the in the graph widget and a popup is shown.
     * @return the query generated
     */
    public PathQuery getSimplePathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        List<String> keyFieldNames = bag.getKeyFieldNames();
        String prefix;
        if (!((GraphWidgetConfig) config).isListPathSet()) {
            prefix = config.getStartClass() + ".";
        } else {
            prefix = ((GraphWidgetConfig) config).getListPath() + ".";
        }
        for (String keyFieldName : keyFieldNames) {
            if (!keyFieldName.startsWith(prefix)) {
                keyFieldName = prefix + keyFieldName;
            }
            q.addView(keyFieldName);
        }

        // bag constraint
        if (((GraphWidgetConfig) config).isListPathSet()) {
            q.addConstraint(Constraints.in(((GraphWidgetConfig) config).getListPath(),
                                           bag.getName()));
        } else {
            q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        }

        //category constraint
        q.addConstraint(Constraints.eq(prefix + ((GraphWidgetConfig) config).getCategoryPath(),
                                      "%category"));
        //series constraint
        if (((GraphWidgetConfig) config).hasSeries()) {
            q.addConstraint(Constraints.eq(prefix + ((GraphWidgetConfig) config).getSeriesPath(),
                                      "%series"));
        }

        return q;
    }

    /**
     * class used to format the p-values on the graph
     * @author julie
     */
    public class DivNumberFormat extends DecimalFormat
    {
        /**
         * Generated serial-id.
         */
        private static final long serialVersionUID = 8247038065756921184L;
        private int magnitude;

        /**
         * @param magnitude what to multiply the p-value by
         */
        public DivNumberFormat(int magnitude) {
            this.magnitude = magnitude;
        }

        /**
         * @param number number to format
         * @param result buffer to put the result in
         * @param fieldPosition the field position
         * @return the format
         */
        @Override
        public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
        {
            return super.format(number * magnitude, result, fieldPosition);
        }

        /**
         * @param number number to format
         * @param result buffer to put the result in
         * @param fieldPosition the field position
         * @return the format
         */
        @Override
        public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
            return super.format(number * magnitude, result, fieldPosition);
        }
    }
}
