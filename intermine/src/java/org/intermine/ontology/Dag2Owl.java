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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import org.flymine.util.StringUtil;

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
}

