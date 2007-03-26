package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Class to search a dag ontology for errors/complications, validate method returns
 * true if the dag is deemed valid.  Checks are run for:
 *    duplicate names
 *    duplciate ids
 *    synonyms that are terms
 *    partof elements that are not defined in isa relationships
 * A list of issues is available by the getOutput() method.
 *
 * @author Richard Smith
 */
public class DagValidator
{
    private Map ids = new HashMap();
    private Map names = new HashMap();
    private Map synMap = new HashMap();
    private Map nameMap = new HashMap();
    private StringBuffer output = new StringBuffer();
    private Set partofs = new HashSet();
    private Set isas = new HashSet();


    /**
     * Search ontology for errors/complications, return true if dag is deemed valid.
     * @param rootTerms a collection of toplevel ontology terms
     * @return true if dag is valid
     * @throws Exception if anyhting goes wrong
     */
    public boolean validate(Collection rootTerms) throws Exception {
        boolean dupNames = duplicateNames(rootTerms);
        boolean dupIds = duplicateIds(rootTerms);
        boolean synonyms = synonymsAreTerms(rootTerms);
        boolean orphanPartOfs = orphanPartOfs(rootTerms);

        return dupNames && dupIds && synonyms && orphanPartOfs;
    }


    /**
     * Return details of issues discovered when parsing dag.
     * @return detailed results from parsing dag
     */
    public String getOutput() {
        return output.toString();
    }

    /**
     * Recursively run given method on a DagTerm and all its children/compnent parts.
     * @param term DagTerm to run method on
     * @param m a method to run on each DagTerm that takes a DagTerm as its only argument
     * @throws NoSuchMethodException if reflection problem occurs
     * @throws IllegalAccessException if reflection problem occurs
     * @throws InvocationTargetException if reflection problem occurs
     */
    protected void operateOnTree(DagTerm term, Method m)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Iterator iter = term.getChildren().iterator();
        while (iter.hasNext()) {
            operateOnTree((DagTerm) iter.next(), m);
        }

        iter = term.getComponents().iterator();
        while (iter.hasNext()) {
            operateOnTree((DagTerm) iter.next(), m);
        }

