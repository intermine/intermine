package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.ConvertedObjectPair;
import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;

import org.intermine.webservice.server.idresolution.BagResultFormatter;
import org.json.JSONObject;

/**
 * Controller for the bagUploadConfirm
 * @author Kim Rutherford
 */
public class BagUploadConfirmController extends TilesAction
{
    /**
     * Set up the bagUploadConfirm page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        String bagName = (String) request.getAttribute("newBagName");
        String bagQueryResultLabel = "bagQueryResult";
        if (bagName != null) {
            bagQueryResultLabel = bagQueryResultLabel + "_" + bagName;
        }

        BagQueryResult bagQueryResult = (BagQueryResult) session.getAttribute(bagQueryResultLabel);

        // Use WS formatter for consistency.
        BagResultFormatter formatter = new BagResultFormatter(im);
        request.setAttribute("payload", (new JSONObject(formatter.format(bagQueryResult))).toString());

        BagUploadConfirmForm bagUploadConfirmForm = ((BagUploadConfirmForm) form);
        if (request.getAttribute("bagType") != null) {
            bagUploadConfirmForm.setBagType((String) request.getAttribute("bagType"));
        }

        if (bagName != null) {
            request.setAttribute("bagName", bagName);
        }

        return null;
    }

}