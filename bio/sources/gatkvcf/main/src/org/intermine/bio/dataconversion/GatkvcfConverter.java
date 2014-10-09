package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2013 Phytozome
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.util.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.util.FormattedTextParser;

/**
 * 
 * @author 
 */
public class GatkvcfConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "GATK VCF Data";
  private static final String DATA_SOURCE_NAME = "Phytozome";
  private static final Logger LOG = Logger.getLogger(GatkvcfConverter.class);

  // hashes of inserted things...
  // The chromosomes that we reference. chromosomes keyed by primaryIdentifier
  private HashMap<String,String> chrMap = new HashMap<String,String>();
  
  // the one organism we're working on. proteomeId is
  // set by setter. organism is the registered Item
  private Integer proteomeId;
  private Item organism = null;
  // do we make the links with the sample in the integration step?
  // or the postprocessing step? true means in the integration step
  // doing this in the integration step is slower, but more 'intermine-y'
  private boolean makeLinks = false;
  // the referenced consequences and consequence type types.
  private Map<String,String> consequenceMap = new HashMap<String,String>();
  private Map<String,String> consequenceTypeMap = new HashMap<String,String>();
  // referenced genes and transcripts.
  private Map<String,String> geneMap = new HashMap<String,String>();
  private Map<String,String> mRNAMap = new HashMap<String,String>();
  // we'll get this from the header. When parsing, we need to keep these in order
  private ArrayList<String> sampleList = new ArrayList<String>();
  // what we expect to see in the vcf header
  final static String[] expectedHeaders = {"#CHROM","POS","ID","REF","ALT",
    "QUAL","FILTER","INFO","FORMAT"};
  final static int formatPosition = 8;
  private Pattern effPattern;
  // we'll use this for printing log message when we go to a new chromosome
  private String lastChromosome = null;

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public GatkvcfConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    effPattern = Pattern.compile("(\\w+)\\((.+)\\)");
  }

  public void setProteomeId(String proteome) {
    try {
      proteomeId = Integer.valueOf(proteome);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Cannot find numerical proteome id for: " + proteome);
    }
  }
  
  public void setMakeLinks(String linksYesNo) {
    if (linksYesNo == null || linksYesNo.isEmpty() || 
        linksYesNo.equalsIgnoreCase("true") ) {
      makeLinks = true;
    } else {
      makeLinks = false;
    }
  }
  /**
   * the main event
   */
  public void process(Reader reader) throws Exception {
    File theFile = getCurrentFile();

    // register the organism if needed
    if ( organism == null ) {
      if (proteomeId != null ) {
        organism = createItem("Organism");
        organism.setAttribute("proteomeId", proteomeId.toString());
        try {
          store(organism);
        } catch (ObjectStoreException e) {
          throw new RuntimeException("failed to store organism with proteomeId: " +
                proteomeId, e);
        }
      } else {
        throw new BuildException("No taxon Id specified.");
      }
    }
    LOG.info("Processing file " + theFile.getName() + "...");
    
    if( !theFile.getName().endsWith(".vcf") ) {
      LOG.info("Ignoring file " + theFile.getName() + ". Not a SnpEff-processed GATK vcf file.");
    } else {
      // we need to open and find the header line. Since this starts with a "#", the
      // FormattedTextParser calls this a comment line and will not return it to us.
      // TODO: replace FormattedTextParser.
      BufferedReader in = new BufferedReader(new FileReader(theFile));
      String line;
      while ( (line = in.readLine()) != null) {
        if (line.startsWith("##")) continue;
        if (line.startsWith("#") ) {
          String[] fields = line.split("\\t");
          processHeader(fields);
          break;
        }
      }
      // make sure we processed the header at this point
      if (sampleList.size() == 0) {
        throw new BuildException("Cannot find sample names in vcf file.");
      }
      // now we can proceed with the FormattedTextParser
      Iterator<?> tsvIter;
      try {
        tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
      } catch (Exception e) {
        throw new BuildException("Cannot parse file: " + getCurrentFile(),e);
      }
      int ctr = 0;
      while (tsvIter.hasNext() ) {
        ctr++;
        String[] fields = (String[]) tsvIter.next();
        if (!processData(fields)) {
          return;
        }
        if ((ctr%100000) == 0) {
          LOG.info("Processed " + ctr + " lines...");
        }
      }
      LOG.info("Processed " + ctr + " lines.");
    }
  }

  private void processHeader(String[] header) throws BuildException
  {
    // here is what we expect for the first few columns. Complain if this
    // is not case;
    if (header.length < expectedHeaders.length + 1) {
      // there needs to be at least 1 sample. Or is it OK to have
      // a VCF file w/o samples?
      throw new BuildException("Unexpected length of header fields.");
    } else {
      for(int i=0;i<expectedHeaders.length;i++) {
        if ( header[i] == null || !header[i].equals(expectedHeaders[i]) ) {
          throw new BuildException("Unexpected item in header at position "+
             i+": "+header[i]);
        }
      }
      // we're going to be certain there are no duplicates.
      HashSet<String> sampleNameSet = new HashSet<String>();
      for(int i=expectedHeaders.length;i<header.length;i++) {
        if( sampleNameSet.contains(header[i]) ) {
          throw new BuildException("Duplicated sample in header name: "+ header[i]);
        }
        Item source = createItem("DiversitySample");
        source.setAttribute("name",header[i]);
        source.setReference("organism",organism);
        try {
        store(source);
        } catch (ObjectStoreException e) {
          throw new BuildException("Cannot store source " + header[i]);
        }
        sampleList.add(source.getIdentifier());
      }
    }
  }
  private boolean processData(String[] fields) throws BuildException
  {
    if (fields.length < expectedHeaders.length + 1) {
      throw new BuildException("Unexpected number of columns in VCF file.");
    }
    String chr = fields[0];
    
    if (lastChromosome==null || !chr.equals(lastChromosome)) {
      lastChromosome = chr;
      LOG.info("Processing "+chr);
    }

    Integer pos = new Integer(fields[1]);
    String name = fields[2];
    String ref = fields[3];
    String alt = fields[4];
    String quality = fields[5];
    String filter = fields[6];
    String info = fields[7];

    // create the chromosome if we haven't seen this before
    if (! chrMap.containsKey(chr) ) {
      Item chrItem = createItem("Chromosome");
      chrItem.setAttribute("primaryIdentifier",chr);
      chrItem.setReference("organism",organism);
      try {
        store(chrItem);
      } catch (ObjectStoreException e) {
        throw new BuildException("Cannot store chromosome item: " + e);
      }
      chrMap.put(chr,chrItem.getIdentifier());
    }
    // make and store the feature
    Item snp = createItem("SNP");
    snp.setReference("organism",organism);
    snp.setAttribute("reference",ref);
    snp.setAttribute("alternate", alt);
    try {
      // only add if a number. Silently ignore non-numbers
      Integer.parseInt(quality);
      snp.setAttribute("quality", quality);
    } catch ( NumberFormatException e) {}
    snp.setAttribute("name",name);
    snp.setAttribute("filter", filter);
    Integer nSamples = parseInfo(snp,info);
    if (nSamples != null && nSamples > 0) {
      snp.setAttribute("sampleCount", nSamples.toString());
    }
    // create and store the location
    makeLocation(chrMap.get(chr),snp.getIdentifier(),pos.toString(),
        Integer.toString(pos+ref.length()),"1",true);
    try {
      // and store the snp.
      store(snp);
    } catch (ObjectStoreException e) {
      throw new BuildException("Problem storing SNP: " + e);
    }

    if (makeLinks) {
      // process the genotype field. First we have attribute:attribute:attribute... 
      // and value:value:value... Convert these to attribute=value;attribute=value;...
      String[] attBits = fields[formatPosition].split(":");

      // look through the different genotype scores for column 9 onward.
      for(int col=expectedHeaders.length;col<fields.length;col++) {
        Boolean passField = null;
        Boolean genoField = null;
        String[] valBits = fields[col].split(":");
        StringBuffer genotype = new StringBuffer();
        StringBuffer format = new StringBuffer();
        if ( (valBits.length != attBits.length) && !fields[col].equals("./."))
          LOG.warn("Genotype fields have unexpected length.");
        for(int i=0; i< attBits.length && i<valBits.length;i++) {
          if (attBits[i].equals("GT")) {
            genoField = (valBits[i].equals("./."))?false:true;
            genotype = new StringBuffer(valBits[i]);
          } else {
            if (format.length() > 0) format.append(":");
            format.append(attBits[i] + "=" + valBits[i]);
            if (attBits[i].equals("FT")) {
              passField = (valBits[i].equals("PASS"))?true:false;
            }  
          }
        }
        if ((passField != null && passField) ||
            (passField == null && genoField != null && genoField)) {
          Item snpSource = createItem("SNPDiversitySample");
          if (!genotype.toString().isEmpty()) 
            snpSource.setAttribute("genotype", genotype.toString());
          if (!format.toString().isEmpty() )
            snpSource.setAttribute("format", format.toString());
          snpSource.setReference("diversitySample", sampleList.get(col-expectedHeaders.length));
          snpSource.setReference("snp",snp.getIdentifier());
          try {
            store(snpSource);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing SNPDiversitySample: " + e);
          }
        }
      }
    }

    return true;
  }
  /**
   * parseInfo
   * We're taking the INFO field of the VCF record, extracting the EFF tag and
   * stuffing the remainder into the info attribute. At first we were storing the
   * info field without all EFF tags. But now we're reverting to saving the full
   * field
   * @param snp The snp record being processed
   * @param info The info string
   */
  private Integer parseInfo(Item snp, String info) {
    Integer nSamples = null;
    if (info == null) return null;
    // initialize the new string buffer to be same length as old.
    StringBuffer newInfo = new StringBuffer(info.length());
    String[] bits = info.split(";");
    for (String keyVal: bits) {
      String[] kV = keyVal.split("=",2);
      if (kV[0].equals("EFF")) {
        // deal with the SnpEff calls.
        parseEff(snp,kV[1]);
      } else if (kV[0].equals("set")) {
        // we're going to drop the set= tags. But we will use it to determine
        // the number of samples
        nSamples = kV[1].split("-").length;
      } else {
        // if not EFF tag, append to newInfo
        if (newInfo.length() > 0) newInfo.append(';');
        newInfo.append(keyVal);
      }
    }
    // here is where we decide if we're storing the stripped down
    // info field, or the original stored newInfo.
    //snp.setAttribute("info",newInfo.toString());
    // but now were saving the original (without the set tag)
    snp.setAttribute("info",info);
    return nSamples;
  }

  private void parseEff(Item snp, String eff) {
    if (eff == null) return;
    
    for (String bit : eff.split(",") ) {
      Matcher match = effPattern.matcher(bit);
      if (match.matches() ) {
        String cType = match.group(1);
        String effect = match.group(2);
        String[] fields = effect.split("\\|");
        if ( fields.length < 9) return;
        if (!consequenceTypeMap.containsKey(cType) ) {
          Item conTypeItem = createItem("ConsequenceType");
          conTypeItem.setAttribute("type", cType);
          try {
            store(conTypeItem);
          } catch (ObjectStoreException e) {
            throw new BuildException("Cannot store consequencetype: " + e);
          }
          consequenceTypeMap.put(cType,conTypeItem.getIdentifier());
        }

        // has this consequence been seen before?
        // first, construct the key to the map
        StringBuffer conKey = new StringBuffer(cType);
        conKey.append(":");
        if (fields[3] != null) conKey.append(fields[3]);
        conKey.append(":");
        if (fields[5] != null) conKey.append(fields[5]);
        conKey.append(":");
        if (fields[8] != null) conKey.append(fields[8]);
        
        if (!consequenceMap.containsKey(conKey.toString()) ) {
          Item con = createItem("Consequence");
          con.setReference("type",consequenceTypeMap.get(cType));
          if (fields[3] != null && fields[3].length() > 0) {
            con.setAttribute("substitution", fields[3]);
          }
          if (fields[5] != null && fields[5].length() > 0 ) {
            String geneName = getGene(fields[5]);
            if (geneName != null) con.setReference("gene", geneName);
          }
          if (fields[8] != null && fields[8].length() > 0 ) {
            String transName = getMRNA(fields[8]);
            if (transName != null) con.setReference("transcript",transName);
          }
          try {
            store(con);
          } catch (ObjectStoreException e) {
            throw new BuildException("Cannot store consequencetype: " + e);
          }
          consequenceMap.put(conKey.toString(),con.getIdentifier());
        }
        String conID = consequenceMap.get(conKey.toString());
        snp.addToCollection("consequences", conID);
      }
    }
  }  
  private String getGene(String gene_name) {
    if (!geneMap.containsKey(gene_name)) {
      Item gene = createItem("Gene");
      gene.setAttribute("primaryIdentifier", gene_name);
      gene.setReference("organism", organism);
      try {
        store(gene);
      } catch (ObjectStoreException e) {
        throw new BuildException("Cannot store gene object " +e.getMessage());
      }
      geneMap.put(gene_name,gene.getIdentifier());
    }
    return geneMap.get(gene_name);
  }
  private String getMRNA(String mrna_name) {
    if (!mRNAMap.containsKey(mrna_name)) {
      Item mRNA = createItem("MRNA");
      mRNA.setAttribute("primaryIdentifier", mrna_name);
      mRNA.setReference("organism", organism);
      try {
        store(mRNA);
      } catch (ObjectStoreException e) {
        throw new BuildException("Cannot store mRNA object " +e.getMessage());
      }
      mRNAMap.put(mrna_name,mRNA.getIdentifier());
    }
    return mRNAMap.get(mrna_name);
  }
}
