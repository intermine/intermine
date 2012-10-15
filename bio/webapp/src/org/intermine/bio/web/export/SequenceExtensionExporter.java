package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Path;
import org.intermine.util.IntPresentSet;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;

/**
 * Export data in FASTA format with extension. Select cell in each row that can be exported as
 * a sequence and fetch associated sequence.
 *
 * @author Fengyuan Hu
 **/
public class SequenceExtensionExporter implements Exporter
{

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(SequenceExtensionExporter.class);

    private ObjectStore os;
    private OutputStream out;
    private int featureIndex;
    private int writtenResultsCount = 0;
    private final Map<String, List<FieldDescriptor>> classKeys;
    private int extension; // must >= 1
    // Map to hold DNA sequence of a whole chromosome in memory
    private static Map<MultiKey, String> chromosomeSequenceMap = new HashMap<MultiKey, String>();

    /**
     * Constructor.
     *
     * @param os
     *            object store used for fetching sequence for exported object
     * @param outputStream
     *            output stream
     * @param featureIndex
     *            index of cell in row that contains object to be exported
     * @param classKeys for the model
     */
    public SequenceExtensionExporter(ObjectStore os, OutputStream outputStream,
            int featureIndex, Map<String, List<FieldDescriptor>> classKeys, int extension) {
        this.os = os;
        this.out = outputStream;
        this.featureIndex = featureIndex;
        this.classKeys = classKeys;
        this.extension = extension;
    }

    /**
     * {@inheritDoc}
     */
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void export(Iterator<? extends List<ResultElement>> resultIt) {
        export(resultIt, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    /**
     * {@inheritDoc} Lines are always separated with \n because third party tool
     * writeFasta is used for writing sequence.
     */
    public void export(Iterator<? extends List<ResultElement>> resultIt,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {
        // IDs of the features we have successfully output - used to avoid
        // duplicates
        IntPresentSet exportedIDs = new IntPresentSet();

        try {
            while (resultIt.hasNext()) {
                List<ResultElement> row = resultIt.next();

                StringBuffer header = new StringBuffer();

                ResultElement resultElement = row.get(featureIndex);

                Sequence seq;
                Object object = os.getObjectById(resultElement.getId());
                if (!(object instanceof InterMineObject)) {
                    continue;
                }

                Integer objectId = ((InterMineObject) object).getId();
                if (exportedIDs.contains(objectId)) {
                    // exported already
                    continue;
                }

                if (object instanceof SequenceFeature) {
                    seq = createSequenceFeature(header, object,
                            row, unionPathCollection, newPathCollection);
                } else {
                    // ignore other objects
                    continue;
                }

                if (seq == null) {
                    // the object doesn't have a sequence
                    header.append("no sequence attached.");
                    continue;
                }

                Annotation annotation = seq.getAnnotation();
                String headerString = header.toString();

                if (headerString.length() > 0) {
                    annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE, headerString);
                } else {
                    if (object instanceof BioEntity) {
                        annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE,
                                ((BioEntity) object).getPrimaryIdentifier());
                    } else {
                        // last resort
                        annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE,
                                "sequence_" + exportedIDs.size());
                    }
                }
                SeqIOTools.writeFasta(out, seq);
                writtenResultsCount++;
                exportedIDs.add(objectId);
            }

            if (writtenResultsCount == 0) {
                out.write("Nothing was found for export".getBytes(Charset.forName("UTF-8")));
            }

            out.flush();
        } catch (Exception e) {
            throw new ExportException("Export failed.", e);
        }
    }

