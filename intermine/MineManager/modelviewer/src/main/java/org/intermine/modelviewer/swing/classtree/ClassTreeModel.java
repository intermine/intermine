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

import javax.swing.tree.DefaultTreeModel;

import org.intermine.modelviewer.model.ModelClass;

/**
 * Tree model implementation for the class tree.
 */
public class ClassTreeModel extends DefaultTreeModel
{
    private static final long serialVersionUID = -984927994395328727L;

    /**
     * Initialise this model with only an empty root node.
     */
    public ClassTreeModel() {
        super(new ClassTreeRootNode(Collections.<ModelClass>emptyList()));
    }
    
    /**
     * Initialise this model with the given model classes.
     * 
     * @param classes The collection of ModelClass objects to create the tree
     * around.
     */
    public ClassTreeModel(Collection<ModelClass> classes) {
        super(new ClassTreeRootNode(classes));
    }

}
