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

import java.util.Enumeration;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * A customised JTable for the display of model class references and collections.
 * Sets up the default renderers for its columns. Optionally, it can provide
 * table sorting (commented out, Java 1.6 only).
 */
public class ReferenceTable extends JTable
{
    private static final long serialVersionUID = -7432160746961157258L;

    /**
     * Initialise a new ReferenceTable.
     */
    public ReferenceTable() {
    }

    /**
     * Initialise a new ReferenceTable with the given model.
     * @param model The table model.
     */
    public ReferenceTable(ReferenceTableModel model) {
        super(model);
    }

    /**
     * Create the default table model when no model is provided.
     * @return A new instance of ReferenceTableModel.
     * 
     * @see ReferenceTableModel
     */
    @Override
    protected TableModel createDefaultDataModel() {
        return new ReferenceTableModel();
    }

    /**
     * Create the default cell renderers. This is an instance of
     * {@link ReferenceTableCellRenderer}, set on all columns.
     */
    @Override
    protected void createDefaultRenderers() {
        super.createDefaultRenderers();
        
        TableCellRenderer renderer = new ReferenceTableCellRenderer();
        Enumeration<TableColumn> tcIter = columnModel.getColumns();
        while (tcIter.hasMoreElements()) {
            tcIter.nextElement().setCellRenderer(renderer);
        }
    }

    /**
     * Initialise default table properties.
     * <p>Currently disabled, this override will set up column sorting.</p>
     */
    @Override
    protected void initializeLocalVars() {
        super.initializeLocalVars();
        
        /*
        TableRowSorter<AttributeTableModel> sorter =
            new TableRowSorter<AttributeTableModel>((AttributeTableModel)dataModel);
        setRowSorter(sorter);
        
        Comparator<Object> comparator = new NullComparator<Object>(Collator.getInstance(), false);
        
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys); 
    
        for (int i = 0; i < dataModel.getColumnCount(); i++) {
            sorter.setComparator(i, comparator);
        }
        */
    }
}
