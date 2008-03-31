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

/**
 * DataConverter to parse pride data into items
 * @author Dominik Grimm
 */

//PridePeptideData class
public class PridePeptideData {
	private int 	nStartPos;
	private int 	nEndPos;
	private float	specRef;
	private String 	sSeq;
	
	//standard constructor
	public PridePeptideData() {
		nStartPos = 0;
		nEndPos = 0;
	}
	
	//alternative constructor 1
	public PridePeptideData(String sSeq, int nStartPos, int nEndPos) {
		this.sSeq = sSeq;
		this.nStartPos = nStartPos;
		this.nEndPos = nEndPos;
		this.specRef = 0;
	}
	
	//alternative constructor2
	public PridePeptideData(String sSeq, int nStartPos, int nEndPos, float specRef) {
		this.sSeq = sSeq;
		this.nStartPos = nStartPos;
		this.nEndPos = nEndPos;
		this.specRef = specRef;
	}
	
	//Setter
	public void setSequence(String sSeq) {
		this.sSeq = sSeq;
	}
	
	public void setStartPos(int nStartPos) {
		this.nStartPos = nStartPos;
	}
	
	public void setEndPos(int nEndPos) {
		this.nEndPos = nEndPos;
	}
	
	public void setSpecRef(float specRef) {
		this.specRef = specRef;
	}
	
	//Getter
	public int getEndPos() {
		return this.nEndPos;
	}
	
	public int getStartPos() {
		return this.nStartPos;
	}
	
	public String getSequence() {
		return this.sSeq;
	}
	
	public float getSpecRef() {
		return this.specRef;
	}
	
	//specific key
	public String getKey() {
		return (Integer.toString(nStartPos) + "_" + Integer.toString(nEndPos) + "_" 
					+ Float.toString(specRef) + "_" + sSeq);
	}
}
