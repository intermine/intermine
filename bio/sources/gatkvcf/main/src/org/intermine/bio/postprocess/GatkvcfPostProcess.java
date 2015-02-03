/**
 * 
 */
package org.intermine.bio.postprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.postgresql.copy.CopyManager;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.DiversitySample;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SNP;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.postprocess.PostProcessor;
import org.intermine.util.FormattedTextParser;

/**
 * this was originally written as a test to see if adding the SimpleObject table
 * linking the SNPs to the samples could be done faster as a postprocessing step
 * than as an integration step. It can.
 * There is much code duplication between this and the integration processor. tsk tsk tsk.
 * @author jcarlson
 *
 */
public class GatkvcfPostProcess extends PostProcessor {

  Integer proteomeId;
  String dataDir;
  String dataDirIncludes;
  ObjectStoreWriter osw;
  private ArrayList<String> sampleList = new ArrayList<String>();
  private static final Logger LOG = Logger.getLogger(GatkvcfPostProcess.class);
  private HashMap<String,Integer> sampleIdMap;
  private HashMap<String,Integer> ssIdMap;

  final static String[] expectedHeaders = {"#CHROM","POS","ID","REF","ALT",
    "QUAL","FILTER","INFO","FORMAT"};
  final static int formatPosition = 8;

