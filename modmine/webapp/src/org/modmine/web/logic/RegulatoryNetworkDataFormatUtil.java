package org.modmine.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;

/**
 * This class has the logics to generate different regulatory network formats such as XGMML,
 * JSON etc.
 *
 * @author Fengyuan Hu
 *
 */
public final class RegulatoryNetworkDataFormatUtil
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(RegulatoryNetworkDataFormatUtil.class);

    // TODO Can be paras passing from action
    private static final String FLY_NETWORK_NAME = "fly_regulatory_network";
    private static final String WORM_NETWORK_NAME = "worm_regulatory_network";
    private static final String NETWORK_TYPE = "miRNA-TF regulatory network";

    private static String flyNetworkXGMML = "";
    private static String wormNetworkXGMML = "";

    private RegulatoryNetworkDataFormatUtil() {

    }

    //============ for XGMML format ============
    //=== Fly regulatory network ===
    /**
     * Convert a set of CytoscapeNetworkData to String in XGMML format.
     * Use StringBuffer instead of DOM or SAX.
     *
     * @param interactionEdgeSet a set of CytoscapeNetworkEdgeData objects
     * @param interactionNodeSet a set of CytoscapeNetworkNodeData objects
     * @return the network in XGMML format as a string or text
     */
    public static String createFlyRegulatoryNetworkInXGMML(
            Set<CytoscapeNetworkNodeData> interactionNodeSet,
            Set<CytoscapeNetworkEdgeData> interactionEdgeSet) {

        if ("".equals(flyNetworkXGMML)) {
            StringBuffer sb = new StringBuffer();

            sb = addHeaderToFlyRegulatoryNetworkInXGMML(sb);
            sb = addNodesToFlyRegulatoryNetworkInXGMML(sb, interactionNodeSet);
            sb = addEdgesToFlyRegulatoryNetworkInXGMML(sb, interactionEdgeSet);
            sb = addTailToFlyRegulatoryNetworkInXGMML(sb);

            return sb.toString();
        } else {
            return flyNetworkXGMML;
        }
    }

    /**
     * Generate the header of XGMML.
     *
     * @param sb a StringBuffer
     * @return sb
     */
    private static StringBuffer addHeaderToFlyRegulatoryNetworkInXGMML(StringBuffer sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            .append("<graph label=\"gene_interactions.xgmml\" xmlns:dc=\"http://purl.org/dc/"
                    + "elements/1.1/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:"
                    + "rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:cy=\"http:"
                    + "//www.cytoscape.org\" xmlns=\"http://www.cs.rpi.edu/XGMML\" >")
            .append("<att name=\"documentVersion\" value=\"0.1\"/>")
            .append("<att name=\"networkMetadata\">")
            .append("<rdf:RDF>")
            .append("<rdf:Description rdf:about=\"http://www.cytoscape.org/\">")
            .append("<dc:type>" + NETWORK_TYPE + "</dc:type>")
            .append("<dc:description>N/A</dc:description>")
            .append("<dc:identifier>N/A</dc:identifier>")
            .append("<dc:date>YYYY-MM-YYHH:MM:SS</dc:date>")
            .append("<dc:title>" + FLY_NETWORK_NAME + ".xgmml</dc:title>")
            .append("<dc:source>http://www.cytoscape.org/</dc:source>")
            .append("<dc:format>Cytoscape-XGMML</dc:format>")
            .append("</rdf:Description>")
            .append("</rdf:RDF>")
            .append("</attr>")
            .append("<att type=\"string\" name=\"backgroundColor\" value=\"#ffffff\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_ZOOM\" value=\"1\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_CENTER_X\" value=\"0\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_CENTER_Y\" value=\"0\"/>");

        return sb;
    }

    /**
     * Generate the tail of XGMML.
     *
     * @param sb a StringBuffer
     * @return sb
     */
    private static StringBuffer addTailToFlyRegulatoryNetworkInXGMML(StringBuffer sb) {
        sb.append("</graph>");

        return sb;
    }

    /**
     * Generate the network nodes of XGMML. (the code is really ugly...)
     *
     * @param sb a StringBuffer
     * @param interactionNodeSet
     * @return sb
     */
    private static StringBuffer addNodesToFlyRegulatoryNetworkInXGMML(StringBuffer sb,
            Set<CytoscapeNetworkNodeData> interactionNodeSet) {

        // network properties
        int initHeight = 800; // Y
//        int initWidth = 1500; // X

        double nodeXLeft = 0;
        double nodeXLeftInit = 0;
        double nodeYLeft = 200;
        double stepYLeft = initHeight / 21; // 21 nodes
        int stepXCounterLeft = 1;

        double nodeXRight = 1000;
        double nodeXRightInit = 1000;
        double nodeYRight = 200;
        double stepYRight = initHeight / 31; // 31 nodes
        int stepXCounterRight = 1;

        int nodeXLevel1 = 300; // centre 500, step 50, 9 nodes
        int nodeXLevel2 = 200; // centre 500, step 50, 13 nodes
        int nodeXLevel3 = -25; // center 500, step 50, 22 nodes
        int nodeXLevel4 = 225; // center 500, step 50, 12 nodes
        int nodeXLevel5 = 0;   // center 500, step 50, 20 nodes

        for (CytoscapeNetworkNodeData node : interactionNodeSet) {
            if (node.getSourceLabel() == null || "".equals(node.getSourceLabel())) {
                sb.append("<node id=\"" + node.getSourceId() + "\" label=\""
                        + node.getSourceId() + "\">");
            } else {
                sb.append("<node id=\"" + node.getSourceId() + "\" label=\""
                        + node.getSourceLabel() + "\">");
            }

            // To distinguish miRNA and TF by position
            // miRNA - position: left|right
            // TF - level: 1-5
            String position = node.getPosition();

            if ("left".equals(position) || "right".equals(position)) {
                if ("left".equals(position)) {
                    sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Double.toString(nodeXLeft)
                                    + "\" y=\""
                                    + Double.toString(nodeYLeft)
                                    + "\" w=\"32\" outline=\"#666666\" fill=\"#ff0000\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");

                    nodeYLeft = nodeYLeft + stepYLeft;
                    nodeXLeft = nodeXLeftInit - 100 * Math.sin((Math.PI / 21) * stepXCounterLeft);
                    stepXCounterLeft++;

                } else if ("right".equals(position)) {
                    sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Double.toString(nodeXRight)
                                    + "\" y=\""
                                    + Double.toString(nodeYRight)
                                    + "\" w=\"32\" outline=\"#666666\" fill=\"#ff0000\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");

                    nodeYRight = nodeYRight + stepYRight;
                    nodeXRight = nodeXRightInit + 100
                            * Math.sin((Math.PI / 31) * stepXCounterRight);
                    stepXCounterRight++;
                }
            } else {
                switch (Integer.parseInt(position)) {
                    case 1:
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Integer.toString(nodeXLevel1)
                                    + "\" y=\"250\" w=\"32\" outline=\"#666666\" fill=\"#33cc33\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");
                        nodeXLevel1 = nodeXLevel1 + 50; // step 50
                        break;
                    case 2:
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Integer.toString(nodeXLevel2)
                                    + "\" y=\"400\" w=\"32\" outline=\"#666666\" fill=\"#33cc33\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");
                        nodeXLevel2 = nodeXLevel2 + 50;
                        break;
                    case 3:
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Integer.toString(nodeXLevel3)
                                    + "\" y=\"550\" w=\"32\" outline=\"#666666\" fill=\"#33cc33\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");
                        nodeXLevel3 = nodeXLevel3 + 50;
                        break;
                    case 4:
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Integer.toString(nodeXLevel4)
                                    + "\" y=\"700\" w=\"32\" outline=\"#666666\" fill=\"#33cc33\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");
                        nodeXLevel4 = nodeXLevel4 + 50;
                        break;
                    case 5:
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                    + Integer.toString(nodeXLevel5)
                                    + "\" y=\"850\" w=\"32\" outline=\"#666666\" fill=\"#33cc33\" "
                                    + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                    + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                    + "type=\"ELLIPSE\"/>");
                        nodeXLevel5 = nodeXLevel5 + 50;
                        break;
                    default:
                        throw new RuntimeException("Internal error: level can not exceed 5...");
                }

            }

            sb.append("</node>");
        }

        return sb;
    }

    /**
     * Generate the network edges of XGMML.
     *
     * @param sb a StringBuffer
     * @param interactionEdgeSet
     * @return sb
     */
    private static StringBuffer addEdgesToFlyRegulatoryNetworkInXGMML(StringBuffer sb,
            Set<CytoscapeNetworkEdgeData> interactionEdgeSet) {
        for (CytoscapeNetworkEdgeData edge : interactionEdgeSet) {
            sb.append("<edge source=\"" + edge.getSourceId() + "\" directed=\"true\" "
                    + "target=\"" + edge.getTargetId() + "\" id=\"" + edge.getSourceId() + " ("
                    + edge.getInteractionType() + ") " + edge.getTargetId() + "\" "
                    + "label=\"" + edge.getInteractionType() + "\">");

            sb.append("<att type=\"string\" name=\"interactionType\" "
                    + "value=\"" + edge.getInteractionType() + "\"/>");

            if ("both".equals(edge.getDirection())) {
                if (edge.getInteractionType().startsWith("miRNA")) {
                    sb.append("<graphics cy:sourceArrowColor=\"#000000\" cy:sourceArrow=\"3\" "
                            + "fill=\"#ff0000\" width=\"1\" cy:targetArrow=\"3\" "
                            + "cy:targetArrowColor=\"#000000\" cy:edgeLineType=\"SOLID\"/>");
                } else if (edge.getInteractionType().startsWith("TF")) {
                    sb.append("<graphics cy:sourceArrowColor=\"#000000\" cy:sourceArrow=\"3\" "
                            + "fill=\"#33cc33\" width=\"1\" cy:targetArrow=\"3\" "
                            + "cy:targetArrowColor=\"#000000\" cy:edgeLineType=\"SOLID\"/>");
                }
            } else if ("one".equals(edge.getDirection())) {
                if (edge.getInteractionType().startsWith("miRNA")) {
                    sb.append("<graphics cy:sourceArrowColor=\"#000000\" "
                            + "cy:sourceArrow=\"0\" fill=\"#ff0000\" width=\"1\" "
                            + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#000000\" "
                            + "cy:edgeLineType=\"SOLID\"/>");
                } else if (edge.getInteractionType().startsWith("TF")) {
                    sb.append("<graphics cy:sourceArrowColor=\"#000000\" "
                            + "cy:sourceArrow=\"0\" fill=\"#33cc33\" width=\"1\" "
                            + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#000000\" "
                            + "cy:edgeLineType=\"SOLID\"/>");
                }
            }
            sb.append("</edge>");
        }

        return sb;
    }

    //=== Worm regulatory network ===
    /**
     * Convert a set of CytoscapeNetworkData to String in XGMML format.
     * Use StringBuffer instead of DOM or SAX.
     *
     * @param interactionEdgeSet a set of CytoscapeNetworkEdgeData objects
     * @param interactionNodeSet a set of CytoscapeNetworkNodeData objects
     * @return the network in XGMML format as a string or text
     */
    public static String createWormRegulatoryNetworkInXGMML(
            Set<CytoscapeNetworkNodeData> interactionNodeSet,
            Set<CytoscapeNetworkEdgeData> interactionEdgeSet) {

        if ("".equals(wormNetworkXGMML)) {
            StringBuffer sb = new StringBuffer();

            sb = addHeaderToWormRegulatoryNetworkInXGMML(sb);
            sb = addNodesToWormRegulatoryNetworkInXGMML(sb, interactionNodeSet);
            sb = addEdgesToWormRegulatoryNetworkInXGMML(sb, interactionEdgeSet);
            sb = addTailToWormRegulatoryNetworkInXGMML(sb);

            return sb.toString();
        } else {
            return wormNetworkXGMML;
        }
    }

    private static StringBuffer addHeaderToWormRegulatoryNetworkInXGMML(
            StringBuffer sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            .append("<graph label=\"gene_interactions.xgmml\" xmlns:dc=\"http://purl.org/dc/"
                    + "elements/1.1/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:"
                    + "rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:cy=\"http:"
                    + "//www.cytoscape.org\" xmlns=\"http://www.cs.rpi.edu/XGMML\" >")
            .append("<att name=\"documentVersion\" value=\"0.1\"/>")
            .append("<att name=\"networkMetadata\">")
            .append("<rdf:RDF>")
            .append("<rdf:Description rdf:about=\"http://www.cytoscape.org/\">")
            .append("<dc:type>" + NETWORK_TYPE + "</dc:type>")
            .append("<dc:description>N/A</dc:description>")
            .append("<dc:identifier>N/A</dc:identifier>")
            .append("<dc:date>YYYY-MM-YYHH:MM:SS</dc:date>")
            .append("<dc:title>" + WORM_NETWORK_NAME + ".xgmml</dc:title>")
            .append("<dc:source>http://www.cytoscape.org/</dc:source>")
            .append("<dc:format>Cytoscape-XGMML</dc:format>")
            .append("</rdf:Description>")
            .append("</rdf:RDF>")
            .append("</attr>")
            .append("<att type=\"string\" name=\"backgroundColor\" value=\"#ffffff\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_ZOOM\" value=\"1\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_CENTER_X\" value=\"0\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_CENTER_Y\" value=\"0\"/>");

        return sb;
    }

    private static StringBuffer addNodesToWormRegulatoryNetworkInXGMML(
            StringBuffer sb, Set<CytoscapeNetworkNodeData> interactionNodeSet) {

        int nodeXLeftTop = -330;
        int nodeXLeftMid = -135;
        int nodeXLeftBot = -155;
        int nodeXMidTop = 250;
        int nodeXMidMid = 353;
        int nodeXMidBot = 270;
        int nodeXRightTop = 845;
        int nodeXRightMid = 700;
        int nodeXRightBot = 835;

        for (CytoscapeNetworkNodeData node : interactionNodeSet) {
            if (node.getSourceLabel().startsWith("mir")) { // miRNA node
                sb.append("<node id=\"" + node.getSourceId() + "\" label=\""
                        + node.getSourceLabel().replace("mir-", "miR") + "\">");

                if ("left".equals(node.getExtraInfo().get("hposition"))) {
                    if ("top".equals(node.getExtraInfo().get("vposition"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXLeftTop)
                                + "\" y=\"-250\" w=\"24\" fill=\"#ff00ff\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"0\" "
                                + "cy:nodeLabelFont=\"Arial-0-8\" h=\"24\" labelanchor=\"c\" "
                                + "type=\"ELLIPSE\"/>");
                        nodeXLeftTop = nodeXLeftTop + 30;
                    } else if ("middle".equals(node.getExtraInfo().get("vposition"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXLeftMid)
                                + "\" y=\"-50\" w=\"24\" fill=\"#ff00ff\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"0\" "
                                + "cy:nodeLabelFont=\"Arial-0-8\" h=\"24\" labelanchor=\"c\" "
                                + "type=\"ELLIPSE\"/>");
                        nodeXLeftMid = nodeXLeftMid + 40;
                    } else if ("bottom".equals(node.getExtraInfo().get("vposition"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXLeftBot)
                                + "\" y=\"150\" w=\"24\" fill=\"#ff00ff\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"0\" "
                                + "cy:nodeLabelFont=\"Arial-0-8\" h=\"24\" labelanchor=\"c\" "
                                + "type=\"ELLIPSE\"/>");
                        nodeXLeftBot = nodeXLeftBot + 50;
                    }
                } else if ("right".equals(node.getExtraInfo().get("hposition"))) {
                    if ("top".equals(node.getExtraInfo().get("vposition"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXRightTop)
                                + "\" y=\"-20\" w=\"24\" fill=\"#ff00ff\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"0\" "
                                + "cy:nodeLabelFont=\"Arial-0-8\" h=\"24\" labelanchor=\"c\" "
                                + "type=\"ELLIPSE\"/>");
                        nodeXRightTop = nodeXRightTop + 40;
                    } else if ("middle".equals(node.getExtraInfo().get("vposition"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXRightMid)
                                + "\" y=\"180\" w=\"24\" fill=\"#ff00ff\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"0\" "
                                + "cy:nodeLabelFont=\"Arial-0-8\" h=\"24\" labelanchor=\"c\" "
                                + "type=\"ELLIPSE\"/>");
                        nodeXRightMid = nodeXRightMid + 30;
                    } else if ("bottom".equals(node.getExtraInfo().get("vposition"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXRightBot)
                                + "\" y=\"380\" w=\"24\" fill=\"#ff00ff\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"0\" "
                                + "cy:nodeLabelFont=\"Arial-0-8\" h=\"24\" labelanchor=\"c\" "
                                + "type=\"ELLIPSE\"/>");
                        nodeXRightBot = nodeXRightBot + 50;
                    }
                }
            } else { // TF node
                sb.append("<node id=\"" + node.getSourceId() + "\" label=\""
                        + node.getSourceLabel().split("-")[0].toUpperCase()
                        + node.getSourceLabel().split("-")[1] + "\">");

                if ("top".equals(node.getExtraInfo().get("vposition"))) {
                    if ("yellow".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidTop)
                                + "\" y=\"-125\" w=\"32\" outline=\"#666666\" fill=\"yellow\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    } else if ("blu".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidTop)
                                + "\" y=\"-125\" w=\"32\" outline=\"#666666\" fill=\"#4169e1\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    } else if ("red".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidTop)
                                + "\" y=\"-125\" w=\"32\" outline=\"#666666\" fill=\"red\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    }
                    nodeXMidTop = nodeXMidTop + 50;
                } else if ("middle".equals(node.getExtraInfo().get("vposition"))) {
                    if ("yellow".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidMid)
                                + "\" y=\"75\" w=\"32\" outline=\"#666666\" fill=\"yellow\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    } else if ("blu".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidMid)
                                + "\" y=\"75\" w=\"32\" outline=\"#666666\" fill=\"#4169e1\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    } else if ("red".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidMid)
                                + "\" y=\"75\" w=\"32\" outline=\"#666666\" fill=\"red\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    }
                    nodeXMidMid = nodeXMidMid + 65;
                } else if ("bottom".equals(node.getExtraInfo().get("vposition"))) {
                    if ("yellow".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidBot)
                                + "\" y=\"275\" w=\"32\" outline=\"#666666\" fill=\"yellow\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    } else if ("blu".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidBot)
                                + "\" y=\"275\" w=\"32\" outline=\"#666666\" fill=\"#4169e1\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    } else if ("red".equals(node.getExtraInfo().get("TF_type"))) {
                        sb.append("<att type=\"string\" name=\"shape\" value=\"TRIANGLE\"/>")
                            .append("<graphics x=\""
                                + Integer.toString(nodeXMidBot)
                                + "\" y=\"275\" w=\"32\" outline=\"#666666\" fill=\"red\" "
                                + "cy:nodeTransparency=\"0.8\" width=\"1\" "
                                + "cy:nodeLabelFont=\"Arial-0-11\" h=\"32\" labelanchor=\"c\" "
                                + "type=\"TRIANGLE\"/>");
                    }
                    nodeXMidBot = nodeXMidBot + 90;
                }
            }
            sb.append("</node>");
        }

        return sb;
    }

    private static StringBuffer addEdgesToWormRegulatoryNetworkInXGMML(
            StringBuffer sb, Set<CytoscapeNetworkEdgeData> interactionEdgeSet) {
        for (CytoscapeNetworkEdgeData edge : interactionEdgeSet) {
            sb.append("<edge source=\"" + edge.getSourceId() + "\" directed=\"true\" "
                    + "target=\"" + edge.getTargetId() + "\" label=\""
                    + edge.getInteractionType() + "\">");

            sb.append("<att type=\"string\" name=\"interactionType\" "
                    + "value=\"" + edge.getInteractionType() + "\"/>");

            if ("miRNA-TF".equals(edge.getInteractionType())) {
                sb.append("<graphics cy:sourceArrowColor=\"#000000\" "
                        + "cy:sourceArrow=\"0\" fill=\"#ff0000\" width=\"1\" "
                        + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#000000\" "
                        + "cy:edgeLineType=\"SOLID\"/>");
            } else if ("TF-miRNA".equals(edge.getInteractionType())) {
                sb.append("<graphics cy:sourceArrowColor=\"#000000\" "
                        + "cy:sourceArrow=\"0\" fill=\"#00ff7f\" width=\"1\" "
                        + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#000000\" "
                        + "cy:edgeLineType=\"SOLID\"/>");
            } else if ("TF-TF".equals(edge.getInteractionType())) {
                sb.append("<graphics cy:sourceArrowColor=\"#000000\" "
                        + "cy:sourceArrow=\"0\" fill=\"#000000\" width=\"3\" "
                        + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#000000\" "
                        + "cy:edgeLineType=\"SOLID\"/>");
            }

            sb.append("</edge>");
        }

        return sb;
    }

    private static StringBuffer addTailToWormRegulatoryNetworkInXGMML(
            StringBuffer sb) {
        sb.append("</graph>");
        return sb;
    }

    //============ for JSON format ============
    // TODO must implemente
}
