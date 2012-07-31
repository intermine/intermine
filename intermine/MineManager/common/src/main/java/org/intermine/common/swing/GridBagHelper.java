package org.intermine.common.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Simple helper class to create more appropriately initialised
 * <code>GridBagConstraints</code> objects.
 */
public class GridBagHelper
{
    /**
     * Create default GridBagConstraints, suitable to start adding
     * components filling the width of the container with 4 pixel
     * insets.
     *
     * <P>Attributes are set as follows:
     * <UL>
     * <LI><code>anchor</code> = <code>CENTER</code></LI>
     * <LI><code>fill</code> = <code>HORIZONTAL</code></LI>
     * <LI><code>gridx</code> = 0</LI>
     * <LI><code>gridy</code> = 0</LI>
     * <LI><code>gridwidth</code> = 1</LI>
     * <LI><code>gridheight</code> = 1</LI>
     * <LI><code>insets</code> = 4 pixels on each edge</LI>
     * <LI><code>ipadx</code> = 0</LI>
     * <LI><code>ipady</code> = 0</LI>
     * <LI><code>weightx</code> = 0.0</LI>
     * <LI><code>weighty</code> = 0.0</LI>
     * </UL>
     *
     * @return The GridBagConstraints object.
     *
     * @see GridBagConstraints
     */
    public static GridBagConstraints defaultConstraints() {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = 0;
        cons.gridy = 0;
        cons.gridwidth = 1;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.insets = new Insets(4, 4, 4, 4);
        cons.weightx = 0;
        cons.weighty = 0;
        return cons;
    }
    
    /**
     * Set a <code>GridBagLayout</code> on the given container and return
     * a GridBagConstraints object as initialised by {@link #defaultConstraints()}.
     * 
     * @param c The container to set the layout on.
     * 
     * @return The GridBagConstraints object.
     */
    public static GridBagConstraints setup(Container c) {
        c.setLayout(new GridBagLayout());
        return defaultConstraints();
    }
}
