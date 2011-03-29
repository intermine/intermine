package org.intermine.bio.web.displayer;

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
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.GeneModel;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Transcript;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.results.InlineTableResultElement;
import org.intermine.web.logic.session.SessionMethods;
import org.jfree.util.Log;

public class GeneStructureDisplayer extends CustomDisplayer {

    protected static final Logger LOG = Logger.getLogger(GeneStructureDisplayer.class);

    public GeneStructureDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        String clsName = DynamicUtil.getSimpleClass(reportObject.getObject()).getSimpleName();

        request.setAttribute("message", "Gene structure displayer for type: " + clsName);

        Gene gene = null;
        if ("Gene".equals(clsName)) {
            gene = (Gene) reportObject.getObject();
        } else if ("Transcript".equals(clsName) || "MRNA".equals(clsName)
                || "Exon".equals(clsName)) {
            InterMineObject imObj = (InterMineObject) reportObject.getObject();
            try {
                gene = (Gene) imObj.getFieldValue("gene");
            } catch (IllegalAccessException e) {
                Log.warn("Failed to get gene from " + clsName + ": " + imObj.getId());
            }
        }

        if (gene != null) {
            try {
                List<GeneModel> geneModels = new ArrayList<GeneModel>();
                for (InterMineObject transcript : (Collection<InterMineObject>) gene.getFieldValue("transcripts")) {
                    geneModels.add(new GeneModel(im.getModel(), transcript));
                }
                request.setAttribute("geneModels", geneModels);
            } catch (IllegalAccessException e) {
                LOG.error("Error accessing transcripts collection for gene: "
                        + gene.getPrimaryIdentifier() + ", " + gene.getId());
            }
            request.setAttribute("gene", gene);
            request.setAttribute("actualId", reportObject.getId());
        }


    }

    private void old(HttpServletRequest request, ReportObject reportObject) {
        // 0. work out available classes for organism
        // 1. work out feature type
        // 2. get to gene from feature
        // 3. create table starting from gene
        //    - list of lists of result elements
        //    - one row per transcript
        //    - columns:  gene, transcript, exon, intron, translation, CDS, protein
        //    - exons/introns column with multiple result elements ordered by start position
        //    - request: mark current feature for highlighting
        // 4. cache table

        String clsName = DynamicUtil.getSimpleClass(reportObject.getObject()).getSimpleName();

        request.setAttribute("message", "Gene structure displayer for type: " + clsName);

        List<List<List<List<ResultElement>>>> ugly = new ArrayList<List<List<List<ResultElement>>>>();
        if ("Gene".equals(clsName)) {
            Gene gene = (Gene) reportObject.getObject();

            PathQuery pq = new PathQuery(im.getModel());
            pq.addView("Gene.symbol");
            pq.addConstraint(Constraints.eq("Gene.id", "" + gene.getId()));

            ClassDescriptor geneCld = im.getModel().getClassDescriptorByName("Gene");
            CollectionDescriptor transcriptsCol = geneCld.getCollectionDescriptorByName("transcripts");
            if (transcriptsCol != null) {

                pq.addView("Gene.transcripts.primaryIdentifier");
                ClassDescriptor transcriptCld = transcriptsCol.getReferencedClassDescriptor();
                CollectionDescriptor exonsCol = transcriptCld.getCollectionDescriptorByName("exons");
                if (exonsCol != null) {
                    pq.addView("Gene.transcripts.exons.primaryIdentifier");
                    pq.setOuterJoinStatus("Gene.transcripts.exons", OuterJoinStatus.OUTER);
                }
            }

            Profile profile = SessionMethods.getProfile(request.getSession());
            PathQueryExecutor pqe = im.getPathQueryExecutor(profile);

            WebConfig webConfig = SessionMethods.getWebConfig(request);

            List<List<InlineTableResultElement>> table = new ArrayList<List<InlineTableResultElement>>();
            ExportResultsIterator iter = pqe.execute(pq);
            while (iter.hasNext()) {
                List<ResultElement> row = iter.next();

                List<InlineTableResultElement> displayRow = new ArrayList<InlineTableResultElement>();
                for (ResultElement re : row) {
                    InterMineObject obj = (InterMineObject) re.getObject();
                    addFieldOrDisplayer(displayRow, obj, webConfig, re.getPath().toString());
                    if (Transcript.class.isAssignableFrom(DynamicUtil.getSimpleClass(obj))) {
                        addFieldOrDisplayer(displayRow, obj, webConfig, "Gene.transcripts.length");
                        addFieldOrDisplayer(displayRow, obj, webConfig, "Gene.transcripts.chromosomeLocation");
                    }
                    if (Exon.class.isAssignableFrom(DynamicUtil.getSimpleClass(obj))) {
                        addFieldOrDisplayer(displayRow, obj, webConfig, "Gene.transcripts.exons.length");
                        addFieldOrDisplayer(displayRow, obj, webConfig, "Gene.transcripts.exons.chromosomeLocation");
                    }
                }

                table.add(displayRow);
            }
            request.setAttribute("table", table);
        }
    }

    private void addFieldOrDisplayer(List<InlineTableResultElement> displayRow, InterMineObject obj,
            WebConfig webConfig, String pathString) {
        Path path = null;
        try {
            path = new Path(im.getModel(), pathString);
        } catch (PathException e) {
            return;
        }
        FieldConfig fc = getFieldConfig(webConfig, obj, path.getLastElement());
        if (fc != null) {
            displayRow.add(new InlineTableResultElement(obj, path, fc, true));
        }

    }

    private FieldConfig getFieldConfig(WebConfig webConfig, InterMineObject obj, String fieldName) {
        String clsName = DynamicUtil.getSimpleClass(obj).getSimpleName();
        for (FieldConfig fc : getFieldConfigs(webConfig, clsName)) {
            if (fc.getFieldExpr().equals(fieldName)) {
                return fc;
            }
        }
        return null;
    }

    private List<FieldConfig> getFieldConfigs(WebConfig webConfig, String clsName) {
        ClassDescriptor cld = im.getModel().getClassDescriptorByName(clsName);
        return FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
    }



    private boolean classExists(String clsName) {
        ClassDescriptor cld = im.getModel().getClassDescriptorByName(clsName);
        return !(cld == null);
    }

    private boolean fieldExists(String clsName, String fieldName) {
        ClassDescriptor cld = im.getModel().getClassDescriptorByName(clsName);
        FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
        return !(fd == null);
    }

}
