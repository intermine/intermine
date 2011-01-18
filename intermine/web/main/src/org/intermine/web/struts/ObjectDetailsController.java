package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.api.tag.TagNames;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.PortalHelper;
import org.intermine.web.logic.results.DisplayCollection;
import org.intermine.web.logic.results.DisplayField;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.DisplayObjectFactory;
import org.intermine.web.logic.results.DisplayReference;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.model.userprofile.Tag;

/**
 * Implementation of <strong>Action</strong> that assembles data for viewing an
 * object.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ObjectDetailsController extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(ObjectDetailsController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        TagManager tagManager = im.getTagManager();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = im.getObjectStore();
        DisplayObjectFactory displayObjects = SessionMethods.getDisplayObjects(session);

        String idString = request.getParameter("id");

        Integer id = null;
        try {
            id = new Integer(Integer.parseInt(idString));
        } catch (NumberFormatException e) {
            return null;
        }
        InterMineObject object = os.getObjectById(id);
        if (object == null) {
            return null;
        }

        String superuser = im.getProfileManager().getSuperuser();

        DisplayObject dobj = displayObjects.get(object);
        dobj.getClass();
        request.setAttribute("object", dobj);
        session.setAttribute("displayObject", dobj);

        Map<String, Map<String, DisplayField>> placementRefsAndCollections = new TreeMap<String,
            Map<String, DisplayField>>();
        Set<String> aspects = new HashSet<String>(SessionMethods.getCategories(servletContext));

        Set<ClassDescriptor> cds = os.getModel().getClassDescriptorsForClass(object.getClass());

        placementRefsAndCollections.put(TagNames.IM_SUMMARY,
                getSummaryFields(tagManager, superuser, dobj, cds));

        for (String aspect : aspects) {
            placementRefsAndCollections.put(TagNames.IM_ASPECT_PREFIX + aspect,
                    new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER));
        }

        Map<String, DisplayField> miscRefs = new TreeMap<String, DisplayField>(
                dobj.getRefsAndCollections());
        placementRefsAndCollections.put(TagNames.IM_ASPECT_MISC, miscRefs);

        for (Iterator<Entry<String, DisplayField>> iter
                = dobj.getRefsAndCollections().entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, DisplayField> entry = iter.next();
            DisplayField df = entry.getValue();
            if (df instanceof DisplayReference) {
                categoriseBasedOnTags(((DisplayReference) df).getDescriptor(),
                        "reference", df, miscRefs, tagManager, superuser,
                        placementRefsAndCollections, SessionMethods.isSuperUser(session));
            } else if (df instanceof DisplayCollection) {
                categoriseBasedOnTags(((DisplayCollection) df).getDescriptor(),
                        "collection", df, miscRefs, tagManager, superuser,
                        placementRefsAndCollections, SessionMethods.isSuperUser(session));
            }
        }

        request.setAttribute("placementRefsAndCollections", placementRefsAndCollections);

        String type = DynamicUtil.getSimpleClass(object.getClass()).getCanonicalName();
        request.setAttribute("objectType", type);

        String stableLink = PortalHelper.generatePortalLink(object, im, request);
        if (stableLink != null) {
            request.setAttribute("stableLink", stableLink);
        }
        return null;
    }

    /**
     * Returns fields that should be displayed in summary.
     * @param tagManager tag manager
     * @param superuser
     * @param dobj
     * @param cds
     * @return
     */
    private Map<String, DisplayField> getSummaryFields(TagManager tagManager, String superuser,
            DisplayObject dobj, Set<ClassDescriptor> cds) {
        Map<String, DisplayField> ret =
            new TreeMap<String, DisplayField>(String.CASE_INSENSITIVE_ORDER);
        for (ClassDescriptor cd : cds) {

            // get all summary tags for all refs and collections of
            // this class
            List<Tag> placementTags = new ArrayList<Tag>(tagManager.getTags(TagNames.IM_SUMMARY,
                    cd.getUnqualifiedName() + ".%", "reference", superuser));
            placementTags.addAll(tagManager.getTags(TagNames.IM_SUMMARY,
                    cd.getUnqualifiedName() + ".%", "collection", superuser));

            for (Tag tag : placementTags) {
                String name = getFieldName(tag);
                ret.put(name, dobj.getRefsAndCollections().get(name));
            }
        }
        return ret;
    }

    private String getFieldName(Tag tag) {
        String objectIdentifier = tag.getObjectIdentifier();
        int dotIndex = objectIdentifier.indexOf(".");
        String fieldName = objectIdentifier.substring(dotIndex + 1);
        return fieldName;
    }

    /**
     * For a given FieldDescriptor, look up its 'aspect:' tags and place it in
     * the correct map within placementRefsAndCollections. If categorised,
     * remove it from the supplied miscRefs map.
     *
     * @param fd the FieldDecriptor (a references or collection)
     * @param taggedType 'reference' or 'collection'
     * @param dispRef the corresponding DisplayReference or DisplayCollection
     * @param miscRefs map that contains dispRef (may be removed by this method)
     * @param tagManager the tag manager
     * @param sup  the superuser account name
     * @param placementRefsAndCollections take from the DisplayObject
     * @param isSuperUser if current user is superuser
     */
    public static void categoriseBasedOnTags(FieldDescriptor fd,
            String taggedType, DisplayField dispRef, Map<String, DisplayField> miscRefs,
            TagManager tagManager, String sup, Map<String, Map<String, DisplayField>>
            placementRefsAndCollections, boolean isSuperUser) {
        List<Tag> tags = tagManager.getTags(null, fd.getClassDescriptor()
                .getUnqualifiedName()
                + "." + fd.getName(), taggedType, sup);
        for (Tag tag : tags) {
            String tagName = tag.getTagName();
            if (!isSuperUser && tagName.equals(TagNames.IM_HIDDEN)) {
                miscRefs.remove(fd.getName());
                // Maybe it was added already to some placement and
                // that's why it must be removed
                removeField(fd.getName(), placementRefsAndCollections);
                return;
            }
            if (AspectTagUtil.isAspectTag(tagName)) {
                Map<String, DisplayField> refs = placementRefsAndCollections.get(tagName);
                if (refs != null) {
                    refs.put(fd.getName(), dispRef);
                    miscRefs.remove(fd.getName());
                }
            } else if (tagName.equals(TagNames.IM_SUMMARY)) {
                miscRefs.remove(fd.getName());
            }
        }
    }

    /**
     * Removes field from placements.
     *
     * @param name
     * @param placementRefsAndCollections
     */
    private static void removeField(String name,
            Map<String, Map<String, DisplayField>> placementRefsAndCollections) {
        Iterator<Entry<String, Map<String, DisplayField>>> it = placementRefsAndCollections
                .entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Map<String, DisplayField>> entry = it.next();
            entry.getValue().remove(name);
        }
    }
}
