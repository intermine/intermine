/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Derive torque db schema from OJB repository file
 *
 * @author Mark Woodbridge
 */
public class SchemaOutput
{
    static final String INDENT = "    ";
    static final String ID = "ID";
    static final String ENDL = System.getProperty("line.separator");

    Map tables = new HashMap();
    Table currentTable;
    String currentTableClass;
    Map classTable = new HashMap();

    /**
     * Constructor
     * @param repositoryFileName the filename of the input repository
     * @param outputFileName the filename of the output schema
     * @throws Exception if the was a problem parsing the repository or outputting the schema
     */
    public SchemaOutput(String repositoryFileName, String outputFileName)
        throws Exception {
        SAXParserFactory.newInstance().newSAXParser().parse(
                                               new File(repositoryFileName), new MyHandler());

        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" + ENDL)
            .append("<!DOCTYPE database SYSTEM \"" 
                    + "http://jakarta.apache.org/turbine/dtd/database.dtd\">" + ENDL)
            .append("<database name=\"\">" + ENDL);
        Iterator entries = tables.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String tableName = (String) entry.getKey();
            Table table = (Table) entry.getValue();
            if (tableName.startsWith("OJB")) {
                continue;
            }
            sb.append(INDENT + "<table name=\"" + tableName + "\">" + ENDL);
            Iterator columns = table.columns.iterator();
            while (columns.hasNext()) {
                Column c = (Column) columns.next();
                if (c.name.equals(ID)) {
                    sb.append(INDENT + INDENT + "<column name=\"") 
                        .append(c.name + "\" required=\"true\"")
                        .append(" primaryKey=\"true\" type=\"" + c.type + "\" />" + ENDL);
                } else {
                    sb.append(INDENT + INDENT + "<column name=\"")
                        .append(c.name + "\" type=\"" + c.type + "\" />" + ENDL);
                }
            }
            sb.append(INDENT + "</table>" + ENDL);
        }
        sb.append("</database>" + ENDL);
        
        File outputFile = new File(outputFileName);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        BufferedWriter fos = new BufferedWriter(new FileWriter (outputFile));
        fos.write(sb.toString());
        fos.close();
    }

    /**
     * Add a table to the list if it doesn't exist
     * @param tableName the name of table
     * @param isIndirection  whether this table is for non-decomposed M:N mappings
     */
    protected void addTable(String tableName, boolean isIndirection) {
        if (tableName != null) {
            currentTable = (Table) tables.get(tableName);
            if (currentTable == null) {
                currentTable = new Table(isIndirection);
                tables.put(tableName, currentTable);
            }
        }
    }

    /**
     * Add a column to the specified table if it doesn't already exist
     * @param table the table
     * @param column the column
     */
    protected void addColumn(Table table, Column column) {
        if (!table.columns.contains(column)) {
            table.columns.add(column);
        }
    }

    /**
     * Custom handler for SAX events
     */
    protected class MyHandler extends DefaultHandler
    {
        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.equals("class-descriptor")) {
                addTable(attrs.getValue("table"), false);
                currentTableClass = attrs.getValue("class");
                classTable.put(attrs.getValue("class"), attrs.getValue("table"));
            }
            if (qName.equals("field-descriptor")) {
                currentTable.columns.add(new Column(
                                            attrs.getValue("column"), attrs.getValue("jdbc-type")));
            }
            if (qName.equals("collection-descriptor")) {
                addTable(attrs.getValue("indirection-table"), true); // this alters currentTable
            }
            if (qName.equals("fk-pointing-to-this-class")) {
                addColumn(currentTable, new Column(attrs.getValue("column"), "INTEGER"));
            }
            if (qName.equals("fk-pointing-to-element-class")) {
                addColumn(currentTable, new Column(attrs.getValue("column"), "INTEGER"));
            }
        }   
    }
        
    /**
     * Class to represent table in schema
     */
    protected class Table
    {
        boolean isIndirection;
        Set columns = new LinkedHashSet();
        Set foreignKeys = new LinkedHashSet();
        /**
         * Constructor
         * @param isIndirection whether this table is for non-decomposed M:N mappings
         */
        Table(boolean isIndirection) {
            this.isIndirection = isIndirection;
        }
    }

    /**
     * Class to represent column of table in schema
     */
    protected class Column
    {
        String name, type;
        /**
         * Constructor
         * @param name column name
         * @param type the type of the column
         */
        Column(String name, String type) {
            this.name = name;
            this.type = type;
        }
        /**
         * @see Object#hashCode
         */
        public int hashCode() {
            return name.hashCode();
        }
        /**
         * @see Object#equals
         */
        public boolean equals(Object obj) {
            return (obj != null && ((Column) obj).hashCode() == hashCode());
        }
    }

    /**
     * main method (soon to be replaced by a task)
     * @param args the command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        new SchemaOutput(args[0], args[1]);
    }
}
