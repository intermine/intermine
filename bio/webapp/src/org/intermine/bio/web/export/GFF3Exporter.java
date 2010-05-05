package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.bio.util.GFF3Util;
import org.intermine.model.bio.LocatedSequenceFeature;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.IntPresentSet;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;

/**
 * Exports LocatedSequenceFeature objects in GFF3 format.
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 */
public class GFF3Exporter implements Exporter
{
    private static final Logger LOG = Logger.getLogger(GFF3Exporter.class);

//    public static final Set<String> GFF_FIELDS = Collections
//        .unmodifiableSet(new HashSet<String>(Arrays.asList("chromosome.primaryIdentifier",
//                        "chromosomeLocation.start", "chromosomeLocation.end",
//                        "chromosomeLocation.strand", "primaryIdentifier", "score")));

    public static final Set<String> GFF_FIELDS = Collections
    .unmodifiableSet(new HashSet<String>(Arrays.asList("primaryIdentifier",
                    "start", "end", "strand", "score")));
    PrintWriter out;
    private List<Integer> featureIndexes;
    private Map<String, String> soClassNames;
    private int writtenResultsCount = 0;
    private boolean headerPrinted = false;
    private IntPresentSet exportedIds = new IntPresentSet();
    private List<String> attributesNames;
    private String sourceName;
    private Set<Integer> organisms;

    /**
     * Constructor.
     * @param out output stream
     * @param indexes index of column with exported sequence
     * @param soClassNames mapping
     * @param attributesNames names of attributes that are printed in record,
     *  they are names of columns in results table, they are in the same order
     *  as corresponding columns in results table
     * @param sourceName name of Mine to put in GFF source column
     */
    public GFF3Exporter(PrintWriter out, List<Integer> indexes, Map<String, String> soClassNames,
            List<String> attributesNames, String sourceName, Set<Integer> organisms) {
        this.out = out;
        this.featureIndexes = indexes;
        this.soClassNames = soClassNames;
        this.attributesNames = attributesNames;
        this.sourceName = sourceName;
        this.organisms = organisms;
    }
    
    
    /**
     * to read genome and project versions
     * @return header further info about versions
     */
    
    private String getHeaderParts()
    {
        StringBuffer header = new StringBuffer();
        Properties props = PropertiesUtil.getProperties();

        if (organisms != null) {
            for (Integer taxId : organisms){
                if (taxId == 7227){
                    String fV = props.getProperty("genomeVersion.fly");
                    if (fV != null && fV.length() > 0){
                        header.append("\n##species http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=7227");
                        header.append("\n##genome-build FlyBase r"+ fV + "(drosophila)");
                    }
                }
            }
            for (Integer taxId : organisms){
                if (taxId == 6239){
                    String wV = props.getProperty("genomeVersion.worm");
                    if (wV != null && wV.length() > 0) {
                        header.append("\n##species http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=6239");
                        header.append("\n##genome-build WormBase r"+ wV + "(worm)");
                    }
                }
            }

            // display only if organism is set
            String mV = props.getProperty("project.releaseVersion");
            if (mV != null && mV.length() > 0) {
                header.append("\n#" + this.sourceName + " " + mV);
                header.append("\n# #index-subfeatures 1");
            }

        }

        return header.toString();
    }

    private String getHeader() {
        return "##gff-version 3" + getHeaderParts();
    }

    
    /**
     * {@inheritDoc}
     */
    
        
    public void export(Iterator<? extends List<ResultElement>> resultIt) {
        if (featureIndexes.size() == 0) {
            throw new ExportException("No columns with sequence");
        }
        try {
            while (resultIt.hasNext()) {
                List<ResultElement> row = resultIt.next();
                exportRow(row);
            }
            finishLastRow();
            out.flush();
        } catch (Exception ex) {
            throw new ExportException("Export failed", ex);
        }
    }
    
    /* State for the exportRow method, to allow several rows to be merged. */
    private Map<String, Integer> attributeVersions = new HashMap<String, Integer>();
    private Integer lastLsfId = null;
    private LocatedSequenceFeature lastLsf = null;
    private Map<String, List<String>> attributes = null;
    private Map<String, Set<Integer>> seenAttributes = new HashMap<String, Set<Integer>>();

