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
 * Form for sequence export (FASTA probably)
 * @author Kim Rutherford
 */
public class GFF3ExportForm extends TableExportForm
{
    private Set<Integer> taxIds = null;


    /**
     * Set the organisms set
     *
     * @param set of taxon ids
     */
    public void setOrganisms(Set taxIds) {
        this.taxIds = taxIds;
    }

    /**
     * Get the organisms set
     *
     * @return a set of taxon ids
     */
    public Set<Integer> getOrganisms() {
        return taxIds;
    }

    
}
