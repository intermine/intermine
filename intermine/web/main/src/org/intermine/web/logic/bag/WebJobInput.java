package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.api.idresolution.JobInput;
import org.intermine.web.struts.BuildBagForm;

/**
 * @author Radek
 *
 */
public final class WebJobInput implements JobInput
{

    private final String type;
    private final Collection<String> idents;
    private final boolean caseSensitive;
    private final String extraValue;

    /**
     * Create a web-job.
     * @param type The type of thing to find.
     * @param idents The identifiers to look for.
     * @param form The form containing optional properties.
     */
    public WebJobInput(String type, Collection<String> idents, BuildBagForm form) {
        this.type = type;
        this.idents = idents;
        this.caseSensitive = form.getCaseSensitive();
        this.extraValue = form.getExtraFieldValue();
    }

    @Override
    public Collection<String> getIds() {
        return idents;
    }

    @Override
    public String getExtraValue() {
        return this.extraValue;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Boolean getCaseSensitive() {
        return this.caseSensitive;
    }

    @Override
    public Boolean getWildCards() {
        return false;
    }

}
