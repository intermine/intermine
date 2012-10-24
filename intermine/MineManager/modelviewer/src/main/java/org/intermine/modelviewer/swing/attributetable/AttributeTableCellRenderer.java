package org.intermine.modelviewer.swing.attributetable;

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

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A cell renderer for the AttributeTable.
 */
public class AttributeTableCellRenderer extends DefaultTableCellRenderer
{
    private static final long serialVersionUID = 9183263190800739627L;

    /**
     * Returns the default table cell renderer.
     * <p>Sets up the correct background colour for plotting each row
     * based on its <code>derived</code> flag.</p>
     *
     * @param table The <code>JTable</code>.
     * @param value The value to assign to the cell at <code>[row, column]</code>.
     * @param isSelected <code>true</code> if cell is selected.
     * @param hasFocus <code>true</code> if cell has focus.
     * @param row The row of the cell to render.
     * @param column The column of the cell to render.
     * 
     * @return The default table cell renderer, namely <code>this</code>,
     * properly set up.
     * 
     * @see DefaultTableCellRenderer#getTableCellRendererComponent
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        
        AttributeTableModel model = (AttributeTableModel) table.getModel();
        int modelRow = table.convertRowIndexToModel(row);
        boolean derived = model.isDerived(modelRow);
        
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBackground(derived ? table.getBackground().darker() : table.getBackground());
        return this;
    }

}
