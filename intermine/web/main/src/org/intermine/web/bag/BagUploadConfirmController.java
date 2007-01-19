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
import javax.servlet.http.HttpSession;

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
        HttpSession session = request.getSession();
        BagQueryResult bagQueryResult = (BagQueryResult) session.getAttribute("bagQueryResult");
        request.setAttribute("matches", bagQueryResult.getMatches());
        request.setAttribute("issues", bagQueryResult.getIssues());
        request.setAttribute("unresolved", bagQueryResult.getUnresolved());

        StringBuffer matchesStringBuffer = new StringBuffer();
        BagUploadConfirmForm bagUploadConfirmForm = ((BagUploadConfirmForm) form);
        Map matches = bagQueryResult.getMatches();
        // matches will be null if we get here if the form.validate() method fails
        if (matches != null) {
            Iterator matchIDIter = matches.keySet().iterator();
            while (matchIDIter.hasNext()) {
                matchesStringBuffer.append(matchIDIter.next()).append(' ');
            }
            bagUploadConfirmForm.setMatchIDs(matchesStringBuffer.toString().trim());
        }
        if (request.getAttribute("bagType") != null) {
            bagUploadConfirmForm.setBagType((String) request.getAttribute("bagType"));
        }
        String trimmedIds = bagUploadConfirmForm.getMatchIDs().trim();
        int matchCount;
        if (trimmedIds.length() > 0) {
            int spaceCount = StringUtils.countMatches(trimmedIds, " ");
            matchCount = spaceCount + 1;
        } else {
            matchCount = 0;
        }
        request.setAttribute("matchCount", new Integer(matchCount));
        return null;
    }
}
