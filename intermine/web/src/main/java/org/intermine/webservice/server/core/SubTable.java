package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.api.results.ResultCell;
import org.intermine.pathquery.Path;

/**
 * A class that represents a table nested within a cell in another table.
 * @author Alex Kalderimis
 *
 */
public class SubTable
{
    private final List<List<Either<ResultCell, SubTable>>> subrows;
    private final List<Path> columns;
    private final Path joinPath;

    /**
     * Construct a sub table.
     * @param outerJoinGroup The outer join group this table is located at.
     * @param columns The columns in this sub table.
     * @param rows The data within the table.
     */
    public SubTable(
            Path outerJoinGroup,
            List<Path> columns,
            List<List<Either<ResultCell, SubTable>>> rows) {
        this.joinPath = outerJoinGroup;
        this.columns = columns;
        this.subrows = new ArrayList<List<Either<ResultCell, SubTable>>>(rows);
    }

    /**
     * @return The rows that make up this sub table.
     */
    public List<List<Either<ResultCell, SubTable>>> getRows() {
        return subrows;
    }

    /** @return the columns of this sub-table **/
    public List<Path> getColumns() {
        return columns;
    }

    /** @return the path below which all the columns are nested. **/
    public Path getJoinPath() {
        return joinPath;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("SubTable [%d %s\n",
                subrows.size(), joinPath.getLastClassDescriptor().getUnqualifiedName()));
        for (List<Either<ResultCell, SubTable>> row: subrows) {
            sb.append(String.valueOf(row));
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
