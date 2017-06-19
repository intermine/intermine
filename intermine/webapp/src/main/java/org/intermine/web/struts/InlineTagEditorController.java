package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.Taggable;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the inline tag editing tile
 * @author Thomas Riley
 */
public class InlineTagEditorController extends TilesAction
{

    private static int counter = 0;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        // Retrieve the taggable thing from the context
        Object taggable = context.getAttribute("taggable");
        Profile profile = SessionMethods.getProfile(request.getSession());

        String tagged = null;
        String type = null;

        if (taggable instanceof Taggable) {
            tagged = ((Taggable) taggable).getName();
            type = ((Taggable) taggable).getTagType();
        } else if (taggable instanceof FieldDescriptor) {
            FieldDescriptor fd = (FieldDescriptor) taggable;
            tagged = fd.getClassDescriptor().getUnqualifiedName() + "." + fd.getName();
            if (taggable instanceof CollectionDescriptor) {
                type = "collection";
            } else if (taggable instanceof ReferenceDescriptor) {
                type = "reference";
            } else {
                type = "attribute";
            }
        } else if (taggable instanceof ClassDescriptor) {
            tagged = ((ClassDescriptor) taggable).getName();
            type = TagTypes.CLASS;
        }

        request.setAttribute("editorId", createUniqueEditorId());
        request.setAttribute("taggableIdentifer", tagged);
        request.setAttribute("type", type);

        Set<String> currentTags;
        Set<String> availableTags;
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        TagManager tagManager = im.getTagManager();
        if (profile.isLoggedIn()) {
            currentTags = tagManager.getObjectTagNames(tagged, type, profile.getUsername());
            availableTags = tagManager.getUserTagNames(type, profile.getUsername());
        } else {
            currentTags = new TreeSet<String>();
            availableTags = new TreeSet<String>();
        }
        request.setAttribute("currentTags", currentTags);
        request.setAttribute("availableTags", availableTags);
        return null;
    }

    private String createUniqueEditorId() {
        return "inline-tag-editor-" + counter++;
    }
}
