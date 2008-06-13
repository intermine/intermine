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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
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
 * Configuration object describing details of a graph displayer
 *
 * @author Xavier Watkins
 */
public class GraphWidget extends Widget
{
    private String domainLabel;
    private String rangeLabel;
    private String toolTipGen;

    private String graphType;
    private String fileName = null;
    private String imageMap = null;
    private static final int WIDTH = 430;
    private static final int HEIGHT = 350;
    private String extraAttributeClass, externalLink, externalLinkLabel;
    private HttpSession session;
    private DataSetLdr dataSetLdr;
    private int notAnalysed = 0;

    /**
     * {@inheritDoc}
     */
    public void process(InterMineBag imBag, ObjectStore os) {
        try {
            String dataSetLoader = getDataSetLoader();
            Class<?> clazz = TypeUtil.instantiate(dataSetLoader);
            Constructor<?> constr = clazz.getConstructor(new Class[]
                {
                    InterMineBag.class, ObjectStore.class, String.class
                });
            dataSetLdr = (DataSetLdr) constr.newInstance(new Object[]
                {
                    imBag, os, getSelectedExtraAttribute()
                });

            notAnalysed = imBag.getSize() - dataSetLdr.getWidgetTotal();

            // TODO use caching here
            JFreeChart chart = null;
            CategoryPlot plot = null;
            BarRenderer renderer = null;
            CategoryDataset graphDataSet = dataSetLdr.getDataSet();

            /* stacked bar chart */
            if (graphType.equals("StackedBarChart")) {
                chart = ChartFactory.createStackedBarChart(getTitle(), // chart title
                                getDomainLabel(), // domain axis label
                                getRangeLabel(), // range axis label
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
                    chart = ChartFactory.createBarChart(getTitle(), // chart title
                                    getDomainLabel(), // domain axis label
                                    getRangeLabel(), // range axis label
                                    graphDataSet, // data
                                    PlotOrientation.VERTICAL, true, true, // tooltips?
                                    false // URLs?
                                    );
                    chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

                    if (getSelectedExtraAttribute() != null
                    && !getSelectedExtraAttribute().startsWith("any")) {
                        TextTitle subtitleText = new TextTitle(getSelectedExtraAttribute());
                        subtitleText.setFont(new Font("SansSerif", Font.ITALIC, 10));
                        chart.addSubtitle(subtitleText);
                    }
                    plot = chart.getCategoryPlot();

                    renderer = new BarRenderer();

                    renderer.setItemMargin(0);
                    plot.setRenderer(renderer);
                    CategoryURLGenerator categoryUrlGen = null;
                    // set series 0 to have URLgenerator specified in config file
                    // set series 1 to have no URL generator.
                    try {
                        Class<?> clazz2 = TypeUtil.instantiate(getLink());
                        Constructor<?> urlGenConstructor = clazz2.getConstructor(new Class[]
                            {
                                String.class, String.class
                            });
                        categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
                                        .newInstance(new Object[]
                                            {
                                                imBag.getName(), getSelectedExtraAttribute()
                                            });

                    } catch (Exception err) {
                        err.printStackTrace();
                    }

                    // renderer.setItemURLGenerator(null);
                    renderer.setSeriesItemURLGenerator(0, categoryUrlGen);
                    renderer.setSeriesItemURLGenerator(1, categoryUrlGen);

                    // integers only
                    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

                }

            chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 12));

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

            Class<?> clazz1 = TypeUtil.instantiate(toolTipGen);
            Constructor<?> toolTipConstructor = clazz1.getConstructor(new Class[]
                {});
            CategoryToolTipGenerator categoryToolTipGen
            = (CategoryToolTipGenerator) toolTipConstructor
                            .newInstance(new Object[]
                                {});
            plot.getRenderer().setBaseToolTipGenerator(categoryToolTipGen);

            // url to display genes
            // this may be already set individually for the different series
            if (getLink() != null) {
                Class<?> clazz2 = TypeUtil.instantiate(getLink());
                Constructor<?> urlGenConstructor = clazz2.getConstructor(new Class[]
                    {
                        String.class, String.class
                    });
                CategoryURLGenerator categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
                                .newInstance(new Object[]
                                    {
                                        imBag.getName(), getSelectedExtraAttribute()
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
            fileName = ServletUtilities.saveChartAsPNG(chart, WIDTH, HEIGHT, info, session);
            imageMap = ImageMapUtilities.getImageMap("chart" + fileName, info);
        } catch (Exception e) {
            throw new RuntimeException("unexpected exception", e);
        }
    }

    /**
     * @param session the session to set
     */
    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * Get the domainLabel
     * @return the domainLabel
     */
    public String getDomainLabel() {
        return domainLabel;
    }


    /**
     * Set the value of domainLabel
     * @param domainLabel a String
     */
    public void setDomainLabel(String domainLabel) {
        this.domainLabel = domainLabel;
    }


    /**
     * Get the value of rangeLabel
     * @return the rangeLabel
     */
    public String getRangeLabel() {
        return rangeLabel;
    }


    /**
     * Set the value of rangeLabel
     * @param rangeLabel a String
     */
    public void setRangeLabel(String rangeLabel) {
        this.rangeLabel = rangeLabel;
    }


    /**
     * Get the value of toolTipGen
     * @return the toolTipGen
     */
    public String getToolTipGen() {
        return toolTipGen;
    }


    /**
     * Set the value of toolTipGen
     * @param toolTipGen a String
     */
    public void setToolTipGen(String toolTipGen) {
        this.toolTipGen = toolTipGen;
    }


    /**
     * @param graphType type of graph, e.g. BarChart, StackedBarChart
     */
    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }


    /**
     * Get the type of this graph, e.g. BarChart, StackedBarChart
     * @return the type of this graph
     */
    public String getGraphType() {
        return graphType;
    }

    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + " domainLabel=\"" + domainLabel + " rangeLabel=\""
               + rangeLabel + " dataSetLoader=\"" + getDataSetLoader() + " toolTipGen=\""
               + toolTipGen + " urlGen=\"" + getLink() + "\" />";
    }

