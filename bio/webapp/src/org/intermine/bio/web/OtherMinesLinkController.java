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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Show Other Mines Links on "Gene" page
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
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        // we only want to display the links on a Gene Report Page
        InterMineObject o = (InterMineObject) request.getAttribute("object");
        if ("Gene".equals(DynamicUtil.getSimpleClass(o.getClass()).getSimpleName())) {
            FriendlyMineManager linkManager = im.getFriendlyMineManager();
            Collection<Mine> mines = linkManager.getFriendlyMines();
            request.setAttribute("otherMines", mines);
        }
        return null;
    }
}