  public GatkvcfPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
    proteomeId = null;
    dataDir = null;
    dataDirIncludes = null;
    sampleIdMap = new HashMap<String,Integer>();
    ssIdMap = new HashMap<String,Integer>();
  }

  public void postProcess() throws BuildException, ObjectStoreException {
    if (proteomeId==null) {
      LOG.error("Proteome Id is not set.");
      throw new BuildException("Proteome Id is not set.");
    }

    LOG.info("Getting sample names...");
    fillSampleIdHash();
    LOG.info("Getting ss id's...");
    fillSsIdHash();
    DirectoryScanner ds = new DirectoryScanner();
    if (dataDirIncludes != null)
      ds.setIncludes(new String[] {dataDirIncludes});
    ds.setBasedir(new File(dataDir));
    ds.setCaseSensitive(true);
    ds.scan();
    for( String file : ds.getIncludedFiles() ) {
      LOG.info("Processing file "+file);
      {
        try {
          // we need to open and find the header line. Since this starts with a "#", the
          //  FormattedTextParser calls this a comment line and will not return it to us.
          BufferedReader in = new BufferedReader(new FileReader(dataDir+"/"+file));
          // the innie and the outie of the COPY data
          PipedWriter sdsOut = new PipedWriter();
          PipedReader sdsIn = new PipedReader(sdsOut);
          // run the snp-diversitysample COPY processes in a separate thread
          Thread sdsCopyThread = new Thread( new CopyThread((ObjectStoreInterMineImpl)osw,sdsIn,
              "SNPDiversitySample (format,genotype,diversitysampleid,snpid)"));
          sdsCopyThread.start();
          PipedWriter genoOut = new PipedWriter();
          PipedReader genoIn = new PipedReader(genoOut);
          // run the genotype COPY processes in a separate thread
          Thread genoCopyThread = new Thread( new CopyThread((ObjectStoreInterMineImpl)osw,genoIn,
              "Genotype (snpid,genotype,sampleinfo)"));
          genoCopyThread.start();

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
            in.close();
            sdsOut.close();
            genoOut.close();
            LOG.error("Cannot find sample names in vcf file.");
            throw new BuildException("Cannot find sample names in vcf file.");
          }
          Iterator<?> tsvIter;
          try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(in);
          } catch (Exception e) {
            sdsOut.close();
            genoOut.close();
            in.close();
            sdsIn.close();
            genoIn.close();
            LOG.error("Cannot parse file: " + file + ": "+e.getMessage());
            throw new BuildException("Cannot parse file: " + file + ": "+e.getMessage());
          }
          int ctr = 0;
          while (tsvIter.hasNext() ) {
            ctr++;
            String[] fields = (String[]) tsvIter.next();
            if (!processData(sdsOut,genoOut,fields)) {
              return;
            }
            if ((ctr%100000) == 0) {
              LOG.info("Processed " + ctr + " lines...");
            }
          }
          LOG.info("Processed " + ctr + " lines.");
          in.close();
          sdsOut.flush();
          sdsOut.close();
          genoOut.flush();
          genoOut.close();
          // make sure the writer threads are done
          while ( sdsCopyThread.getState() != Thread.State.TERMINATED && 
              genoCopyThread.getState() != Thread.State.TERMINATED){
            LOG.info("Writer thread is not finished. Sleeping...");
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
          }
          sdsIn.close();
          genoIn.close();
        } catch (IOException e) {
          LOG.error("IO Exception duing processing: "+ e.getMessage());
          throw new BuildException("IO Exception duing processing: "+ e.getMessage());
        }
      }

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
          LOG.error("Unexpected item in header at position "+
              i+": "+header[i]);
          throw new BuildException("Unexpected item in header at position "+
              i+": "+header[i]);
        }
      }
      // we're going to be certain there are no duplicates.
      HashSet<String> sampleNameSet = new HashSet<String>();
      for(int i=expectedHeaders.length;i<header.length;i++) {
        if( sampleNameSet.contains(header[i]) ) {
          LOG.error("Duplicated sample in header name: "+ header[i]);
          throw new BuildException("Duplicated sample in header name: "+ header[i]);
        };
        sampleList.add(header[i]);
      }
    }
  }

  //private boolean processData(Writer out1,Writer out2,String[] fields) throws BuildException
  private boolean processData(Writer out1,Writer out2,String[] fields) throws BuildException
  {
    if (fields.length < expectedHeaders.length + 1) {
      throw new BuildException("Unexpected number of columns in VCF file.");
    }
    String name = fields[2];
    if (!ssIdMap.containsKey(name)) {
      throw new BuildException("Sample name "+name+" was not registered.");
    }

    // process the genotype field. First we have attribute:attribute:attribute... 
    // and value:value:value... Convert these to attribute=value;attribute=value;...
    String[] attBits = fields[formatPosition].split(":");
    HashMap<String,HashMap<String,String>> genoHash = new HashMap<String,HashMap<String,String>>();
    

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
          if (!genoHash.containsKey(genotype.toString())) 
            genoHash.put(genotype.toString(), new HashMap<String,String>());
        } else {
          if (format.length() > 0) format.append(":");
          format.append(attBits[i] + "=" + valBits[i]);
          if (attBits[i].equals("FT")) {
            passField = (valBits[i].equals("PASS"))?true:false;
          }  
        }
      }
      genoHash.get(genotype.toString()).put(
          sampleList.get(col-expectedHeaders.length), "GT="+genotype.toString()+
          ((format.length()>0)?(":"+format.toString()):""));
      if ((passField != null && passField) ||
          (passField == null && genoField != null && genoField)) {
        try {
          //TODO: is writing binary faster?
          if (format.toString().isEmpty() ) {
            // a NULL record.
            format = new StringBuffer("\\N");
          }
          out1.write(format.toString()+"\t"+
                    genotype.toString()+"\t"+
              sampleIdMap.get(sampleList.get(col-expectedHeaders.length)).toString()+
              "\t"+
              ssIdMap.get(name).toString()+"\n");
        } catch (IOException e) {
          LOG.error("Trouble writing to SQL pipe: "+e.getMessage());
          throw new BuildException("Trouble writing to SQL pipe: "+e.getMessage());
        }
      }
    }
    // now write to the genotype sucker
    for( String genotype: genoHash.keySet()) {
      try { 
        out2.write(ssIdMap.get(name).toString()+"\t"+
            genotype+"\t"+
            toJSON(genoHash.get(genotype))+"\n");
      } catch (IOException e) {
        LOG.error("Trouble writing to SQL pipe: "+e.getMessage());
        throw new BuildException("Trouble writing to SQL pipe: "+e.getMessage());
      }
    }
    return true;
  } 
  
  private String toJSON(HashMap<String,String> hash) {
    StringBuilder ret = new StringBuilder("{");
    for(String key : hash.keySet()) {
      if (ret.length() > 1) ret.append(",");
      ret.append("\""+key+"\":\""+hash.get(key)+"\"");
    }
    ret.append("}");
    return ret.toString();
  }
  
  private void fillSampleIdHash() throws BuildException {
    try {
      Query q = new Query();

      q.setDistinct(false);

      QueryClass qcSample = new QueryClass(DiversitySample.class);
      QueryClass qcOrganism = new QueryClass(Organism.class);

      QueryValue qv = new QueryValue(proteomeId);
      QueryField qf = new QueryField(qcOrganism,"proteomeId");

      QueryField qsName = new QueryField(qcSample,"name");
      QueryField qsId = new QueryField(qcSample,"id");
      q.addFrom(qcSample);
      q.addFrom(qcOrganism);

      q.addToSelect(qsName);
      q.addToSelect(qsId);

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS, qv));
      QueryObjectReference orgSampleRef = new QueryObjectReference(qcSample, "organism");
      cs.addConstraint(new ContainsConstraint(orgSampleRef, ConstraintOp.CONTAINS, qcOrganism));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 1000, true, true, true);
      Iterator<Object> resIter = res.iterator();
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        sampleIdMap.put((String)rr.get(0),(Integer)rr.get(1));
      }
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
  }
  private void fillSsIdHash() throws BuildException {
    try {
      Query q = new Query();

      q.setDistinct(false);

      QueryClass qcSnp = new QueryClass(SNP.class);
      QueryClass qcOrganism = new QueryClass(Organism.class);
      
      QueryField qsName = new QueryField(qcSnp,"name");
      QueryField qsId = new QueryField(qcSnp,"id");
      q.addFrom(qcSnp);
      q.addFrom(qcOrganism);
      q.addToSelect(qsName);
      q.addToSelect(qsId);

      QueryValue qv = new QueryValue(proteomeId);
      QueryField qf = new QueryField(qcOrganism,"proteomeId");

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS, qv));
      QueryObjectReference orgSampleRef = new QueryObjectReference(qcSnp, "organism");
      cs.addConstraint(new ContainsConstraint(orgSampleRef, ConstraintOp.CONTAINS, qcOrganism));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 50000, true, true, true);
      Iterator<Object> resIter = res.iterator();
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        ssIdMap.put((String)rr.get(0),(Integer)rr.get(1));
      }
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
  }
 
  public void setProteomeId(String proteome) {
    try {
      proteomeId = Integer.valueOf(proteome);
    } catch (NumberFormatException e) {
      LOG.error("Cannot find numerical proteome id for: " + proteome);
      throw new BuildException("Cannot find numerical proteome id for: " + proteome);
    }
  }

  public void setSrcDataDir(String dir) {
    dataDir = dir;
  }

  public void setSrcDataDirIncludes(String includes) {
    dataDirIncludes = includes;
  }

  private class CopyThread implements Runnable {
    Reader r;
    CopyManager cm;
    String sqlTable;
    CopyThread(ObjectStoreInterMineImpl osw,Reader r,String sqlTable) {
      super();
      this.r = r;
      this.sqlTable = sqlTable;
      try {
        org.postgresql.PGConnection conn;
        try {
          conn = (org.postgresql.PGConnection)(osw.getDatabase().getConnection());
        } catch (Exception e) {
          LOG.error("Error getting ObjectStoreWriter: "+e.getMessage());
          throw new BuildException("Error getting ObjectStoreWriter: "+e.getMessage());
        }
        cm = conn.getCopyAPI();
      } catch (SQLException e) {
        LOG.error("Error getting CopyManager: "+e.getMessage());
        throw new BuildException("Error getting CopyManager: "+e.getMessage());
      } 
    }
    public void run() {
      try {
        cm.copyIn("COPY "+sqlTable+" from STDIN",r,1024*1024);
      } catch (SQLException e) {
        LOG.error("SQL problem in copy: " + e.getMessage());
        throw new BuildException("SQL problem in copy: " + e.getMessage());
      } catch (IOException e) {
        LOG.error("IO problem in copy: " + e.getMessage());
        throw new BuildException("IO problem in copy: " + e.getMessage());
      }
    }
  }
}
