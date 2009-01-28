package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;
import org.intermine.bio.web.biojava.BioSequence;
import org.intermine.bio.web.biojava.BioSequenceFactory;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.IntPresentSet;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.results.ResultElement;

/**
 * Export data in FASTA format. Select cell in each row
 * that can be exported as a sequence and fetch associated sequence.
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 **/
public class SequenceExporter implements Exporter
{

    private ObjectStore os;
    private OutputStream out;
    private int featureIndex;
    private int writtenResultsCount = 0;

    /**
     * Constructor.
     * @param os object store used for fetching sequence for  exported object
     * @param outputStream output stream
     * @param featureIndex index of cell in row that contains object to be exported
     */
    public SequenceExporter(ObjectStore os, OutputStream outputStream,
                            int featureIndex) {
        this.os = os;
        this.out = outputStream;
        this.featureIndex = featureIndex;
    }

    /**
     * {@inheritDoc}
     */
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }

    /**
     * {@inheritDoc}
     * Lines are always separated with \n because third party tool writeFasta
     * is used for writing sequence.
     */
    public void export(Iterator<List<ResultElement>> resultIt) {
        // IDs of the features we have successfully output - used to avoid duplicates
        IntPresentSet exportedIDs = new IntPresentSet();

        try {
            while (resultIt.hasNext()) {
                List<ResultElement> row = resultIt.next();

                StringBuffer header = new StringBuffer();

                ResultElement resultElement = row.get(featureIndex);

                BioSequence bioSequence;
                Object object = os.getObjectById(resultElement.getId());
                if (!(object instanceof InterMineObject)) {
                    continue;
                }

                Integer objectId = ((InterMineObject) object).getId();
                if (exportedIDs.contains(objectId)) {
                    // exported already
                    continue;
                }

                if (object instanceof LocatedSequenceFeature) {
                    bioSequence = createLocatedSequenceFeature(header, object, row);
                } else if (object instanceof Protein) {
                    bioSequence = createProtein(header, object, row);
                } else {
                    // ignore other objects
                    continue;
                }

                if (bioSequence == null) {
                    // the object doesn't have a sequence
                    continue;
                }

                Annotation annotation = bioSequence.getAnnotation();
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

                SeqIOTools.writeFasta(out, bioSequence);
                writtenResultsCount++;
                exportedIDs.add(objectId);
            }

            out.flush();
        } catch (Exception e) {
              throw new ExportException("Export failed.", e);
        }
    }

    private BioSequence createProtein(StringBuffer header, Object object, List<ResultElement> row)
            throws IllegalSymbolException {
        BioSequence bioSequence;
        Protein protein = (Protein) object;
        bioSequence = BioSequenceFactory.make(protein);

        makeHeader(header, row);

        return bioSequence;
    }

    private BioSequence createLocatedSequenceFeature(StringBuffer header,
                                                     Object object, List<ResultElement> row)
        throws IllegalSymbolException {
        BioSequence bioSequence;
        LocatedSequenceFeature feature = (LocatedSequenceFeature) object;
        bioSequence = BioSequenceFactory.make(feature);

        makeHeader(header, row);

        return bioSequence;
    }

    /**
     * Set the header to be the contents of row, separated by spaces.
     */
    private void makeHeader(StringBuffer header, List<ResultElement> row) {

        List<String> headerBits = new ArrayList<String>();

        for (ResultElement re: row) {
            Object fieldValue = re.getField();
            if (fieldValue == null) {
                headerBits.add("-");
            } else {
                if (fieldValue instanceof Location) {
                    Location location = (Location) fieldValue;
                    String primaryIdentifier = location.getObject().getPrimaryIdentifier();
                    Integer start = location.getStart();
                    Integer end = location.getEnd();
                    String locString = primaryIdentifier + ':' + start + '-' + end;
                    headerBits.add(locString);
                } else {
                    headerBits.add(fieldValue.toString());
                }
            }
        }

        header.append(StringUtil.join(headerBits, " "));

    }

    /**
     * {@inheritDoc}
     */
    public boolean canExport(List<Class> clazzes) {
        return canExportStatic(clazzes);
    }

    /* Method must have different name than canExport because canExport() method
     * is  inherited from Exporter interface */
    /**
     * @param clazzes classes of result
     * @return true if this exporter can export result composed of specified classes
     */
    public static boolean canExportStatic(List<Class> clazzes) {
        return (
                ExportHelper.getClassIndex(clazzes, LocatedSequenceFeature.class) >= 0
                || ExportHelper.getClassIndex(clazzes, Protein.class) >= 0
                || ExportHelper.getClassIndex(clazzes, Sequence.class) >= 0);
    }
}
