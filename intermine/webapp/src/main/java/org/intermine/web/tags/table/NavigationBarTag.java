package org.intermine.web.tags.table;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * Class implementing rendering of html of navigation bar.
 * @author Jakub Kulaviak
 **/
public class NavigationBarTag extends SimpleTagSupport
{

    private String baseLink;

    private Integer pageSize;

    private Integer currentPage;

    private boolean nextEnabled = false;

    /**
     * @return true if next link should be generated as a active link
     */
    public boolean isNextEnabled() {
        return nextEnabled;
    }

    /**
     * @param nextEnabled true if next link should be generated as a active link
     */
    public void setNextEnabled(boolean nextEnabled) {
        this.nextEnabled = nextEnabled;
    }

    /**
     * According pageSize and currentPage parameter navigation bar
     * computes link to following and previous page.
     * example of base link: http://localhost:8080/query/data/template/results?name=
     *      AllGene_Chromosome&op1=eq&value1=Drosophila+melanogaster&size=10
     * <b>Important:</b> baseLink can't contain size or start parameter
     * @throws IOException if some error happens
     */
    @Override
    public void doTag() throws IOException {
        JspWriter writer = getJspContext().getOut();
        writer.print("<span class=\"navigationBar\" style=\"white-space:nowrap;\">");
        writer.print(getLinkHtml("< Previous", getPreviousLink(currentPage)));
        writer.print("&nbsp;");
        writer.print(currentPage + 1);
        writer.print("&nbsp;");
        writer.print(getLinkHtml("Next >", getNextLink(currentPage)));
        writer.print("</span>");
    }

    /**
     * @return baseLink that is prefix common for all links
     */
    public String getBaseLink() {
        return baseLink;
    }

    /**
     * @return index of current page, 0-based
     */
    public Integer getCurrentPage() {
        return currentPage;
    }

    private String getLinkHtml(String title, String url) {
        if (url != null) {
            String ret = "<a  href=\"" + url + "\">" + title + "</a>";
            return ret;
        } else {
            return title;
        }
    }

    /**
     * @param pageIndex index
     * @return link to page next to the page with provided index
     */
    public String getNextLink(int pageIndex) {
        if (nextEnabled) {
            return getPageLink(pageIndex + 1);
        } else {
            return null;
        }
    }

    /**
     * @param pageIndex index
     * @return link to the page of provided index
     */
    public String getPageLink(int pageIndex) {
        return baseLink + "&start=" + (pageIndex * pageSize) + "&size=" + pageSize;
    }

    /**
     * @return page size that is number of result per page
     */
    public Integer getPageSize() {
        return pageSize;
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
     * @param baseLink that is prefix common for all links
     */
    public void setBaseLink(String baseLink) {
        this.baseLink = baseLink;
    }

    /**
     * @param currentPage index of page that should be current
     */
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * @param pageSize new page size
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @return html of navigation bar.
     */
    @Override
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
}
