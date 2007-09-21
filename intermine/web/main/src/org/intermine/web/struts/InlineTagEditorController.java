package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the inline tag editing tile
 * @author Thomas Riley
 */
public class InlineTagEditorController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(InlineTagEditorController.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        // Retrieve the taggable thing from the context
        Object taggable = context.getAttribute("taggable");
        ProfileManager pm = (ProfileManager) request.getSession()
            .getServletContext().getAttribute(Constants.PROFILE_MANAGER);
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        
        String uid = null;
        String type = null;
        
        if (taggable instanceof FieldDescriptor) {
            FieldDescriptor fd = (FieldDescriptor) taggable;
            uid = fd.getClassDescriptor().getUnqualifiedName() + "." + fd.getName();
            if (taggable instanceof CollectionDescriptor) {
                type = "collection";
            } else if (taggable instanceof ReferenceDescriptor) {
                type = "reference";
            } else {
                type = "attribute";
            }
        } else if (taggable instanceof TemplateQuery) {
            type = TagTypes.TEMPLATE;
            uid = ((TemplateQuery) taggable).getName();
        } else if (taggable instanceof ClassDescriptor) {
            type = TagTypes.CLASS;
            uid = ((ClassDescriptor) taggable).getName();
        } else if (taggable instanceof InterMineBag) {
            type = TagTypes.BAG;
            uid = ((InterMineBag) taggable).getName();
        }
        
        request.setAttribute("uid", uid);
        request.setAttribute("type", type);
        request.setAttribute("currentTags", pm.getTags(null, uid, type, profile.getUsername()));
        
        return null;
    }
}


