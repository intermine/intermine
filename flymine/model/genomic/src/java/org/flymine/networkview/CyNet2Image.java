package org.flymine.networkview;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.BasicStroke;
import java.awt.Color;
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

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.flymine.networkview.network.FlyEdge;
import org.flymine.networkview.network.FlyNetwork;
import org.flymine.networkview.network.FlyNode;

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
import giny.util.SpringEmbeddedLayouter;
import giny.view.EdgeView;
import giny.view.NodeView;

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
     * saves a image to a file
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
        int size = 1000;
        pnv.getCanvas().setSize(size, size);

        // some calculations needed to spread the nodes over the canvas
        int nodeCount = pnv.getNodeViewCount();
        int nodesPerRow = (new Double(Math.sqrt(nodeCount))).intValue() + 1;

        int hSpace = size / (nodesPerRow + 1);
        int wSpace = size / (nodesPerRow + 1);

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
        //String vizFile = "/home/flo/.cytoscape/vizmap.props";
        //String netFile = "/home/flo/FlyMine/Cytoscape/cytoscape_testdata2.sif";
        //
        //try {
        //    CyNetwork net = Cytoscape.createNetworkFromFile(netFile);
        //    Image i1 = CyNet2Image.convertNetwork2Image(net, 500, 500, vizFile, "test1");
        //    Image i2 = CyNet2Image.convertNetwork2Image(net);
        //    image2File(i1, "/tmp/test1.png");
        //    image2File(i2, "/tmp/test2.png");
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}

        FlyNetwork fn = new FlyNetwork();
        FlyNode n1 = new FlyNode("node1");
        FlyNode n2 = new FlyNode("node2");
        FlyNode n3 = new FlyNode("node3");
        FlyNode n4 = new FlyNode("node4");
        FlyEdge e1 = new FlyEdge(n1, n2);
        FlyEdge e2 = new FlyEdge(n2, n2);
        FlyEdge e3 = new FlyEdge(n2, n3);
        FlyEdge e4 = new FlyEdge(n3, n4);
        fn.addNode(n1);
        fn.addNode(n2);
        fn.addNode(n3);
        fn.addNode(n4);
        fn.addEdge(e1);
        fn.addEdge(e2);
        fn.addEdge(e3);
        fn.addEdge(e4);

        Collection nc = FlyNetworkIntegrator.convertNodesFly2Cy(fn.getNodes());
        Collection ec = FlyNetworkIntegrator.convertEdgesFly2Cy(fn.getEdges());
        CyNetwork net = Cytoscape.createNetwork(nc, ec, "testnet");

        Image i = CyNet2Image.convertNetwork2Image(net);
        try {
            image2File(i, "/tmp/test.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
