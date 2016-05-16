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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the object trail tile.
 *
 * @author Thomas Riley
 */
public class ObjectTrailController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(ObjectTrailController.class);

    /**
     * Looks at the "trail" request parameter and extracts the object ids from it, then
     * looks up the actual objects and creates a list of TrailItems.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        String trail = request.getParameter("trail");
        String queryBuilder = request.getParameter("queryBuilder");

        String[] ids = (!StringUtils.isEmpty(trail)) ? StringUtils.split(trail.substring(1), '|')
                : new String[0];
        ArrayList<TrailElement> elements = new ArrayList<TrailElement>();
        String elementTrail = "";

        for (int i = 0; i < ids.length; i++) {
            elementTrail += "|" + ids[i];

            // split this param pair again with . delimiter
            // will be something like bag.baggieName or results.col0 or itt.template.id
            String urlParam = ids[i];
            String[] breadcrumbs = StringUtils.split(urlParam, '.');

            // bag names allow for dots
            if ("bag".equals(breadcrumbs[0])) {
                List<String> bagName = new ArrayList<String>();
                for (int j = 1; j < breadcrumbs.length; j++) {
                    // replace all spaces with plus signs
                    bagName.add(breadcrumbs[j].replaceAll(" ", "+"));
                }
                // join on a dot. Also, array constants can be used in initialisers.
                String[] newBreadcrumbs = {"bag", StringUtils.join(bagName, ".")};
                breadcrumbs = newBreadcrumbs;
            }

            if ("results".equals(breadcrumbs[0])) {
                            //&& SessionMethods.getResultsTable(session, ids[i]) != null) {
                /* breadcrumbs[1] is the results table id
                 *  can be:
                 *      col0, col1, ... <-- template
                 *      qid0, qid1, ... <-- query
                 *      results.0   ... <-- query from querybuilder or quicksearch
                 *          ~~ re-add results.
                 *      itt <-- inline template table
                 *          ~~ re-concatenate itt.templateName.id
                 */
                String resultsTableId = breadcrumbs[1];
                String prepend = "";

                //(String label, String trail, int id)
                if (resultsTableId.startsWith("itt")) {
                    // inline template
                    String table = breadcrumbs[1] + "." + breadcrumbs[2] + "." + breadcrumbs[3];
                    elements.add(new TrailElement(table, elementTrail, "results"));
                } else {
                    /* results.do?table=col0&trail=|results.col0
                     * results.do?table=results.1636&trail=|results.1636
                     * sometimes you need to re-add "results." to the tablename in the trail
                     */
                    try {
                        Integer.parseInt(resultsTableId);
                        prepend = "results.";
                    } catch (Exception e)  {
                        // nothing to do
                    }
                    // for tables with id like 'bag.list50'
                    String tableId = "";
                    for (int j = 1; j < breadcrumbs.length; j++) {
                        tableId += breadcrumbs[j];
                        if (j < breadcrumbs.length - 1) {
                            tableId += ".";
                        }
                    }
                    elements.add(new TrailElement(prepend + tableId, elementTrail, "results"));
                }

            } else if ("bag".equals(breadcrumbs[0])) {
                // breadcrumbs[1] is the bag name
                elements.add(new TrailElement(breadcrumbs[1], elementTrail, "bag"));
            } else if ("query".equals(breadcrumbs[0])) {
                elements.add(new TrailElement("query", elementTrail, "query"));
            } else {
                InterMineObject o = null;
                try {
                    o = os.getObjectById(new Integer(breadcrumbs[0]));
                } catch (NumberFormatException err) {
                    LOG.warn("bad object id " + breadcrumbs[0]);
                    continue;
                }
                if (o == null) {
                    LOG.warn("failed to getObjectById " + breadcrumbs[0]);
                    continue;
                }
                String label = createTrailLabel(o, os.getModel());
                elements.add(new TrailElement(label, elementTrail, o.getId().intValue()));
            }
        }
        request.setAttribute("trailElements", elements);

        if (queryBuilder != null) {
            request.setAttribute("queryBuilder", queryBuilder);
        }
        return null;
    }

    /**
     * Create trail element label. Label is a list of each leaf class name.
     * @param object the intermine object associated with the trail element
     * @param model the model
     * @return label for TrailElement
     */
    protected static String createTrailLabel(InterMineObject object, Model model) {
        return StringUtils.trim(model.getClassDescriptorByName(
                DynamicUtil.getSimpleClassName(object)).getUnqualifiedName());
    }

    /**
     * Bean passed to JSP to represent an element in the trail.
     */
    public static final class TrailElement
    {
        private String label;
        private String trail;
        private int id;
        private String type;        // query, bag or table (as in results table)
        private String elementId;   // tableId or bagName or itt.templatename.id

        /**
         * Construct an object trail element.
         * @param label link label
         * @param trail partial trail
         * @param id object id
         */
        private TrailElement(String label, String trail, int id) {
            this.label = label;
            this.trail = trail;
            this.id = id;
            this.type = "object";
        }

        /**
         * Construct a trail element.
         * @param id identifier
         * @param whichObject what kind of trail element this is:  bag, query or table
         * @param trail object trail - only used for results tables.
         */
        private TrailElement(String id, String trail, String whichObject) {
            this.type = whichObject;
            this.elementId = id;
            this.trail = trail;
        }

        /**
         * Return whether or not this trail element refers to a table.
         * @return true if this element refers to a table
         */
        public String getType() {
            return type;
        }

        /**
         * Return the table identifier if isTable==true. If isTable==false this method
         * will return null.
         * @return table identifier
         */
        public String getElementId() {
            return elementId;
        }

        /**
         * Get the trail URL parameter for this trail element.
         * @return trail URL parameter for the trail element
         */
        public String getTrail() {
            return trail;
        }

        /**
         * Get the label for this trail element.
         * @return label for the trail element
         */
        public String getLabel() {
            return label;
        }

        /**
         * Get the object id for this trail element.
         * @return the object id for the trail element
         */
        public int getObjectId() {
            return id;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return new ToStringBuilder(this)
                .append("type", type)
                .append("elementId", elementId)
                .append("objectId", id).toString();
        }
    }
}
