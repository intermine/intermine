package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.results.ResultElement;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.FieldConfig;

/**
 * ResultElement used in an InlineResultsTable
 * @author radek
 *
 */
public class InlineTableResultElement extends ResultElement
{
    /** @FieldConfig used to determine if the said field has a displayer defined */
    private FieldConfig fc = null;

    /**
     * Instantiate ResultElement while saving its FieldConfig
     * @param imObj InterMine Object
     * @param path Path
     * @param fc FieldConfig
     * @param isKeyField is this a key field?
     */
    public InlineTableResultElement(FastPathObject imObj, Path path, FieldConfig fc,
            Boolean isKeyField) {
        super(imObj, path, isKeyField);
        this.fc = fc;
    }

    /**
     *
     * @return if a Displayer is configured
     */
    public Boolean getHasDisplayer() {
        return (fc.getDisplayer() != null && fc.getDisplayer().length() > 0);
    }

    /**
     *
     * @return FieldConfig
     */
    public FieldConfig getFieldConfig() {
        return fc;
    }

    /**
     *
     * @return true if this is a key field
     */
    public Boolean getIsKeyField() {
        return keyField;
    }

    /**
     *
     * @return a class name so we can display what kind of object, in a Collection, this is
     */
    public String getClassName() {
        return DynamicUtil.getSimpleClass((InterMineObject) imObj).getSimpleName();
    }

}