        m.invoke(this, new Object[] {term});
    }

    /**
     * Search ontology for ids associated with more than one name.
     * @param rootTerms a collection of toplevel ontology terms
     * @return true if no duplicate names found
     * @throws Exception if anything goes wrong
     */
    protected boolean duplicateNames(Collection rootTerms) throws Exception {
        Method m = this.getClass().getDeclaredMethod("collectNames", new Class[] {DagTerm.class});
        for (Iterator iter = rootTerms.iterator(); iter.hasNext(); ) {
            operateOnTree((DagTerm) iter.next(), m);
        }

        boolean valid = true;

        Iterator idIter = ids.entrySet().iterator();
        while (idIter.hasNext()) {
            Map.Entry e = (Map.Entry) idIter.next();
            if (((Set) e.getValue()).size() > 1) {
                String nameStr = "";
                for (Iterator i = ((Set) e.getValue()).iterator(); i.hasNext(); ) {
                    nameStr = nameStr + ((DagTerm) i.next()).getName() + " ";
                }

                output.append("id: \'" + e.getKey() + "\' has duplicate names: " + nameStr + "\n");
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Search ontology for names associated with more than one id.
     * @param rootTerms a collection of toplevel ontology terms
     * @return true if no duplicate ids found
     * @throws Exception if anything goes wrong
     */
    protected boolean duplicateIds(Collection rootTerms) throws Exception {
        Method m = this.getClass().getDeclaredMethod("collectIds", new Class[] {DagTerm.class});
        for (Iterator iter = rootTerms.iterator(); iter.hasNext(); ) {
            operateOnTree((DagTerm) iter.next(), m);
        }

        boolean valid = true;

        Iterator namesIter = names.entrySet().iterator();
        while (namesIter.hasNext()) {
            Map.Entry e = (Map.Entry) namesIter.next();
            if (((Set) e.getValue()).size() > 1) {
                String idStr = "";
                for (Iterator i = ((Set) e.getValue()).iterator(); i.hasNext(); ) {
                    idStr = idStr + ((DagTerm) i.next()).getId() + " ";
                }
                output.append("name: \'" + e.getKey() + "\' has duplicate ids: " + idStr + "\n");
                valid = false;
            }
        }
        return valid;
    }

    /**
     *
     * @param rootTerms a collection of toplevel ontology terms
     * @return true of no synonyms are terms
     * @throws Exception if anything goes wrong
     */
    protected boolean synonymsAreTerms(Collection rootTerms) throws Exception {
        Method m = this.getClass().getDeclaredMethod("collectSynonyms",
                                                     new Class[] {DagTerm.class});
        for (Iterator iter = rootTerms.iterator(); iter.hasNext(); ) {
            operateOnTree((DagTerm) iter.next(), m);
        }

        boolean valid = true;

        Iterator iter = synMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            Iterator synIter = ((Set) e.getValue()).iterator();
            while (synIter.hasNext()) {
                DagTermSynonym synonym = (DagTermSynonym) synIter.next();
                if (synMap.containsKey(synonym.getName())) {
                    DagTerm term = (DagTerm) nameMap.get((String) e.getKey());
                    DagTerm synTerm = (DagTerm) nameMap.get(synonym.getName());
                    output.append("synonym \'" + synonym + "\' for term: \'" + term.toString()
                                  + "\' is also term: " + synTerm.getId() + "\n");
                    valid = false;
                }
            }
        }
        return valid;
    }


    /**
     * Find terms that are defined in partof relationships and not defined elsewhere
     * in an isa relationship.  This situation means the terms cannot fit into a subclass
     * hierarchy and can onlt be placed at the top level.
     * @param rootTerms a set of terms at the top of the ontology tree
     * @return true if there are no orphan partOfs
     * @throws Exception if anything goes wrong
     */
    protected boolean orphanPartOfs(Collection rootTerms) throws Exception {
        Method m = this.getClass().getDeclaredMethod("flattenRelations",
                                                     new Class[] {DagTerm.class});
        for (Iterator iter = rootTerms.iterator(); iter.hasNext(); ) {
            operateOnTree((DagTerm) iter.next(), m);
        }

        boolean valid = true;

        Iterator iter = partofs.iterator();
        while (iter.hasNext()) {
            DagTerm term = (DagTerm) iter.next();
            if (!isas.contains(term)) {
                output.append("orphan partof: " + term.getId() + ", " + term.getName() + "\n");
                valid = false;
            }
        }
        return valid;
    }


    private void collectSynonyms(DagTerm term) {
        synMap.put(term.getName(), term.getSynonyms());
        nameMap.put(term.getName(), term);
    }

    private void collectNames(DagTerm term) {
        HashSet tmpIds = (HashSet) ids.get(term.getId());
        if (tmpIds == null) {
            tmpIds = new HashSet();
            ids.put(term.getId(), tmpIds);
        }
        tmpIds.add(term);
    }

    private void collectIds(DagTerm term) {
        HashSet tmpNames = (HashSet) names.get(term.getName());
        if (tmpNames == null) {
            tmpNames = new HashSet();
            names.put(term.getName(), tmpNames);
        }
        tmpNames.add(term);
    }

    private void flattenRelations(DagTerm term) {
        partofs.addAll(term.getComponents());
        isas.addAll(term.getChildren());
    }




//     public static void main(String[] args) {
//         String dagFilename = args[0];
//         String outputFilename = args[1];
//         try {
//             File dagFile = new File(dagFilename);
//             File outputFile = new File(outputFilename);

//             DagValidator validator = new DagValidator();
//             DagParser parser = new DagParser();

//             Set terms = parser.process(new BufferedReader(new FileReader(dagFile)));
//             validator.validate(terms);

//             BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
//             out.write(validator.getOutput());
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

}
