package org.intermine.modelviewer.swing.referencetable;

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
import org.intermine.modelviewer.model.ForeignKey;
import org.intermine.modelviewer.model.ModelClass;

/**
 * A Swing table model for the reference table.
 * Provides a model with six columns:
 * 
 * <ol>
 * <li>Reference name</li>
 * <li>Reference type (collection or reference)</li>
 * <li>Class name for the target class of this reference</li>
 * <li>Reverse reference name</li>
 * <li>Class name for the class the reference is defined on</li>
 * <li>The tag for the file the attribute was defined in</li>
 * </ol>
 */
public class ReferenceTableModel extends GenericTableModel<ReferenceInfo>
{
    private static final long serialVersionUID = 5696220330775747608L;
    
    /**
     * Model index for the reference name column.
     */
    public static final int NAME_COLUMN = 0;
    
    /**
     * Model index for the reference name column.
     */
    public static final int TYPE_COLUMN = 1;
    
    /**
     * Model index for the reference target class name column.
     */
    public static final int REFERENCED_TYPE_COLUMN = 2;

    /**
     * Model index for the reverse reference name column.
     */
    public static final int REVERSE_REFERENCE_COLUMN = 3;
    
    /**
     * Model index for the reference's source class column.
     */
    public static final int CLASS_COLUMN = 4;
    
    /**
     * Model index for the reference's origin tag column.
     */
    public static final int SOURCE_COLUMN = 5;

    /**
     * The text to print when the foreign key is a collection.
     * @serial
     */
    private String collectionText;
    
    /**
     * The text to print when the foreign key is a reference.
     * @serial
     */
    private String referenceText;
    
    /**
     * Column header strings.
     * @serial
     */
    private String[] headers;


    /**
     * Initialise a new, empty ReferenceTableModel.
     */
    public ReferenceTableModel() {
        headers = new String[] {
                Messages.getMessage("rtable.name"),
                Messages.getMessage("rtable.type"),
                Messages.getMessage("rtable.referencedtype"),
                Messages.getMessage("rtable.reversereference"),
                Messages.getMessage("rtable.class"),
                Messages.getMessage("rtable.source")
        };
        
        collectionText = Messages.getMessage("type.collection");
        referenceText = Messages.getMessage("type.reference");
    }
    
    /**
     * Get the number of columns.
     * @return 6
     */
    @Override
    public int getColumnCount() {
        return 6;
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
            case REFERENCED_TYPE_COLUMN:
            case REVERSE_REFERENCE_COLUMN:
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
            case REFERENCED_TYPE_COLUMN:
            case REVERSE_REFERENCE_COLUMN:
            case CLASS_COLUMN:
            case SOURCE_COLUMN:
                return headers[columnIndex];
        }
        throw new IndexOutOfBoundsException(columnIndex + " is out of range.");
    }

    /**
     * Check whether the given cell is editable. No cell in the reference table
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
     * Check whether the reference shown in the given row is a derived reference.
     * 
     * @param rowIndex The row index for the reference in question.
     * 
     * @return The derived flag for the given reference.
     * 
     * @throws IndexOutOfBoundsException if <code>rowIndex</code> is not a
     * valid row index.
     */
    public boolean isDerived(int rowIndex) {
        ReferenceInfo info = rows.get(rowIndex);
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReferenceInfo info = rows.get(rowIndex);
        switch (columnIndex) {
            case NAME_COLUMN:              return info.reference.getName();
            case TYPE_COLUMN:              return info.collection ? collectionText : referenceText;
            case REFERENCED_TYPE_COLUMN:   return info.reference.getReferencedType();
            case REVERSE_REFERENCE_COLUMN: return info.reference.getReverseReference();
            case CLASS_COLUMN:             return info.sourceClass.getName();
            case SOURCE_COLUMN:            return info.reference.getTag();
        }
        throw new IndexOutOfBoundsException(columnIndex + " is out of range.");
    }

    /**
     * Set the model class this table model will now display. Causes the current
     * model to be cleared and repopulated with the references and collections
     * of the given class.
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
     * topmost class is found, then fill the model with the references and collections
     * from each class.
     * 
     * @param modelClass The model class currently under examination.
     * @param derived Flag indicating that the foreign keys of <code>modelClass</code> are
     * derived, from the point of view of the original class.
     */
    private void initHierarchy(ModelClass modelClass, boolean derived) {
        if (modelClass.getSuperclass() != null) {
            initHierarchy(modelClass.getSuperclass(), true);
        }
        for (ForeignKey fk : modelClass.getReferences().values()) {
            rows.add(new ReferenceInfo(fk, modelClass, derived, false));
        }
        for (ForeignKey fk : modelClass.getCollections().values()) {
            rows.add(new ReferenceInfo(fk, modelClass, derived, true));
        }
    }
}
