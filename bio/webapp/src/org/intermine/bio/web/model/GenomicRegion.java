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
    private Integer extendedEnd = 0;
    private int extendedRegionSize = 0;

    /**
     * Default constructor
     *
     * a new GenomicRegion must use setters to set start, end, extendedRegionSize
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
     * @return the extendedEnd
     */
    public Integer getExtendedEnd() {
        return extendedEnd;
    }

    /**
     * @param extendedEnd the extendedEnd to set
     */
    public void setExtendedEnd(Integer extendedEnd) {
        this.extendedEnd = extendedEnd;
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
    public String getExtendedRegion() {
        return chr + ":" + extendedStart + ".." + extendedEnd;
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
                    && extendedStart.equals(s.getExtendedStart()) && extendedEnd
                    .equals(s.getExtendedEnd())
                    && start.equals(s.getStart()) && end.equals(s.getEnd()));
        }
        return false;
    }
    
    @Override 
    public String toString() {
        return getOriginalRegion() + (getOriginalRegion().equals(getExtendedRegion()) ? "" : " +/- " + extendedRegionSize);
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return chr.hashCode() + start.hashCode() + end.hashCode()
                + extendedStart.hashCode() + extendedEnd.hashCode();
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
