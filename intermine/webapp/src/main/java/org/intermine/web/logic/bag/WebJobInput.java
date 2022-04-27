package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2022 FlyMine
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
    private boolean caseSensitive = false;
    private String extraValue = "";
    // TRUE if we are uploading a list.
    // match behaviour is different in LOOKUPs and list uploads. See #1494
    private final boolean ignoreConfig;

    /**
     * Create a web-job.
     * @param type The type of thing to find.
     * @param idents The identifiers to look for.
     * @param form The form containing optional properties.
     */
    public WebJobInput(String type, Collection<String> idents, BuildBagForm form) {
        this.type = type;
        this.idents = idents;
        if (form != null) {
            this.caseSensitive = form.getCaseSensitive();
            this.extraValue = form.getExtraFieldValue();
        }
        ignoreConfig = false;
    }

    /**
     * Create a web-job.
     * @param type The type of thing to find.
     * @param idents The identifiers to look for.
     * @param form The form containing optional properties.
     * @param ignoreConfig TRUE if this is a LOOKUP query and we want to ignore config
     */
    public WebJobInput(String type, Collection<String> idents, BuildBagForm form,
        boolean ignoreConfig) {
        this.type = type;
        this.idents = idents;
        this.caseSensitive = form.getCaseSensitive();
        this.extraValue = form.getExtraFieldValue();
        this.ignoreConfig = ignoreConfig;
    }


    @Override
    public Collection<String> getIds() {
        return idents;
    }

    @Override
    public String getExtraValue() {
        return this.extraValue;
    }

    /**
     * Only used in the portal when we have to link directly to the list upload
     * @param extraValue organism name
     */
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
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

    @Override
    public Boolean getIgnoreConfig() {
        return ignoreConfig;
    }
}
