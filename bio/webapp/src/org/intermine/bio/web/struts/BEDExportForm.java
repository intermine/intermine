package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.web.struts.TableExportForm;

/**
 * Form for sequence export in BED format
 *
 * @author Fengyuan Hu
 */
public class BEDExportForm extends TableExportForm
{
    private static final long serialVersionUID = 1L;
    private Set<Integer> taxonIds = null;
    private boolean makeUcscCompatible = false;
    private String ucscCompatibleCheck; // a patch to Struts checkbox design...
    private String trackDescription = "";

    /**
     * @return the trackDescription
     */
    public String getTrackDescription() {
        return trackDescription;
    }

    /**
     * @param trackDescription the trackDescription to set
     */
    public void setTrackDescription(String trackDescription) {
        this.trackDescription = trackDescription;
    }

    /**
     * Return whether to make exported BED compatible with UCSC genome browser.
     * @return true if BED should be UCSC genome browser compatible
     */
    public boolean getMakeUcscCompatible() {
        return makeUcscCompatible;
    }

    /**
     * Set whether to make exported BED compatible with UCSC genome browser.
     * @param makeUcscCompatible true if BED should be UCSC genome browser compatible
     */
    public void setMakeUcscCompatible(boolean makeUcscCompatible) {
        this.makeUcscCompatible = makeUcscCompatible;
    }

    /**
     * Set the organisms
     *
     * @param taxonIds set of taxon ids
     */
    public void setOrganisms(Set<Integer> taxonIds) {
        this.taxonIds = taxonIds;
    }

    /**
     * Get the organisms set
     *
     * @return a set of taxon ids
     */
    public Set<Integer> getOrganisms() {
        return taxonIds;
    }

    /**
     * @return the ucscCompatibleCheck
     */
    public String getUcscCompatibleCheck() {
        return ucscCompatibleCheck;
    }

    /**
     * @param ucscCompatibleCheck the ucscCompatibleCheck to set
     */
    public void setUcscCompatibleCheck(String ucscCompatibleCheck) {
        this.ucscCompatibleCheck = ucscCompatibleCheck;
    }

    /**
     *
     */
    public BEDExportForm() {
        super();
        this.makeUcscCompatible = true;
    }
}
