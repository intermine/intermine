package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.bio.web.biojava.BioSequence;
import org.intermine.bio.web.biojava.BioSequenceFactory;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.IntPresentSet;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.ExportHelper;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.ResultElement;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;
import org.flymine.model.genomic.Translation;

import java.io.OutputStream;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.IllegalSymbolException;


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
    public void export(List<List<ResultElement>> results, List<Column> columns) {
        // IDs of the features we have successfully output - used to avoid duplicates
        IntPresentSet exportedIDs = new IntPresentSet();

        try {
            for (int rowIndex = 0; rowIndex < results.size(); rowIndex++) {
                List<ResultElement> row = results.get(rowIndex);

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
                    bioSequence = createLocatedSequenceFeature(header, object, row, columns);
                } else if (object instanceof Protein) {
                    bioSequence = createProtein(header, object);
                } else if (object instanceof Translation) {
                    Model model = os.getModel();
                    bioSequence = createTranslation(header, object, model);
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

    private BioSequence createTranslation(StringBuffer header, Object object,
                                          Model model) throws IllegalSymbolException {
        BioSequence bioSequence;
        ClassDescriptor cld = model.getClassDescriptorByName(model.getPackageName()
                                                             + "." + "Translation");
        if (cld.getReferenceDescriptorByName("sequence", true) != null) {
            Translation translation = (Translation) object;
            bioSequence = BioSequenceFactory.make(translation);
            header.append(translation.getPrimaryIdentifier());
            header.append(' ');
            if (translation.getName() == null) {
                header.append("[unknown_name]");
            } else {
                header.append(translation.getName());
            }
            if (translation.getGene() != null) {
                Gene gene = translation.getGene();
                String geneIdentifier = gene.getPrimaryIdentifier();
                if (geneIdentifier != null) {
                    header.append(' ');
                    header.append("gene:");
                    header.append(geneIdentifier);
                }
            }
        } else {
            bioSequence = null;
        }
        return bioSequence;
    }

    private BioSequence createProtein(StringBuffer header, Object object)
            throws IllegalSymbolException {
        BioSequence bioSequence;
        Protein protein = (Protein) object;
        bioSequence = BioSequenceFactory.make(protein);
        header.append(protein.getPrimaryIdentifier());
        header.append(' ');
        if (protein.getName() == null) {
            header.append("[unknown_name]");
        } else {
            header.append(protein.getName());
        }
        Iterator<Gene> iter = protein.getGenes().iterator();
        while (iter.hasNext()) {
            Gene gene = iter.next();
            String geneIdentifier = gene.getPrimaryIdentifier();
            if (geneIdentifier != null) {
                header.append(' ');
                header.append("gene:");
                header.append(geneIdentifier);
            }

        }
        return bioSequence;
    }

    private BioSequence createLocatedSequenceFeature(StringBuffer header,
                                                     Object object, List<ResultElement> row,
                                                     List<Column> columns)
        throws IllegalSymbolException {
        BioSequence bioSequence;
        LocatedSequenceFeature feature = (LocatedSequenceFeature) object;
        bioSequence = BioSequenceFactory.make(feature);
        if (feature.getPrimaryIdentifier() == null) {
            header.append("[unknown_identifier]");
        } else {
            header.append(feature.getPrimaryIdentifier());
        }
        header.append(' ');
        header.append(getHeadersFromColumns(row, columns));

        Location chromosomeLoc = feature.getChromosomeLocation();

        if (chromosomeLoc != null) {
            header.append(' ').append(chromosomeLoc.getObject().getPrimaryIdentifier());
            header.append(':').append(chromosomeLoc.getStart());
            header.append('-').append(chromosomeLoc.getEnd());
            header.append(' ').append(feature.getLength());
        }
        try {
            Gene gene = (Gene) TypeUtil.getFieldValue(feature, "gene");
            if (gene != null) {
                String geneIdentifier = gene.getPrimaryIdentifier();
                if (geneIdentifier != null) {
                    header.append(' ').append("gene:").append(geneIdentifier);
                }
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
        return bioSequence;
    }

    /**
     * Look at the Columns and the current row and return a String containing all the elements of
     * the row except the primaryIdentifier (if any).
     */
    private String getHeadersFromColumns(List<ResultElement> row, List<Column> columns) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < row.size(); i++) {
            Column column = columns.get(i);
            if (!column.getPath().getEndFieldDescriptor().getName().equals("primaryIdentifier")) {
                ResultElement resultElement = row.get(i);
                Object rawField = resultElement.getField();
                String field;
                if (rawField == null) {
                    field = "-";
                } else {
                    field = rawField.toString();
                }
                sb.append(field).append(' ');
            }
        }

        return sb.toString();
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
                || ExportHelper.getClassIndex(clazzes, Translation.class) >= 0
                || ExportHelper.getClassIndex(clazzes, Sequence.class) >= 0);
    }
}
