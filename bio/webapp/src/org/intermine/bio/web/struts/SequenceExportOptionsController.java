package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.results.PagedTable;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;
import org.flymine.model.genomic.Translation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for sequence export tile.
 * @author Kim Rutherford
 */
public class SequenceExportOptionsController extends TilesAction
{
    /**
     * Set up the bagUploadConfirm page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        PagedTable pt = (PagedTable) request.getAttribute("resultsTable");
        List<Integer> featureColumns = getFeatureColumns(pt);
        request.setAttribute("testAttr", StringUtil.join(featureColumns, " "));
        return null;
    }

    /**
     * Return the columns that contains Sequence objects or features that have a sequence
     * reference.
     */
    private List<Integer> getFeatureColumns(PagedTable pt) {
        List<Integer> retList = new ArrayList<Integer>();
        List<Class> clazzes = ExportHelper.getColumnClasses(pt);
        for (int i = 0; i < clazzes.size(); i++) {
            if (Protein.class.isAssignableFrom(clazzes.get(i))
                || LocatedSequenceFeature.class.isAssignableFrom(clazzes.get(i))
                || Sequence.class.isAssignableFrom(clazzes.get(i))
                || Translation.class.isAssignableFrom(clazzes.get(i))) {
                retList.add(i);
            }
        }
        return retList;
    }
}
