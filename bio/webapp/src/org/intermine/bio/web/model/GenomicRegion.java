package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2012 FlyMine
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
public class GenomicRegion implements Comparable<GenomicRegion>
{
    private String organism;
    private String chr;
    private Integer start;
    private Integer end;
    private Integer extendedRegionSize = new Integer(0); // user add region flanking
    private Integer extendedStart;
    private Integer extendedEnd;

    //user identifier to tag the order of input e.g. X:7880589..7880644:5 is the 5th input
    private Integer tag = null;

    /**
     * Default constructor
     *
     * a new GenomicRegion must use setters to set start, end, extendedRegionSize
     */
    public GenomicRegion() {

    }

    /**
     * @param organism short name
     */
    public void setOrganism(String organism) {
        this.organism = organism;
    }

    /**
     * @return organism
     */
    public String getOrganism() {
        return organism;
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
     * @param tag as integer
     */
    public void setTag(Integer tag) {
        this.tag = tag;
    }

    /**
     * @return tag value
     */
    public Integer getTag() {
        return tag;
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
     * @return chr:extendedStart..extenededEnd|chr:start..end
     */
    public String getFullRegionInfo() {
        if (extendedRegionSize == 0) {
            return chr + ":" + start + ".." + end + "|" + extendedRegionSize + "|" + organism;
        } else {
            return chr + ":" + extendedStart + ".." + extendedEnd + "|" + chr
                + ":" + start + ".." + end + "|" + extendedRegionSize + "|" + organism;
        }
    }

    /**
     * @param obj a GenomicRegion object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenomicRegion) {
            GenomicRegion gr = (GenomicRegion) obj;

            // To uniquely identify a region
            if (gr.getOrganism() == null || gr.getTag() == null) { // for simpler version
                return (chr.equals(gr.getChr())
                        && start.equals(gr.getStart())
                        && end.equals(gr.getEnd()));
            } else {                                               // for full version
                return (chr.equals(gr.getChr())
                        && start.equals(gr.getStart())
                        && end.equals(gr.getEnd())
                        && organism.equals(gr.getOrganism())
                        && extendedRegionSize.equals(gr.getExtendedRegionSize())
                        && tag == gr.getTag());
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getOriginalRegion()
                + (getOriginalRegion().equals(getExtendedRegion()) ? ""
                        : " +/- " + extendedRegionSize);
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        if (organism == null) {  // for simpler version
            return chr.hashCode()
                + start.hashCode()
                + end.hashCode();
        } else {                 // for full version
            return chr.hashCode()
                + start.hashCode()
                + end.hashCode()
                + organism.hashCode()
                + extendedRegionSize.hashCode();
        }
    }

    @Override
    public int compareTo(GenomicRegion gr) {
        final int bEFORE = -1;
        final int eQUAL = 0;
        final int aFTER = 1;

        //this optimization is usually worthwhile, and can always be added
        if (this == gr) {
            return eQUAL;
        }

        if (this.getChr().compareTo(gr.getChr()) < 0) {
            return bEFORE;
        }

        if (this.getChr().equals(gr.getChr())) {
            if (extendedRegionSize == 0) {
                if (this.getStart() < gr.getStart()) {
                    return bEFORE;
                } else if (this.getStart() > gr.getStart()) {
                    return aFTER;
                } else {
                    if (this.getEnd() < gr.getEnd()) {
                        return bEFORE;
                    } else {
                        return aFTER;
                    }
                }
            } else {
                if (this.getExtendedStart() < gr.getExtendedStart()) {
                    return bEFORE;
                } else if (this.getExtendedStart() > gr.getExtendedStart()) {
                    return aFTER;
                } else {
                    if (this.getExtendedEnd() < gr.getExtendedEnd()) {
                        return bEFORE;
                    } else {
                        return aFTER;
                    }
                }
            }
        }

        return eQUAL;
    }

    /**
     * Test if two regions are overlapped.
     *
     * @param gr GenomicRegion
     * @return A boolean value
     */
    public boolean isOverlapped(GenomicRegion gr) {

        // TODO use extended region?

        return false;
    }
}
