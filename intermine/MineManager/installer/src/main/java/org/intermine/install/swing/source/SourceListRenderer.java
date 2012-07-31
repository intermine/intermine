package org.intermine.install.swing.source;

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

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.intermine.modelviewer.project.Source;


/**
 * A renderer for the source list.
 */
public class SourceListRenderer extends DefaultListCellRenderer
{
    private static final long serialVersionUID = -7218549384020383696L;

    /**
     * Prepare this renderer for drawing the given source.
     * 
     * @param list The list component.
     * @param value The value (source) to display.
     * @param index The index of <code>value</code> in the list.
     * @param isSelected Whether the cell is selected.
     * @param cellHasFocus Whether the cell has focus.
     * 
     * @return <code>this</code>, suitable prepared.
     */
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (value instanceof Source) {
            value = ((Source) value).getName();
        }
        
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
}
