package org.intermine.bio.webservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.bio.web.logic.GenomicRegionSearchQueryRunner;
import org.intermine.bio.web.logic.GenomicRegionSearchUtil;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.bio.web.model.RegionParseException;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.lists.ListInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GenomicRegionSearchListInput extends ListInput {

    private final InterMineAPI api;
    private final GenomicRegionSearchInfo info;
    
    public GenomicRegionSearchListInput(HttpServletRequest request,
            BagManager bagManager, InterMineAPI im) throws JSONException {
        super(request, bagManager);
        api = im;
        info = parseRegionRequest();
    }

    private GenomicRegionSearchInfo parseRegionRequest() throws JSONException {
        JSONObject jsonRequest = new JSONObject(request.getParameter("query"));
        GenomicRegionSearchInfo parsed = new GenomicRegionSearchInfo();
        parsed.setOrganism(jsonRequest.getString("organism"));
        if (!jsonRequest.isNull("isInterbase")) {
            parsed.setInterbase(jsonRequest.getBoolean("isInterbase"));
        }
        if (!jsonRequest.isNull("extension")) {
            parsed.setExtension(jsonRequest.getInt("extension"));
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
        
        return parsed;
    }
    
    public GenomicRegionSearchInfo getSearchInfo() {
        return info;
    }

    public class GenomicRegionSearchInfo {
        private String organism;
        private List<String> featureTypes;
        private List<String> regions;
        private int extension = 0;
        private boolean isInterbase = false;
        private Set<String> invalidSpans = new HashSet<String>(); 
        
        public Set<String> getInvalidSpans() {
            return invalidSpans;
        }
        public String getOrganism() {
            return organism;
        }
        public void setOrganism(String organism) {
            this.organism = organism;
        }
        public List<String> getFeatureTypes() {
            return featureTypes;
        }
        public void setFeatureTypes(List<String> featureTypes) {
            this.featureTypes = featureTypes;
        }
        
        public Set<ClassDescriptor> getFeatureCds() {
            Set<ClassDescriptor> fcdSet = new HashSet<ClassDescriptor>();
            Model model = api.getModel();
            for (String f : getFeatureTypes()) {
                ClassDescriptor cld = model.getClassDescriptorByName(f);
                fcdSet.add(cld);
                for (ClassDescriptor subCld : model.getAllSubs(cld)) {
                    fcdSet.add(subCld);
                }
            }
            return fcdSet;
        }

        public Set<Class<?>> getFeatureClasses() {
            Set<Class<?>> ftSet = new HashSet<Class<?>>();
            for (ClassDescriptor cld : getFeatureCds()) {
                ftSet.add(cld.getType());
            }
            return ftSet;
        }

        public List<String> getRegions() {
            return regions;
        }

        public void setRegions(List<String> regions) {
            this.regions = regions;
        }

        public List<GenomicRegion> getGenomicRegions() {
            Set<String> spans = new HashSet<String>(getRegions());
            List<GenomicRegion> regions = new ArrayList<GenomicRegion>();
            Map<String, ChromosomeInfo> chromsForOrg 
                = GenomicRegionSearchQueryRunner.getChromosomeInfo(
                        api, SessionMethods.getProfile(request.getSession())).get(getOrganism());
            for (String span : spans) {
                try {
                    regions.add(GenomicRegionSearchUtil.parseRegion(span, isInterbase(), chromsForOrg));
                } catch (RegionParseException e) {
                    invalidSpans.add(span + "; " + e.getMessage());
                }
            }
            return regions;
        }
        
        public int getExtension() {
            return extension;
        }
        public void setExtension(int extension) {
            this.extension = extension;
        }
        public boolean isInterbase() {
            return isInterbase;
        }
        public void setInterbase(boolean isInterbase) {
            this.isInterbase = isInterbase;
        }
        
        public GenomicRegionSearchConstraint asSearchConstraint() {
            GenomicRegionSearchConstraint grsc = new GenomicRegionSearchConstraint();
            grsc.setOrgName(organism);
            grsc.setFeatureTypes(getFeatureClasses());
            grsc.setGenomicRegionList(getGenomicRegions());
            grsc.setExtededRegionSize(extension);
            return grsc;
        }
        
    }


}
