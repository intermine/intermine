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


import java.util.Iterator;
import java.util.Stack;

/**
 * DataConverter to parse pride data into items
 * @author Dominik Grimm and Michael Menden
 */

public class PrideCalculatePos implements Iterator<PridePeptideData>
{
       
       //private fields
       private String   sProteinSeq;
       private String   sPeptideSeq;
       private Stack<PridePeptideData> stackData;
       private int nIndex;
       private PridePeptideData dataOld;
       private PridePeptideData data;
       
       /**
        * Constructor
        * @param sProteinSeq protein sequence
        * @param dataOld current Pridedata 
        */
       public PrideCalculatePos(String sProteinSeq, PridePeptideData dataOld) {
              this.sProteinSeq = sProteinSeq.toUpperCase();
              this.sPeptideSeq = dataOld.getSequence().toUpperCase();
              this.dataOld = dataOld;
              stackData = new Stack<PridePeptideData>();
              nIndex = 0;
              calculate();
       }

       /**
        * hasNext
        * @return true if stack has top element
        */
       public boolean hasNext() {
              return nIndex != 0;
       }
       /**
        * next
        * @return top element of the stack
        */
       public PridePeptideData next() {
              return stackData.peek();
       }
       /**
        * remove the top element of the stack
        */
       public void remove() {
              stackData.pop();
              nIndex--;
       }
       
       //private method: calculates the correct start and end positions
       private void calculate() {
                     int start = 0, end = 0;
                     for (int i = 0; i < sProteinSeq.length(); i++) {
                            data = new PridePeptideData();
                            data.setSequence(sPeptideSeq);
                            data.setSpecRef(dataOld.getSpecRef());
                            data.setStartPos(sProteinSeq.indexOf(sPeptideSeq, i) + 1);
                            start = data.getStartPos();
                            if (start - 1 == -1) {
                                   break;
                            }
                            data.setEndPos(start + sPeptideSeq.length() - 1);
                            stackData.push(data);
                            nIndex++;
                            end = data.getEndPos();
                            if (end == start) {
                                   i = end - 1;
                            } else {
                                   i = start;
                            }
                     }
       }
       
}
