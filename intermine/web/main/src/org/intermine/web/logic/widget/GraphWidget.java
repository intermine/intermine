package org.intermine.web.logic.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.CategoryTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * @author "Xavier Watkins"
 * @author "Alex Kalderimis"
 */
public class GraphWidget extends Widget
{
    private static final Logger LOG = Logger.getLogger(GraphWidget.class);
    private int notAnalysed = 0;
    private GraphWidgetLoader grapgWidgetLdr;
    private InterMineBag bag;
    private ObjectStore os;
    private String selectedExtraAttribute;


    /**
     * @param config config for widget
     * @param interMineBag bag for widget
     * @param os objectstore
     * @param selectedExtraAttribute extra attribute
     */
    public GraphWidget(GraphWidgetConfig config, InterMineBag interMineBag, ObjectStore os,
                       String selectedExtraAttribute) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        this.selectedExtraAttribute = selectedExtraAttribute;
        process();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List getElementInList() {
        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process() {
        grapgWidgetLdr = new GraphWidgetLoader(bag, os, (GraphWidgetConfig) config);
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
    public List<List<String[]>> getFlattenedResults() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getHasResults() {
        return (grapgWidgetLdr != null
                && grapgWidgetLdr.getResults() != null
                && grapgWidgetLdr.getResults().size() > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotAnalysed(int notAnalysed) {
        this.notAnalysed = notAnalysed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNotAnalysed() {
        return notAnalysed;
    }

 /*   *//**
     * Get the HTML that will display the graph and imagemap
     * @return the HTML as a String
     *//*
    public String getHtml() {
        // IE doesn't support base64, so for now we are just going to pass back location of png file
        // see http://en.wikipedia.org/wiki/Data:_URI_scheme
        String img = "<img src=\"loadTmpImg.do?fileName=" + fileName
            + "\" width=\"" + ((GraphWidgetConfig) config).getWidth()
            + "\" height=\"" + ((GraphWidgetConfig) config).getHeight()
            + "\" usemap=\"#chart" + fileName + "\">";
        StringBuffer sb = new StringBuffer(img);
        sb.append(imageMap);
        return sb.toString();
    }*/

    public List<List<Object>> getResults() {
        return grapgWidgetLdr.getResultTable();
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

