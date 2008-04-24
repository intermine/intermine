package org.intermine.webservice.output;

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
 * Class implementing rendering of html of navigation bar.
 * @author Jakub Kulaviak
 **/
public class NavigationBar 
{

    private String baseLink;
    
    private int pageSize;
    
    private int currentPage;
    
    /**
     * Constructor. According pageSize and currentPage parameter navigation bar 
     * computes link to following and previous page. 
     * @param baseLink prefix common for all links
     * example of base link: http://localhost:8080/query/data/template/results?name=
     *      AllGene_Chromosome&op1=eq&value1=Drosophila+melanogaster&size=10
     * <b>Important:</b> baseLink can't contain size or start parameter 
     * @param pageSize page size,  
     * @param currentPage index of current page,
     *  
     */
    public NavigationBar(String baseLink, int pageSize, int currentPage) {
        this.baseLink = baseLink;
        this.pageSize = pageSize;
        this.currentPage = currentPage;
    }
 
    /**
     * @param pageIndex index
     * @return link to the page with provided index
     */
    public String getPageLink(int pageIndex) {
        return baseLink + "&start=" + (pageIndex * pageSize + 1) + "&size=" + pageSize;
    }
    
    /**
     * @param pageIndex index
     * @return link to page previous to the page with provided index
     */
    public String getPreviousLink(int pageIndex) {
        if (pageIndex > 0) {
            return getPageLink(pageIndex - 1);    
        } else {
            return null;
        }
    }

    /**
     * @param pageIndex index
     * @return link to page next to the page with provided index
     */
    public String getNextLink(int pageIndex) {
        return getPageLink(pageIndex + 1);
    }
    
    /**
     * @return html of navigation bar.
     */
    public String toString() {
        String ret = "<span class=\"navigationBar\" style=\"white-space:nowrap;\">";
        ret += getLinkHtml("< Previous", getPreviousLink(currentPage));
        ret += "&nbsp;";
        ret += currentPage + 1;
        ret += "&nbsp;";
        ret += getLinkHtml("Next >", getNextLink(currentPage));
        ret += "</span>";
        return ret;
    }

    private String getLinkHtml(String title, String url) {
        if (url != null) {
            String ret = "<a  href=\"" + url + "\">" + title + "</a>";
            return ret;
        } else {
            return title;
        }
    }
}
