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

/**
 * DataConverter to parse pride data into items
 * @author Dominik Grimm and Michael Menden
 */

//PridePeptideData class
public class PridePeptideData 
{
       private int        nStartPos;
       private int        nEndPos;
       private float       specRef;
       private String        sSeq;
       
       /**
        * Constructor
        */
       //standard constructor
       public PridePeptideData() {
              nStartPos = 0;
              nEndPos = 0;
       }
       
       /**
        * Constructor
        * @param sSeq peptide sequence
        * @param nStartPos start position of the peptide
        * @param nEndPos end position of the peptide
        */
       public PridePeptideData(String sSeq, int nStartPos, int nEndPos) {
              this.sSeq = sSeq;
              this.nStartPos = nStartPos;
              this.nEndPos = nEndPos;
              this.specRef = 0;
       }
       /**
        * Constructor
        * @param sSeq peptide sequence
        * @param nStartPos start position of the peptide
        * @param nEndPos end position of the peptide
        * @param specRef spectrumReference of the peptide
        */
       public PridePeptideData(String sSeq, int nStartPos, int nEndPos, float specRef) {
              this.sSeq = sSeq;
              this.nStartPos = nStartPos;
              this.nEndPos = nEndPos;
              this.specRef = specRef;
       }
       
       /**
        * setSequence
        * @param sSeq sequence of the peptide
        */
       public void setSequence(String sSeq) {
              this.sSeq = sSeq;
       }
       /**
        * setSartPos
        * @param nStartPos start position of the peptide
        */
       public void setStartPos(int nStartPos) {
              this.nStartPos = nStartPos;
       }
       /**
        * setEndPos
        * @param nEndPos of the peptide
        */
       public void setEndPos(int nEndPos) {
              this.nEndPos = nEndPos;
       }
       /**
        * setSpecRef
        * @param specRef spectrumReference of the peptide
        */
       public void setSpecRef(float specRef) {
              this.specRef = specRef;
       }
       
       /**
        * getEndPos
        * @return end position of the peptide
        */
       public int getEndPos() {
              return this.nEndPos;
       }
       /**
        * getStartPos
        * @return start position of the peptide
        */
       public int getStartPos() {
              return this.nStartPos;
       }
       /**
        * getSequence
        * @return sequence of the peptide
        */
       public String getSequence() {
              return this.sSeq;
       }
       /**
        * getSpecRef
        * @return spectrumReference of the peptide
        */
       public float getSpecRef() {
              return this.specRef;
       }
       /**
        * getKey
        * @return unique key of the peptide
        */
       public String getKey() {
              return (Integer.toString(nStartPos) + "_" + Integer.toString(nEndPos) + "_" 
                                   + Float.toString(specRef) + "_" + sSeq);
       }
}
