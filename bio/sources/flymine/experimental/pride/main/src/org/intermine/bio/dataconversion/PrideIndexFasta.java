package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.biojava.bio.BioException;
import org.biojava.bio.program.indexdb.IndexTools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.db.SequenceDBLite;
import org.biojava.bio.seq.db.flat.FlatSequenceDB;
import org.biojava.bio.seq.io.SeqIOConstants;
import org.biojava.utils.ParserException;

/**
 * PrideIndexFasta for indexing fasta files
 * @author Dominik Grimm and Michael Menden
 */
public class PrideIndexFasta
{

       private File[] fasta;
       private SequenceDBLite db;

       /**
        * Constructor
        * @param path directory of the fasta files or one fasta file
        */
       public PrideIndexFasta(String path) {
              readFastaFiles(path);
              createIndexFile();
       }

       private void readFastaFiles(String path) {
              File file = new File(path);

              if (file.isDirectory()) {
                     fasta = file.listFiles();
              } else {
                     fasta = new File[1];
                     fasta[0] = new File(path);
              }
       }

       private long getLastModifiedFasta() {
              long age = 0;
              for (int i = 0; i < fasta.length; i++) {
                     if (age < fasta[i].lastModified()) {
                            age = fasta[i].lastModified();
                     }
              }
              return age;
       }

       private void createIndexFile() {
              String fileName = "build/IndexFile";
              File indexFolder = new File(fileName);

              try {
                     if (!indexFolder.exists()) {
                            //create indexfolder and files
                            IndexTools.indexFasta("index", indexFolder , fasta, SeqIOConstants.AA);
                     } else if (getLastModifiedFasta() > indexFolder.lastModified()) {

                            File [] indexFiles = indexFolder.listFiles();

                            //first delete the contents of the folder
                            for (int i = 0; i < indexFiles.length; i++) {
                                   indexFiles[i].delete();
                            }
                            indexFolder.delete();

                            //create indexfolder and files
                            IndexTools.indexFasta("index", indexFolder , fasta, SeqIOConstants.AA);
                     }
              } catch (FileNotFoundException ex) {
                     ex.printStackTrace();
              } catch (IOException ex) {
                     ex.printStackTrace();
              } catch (ParserException ex) {
                     ex.printStackTrace();
              } catch (BioException ex) {
                     ex.printStackTrace();
              }

              try {
                     //create SequenceDB lockuptable (build in RAM)
               db = new FlatSequenceDB(fileName, "db");
        } catch (IOException ex) {
               ex.printStackTrace();
        } catch (BioException ex) {
               ex.printStackTrace();
        }
       }

       /**
        * Constructor
        * @param accession accessionId to find the equivalent protein
        * @return result returns the sequence of the protein
        */
       public String getProtein(String accession) {

              String result = null;

              try {
                     Sequence seq = db.getSequence(accession);
                     result = seq.seqString();
              } catch (BioException ex) {
               ex.printStackTrace();
        }

              return result;
       }

}