    /**
     * Get the HTML that will display the graph and imagemap
     * @return the HTML as a String
     */
    public String getHtml() {
        StringBuffer sb = new StringBuffer("<img src=\"loadTmpImg.do?fileName=" + fileName
                                           + "\" width=\"" + WIDTH + "\" height=\"" + HEIGHT
                                           + "\" usemap=\"#chart" + fileName + "\">");
        sb.append(imageMap);
        return sb.toString();
    }

    /**
     * @return the extraAttributeClass
     */
    public String getExtraAttributeClass() {
        return extraAttributeClass;
    }

    /**
     * @param extraAttributeClass the extraAttributeClass to set
     */
    public void setExtraAttributeClass(String extraAttributeClass) {
        this.extraAttributeClass = extraAttributeClass;
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag, ObjectStore os)
        throws Exception {
        Collection<String> extraAttributes = new ArrayList<String>();
        Map<String, Collection<String>> returnMap = new HashMap<String, Collection<String>>();
        if (extraAttributeClass != null && extraAttributeClass.length() > 0) {
            try {
                Class<?> clazz = TypeUtil.instantiate(extraAttributeClass);
                Constructor<?> constr = clazz.getConstructor(new Class[]{});
                WidgetUtil widgetUtil = (WidgetUtil) constr.newInstance(new Object[] {});
                extraAttributes = widgetUtil.getExtraAttributes(os, imBag);
                if (getSelectedExtraAttribute() == null
                    || getSelectedExtraAttribute().length() == 0) {
                    setSelectedExtraAttribute(extraAttributes.iterator().next());
                }
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
        if (extraAttributes.size() > 0) {
            returnMap.put("Organism", extraAttributes);
        }
        return returnMap;
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
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public String getExternalLinkLabel() {
        return externalLinkLabel;
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalLinkLabel(String externalLinkLabel) {
        this.externalLinkLabel = externalLinkLabel;
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
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    public List<List<String>> getExportResults(String[]selected) throws Exception {
        // TODO this method
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String[]>> getFlattenedResults() {
        // TODO this method
        return null;
    }
}
