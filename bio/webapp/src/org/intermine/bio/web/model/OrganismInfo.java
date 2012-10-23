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
 * Bean to store organism information for Galaxy use.
 *
 * @author Fengyuan Hu
 *
 */

public class OrganismInfo
{
    private int idx;
    private String orgList;
    private String genomeBuild;

    /**
     * Constructor
     * @param idx the index of Sequence feature in query path view
     * @param orgList a list of organisms which the features belong to
     * @param genomeBuild genome build for the features
     */
    public OrganismInfo (int idx, String orgList, String genomeBuild) {

        this.idx = idx;
        this.orgList = orgList;
        this.genomeBuild = genomeBuild;
    }

    /**
     *
     * @return index
     */
    public int getIdx() {
        return idx;
    }

    /**
     *
     * @param idx the index of Sequence feature in query path view
     */
    public void setIdx(int idx) {
        this.idx = idx;
    }

    /**
     *
     * @return orgList
     */
    public String getOrgList() {
        return orgList;
    }

    /**
     *
     * @param orgList a list of organisms which the features belong to
     */
    public void setOrgList(String orgList) {
        this.orgList = orgList;
    }

    /**
     *
     * @return genomeBuild
     */
    public String getGenomeBuild() {
        return genomeBuild;
    }

    /**
     *
     * @param genomeBuild genome build for the features
     */
    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }



}
