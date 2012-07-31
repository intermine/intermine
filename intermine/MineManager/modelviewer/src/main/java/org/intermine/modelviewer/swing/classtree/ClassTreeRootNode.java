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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.intermine.modelviewer.model.ModelClass;

/**
 * A root node implementation for the class tree.
 */
public class ClassTreeRootNode extends DefaultMutableTreeNode
{
    private static final long serialVersionUID = -7927903727709087880L;

    /**
     * The child nodes of this node in the tree, indexed by class name.
     * @serial
     */
    private Map<String, ClassTreeNode> nodes;
    
    /**
     * Initialise this root node with the given model classes.
     * 
     * @param classes The collection of ModelClass objects to create the tree
     * around.
     */
    public ClassTreeRootNode(Collection<ModelClass> classes) {
        nodes = new HashMap<String, ClassTreeNode>();
        
        for (ModelClass mc : classes) {
            if (!nodes.containsKey(mc.getName())) {
                createTreeNode(mc);
            }
        }
        
        for (ClassTreeNode node : nodes.values()) {
            node.sort();
        }
        sort();
    }
    
    /**
     * Sort the children immediately under this node by class name.
     * 
     * @see ModelClassTreeComparator
     */
    @SuppressWarnings("unchecked")
    private void sort() {
        if (children != null) {
            Collections.sort(children, ModelClassTreeComparator.INSTANCE);
        }
    }
    
    /**
     * Create a tree node for the given class. Steps through each class in
     * the given class's inheritance hierarchy and ensures the parent class
     * has a node in this tree (recursively). It then adds a new node for
     * the given class.
     * 
     * @param mc The class to create a node for.
     * 
     * @return The node created for the given class.
     */
    private ClassTreeNode createTreeNode(ModelClass mc) {
        ModelClass superClass = mc.getSuperclass();
        DefaultMutableTreeNode parent = this;
        if (superClass != null) {
            parent = nodes.get(superClass.getName());
            if (parent == null) {
                parent = createTreeNode(superClass);
            }
        }
        
        ClassTreeNode node = new ClassTreeNode(mc);
        parent.add(node);
        nodes.put(mc.getName(), node);
        
        return node;
    }

    /**
     * Get the root of the tree.
     * @return The tree's root node, i.e. <code>this</code>.
     */
    @Override
    public TreeNode getRoot() {
        return this;
    }

    /**
     * Check whether this node is the root node.
     * @return <code>true</code>.
     */
    @Override
    public boolean isRoot() {
        return true;
    }
}
