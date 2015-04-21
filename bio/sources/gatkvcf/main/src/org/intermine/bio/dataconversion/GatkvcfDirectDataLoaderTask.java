package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Consequence;
import org.intermine.model.bio.ConsequenceType;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.DiversitySample;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.MRNA;
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SNP;
import org.intermine.model.bio.Genotype;
import org.intermine.model.bio.SOTerm;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.FormattedTextParser;

/**
 * A DirectDataLoader for Phytozome diversity data. This skips the items step completely
 * and creates/store InterMineObjects directly, providing significant speed increase and
 * removing need for a separate post-processing step.
 * 
 * Skipping the items step means that queries for merging objects in the target database
 * are run individually rather than in batches. This is slower but here the number of
 * objects being merged (organism, genes, mRNAs, etc) is very small compared to the total
 * data size. 
 * 
 * @author Richard Smith
 *
 */
public class GatkvcfDirectDataLoaderTask extends FileDirectDataLoaderTask {
	// NOTE if DataSet and DataSource aren't important in the webapp could disable
	// creating and setting references to save some disk writing.
	private static final String DATASET_TITLE = "GATK VCF Data";
	private static final String DATA_SOURCE_NAME = "Phytozome";
	private static final Logger LOG = Logger.getLogger(GatkvcfConverter.class);

	// the one organism we're working on. proteomeId is set by setter.
	private Integer proteomeId;
	
	// we'll get this from the header. When parsing, we need to keep these in order
	final static String[] expectedHeaders = { "#CHROM", "POS", "ID", "REF",
			"ALT", "QUAL", "FILTER", "INFO", "FORMAT" };
	final static int formatPosition = 8;
	private Pattern effPattern = Pattern.compile("(\\w+)\\((.+)\\)");
	// we'll use this for printing log message when we go to a new chromosome
	private String lastChromosome = null;
	
	// keep ids of created objects. ProxyReference is a wrapper for a stored object id and is
	// sufficient to create a reference to that object, saves keeping full objects in memory.
	private ProxyReference orgRef = null;
	private DataSet dataSet = null;
	private DataSource dataSource = null;
	private ProxyReference ontologyRef;
	private List<ProxyReference> samples = new ArrayList<ProxyReference>();
  private List<String> sampleNames = new ArrayList<String>();
	private Map<String, ProxyReference> consequenceTypes = new HashMap<String, ProxyReference>();
	private Map<String, ProxyReference> genes = new HashMap<String, ProxyReference>();
	private Map<String, ProxyReference> mrnas = new HashMap<String, ProxyReference>();
	private Map<String, ProxyReference> chromosomes = new HashMap<String, ProxyReference>();
	private Map<String, ProxyReference> soTerms = new HashMap<String, ProxyReference>();
	
	// consequences are added to a collection so need to keep actual objects not ProxyReference
	private Map<String, Consequence> consequences = new HashMap<String, Consequence>();
    
	public void setProteomeId(String proteome) {
		try {
			proteomeId = Integer.valueOf(proteome);
		} catch (NumberFormatException e) {
			throw new RuntimeException(
					"Cannot find numerical proteome id for: " + proteome);
		}
	}

	/**
	 * Called by parent process method for each file found
	 *
	 * {@inheritDoc}
	 */
	public void processFile(File theFile) {
		String message = "Processing file: " + theFile.getName();
		System.out.println(message);
		LOG.info(message);

		if (!theFile.getName().endsWith(".vcf")) {
			LOG.info("Ignoring file " + theFile.getName()
					+ ". Not a SnpEff-processed GATK vcf file.");
		} else {
		  // prefill the gene/mrna/chromsome proxy references.
		  preFill(genes,Gene.class);
		  preFill(mrnas,MRNA.class);
		  preFill(chromosomes,Chromosome.class);
		  
			// we need to open and find the header line. Since this starts with
			// a "#", the
			// FormattedTextParser calls this a comment line and will not return
			// it to us.
			// TODO: replace FormattedTextParser.
			try {
				BufferedReader in = new BufferedReader(new FileReader(theFile));
				String line;
				while ((line = in.readLine()) != null) {
					if (line.startsWith("##"))
						continue;
					if (line.startsWith("#")) {
						String[] fields = line.split("\\t");
						processHeader(fields);
						break;
					}
				}
				in.close();
			} catch (IOException e) {
				throw new BuildException("Failed to open file: " + theFile, e);
			}
			// make sure we processed the header at this point
			if (samples.size() == 0) {
				throw new BuildException(
						"Cannot find sample names in vcf file.");
			}
			Iterator<?> tsvIter;
			try {
				FileReader reader = new FileReader(theFile);
				tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
			} catch (Exception e) {
				throw new BuildException("Cannot parse file: "
						+ theFile, e);
			}
			int ctr = 0;
			while (tsvIter.hasNext()) {
				ctr++;
				String[] fields = (String[]) tsvIter.next();
				try {
					if (!processData(fields)) {
						return;
					}
				} catch (ObjectStoreException e) {
					throw new BuildException("Error procesing data:", e);
				}
				if ((ctr % 100000) == 0) {
					LOG.info("Processed " + ctr + " lines...");
				}
			}
			LOG.info("Processed " + ctr + " lines.");
		}
	}


