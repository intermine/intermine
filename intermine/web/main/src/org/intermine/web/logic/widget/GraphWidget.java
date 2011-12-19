package org.intermine.web.logic.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.FieldPosition;
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
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.PieDataset;
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
 *
 */
public class GraphWidget extends Widget
{
    private static final Logger LOG = Logger.getLogger(GraphWidget.class);
    private int notAnalysed = 0;
    private DataSetLdr dataSetLdr;
    private InterMineBag bag;
    private ObjectStore os;
    private String fileName = null;
    private String imageMap = null;
    private String selectedExtraAttribute;
    private static final ChartColor BLUE = new ChartColor(47, 114, 255);
    private static final ChartColor LIGHT_BLUE = new ChartColor(159, 192, 255);
    private static final ChartColor DARK_BLUE = new ChartColor(39, 77, 216);
    private Dataset graphDataSet;

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

        String dataSetLoader = config.getDataSetLoader();
        Class<?> clazz = TypeUtil.instantiate(dataSetLoader);
        try {
            Constructor<?> constr = clazz.getConstructor(new Class[] {InterMineBag.class,
                ObjectStore.class, String.class});
            dataSetLdr = (DataSetLdr) constr.newInstance(new Object[] {bag, os,
                selectedExtraAttribute});
            notAnalysed = bag.getSize() - dataSetLdr.getWidgetTotal();
        } catch (Exception err) {
            LOG.warn("Error rendering graph widget.", err);
            return;
        }

        if (dataSetLdr == null || dataSetLdr.getDataSet() == null) {
            LOG.warn("No data found for graph widget");
            return;
        }
        BarRenderer.setDefaultShadowsVisible(false);
        JFreeChart chart = null;
        graphDataSet = dataSetLdr.getDataSet();
        String graphType = ((GraphWidgetConfig) config).getGraphType();
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());

        if (StringUtils.isNotEmpty(graphType) && "ScatterPlot".equals(graphType)) {
            chart = ChartFactory.createScatterPlot(
                    config.getTitle(),
                    ((GraphWidgetConfig) config).getDomainLabel(),
                    ((GraphWidgetConfig) config).getRangeLabel(),
                    (XYDataset) graphDataSet,
                    PlotOrientation.VERTICAL,
                    true, true, false);
            XYPlot plot = chart.getXYPlot();

            XYItemRenderer xyrenderer = plot.getRenderer();
            StandardXYItemRenderer regressionRenderer
                = new StandardXYItemRenderer();
            regressionRenderer.setBaseSeriesVisibleInLegend(false);
            plot.setDataset(1, regress((XYDataset) graphDataSet));
            plot.setRenderer(1, regressionRenderer);
            xyrenderer.setSeriesStroke(0, new BasicStroke(3.0f));
            xyrenderer.setSeriesStroke(1, new BasicStroke(3.0f));

            xyrenderer.setSeriesPaint(0, BLUE);
            xyrenderer.setSeriesPaint(1, LIGHT_BLUE);

            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

            DivNumberFormat formatter = new DivNumberFormat(1000);
            NumberAxis axis = (NumberAxis) plot.getRangeAxis();
            axis.setNumberFormatOverride(formatter);

        } else if (StringUtils.isNotEmpty(graphType) && "XYLineChart".equals(graphType)) {

            chart = ChartFactory.createXYLineChart(config.getTitle(),
                    ((GraphWidgetConfig) config).getDomainLabel(),
                    ((GraphWidgetConfig) config).getRangeLabel(), (XYDataset) graphDataSet,
                    PlotOrientation.VERTICAL, true, true, false);

            XYPlot plot = chart.getXYPlot();

            XYItemRenderer xyrenderer = plot.getRenderer();
            xyrenderer.setSeriesStroke(0, new BasicStroke(3.0f));
            xyrenderer.setSeriesStroke(1, new BasicStroke(3.0f));

            xyrenderer.setSeriesPaint(0, BLUE);
            xyrenderer.setSeriesPaint(1, LIGHT_BLUE);

            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

            DivNumberFormat formatter = new DivNumberFormat(1000);
            NumberAxis axis = (NumberAxis) plot.getRangeAxis();
            axis.setNumberFormatOverride(formatter);
        } else if (StringUtils.isNotEmpty(graphType) && "PieChart".equals(graphType)) {
            chart = ChartFactory.createPieChart(
                    config.getTitle(), (PieDataset) graphDataSet,
                    true, true, false);
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setNoDataMessage("No data available");
            plot.setCircular(true);
            plot.setLabelGap(0.02);
            plot.setBackgroundPaint(Color.white);
            Font labelFont = new Font("SansSerif", Font.BOLD, 12);
            plot.setLabelFont(labelFont);
            plot.setMaximumLabelWidth(0.20d);
            @SuppressWarnings("rawtypes")
            List keys = ((PieDataset) graphDataSet).getKeys();
            int mod = (keys.size() % 3 == 1) ? 2 : 3;
            int i = 0;
            for (Object key: keys) {
                Color paint;
                switch (i++ % mod) {
                    case 0: paint = BLUE; break;
                    case 1: paint = LIGHT_BLUE; break;
                    case 2: paint = DARK_BLUE; break;
                    default: paint = BLUE;
                }
                plot.setSectionPaint((Comparable<?>) key, paint);
            }
        } else if (StringUtils.isNotEmpty(graphType) && "StackedBarChart".equals(graphType)) {
            chart = ChartFactory.createStackedBarChart(config.getTitle(), // chart title
                    ((GraphWidgetConfig) config).getDomainLabel(), // domain axis label
                    ((GraphWidgetConfig) config).getRangeLabel(), // range axis label
                    (CategoryDataset) graphDataSet, // data
                    PlotOrientation.HORIZONTAL, true, true, // include legend,tooltips?
                    false // URLs?
            );
            CategoryPlot categoryPlot = chart.getCategoryPlot();
            CategoryItemRenderer categoryRenderer = categoryPlot.getRenderer();
            categoryRenderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(
                        ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT));
            categoryRenderer.setBaseNegativeItemLabelPosition(new ItemLabelPosition(
                        ItemLabelAnchor.OUTSIDE9, TextAnchor.CENTER_RIGHT));

            ((GraphWidgetConfig) config).setHeight(400);
            formatBarCharts(categoryPlot);
            setURLGen(categoryRenderer);
        /* regular bar chart */
        } else {
            chart = ChartFactory.createBarChart(config.getTitle(), // chart title
                                                ((GraphWidgetConfig) config).getDomainLabel(),
                                                ((GraphWidgetConfig) config).getRangeLabel(),
                                                (CategoryDataset) graphDataSet, // data
                                                PlotOrientation.VERTICAL, true, true, // tooltips?
                                                false // URLs?
            );

            if (selectedExtraAttribute != null
                            && !selectedExtraAttribute.startsWith("any")) {
                TextTitle subtitleText = new TextTitle(selectedExtraAttribute);
                subtitleText.setFont(new Font("SansSerif", Font.ITALIC, 10));
                chart.addSubtitle(subtitleText);
            }
            CategoryPlot categoryPlot = chart.getCategoryPlot();
            CategoryItemRenderer categoryRenderer = new BarRenderer();
            ((BarRenderer) categoryRenderer).setItemMargin(0);
            categoryRenderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(
                        ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
            categoryRenderer.setBaseNegativeItemLabelPosition(new ItemLabelPosition(
                        ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER));
            categoryPlot.setRenderer(categoryRenderer);

            // rotate the category labels
            categoryPlot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions
                    .createUpRotationLabelPositions(Math.PI / 6.0));

            setURLGen(categoryRenderer);

            ((BarRenderer) categoryRenderer).setNegativeItemLabelPositionFallback(
                new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.BASELINE_LEFT));

            formatBarCharts(categoryPlot);
        }

        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 12));
        }

        chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
