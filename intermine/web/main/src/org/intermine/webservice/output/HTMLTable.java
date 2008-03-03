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
import java.util.List;


/**
 * Formats data as HTML table. Code example: 
 * <pre>
 *   HTMLTable table = new HTMLTable();
 *   table.setColumnNames(columnNames);
 *   table.setRows(rows);
 *   String html = table.getHTML();
 * </pre>
 * @author Jakub Kulaviak
 **/
public class HTMLTable
{
    
    private List<String> columnNames;
    
    private List<List<String>> rows;
    
    private String title;
    
    private String description;
    
    /**
     * Returns description of data, that will be displayed above table. 
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     * @param description description
     * @see #getDescription()
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns column names.
     * @return column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Sets column names
     * @param columnNames column names
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }
    
    /**
     * Returns table title, that will be displayed above table.
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets table title.
     * @param title title
     * @see #getTitle()
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Constructor.
     */
    public HTMLTable() { }
    
    /**
     * Return html string of table.
     * @return html
     */
    public String getHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitleHtml());
        sb.append(getDescriptionHtml());
        sb.append("<table border=\"1px\" style=\"border-collapse:collapse;\">");
        sb.append(getHeaderHtml());
        sb.append(getBodyHtml());
        sb.append("</table>");
        return sb.toString();
    }

    private String getDescriptionHtml() {
        if (description == null) { return ""; }
        StringBuilder sb = new  StringBuilder();
        sb.append("<div class=\"description\">");
        sb.append(description);
        sb.append("</div>\n");
        return sb.toString();
    }

    private String getBodyHtml() {
        if (rows == null) { return ""; }
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            sb.append("<tr>\n");
            for (String cell : row) {
                sb.append("<td>");
                if (cell == null || cell.length() == 0) {
                    sb.append("&nbsp;");
                } else {
                    sb.append(cell);    
                }
                sb.append("</td>\n");
            }
            sb.append("</tr>\n");
        }
        return sb.toString();
    }

    private String getHeaderHtml() {
        if (columnNames == null) { return ""; }
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>\n");
        for (String name : columnNames) {
            sb.append("<th>");
            sb.append(name);
            sb.append("</th>\n");
        }
        sb.append("</tr>\n");
        return sb.toString();
    }

    private String getTitleHtml() {
        if (title == null) { return ""; }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"title\">");
        sb.append(title);
        sb.append("</div>\n");
        return sb.toString();
    }

    /** Returns rows of table. 
     * @return rows
     */
    public List<List<String>> getRows() {
        return rows;
    }

    /**
     * Sets rows of table.
     * @param rows rows
     */
    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }
}
