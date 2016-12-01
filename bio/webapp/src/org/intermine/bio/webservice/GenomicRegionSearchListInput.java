package org.intermine.bio.webservice;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.bio.web.logic.GenomicRegionSearchQueryRunner;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.logic.RegionParseException;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.lists.ListInput;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Alex
 *
 */
public class GenomicRegionSearchListInput extends ListInput {

    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchListInput.class);

    private final InterMineAPI api;
    private final GenomicRegionSearchInfo info;

    /**
     * A representation of a request to a region based web service. It knows how
     * to parse and validate its own input.
     * @param request The incoming web service request.
     * @param bagManager A bag manager.
     * @param profile The current user.
     * @param im The InterMine API
     * @throws Exception If the region request is malformed.
     */
    public GenomicRegionSearchListInput(HttpServletRequest request,
            BagManager bagManager, Profile profile, InterMineAPI im)
        throws Exception {
        super(request, bagManager, profile);
        api = im;
        info = parseRegionRequest();
    }

    private GenomicRegionSearchInfo parseRegionRequest() throws Exception {
        String input = "";
        if ("application/x-www-form-urlencoded".equals(request.getContentType())
                || "GET".equalsIgnoreCase(request.getMethod())) {
            input = request.getParameter("query");
        } else {
            input = IOUtils.toString(request.getInputStream());
        }
        JSONObject jsonRequest = new JSONObject(input);
        GenomicRegionSearchInfo parsed = new GenomicRegionSearchInfo();
        parsed.setOrganism(jsonRequest.getString("organism"));
        if (!jsonRequest.isNull("isInterbase")) {
            parsed.setInterbase(jsonRequest.getBoolean("isInterbase"));
        }
        if (!jsonRequest.isNull("extension")) {
            parsed.setExtension(jsonRequest.optInt("extension", 0));
        }
        JSONArray fts = jsonRequest.getJSONArray("featureTypes");
        int noOfTypes = fts.length();
        List<String> featureTypes = new ArrayList<String>();
        for (int i = 0; i < noOfTypes; i++) {
            featureTypes.add(fts.getString(i));
        }
        parsed.setFeatureTypes(featureTypes);

        JSONArray regs = jsonRequest.getJSONArray("regions");
        int noOfRegs = regs.length();
        List<String> regions = new ArrayList<String>();
        for (int i = 0; i < noOfRegs; i++) {
            regions.add(regs.getString(i));
        }
        parsed.setRegions(regions);

        parsed.setStrandSpecific(jsonRequest.getBoolean("strandSpecific"));
        
        return parsed;
    }

    @Override
    protected String produceName() {
        String name = request.getParameter("listName");
        if (!StringUtils.isBlank(name)) {
            return name;
        } else {
            return super.produceName();
        }
    }

    /**
     * @return region search results
     */
    public GenomicRegionSearchInfo getSearchInfo() {
        return info;
    }

    /**
     *
     * @author Alex
     *
     */
    public class GenomicRegionSearchInfo {
        
        private final String sequenceFeature = "org.intermine.model.bio.SequenceFeature";
        private String organism;
        private Set<String> featureTypes;
        private Set<ClassDescriptor> featureCds;
        private List<String> regions;
        private int extension = 0;
        private boolean isInterbase = false;
        private Set<String> invalidSpans = new HashSet<String>();
        private boolean strandSpecific;

        /**
         *
         * @return set of invalid spans
         */
        public Set<String> getInvalidSpans() {
            return invalidSpans;
        }

        /**
         * @return organism
         */
        public String getOrganism() {
            return organism;
        }

        /**
         *
         * @param organism organism
         */
        public void setOrganism(String organism) {
            this.organism = organism;
        }

        /**
         * @return strandSpecific
         */
        public boolean getStrandSpecific() {
            return strandSpecific;
        }

        /**
         * @param strandSpecific
         */
        public void setStrandSpecific(boolean strandSpecific) {
            this.strandSpecific = strandSpecific;
        }

        /**
         *
         * @return featuretypes
         */
        public Set<String> getFeatureTypes() {
            return Collections.unmodifiableSet(featureTypes);
        }

        /**
         * Set the feature types for this request. Immediately parses the class
         * names to ClassDescriptors and fails as soon as possible.
         *
         * @param featureTypes A collection of feature type names.
         * @throws BadRequestException if the feature types are not mappable to classes, and if
         *                             those classes are not Sequence Features.
         */
        public void setFeatureTypes(Collection<String> featureTypes) {
            this.featureTypes = new HashSet<String>(featureTypes);
            this.featureCds = new HashSet<ClassDescriptor>();

            Set<String> badTypes = new HashSet<String>();
            Model model = api.getModel();
            ClassDescriptor sfCd = model.getClassDescriptorByName(sequenceFeature);
            for (String f : this.featureTypes) {
                ClassDescriptor cld = model.getClassDescriptorByName(f);
                if (cld == null) {
                    badTypes.add(f);
                } else {
                    try {
                        if (!sequenceFeature.equals(f) && !sfCd.getUnqualifiedName().equals(f)
                                && !ClassDescriptor.findSuperClassNames(model, f)
                                    .contains(sequenceFeature)) {
                            throw new BadRequestException(f + " is not a " + sequenceFeature);
                        }
                    } catch (MetaDataException e) {
                        // This should never happen.
                        throw new ServiceException(e);
                    }
                    featureCds.add(cld);
                    for (ClassDescriptor subCld : model.getAllSubs(cld)) {
                        featureCds.add(subCld);
                    }
                }
            }
            if (!badTypes.isEmpty()) {
                throw new BadRequestException("The following feature types are not "
                        + "valid feature class names: " + badTypes);
            }
        }

        /**
         * Returns an unmodifiable set of the classdescriptors corresponding to the
         * feature types in this query.
         * @return feature class descriptors
         */
        public Set<ClassDescriptor> getFeatureCds() {
            return Collections.unmodifiableSet(featureCds);
        }

        /**
         * @return an unmodifiable set of the classes that the Class-Descriptors
         * in this query represent.
         */
        public Set<Class<?>> getFeatureClasses() {
            Set<Class<?>> ftSet = new HashSet<Class<?>>();
            for (ClassDescriptor cld : getFeatureCds()) {
                ftSet.add(cld.getType());
            }
            return Collections.unmodifiableSet(ftSet);
        }

        /**
         *
         * @return regions
         */
        public List<String> getRegions() {
            return regions;
        }

        /**
         *
         * @param regions regions
         */
        public void setRegions(List<String> regions) {
            this.regions = regions;
        }

        /**
         *
         * @return list of valid regions
         */
        public List<GenomicRegion> getGenomicRegions() {
            Set<String> spans = new HashSet<String>(getRegions());
            List<GenomicRegion> newRegions = new ArrayList<GenomicRegion>();
            Map<String, ChromosomeInfo> chromsForOrg
                = GenomicRegionSearchQueryRunner.getChromosomeInfo(api).get(getOrganism());
            for (String span : spans) {
                try {
                    newRegions.add(GenomicRegionSearchUtil.parseRegion(
                            span, isInterbase(), chromsForOrg));
                } catch (RegionParseException e) {
                    invalidSpans.add(span + "; " + e.getMessage());
                }
            }
            return newRegions;
        }

        /**
         *
         * @return extension
         */
        public int getExtension() {
            return extension;
        }

        /**
         *
         * @param extension extension
         */
        public void setExtension(int extension) {
            this.extension = extension;
        }

        /**
         *
         * @return true if interbase
         */
        public boolean isInterbase() {
            return isInterbase;
        }

        /**
         *
         * @param isInterbase true if interbase
         */
        public void setInterbase(boolean isInterbase) {
            this.isInterbase = isInterbase;
        }

        /**
         *
         * @return region search constraint
         */
        public GenomicRegionSearchConstraint asSearchConstraint() {
            GenomicRegionSearchConstraint grsc = new GenomicRegionSearchConstraint();
            grsc.setOrgName(organism);
            grsc.setFeatureTypes(getFeatureClasses());
            grsc.setGenomicRegionList(getGenomicRegions());
            grsc.setExtendedRegionSize(extension);
            return grsc;
        }

    }


}
