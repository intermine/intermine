package org.intermine.modelviewer.swing.classtree;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;

import org.intermine.modelviewer.model.ModelClass;

/**
 * A branch/leaf node implementation for the class tree.
 */
public class ClassTreeNode extends DefaultMutableTreeNode
{
    private static final long serialVersionUID = -2329445513655143182L;

    /**
     * Initialise this node around the given model class object.
     * @param mc The model class object.
     */
    public ClassTreeNode(ModelClass mc) {
        super(mc);
    }

    /**
     * Get the model class object wrapped by this node.
     * @return The ModelClass object.
     */
    public ModelClass getModelClass() {
        return (ModelClass) userObject;
    }
    
    /**
     * Check whether this node is the root node.
     * @return <code>false</code>.
     */
    @Override
    public boolean isRoot() {
        return false;
    }
    
    /**
     * Sort the children immediately under this node by class name.
     * 
     * @see ModelClassTreeComparator
     */
    @SuppressWarnings("unchecked")
    void sort() {
        if (children != null) {
            Collections.sort(children, ModelClassTreeComparator.INSTANCE);
        }
    }
}
