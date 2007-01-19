package org.intermine.web.bag;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the bagUploadConfirm
 * @author Kim Rutherford
 */
public class BagUploadConfirmController extends TilesAction
{
    /**
     * Set up the bagUploadConfirm page.
     * @see TilesAction#execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        StringBuffer matchesStringBuffer = new StringBuffer();
        BagUploadConfirmForm bagUploadConfirmForm = ((BagUploadConfirmForm) form);
        Map matches = (Map) request.getAttribute("matches");
        // matches will be null if we get here if the form.validate() method fails
        if (matches != null) {
            Iterator matchIDIter = matches.keySet().iterator();
            while (matchIDIter.hasNext()) {
                matchesStringBuffer.append(matchIDIter.next()).append(' ');
            }

            bagUploadConfirmForm.setBagType((String) request.getAttribute("bagType"));
            bagUploadConfirmForm.setMatchIDs(matchesStringBuffer.toString().trim());
        }
        int spaceCount = StringUtils.countMatches(bagUploadConfirmForm.getMatchIDs().trim(), " ");
        request.setAttribute("matchCount", new Integer(spaceCount + 1));
        return null;
    }
}
