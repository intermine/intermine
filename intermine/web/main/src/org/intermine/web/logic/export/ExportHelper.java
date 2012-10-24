package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.ArrayList;
import java.util.List;

import org.intermine.api.results.Column;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.results.PagedTable;

/**
 * Helper methods for exporters.
 *
 * @author rns
 */
public final class ExportHelper
{
    private ExportHelper() {

    }

    /**
     * Return true if the specified class or a subclass is found in a column of the
     * paged table (or is the parent class of a field displayed in the table).
     * @param pt a table to export from
     * @param cls a class to look for in the results
     * @return true if the class is found
     */
    public static boolean canExport(PagedTable pt, Class<?> cls) {
        return  (getClassIndex(getColumnClasses(pt), cls) >= 0);
    }

    /**
     * @param clazzes classes
     * @param cls searched class
     * @return index of class that is assignable to given class
     */
    public static int getClassIndex(List<Class<?>> clazzes, Class<?> cls) {
        for (int i = 0; i < clazzes.size(); i++) {
            if (cls.isAssignableFrom(clazzes.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param clazzes classes
     * @param searched searched class
     * @return index of class that is assignable to given class
     */
    public static List<Integer> getClassIndexes(List<Class<?>> clazzes,
            Class<?> searched) {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = 0; i < clazzes.size(); i++) {
            if (searched.isAssignableFrom(clazzes.get(i))) {
                ret.add(i);
            }
        }
        return ret;
    }

    /**
     * @param pt paged table
     * @return classes of columns
     */
    public static List<Class<?>> getColumnClasses(PagedTable pt) {
        List<Column> columns = pt.getColumns();
        List<Class<?>> ret = new ArrayList<Class<?>>();

        for (int i = 0; i < columns.size(); i++) {
            ret.add(pt.getTypeForColumn(i));
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
    public static int getFirstColumnForClass(PagedTable pt, Class<?> cls) {
        return getClassIndex(getColumnClasses(pt), cls);
    }

    /**
     * Return a List containing the Path objects from the Columns of this PagedTable.
     * @param pt the paged table
     * @return the Paths
     */
    public static List<Path> getColumnPaths(PagedTable pt) {
        List<Path> paths = new ArrayList<Path>();
        for (Column col: pt.getColumns()) {
            paths.add(col.getPath());
        }
        return paths;
    }

}
