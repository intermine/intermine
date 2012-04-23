package org.modmine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.util.NameUtil;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.struts.GFF3ExportForm;
import org.intermine.bio.web.struts.SequenceExportForm;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;
import org.intermine.web.struts.TableExportForm;
import org.modmine.web.model.SpanQueryResultRow;
import org.modmine.web.model.SpanUploadConstraint;

/**
 * Generate queries for overlaps of submission features and overlaps with gene flanking regions.
 *
 * @author Richard Smith
  */
public class FeaturesAction extends InterMineAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(FeaturesAction.class);

    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers in text field.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();
        Model model = im.getModel();

        String type = request.getParameter("type");
        String featureType = request.getParameter("feature");
        String action = request.getParameter("action");

        String dccId = null;
        String sourceFile = null;
        String experimentName = null;

        final Map<String, LinkedList<String>> gffFields = new HashMap<String, LinkedList<String>>();
        populateGFFRelationships(gffFields);

        boolean doGzip = false;
        if (request.getParameter("gzip") != null
//                && request.getParameter("gzip").equalsIgnoreCase("true")) {
            && "true".equalsIgnoreCase(request.getParameter("gzip"))) {
            doGzip = true;
        }

        Set<Integer> taxIds = new HashSet<Integer>();

        PathQuery q = new PathQuery(model);

        boolean hasPrimer = false;

        if ("experiment".equals(type)) {
            experimentName = request.getParameter("experiment");
            DisplayExperiment exp = MetadataCache.getExperimentByName(os, experimentName);

            Set<String> organisms = exp.getOrganisms();
            taxIds = getTaxonIds(organisms);

            if ("all".equalsIgnoreCase(featureType)) {
                // fixed query for the moment
                String project = request.getParameter("project");
                String rootChoice = getRootFeature(project);
                List<String> gffFeatures = new LinkedList<String>(gffFields.get(project));

                for (String f : gffFeatures) {
                    q.addView(f + ".primaryIdentifier");
                    q.addView(f + ".score");
                }

                q.addConstraint(Constraints.eq(rootChoice + ".submissions.experiment.name",
                        experimentName));
            } else {

                List<String> expSubsIds = exp.getSubmissionsDccId();
                Set<String> allUnlocated = new HashSet<String>();

                String ef = getFactors(exp);
                if (ef.contains("primer")) {
                    hasPrimer = true;
                }

                String description = "All " + featureType + " features generated by experiment '"
                    + exp.getName() + "' in " + StringUtil.prettyList(exp.getOrganisms())
                    + " (" + exp.getPi() + ")." + ef;
                q.setDescription(description);

                for (String subId : expSubsIds) {
                    if (MetadataCache.getUnlocatedFeatureTypes(os).containsKey(subId))
                    {
                        allUnlocated.addAll(
                                MetadataCache.getUnlocatedFeatureTypes(os).get(subId));
                    }
                }

                q.addView(featureType + ".primaryIdentifier");
                q.addView(featureType + ".score");
                if ("results".equals(action)) {
                    // we don't want this field on exports
                    q.addView(featureType + ".scoreProtocol.name");
                    q.setOuterJoinStatus(featureType + ".scoreProtocol",
                            OuterJoinStatus.OUTER);
                }
                q.addConstraint(Constraints.eq(featureType + ".submissions.experiment.name",
                        experimentName));

                if (allUnlocated.contains(featureType)) {
                    q.addView(featureType + ".submissions.DCCid");
                    addEFactorToQuery(q, featureType, hasPrimer);
                } else {
                    addLocationToQuery(q, featureType);
                    addEFactorToQuery(q, featureType, hasPrimer);
                }
            }
        } else if ("submission".equals(type)) {
            dccId = request.getParameter("submission");
            if (request.getParameter("file") != null) {
                sourceFile = request.getParameter("file").replace(" ", "+");
            }

            Submission sub = MetadataCache.getSubmissionByDccId(os, dccId);
            List<String>  unlocFeatures =
                MetadataCache.getUnlocatedFeatureTypes(os).get(dccId);

            Integer organism = sub.getOrganism().getTaxonId();
            taxIds.add(organism);

            if ("all".equalsIgnoreCase(featureType)) {
                String project = request.getParameter("project");
                String rootChoice = getRootFeature(project);
                List<String> gffFeatures = new LinkedList<String>(gffFields.get(project));

                for (String f : gffFeatures) {
                    q.addView(f + ".primaryIdentifier");
                    q.addView(f + ".score");
                }

                q.addConstraint(Constraints.eq(rootChoice + ".submissions.DCCid", dccId));
            } else {

                // to build the query description
                String experimentType = "";
                if (!StringUtils.isBlank(sub.getExperimentType())) {
                    experimentType = StringUtil.indefiniteArticle(sub.getExperimentType())
                        + " " + sub.getExperimentType() + " experiment in";
                }

                String efSub = "";
                if (SubmissionHelper.getExperimentalFactorString(sub).length() > 1) {
                    efSub = " using " + SubmissionHelper.getExperimentalFactorString(sub);
                    if (efSub.contains("primer")) {
                        hasPrimer = true;
                        efSub = "";
                    }
                }

                String description = "All " + featureType
                    + " features generated by submission " + dccId
                    + ", " + experimentType + " "
                    + sub.getOrganism().getShortName() + efSub
                    + " (" + sub.getProject().getSurnamePI() + ").";
                q.setDescription(description);

                q.addView(featureType + ".primaryIdentifier");
                q.addView(featureType + ".score");
                if ("results".equals(action)) {
                    q.addView(featureType + ".scoreProtocol.name");
                    q.setOuterJoinStatus(featureType + ".scoreProtocol",
                            OuterJoinStatus.OUTER);
                }
                q.addConstraint(Constraints.eq(featureType + ".submissions.DCCid", dccId));

                if (unlocFeatures == null || !unlocFeatures.contains(featureType)) {
                    addLocationToQuery(q, featureType);
                    addEFactorToQuery(q, featureType, hasPrimer);

                } else {
                    q.addView(featureType + ".submissions.DCCid");
                    addEFactorToQuery(q, featureType, hasPrimer);
                }

             // source file
                if (sourceFile != null) {
                    q.addConstraint(Constraints.eq(featureType + ".sourceFile", sourceFile));
                }

            }
        } else if ("subEL".equals(type)) {
            // For the expression levels
            dccId = request.getParameter("submission");
            q.addConstraint(Constraints.type("Submission.features", featureType));

            String path = "Submission.features.expressionLevels";
            q.addView("Submission.features.primaryIdentifier");
            q.addView(path + ".value");
            q.addView(path + ".readCount");
            q.addView(path + ".dcpm");
            q.addView(path + ".dcpmBases");
            q.addView(path + ".transcribed");
            q.addView(path + ".predictionStatus");
            q.addView(path + ".name");

            q.addConstraint(Constraints.eq("Submission.DCCid", dccId));
        } else if ("expEL".equals(type)) {
            String eName = request.getParameter("experiment");

            q.addConstraint(Constraints.type("Experiment.submissions.features", featureType));

            String path = "Experiment.submissions.features.expressionLevels";
            q.addView("Experiment.submissions.features.primaryIdentifier");
            q.addView(path + ".value");
            q.addView(path + ".readCount");
            q.addView(path + ".dcpm");
            q.addView(path + ".dcpmBases");
            q.addView(path + ".transcribed");
            q.addView(path + ".predictionStatus");
            q.addView(path + ".name");

            q.addConstraint(Constraints.eq("Experiment.name", eName));
        } else if ("span".equals(type)) {
            @SuppressWarnings("unchecked")
            Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>> spanOverlapFullResultMap =
                 (Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>>) request
                                .getSession().getAttribute("spanOverlapFullResultMap");

            @SuppressWarnings("unchecked")
            Map<SpanUploadConstraint, String> spanConstraintMap =
                (HashMap<SpanUploadConstraint, String>)  request
                .getSession().getAttribute("spanConstraintMap");

            String spanUUIDString = request.getParameter("spanUUIDString");
            String criteria = request.getParameter("criteria");

             // Use feature pids as the value in lookup constraint
            Set<Integer> value =
                getSpanOverlapFeatures(spanUUIDString, criteria, spanOverlapFullResultMap);

            // Create a path query
            String path = "SequenceFeature";
            q.addView(path + ".primaryIdentifier");
            q.addView(path + ".chromosomeLocation.locatedOn.primaryIdentifier");
            q.addView(path + ".chromosomeLocation.start");
            q.addView(path + ".chromosomeLocation.end");
            q.addView(path + ".submissions.DCCid");
            q.addView(path + ".submissions.title");
            q.addView(path + ".organism.name");

//            q.addConstraint(Constraints.lookup(path, value, null));
            q.addConstraint(Constraints.inIds(path, value));

            String organism = getSpanOrganism(spanUUIDString, spanConstraintMap);
            Set<String> organisms = new HashSet<String>();
            organisms.add(organism);
            taxIds = getTaxonIds(organisms);
        }

        if ("results".equals(action)) {
            String qid = SessionMethods.startQueryWithTimeout(request, false, q);
            Thread.sleep(200);

            return new ForwardParameters(mapping.findForward("waiting"))
                .addParameter("qid", qid)
                .forward();
        } else if ("export".equals(action)) {
            String format = request.getParameter("format");

            Profile profile = SessionMethods.getProfile(session);
            WebResultsExecutor executor = im.getWebResultsExecutor(profile);
            PagedTable pt = new PagedTable(executor.execute(q));

            if (pt.getWebTable() instanceof WebResults) {
                ((WebResults) pt.getWebTable()).goFaster();
            }

            WebConfig webConfig = SessionMethods.getWebConfig(request);
            TableExporterFactory factory = new TableExporterFactory(webConfig);

            TableHttpExporter exporter = factory.getExporter(format);

            if (exporter == null) {
                throw new RuntimeException("unknown export format: " + format);
            }

            TableExportForm exportForm = new TableExportForm();
            // Ref to StandardHttpExporter
            exportForm.setIncludeHeaders(true);

            if ("gff3".equals(format)) {
                boolean makeUcscCompatible = readUcscParameter(request);

                exportForm = new GFF3ExportForm();
                exportForm.setDoGzip(doGzip);
                ((GFF3ExportForm) exportForm).setMakeUcscCompatible(makeUcscCompatible);
                ((GFF3ExportForm) exportForm).setOrganisms(taxIds);
            }

            if ("sequence".equals(format)) {
                exportForm = new SequenceExportForm();
                exportForm.setDoGzip(doGzip);
            }

            exporter.export(pt, request, response, exportForm, null, null);

            // If null is returned then no forwarding is performed and
            // to the output is not flushed any jsp output, so user
            // will get only required export data
            return null;

        } else if ("list".equals(action)) {
            // need to select just id of featureType to create list
            //q.clearView();
            q.addView(featureType + ".id");
            dccId = request.getParameter("submission");
            q.addConstraint(Constraints.eq(featureType + ".submissions.DCCid", dccId));
            if (request.getParameter("file") != null) {
                sourceFile = request.getParameter("file").replace(" ", "+");
                q.addConstraint(Constraints.eq(featureType + ".sourceFile", sourceFile));
            }

            Profile profile = SessionMethods.getProfile(session);

            //BagQueryRunner bagQueryRunner = im.getBagQueryRunner();

            String bagName = (dccId != null ? "submission_" + dccId : experimentName)
                + "_" + featureType + "_features";
            bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(), bagName);
            BagHelper.createBagFromPathQuery(q, bagName, q.getDescription(), featureType, profile,
                    im);
            ForwardParameters forwardParameters =
                new ForwardParameters(mapping.findForward("bagDetails"));
            return forwardParameters.addParameter("bagName", bagName).forward();
        }
        return null;
    }

    /**
     * @param project
     * @return
     */
    private String getRootFeature(String project) {
        String rootChoice = null;
        if ("Waterston".equalsIgnoreCase(project)) {
            rootChoice = "Transcript";
        } else {
            rootChoice = "Gene";
        }
        return rootChoice;
    }

    private boolean readUcscParameter(HttpServletRequest request) {
        String ucscParameter = request.getParameter("UCSC");
        if (ucscParameter != null) {
            return true;
        }
        return false;
    }

    /**
     *
     */
    private Map<String, LinkedList<String>>
    populateGFFRelationships(Map<String, LinkedList<String>> m) {

        final LinkedList<String> gffPiano = new LinkedList<String>();
        gffPiano.add("Gene");
        gffPiano.add("Gene.UTRs");
        gffPiano.add("Gene.transcripts");
//        gffPiano.add("Gene.transcripts.mRNA");

        final LinkedList<String> gffCelniker = new LinkedList<String>();

        gffCelniker.add("Gene.transcripts.CDSs");
//        gffCelniker.add("Gene.UTRs.transcripts.PolyASites");
        gffCelniker.add("Gene.transcripts.startCodon");
        gffCelniker.add("Gene.transcripts.stopCodon");
        gffCelniker.add("Gene.transcripts.CDSs");
        gffCelniker.add("Gene.transcripts.fivePrimeUTR");
        gffCelniker.add("Gene.transcripts.threePrimeUTR");

        final LinkedList<String> gffWaterston = new LinkedList<String>();
        gffWaterston.add("Transcript.TSSs");
        gffWaterston.add("Transcript.SL1AcceptorSites");
        gffWaterston.add("Transcript.SL2AcceptorSites");
        gffWaterston.add("Transcript.transcriptionEndSites");
        gffWaterston.add("Transcript.polyASites");
        gffWaterston.add("Transcript.introns");
        gffWaterston.add("Transcript.exons");

        m.put("Piano", gffPiano);
        m.put("Celniker", gffCelniker);
        m.put("Waterston", gffWaterston);

        return m;
    }

    /**
     * @param q
     * @param featureType
     * @param hasPrimer
     */
    private void addEFactorToQuery(PathQuery q, String featureType,
            boolean hasPrimer) {
        if (!hasPrimer) {
            q.addView(featureType + ".submissions.experimentalFactors.type");
            q.addView(featureType + ".submissions.experimentalFactors.name");
            q.setOuterJoinStatus(featureType + ".submissions.experimentalFactors",
                    OuterJoinStatus.OUTER);
        }
    }


    /**
     * @param q
     * @param featureType the feature type
     */
    private void addLocationToQuery(PathQuery q, String featureType) {
        q.addView(featureType + ".chromosome.primaryIdentifier");
        q.addView(featureType + ".chromosomeLocation.start");
        q.addView(featureType + ".chromosomeLocation.end");
        q.addView(featureType + ".chromosomeLocation.strand");
        q.addView(featureType + ".submissions.DCCid");

        q.addOrderBy(featureType + ".chromosome.primaryIdentifier", OrderDirection.ASC);
        q.addOrderBy(featureType + ".chromosomeLocation.start", OrderDirection.ASC);
    }

    private String getFactors(DisplayExperiment exp) {
        String ef = "";
        if (exp.getFactorTypes().size() == 1) {
            ef = "Experimental factor is the " + StringUtil.prettyList(exp.getFactorTypes())
                + " used.";
        }
        if (exp.getFactorTypes().size() > 1) {
            ef = "Experimental factors are the " + StringUtil.prettyList(exp.getFactorTypes())
                + " used.";
        }
        return ef;
    }

    private Set<Integer> getTaxonIds(Set<String> organisms) {
        Set<Integer> taxIds = new HashSet<Integer>();
        for (String name : organisms) {
            if ("D. melanogaster".equalsIgnoreCase(name)) {
                taxIds.add(7227);
            }
            if ("C. elegans".equalsIgnoreCase(name)) {
                taxIds.add(6239);
            }
        }
        return taxIds;
    }

    /**
     * To export all overlap features
     * @param spanUUIDString
     * @param criteria could be "all" or a span string
     * @param request HttpServletRequest
     * @return comma separated feature pids as a string
     */
    private Set<Integer> getSpanOverlapFeatures(String spanUUIDString, String criteria,
            Map<String, Map<GenomicRegion, List<SpanQueryResultRow>>> spanOverlapFullResultMap) {

        Set<Integer> featureSet = new HashSet<Integer>();
        Map<GenomicRegion, List<SpanQueryResultRow>> featureMap = spanOverlapFullResultMap
                .get(spanUUIDString);

        if ("all".equals(criteria)) {
            for (List<SpanQueryResultRow> l : featureMap.values()) {
                if (l != null) {
                    for (SpanQueryResultRow r : l) {
                        featureSet.add(r.getFeatureId());
                    }
                }
            }

        } else {
            GenomicRegion spanToExport = new GenomicRegion();
            String[] temp = criteria.split(":");
            spanToExport.setChr(temp[0]);
            temp = temp[1].split("\\.\\.");
            spanToExport.setStart(Integer.parseInt(temp[0]));
            spanToExport.setEnd(Integer.parseInt(temp[1]));
            for (SpanQueryResultRow r : featureMap.get(spanToExport)) {
                featureSet.add(r.getFeatureId());
            }
        }

        return featureSet;
    }

    private String getSpanOrganism(String spanUUIDString,
            Map<SpanUploadConstraint, String> spanConstraintMap) {

        for (Entry<SpanUploadConstraint, String> e : spanConstraintMap.entrySet()) {
            if (e.getValue().equals(spanUUIDString)) {
                return e.getKey().getSpanOrgName();
            }
        }

        return null;
    }
}

