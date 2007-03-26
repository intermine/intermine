package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.ProteinInteraction;
import org.flymine.model.genomic.ExperimentalResult;
import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.Comment;

import org.apache.log4j.Logger;

/**
 * Class to add "availability" statements to relevant objects
 * @author Mark Woodbridge
 */
public class AddLicences
{
    protected static final Logger LOG = Logger.getLogger(AddLicences.class);
    protected static final String HGX_DATABASE = "Hybrigenics data set";
    protected static final String HGX_LICENSE = "These interactions are the sole property of "
        + "HYBRIGENICS, and shall not be used for any business or commercial purposes without the "
        + "prior written license from HYBRIGENICS (http://www.hybrigenics.com)";

    protected ObjectStoreWriter osw;

    /**
     * Constructor
     * @param osw the ObjectStoreWriter to read from and write to
     */
    public AddLicences(ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Add "availability" statements to relevant objects
     * @throws Exception if an error occurs
     */
    public void execute() throws Exception {
        for (Iterator i = getHybrigenicsProteinInteractions().iterator(); i.hasNext();) {
            ProteinInteraction interaction = (ProteinInteraction)
                PostProcessUtil.cloneInterMineObject((ProteinInteraction) i.next());
            addComment(interaction, "availability", HGX_LICENSE);
            storeInteraction(interaction);
        }
    }

    /**
     * Add comment to suitable object
     * @param interaction the object
     * @param type the type of the comment
     * @param text the text of the comment
     */
    protected void addComment(ProteinInteraction interaction, String type, String text) {
        Comment comment = (Comment)
            DynamicUtil.createObject(Collections.singleton(Comment.class));
        comment.setType(type);
        comment.setText(text);
        HashSet comments = new HashSet(interaction.getComments());
        comments.add(comment);
        interaction.setComments(comments);
    }

    /**
     * Retrieve the publications to be updated
     * @return a List of publications
     */
    protected List getHybrigenicsProteinInteractions() {
        // proteininteractions where evidence contains an
        // experimentalresult with source.title = 'Hybrigenics'
        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        QueryClass qc = new QueryClass(ProteinInteraction.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryClass qc2 = new QueryClass(ExperimentalResult.class);
        q.addFrom(qc2);
        ContainsConstraint cc = new ContainsConstraint(new QueryCollectionReference(qc, "evidence"),
                                                       ConstraintOp.CONTAINS, qc2);
        cs.addConstraint(cc);
        QueryClass qc3 = new QueryClass(DataSet.class);
        q.addFrom(qc3);
        ContainsConstraint cc2 = new ContainsConstraint(new QueryObjectReference(qc2, "source"),
                                                        ConstraintOp.CONTAINS, qc3);
        cs.addConstraint(cc2);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc3, "title"),
                                                   ConstraintOp.EQUALS,
                                                   new QueryValue(HGX_DATABASE));
        cs.addConstraint(sc);
        return new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore().getSequence());
    }

    /**
     * Store an ProteinInteraction and its attached comments
     * @param interaction the interaction
     * @throws ObjectStoreException if an error occurs
     */
    protected void storeInteraction(ProteinInteraction interaction) throws ObjectStoreException {
        osw.beginTransaction();
        osw.store(interaction);
        for (Iterator i = interaction.getComments().iterator(); i.hasNext();) {
            Comment comment = (Comment) i.next();
            osw.store(comment);
        }
        osw.commitTransaction();
    }

    /**
     * Main method
     * @param args the arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        ObjectStoreWriter osw =
            ObjectStoreWriterFactory.getObjectStoreWriter("osw.production");
        DataSet database = (DataSet)
            DynamicUtil.createObject(Collections.singleton(DataSet.class));
        database.setTitle(HGX_DATABASE);
        ProteinInteraction interaction = (ProteinInteraction)
            DynamicUtil.createObject(Collections.singleton(ProteinInteraction.class));
        ExperimentalResult result = (ExperimentalResult)
            DynamicUtil.createObject(Collections.singleton(ExperimentalResult.class));
        result.setSource(database);
        result.getRelations().add(interaction);
        osw.store(interaction);
        osw.store(result);
        osw.store(database);
        new AddLicences(osw).execute();
        osw.close();
    }
}
