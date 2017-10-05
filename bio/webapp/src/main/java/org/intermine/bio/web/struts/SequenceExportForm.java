package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.intermine.web.struts.TableExportForm;

/**
 * Form for sequence export (FASTA etc.)
 * @author Kim Rutherford
 */
public class SequenceExportForm extends TableExportForm
{
    private static final long serialVersionUID = 1L;
    private String sequencePath;

    /**
     * Constructor
     */
    public SequenceExportForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    @Override
    public void initialise() {
        super.initialise();
        sequencePath = null;
    }

    /**
     * Sets the selected sequence path.  ie. the sequence to export
     *
     * @param sequencePath the selected path
     */
    public void setSequencePath(String sequencePath) {
        this.sequencePath = sequencePath;
    }

    /**
     * Gets the selected path
     *
     * @return the selected path
     */
    public String getSequencePath() {
        return sequencePath;
    }

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
    }
}
