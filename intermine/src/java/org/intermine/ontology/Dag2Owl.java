package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import org.flymine.util.StringUtil;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;

/**
 * Processes list of root DagTerms to produce the equivalent OWL OntModel
 *
 * @author Mark Woodbridge
 */
public class Dag2Owl
{
    /**
     * Namespace to use for generated classes in OWL document
     */
    protected static final String NS = "http://www.flymine.org/namespace#";

    /**
     * Perform the conversion by iterating over the root terms
     * @param rootTerms a collection of rootTerms
     * @return the corresponding OntModel
     */
    public static OntModel process(Collection rootTerms) {
        OntModel ontModel = ModelFactory.createOntologyModel();
        for (Iterator i = rootTerms.iterator(); i.hasNext(); ) {
            processTerm((DagTerm) i.next(), ontModel);
        }
        return ontModel;
    }

    /**
     * Actually convert a DagTerm into a OntClass
     * @param term a DagTerm
     * @param ontModel the OWL model in which the class will be created
     * @return the corresponding OntClass
     */
    public static OntClass processTerm(DagTerm term, OntModel ontModel) {
        OntClass cls = ontModel.getOntClass(NS + term.getId());
        if (cls == null) {
            cls = ontModel.createClass(NS + term.getId());
            cls.setLabel(term.getName(), null);
            if (term.getSynonyms().size() > 0) {
                cls.addComment("synonyms=" + StringUtil.join(term.getSynonyms(), ", "), null);
            }
            for (Iterator i = term.getChildren().iterator(); i.hasNext(); ) {
                cls.addSubClass(processTerm((DagTerm) i.next(), ontModel));
            }
        }
        return cls;
    }

    /**
     * Run conversion from DAG to OWL format, still produces OWL if validation fails,
     * details of problems written to error file.
     * @param args dagFilename, owlFilename, errorFilename
     * @throws Exception if anthing goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("Usage: Dag2Owl dagfile owlfile errorfile");
        }

        String dagFilename = args[0];
        String owlFilename = args[1];
        String errorFilename = args[2];

        try {
            File dagFile = new File(dagFilename);
            File owlFile = new File(owlFilename);
            File errorFile = new File(errorFilename);

            DagParser parser = new DagParser();
            Set rootTerms = parser.process(new BufferedReader(new FileReader(dagFile)));

            DagValidator validator = new DagValidator();
            if (!validator.validate(rootTerms)) {
                 BufferedWriter out = new BufferedWriter(new FileWriter(errorFile));
                 out.write(validator.getOutput());
            }

            Dag2Owl owler = new Dag2Owl();
            BufferedWriter out = new BufferedWriter(new FileWriter(owlFile));
            owler.process(rootTerms).write(out);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}

