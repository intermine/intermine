package org.intermine.web.widget;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Font;
import java.lang.reflect.Constructor;

import javax.servlet.http.HttpSession;

import org.intermine.util.TypeUtil;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * @author Xavier Watkins
 *
 */
public class BagGraphWidget
{

    private String fileName;
    private String imageMap;
    private static final int WIDTH = 430;
    private static final int HEIGHT = 350;
    
    /**
     * Creates a BagGraphWidet object which handles
     * the creation of thhe JFreeChart for the given
     * webconfig
     * @param session the HttpSession
     * @param dataSet the CategoryDataset as created by the DataSetLdr
     * @param geneCategoryArray the geneCategoryArray as created by the DataSetLdr
     * @param bagName the ba name
     * @param title the graph title
     * @param domain the graph domain (x azis)
     * @param range the graph range (y axis)
     * @param toolTipGen the ToolTipGenerator to use
     * @param urlGen the UrlGenerator to use
     */
    public BagGraphWidget(HttpSession session, CategoryDataset dataSet, Object[] geneCategoryArray,
        String bagName, String title, String domain, String range, String toolTipGen, 
        String urlGen) {
        super();
        try {
            JFreeChart chart = ChartFactory.createStackedBarChart(title, // chart title
                                                                  domain, // domain axis label
                                                                  range, // range axis label
                                                                  dataSet, // data 
                                                                  PlotOrientation.VERTICAL, true, 
                                                                  true, // tooltips? 
                                                                  false // URLs? 
                );

            CategoryPlot plot = chart.getCategoryPlot();
            StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();

            // display values for each column
            CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator();
            renderer.setItemLabelsVisible(true);
            renderer.setItemLabelGenerator(generator);

            // gene names as toolips
            Class clazz1 = TypeUtil.instantiate(toolTipGen);
            Constructor toolTipConstructor = clazz1.getConstructor(new Class[]
                {
                    Object[].class
                });
            CategoryToolTipGenerator categoryToolTipGen = (CategoryToolTipGenerator) 
                toolTipConstructor.newInstance(new Object[]
                    {
                        geneCategoryArray
                    });
            renderer.setToolTipGenerator(categoryToolTipGen);

            // url to display genes
            Class clazz2 = TypeUtil.instantiate(urlGen);
            Constructor urlGenConstructor = clazz2.getConstructor(new Class[]
                {
                    String.class
                });
            CategoryURLGenerator categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
                .newInstance(new Object[]
                    {
                        bagName
                    });
            renderer.setItemURLGenerator(categoryUrlGen);

            Font labelFont = new Font("SansSerif", Font.BOLD, 12);
            plot.getDomainAxis().setLabelFont(labelFont);
            plot.getRangeAxis().setLabelFont(labelFont);
            plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);
            plot.getDomainAxis()
                .setCategoryLabelPositions(
                                           CategoryLabelPositions
                                               .createUpRotationLabelPositions(Math.PI / 6.0));

            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());

           
            // generate the image and imagemap
            fileName = ServletUtilities.saveChartAsPNG(chart, WIDTH, HEIGHT, info, session);
            imageMap = ImageMapUtilities.getImageMap("chart", info);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /**
     * Get the HTML that will display the graph and imagemap
     * @return the HTML as a String
     */
    public String getHTML() {
        StringBuffer sb = new StringBuffer("<img src=\"loadTmpImg.do?fileName=" + fileName
                                           + "\" width=\"" + WIDTH + "\" height=\"" + HEIGHT
                                           + "\" usemap=\"#chart\">");
        sb.append(imageMap);
        return sb.toString();
    }
}