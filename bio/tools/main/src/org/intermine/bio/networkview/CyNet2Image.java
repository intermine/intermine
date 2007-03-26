package org.intermine.bio.networkview;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import giny.util.SpringEmbeddedLayouter;
import giny.view.EdgeView;
import giny.view.NodeView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.giny.PhoebeNetworkView;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.CalculatorIO;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.PassThroughMapping;


/**
 * utility class to create a image from the network
 * @author Florian Reisinger
 *
 */
public class CyNet2Image
{

    private static final Logger LOG = Logger.getLogger(CyNet2Image.class);

    /**
     * Convert a network to an image.
     * use default values for size and style
     * @param net network to convert to image
     * @return image representing the network
     * @see #convertNetwork2Image(CyNetwork, String, String)
     */
    public static Image convertNetwork2Image(CyNetwork net) {
        return convertNetwork2Image(net, 0, 0, null, null);
    }

    /**
     * Convert a network to an image.
     * creates a image with a size determined by the layout algorithm
     * @param net Input network to convert to an image
     * @param vizpropsFile String specifying the location of the vizmap properties file
     * @param style String specifying the name of the style
     * @return The resulting image
     * @see #convertNetwork2Image(CyNetwork, int, int, String, String)
     */
    public static Image convertNetwork2Image(CyNetwork net, String vizpropsFile,
            String style) {
        return convertNetwork2Image(net, 0, 0, vizpropsFile, style);
    }

    /**
     * Convert a network to an image.
     * @param net Input network to convert to an image
     * @param width Width that the resulting image should be
     * @param height Height that the resulting image should be
     * @param vizpropsFile String specifying the location of the vizmap properties file
     * @param style String specifying the name of the style
     * @return The resulting image
     */
    public static Image convertNetwork2Image(CyNetwork net, int width, int height,
            String vizpropsFile, String style) {
        PhoebeNetworkView pnv;
        Image image;
        Color bgColor = Color.WHITE; // set default background color
        boolean doDefault = false;

        // create view of the network
        pnv = new PhoebeNetworkView(net, "tmpview");
        int nodeCount = pnv.getNodeViewCount();
        
        // do not create network image if there is just a single node that 
        // has no interactions
        // instead create default image
        if (nodeCount == 1) {
            NodeView nv = (NodeView) pnv.getNodeViewsList().get(0);
            String s = nv.getNode().getIdentifier();
            return CyNet2Image.createSingleNodeImage(s);
        }

        // try to apply the specified userstyle from the specified vizmap properties
        userStyle: if (vizpropsFile != null && style != null) {
            // get the visual settings form a properties file
            // TODO: check if file exists and is valid (has needed properities)
            File vizmaps = new File(vizpropsFile);
            Properties vizprops = new Properties();
            try {
                vizprops.load(new FileInputStream(vizmaps));
            } catch (IOException e) {
                LOG.error("Error loading vizmap properties");
                doDefault = true;
                break userStyle;

            }
            // create a new CalculatorCatalog
            CalculatorCatalog cCatalog = new CalculatorCatalog();
            cCatalog.addMapping("Discrete Mapper", DiscreteMapping.class);
            cCatalog.addMapping("Continuous Mapper", ContinuousMapping.class);
            cCatalog.addMapping("Passthrough Mapper", PassThroughMapping.class);
            CalculatorIO.loadCalculators(vizprops, cCatalog);
            // create manager that will do the job
            VisualMappingManager vmm = new VisualMappingManager(pnv, cCatalog);
            // get the desired visual style
            CalculatorCatalog tcc = vmm.getCalculatorCatalog();
            VisualStyle myVS = tcc.getVisualStyle(style);
            if (myVS == null) {
                // use log4j
                doDefault = true;
                break userStyle;
            }

            // get the backgroundcolor of that style
            // -> used later when transfering into image
            bgColor = myVS.getGlobalAppearanceCalculator().getDefaultBackgroundColor();
            // set & apply style
            vmm.setVisualStyle(myVS);
            vmm.applyAppearances();
        } else { // vizpropsFile or style not specified
            doDefault = true;
        } // end userStyle

        if (doDefault) {
            // start over again and apply default style
            pnv = new PhoebeNetworkView(net, "defaultview");
            applyDefaultStyle(pnv);
        }

        // apply cytoscape's embedded layout
        doEmbeddedLayout(pnv);

        // check if a predefined size is specified
        if (height == 0 && width == 0) {
            double w = pnv.getCanvas().getLayer().getFullBounds().width;
            double h = pnv.getCanvas().getLayer().getFullBounds().height;
            width = (new Double(w)).intValue();
            height = (new Double(h)).intValue();
        }

        // create a image from canvas

        image = pnv.getCanvas().getLayer().toImage(width, height, bgColor);
        return (image);
    }

