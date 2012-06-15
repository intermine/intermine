package org.intermine.webservice.server.core;

import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

public final class TableCell
{
    private final FastPathObject fpo;
    
    private final Path column;

    public TableCell(FastPathObject fpo, Path view) {
        this.fpo = fpo;
        this.column = view;
    }
    
    public TableCell(Path view) {
        this.fpo = null;
        this.column = view;
    }
    
    public Object getValue() {
        if (fpo == null) {
            return null;
        }
        try {
            return fpo.getFieldValue(column.getEndFieldDescriptor().getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error retrieving field value.", e);
        }
    };
    
    public FastPathObject getObject() {
        return fpo;
    }
    
    public String getType() {
        if (fpo == null) {
            return null;
        }
        String cls = DynamicUtil.getSimpleClassName(fpo.getClass());
        return TypeUtil.unqualifiedName(cls);
    }
    
    public Path getColumn() {
        return column;
    }
    
    public Integer getId() {
        if (fpo instanceof InterMineObject) {
            return ((InterMineObject) fpo).getId();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("TableCell [ %s=%s ]", column, getValue());
    }
}
