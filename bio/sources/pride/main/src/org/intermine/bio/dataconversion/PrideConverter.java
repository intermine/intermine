package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

import java.io.Reader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * DataConverter to parse pride data into items.
 * @author Dominik Grimm and Michael Menden
 */
public class PrideConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    //the following maps should avoid that not unnecessary objects will be created
    private Map<String, String> mapOrganism = new HashMap<String, String>();
    private Map<String, String> mapPublication = new HashMap<String, String>();
    private Map<String, String> mapGOTerm = new HashMap<String, String>();
    private Map<String, String> mapProtein = new HashMap<String, String>();
    private Map<String, String> mapPSIMod = new HashMap<String, String>();
    private Map<String, String> mapCellType = new HashMap<String, String>();
    private Map<String, String> mapDisease = new HashMap<String, String>();
    private Map<String, String> mapTissue = new HashMap<String, String>();
    private Map<String, String> mapProject = new HashMap<String, String>();
    private Map<String, String> mapPeptide = new HashMap<String, String>();

    private final PrideIndexFasta proteinIndex = new PrideIndexFasta("/shared/data/pride/fasta/");

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PrideConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {

        PrideHandler handler = new PrideHandler(getItemWriter());

        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    /**
     * Handles xml file
     */
    private class PrideHandler extends DefaultHandler
    {
        //private data fields
        private ItemWriter writer;
        private Stack<String> stack = new Stack<String>();
        private String attName = null;
        private StringBuffer attValue = null;


        //private Items
        private Item itemOrganism = null;
        private Item itemPublication = null;
        private Item itemGOTerm = null;
        private Item itemProtein = null;
        private Item itemPSIMod = null;
        private Item itemCellType = null;
        //private Item itemMedSubject = null;
        private Item itemDisease = null;
        private Item itemTissue = null;
        private Item itemProteinIdentification = null;
        private Item itemPrideProject = null;
        private Item itemPrideExperiment = null;
        private Item itemPeptide = null;
        private Item itemPeptideModification = null;

        //private reference strings
        private String[]     proteinAccessionId     = null;
        private String[]    proteinIdentifierId = null;

        //private bool
        private boolean swissprotFlag = false;

        private PridePeptideData peptideData = new PridePeptideData();

        private Stack<PridePeptideData> stackPeptides = new Stack<PridePeptideData>();

        //private ReferenceList
        private ReferenceList listPeptides = null;

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public PrideHandler(ItemWriter writer) {
            this.writer = writer;
        }



        /**
         * if only attName is set, the the attValue is between the tags and must
         *  store in endElement().
         * if the value is included in the tag, you have to set the attribut now.
         * Objects (=items) have to be builded here.
         *
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            attName = null;
            // <ExperimentCollection><Experiment>
            if (qName.equals("Experiment")) {
                itemPrideExperiment = createItem("PrideExperiment");
            }
            // <ExperimentCollection><Experiment><ExperimentAccession>
            else if (qName.equals("ExperimentAccession")) {
                attName = "accessionId";
            }
            // <ExperimentCollection><Experiment><Title>
            else if (qName.equals("Title")) {
                attName = "title";
            }
            // <ExperimentCollection><Experiment><ShortLabel>
            else if (qName.equals("ShortLabel")) {
                attName = "shortLabel";
            }
            // <ExperimentCollection><Experiment><Protocol><ProtocolName>
            else if (qName.equals("ProtocolName") && stack.peek().equals("Protocol")) {
                attName = "protocolName";
            }
            /**PrideProject*/
            //<ExperimentCollection><Experiment><additional><cvParam>
            else if (qName.equals("cvParam")  && stack.peek().equals("additional")
                     && attrs.getValue("name").equals("Project")
                     && attrs.getValue("accession").equals("PRIDE:0000097") && stack.size() == 3) {
                storeProject(attrs);
            }
            /**ProteinIdentification*/
            // <GelFreeIdentification || TwoDimensionalIdentification>
            else if (qName.equals("GelFreeIdentification")
                    || qName.equals("TwoDimensionalIdentification")) {
                itemProteinIdentification = createItem("ProteinIdentification");
            }
            // <GelFreeIdentification || TwoDimensionalIdentification><Score>
            else if (qName.equals("Score") && (stack.peek().equals("GelFreeIdentification")
                        || stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "score";
            }
            // <GelFreeIdentification || TwoDimensionalIdentification><Threshold>
            else if (qName.equals("Threshold")  && (stack.peek().equals("GelFreeIdentification")
                        || stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "threshold";
            }
            // <GelFreeIdentification || TwoDimensionalIdentification><SearchEngine>
            else if (qName.equals("SearchEngine")  && (stack.peek().equals("GelFreeIdentification")
                        || stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "searchEngine";
            }
            // <GelFreeIdentification || TwoDimensionalIdentification><SpliceIsoform>
            else if (qName.equals("SpliceIsoform") && (stack.peek().equals("GelFreeIdentification")
                        || stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "spliceIsoform";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><SpectrumReference>
            else if (qName.equals("SpectrumReference")
                    && (stack.peek().equals("GelFreeIdentification")
                        || stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "spectrumReference";
            }
            // <GelFreeIdentification || TwoDimensionalIdentification><SequenceCoverage>
            else if (qName.equals("SequenceCoverage")
                    && (stack.peek().equals("GelFreeIdentification")
                        || stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "sequenceCoverage";
            }
            // <TwoDimensionalIdentification><MolecularWeight>
            else if (qName.equals("MolecularWeight")
                    && (stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "molecularWeight";
            }
            //<TwoDimensionalIdentification><pI>
            else if (qName.equals("pI") && (stack.peek().equals("TwoDimensionalIdentification"))) {
                attName = "pI";
            }
            //<TwoDimensionalIdentification><Gel><GelLink>
            else if (qName.equals("GelLink") && stack.peek().equals("Gel")) {
                attName = "gelLink";
            }
            // <TwoDimensionalIdentification><GelLocation><XCoordinate>
            else if (qName.equals("XCoordinate") && stack.peek().equals("GelLocation")) {
                attName = "gelXCoordinate";
            }
            // <TwoDimensionalIdentification><GelLocation><YCoordinate>
            else if (qName.equals("YCoordinate") && stack.peek().equals("GelLocation")) {
                attName = "gelYCoordinate";
            }
            /** protein class*/
           // <GelFreeIdentification || TwoDimensionalIdentification><additional><cvParam>
           else if (qName.equals("cvParam") && stack.peek().equals("additional")
                    && attrs.getValue("accession").toString().equals("PRIDE:0000165")) {
                      //start swissprotFlag identification
               initProtein(attrs);
           }
            /**peptide class*/
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
            else if (qName.equals("Sequence") && (stack.peek().equals("PeptideItem"))) {
                   attName = "sequence";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Start>
            else if (qName.equals("Start") && (stack.peek().equals("PeptideItem"))) {
                   attName = "start";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><End>
            else if (qName.equals("End") && (stack.peek().equals("PeptideItem"))) {
                   attName = "end";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<SpectrumReference>
            else if (qName.equals("SpectrumReference") && (stack.peek().equals("PeptideItem"))) {
                   attName = "spectrumReference";
            }
            /**PeptideModification class*/
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><ModificationItem>
            else if (qName.equals("ModificationItem") && stack.peek().equals("PeptideItem")) {
                  itemPeptideModification = createItem("PeptideModification");
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<ModificationItem><ModLocation>
            else if (qName.equals("ModLocation") && stack.peek().equals("ModificationItem")) {
                   attName = "location";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<ModificationItem><ModAccession>
            else if (qName.equals("ModAccession") && stack.peek().equals("ModificationItem")) {
                   attName = "accessionId";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<ModificationItem><ModDatabase>
            else if (qName.equals("ModDatabase") && stack.peek().equals("ModificationItem")) {
                   attName = "modDB";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<ModificationItem><ModDatabaseVersion>
            else if (qName.equals("ModDatabaseVersion")
                    && stack.peek().equals("ModificationItem")) {
                   attName = "modDBVersion";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<ModificationItem><ModMonoDelta>
            else if (qName.equals("ModMonoDelta") && stack.peek().equals("ModificationItem")) {
                   attName = "modMonoDelta";
            }
            //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem>
            //<ModificationItem><ModAvgDelta>
            else if (qName.equals("ModAvgDelta") && stack.peek().equals("ModificationItem")) {
                   attName = "modAvgDelta";
            }
            /**Organism class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("NEWT")) {
                storeOrganism(attrs);
            }
            /**Publication class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("PubMed")) {
                storePublication(attrs);
            }
            /**GOTerm class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("GO")) {
                storeGOTerm(attrs);
            }
            /**PSIMod class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("PSI-MOD")) {
                storePSIMod(attrs);
            }
            /**CellType class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("CL")) {
                storeCellType(attrs);
            }
            /**Disease class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("DOID")) {
                storeDisease(attrs);
            }
            /**Tissue class*/
            //<mzData><description><admin><sampleDescription><cvParam>
            else if (qName.equals("cvParam") && stack.peek().equals("sampleDescription")
                     && attrs.getValue("cvLabel").equals("BTO")) {
                storeTissue(attrs);
            }
            super.startElement(uri, localName, qName, attrs);
            stack.push(qName);
            attValue = new StringBuffer();
        }


        private void storeTissue(Attributes attrs) throws SAXException {
            String refId;
            //is the organism available?
            if (mapTissue.get(attrs.getValue("name")) == null) {
                itemTissue = createItem("Tissue");
                itemTissue.setAttribute("name", attrs.getValue("name").toString());
                refId = itemTissue.getIdentifier();
                // put onto hashMap taxonId (=key) and identifier (=value)
                mapTissue.put(attrs.getValue("name"), refId);
                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemTissue));
                    itemTissue = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                refId = mapTissue.get(attrs.getValue("name"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("tissue", refId));
        }


        private void storeDisease(Attributes attrs) throws SAXException {
            String refId;
            //is the organism available?
            if (mapDisease.get(attrs.getValue("name")) == null) {
                itemDisease = createItem("PrideDisease");
                itemDisease.setAttribute("name", attrs.getValue("name").toString());
                refId = itemDisease.getIdentifier();
                // put onto hashMap taxonId (=key) and identifier (=value)
                mapDisease.put(attrs.getValue("name"), refId);

                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemDisease));
                    itemDisease = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                refId = mapDisease.get(attrs.getValue("name"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("disease", refId));
        }


        private void storeCellType(Attributes attrs) throws SAXException {
            String refId;
            //is the organism available?
            if (mapCellType.get(attrs.getValue("name")) == null) {
                itemCellType = createItem("CellType");
                itemCellType.setAttribute("name", attrs.getValue("name").toString());
                refId = itemCellType.getIdentifier();
                // put onto hashMap taxonId (=key) and identifier (=value)
                mapCellType.put(attrs.getValue("name"), refId);
                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemCellType));
                    itemCellType = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                refId = mapCellType.get(attrs.getValue("name"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("cellType", refId));
        }


        private void storePSIMod(Attributes attrs) throws SAXException {
            String refId;
            //is the organism available?
            if (mapPSIMod.get(attrs.getValue("name")) == null) {
                itemPSIMod = createItem("PSIMod");
                itemPSIMod.setAttribute("name", attrs.getValue("name").toString());
                refId = itemPSIMod.getIdentifier();
                // put onto hashMap taxonId (=key) and identifier (=value)
                mapPSIMod.put(attrs.getValue("name"), refId);
                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemPSIMod));
                    itemPSIMod = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                refId = mapPSIMod.get(attrs.getValue("name"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("psiMod", refId));
        }


        private void storeGOTerm(Attributes attrs) throws SAXException {
            String refId;
            //is the organism available?
            if (mapGOTerm.get(attrs.getValue("name")) == null) {
                // put onto hashMap taxonId (=key) and identifier (=value)
                itemGOTerm = createItem("GOTerm");
                itemGOTerm.setAttribute("name", attrs.getValue("name").toString());
                refId = itemGOTerm.getIdentifier();
                mapGOTerm.put(attrs.getValue("name"), refId);
                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemGOTerm));
                    itemGOTerm = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                refId = mapGOTerm.get(attrs.getValue("name"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("goTerm", refId));
        }


        private void storePublication(Attributes attrs) throws SAXException {
            String refId;
            //is the organism available?
            if (mapPublication.get(attrs.getValue("accession")) == null) {
                // put onto hashMap taxonId (=key) and identifier (=value)
                itemPublication = createItem("Publication");
                // store in itemOrganism the taxonId
                itemPublication.setAttribute("pubMedId",
                                             attrs.getValue("accession").toString());
                refId = itemPublication.getIdentifier();
                mapPublication.put(attrs.getValue("accession"), refId);
                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemPublication));
                    itemPublication = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                refId = mapPublication.get(attrs.getValue("accession"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("publication", refId));
        }


        private void storeOrganism(Attributes attrs) throws SAXException {
            String accId;
            //is the organism available?
            if (mapOrganism.get(attrs.getValue("accession")) == null) {
                // put onto hashMap taxonId (=key) and identifier (=value)
                itemOrganism = createItem("Organism");
                // store in itemOrganism the taxonId
                itemOrganism.setAttribute("taxonId", attrs.getValue("accession"));
                accId = itemOrganism.getIdentifier();
                mapOrganism.put(attrs.getValue("accession"), accId);
                try {
                    //store as object in file
                    writer.store(ItemHelper.convert(itemOrganism));
                    itemOrganism = null;
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            else {
                //store in item the right identiefier
                accId = mapOrganism.get(attrs.getValue("accession"));
            }
            //set reference
            itemPrideExperiment.addReference(new Reference("organism", accId));
        }


        private void initProtein(Attributes attrs) {
            PrideExpression exp = new PrideExpression(attrs.getValue("value").toString());

               proteinAccessionId = new String[exp.getAccessionCounter()];
               proteinIdentifierId = new String[exp.getIdentifierCounter()];
                  //store accessionIds
                  if (exp.findSwissport()) {
                      swissprotFlag = true;
                      proteinAccessionId = exp.getAccession();
                      proteinIdentifierId = exp.getIdentifier();
                  }
        }


        private void storeProject(Attributes attrs) throws SAXException {
            String refId;
            if (attrs.getValue("value") != null) {
                if (!attrs.getValue("value").toString().equals("")) {
                    if (mapProject.get(attrs.getValue("value").toString()) == null) {
                        itemPrideProject = createItem("PrideProject");
                          itemPrideProject.setAttribute("title",
                                           attrs.getValue("value").toString());
                        refId = itemPrideProject.getIdentifier();
                        mapProject.put(attrs.getValue("value").toString(), refId);

                        try {
                            writer.store(ItemHelper.convert(itemPrideProject));
                            itemPrideProject = null;
                        } catch (ObjectStoreException e) {
                            throw new SAXException(e);
                        }
                    }
                    else {
                        refId = mapProject.get(attrs.getValue("value").toString());
                    }
                    itemPrideExperiment.addReference(new Reference("prideProject", refId));
              }
           }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) {

            if (attName != null) {

                // DefaultHandler may call this method more than once for a single
                // attribute content -> hold text & create attribute in endElement
                while (length > 0) {
                    boolean whitespace = false;
                    switch(ch[start]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        whitespace = true;
                        break;
                    default:
                        break;
                    }
                    if (!whitespace) {
                        break;
                    }
                    ++start;
                    --length;
                }

                if (length > 0) {
                    StringBuffer s = new StringBuffer();
                    s.append(ch, start, length);
                    attValue.append(s);
                }
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {

            super.endElement(uri, localName, qName);

            try {
                stack.pop();
                /**
                 * PrideExperiment start
                 */
                //<ExperimentCollection><Experiment><ExperimentAccession>
                if (qName.equals("ExperimentAccession")) {
                    itemPrideExperiment.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><Title>
                else if (qName.equals("Title")) {
                    itemPrideExperiment.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><ShortLabel>
                else if (qName.equals("ShortLabel")) {
                    itemPrideExperiment.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                else if (qName.equals("Experiment")) {
                    writer.store(ItemHelper.convert(itemPrideExperiment));
                    itemPrideExperiment = null;
                }
                /**
                 * ProteinIndentification
                 */
                // <ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><Score>
                else if (qName.equals("Score")
                        && (stack.peek().equals("GelFreeIdentification")
                                || stack.peek().equals("TwoDimensionalIdentification"))) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><Threshold>
                else if (qName.equals("Threshold")
                        && (stack.peek().equals("GelFreeIdentification")
                                || stack.peek().equals("TwoDimensionalIdentification"))) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><SearchEngine>
                else if (qName.equals("SearchEngine")
                        && (stack.peek().equals("GelFreeIdentification")
                                || stack.peek().equals("TwoDimensionalIdentification"))) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><SpliceIsoform>
                else if (qName.equals("SpliceIsoform")
                        && (stack.peek().equals("GelFreeIdentification")
                                || stack.peek().equals("TwoDimensionalIdentification"))) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><SpectrumReference>
                else if (qName.equals("SpectrumReference")
                        && (stack.peek().equals("GelFreeIdentification")
                                || stack.peek().equals("TwoDimensionalIdentification"))) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><SequenceCoverage>
                else if (qName.equals("SequenceCoverage")
                        && (stack.peek().equals("GelFreeIdentification")
                                || stack.peek().equals("TwoDimensionalIdentification"))) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><MolecularWeight>
                else if (qName.equals("MolecularWeight")
                        && stack.peek().equals("TwoDimensionalIdentification")) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><pI>
                else if (qName.equals("pI")
                        && stack.peek().equals("TwoDimensionalIdentification")) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                // <ExperimentCollection><Experiment><TwoDimensionalIdentification><Gel><GelLink>
                else if (qName.equals("GelLink")
                        && stack.peek().equals("Gel")) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //<TwoDimensionalIdentification><GelLocation><XCoordinate>
                else if (qName.equals("XCoordinate")
                        && stack.peek().equals("GelLocation")) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //<TwoDimensionalIdentification><GelLocation><YCoordinate>
                else if (qName.equals("YCoordinate")
                        && stack.peek().equals("GelLocation")) {
                    itemProteinIdentification.setAttribute(attName, attValue.toString());
                }
               /**
                * Peptide class
                */
                //<ExperimentCollection><Experiment>
                //GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("Sequence")
                            && stack.peek().equals("PeptideItem")) {
                    peptideData = new PridePeptideData();
                    peptideData.setSequence(attValue.toString());
                }
                //<ExperimentCollection><Experiment>
                //GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Start>
                else if (qName.equals("Start")
                            && stack.peek().equals("PeptideItem")) {
                    peptideData.setStartPos(Integer.parseInt(attValue.toString()));
                }
                //<ExperimentCollection><Experiment>
                //GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><End>
                else if (qName.equals("End")
                            && stack.peek().equals("PeptideItem")) {
                    peptideData.setEndPos(Integer.parseInt(attValue.toString()));
                }
                //<ExperimentCollection><Experiment>
                //GelFreeIdentification || TwoDimensionalIdentification>
                //<PeptideItem><SpectrumReference>
                else if (qName.equals("SpectrumReference")
                            && stack.peek().equals("PeptideItem")) {
                    peptideData.setSpecRef(Float.parseFloat(attValue.toString()));
                }
                else if (qName.equals("PeptideItem")) {
                    stackPeptides.push(peptideData);
                }
                /**
                 * PeptideModification class
                 */
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("ModLocation")
                             && stack.peek().equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                     itemPeptideModification.setAttribute(attName, attValue.toString());
                 }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("ModAccession")
                             && stack.peek().equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                     itemPeptideModification.setAttribute(attName, attValue.toString());
                 }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("ModDatabase")
                             && stack.peek().equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                     itemPeptideModification.setAttribute(attName, attValue.toString());
                 }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("ModDatabaseVersion")
                             && stack.peek().equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                     itemPeptideModification.setAttribute(attName, attValue.toString());
                 }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                else if (qName.equals("ModMonoDelta")
                             && stack.peek().equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                     itemPeptideModification.setAttribute(attName, attValue.toString());
                 }
                //<ExperimentCollection><Experiment>
                //<GelFreeIdentification || TwoDimensionalIdentification><PeptideItem><Sequence>
                 else if (qName.equals("ModAvgDelta")
                             && stack.peek().equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                     itemPeptideModification.setAttribute(attName, attValue.toString());
                 }
                 else if (qName.equals("ModificationItem")) {
                     // store in itemOrganism the taxonId
                    writer.store(ItemHelper.convert(itemPeptideModification));
                    itemPeptideModification = null;
                 }
               /**
                * Identification closing tag. Time to store all of the items
                */
               //<ExperimentCollection><Experiment>
               //<GelFreeIdentification || TwoDimensionalIdentification>
               else if (qName.equals("GelFreeIdentification")
                          || qName.equals("TwoDimensionalIdentification")) {
                   //Store Protein accessionId and identifier
                   //only if swissprotFlag accession id exists
                   storeSwissprotProteinsAndPeptides();
                   //store ProteinIdentification
                   storeProteinIdentification();
                }

            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }

        }


        private void storeSwissprotProteinsAndPeptides()
                throws ObjectStoreException {
            if (swissprotFlag) {
                   String refId = null;
                   for (int i = 0; i < proteinAccessionId.length; i++) {
                       //create new Item if no protein with the same accessionId exists
                       if (mapProtein.get(proteinAccessionId[i]) == null) {
                           itemProtein = createItem("Protein");
                           itemProtein.setAttribute("primaryAccession", proteinAccessionId[i]);

                           //get number of peptides
                           int stackSize = stackPeptides.size();
                           //store all peptides if no peptide with the same key exists
                               for (int j = 0; j < stackSize; j++) {
                                   peptideData = null;
                                   //get top peptide
                                   peptideData = stackPeptides.peek();
                                   //store protein
                                   storeProtein(i);
                                   //remove top element at stack
                                   stackPeptides.pop();
                               }
                               //get identifer for protein and store protein
                               refId = itemProtein.getIdentifier();
                               mapProtein.put(proteinAccessionId[i], refId);
                               writer.store(ItemHelper.convert(itemProtein));
                               itemProtein = null;
                            listPeptides = null;
                            itemPeptide = null;

                       } else {
                           refId = mapProtein.get(proteinAccessionId[i]);
                       }
                       //set reference
                       itemProteinIdentification.addReference(new Reference("protein", refId));
                   }
                   swissprotFlag = false;
               }
        }


        private void storeProteinIdentification() throws ObjectStoreException {
            itemProteinIdentification.addReference(new Reference("prideExperiment",
                                                          itemPrideExperiment.getIdentifier()));
               writer.store(ItemHelper.convert(itemProteinIdentification));
               itemProteinIdentification = null;
        }

        private void storeProtein(int i) throws ObjectStoreException {
            if (proteinIndex.getProtein(proteinAccessionId[i]) != null) {
                //calculate new start and end positions
                PrideCalculatePos calcPos = initPeptide(i);
                if (listPeptides.getRefIds().isEmpty()) {
                    itemProtein.addCollection(listPeptides);
                    }
                //if there are new start and end positions then do...
                while (calcPos.hasNext()) {
                    if (mapPeptide.get(peptideData.getKey()) == null) {
                        //createItem and store the correct data
                        createPeptide(calcPos);
                        //else if a peptide already exists get correct identifer
                    } else {
                        listPeptides.addRefId(mapPeptide.get(peptideData.getKey()));
                        calcPos.remove();
                    }
                 }
             }
        }

        private void createPeptide(PrideCalculatePos calcPos)
                throws ObjectStoreException {
            String refId;
            itemPeptide = createItem("Peptide");
            itemPeptide.setAttribute("sequence", peptideData.getSequence());
            itemPeptide.setAttribute("spectrumReference", Float.toString(peptideData.getSpecRef()));
            itemPeptide.setAttribute("start", Integer.toString(peptideData.getStartPos()));
            itemPeptide.setAttribute("end", Integer.toString(peptideData.getEndPos()));
            refId = itemPeptide.getIdentifier();
            //put new peptide to map
            mapPeptide.put(peptideData.getKey(), refId);
            //set identifiers
            itemPeptide.addReference(new Reference("proteinIdentification",
                                                    itemProteinIdentification.getIdentifier()));
            //itemPeptide.addReference(new Reference("protein", itemProtein.getIdentifier()));
            listPeptides.addRefId(refId);
            //write item
            writer.store(ItemHelper.convert(itemPeptide));
            //remove current iterator object
            calcPos.remove();
        }

        private PrideCalculatePos initPeptide(int i) {
            PrideCalculatePos calcPos = new PrideCalculatePos(proteinIndex.getProtein(
                                                               proteinAccessionId[i]), peptideData);
            listPeptides = new ReferenceList("peptides", new ArrayList<String>());
            return calcPos;
        }

        protected Item createItem(String className) {
            return PrideConverter.this.createItem(className);
        }
    }
}