    /**
     * this method will create a default image showing some text
     * @return a default image
     */
    private static Image createSingleNodeImage(String protein) {
        String line1 = "No interactions";
        String line2 = "with other proteins";
        String line3 = "found for protein";
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, 200, 200);
        g.setColor(Color.BLACK);
        g.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 20));
        g.drawString(line1, 20, 40);
        g.drawString(line2, 1, 80);
        g.drawString(line3, 10, 120);
        g.drawString(protein, 60, 165);
        return img;
    }
    
    /**
     * Applies a default style 
     * @param pnv networkview to apply the default style to
     */
    private static void applyDefaultStyle(PhoebeNetworkView pnv) {
        for (Iterator in = pnv.getNodeViewsIterator(); in.hasNext();) {
            NodeView nv = (NodeView) in.next();
            String label = nv.getNode().getIdentifier();
            nv.getLabel().setText(label);
            nv.setShape(NodeView.ROUNDED_RECTANGLE);
            nv.setUnselectedPaint(Color.YELLOW);
            nv.setBorderPaint(Color.black);
            Font x = new Font(nv.getLabel().getFont().getFontName(), 
                    Font.BOLD, 26);
            nv.setBorderWidth(0.5f);
            nv.setHeight(30);
            nv.setWidth(120);
            nv.getLabel().setFont(x);
        }
        for (Iterator ie = pnv.getEdgeViewsIterator(); ie.hasNext();) {
            EdgeView ev = (EdgeView) ie.next();
            ev.setUnselectedPaint(Color.GREEN);
            ev.setLineType(2);
            ev.setSourceEdgeEnd(EdgeView.NO_END);
            ev.setTargetEdgeEnd(EdgeView.NO_END);
            ev.setStroke(new BasicStroke(5f));
            ev.setStrokeWidth(2f);
        }

    }

    /**
     * saves a image to a file in png format
     * @param img image to save
     * @param filepath location to save the file to
     * 
     * @throws IOException IOException
     */
    public static void image2File(Image img, String filepath) throws IOException {
        BufferedImage buffImg = new BufferedImage(img.getWidth(null),
                img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImg.createGraphics();
        g.drawImage(img, null, null);

        // Save image to file as PNG
        ImageIO.write(buffImg, "png", new File(filepath));

    }

    /**
     * write image to a OutputStream
     * @param img image to write
     * @param out OutputStream to write to
     * @return true if writing was successfull false otherwise
     */
    public static boolean imageOut(Image img, OutputStream out) {
        boolean success;
        BufferedImage buffImg = new BufferedImage(img.getWidth(null),
                img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffImg.createGraphics();
        g.drawImage(img, null, null);

        // Save image to OutputStream as PNG
        try {
            ImageIO.write(buffImg, "png", out);
            success = true;
        } catch (IOException e) {
            success = false;
            // TODO print to logger
        }
        return success;
    }

    /**
     * Apply default cytoscape embedded layout
     * @param pnv PhoebeNetworkView of network
     */
    private static void doEmbeddedLayout(PhoebeNetworkView pnv) {
        // TODO: almost same code as in SampleFlyPlugin!!! watch changes
        // TODO: maybe change PhoebeNetworkView to CyNetworkView
        SpringEmbeddedLayouter lay;
        // need the same size as in CyNet2Image, so the result of the layouter 
        // will look the same as the image created with the CyNet2Image utility
        int size = 350;
        pnv.getCanvas().setSize(size, size);
        // some calculations needed to spread the nodes over the canvas
        int nodeCount = pnv.getNodeViewCount();
        int nodesPerRow = (new Double(Math.sqrt(nodeCount))).intValue();

        int hSpace = size / (nodesPerRow);
        int wSpace = size / (nodesPerRow);

        int wPos = wSpace;
        int hPos = hSpace;

        for (Iterator in = pnv.getNodeViewsIterator(); in.hasNext();) {
            NodeView nv = (NodeView) in.next();

            //spread node positions before layout so that they don't  
            //all layout in a line (-> they don't fall into a local minimum 
            //for the SpringEmbedder)
            //If the SpringEmbedder implementation changes, this code  
            //may need to be removed
            nv.setXPosition(wPos);
            nv.setYPosition(hPos);

            wPos += wSpace;
            if (wPos > size) {
                wPos = wSpace;
                hPos += hSpace;
            }

        }

        // layout the graph using embedded cytoscape layout
        lay = new SpringEmbeddedLayouter(pnv);
        lay.doLayout();
    }

    /**
     * main method used for testing
     * @param args arguments to the program
     */
    public static void main(String[] args) {
        int iSize = 350;
        int iSmallSize = 200;
//        String vizFile = "/home/flo/.cytoscape/vizmap.props";
//        String netFile = "/home/flo/FlyMine/Cytoscape/cytoscape_testdata2.sif";
//        
//        try {
//            CyNetwork net = Cytoscape.createNetworkFromFile(netFile);
////            Image i1 = CyNet2Image.convertNetwork2Image(net, 500, 500, vizFile, "test1");
//            Image i1 = CyNet2Image.convertNetwork2Image(net, iSize, iSize, null, null);
//            image2File(i1, "/tmp/test1img.png");
//            Image i1sf = i1.getScaledInstance(iSmallSize, iSmallSize, Image.SCALE_FAST);
//            image2File(i1sf, "/tmp/test1sfimg.png");
//            Image i1ss = i1.getScaledInstance(iSmallSize, iSmallSize, Image.SCALE_SMOOTH);
//            image2File(i1ss, "/tmp/test1ssimg.png");
//            Image i2 = CyNet2Image.convertNetwork2Image(net);
//            image2File(i2, "/tmp/test2img.png");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        FlyNetwork fn = new FlyNetwork();
        FlyNode n1 = new FlyNode("Q9WV19");
//        FlyNode n2 = new FlyNode("node2");
//        FlyNode n3 = new FlyNode("node3");
//        FlyNode n4 = new FlyNode("node4");
//        FlyNode n5 = new FlyNode("node5");
//        FlyEdge e1 = new FlyEdge(n1, n2);
        FlyEdge e2 = new FlyEdge(n1, n1);
//        FlyEdge e3 = new FlyEdge(n2, n3);
//        FlyEdge e4 = new FlyEdge(n3, n4);
//        FlyEdge e5 = new FlyEdge(n3, n5);
        fn.addNode(n1);
//        fn.addNode(n2);
//        fn.addNode(n3);
//        fn.addNode(n4);
//        fn.addNode(n5);
//        fn.addEdge(e1);
        fn.addEdge(e2);
//        fn.addEdge(e3);
//        fn.addEdge(e4);
//        fn.addEdge(e5);
        
        Collection nc = FlyNetworkIntegrator.convertNodesFly2Cy(fn.getNodes());
        Collection ec = FlyNetworkIntegrator.convertEdgesFly2Cy(fn.getEdges());
        CyNetwork net = Cytoscape.createNetwork(nc, ec, "testnet");

        try {
            Image i3 = CyNet2Image.convertNetwork2Image(net, iSize, iSize, null, null);
            image2File(i3, "/tmp/test3img.png");
            Image i3sf = i3.getScaledInstance(iSmallSize, iSmallSize, Image.SCALE_FAST);
            image2File(i3sf, "/tmp/test3sfimg.png");
            Image i3ss = i3.getScaledInstance(iSmallSize, iSmallSize, Image.SCALE_SMOOTH);
            image2File(i3ss, "/tmp/test3ssimg.png");
            Image i4 = CyNet2Image.convertNetwork2Image(net);
            image2File(i4, "/tmp/test4img.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
