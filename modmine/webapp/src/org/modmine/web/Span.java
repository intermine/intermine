package org.modmine.web;

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
 * The record should be in BED format: "chr start end".
 *
 * @author Fengyuan Hu
 */
public class Span
{
    private String chr;
    private Integer start;
    private Integer end;

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
     * @return chr:start..end
     */
    @Override
    public String toString() {
        return chr + ":" + start + ".." + end;
    }
}