    private Sequence createSequenceFeature(StringBuffer header,
            Object object, List<ResultElement> row,
            Collection<Path> unionPathCollection,
            Collection<Path> newPathCollection)
        throws IllegalSymbolException {

        SequenceFeature feature = (SequenceFeature) object;

        Chromosome chr = feature.getChromosome();
        String chrName = chr.getPrimaryIdentifier();
        int chrLength = chr.getLength();
        int start = feature.getChromosomeLocation().getStart();
        int end = feature.getChromosomeLocation().getEnd();
        String org = feature.getOrganism().getShortName();

        String chrResidueString;
        if (chromosomeSequenceMap.get(new MultiKey(chrName, org)) == null) {
            chrResidueString = chr.getSequence().getResidues()
                    .toString();
            chromosomeSequenceMap.put(
                    new MultiKey(chrName, org), chr.getSequence().getResidues().toString());
        } else {
            chrResidueString = chromosomeSequenceMap.get(new MultiKey(chrName, org));
        }

        if (extension > 0) {
            start = start - extension;
            end = end + extension;
        }

        end = Math.min(end, chrLength);
        start = Math.max(start, 1);

        String seqName = "genomic_region_" + chrName + "_"
                + start + "_" + end + "_"
                + org.replace("\\. ", "_");

        Sequence seq = DNATools.createDNASequence(chrResidueString.substring(start - 1, end),
                        seqName);

        makeHeader(header, object, row, unionPathCollection, newPathCollection);
        return seq;
    }

    /**
     * Set the header to be the contents of row, separated by spaces.
     */
    private void makeHeader(StringBuffer header, Object object,
            List<ResultElement> row, Collection<Path> unionPathCollection,
            Collection<Path> newPathCollection) {

        List<String> headerBits = new ArrayList<String>();

        // add the Object's (Protein or LocatedSequenceFeature)
        // primaryIdentifier at the first place
        // in the header
        Object keyFieldValue =
            ClassKeyHelper.getKeyFieldValue((FastPathObject) object, this.classKeys);
        if (keyFieldValue != null) {
            headerBits.add(keyFieldValue.toString());
        } else {
            headerBits.add("-");
        }

        List<ResultElement> subRow = new ArrayList<ResultElement>();
        if (newPathCollection != null && unionPathCollection != null
                && unionPathCollection.containsAll(newPathCollection)) {
            for (Path p : newPathCollection) {
                subRow.add(row.get(((List<Path>) unionPathCollection).indexOf(p)));
            }
        } else {
            subRow = row;
        }

        // two instances
        if (object instanceof SequenceFeature) {

            // add the sequence location info at the second place in the header
            SequenceFeature feature = (SequenceFeature) object;
            Location loc = feature.getChromosomeLocation();
            if (loc == null) {
                headerBits.add("-");
            } else {
                // Assume if loc exits, the following information should be available
                String chr = loc.getLocatedOn().getPrimaryIdentifier();
                Integer start = loc.getStart();
                Integer end = loc.getEnd();

                String locString = chr + ':' + start + '-' + end;
                headerBits.add(locString);
            }

            for (ResultElement re : subRow) {
                // to avoid failure in modmine when no experimental factors (sub 2745)
                if (re == null) {
                    continue;
                }

                Object fieldValue = re.getField();
                if (fieldValue == null) {
                    headerBits.add("-");
                } else if (fieldValue.toString().equals(keyFieldValue)
                        || (re.getObject() instanceof Location)
                        || (re.getObject() instanceof Chromosome)) {
                    // ignore the primaryIdentifier and Location in
                    // ResultElement
                    continue;
                } else {
                    headerBits.add(fieldValue.toString());
                }
            }

        }

        headerBits.add(extension + "bp");

        header.append(StringUtil.join(headerBits, " "));
    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(List<Class<?>> clazzes) {
        return canExportStatic(clazzes);
    }

    /*
     * Method must have different name than canExport because canExport() method
     * is inherited from Exporter interface
     */
    /**
     * @param clazzes
     *            classes of result
     * @return true if this exporter can export result composed of specified
     *         classes
     */
    public static boolean canExportStatic(List<Class<?>> clazzes) {
        return (ExportHelper.getClassIndex(clazzes,
                SequenceFeature.class) >= 0
//                || ExportHelper.getClassIndex(clazzes, Protein.class) >= 0
//                || ExportHelper.getClassIndex(clazzes, Sequence.class) >= 0
                );
    }
}
