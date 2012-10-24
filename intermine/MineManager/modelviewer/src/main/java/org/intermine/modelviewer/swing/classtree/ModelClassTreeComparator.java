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

import java.text.Collator;
import java.util.Comparator;

/**
 * Comparator to sort <code>ClassTreeNode</code>s by the name of the
 * model classes they wrap.
 */
class ModelClassTreeComparator implements Comparator<ClassTreeNode>
{
    /**
     * Shared-use instance of ModelClassTreeComparator.
     */
    public static final Comparator<ClassTreeNode> INSTANCE = new ModelClassTreeComparator();
    
    /**
     * String comparator for the class names using the default collator.
     *
     * @see Collator
     */
    private Comparator<Object> stringCompare = Collator.getInstance();

    /**
     * Compare the two ClassTreeNode objects for ordering by model class name.
     * 
     * @param o1 The first ClassTreeNode.
     * @param o2 The second ClassTreeNode.
     * 
     * @return A negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     */
    @Override
    public int compare(ClassTreeNode o1, ClassTreeNode o2) {
        String c1 = o1.getModelClass().getName();
        String c2 = o2.getModelClass().getName();
        return stringCompare.compare(c1, c2);
    }
}
