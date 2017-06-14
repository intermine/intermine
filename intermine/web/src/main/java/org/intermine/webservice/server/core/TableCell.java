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

import org.intermine.api.results.ResultCell;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.metadata.TypeUtil;

/**
 * A representation of a table cell.
 * @author Alex Kalderimis
 *
 */
public final class TableCell implements ResultCell
{
    private final FastPathObject fpo;
    private final boolean isKeyField;
    private final Path column;

    /**
     * Construct a table cell.
     * @param fpo The object backing this cell.
     * @param view The path this cell is a projection of.
     * @param isKeyField Whether this cell is a key field.
     */
    public TableCell(FastPathObject fpo, Path view, boolean isKeyField) {
        this.fpo = fpo;
        this.column = view;
        this.isKeyField = isKeyField;
    }

    /**
     * Construct a table cell for a null value.
     * @param view The path this cell is a projection of.
     */
    public TableCell(Path view) {
        this.fpo = null;
        this.column = view;
        this.isKeyField = false;
    }

    @Override
    public boolean isKeyField() {
        return isKeyField;
    }

    @Override
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

    @Override
    public FastPathObject getObject() {
        return fpo;
    }

    @Override
    public String getType() {
        if (fpo == null) {
            return null;
        }
        String cls = DynamicUtil.getSimpleClassName(fpo.getClass());
        return TypeUtil.unqualifiedName(cls);
    }

    @Override
    public Path getPath() {
        return column;
    }

    @Override
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
