package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.ontology.OboParser;
import org.intermine.bio.ontology.OboRelation;
import org.intermine.bio.ontology.OboTerm;
import org.intermine.bio.ontology.OboTermSynonym;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;

/**
 * Convert tree of OboTerms into Items.
 *
 * @author Thomas Riley
 * @see DagConverter
 */
public class EmapaOboConverter extends OboConverter implements OboConverterInterface {



public EmapaOboConverter(ItemWriter writer, Model model, String dagFilename, String dagName,
                        String url, String termClass) {

	super(writer,model,dagFilename,dagName,url,termClass);
}



    /**
     * Set up attributes and references for the Item created from a DagTerm. Subclasses
     * can override this method to perform extra setup, for example by casting the
     * DagTerm to some someclass and retrieving extra attributes. Subclasses should call this
     * inherited method first. This method will call process() for each child/component term,
     * resulting in recursion.
     *
     * @param termId the term id
     * @param item the Item created (see termClass field for type)
     * @param term the source DagTerm
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void configureItem(String termId, Item item, OboTerm term)
        throws ObjectStoreException {
        item.addAttribute(new Attribute("name", term.getName()));
        item.addReference(new Reference("ontology", ontology.getIdentifier()));
        if (term.getId() != null) {
            item.addAttribute(new Attribute("identifier", term.getId()));
        }
        // this is the spcific EMAPA stuff 
        if(  term.getId()!= null && term.getId().indexOf("EMAPA") != -1){
           try{
             String starts_at =(String) ((List)term.getTagValues().get("starts_at")).get(0);
             String ends_at =(String) ((List)term.getTagValues().get("ends_at")).get(0);
             item.addAttribute(new Attribute("startsAt", starts_at));
             item.addAttribute(new Attribute("endsAt", ends_at));
           }catch(Exception e){}

        }

        for (OboTermSynonym syn : term.getSynonyms()) {
            Item synItem = synToItem.get(syn);
            if (synItem == null) {
                synItem = createItem("OntologyTermSynonym");
                synToItem.put(syn, synItem);
                configureSynonymItem(syn, synItem, term);
            }
            item.addToCollection("synonyms", synItem);
        }
        for (OboTerm xref : term.getXrefs()) {
            String identifier = xref.getId();
            String refId = xrefs.get(identifier);
            if (refId == null) {
                Item xrefTerm = createItem("OntologyTerm");
                refId = xrefTerm.getIdentifier();
                xrefs.put(identifier, refId);
                xrefTerm.setAttribute("identifier", identifier);
                xrefTerm.addToCollection("crossReferences", item.getIdentifier());
                store(xrefTerm);
            }
            item.addToCollection("crossReferences", refId);
        }
        OboTerm oboterm = term;
        if (!StringUtils.isEmpty(oboterm.getNamespace())) {
            item.setAttribute("namespace", oboterm.getNamespace());
        }
        if (!StringUtils.isEmpty(oboterm.getDescription())) {
            item.setAttribute("description", oboterm.getDescription());
        }
        item.setAttribute("obsolete", "" + oboterm.isObsolete());
        // Term is its own parent
        item.addToCollection("parents", item);
    }

}
