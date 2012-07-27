package org.intermine.webservice.server.core;

import org.intermine.api.results.ResultCell;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

public final class TableCell implements ResultCell
{
    private final FastPathObject fpo;
    private final boolean isKeyField;
    private final Path column;

    public TableCell(FastPathObject fpo, Path view, boolean isKeyField) {
        this.fpo = fpo;
        this.column = view;
        this.isKeyField = isKeyField;
    }
    
    public TableCell(Path view) {
        this.fpo = null;
        this.column = view;
        this.isKeyField = false;
    }
    
    public boolean isKeyField() {
        return isKeyField;
    }
    
    public Object getField() {
        if (fpo == null) {
            return null;
        }
        try {
            return fpo.getFieldValue(column.getEndFieldDescriptor().getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error retrieving field value for " + column, e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error retrieving field value for " + column, e);
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
    
    public Path getPath() {
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
        return String.format("TableCell [ %s=%s ]", column, getField());
    }
}
