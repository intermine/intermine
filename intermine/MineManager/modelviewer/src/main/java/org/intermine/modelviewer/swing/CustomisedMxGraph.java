package org.intermine.modelviewer.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.modelviewer.model.ModelClass;

import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

/**
 * A version of <code>mxGraph</code> that is customised with different edge
 * styles for the different types of connections between classes in the model.
 * It also turns off editing capabilities and automatically uses a ModelClass's
 * name for printing.
 * 
 * @see mxGraph
 * @see <a href="http://jgraph.com/jgraph.html">The JGraphX documentation</a>
 */
public class CustomisedMxGraph extends mxGraph
{
    /**
     * Inheritance relationship edge style name.
     */
    public static final String INHERITANCE_EDGE_STYLE = "inheritance edge";
    
    /**
     * Reference relationship edge style name.
     */
    public static final String REFERENCE_EDGE_STYLE = "reference edge";
    
    /**
     * Self-reference relationship edge style name.
     */
    public static final String SELF_REFERENCE_EDGE_STYLE = "self reference edge";
    
    /**
     * Collection relationship edge style name.
     */
    public static final String COLLECTION_EDGE_STYLE = "collection edge";
    
    /**
     * Self-referenced collection relationship edge style name.
     */
    public static final String SELF_COLLECTION_EDGE_STYLE = "self collection edge";

    
    /**
     * Initialise as a default graph.
     */
    public CustomisedMxGraph() {
        super();
    }

    /**
     * Initialise with a given model and style sheet.
     * 
     * @param model The graph model.
     * @param stylesheet The style sheet.
     */
    public CustomisedMxGraph(mxIGraphModel model, mxStylesheet stylesheet) {
        super(model, stylesheet);
    }

    /**
     * Initialise with a given model.
     * @param model The graph model.
     */
    public CustomisedMxGraph(mxIGraphModel model) {
        super(model);
    }

    /**
     * Initialise with a given style sheet.
     * 
     * @param stylesheet The style sheet.
     */
    public CustomisedMxGraph(mxStylesheet stylesheet) {
        super(stylesheet);
    }

    
    /**
     * Overridden creation of a default style sheet for this component.
     * This method creates suitable edge styles for the different types
     * of relationship between classes and returns them in a new
     * <code>mxStylesheet</code> object.
     * 
     * @return A fully populated <code>mxStylesheet</code>.
     */
    @Override
    protected mxStylesheet createStylesheet() {
        mxStylesheet ss = new mxStylesheet();
        Map<String, Object> edgeStyle = new HashMap<String, Object>(ss.getDefaultEdgeStyle());
        edgeStyle.put(mxConstants.STYLE_DASHED, Boolean.TRUE);
        ss.putCellStyle(INHERITANCE_EDGE_STYLE, edgeStyle);
        
        edgeStyle = new HashMap<String, Object>(ss.getDefaultEdgeStyle());
        //edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ELBOW);
        //edgeStyle.put(mxConstants.STYLE_ELBOW, mxConstants.ELBOW_HORIZONTAL);
        ss.putCellStyle(REFERENCE_EDGE_STYLE, edgeStyle);
        
        edgeStyle = new HashMap<String, Object>(ss.getDefaultEdgeStyle());
        edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_LOOP);
        ss.putCellStyle(SELF_REFERENCE_EDGE_STYLE, edgeStyle);
        
        edgeStyle = new HashMap<String, Object>(ss.getDefaultEdgeStyle());
        edgeStyle.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_DIAMOND);
        //edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ENTITY_RELATION);
        ss.putCellStyle(COLLECTION_EDGE_STYLE, edgeStyle);

        edgeStyle = new HashMap<String, Object>(ss.getDefaultEdgeStyle());
        edgeStyle.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_DIAMOND);
        edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_LOOP);
        ss.putCellStyle(SELF_COLLECTION_EDGE_STYLE, edgeStyle);
        
        return ss;
    }

    /**
     * Returns a textual representation of the given cell. This override
     * checks to see if <code>cell</code> is a ModelClass, and if so it returns
     * its name. Otherwise, the implementation of the normal mxGraph is used.
     * 
     * @param cell The object in the graph.
     * 
     * @return The textual representation of <code>cell</code>.
     * 
     * @see mxGraph#convertValueToString(Object)
     */
    @Override
    public String convertValueToString(Object cell) {
        Object result = model.getValue(cell);

        if (result instanceof ModelClass) {
            return ((ModelClass) result).getName();
        }
        return super.convertValueToString(cell);
    }

    /**
     * Indicates cells cannot be disconnected.
     * @return <code>false</code>
     */
    @Override
    public boolean isCellsDisconnectable() {
        return false;
    }

    /**
     * Indicates cells cannot be edited.
     * @return <code>false</code>
     */
    @Override
    public boolean isCellsEditable() {
        return false;
    }

    /**
     * Indicates that any cell cannot be connected.
     * 
     * @param cell The cell object.
     * @return <code>false</code>
     */
    @Override
    public boolean isCellConnectable(Object cell) {
        return false;
    }
}
