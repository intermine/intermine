package org.intermine.bio.io.bed;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * A record in a BED formatted file.
 * For more information about UCSC BED format, refer to
 * http://genome.ucsc.edu/FAQ/FAQformat.html#format1
 * http://www.ensembl.org/info/website/upload/bed.html
 *
 * BED format provides a flexible way to define the data lines that are displayed in an annotation
 * track. BED lines have three required fields and nine additional optional fields. The number of
 * fields per line must be consistent throughout any single set of data in an annotation track. The
 * order of the optional fields is binding: lower-numbered fields must always be populated if
 * higher-numbered fields are used.
 *
 * The first three required BED fields are:
 * 1. chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random) or scaffold
 *            (e.g. scaffold10671), and chromosome names can be given with or without the 'chr'
 *            prefix.
 * 2. chromStart - The starting position of the feature in the chromosome or scaffold. The first
 *                 base in a chromosome is numbered 0.
 * 3. chromEnd - The ending position of the feature in the chromosome or scaffold. The chromEnd base
 *               is not included in the display of the feature. For example, the first 100 bases of
 *               a chromosome are defined as chromStart=0, chromEnd=100, and span the bases numbered
 *               0-99.
 *
 * The 9 additional optional BED fields are:
 * 4. name - Defines the name of the BED line. This label is displayed to the left of the BED line
 *           in the Genome Browser window when the track is open to full display mode or directly to
 *           the left of the item in pack mode. Escape special characters, e.g. space.
 * 5. score - A score between 0 and 1000. If the track line useScore attribute is set to 1 for this
 *            annotation data set, the score value will determine the level of gray in which this
 *            feature is displayed (higher numbers = darker gray).
 * 6. strand - Defines the strand - either '+' or '-'.
 * 7. thickStart - The starting position at which the feature is drawn thickly (for example, the
 *                 start codon in gene displays).
 * 8. thickEnd - The ending position at which the feature is drawn thickly (for example, the stop
 *               codon in gene displays).
 * 9. itemRgb - An RGB value of the form R,G,B (e.g. 255,0,0). If the track line itemRgb attribute
 *              is set to "On", this RBG value will determine the display color of the data
 *              contained in this BED line. NOTE: It is recommended that a simple color scheme
 *              (eight colors or less) be used with this attribute to avoid overwhelming the color
 *              resources of the Genome Browser and your Internet browser.
 * 10. blockCount - The number of blocks (exons) in the BED line.
 * 11. blockSizes - A comma-separated list of the block sizes. The number of items in this list
 *                  should correspond to blockCount.
 * 12. blockStarts - A comma-separated list of block starts. All of the blockStart positions should
 *                   be calculated relative to chromStart. The number of items in this list should
 *                   correspond to blockCount.
 *
 * Example:
 * Here's an example of an annotation track that uses a complete BED definition:
 *
 * # track name=pairedReads description="Clone Paired Reads" useScore=1
 * chr22 1000 5000 cloneA 960 + 1000 5000 0 2 567,488, 0,3512
 * chr22 2000 6000 cloneB 900 - 2000 6000 0 2 433,399, 0,3601
 *
 * @author Fengyuan Hu
 */
public class BEDRecord
{
    private String chrom;
    private int chromStart = -1;
    private int chromEnd = -1;
    private String name;
    private int score = 0; // InterMine cases don't have this score, give 0 as default
    private String strand = ".";
    private int thickStart = -1;
    private int thickEnd = -1;
    private int itemRgb = -1;
    private int blockCount = -1;
    private List<Integer> blockSizes;
    private List<Integer> blockStarts;

    /**
     * Constructor
     * The following five attributes are normally hosted in InterMine cases.
     *
     * @param chrom The name of the chromosome
     * @param chromStart start position of feature
     * @param chromEnd end position of feature
     * @param name symbol/Id of feature
     * @param score A score between 0 and 1000
     * @param strand direction on genome
     */
    public BEDRecord(String chrom, int chromStart, int chromEnd, String name,
            int score, String strand) {
        this.chrom = chrom;
        this.chromStart = chromStart;
        this.chromEnd = chromEnd;
        this.name = name;
        this.score = score;
        this.strand = strand;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "<BEDRecord: chrom: " + chrom + " chromStart: " + chromStart + " chromEnd: "
            + chromEnd + " name: " + name + " score: " + score + " strand: " + strand + ">";
    }

    /**
     * Return this record in BED format.  The String is suitable for output to a BED file.
     * This method will be called in BEDExporter
     *
     * @return a BED line
     */
    public String toBED() {
        return chrom + "\t" + chromStart + "\t" + chromEnd + "\t"
            + name + "\t" + score + "\t" + strand;
    }

    /**
    * @return the chromosome identifier with the prefix "chr", e.g. chr4
    */
    public String getChrom() {
        return chrom;
    }
    /**
     * @param chrom the chrom to set
     */
    public void setChrom(String chrom) {
        this.chrom = chrom;
    }
    /**
     * @return the chromStart
     */
    public int getChromStart() {
        return chromStart;
    }
    /**
     * @param chromStart the chromStart to set
     */
    public void setChromStart(int chromStart) {
        this.chromStart = chromStart;
    }
    /**
     * @return the chromEnd
     */
    public int getChromEnd() {
        return chromEnd;
    }
    /**
     * @param chromEnd the chromEnd to set
     */
    public void setChromEnd(int chromEnd) {
        this.chromEnd = chromEnd;
    }
    /**
     * @return the name - identifier or symbol or UNKNOWN
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the score - always zero
     */
    public int getScore() {
        return score;
    }
    /**
     * @param score the score to set
     */
    public void setScore(int score) {
        this.score = score;
    }
    /**
     * @return the strand
     */
    public String getStrand() {
        return strand;
    }
    /**
     * @param strand the strand to set
     */
    public void setStrand(String strand) {
        this.strand = strand;
    }
    /**
     * @return the thickStart
     */
    public int getThickStart() {
        return thickStart;
    }
    /**
     * @param thickStart the thickStart to set
     */
    public void setThickStart(int thickStart) {
        this.thickStart = thickStart;
    }
    /**
     * @return the thickEnd
     */
    public int getThickEnd() {
        return thickEnd;
    }
    /**
     * @param thickEnd the thickEnd to set
     */
    public void setThickEnd(int thickEnd) {
        this.thickEnd = thickEnd;
    }
    /**
     * @return the itemRgb
     */
    public int getItemRgb() {
        return itemRgb;
    }
    /**
     * @param itemRgb the itemRgb to set
     */
    public void setItemRgb(int itemRgb) {
        this.itemRgb = itemRgb;
    }
    /**
     * @return the blockCount
     */
    public int getBlockCount() {
        return blockCount;
    }
    /**
     * @param blockCount the blockCount to set
     */
    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }
    /**
     * @return the blockSizes
     */
    public List<Integer> getBlockSizes() {
        return blockSizes;
    }
    /**
     * @param blockSizes the blockSizes to set
     */
    public void setBlockSizes(List<Integer> blockSizes) {
        this.blockSizes = blockSizes;
    }
    /**
     * @return the blockStarts
     */
    public List<Integer> getBlockStarts() {
        return blockStarts;
    }
    /**
     * @param blockStarts the blockStarts to set
     */
    public void setBlockStarts(List<Integer> blockStarts) {
        this.blockStarts = blockStarts;
    }
}
