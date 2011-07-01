package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * This Java bean represents one record of Chromosome coordinates from user input
 * The record should be in BED format: "chr\tstart\tend".
 *
 * @author Fengyuan Hu
 */
public class GenomicRegion
{
    private String chr;
    private Integer start;
    private Integer end;
    // user add region flanking
    private Integer extendedStart = 0;
    private Integer extenededEnd = 0;
    private int extendedRegionSize = 0;

    /**
     * Constructor
     * @param grAsString a genomic region such as X:100..105
     */
    public GenomicRegion (String grAsString) {
        String[] temp = grAsString.split(":");
        this.chr = temp[0];
        temp = temp[1].split("\\.\\.");
        this.start = Integer.parseInt(temp[0]);
        this.end = Integer.parseInt(temp[1]);
        this.extendedStart = this.start;
        this.extenededEnd = this.end;
    }

    /**
     * Default constructor
     */
    public GenomicRegion() {

    }
    /**
     * @return chr
     */
    public String getChr() {
        return chr;
    }

    /**
     * @param chr chromosome
     */
    public void setChr(String chr) {
        this.chr = chr;
    }

    /**
     * @return start
     */
    public Integer getStart() {
        return start;
    }

    /**
     * @param start start poistion
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * @return end
     */
    public Integer getEnd() {
        return end;
    }

    /**
     * @param end end position
     */
    public void setEnd(Integer end) {
        this.end = end;
    }

    /**
     * @return the extendedStart
     */
    public Integer getExtendedStart() {
        return extendedStart;
    }

    /**
     * @param extendedStart the extendedStart to set
     */
    public void setExtendedStart(Integer extendedStart) {
        this.extendedStart = extendedStart;
    }

    /**
     * @return the extenededEnd
     */
    public Integer getExtenededEnd() {
        return extenededEnd;
    }

    /**
     * @param extenededEnd the extenededEnd to set
     */
    public void setExtenededEnd(Integer extenededEnd) {
        this.extenededEnd = extenededEnd;
    }

    /**
     * @return the extendedRegionSize
     */
    public int getExtendedRegionSize() {
        return extendedRegionSize;
    }

    /**
     * @param extendedRegionSize the extendedRegionSize to set
     */
    public void setExtendedRegionSize(int extendedRegionSize) {
        this.extendedRegionSize = extendedRegionSize;
    }

    /**
     * Make a string of orginal region if extended
     * @return chr:start..end
     */
    public String getOriginalRegion() {
        return chr + ":" + start + ".." + end;
    }

    /**
     * @return chr:extendedStart..extenededEnd
     */
    public String getExtentedRegion() {
        return chr + ":" + extendedStart + ".." + extenededEnd;
    }

    /**
     * @param obj a GenomicRegion object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenomicRegion) {
            GenomicRegion s = (GenomicRegion) obj;

            return (chr.equals(s.getChr())
                    && extendedStart.equals(s.getExtendedStart()) && extenededEnd
                    .equals(s.getExtenededEnd())
                    && start.equals(s.getStart()) && end.equals(s.getEnd()));
        }
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return chr.hashCode() + start.hashCode() + end.hashCode()
                + extendedStart.hashCode() + extenededEnd.hashCode();
    }

    /**
     * Test if region is extended
     * @return a boolean value
     */
    public boolean isRegionExtended() {
        if (this.extendedRegionSize > 0) {
            return true;
        } else {
            return false;
        }
    }
}
