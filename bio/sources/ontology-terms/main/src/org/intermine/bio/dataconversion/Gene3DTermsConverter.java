/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * @author jcarlson
 *
 */
public class Gene3DTermsConverter extends OntologyTermsFileConverter {

  // a second file that maps names in the hmm file to accession number
  String srcMapFile = null;
  HashMap<String,String> nameMap;
  HashMap<String,String> descMap;
  // since multiple reps may have the same accession
  HashSet<String> beenThereDoneThat;
  
  /**
   * @param writer
   * @param model
   */
  public Gene3DTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    identifierKey = "^NAME.*";
    identifierReplacement = "^NAME\\s*";
    nameKey = null;
    descKey = null;
    namespaceKey = null;
  }
  public void process(Reader reader) throws Exception {
    if (srcMapFile != null) loadMapFile();
    super.process(reader);
  }
  public String storeRecord() throws ObjectStoreException {
    // the identifier line is the representative member.
    if (descMap.containsKey(cleanId(identifierLine)) ) {
      // put this in the name field
      nameLine = descMap.get(cleanId(identifierLine));
    }
    if (nameMap.containsKey(cleanId(identifierLine)) ) {
      // now we change it to the accession number
      identifierLine = nameMap.get(cleanId(identifierLine));
      if (!beenThereDoneThat.contains(identifierLine)) {
        beenThereDoneThat.add(identifierLine);
        return super.storeRecord();
      } else {
        return null;
      }
    }
    return null;
  }
  public void setSrcMapFile(String file) {
    srcMapFile = file;
  }
  
  private void loadMapFile() throws BuildException {
    try {
      BufferedReader br = new BufferedReader(new FileReader(srcDataDir+"/"+srcMapFile));
      String line;
      nameMap = new HashMap<String,String>();
      descMap = new HashMap<String,String>();
      beenThereDoneThat = new HashSet<String>();
      while( (line= br.readLine()) != null ) {
        String[] fields = line.split("\\t",3);
        if (fields.length < 2) {
          throw new BuildException("Too few fields in map file: "+line);
        }
        // field1 (name) may need to have a G3DSA: prepended
        if (fields[1].startsWith("G3DSA:")) {
          nameMap.put(fields[0], fields[1]);
        } else if (!fields[1].isEmpty()) {
          nameMap.put(fields[0], "G3DSA:"+fields[1]);
        }
        if (fields[2] != null && !fields[2].isEmpty() && !fields[2].equals("null") ) {
          descMap.put(fields[0],fields[2]);
        }
      }
      br.close();
    } catch (Exception e) {
      throw new BuildException("Problem in reading map file: "+e.getMessage());
    }
    
  }
  
  
}
