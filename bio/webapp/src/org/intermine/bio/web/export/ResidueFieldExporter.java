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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.utils.ChangeVetoException;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
import org.intermine.bio.web.biojava.BioSequence;
import org.intermine.bio.web.biojava.BioSequenceFactory;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.FieldExporter;

/**
 * ResidueFieldExporter class
 *
 * @author Kim Rutherford
 */

public class ResidueFieldExporter implements FieldExporter
{
    protected static final Logger LOG = Logger.getLogger(ResidueFieldExporter.class);

    /**
     * Export a field containing residues in FASTA format.
     *
     * @param object the object of interest
     * @param fieldName the field of the object to output - should be sequence
     * @param os the ObjectStore that contains the object
     * @param response The HTTP response we are creating - used to get the OutputStream to write to
     * @throws ExportException if the application business logic throws an exception
     */
    public void exportField(InterMineObject object, String fieldName, ObjectStore os,
            HttpServletResponse response) {
        if (!(object instanceof Sequence)) {
            throw new IllegalArgumentException("ResidueFieldExporter can only export "
                                               + "Sequence.residues fields");
        }

        Sequence sequence = (Sequence) object;

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=" + fieldName + ".fasta");

        try {
            SequenceFeature lsf =
                getSequenceFeatureForSequence(os, (Sequence) object);
            BioEntity bioEntity = lsf;

            Protein protein = null;

            if (bioEntity == null) {
                protein = getProteinForSequence(os, sequence);
                bioEntity = protein;
            }

            if (bioEntity == null) {
                LOG.error("No SequenceFeature or Protein has a Sequence with id "
                          + sequence.getId());
                OutputStream outputStream = response.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                sequence.getResidues().drainToPrintStream(printStream);
                printStream.println("");
                printStream.close();
                outputStream.close();
                return;
            }

            BioSequence bioSequence;

            if (lsf != null) {
                bioSequence = BioSequenceFactory.make(lsf);
            } else {
                bioSequence = BioSequenceFactory.make(protein);
            }

            // avoid opening the OutputStream until we have all the data - this avoids some problems
            // that occur when getOutputStream() is called twice (once by this method and again to
            // write the error)
            OutputStream outputStream = response.getOutputStream();

            Annotation annotation = bioSequence.getAnnotation();

            annotation.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE,
                                   bioEntity.getPrimaryIdentifier());

            SeqIOTools.writeFasta(outputStream, bioSequence);

            outputStream.close();
        } catch (IllegalSymbolException e) {
            throw new ExportException("unexpected error while exporting", e);
        } catch (IllegalArgumentException e) {
            throw new ExportException("unexpected error while exporting", e);
        } catch (ChangeVetoException e) {
            throw new ExportException("unexpected error while exporting", e);
        } catch (IOException e) {
            throw new ExportException("unexpected IO error while exporting", e);
        }
    }

    /**
     * Find the SequenceFeature that references the given Sequence.
     * @param os the ObjectStore
     * @param sequence the Sequence
     * @return the SequenceFeature
     */
    public static SequenceFeature getSequenceFeatureForSequence(ObjectStore os,
                                                                              Sequence sequence) {
        Query q = new Query();

        QueryClass lsfQc = new QueryClass(SequenceFeature.class);
        q.addFrom(lsfQc);
        q.addToSelect(lsfQc);

        QueryClass sequenceQc = new QueryClass(Sequence.class);
        q.addFrom(sequenceQc);
        QueryReference ref = new QueryObjectReference(lsfQc, "sequence");
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(ref, ConstraintOp.CONTAINS, sequenceQc));

        QueryField seqIdQf = new QueryField(sequenceQc, "id");
        QueryValue seqIdQv = new QueryValue(sequence.getId());
        cs.addConstraint(new SimpleConstraint(seqIdQf, ConstraintOp.EQUALS, seqIdQv));
        q.setConstraint(cs);

        Results results = os.execute(q);

        if (results.size() == 1) {
            return (SequenceFeature) ((List) results.get(0)).get(0);
        } else {
            return null;
        }
    }


    /**
     * Find the Protein that references the given Sequence.
     * @param os the ObjectStore
     * @param sequence the Sequence
     * @return the Protein
     */
    public static Protein getProteinForSequence(ObjectStore os, Sequence sequence) {
        Query q = new Query();

        QueryClass proteinQc = new QueryClass(Protein.class);
        q.addFrom(proteinQc);
        q.addToSelect(proteinQc);

        QueryClass sequenceQc = new QueryClass(Sequence.class);
        q.addFrom(sequenceQc);
        QueryReference ref = new QueryObjectReference(proteinQc, "sequence");
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(ref, ConstraintOp.CONTAINS, sequenceQc));

        QueryField seqIdQf = new QueryField(sequenceQc, "id");
        QueryValue seqIdQv = new QueryValue(sequence.getId());
        cs.addConstraint(new SimpleConstraint(seqIdQf, ConstraintOp.EQUALS, seqIdQv));
        q.setConstraint(cs);

        Results results = os.execute(q);

        if (results.size() == 1) {
            return (Protein) ((List) results.get(0)).get(0);
        } else {
            return null;
        }
    }

    /**
     * Find the IntermineObject that references the given Sequence.
     * @param os the ObjectStore
     * @param clazz the class of the InterMineObject
     * @param sequence the Sequence
     * @return the IntermineObject
     */
    public static InterMineObject getIMObjectForSequence(ObjectStore os, Class clazz,
                                                Sequence sequence) {
        Query q = new Query();

        QueryClass queryClass = new QueryClass(clazz);
        q.addFrom(queryClass);
        q.addToSelect(queryClass);

        QueryClass sequenceQc = new QueryClass(Sequence.class);
        q.addFrom(sequenceQc);
        QueryReference ref = new QueryObjectReference(queryClass, "sequence");
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(ref, ConstraintOp.CONTAINS, sequenceQc));

        QueryField seqIdQf = new QueryField(sequenceQc, "id");
        QueryValue seqIdQv = new QueryValue(sequence.getId());
        cs.addConstraint(new SimpleConstraint(seqIdQf, ConstraintOp.EQUALS, seqIdQv));
        q.setConstraint(cs);

        Results results = os.execute(q);

        if (results.size() == 1) {
            return (InterMineObject) ((List) results.get(0)).get(0);
        } else {
            return null;
        }
    }
}
