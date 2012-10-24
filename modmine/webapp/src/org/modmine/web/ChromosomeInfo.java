package org.modmine.web;

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
 * This Java bean stores the information for span validation use.
 *
 * @author Fengyuan Hu
 *
 */
public class ChromosomeInfo {

    private String orgName;
    private String chrPID;
    private Integer chrLength;


    /**
     *
     * @return organism name
     */
    public String getOrgName() {
        return orgName;
    }

    /**
     *
     * @param orgName organism name
     */
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    /**
     *
     * @return organism name
     */
    public String getChrPID() {
        return chrPID;
    }

    /**
     *
     * @param chrPID chromosome identifier
     */
    public void setChrPID(String chrPID) {
        this.chrPID = chrPID;
    }
    /**
     *
     * @return organism name
     */
    public Integer getChrLength() {
        return chrLength;
    }
    /**
     *
     * @param chrLength chromosome length
     */
    public void setChrLength(Integer chrLength) {
        this.chrLength = chrLength;
    }
}
