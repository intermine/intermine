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
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
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
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        final HttpSession session = request.getSession();
        final ServletContext servletContext = session.getServletContext();
        final Properties webProperties = SessionMethods.getWebProperties(servletContext);
        InterMineObject o = (InterMineObject) request.getAttribute("object");
        Model model = im.getModel();
        Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(o.getClass());
        ClassDescriptor gene = model.getClassDescriptorByName("org.intermine.model.bio.Gene");
        if (classDescriptors.contains(gene)) {
            final FriendlyMineManager linkManager
                = FriendlyMineManager.getInstance(im, webProperties);
            Collection<Mine> mines = linkManager.getFriendlyMines();
            request.setAttribute("otherMines", mines);
        }
        return null;
    }

}