// this would speed up chart rendering, but it wouldn't look as nice.
//        chart.setAntiAlias(false);
        ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());

        // generate the image and imagemap
        try {
            fileName = ServletUtilities.saveChartAsPNG(chart,
                    ((GraphWidgetConfig) config).getWidth(),
                    ((GraphWidgetConfig) config).getHeight(), info,
                    ((GraphWidgetConfig) config).getSession());
        } catch (IOException e) {
            throw new RuntimeException("error rendering html", e);
        }

        imageMap = ImageMapUtilities.getImageMap("chart" + fileName, info);
    }

    private void setURLGen(CategoryItemRenderer categoryRenderer) {
        if (config.getLink() == null) {
            return;
        }
        CategoryURLGenerator categoryUrlGen = null;

        // set series 0 to have URLgenerator specified in config file
        // set series 1 to have no URL generator.
        try {
            Class<?> clazz2 = TypeUtil.instantiate(config.getLink());
            Constructor<?> urlGenConstructor = clazz2.getConstructor(new Class[] {String.class,
                String.class});
            categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
                .newInstance(new Object[] {bag.getName(), selectedExtraAttribute});
        } catch (Exception err) {
            err.printStackTrace();
            return;
        }

        // renderer.setItemURLGenerator(null);
        categoryRenderer.setSeriesItemURLGenerator(0, categoryUrlGen);
        categoryRenderer.setSeriesItemURLGenerator(1, categoryUrlGen);
    }


    private void formatBarCharts(CategoryPlot categoryPlot) {
        if (categoryPlot != null) {
            // display values for each column
            CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator();
            categoryPlot.getRenderer().setBaseItemLabelsVisible(true);
            categoryPlot.getRenderer().setBaseItemLabelGenerator(generator);
            categoryPlot.getRenderer().setBaseToolTipGenerator(new ToolTipGenerator());

            NumberAxis rangeAxis = (NumberAxis) categoryPlot.getRangeAxis();
            rangeAxis.setUpperMargin(0.15);
            rangeAxis.setLowerMargin(0.15);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

            Font labelFont = new Font("SansSerif", Font.BOLD, 12);
            categoryPlot.getDomainAxis().setLabelFont(labelFont);
            rangeAxis.setLabelPaint(Color.darkGray);
            rangeAxis.setLabelFont(labelFont);
            categoryPlot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);

            categoryPlot.getRenderer().setSeriesPaint(0, BLUE);
            categoryPlot.getRenderer().setSeriesPaint(1, LIGHT_BLUE);
            categoryPlot.getRenderer().setSeriesPaint(2, DARK_BLUE);

            categoryPlot.getRenderer().setSeriesOutlineStroke(1, new BasicStroke(0.0F));

            categoryPlot.setBackgroundPaint(Color.white);
            categoryPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            categoryPlot.setDomainGridlinesVisible(true);
            categoryPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);

