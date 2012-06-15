package org.intermine.webservice.server.core;

import java.util.ArrayList;
import java.util.List;

import org.intermine.pathquery.Path;

public class SubTable
{
    private final List<List<Either<TableCell, SubTable>>> subrows;
    private final List<Path> columns;
    private final Path joinPath;

    public SubTable(
            Path outerJoinGroup,
            List<Path> columns,
            List<List<Either<TableCell, SubTable>>> rows)
    {
        this.joinPath = outerJoinGroup;
        this.columns = columns;
        this.subrows = new ArrayList<List<Either<TableCell, SubTable>>>(rows);
    }

    public List<List<Either<TableCell, SubTable>>> getRows() {
        return subrows;
    }

    public List<Path> getColumns() {
        return columns;
    }

    public Path getJoinPath() {
        return joinPath;
    }
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("SubTable [%d %s\n", subrows.size(), joinPath.getLastClassDescriptor().getUnqualifiedName()));
        for (List<Either<TableCell, SubTable>> row: subrows) {
            sb.append(String.valueOf(row));
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