	private void processHeader(String[] header) throws BuildException {
		// here is what we expect for the first few columns. Complain if this
		// is not case;
		if (header.length < expectedHeaders.length + 1) {
			// there needs to be at least 1 sample. Or is it OK to have
			// a VCF file w/o samples?
			throw new BuildException("Unexpected length of header fields.");
		} else {
			for (int i = 0; i < expectedHeaders.length; i++) {
				if (header[i] == null || !header[i].equals(expectedHeaders[i])) {
					throw new BuildException(
							"Unexpected item in header at position " + i + ": "
									+ header[i]);
				}
			}
			// we're going to be certain there are no duplicates.
			HashSet<String> sampleNameSet = new HashSet<String>();
			for (int i = expectedHeaders.length; i < header.length; i++) {
				if (sampleNameSet.contains(header[i])) {
					throw new BuildException(
							"Duplicated sample in header name: " + header[i]);
				}
				try {
					samples.add(getDiversitySample(header[i]));
					sampleNames.add(header[i]);
				} catch (ObjectStoreException e) {
					throw new BuildException("Failed to store DiversitySample", e);
				}
			}
		}
	}

	private boolean processData(String[] fields) throws ObjectStoreException {
	  if (fields.length < expectedHeaders.length + 1) {
	    throw new BuildException(
	        "Unexpected number of columns in VCF file.");
	  }
	  String chr = fields[0];
	  // TODO remove for production
	  // if (!chr.equals("Chr01")) return false;

	  if (lastChromosome == null || !chr.equals(lastChromosome)) {
	    lastChromosome = chr;
	    LOG.info("Processing " + chr);
	  }

	  Integer pos = new Integer(fields[1]);
	  // TODO remove for production
	  // if (pos > 100000) return false;

	  String name = fields[2];
	  String ref = fields[3];
	  String alt = fields[4];
	  String quality = fields[5];
	  String filter = fields[6];
	  String info = fields[7];

	  // create the chromosome if we haven't seen this before		
	  ProxyReference chrRef = getChromosome(chr);

	  SNP snp = getDirectDataLoader().createObject(SNP.class);
	  snp.proxyOrganism(getOrganism());
	  snp.setReference(ref);
	  snp.setAlternate(alt);
	  try {
	    // only add if a number
	    snp.setQuality(Double.parseDouble(quality));
	  } catch (NumberFormatException e) {
	  }
	  snp.setName(name);
	  snp.setFilter(filter);
	  Integer nSamples = parseInfo(snp, info);
	  if (nSamples != null && nSamples > 0) {
	    snp.setSampleCount(nSamples);
	  }
	  // create and store the location
	  // SNP isn't a SequenceFeature so we don't set chromosomeLocation
	  makeLocation(chrRef, snp, pos, pos + ref.length(), "1");


	  // process the genotype field. First we have
	  // attribute:attribute:attribute...
	  // and value:value:value... Convert these to
	  // attribute=value;attribute=value;...
	  String[] attBits = fields[formatPosition].split(":");

	  // look through the different genotype scores for column 9 onward.
	  HashMap<String,HashMap<String,String>> genoHash = new HashMap<String,HashMap<String,String>>();
	  for (int col = expectedHeaders.length; col < fields.length; col++) {
	    String[] valBits = fields[col].split(":");
	    String genotype = null;
	    StringBuffer sampleInfo = new StringBuffer();
	    if ((valBits.length != attBits.length)
	        && !fields[col].equals("./."))
	      LOG.warn("Genotype fields have unexpected length.");
	    for (int i = 0; i < attBits.length && i < valBits.length; i++) {
	      if (attBits[i].equals("GT")) {
	        genotype = valBits[i];
	        if (!genoHash.containsKey(genotype) ) {
	          genoHash.put(genotype,new HashMap<String,String>());
	        }
	      }
	      if (sampleInfo.length() > 0) sampleInfo.append(":");
	      sampleInfo.append(attBits[i] + "=" + valBits[i]);
	    }
	    if (genotype != null) {
	      genoHash.get(genotype).put(sampleNames.get(col-expectedHeaders.length),sampleInfo.toString());
	    }
	  }
	  // and register the genotypes for the snp

    // now construct the JSON string
    StringBuffer JSONString = new StringBuffer("{");

    boolean storeGenotype = false;
    for( String genotype : genoHash.keySet()) {
      if (JSONString.length() > 1 ) {
        JSONString.append(",");
      }
      JSONString.append("\""+genotype+"\":[");
      boolean needComma = false;
      for( String sampleName : genoHash.get(genotype).keySet()) {
        if (needComma) JSONString.append(",");
        JSONString.append("\""+sampleName+"\"");
        needComma = true;
      }
      JSONString.append("]");
    }
    JSONString.append("}");
    snp.setSampleInfo(JSONString.toString());

    try {
      // and store the snp.
      getDirectDataLoader().store(snp);
    } catch (ObjectStoreException e) {
      throw new BuildException("Problem storing SNP: " + e);
    }
    
    if( storeGenotype ) {
	  for( String genotype : genoHash.keySet()) {

	    Genotype g = getDirectDataLoader().createObject(Genotype.class);
	    g.setGenotype(genotype);
	    g.setSnp(snp);
	    StringBuffer sampleInfo = new StringBuffer();
	    for( String sample : genoHash.get(genotype).keySet()) {
	      if (sampleInfo.length() > 0) {
	        sampleInfo.append(",");
	      }
	      sampleInfo.append("\""+sample+"\":\""+genoHash.get(genotype).get(sample)+"\"");
	    }
	    g.setSampleInfo("{"+sampleInfo.toString()+"}");

	    try {
	      // and store the genotype info.
	      getDirectDataLoader().store(g);
	    } catch (ObjectStoreException e) {
	      throw new BuildException("Problem storing Genotype info: " + e.getMessage());
	    }
	  }
    }
	  
	  return true;
	}