//            StandardChartTheme legacyTheme
//            = (StandardChartTheme) StandardChartTheme.createLegacyTheme();
//            legacyTheme.applyToCategoryPlot((CategoryPlot) categoryPlot);


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
        return (dataSetLdr != null
                && dataSetLdr.getResults() != null
                && dataSetLdr.getResults().size() > 0);
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

    /**
     * Get the HTML that will display the graph and imagemap
     * @return the HTML as a String
     */
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
    }

    @SuppressWarnings({ "rawtypes" })
    public List<List<Object>> getResults() {
        Dataset dataSet = dataSetLdr.getDataSet();
        if (dataSet == null) {
            throw new RuntimeException("null dataset");
        }
        List<List<Object>> ret = new LinkedList<List<Object>>();
        if (graphDataSet instanceof CategoryDataset) {
            CategoryDataset ds = (CategoryDataset) dataSet;
            List<Object> rowKeys = ds.getRowKeys();
            List<Object> labels = new LinkedList<Object>();
            labels.add(""); // Dummy label
            labels.addAll(rowKeys);
            ret.add(labels);
            int colCount = ds.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                List<Object> row = new LinkedList<Object>();
                row.add(ds.getColumnKey(i));
                for (Object rowKey: rowKeys) {
                    int rIdx = ds.getRowIndex((Comparable) rowKey);
                    row.add(ds.getValue(rIdx, i));
                }
                ret.add(row);
            }
        } else if (graphDataSet instanceof XYDataset) {
            XYDataset ds = (XYDataset) graphDataSet;
            List<Object> headers = new LinkedList<Object>();
            headers.add("");
            int sc = ds.getSeriesCount();
            if (sc > 1) {
                throw new RuntimeException("Can't do that, sorry");
            }
            for (int i = 0; i < sc; i++) {
                headers.add(ds.getSeriesKey(i));
            }
            ret.add(headers);
            int ic = ds.getItemCount(0);
            String graphType = ((GraphWidgetConfig) config).getGraphType();
            boolean stringify = !"ScatterPlot".equals(graphType);
            for (int i = 0; i < ic; i++) {
                List<Object> row = new LinkedList<Object>();
                Double x = ds.getXValue(0, i);
                row.add(stringify ? Double.toString(x) : x);
                row.add(ds.getYValue(0, i));
                ret.add(row);
            }
        } else if (graphDataSet instanceof PieDataset) {
            PieDataset ds = (PieDataset) graphDataSet;
            List<Object> headers = new LinkedList<Object>();
            headers.add(((GraphWidgetConfig) config).getDomainLabel());
            headers.add(((GraphWidgetConfig) config).getRangeLabel());
            ret.add(headers);
            int total = dataSetLdr.getWidgetTotal();
            for (Object key: ds.getKeys()) {
                List<Object> row = new LinkedList<Object>();
                row.add(key.toString());
                row.add(ds.getValue((Comparable) key));
                ret.add(row);
            }
        } else {
            throw new RuntimeException("Unknown data-set type");
        }

        return ret;
    }

    private static XYDataset regress(XYDataset data) {
        // Determine bounds
        double xMin = Double.MAX_VALUE, xMax = 0;
        for (int i = 0; i < data.getSeriesCount(); i++) {
            for (int j = 0; j < data.getItemCount(i); j++) {
                double x = data.getXValue(i, j);
                if (x < xMin) {
                    xMin = x;
                }
                if (x > xMax) {
                    xMax = x;
                }
            }
        }
        // Create 2-point series for each of the original series
        XYSeriesCollection coll = new XYSeriesCollection();
        for (int i = 0; i < data.getSeriesCount(); i++) {
            int n = data.getItemCount(i);
            double sx = 0, sy = 0, sxx = 0, sxy = 0;
            for (int j = 0; j < n; j++) {
                double x = data.getXValue(i, j);
                double y = data.getYValue(i, j);
                sx += x;
                sy += y;
                sxx += x * x;
                sxy += x * y;
            }
            double b = (n * sxy - sx * sy) / (n * sxx - sx * sx);
            double a = sy / n - b * sx / n;
            XYSeries regr = new XYSeries(data.getSeriesKey(i));
            regr.add(xMin, a + b * xMin);
            regr.add(xMax, a + b * xMax);
            coll.addSeries(regr);
        }
        return coll;
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

