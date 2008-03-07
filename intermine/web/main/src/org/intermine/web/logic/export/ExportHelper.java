package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.ArrayList;
import java.util.List;

import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;

/**
 * Helper methods for exporters.
 *
 * @author rns
 */
public class ExportHelper
{

    /**
     * Return true if the specified class or a subclass is found in a column of the
     * paged table (or is the parent class of a field displayed in the table).
     * @param pt a table to export from
     * @param cls a class to look for in the results
     * @return true if the class is found
     */
    public static boolean canExport(PagedTable pt, Class cls) {
        return  (getFirstColumnForClass(getColumnClasses(pt), cls) >= 0);
    }

    public static int getFirstColumnForClass(List<Class> clazzes, Class cls) {        
        for (int i = 0; i < clazzes.size(); i++) {
            if (cls.isAssignableFrom(clazzes.get(i))) {
                return i;
            }
        }
        return -1;
    }
    
    public static List<Class> getColumnClasses(PagedTable pt) {
        List columns = pt.getColumns();
        List<Class> ret = new ArrayList<Class>();
        
        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);
            if (column.isVisible()) {
                ret.add(pt.getTypeForColumn(i));
            }
        }
        return ret;
    }

    
    /**
     * Find the first column index for the specified class or a subclass in the
     * page table (may be a parent class of a field being displayed).
     * @param pt a table to export from
     * @param cls a class to look for in the results
     * @return the first column index for the class
     */
    public static int getFirstColumnForClass(PagedTable pt, Class cls) {
        return getFirstColumnForClass(getColumnClasses(pt), cls);
    }
}