	/**
	 * parseInfo We're taking the INFO field of the VCF record, extracting the
	 * EFF tag and stuffing the remainder into the info attribute
	 * 
	 * @param snp
	 *            The snp record being processed
	 * @param info
	 *            The info string
	 */
	private Integer parseInfo(SNP snp, String info) throws ObjectStoreException {
		Integer nSamples = null;
		if (info == null)
			return null;
		// initialize the new string buffer to be same length as old.
		StringBuffer newInfo = new StringBuffer(info.length());
		String[] bits = info.split(";");
		for (String keyVal : bits) {
			String[] kV = keyVal.split("=", 2);
			if (kV[0].equals("EFF")) {
				// deal with the SnpEff calls.
				parseEff(snp, kV[1]);
			} else if (kV[0].equals("set")) {
				// we're going to drop the set= tags. But we will use it to
				// determine
				// the number of samples
				nSamples = kV[1].split("-").length;
			} else {
				// if not EFF tag, append to newInfo
				if (newInfo.length() > 0)
					newInfo.append(';');
				newInfo.append(keyVal);
			}
		}
    // here is where we decide if we're storing the stripped down
    // info field, or the original stored newInfo.
    //snp.setAttribute("info",newInfo.toString());
    // but now were saving the original (without the set tag)
    snp.setInfo(info);
    
		return nSamples;
	}

