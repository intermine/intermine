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

import org.intermine.common.swing.Messages;
import org.intermine.common.swing.table.GenericTableModel;
import org.intermine.modelviewer.model.Attribute;
import org.intermine.modelviewer.model.ModelClass;

/**
 * A Swing table model for the attribute table.
 * Provides a model with four columns:
 * 
 * <ol>
 * <li>Attribute name</li>
 * <li>Attribute type (Java class)</li>
 * <li>Class name for the class the attribute is defined on</li>
 * <li>The tag for the file the attribute was defined in</li>
 * </ol>
 */
public class AttributeTableModel extends GenericTableModel<AttributeInfo>
{
    private static final long serialVersionUID = 5696220330775747608L;

    /**
     * Model index for the attribute name column.
     */
    public static final int NAME_COLUMN = 0;

    /**
     * Model index for the attribute type column.
     */
    public static final int TYPE_COLUMN = 1;

    /**
     * Model index for the attribute's source class column.
     */
    public static final int CLASS_COLUMN = 2;

    /**
     * Model index for the attribute's origin tag column.
     */
    public static final int SOURCE_COLUMN = 3;

    /**
     * Column header strings.
     * @serial
     */
    private String[] headers;

    
    /**
     * Initialise a new, empty AttributeTableModel.
     */
    public AttributeTableModel() {
        headers = new String[] {
                Messages.getMessage("atable.name"),
                Messages.getMessage("atable.type"),
                Messages.getMessage("atable.class"),
                Messages.getMessage("atable.source")
        };
    }
    
    /**
     * Get the number of columns.
     * @return 4
     */
    @Override
    public int getColumnCount() {
        return 4;
    }

    /**
     * Get the type of the given column. All columns are strings.
     * 
     * @param columnIndex The index of the column.
     * @return {@link String String.class}
     * 
     * @throws IndexOutOfBoundsException if <code>columnIndex</code> is not
     * a valid column.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case NAME_COLUMN:
            case TYPE_COLUMN:
            case CLASS_COLUMN:
            case SOURCE_COLUMN:
                return String.class;
        }
        throw new IndexOutOfBoundsException(columnIndex + " is out of range.");
    }

    /**
     * Get the name of the given column.
     * 
     * @param columnIndex The index of the column.
     * @return The relevant column name.
     * 
     * @throws IndexOutOfBoundsException if <code>columnIndex</code> is not
     * a valid column.
     */
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case NAME_COLUMN:
            case TYPE_COLUMN:
            case CLASS_COLUMN:
            case SOURCE_COLUMN:
                return headers[columnIndex];
        }
        throw new IndexOutOfBoundsException(columnIndex + " is out of range.");
    }

    /**
     * Check whether the given cell is editable. No cell in the attribute table
     * is editable.
     * 
     * @param rowIndex The row index.
     * @param columnIndex The column index.
     * 
     * @return <code>false</code>, always.
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Check whether the attribute shown in the given row is a derived attribute.
     * 
     * @param rowIndex The row index for the attribute in question.
     * 
     * @return The derived flag for the given attribute.
     * 
     * @throws IndexOutOfBoundsException if <code>rowIndex</code> is not a
     * valid row index.
     */
    public boolean isDerived(int rowIndex) {
        AttributeInfo info = rows.get(rowIndex);
        return info.derived;
    }
    
    /**
     * Get the value at the given cell.
     * 
     * @param rowIndex The row index.
     * @param columnIndex The column index.
     * 
     * @return The value in the given cell.
     * 
     * @throws IndexOutOfBoundsException if <code>rowIndex</code> is not a
     * valid row index or <code>columnIndex</code> is not a valid column
     * index.
     */
    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        AttributeInfo info = rows.get(rowIndex);
        switch (columnIndex) {
            case NAME_COLUMN:   return info.attribute.getName();
            case TYPE_COLUMN:   return info.attribute.getType();
            case CLASS_COLUMN:  return info.sourceClass.getName();
            case SOURCE_COLUMN: return info.attribute.getTag();
        }
        throw new IllegalArgumentException(columnIndex + " is out of range.");
    }

    /**
     * Set the model class this table model will now display. Causes the current
     * model to be cleared and repopulated with the attributes of the given class.
     * 
     * @param mc The ModelClass to display.
     */
    public void setModelClass(ModelClass mc) {
        rows.clear();
        initHierarchy(mc, false);
        fireTableDataChanged();
    }
    
    /**
     * Recursively populate by iterating up through the class hierarchy until the
     * topmost class is found, then fill the model with the attributes from each
     * class.
     * 
     * @param modelClass The model class currently under examination.
     * @param derived Flag indicating that the attributes of <code>modelClass</code> are
     * derived, from the point of view of the original class.
     */
    private void initHierarchy(ModelClass modelClass, boolean derived) {
        if (modelClass.getSuperclass() != null) {
            initHierarchy(modelClass.getSuperclass(), true);
        }
        for (Attribute a : modelClass.getAttributes().values()) {
            rows.add(new AttributeInfo(a, modelClass, derived));
        }
    }
}
