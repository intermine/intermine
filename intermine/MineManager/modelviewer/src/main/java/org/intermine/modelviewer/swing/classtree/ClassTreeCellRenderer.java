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

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 * TreeCellRenderer for the class tree.
 */
public class ClassTreeCellRenderer extends DefaultTreeCellRenderer
{
    private static final long serialVersionUID = 6798480154694548945L;

    /**
     * Gets the renderer for the given tree cell.
     * <p>This implementation simply fetches the class name for the model class
     * held in the tree node and uses the superclass implementation to deal with
     * that simple String.</p>
     * 
     * @param tree The JTree.
     * @param value The tree node to draw.
     * @param sel Whether the cell is selected or not.
     * @param expanded Whether the cell is expanded or not.
     * @param leaf Whether the cell is a leaf cell.
     * @param row The row index of the cell.
     * @param hasFocus Whether the cell has keyboard focus.
     * 
     * @return The tree cell renderer component, i.e. <code>this</code>, correctly
     * prepared.
     * 
     * @see TreeCellRenderer#getTreeCellRendererComponent
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        if (value instanceof ClassTreeNode) {
            value = ((ClassTreeNode) value).getModelClass().getName();
        }
        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }

}