	private void parseEff(SNP snp, String eff) throws ObjectStoreException {
		if (eff == null)
			return;

		for (String bit : eff.split(",")) {
			Matcher match = effPattern.matcher(bit);
			if (match.matches()) {
				String cType = match.group(1);
				String effect = match.group(2);
				String[] fields = effect.split("\\|");
				if (fields.length < 9)
					return;

				// has this consequence been seen before?
				// first, construct the key to the map
				StringBuffer conKey = new StringBuffer(cType);
				conKey.append(":");
				if (fields[3] != null)
					conKey.append(fields[3]);
				conKey.append(":");
				if (fields[5] != null)
					conKey.append(fields[5]);
				conKey.append(":");
				if (fields[8] != null)
					conKey.append(fields[8]);

				if (!consequences.containsKey(conKey.toString())) {
					Consequence con = getDirectDataLoader().createObject(Consequence.class);
					con.proxyType(getConsequenceType(cType));

					if (fields[3] != null && fields[3].length() > 0) {
						con.setSubstitution(fields[3]);
					}
					if (fields[5] != null && fields[5].length() > 0) {
						con.proxyGene(getGene(fields[5]));
					}
					if (fields[8] != null && fields[8].length() > 0) {
						con.proxyTranscript(getMRNA(fields[8]));
					}

					getDirectDataLoader().store(con);

					consequences.put(conKey.toString(), con);
				}
				snp.addConsequences(consequences.get(conKey.toString()));
			}
		}
	}

	private void preFill(Map<String,ProxyReference> map, Class<? extends InterMineObject> objectClass) {
	  Query q = new Query();
	  QueryClass qC = new QueryClass(objectClass);
	  q.addFrom(qC);
	  QueryField qFName = new QueryField(qC,"primaryIdentifier");
	  QueryField qFId = new QueryField(qC,"id");
	  q.addToSelect(qFName);
	  q.addToSelect(qFId);
	  QueryClass qcOrg = new QueryClass(Organism.class);
	  q.addFrom(qcOrg);
	  QueryObjectReference orgRef = new QueryObjectReference(qC,"organism");
	  QueryField qFProtId = new QueryField(qcOrg,"proteomeId");

	  ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
	  cs.addConstraint(new ContainsConstraint(orgRef,ConstraintOp.CONTAINS,qcOrg));
	  cs.addConstraint(new SimpleConstraint(qFProtId,ConstraintOp.EQUALS,new QueryValue(proteomeId)));

	  q.setConstraint(cs);

	  LOG.info("Prefilling ProxyReferences. Query is "+q);
	  try {
	    Results res = getIntegrationWriter().getObjectStore().execute(q,5000,false,false,false);
	    Iterator<Object> resIter = res.iterator();
	    System.out.println("Iterating...");
	    while (resIter.hasNext()) {
	      @SuppressWarnings("unchecked")
	      ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
	      String name = (String)rr.get(0);
	      Integer id = (Integer)rr.get(1);
	      map.put(name,new ProxyReference(getIntegrationWriter().getObjectStore(),id,objectClass));
	    }
	  } catch (Exception e) {
	    throw new BuildException("Problem in prefilling ProxyReferences: " + e.getMessage());
	  }
	  LOG.info("Retieved "+map.size()+" ProxyReferences.");

	}

	// ------------------------------------------------------------------------------

