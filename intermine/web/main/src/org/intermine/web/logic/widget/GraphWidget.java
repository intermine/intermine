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

import java.awt.BasicStroke;
import java.awt.Font;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Vector;

import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 * @author "Xavier Watkins"
 *
 */
public class GraphWidget extends Widget
{

    private int notAnalysed = 0;
    private DataSetLdr dataSetLdr;
    private InterMineBag bag;
    private ObjectStore os;
    private String fileName = null;
    private String imageMap = null;
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
    public List getElementInList() {
        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public void process() {
        try {
            String dataSetLoader = config.getDataSetLoader();
            Class<?> clazz = TypeUtil.instantiate(dataSetLoader);
            Constructor<?> constr = clazz.getConstructor(new Class[]
                {
                    InterMineBag.class, ObjectStore.class, String.class
                });
            dataSetLdr = (DataSetLdr) constr.newInstance(new Object[]
                {
                    bag, os, selectedExtraAttribute
                });

            notAnalysed = bag.getSize() - dataSetLdr.getWidgetTotal();

            // TODO use caching here
            JFreeChart chart = null;
            CategoryPlot plot = null;
            BarRenderer renderer = null;
            CategoryDataset graphDataSet = dataSetLdr.getDataSet();

            /* stacked bar chart */
            if (((GraphWidgetConfig) config).getGraphType() != null
                && ((GraphWidgetConfig) config).getGraphType().equals("StackedBarChart")) {
                chart = ChartFactory.createStackedBarChart(config.getTitle(), // chart title
                                ((GraphWidgetConfig) config).getDomainLabel(), // domain axis label
                                ((GraphWidgetConfig) config).getRangeLabel(), // range axis label
                                graphDataSet, // data
                                PlotOrientation.VERTICAL, true, true, // tooltips?
                                false // URLs?
                                );
                plot = chart.getCategoryPlot();
                chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

                renderer = (StackedBarRenderer) plot.getRenderer();
                // integers only
                NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

                /* regular bar chart */
            } else {
                    chart = ChartFactory.createBarChart(config.getTitle(), // chart title
                                    ((GraphWidgetConfig) config).getDomainLabel(),
                                    ((GraphWidgetConfig) config).getRangeLabel(),
                                    graphDataSet, // data
                                    PlotOrientation.VERTICAL, true, true, // tooltips?
                                    false // URLs?
                                    );
                    chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

                    if (selectedExtraAttribute != null
                    && !selectedExtraAttribute.startsWith("any")) {
                        TextTitle subtitleText = new TextTitle(selectedExtraAttribute);
                        subtitleText.setFont(new Font("SansSerif", Font.ITALIC, 10));
                        chart.addSubtitle(subtitleText);
                    }
                    plot = chart.getCategoryPlot();

                    renderer = new BarRenderer();

                    renderer.setItemMargin(0);
                    plot.setRenderer(renderer);
                    CategoryURLGenerator categoryUrlGen = null;
                    if (config.getLink() != null) {
                        // set series 0 to have URLgenerator specified in config file
                        // set series 1 to have no URL generator.
                        try {
                            Class<?> clazz2 = TypeUtil.instantiate(config.getLink());
                            Constructor<?> urlGenConstructor = clazz2.getConstructor(new Class[]
                                                                                               {
                                String.class, String.class
                                                                                               });
                            categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
                            .newInstance(new Object[]
                                                    {
                                bag.getName(), selectedExtraAttribute
                                                    });

                        } catch (Exception err) {
                            err.printStackTrace();
                        }

                        // renderer.setItemURLGenerator(null);
                        renderer.setSeriesItemURLGenerator(0, categoryUrlGen);
                        renderer.setSeriesItemURLGenerator(1, categoryUrlGen);
                    }

                    // integers only
                    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

                }

            if (chart.getTitle() != null) {
                chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 12));
            }

            // display values for each column
            CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator();
            plot.getRenderer().setBaseItemLabelsVisible(true);
            plot.getRenderer().setBaseItemLabelGenerator(generator);
            renderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
            renderer.setBaseNegativeItemLabelPosition(new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER));

            // TODO put this in a config file
            // set colors for each data series
            ChartColor blue = new ChartColor(47, 114, 255);
            renderer.setSeriesPaint(0, blue);

            ChartColor lightBlue = new ChartColor(159, 192, 255);
            renderer.setSeriesPaint(1, lightBlue);

            ChartColor darkBlue = new ChartColor(39, 77, 216);
            renderer.setSeriesPaint(2, darkBlue);

            // renderer.setDrawBarOutline(false);
            renderer.setSeriesOutlineStroke(1, new BasicStroke(0.0F));

            plot.getRenderer().setBaseToolTipGenerator(new ToolTipGenerator());

            // url to display genes
            // this may be already set individually for the different series
            if (config.getLink() != null) {
                Class<?> clazz2 = TypeUtil.instantiate(config.getLink());
                Constructor<?> urlGenConstructor = clazz2.getConstructor(new Class[]
                    {
                        String.class, String.class
                    });
                CategoryURLGenerator categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
                                .newInstance(new Object[]
                                    {
                                        bag.getName(), selectedExtraAttribute
                                    });
                plot.getRenderer().setBaseItemURLGenerator(categoryUrlGen);
            }

            renderer.setNegativeItemLabelPositionFallback(new ItemLabelPosition(
                            ItemLabelAnchor.OUTSIDE3, TextAnchor.BASELINE_LEFT));

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setUpperMargin(0.15);
            rangeAxis.setLowerMargin(0.15);

            Font labelFont = new Font("SansSerif", Font.BOLD, 12);
            plot.getDomainAxis().setLabelFont(labelFont);
            rangeAxis.setLabelFont(labelFont);
            plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);
            plot.getDomainAxis().setCategoryLabelPositions(
                            CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());

            // generate the image and imagemap
            fileName = ServletUtilities.saveChartAsPNG(chart,
                            ((GraphWidgetConfig) config).getWIDTH(),
                            ((GraphWidgetConfig) config).getHEIGHT(), info,
                            ((GraphWidgetConfig) config).getSession());
            imageMap = ImageMapUtilities.getImageMap("chart" + fileName, info);
        } catch (Exception e) {
            throw new RuntimeException("unexpected exception", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(@SuppressWarnings("unused") String[] selected)
        throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String[]>> getFlattenedResults() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */

    public boolean getHasResults() {
        return (dataSetLdr.getResults() != null && dataSetLdr.getResults().size() > 0);
    }

    /**
     * {@inheritDoc}
     */
    public void setNotAnalysed(int notAnalysed) {
        this.notAnalysed = notAnalysed;
    }

    /**
     * {@inheritDoc}
     */
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
        + "\" width=\"" + ((GraphWidgetConfig) config).getWIDTH()
        + "\" height=\"" + ((GraphWidgetConfig) config).getHEIGHT()
        + "\" usemap=\"#chart" + fileName + "\">";
        StringBuffer sb = new StringBuffer(img);
        sb.append(imageMap);
        return sb.toString();
    }
}
