package org.intermine.bio.dataconversion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.Traverser;
import org.biopax.paxtools.controller.Visitor;
import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.entity;
import org.biopax.paxtools.model.level2.pathway;

/**
 * @see org.biopax.paxtools.controller.Visitor
 * @see org.biopax.paxtools.controller.Traverser
 */
public class ProteinPerPathwayFetcher implements Visitor {

    Traverser traverser;
    private Set<BioPAXElement> visited = new HashSet();
    private int depth = 0;

    /**
     * @param file OWL file to be parsed
     * @throws FileNotFoundException if file is missing
     */
    public ProteinPerPathwayFetcher(File file) 
    throws FileNotFoundException {
        traverser = new Traverser(new SimpleEditorMap(BioPAXLevel.L2), this);
        JenaIOHandler jenaIOHandler = new JenaIOHandler(null, BioPAXLevel.L2);
        Model model = jenaIOHandler.convertFromOWL(new FileInputStream(file));
        getProteinsPerPathway(model);
    }

    /**
     * Adds the BioPAX element into the model and traverses the element for its dependent elements.
     *
     * @param bpe    the BioPAX element to be added into the model
     * @param model  model into which the element will be added
     * @param editor editor that is going to be used for traversing functionallity
     * @see org.biopax.paxtools.controller.Traverser
     */
    public void visit(BioPAXElement bpe, Model model, PropertyEditor editor) {
        if (bpe != null) {
            if (bpe instanceof entity) {
                for (int i = 0; i < depth; i++) {
                    System.out.print(" ");
                }
                entity entity = (entity) bpe;
                System.out.print(entity.getModelInterface().getSimpleName()+": ");
                System.out.println(entity.getNAME()==null?entity.getRDFId():entity.getNAME());
            }
            if (!visited.contains(bpe)) {
                //if (bpe instanceof pathwayComponent || bpe instanceof sequenceParticipant || bpe instanceof complex)
                visited.add(bpe);
                depth++;
                traverser.traverse(bpe, model);
                depth--;
            }
        }
    }

    /**
     * Traverses and adds the element into the model.
     *
     * @param model   model into which the element will be added
     * @see org.biopax.paxtools.controller.Traverser
     */
    public void getProteinsPerPathway(Model model) {
        Set<pathway> pathways = model.getObjects(pathway.class);
        for (pathway pathway : pathways) {
            // System.out.println("PATHWAY: "+ pathway.getNAME());
            visited= new HashSet();
            traverser.traverse(pathway, model);
        }
    }
//
//    public static void main(String[] args) throws FileNotFoundException {
//        JenaIOHandler jenaIOHandler = new JenaIOHandler(null, BioPAXLevel.L2);
//        File file = new File("/home/julie/tmp/test.owl");
//        Model model = jenaIOHandler.convertFromOWL(new FileInputStream(file));
//        ProteinPerPathwayFetcher fetcher = new ProteinPerPathwayFetcher(new SimpleEditorMap(BioPAXLevel.L2));
//        fetcher.getProteinsPerPathway(model);
//        System.out.println("done");
//    }
}