    private void exportRow(List<ResultElement> row)
    throws ObjectStoreException,
    IllegalAccessException {

        List<ResultElement> elWithObject = getResultElements(row);
//        ResultElement elWithObject = getResultElement(row);
    if (elWithObject == null) {
        return;
    }
    
    // loop through all the objects in a row
    for (ResultElement re : elWithObject ){
    LocatedSequenceFeature lsf = (LocatedSequenceFeature) re.getObject();
     // LOG.info("GFFrePath: " + re.getPath());

boolean isCollection = re.getPath().containsCollections();

    if (exportedIds.contains(lsf.getId()) && !(lsf.getId().equals(lastLsfId))) {
       continue;
    }

    if ((lastLsfId != null) && !(lsf.getId().equals(lastLsfId))) {
        makeRecord();
    }

    if (lastLsfId == null) {
        attributes = new LinkedHashMap<String, List<String>>();
    }

    for (int i = 0; i < row.size(); i++) {
        ResultElement el = row.get(i);


        // checks for assigning attributes
        if (isCollection && !el.getPath().containsCollections()){
            // LOG.info("P1: "+ el.getType() + "|"+ el.getPath().getLastClassDescriptor().getUnqualifiedName()+"<>"+isCollection +"|"+ el.getPath().containsCollections());
            // one is collection, the other is not: do not show
            continue;
        }
        if (!isCollection && el.getPath().containsCollections() 
                && soClassNames.containsKey(el.getType())){
            // show attributes only if they are not linked to features (they will be displayed with the relevant one, see below)
            // LOG.info("P2: "+ el.getType() + "|"+ el.getPath().getLastClassDescriptor().getUnqualifiedName()+"<>"+isCollection +"|"+ el.getPath().containsCollections());
            continue;
        }
        // both are collections: show only if they concord.
        if (isCollection && el.getPath().containsCollections() 
                && !re.getPath().equals(el.getPath())){
            // LOG.info("P3: "+ el.getType() + "|"+ el.getPath().getLastClassDescriptor().getUnqualifiedName()+"<>"+isCollection +"|"+ el.getPath().containsCollections());
                continue;
        }        

        // LOG.info("PP: "+ el.getType() + "|"+ el.getPath().getLastClassDescriptor().getUnqualifiedName()+"<>"+isCollection +"|"+ el.getPath().containsCollections());

        // usa gff list
//        if (el != null && !attributesNames.get(i).contains("primaryIdentifier")) {
        if (el != null) {
            String attributeName = trimAttribute(attributesNames.get(i));
            checkAttribute(el, attributeName);
        }
    }
    lastLsfId = lsf.getId();
    lastLsf = lsf;
    }
}


    private String trimAttribute(String attribute){
        if (!attribute.contains(".")){
            return attribute;
        }
        String plainAttribute = attribute.substring(attribute.lastIndexOf('.')+1);
        return plainAttribute;
    }
    
    /**
     * 
     */
    private void makeRecord() {

        GFF3Record gff3Record = GFF3Util.makeGFF3Record(lastLsf, soClassNames, sourceName,
                attributes);

        if (gff3Record != null) {
            // have a chromosome ref and chromosomeLocation ref
            if (!headerPrinted) {
                out.println(getHeader());
                headerPrinted = true;
            }

            out.println(gff3Record.toGFF3());
            exportedIds.add(lastLsf.getId());
            writtenResultsCount++;
        }
        lastLsfId = null;
        attributeVersions.clear();
        seenAttributes.clear();
    }


    /**
     * @param el
     * @param attributeName
     */
    private void checkAttribute(ResultElement el, String attributeName) {
        if (!GFF_FIELDS.contains(attributeName)) {
            Set<Integer> seenAttributeValues = seenAttributes.get(attributeName);
            if (seenAttributeValues == null) {
                seenAttributeValues = new HashSet<Integer>();
                seenAttributes.put(attributeName, seenAttributeValues);
            }
            if (!seenAttributeValues.contains(el.getId())) {
                seenAttributeValues.add(el.getId());
                Integer version = attributeVersions.get(attributeName);
                if (version == null) {
                    version = new Integer(1);
                    attributes.put(attributeName, formatElementValue(el));
                } else {
                    attributes.put(attributeName + version, formatElementValue(el));
                }
                attributeVersions.put(attributeName, new Integer(version.intValue() + 1));
            }
        }
    }
 


    private List<String> formatElementValue(ResultElement el) {
        List<String> ret = new ArrayList<String>();
        String s;
        if (el == null) {
            s = "-";
        } else {
            Object obj = el.getField();
            if (obj == null) {
                s = "-";
            } else {
                s = obj.toString();
            }
        }
        ret.add(s);
        return ret;
    }

    private ResultElement getResultElement(List<ResultElement> row) {
        ResultElement el = null;
        for (Integer index : featureIndexes) {
            el = row.get(index);
            if (el != null) {
                break;
            }
        }
        return el;
    }

    
    private List<ResultElement> getResultElements(List<ResultElement> row) {
        List<ResultElement> els = new ArrayList<ResultElement>();
        for (Integer index : featureIndexes) {
        if (row.get(index) != null) {
                els.add(row.get(index));                    
            }
        }
        return els;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean canExport(List<Class> clazzes) {
        return canExportStatic(clazzes);
    }

    /* Method must have different name than canExport because canExport() method
     * is inherited from Exporter interface */
    /**
     * @param clazzes classes of result row
     * @return true if this exporter can export result composed of specified classes
     */
    public static boolean canExportStatic(List<Class> clazzes) {
        return ExportHelper.getClassIndex(clazzes, LocatedSequenceFeature.class) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }

    private void finishLastRow() {
        GFF3Record gff3Record = GFF3Util.makeGFF3Record(lastLsf, soClassNames, sourceName,
                attributes);

        if (gff3Record != null) {
            // have a chromsome ref and chromosomeLocation ref
            if (!headerPrinted) {
                out.println(getHeader());
                headerPrinted = true;
            }

            out.println(gff3Record.toGFF3());
            writtenResultsCount++;
        }
        lastLsfId = null;
    }
}
