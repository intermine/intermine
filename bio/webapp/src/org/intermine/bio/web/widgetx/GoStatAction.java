package org.intermine.bio.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;
import org.intermine.web.struts.WebPathCollection;

import org.flymine.model.genomic.GOAnnotation;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Organism;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
/**
 * Builds a query to get all the genes (in bag) associated with specified go term.
 * @author Julie Sullivan
 */
public class GoStatAction extends InterMineAction
{
   private int index = 0;

    /**

     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
                    throws Exception {

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        String bagName = request.getParameter("bag");
        String goTermId = request.getParameter("key");

        Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(currentProfile.getSavedBags(), servletContext);
        InterMineBag bag = allBags.get(bagName);

        // select * from gene where goannotation = goterm and gene in bag

        Query q = new Query();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcGoAnnotation = new QueryClass(GOAnnotation.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcGo = new QueryClass(GOTerm.class);

        QueryField qfQualifier = new QueryField(qcGoAnnotation, "qualifier");
        QueryField qfGeneId = new QueryField(qcGene, "id");
        QueryField qfGoTerm = new QueryField(qcGo, "identifier");

        q.addFrom(qcGene);
        q.addFrom(qcGoAnnotation);
        q.addFrom(qcOrganism);
        q.addFrom(qcGo);

        q.addToSelect(qcGene);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (bag != null) {
            // genes must be in bag
            BagConstraint bc1 = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
            cs.addConstraint(bc1);
        }

        // gene.goAnnotation CONTAINS GOAnnotation
        QueryCollectionReference qr1 = new QueryCollectionReference(qcGene, "allGoAnnotation");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcGoAnnotation);
        cs.addConstraint(cc1);

        // gene is from organism
        QueryObjectReference qr2 = new QueryObjectReference(qcGene, "organism");
        ContainsConstraint cc2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc2);

        // goannotation contains go term
        QueryObjectReference qr3 = new QueryObjectReference(qcGoAnnotation, "property");
        ContainsConstraint cc3 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qcGo);
        cs.addConstraint(cc3);

        // can't be a NOT relationship!
        SimpleConstraint sc1 = new SimpleConstraint(qfQualifier,
                                                     ConstraintOp.IS_NULL);
        cs.addConstraint(sc1);

        SimpleConstraint sc2 = new SimpleConstraint(qfGoTerm,
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue(goTermId));
        cs.addConstraint(sc2);

        q.setConstraint(cs);

        Results results = os.execute(q);

        String columnName = "Gene";
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();
        WebPathCollection webPathCollection =
            new WebPathCollection(os, new Path(model, columnName), results, model, webConfig,
                              classKeys);
        PagedTable pagedColl = new PagedTable(webPathCollection);

        String identifier = "qid" + index++;
        SessionMethods.setResultsTable(session, identifier, pagedColl);

        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier)
                        .addParameter("size", "10")
                        .addParameter("trail", "|bag." + bagName).forward();



    }
}
