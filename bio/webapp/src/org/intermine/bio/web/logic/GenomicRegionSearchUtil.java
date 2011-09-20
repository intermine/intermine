package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.tools.ant.BuildException;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.RegionParseException;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.session.SessionMethods;

/**
 * This utility class instance a GenomicRegionSearchService object based on mine's setting.
 *
 * @author Fengyuan Hu
 */
public final class GenomicRegionSearchUtil
{
    private GenomicRegionSearchUtil() {

    }

    /**
     * Generate GenomicRegionSearchService object by using Java reflection
     *
     * @param request HttpServletRequest
     * @return the current mine's GenomicRegionSearchService object
     */
    public static GenomicRegionSearchService getGenomicRegionSearchService(
            HttpServletRequest request) {

        // Get service class name from web.properties
        String serviceClassName = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.service");

        GenomicRegionSearchService grsService = null;
        if (serviceClassName == null || "".equals(serviceClassName)) {
            grsService = new GenomicRegionSearchService();
            grsService.init(request);
        } else { // reflection
            Class<?> serviceClass;
            try {
                serviceClass = Class.forName(serviceClassName);
            } catch (ClassNotFoundException e) {
                throw new BuildException("Class not found for " + serviceClassName, e);
            }
            Class<?> [] types = new Class[] {HttpServletRequest.class};
            Object [] args = new Object[] {request};
            try {
                grsService = (GenomicRegionSearchService) serviceClass
                        .getConstructor(types).newInstance(args);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return grsService;
    }
    
    /**
     * To extend genomic region
     * @param gr GenomicRegion
     * @return GenomicRegion
     */
    private static GenomicRegion extendGenomicRegion(GenomicRegion gr, int extension, 
            Map<String, ChromosomeInfo> chromInfo) {

        gr.setExtendedRegionSize(extension);

        int max = chromInfo.get(gr.getChr().toLowerCase()).getChrLength();
        int min = 1;

        int start = gr.getStart();
        int end = gr.getEnd();

        int extendedStart = start - extension;
        int extendedEnd = end + extension;

        if (extendedStart < min) {
            gr.setExtendedStart(min);
        } else {
            gr.setExtendedStart(extendedStart);
        }

        if (extendedEnd > max) {
            gr.setExtendedEnd(max);
        } else {
            gr.setExtendedEnd(extendedEnd);
        }

        return gr;
    }
    
    private static final Pattern dotDot = Pattern.compile("[^:]+: ?\\d+\\.{2}\\d+$"); // "chr:start..end"
    private static final Pattern bed = Pattern.compile("[^\\t\\s]+\\t\\d+\\t\\d+"); // "chr(tab)start(tab)end"
    private static final Pattern dash = Pattern.compile("[^:]+: ?\\d+\\-\\d+$"); // "chr:start-end"
    private static final Pattern singlePos = Pattern.compile("[^:]+: ?\\d+$"); // "chr:singlePosition" - [^:]+:[\d]+$
    
    public static GenomicRegion parseRegion(String span, boolean isInterbase, Map<String, ChromosomeInfo> chromsForOrg)
            throws RegionParseException {
        
        String[] parts = parseDotDotSpan(span);
        if (parts == null) {
            parts = parseBedSpan(span);
        }
        if (parts == null) {
            parts = parseDashSpan(span);
        }
        if (parts == null) {
            parts = parseSinglePositionSpan(span);
        }
        if (parts == null) {
            throw new RegionParseException("Span format not recognised");
        }
        
        GenomicRegion region = new GenomicRegion();
        region.setChr(parts[0].trim());
        int start = Integer.valueOf(parts[1].trim()), 
                end = Integer.valueOf(parts[2].trim());
        if (isInterbase) {
            region.setStart(start + 1);
        } else {
            region.setStart(start);
        }
        region.setEnd(end);

        ChromosomeInfo ci = getChromosomeInfo(chromsForOrg, region.getChr());

        if ((region.getStart() >= 1 && region.getStart() <= ci.getChrLength()) 
                && (region.getEnd() >= 1 && region.getEnd() <= ci.getChrLength())) {
            if (region.getStart() > region.getEnd()) {
                // Swap them around.
                int oldStart = region.getStart(), oldEnd = region.getEnd();
                region.setStart(oldStart);
                region.setEnd(oldEnd);
            }
            region.setChr(ci.getChrPID());
        } else {
            throw new RegionParseException("start and/or end values are out of bounds "
                    + "(0 - " + ci.getChrLength() + ")");
        }
        
        return region;
    }
    
    private static ChromosomeInfo getChromosomeInfo(Map<String, ChromosomeInfo> chromsForOrg, String chr) throws RegionParseException {
        chr = chr.toLowerCase();
        if (chromsForOrg.containsKey(chr)) {
            return chromsForOrg.get(chr);
        } else {
            if (chr.startsWith("chr")) {
                if (chromsForOrg.containsKey(chr.substring(3))) {
                    return chromsForOrg.get(chr.substring(3));
                } 
            } 
        }
        throw new RegionParseException(chr + " does not match any chromosome in this organism");
    }

    private static String[] parseDotDotSpan(String span) {
        Matcher m = dotDot.matcher(span);
        if (m.find()) {
            String[] chr = new String[]{span.split(":")[0]};
            return (String[]) ArrayUtils.addAll(chr, span.split(":")[1].split("\\.{2}"));
        } else {
            return null;
        }
    }

    private static String[] parseBedSpan(String span) {
        Matcher m = bed.matcher(span);
        if (m.find()) {
            return span.split("\t");
        } else {
            return null;
        }
    }

    private static String[] parseDashSpan(String span) {
        Matcher m = dash.matcher(span);
        if (m.find()) {
            String[] chr = new String[]{span.split(":")[0]};
            return (String[]) ArrayUtils.addAll(chr, span.split(":")[1].split("-"));
        } else {
            return null;
        }
    }

    private static String[] parseSinglePositionSpan(String span) {
        Matcher m = singlePos.matcher(span);
        String[] ret = new String[3];
        if (m.find()) {
            ret[0] = span.split(":")[0];
            ret[1] = (span.split(":"))[1];
            ret[2] = ret[1];
            return ret;
        } else {
            return null;
        }
    }

    public static Map<GenomicRegion, Query> createQueryList(
            Collection<GenomicRegion> genomicRegions, 
            int extension, 
            Map<String, ChromosomeInfo> chromInfo, 
            String organismName, 
            Set<Class<?>> featureTypes) {
        return createRegionQueries(genomicRegions, extension, chromInfo, organismName,
                featureTypes, false);
    }
    
    public static Map<GenomicRegion, Query> createRegionListQueries(
            Collection<GenomicRegion> genomicRegions, 
            int extension, 
            Map<String, ChromosomeInfo> chromInfo, 
            String organismName, 
            Set<Class<?>> featureTypes) {
        return createRegionQueries(genomicRegions, extension, chromInfo, organismName,
                featureTypes, true);
    }
    
    private static Map<GenomicRegion, Query> createRegionQueries(Collection<GenomicRegion> genomicRegions, 
                int extension, Map<String, ChromosomeInfo> chromInfo, String organismName, 
                Set<Class<?>> featureTypes, boolean idOnly) {

       Map<GenomicRegion, Query> queryMap = new LinkedHashMap<GenomicRegion, Query>();

       for (GenomicRegion aSpan : genomicRegions) {

           aSpan = extendGenomicRegion(aSpan, extension, chromInfo);

           Query q = new Query();
           q.setDistinct(true);

           String chrPID = aSpan.getChr();
           Integer start = aSpan.getExtendedStart();
           Integer end = aSpan.getExtendedEnd();

           QueryClass qcOrg = new QueryClass(Organism.class);
           QueryClass qcChr = new QueryClass(Chromosome.class);
           QueryClass qcFeature = new QueryClass(SequenceFeature.class);
           QueryClass qcLoc = new QueryClass(Location.class);

           QueryField qfOrgName = new QueryField(qcOrg, "shortName");
           QueryField qfFeatureId = new QueryField(qcFeature, "id");
           QueryField qfFeaturePID = new QueryField(qcFeature, "primaryIdentifier");
           QueryField qfFeatureSymbol = new QueryField(qcFeature, "symbol");
           QueryField qfFeatureClass = new QueryField(qcFeature, "class");
           QueryField qfChr = new QueryField(qcChr, "primaryIdentifier");
           QueryField qfLocStart = new QueryField(qcLoc, "start");
           QueryField qfLocEnd = new QueryField(qcLoc, "end");

           q.addToSelect(qfFeatureId);
           q.addFrom(qcFeature);
           q.addFrom(qcChr);
           q.addFrom(qcOrg);
           q.addFrom(qcLoc);
           if (!idOnly) {
               q.addToSelect(qfFeaturePID);
               q.addToSelect(qfFeatureSymbol);
               q.addToSelect(qfFeatureClass);
               q.addToSelect(qfChr);
               q.addToSelect(qfLocStart);
               q.addToSelect(qfLocEnd);
               q.addToOrderBy(qfLocStart, "ascending");
           }

           ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

           q.setConstraint(constraints);

           // SequenceFeature.organism = Organism
           QueryObjectReference organism = new QueryObjectReference(qcFeature,
                   "organism");
           ContainsConstraint ccOrg = new ContainsConstraint(organism,
                   ConstraintOp.CONTAINS, qcOrg);
           constraints.addConstraint(ccOrg);

           // Organism.name = orgName
           SimpleConstraint scOrg = new SimpleConstraint(qfOrgName,
                   ConstraintOp.EQUALS, new QueryValue(organismName));
           constraints.addConstraint(scOrg);

           // Location.feature = SequenceFeature
           QueryObjectReference locSubject = new QueryObjectReference(qcLoc,
                   "feature");
           ContainsConstraint ccLocSubject = new ContainsConstraint(
                   locSubject, ConstraintOp.CONTAINS, qcFeature);
           constraints.addConstraint(ccLocSubject);

           // Location.locatedOn = Chromosome
           QueryObjectReference locObject = new QueryObjectReference(qcLoc,
                   "locatedOn");
           ContainsConstraint ccLocObject = new ContainsConstraint(locObject,
                   ConstraintOp.CONTAINS, qcChr);
           constraints.addConstraint(ccLocObject);

           // Chromosome.primaryIdentifier = chrPID
           SimpleConstraint scChr = new SimpleConstraint(qfChr,
                   ConstraintOp.EQUALS, new QueryValue(chrPID));
           constraints.addConstraint(scChr);

           // SequenceFeature.class in a list
           constraints.addConstraint(new BagConstraint(qfFeatureClass,
                   ConstraintOp.IN, featureTypes));

           OverlapRange overlapInput = new OverlapRange(new QueryValue(start),
                   new QueryValue(end), locObject);
           OverlapRange overlapFeature = new OverlapRange(new QueryField(
                   qcLoc, "start"), new QueryField(qcLoc, "end"), locObject);
           OverlapConstraint oc = new OverlapConstraint(overlapInput,
                   ConstraintOp.OVERLAPS, overlapFeature);
           constraints.addConstraint(oc);

           queryMap.put(aSpan, q);
       }

       return queryMap;
   }

}
