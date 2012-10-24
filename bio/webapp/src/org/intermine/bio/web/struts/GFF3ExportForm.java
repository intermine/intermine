package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * Form for sequence export (FASTA probably)
 * @author Kim Rutherford
 */
public class GFF3ExportForm extends TableExportForm
{
    private Set<Integer> taxonIds = null;
    private boolean makeUcscCompatible = false;

    /**
     * Return whether to make exported GFF3 compatible with UCSC genome browser.
     * @return true if GFF3 should be UCSC genome browser compatible
     */
    public boolean makeUcscCompatible() {
        return makeUcscCompatible;
    }

    /**
     * Set whether to make exported GFF3 compatible with UCSC genome browser.
     * @param makeUcscCompatible true if GFF3 should be UCSC genome browser compatible
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
}
