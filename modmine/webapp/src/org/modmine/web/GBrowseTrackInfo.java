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
 * A Java POJO to store submission track information
 * @author Fengyuan Hu
 */
public class GBrowseTrackInfo
{
    private String organism; // {fly,worm}
    private String track;    // e.g. Snyder_PHA4_GFP_COMB
    private String subTrack; // e.g. PHA4_L2_GFP
    private String DCCid;

    /**
     * Instantiates a GBrowse track fully.
     *
     * @param organism     e.g. fly, worm
     * @param track        e.g. Snyder_PHA4_GFP_COMB
     * @param subTrack     e.g. PHA4_L2_GFP
     * @param DCCid submission ID
     * @return
     *
     */
    public GBrowseTrackInfo(String organism, String track, String subTrack, String DCCid) {
        this.organism  = organism;
        this.track = track;
        this.subTrack = subTrack;
        this.DCCid = DCCid;
    }

    /**
     * @return the organism
     */
    public String getOrganism() {
        return organism;
    }

    /**
     * @return the track name
     */
    public String getTrack() {
        return track;
    }

    /**
     * @return the subTrack
     */
    public String getSubTrack() {
        return subTrack;
    }

    /**
     * @return the DCCid
     */
    public String getDCCid() {
        return DCCid;
    }
}

