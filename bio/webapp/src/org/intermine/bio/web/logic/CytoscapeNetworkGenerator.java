package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;

/**
 * This class has the logics to generate different interaction data formats such as SIF, XGMML,
 * JSON etc.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeNetworkGenerator
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkGenerator.class);

    //============ for SIF format ============
    /**
     * Convert a set of CytoscapeNetworkData to String in SIF format.
     *
     * @param interactionSet a set of CytoscapeNetworkData objects
     * @return the network in SIF format as a string or text
     */
    public String createNetworkInSIF(Set<CytoscapeNetworkEdgeData> interactionSet) {

        StringBuffer theNetwork = new StringBuffer();

        // Build a line of network data, the data will be used in javascript in jsp,
        // js can only take "\n" in a string instead of real new line, so use "\\n" here
        for (CytoscapeNetworkEdgeData interactionString : interactionSet) {
            theNetwork.append(interactionString.generateInteractionString());
            theNetwork.append("\\n");
        }

        return theNetwork.toString();
    }

    //============ for XGMML format ============
    /**
     * Convert a set of CytoscapeNetworkData to String in XGMML format.
     * Use StringBuffer instead of DOM or SAX.
     *
     * @param interactionEdgeSet a set of CytoscapeNetworkEdgeData objects
     * @param interactionNodeSet a set of CytoscapeNetworkNodeData objects
     * @return the network in XGMML format as a string or text
     */
    public String createGeneNetworkInXGMML(
            Set<CytoscapeNetworkNodeData> interactionNodeSet,
            Set<CytoscapeNetworkEdgeData> interactionEdgeSet) {

        StringBuffer sb = new StringBuffer();
        sb = addHeaderToGeneNetworkInXGMML(sb);
        sb = addNodesToGeneNetworkInXGMML(sb, interactionNodeSet);
        sb = addEdgesToGeneNetworkInXGMML(sb, interactionEdgeSet);
        sb = addTailToNetworkInXGMML(sb);

        return sb.toString();
    }

    /**
     * Generate the header of XGMML.
     *
     * @param sb
     * @return
     */
    private StringBuffer addHeaderToGeneNetworkInXGMML(StringBuffer sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            .append("<graph label=\"gene_interactions.xgmml\" xmlns:dc=\"http://purl.org/dc/"
                    + "elements/1.1/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:"
                    + "rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:cy=\"http:"
                    + "//www.cytoscape.org\" xmlns=\"http://www.cs.rpi.edu/XGMML\" >")
            .append("<att name=\"documentVersion\" value=\"0.1\"/>")
            .append("<att name=\"networkMetadata\">")
            .append("<rdf:RDF>")
            .append("<rdf:Description rdf:about=\"http://www.cytoscape.org/\">")
            .append("<dc:type>Gene-Gene Interaction</dc:type>")
            .append("<dc:description>N/A</dc:description>")
            .append("<dc:identifier>N/A</dc:identifier>")
            .append("<dc:date>YYYY-MM-YYHH:MM:SS</dc:date>")
            .append("<dc:title>gene_interactions.xgmml</dc:title>")
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
     * @param sb
     * @return
     */
    private StringBuffer addTailToNetworkInXGMML(StringBuffer sb) {
        sb.append("</graph>");

        return sb;

    }

    /**
     * Generate the network nodes of XGMML.
     *
     * @param sb
     * @param interactionNodeSet
     * @return sb
     */
    private StringBuffer addNodesToGeneNetworkInXGMML(StringBuffer sb,
            Set<CytoscapeNetworkNodeData> interactionNodeSet) {
        for (CytoscapeNetworkNodeData node : interactionNodeSet) {
            if (node.getSourceLabel() == null || "".equals(node.getSourceLabel())) {
                sb.append("<node id=\"" + node.getSoureceId() + "\" label=\""
                        + node.getSoureceId() + "\">");
            } else {
                sb.append("<node id=\"" + node.getSoureceId() + "\" label=\""
                        + node.getSourceLabel() + "\">");
            }
            sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                .append("<graphics x=\"\" y=\"\" w=\"24\" outline=\"#666666\" fill=\"#f5f5f5\" "
                    + "cy:nodeTransparency=\"0.8\" width=\"1\" cy:nodeLabelFont=\"Arial-0-11\" "
                    + "h=\"24\" labelanchor=\"c\" type=\"ELLIPSE\"/>")
                .append("</node>");
        }

        return sb;
    }

    /**
     * Generate the network edges of XGMML.
     *
     * @param sb
     * @param interactionEdgeSet
     * @return sb
     */
    private StringBuffer addEdgesToGeneNetworkInXGMML(StringBuffer sb,
            Set<CytoscapeNetworkEdgeData> interactionEdgeSet) {
        for (CytoscapeNetworkEdgeData edge : interactionEdgeSet) {
            sb.append("<edge source=\"" + edge.getSoureceId() + "\" directed=\"true\" "
                    + "target=\"" + edge.getTargetId() + "\" id=\"" + edge.getSoureceId() + " ("
                    + edge.getInteractionType() + ") " + edge.getTargetId() + "\" "
                    + "label=\"" + edge.getInteractionType() + "\">");

            sb.append("<att type=\"string\" name=\"interactionType\" "
                    + "value=\"" + edge.getInteractionType() + "\"/>")
                .append("<att type=\"list\" name=\"dataSources\">");
            for (String dataSourceName : edge.getDataSources().keySet()) {
                sb.append("<att type=\"string\" name=\"dataSource\" value=\""
                        + dataSourceName + "\"/>");
            }
            sb.append("</att>");

            for (Entry<String, Set<String>> dataSource : edge.getDataSources().entrySet()) {
                sb.append("<att type=\"list\" label=\"dataSource\" name=\"" + dataSource.getKey()
                        + "\" value=\"" + dataSource.getKey() + "\">");
                for (String interactionName : (Set<String>) dataSource.getValue()) {
                    sb.append("<att type=\"string\" label=\"interactionShortName\" name=\""
                            + interactionName + "\" value=\"" + interactionName + "\"/>");
                }
                sb.append("</att>");
            }

            sb.append("<att name=\"weight\" type=\"real\" value=\"1.0\"/>")
                .append("<att type=\"string\" name=\"targetArrowShape\" value=\"delta\"/>");
            if ("both".equals(edge.getDirection())) {
                if ("physical".equals(edge.getInteractionType())) {
                    sb.append("<att type=\"string\" name=\"sourceArrowShape\" value=\"delta\"/>")
                        .append("<graphics cy:sourceArrowColor=\"#ff0000\" cy:sourceArrow=\"3\" "
                             + "fill=\"#ff0000\" width=\"2\" cy:targetArrow=\"3\" "
                             + "cy:targetArrowColor=\"#ff0000\"/>");
                } else if ("genetic".equals(edge.getInteractionType())) {
                    sb.append("<att type=\"string\" name=\"sourceArrowShape\" value=\"delta\"/>")
                        .append("<graphics cy:sourceArrowColor=\"#6666ff\" cy:sourceArrow=\"3\" "
                            + "fill=\"#6666ff\" width=\"2\" cy:targetArrow=\"3\" "
                            + "cy:targetArrowColor=\"#6666ff\"/>");
                }
            } else if ("one".equals(edge.getDirection())) {
                if ("physical".equals(edge.getInteractionType())) {
                    sb.append("<att type=\"string\" name=\"sourceArrowShape\" value=\"none\"/>")
                        .append("<graphics cy:sourceArrowColor=\"#ff0000\" "
                            + "cy:sourceArrow=\"0\" fill=\"#ff0000\" width=\"2\" "
                            + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#ff0000\"/>");
                } else if ("genetic".equals(edge.getInteractionType())) {
                    sb.append("<att type=\"string\" name=\"sourceArrowShape\" value=\"none\"/>")
                        .append("<graphics cy:sourceArrowColor=\"#6666ff\" "
                            + "cy:sourceArrow=\"0\" fill=\"#6666ff\" width=\"2\" "
                            + "cy:targetArrow=\"3\" cy:targetArrowColor=\"#6666ff\"/>");
                }
            }
            sb.append("</edge>");
        }

        return sb;

    }

    // Separate modMine specific logics from bio, created RegulatoryNetworkDataFormatUtil in modMine

    //============ for JSON format ============
    // TODO to be implemented
}
