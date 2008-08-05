package org.modmine.web;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;

/**
 * Controller for results.jsp
 * @author Tom Riley
 */
public class ResultsController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form,
            HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);

            //get the classes and the counts 
            Query q = new Query();
            
            QueryClass qc = new QueryClass(LocatedSequenceFeature.class);
            q.addFrom(qc);
            QueryField qf = new QueryField(qc, "class");
            q.addToSelect(qf);
            q.addToSelect(new QueryFunction());
            q.addToGroupBy(qf);
            q.setDistinct(false);

            Results results = os.execute(q);

            Map<String, Long> fc =
                new LinkedHashMap<String, Long>();

            // for each classes set the values for jsp
            for (Iterator iter = results.iterator(); iter.hasNext(); ) {
                ResultsRow row = (ResultsRow) iter.next();
                Class feat = (Class) row.get(0);
                Long count = (Long) row.get(1);

                fc.put(TypeUtil.unqualifiedName(feat.getName()), count);
                
            }            

           request.setAttribute("features", fc);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
