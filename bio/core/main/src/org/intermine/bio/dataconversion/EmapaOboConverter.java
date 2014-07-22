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
 * Sets EMAPA attributes starts_at and ends_at
 * Requires the EMAPA.obo file to be modified
 * "relationship: starts_at TS01"
 * would need to become
 * "starts_at: TS01"
 * this allows the starts_at value to be parsed as tagValue
 * These two sed commands will do this
 * sed -i 's/relationship: ends_at/ends_at:/g' EMAPA.obo
 * sed -i 's/relationship: starts_at/starts_at:/g' EMAPA.obo
 * @author Steve Neuhauser
 * @see OboConverter
 */
public class EmapaOboConverter extends OboConverter implements OboConverterInterface {
private static final Logger LOG = Logger.getLogger(DataConverter.class);

public EmapaOboConverter(ItemWriter writer, Model model, String dagFilename, String dagName,
                        String url, String termClass) {

	super(writer,model,dagFilename,dagName,url,termClass);
}

    /**
     * Override base class method to set stage min and max attributes (startsAt, endsAt).
     *
     * @param termId the term id
     * @param item the Item created (see termClass field for type)
     * @param term the source DagTerm
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void configureItem(String termId, Item item, OboTerm term)
        throws ObjectStoreException {

	super.configureItem(termId, item, term);

        // this is the spcific EMAPA stuff 
        if(  term.getId()!= null && term.getId().indexOf("EMAPA") != -1){
           try{
             String starts_at =(String) ((List)term.getTagValues().get("starts_at")).get(0);
             String ends_at =(String) ((List)term.getTagValues().get("ends_at")).get(0);
             item.addAttribute(new Attribute("startsAt", starts_at));
             item.addAttribute(new Attribute("endsAt", ends_at));
           }catch(Exception e){}
        }
    }

    /**
     * Override base class method to skip this relation if it is for a descendant whose stage range
     * does NOT overlap the ancestor's. This is possible in the EMAPA.
     *
     * @param oboRelation a relation record
     * @throws ObjectStoreException if an error occurs while writing to the itemWriter
     */
    protected void processRelation(OboRelation oboRelation)
    throws ObjectStoreException {
	Item p = nameToTerm.get(oboRelation.getParentTermId());
	Item c = nameToTerm.get(oboRelation.getChildTermId());
	if(p!=null && c !=null){
	  int ps = Integer.parseInt(p.getAttribute("startsAt").getValue());
	  int pe = Integer.parseInt(p.getAttribute("endsAt").getValue());
	  int cs = Integer.parseInt(c.getAttribute("endsAt").getValue());
	  int ce = Integer.parseInt(c.getAttribute("endsAt").getValue());
	  if( ps <= ce && pe >= cs )
		super.processRelation(oboRelation);
	  else
	      LOG.info("Skipped non-overlapping descendand/ancestor relation. desc="
	      +oboRelation.getChildTermId()+" anc="+oboRelation.getParentTermId());
	}
    }


}
