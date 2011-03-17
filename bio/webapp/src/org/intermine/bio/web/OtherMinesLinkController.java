package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;

/**
 * Show Other Mines Links only on "Gene" page
 *
 * @author radek
 *
 */
public class OtherMinesLinkController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) {

        // we only want to display the links on a Gene Report Page
        InterMineObject imObj = (InterMineObject) request.getAttribute("object");
        if ("Gene".equals(DynamicUtil.getSimpleClass(imObj.getClass()).getSimpleName())) {
            request.setAttribute("gogogo", true);
        }

        return null;
    }
}