	// Create and store referenced objects, keeping ProxyRefererences in maps for
	// reuse where needed.

	
	protected ProxyReference getOrganism() throws ObjectStoreException {
        if (orgRef == null) {
            Organism org = getDirectDataLoader().createObject(Organism.class);
            org.setProteomeId(proteomeId);
            getDirectDataLoader().store(org);
            orgRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    org.getId(), Organism.class);
        }
        return orgRef;
    }
    
    protected ProxyReference getConsequenceType(String type) throws ObjectStoreException {
		ProxyReference conRef = consequenceTypes.get(type);
    	if (conRef == null) {
    		ConsequenceType con = getDirectDataLoader().createObject(ConsequenceType.class);
    		con.setType(type);
    		getDirectDataLoader().store(con);
    		conRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
    				con.getId(), ConsequenceType.class);
    		consequenceTypes.put(type, conRef);
    	}
    	return conRef;
    }
	
    protected ProxyReference getDiversitySample(String name) throws ObjectStoreException {
    	DiversitySample sam = getDirectDataLoader().createObject(DiversitySample.class);
    	sam.setName(name);
    	sam.proxyOrganism(getOrganism());
    	getDirectDataLoader().store(sam);
    	return new ProxyReference(getIntegrationWriter().getObjectStore(),
    			sam.getId(), DiversitySample.class);
    }
    
    private ProxyReference getGene(String identifier) throws ObjectStoreException {
    	ProxyReference geneRef = genes.get(identifier);
    	if (geneRef == null) {
    	  LOG.info("Getting new proxy ref for gene "+identifier);
    		Gene gene = getDirectDataLoader().createObject(Gene.class);
            gene.setPrimaryIdentifier(identifier);
            gene.proxyOrganism(getOrganism());
            gene.addDataSets(getDataSet());
            gene.proxySequenceOntologyTerm(getSOTerm("gene"));
            getDirectDataLoader().store(gene);
            geneRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    gene.getId(), Gene.class);
            genes.put(identifier, geneRef);
    	}
    	
    	return geneRef;
    }

    private ProxyReference getMRNA(String identifier) throws ObjectStoreException {
    	ProxyReference mrnaRef = mrnas.get(identifier);
    	if (mrnaRef == null) {
        LOG.info("Getting new proxy ref for mrna "+identifier);
    		MRNA mrna = getDirectDataLoader().createObject(MRNA.class);
            mrna.setPrimaryIdentifier(identifier);
            mrna.proxyOrganism(getOrganism());
            mrna.addDataSets(getDataSet());
            mrna.proxySequenceOntologyTerm(getSOTerm("mRNA"));
            getDirectDataLoader().store(mrna);
            mrnaRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    mrna.getId(), MRNA.class);
            mrnas.put(identifier, mrnaRef);
    	}
    	return mrnaRef;
    }
    
    private ProxyReference getChromosome(String identifier) throws ObjectStoreException {
    	ProxyReference chrRef = chromosomes.get(identifier);
    	if (chrRef == null) {
        LOG.info("Getting new proxy ref for chromosome "+identifier);
    		Chromosome chr = getDirectDataLoader().createObject(Chromosome.class);
            chr.setPrimaryIdentifier(identifier);
            chr.proxyOrganism(getOrganism());
            chr.addDataSets(getDataSet());
            chr.proxySequenceOntologyTerm(getSOTerm("chromosome"));
            getDirectDataLoader().store(chr);
            chrRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    chr.getId(), Chromosome.class);
            chromosomes.put(identifier, chrRef);
    	}
    	return chrRef;
    }
    
    private void makeLocation(ProxyReference locatedOn, SNP feature,
    		int start, int end, String strand) throws ObjectStoreException {
    	Location location = getDirectDataLoader().createObject(Location.class);
    	location.proxyLocatedOn(locatedOn);
    	location.setFeature((BioEntity) feature);
    	location.setStart(start);
    	location.setEnd(end);
    	location.setStrand(strand);
    	getDirectDataLoader().store(location);
    }
    
    // ------------------------------------------------------------------------------
    
    // Methods to create sequence ontology & dataset related objects, usually handled
    // automatically for items based converters (could go in a BioDirectDataloader superclass)
    private ProxyReference getSequenceOntology() throws ObjectStoreException {
    	if (ontologyRef == null) {
    		Ontology ontology = getDirectDataLoader().createObject(Ontology.class);
            ontology.setName("Sequence Ontology");
            ontology.setUrl("http://www.sequenceontology.org");
            getDirectDataLoader().store(ontology);
            ontologyRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    ontology.getId(), Ontology.class);
    	}
    	return ontologyRef;
    }
    
    private ProxyReference getSOTerm(String featureType) throws ObjectStoreException {
    	ProxyReference soTermRef = soTerms.get(featureType);
    	if (soTermRef == null) {
    		SOTerm term = getDirectDataLoader().createObject(SOTerm.class);
            term.proxyOntology(getSequenceOntology());
            term.setName(featureType);
            getDirectDataLoader().store(term);
            soTermRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    term.getId(), SOTerm.class);
            soTerms.put(featureType, soTermRef);
    	}
    	return soTermRef;
    }
    
    private DataSource getDataSource() throws ObjectStoreException {
    	if (dataSource == null) {
    		dataSource = getDirectDataLoader().createObject(DataSource.class);
            dataSource.setName(DATA_SOURCE_NAME);
            getDirectDataLoader().store(dataSource);
    	}
    	return dataSource;
    }
    
    private DataSet getDataSet() throws ObjectStoreException {
    	if (dataSet == null) {
    		dataSet = getDirectDataLoader().createObject(DataSet.class);
            dataSet.setName(DATASET_TITLE);
            dataSet.setDataSource(getDataSource());
            getDirectDataLoader().store(dataSet);
    	}
    	return dataSet;
    }
    
}
