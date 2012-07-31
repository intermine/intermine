package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the AdinetaGff dataset via GFF files.
 */

public class AdinetaGffGFF3RecordHandler extends GFF3RecordHandler
{

    /**
     * Create a new AdinetaGffGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public AdinetaGffGFF3RecordHandler (Model model) {
        super(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        // This method is called for every line of GFF3 file(s) being read.  Features and their
        // locations are already created but not stored so you can make changes here.  Attributes
        // are from the last column of the file are available in a map with the attribute name as
        // the key.   For example:
        //
        //     Item feature = getFeature();
        //     String symbol = record.getAttributes().get("symbol");
        //     feature.setAttribute("symbol", symbol);
        //
        // Any new Items created can be stored by calling addItem().  For example:
        // 
        //     String geneIdentifier = record.getAttributes().get("gene");
        //     gene = createItem("Gene");
        //     gene.setAttribute("primaryIdentifier", geneIdentifier);
        //     addItem(gene);
        //
        // You should make sure that new Items you create are unique, i.e. by storing in a map by
        // some identifier. 
        
        //TODO Handle repeatmasker ID=Av-lea1-B_288L15:hsp:0;Parent=Av-lea1-B_288L15:hit:0;Name=species:(CCA)n_genus:Simple_repeat;Target=species:(CCA)n_genus:Simple_repeat 3
        //
        //TODO Handle blastx ID=Av-lea1-B_288L15:hit:14;Name=sp|P36609|NCS2_CAEEL;Target=sp|P36609|NCS2_CAEEL 1 172 +;
        // ID=Av-lea1-B_288L15:hsp:18;Parent=Av-lea1-B_288L15:hit:13;Name=sp|Q28IM6|HPCL1_XENTR;Target=sp|Q28IM6|HPCL1_XENTR 115 170 +;
        // String ID = record.getAttributes().get("ID").get(0);
        // String[] IDsplit = ID.split(":");
        // getFeature().setAttribute("primaryIdentifer", IDsplit[0]);
        //        
        // String Name = record.getAttributes().get("Name").get(0);
        // String[] NameSplit = Name.split(":");
        // Item protein = createItem("Protein");
        // protein.setAttribute("primaryAccession",NameSplit[1]);

        
        //
        //TODO Handle CDS ID=maker-Av-lea1-B_288L15-snap-gene-0.9-mRNA-1:cds:0;Parent=maker-Av-lea1-B_288L15-snap-gene-0.9-mRNA-1;
        //
        //TODO Handle exon ID=maker-Av-lea1-B_288L15-snap-gene-0.9-mRNA-1:exon:2;Parent=maker-Av-lea1-B_288L15-snap-gene-0.9-mRNA-1;
        //
        //TODO Handle gene ID=maker-Av-lea1-B_288L15-snap-gene-0.6;Name=maker-Av-lea1-B_288L15-snap-gene-0.6;
        //
        //TODO Handle mRNA
        //
        //TODO load sequence
        //
    }

}
