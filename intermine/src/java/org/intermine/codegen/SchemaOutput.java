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

public class SchemaOutput
{
    static final String INDENT = "    ";
    static final String ID = "ID";
    static final String ENDL = System.getProperty("line.separator");

    Map tables = new HashMap();
    Table currentTable;
    String currentTableClass;
    Map classTable = new HashMap();

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
//             Iterator foreignKeys = table.foreignKeys.iterator();
//             while (foreignKeys.hasNext()) {
//                 ForeignKey k = (ForeignKey) foreignKeys.next();
//                 sb.append(INDENT + INDENT + "<foreign-key foreignTable=\"")
//                     .append((String) classTable.get(k.foreignClass) + "\">" + ENDL)
//                     .append(INDENT + INDENT + INDENT + "<reference local=\"")
//                     .append(k.local + "\" foreign=\"ID\"/>" + ENDL)
//                     .append(INDENT + INDENT + "</foreign-key>" + ENDL);
//             }
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
    
    void addTable(String tableName, boolean isIndirection) {
        if (tableName != null) {
            currentTable = (Table) tables.get(tableName);
            if (currentTable == null) {
                currentTable = new Table(isIndirection);
                tables.put(tableName, currentTable);
            }
        }
    }

    class MyHandler extends DefaultHandler
    {
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
                currentTable.columns.add(new Column(
                                                    attrs.getValue("column"), "INTEGER"));
                currentTable.foreignKeys.add(new ForeignKey(
                                                      attrs.getValue("column"), currentTableClass));
            }
            if (qName.equals("reference-descriptor")) {
                currentTable.foreignKeys.add(new ForeignKey(
                                       attrs.getValue("name") + "Id", attrs.getValue("class-ref")));
            }
        }   
    }
        
    class Table
    {
        boolean isIndirection;
        Set columns = new LinkedHashSet();
        Set foreignKeys = new LinkedHashSet();
        Table(boolean isIndirection) {
            this.isIndirection = isIndirection;
        }
    }
        
    class ForeignKey
    {
        String local, foreignClass;
        ForeignKey(String local, String foreignClass) {
            this.local = local;
            this.foreignClass = foreignClass;
        }
        public int hashCode() {
            return local.hashCode();
        }
        public boolean equals(Object obj) {
            return (obj != null && ((ForeignKey) obj).hashCode() == hashCode());
        }
    }

    class Column
    {
        String name, type;
        Column(String name, String type) {
            this.name = name;
            this.type = type;
        }
        public int hashCode() {
            return name.hashCode();
        }
        public boolean equals(Object obj) {
            return (obj != null && ((Column) obj).hashCode() == hashCode());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage:  SchemaOutput <repository filename> <output filename>");
            System.exit(1);
        }
        String repositoryFileName = args[0];
        String outputFileName = args[1];
        
        new SchemaOutput(repositoryFileName, outputFileName);
    }
}
