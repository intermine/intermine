package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;
import org.intermine.bio.web.model.CytoscapeNetworkNodeData;
import org.json.JSONObject;

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

    //============ for XGMML format ============
    /**
     * Convert a set of CytoscapeNetworkData to String in XGMML format.
     * Use StringBuffer instead of DOM or SAX.
     *
     * @param interactionNodeMap a map of CytoscapeNetworkNodeData objects
     * @param interactionEdgeMap a map of CytoscapeNetworkEdgeData objects
     * @return the network in XGMML format as a string or text
     */
    public String createGeneNetworkInXGMML(
            Map<String, CytoscapeNetworkNodeData> interactionNodeMap,
            Map<String, CytoscapeNetworkEdgeData> interactionEdgeMap) {

        StringBuffer sb = new StringBuffer();
        sb = addHeaderToGeneNetworkInXGMML(sb);
        sb = addNodesToGeneNetworkInXGMML(sb, interactionNodeMap);
        sb = addEdgesToGeneNetworkInXGMML(sb, interactionEdgeMap);
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
            .append("<graph label=\"network.xgmml\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
                    + " xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                    + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                    + " xmlns:cy=\"http://www.cytoscape.org\""
                    + " xmlns=\"http://www.cs.rpi.edu/XGMML\" directed=\"1\">")
            .append("<att name=\"documentVersion\" value=\"1.1\"/>")
            .append("<att name=\"networkMetadata\">")
            .append("<rdf:RDF>")
            .append("<rdf:Description rdf:about=\"http://www.cytoscape.org/\">")
            .append("<dc:type>Protein-Protein Interaction</dc:type>")
            .append("<dc:description>N/A</dc:description>")
            .append("<dc:identifier>N/A</dc:identifier>")
            .append("<dc:date>YYYY-MM-YYHH:MM:SS</dc:date>")
            .append("<dc:title>network.xgmml</dc:title>")
            .append("<dc:source>http://www.cytoscape.org/</dc:source>")
            .append("<dc:format>Cytoscape-XGMML</dc:format>")
            .append("</rdf:Description>")
            .append("</rdf:RDF>")
            .append("</att>")
            .append("<att type=\"string\" name=\"backgroundColor\" value=\"#ffffff\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_ZOOM\" value=\"1.0\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_CENTER_X\" value=\"0.0\"/>")
            .append("<att type=\"real\" name=\"GRAPH_VIEW_CENTER_Y\" value=\"0.0\"/>");

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
     * @param interactionNodeMap
     * @return sb
     */
    private StringBuffer addNodesToGeneNetworkInXGMML(StringBuffer sb,
            Map<String, CytoscapeNetworkNodeData> interactionNodeMap) {
        for (CytoscapeNetworkNodeData node : interactionNodeMap.values()) {
            if (node.getSourceLabel() == null || "".equals(node.getSourceLabel())) {
                sb.append("<node id=\"" + node.getSourceId() + "\" label=\""
                        + node.getSourceId() + "\">");
            } else {
                sb.append("<node id=\"" + node.getSourceId() + "\" label=\""
                        + node.getSourceLabel() + "\">");
            }
            sb.append("<att type=\"string\" name=\"shape\" value=\"ELLIPSE\"/>")
                .append("<graphics x=\"0\" y=\"0\" outline=\"#666666\" fill=\"#f5f5f5\" "
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
     * @param interactionEdgeMap
     * @return sb
     */
    private StringBuffer addEdgesToGeneNetworkInXGMML(StringBuffer sb,
            Map<String, CytoscapeNetworkEdgeData> interactionEdgeMap) {
        for (CytoscapeNetworkEdgeData edge : interactionEdgeMap.values()) {
            sb.append("<edge source=\"" + edge.getSourceId() + "\" directed=\"true\" "
                    + "target=\"" + edge.getTargetId() + "\" id=\"" + edge.getSourceId() + " ("
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
    /**
     * Convert a set of CytoscapeNetworkData to String in JSON format.
     *
     * @param interactionNodeMap a map of CytoscapeNetworkNodeData objects
     * @param interactionEdgeMap a map of CytoscapeNetworkEdgeData objects
     * @return the network in JSON string
     */
    @SuppressWarnings("unchecked")
    public String createGeneNetworkInJSON(
            Map<String, CytoscapeNetworkNodeData> interactionNodeMap,
            Map<String, CytoscapeNetworkEdgeData> interactionEdgeMap) {

        // Create Network Model
        Map<String, Object> networkModel = new HashMap<String, Object>();

        // Create dataSchema
        Map<String, Object> dataSchema =  new HashMap<String, Object>();
        List<Map<String, Object>> nodesSchema =  new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> edgesSchema =  new ArrayList<Map<String, Object>>();

        Map<String, Object> nodeAttrLabel =  new HashMap<String, Object>();
        nodeAttrLabel.put("name", "label");
        nodeAttrLabel.put("type", "string");

        nodesSchema.add(nodeAttrLabel);

        Map<String, Object> edgeAttrDirected =  new HashMap<String, Object>();
        Map<String, Object> edgeAttrInteraction =  new HashMap<String, Object>();

        edgeAttrDirected.put("name", "directed");
        edgeAttrDirected.put("type", "boolean");
        edgeAttrDirected.put("defValue", true);

        edgeAttrInteraction.put("name", "interaction");
        edgeAttrInteraction.put("type", "object");

        edgesSchema.addAll(Arrays.asList(edgeAttrDirected, edgeAttrInteraction));

        dataSchema.put("nodes", nodesSchema);
        dataSchema.put("edges", edgesSchema);
        networkModel.put("dataSchema", dataSchema);

        // Create data
        Map<String, Object> data =  new HashMap<String, Object>();
        List<Map<String, Object>> nodesData =  new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> edgesData =  new ArrayList<Map<String, Object>>();

        // Add nodes
        for (CytoscapeNetworkNodeData n : interactionNodeMap.values()) {
            Map<String, Object> node = new HashMap<String, Object>();
            if (n.getSourceLabel() == null || "".equals(n.getSourceLabel())) {
                node.put("id", n.getSourceId());
                node.put("label", n.getSourceId());
            } else {
                node.put("id", n.getSourceId());
                node.put("label", n.getSourceLabel());
            }
            nodesData.add(node);
        }

        // Add edges
        for (CytoscapeNetworkEdgeData e : interactionEdgeMap.values()) {
            Map<String, Object> edge = new HashMap<String, Object>();
            Map<String, Object> interaction = new HashMap<String, Object>();
            List<Map<String, Object>> dataSourceList =  new ArrayList<Map<String, Object>>();

            for (Entry<String, Set<String>> d : e.getDataSources().entrySet()) {
                Map<String, Object> dataSource = new HashMap<String, Object>();
                List<Map<String, Object>> interactionNameList =
                    new ArrayList<Map<String, Object>>();

                dataSource.put("name", d.getKey());

                for (String n : (Set<String>) d.getValue()) {
                    Map<String, Object> interactionName = new HashMap<String, Object>();

                    interactionName.put("value", n);
                    interactionNameList.add(interactionName);
                }

                dataSource.put("interactionName", interactionNameList);
                dataSourceList.add(dataSource);
            }

            interaction.put("interactionType", e.getInteractionType());
            interaction.put("dataSource", dataSourceList);

            edge.put("id", e.getSourceId() + " (" + e.getInteractionType()
                    + ") " + e.getTargetId());
            edge.put("source", e.getSourceId());
            edge.put("target", e.getTargetId());
            edge.put("label", e.getInteractionType());
            edge.put("interaction", interaction);

            edgesData.add(edge);
        }

        data.put("nodes", nodesData);
        data.put("edges", edgesData);
        networkModel.put("data", data);

        JSONObject jo = new JSONObject(networkModel);

        return jo.toString();
    }
}
