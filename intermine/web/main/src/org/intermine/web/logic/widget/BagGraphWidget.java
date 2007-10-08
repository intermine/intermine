package org.intermine.web.logic.widget;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.TypeUtil;

import java.awt.BasicStroke;
import java.awt.Font;

import java.lang.reflect.Constructor;

import javax.servlet.http.HttpSession;

import org.jfree.chart.ChartColor;
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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.servlet.ServletUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.ui.TextAnchor;
/**
 * @author Xavier Watkins
 *
 */
public class BagGraphWidget
{

    private String fileName = null;
    private String imageMap = null;
    private static final int WIDTH = 430;
    private static final int HEIGHT = 350;
    
    /**
     * Creates a BagGraphWidet object which handles
     * the creation of thhe JFreeChart for the given
     * webconfig
     * @param session the HttpSession
     * @param geneCategoryArray the geneCategoryArray as created by the DataSetLdr
     * @param bagName the bag name     
     * @param toolTipGen the ToolTipGenerator to use
     * @param urlGen the UrlGenerator to use
     * @param chart the chart
     * @param plot the plot
     * @param renderer the renderer
     */
    public BagGraphWidget(HttpSession session, Object[] geneCategoryArray, String bagName, 
                          String toolTipGen, String urlGen, JFreeChart chart, CategoryPlot plot, 
                          BarRenderer renderer) {
        super();
        try {

            // display values for each column
            CategoryItemLabelGenerator generator = new StandardCategoryItemLabelGenerator();
            renderer.setItemLabelsVisible(true);
            renderer.setItemLabelGenerator(generator);
           
            renderer.setPositiveItemLabelPosition(new ItemLabelPosition(
                                        ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
            
            // TODO put this in a config file
            // set colors for each data series
            ChartColor blue = new ChartColor(47, 114,  255);
            renderer.setSeriesPaint(0, blue); 
            
            ChartColor lightBlue = new ChartColor(159, 192,  255);
            renderer.setSeriesPaint(1, lightBlue); 
                        
            //renderer.setDrawBarOutline(false);
            renderer.setSeriesOutlineStroke(1, new BasicStroke(0.0F));
            
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
            // this may be already set individually for the different series
            if (urlGen != null) {
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
            }
/*            final ItemLabelPosition neg = new ItemLabelPosition(ItemLabelAnchor.INSIDE12, 
                                                                TextAnchor.CENTER, 
                                                                TextAnchor.CENTER, 
                                                                0.0D);
                                                                
                                                                
                                                                
            renderer.setNegativeItemLabelPosition(neg); */
            
            
            renderer.setNegativeItemLabelPositionFallback(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.BASELINE_LEFT)); 
            
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setUpperMargin(0.15);
            
            Font labelFont = new Font("SansSerif", Font.BOLD, 12);
            plot.getDomainAxis().setLabelFont(labelFont);
            rangeAxis.setLabelFont(labelFont);
            plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);
            plot.getDomainAxis()
                .setCategoryLabelPositions(
                                           CategoryLabelPositions
                                               .createUpRotationLabelPositions(Math.PI / 6.0));
        
            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
           
            // generate the image and imagemap
            fileName = ServletUtilities.saveChartAsPNG(chart, WIDTH, HEIGHT, info, session);
            imageMap = ImageMapUtilities.getImageMap("chart" + fileName, info);
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
                                           + "\" usemap=\"#chart" + fileName + "\">");
        sb.append(imageMap);
        return sb.toString();
    }
}