package org.intermine.web.tags.table;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;


/**
 * Class rendering html table.
 *
 * @author Jakub Kulaviak
 **/
public class TableTag extends SimpleTagSupport
{

    private List<String> columnNames;

    private List<List<String>> rows;

    private boolean treatColNames = false;

    private String noResultsMessage;

    /**
     * @return true if column names should be formatted from
     * 'Gene.name' to 'Gene > name' else false
     */
    public boolean isTreatColNames() {
        return treatColNames;
    }

    /**
     * @see #isTreatColNames()
     * @param treatColNames true if column names should be treated else false
     */
    public void setTreatColNames(boolean treatColNames) {
        this.treatColNames = treatColNames;
    }

    /**
     * {@inheritDoc}
     */
    public void doTag() throws IOException {
        JspWriter writer = getJspContext().getOut();
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1px\" style=\"border-collapse:collapse;\" "
            + "class=\"results\" cellspacing=\"0\">");
        sb.append(getHeaderHtml());
        sb.append(getBodyHtml());
        sb.append("</table>");
        writer.print(sb.toString());
    }

    private String getHeaderHtml() {
        if (columnNames == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>\n");
        for (String name : columnNames) {
            sb.append("<th align=\"center\">");
            if (isTreatColNames()) {
                sb.append(treatColumnName(name));
            } else {
                sb.append(name);
            }
            sb.append("</th>\n");
        }
        sb.append("</tr>\n");
        return sb.toString();
    }

    private String treatColumnName(String str) {
        String repl = "XXXXXXXXXXXXXXXXXXXXXXXX";
        String ret = str.replaceAll("[.][ ]", repl);
        ret = ret.replaceAll("[.]", "&nbsp;> ");
        ret = ret.replaceAll(repl, ". ");
        return ret;
    }

    /**
     * @return table rows
     */
    public List<List<String>> getRows() {
        return rows;
    }

    /**
     * Set table rows.
     * @param rows table rows
     */
    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }

    /**
     * @return column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * @param columnNames column names
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    private String getBodyHtml() {
        if (rows == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (rows.size() == 0) {
            String ret = "<tr><td colspan=\"" + getColumnsCount() + "\">"
                + getNoResultsMessage() + "</td>";
            ret += "</tr>";
            return ret;
        }
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            sb.append("<tr>\n");
            int cellIndex;
            for (cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                String cell = row.get(cellIndex);
                sb.append("<td>");
                if (cell == null || cell.length() == 0) {
                    sb.append("&nbsp;");
                } else {
                    sb.append(cell);
                }
                sb.append("</td>\n");
            }
            sb.append(getEmptyCellsHtml(getColumnsCount() - cellIndex - 1));
            sb.append("</tr>\n");
        }
        return sb.toString();
    }

    /**
     * @return error message that is displayed when there are no results
     */
    public String getNoResultsMessage() {
        if (noResultsMessage != null) {
            return noResultsMessage;
        } else {
            return "There are no results. If you browsed through results go to the previous page.";
        }
    }

    /**
     * @param noResultsMessage error message that is displayed when there are no results
     */
    public void setNoResultsMessage(String noResultsMessage) {
        this.noResultsMessage = noResultsMessage;
    }

    private int getColumnsCount() {
        if (columnNames != null) {
            return columnNames.size();
        }
        return 0;
    }

    private String getEmptyCellsHtml(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("<td>&nbsp;</td>\n");
        }
        return sb.toString();
    }
}
