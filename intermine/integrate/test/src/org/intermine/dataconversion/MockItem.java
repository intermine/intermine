package org.intermine.dataconversion;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

/**
 * Class used in place of Item for testing.  Uses internal identifier to map relationships to
 * other items, then throws the identifier away.
 *
 * Often when you add an item to
 * @author julie
 */
public class MockItem {
    private String identifier = "DUMMY";
    private String className = "";
    private String implementations = "";
    private Map<String, String> attributes = new HashMap();
    private Map<String, MockItem> references = new LinkedHashMap();
    private Map<String, List<MockItem>> collections = new LinkedHashMap();
    private static final String ENDL = System.getProperty("line.separator");

    /**
     * Constructor
     * @param item item to replace with this mock item
     */
    public MockItem(Item item) {
        this.identifier = item.getIdentifier();
        this.className = item.getClassName();
        this.implementations = item.getImplementations();
        for (Attribute a : item.getAttributes()) {
            attributes.put(a.getName(), a.getValue());
        }
    }

    /**
     * @param name name of reference
     * @return reference reference
     */
    public MockItem getMockReference(String name) {
        MockItem reference = references.get(name);
        return reference;
    }

    /**
     * @param name name of reference
     * @param item item
     */
    public void addMockReference(String name, MockItem item) {
        this.references.put(name, item);
    }

    /**
     * @param references the references to set
     */
    public void setMockReferences(Map<String, MockItem> references) {
        this.references = references;
    }

    /**
     * @param collections the collections to set
     */
    public void setMockCollections(Map<String, List<MockItem>> collections) {
        this.collections = collections;
    }

    /**
     * @param name name of collection
     * @param collection collection
     */
    public void addMockCollection(String name, List collection) {
        this.collections.put(name, collection);
    }

    /**
     * @return the identifier
     */
    public String getMockIdentifier() {
        return this.identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setMockIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the className
     */
    public String getMockClassName() {
        return this.className;
    }

    /**
     * @param className the className to set
     */
    public void setMockClassName(String className) {
        this.className = className;
    }

    /**
     * @return the implementations
     */
    public String getMockImplementations() {
        return this.implementations;
    }

    /**
     * @param implementations the implementations to set
     */
    public void setMockImplementations(String implementations) {
        this.implementations = implementations;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the implementations
     */
    public String getImplementations() {
        return implementations;
    }

    /**
     * @param implementations the implementations to set
     */
    public void setImplementations(String implementations) {
        this.implementations = implementations;
    }


    /**
     * @return the references
     */
    public Collection getReferences() {
        return references.values();
    }

    /**
     * @param references the references to set
     */
    public void setReferences(Map<String, MockItem> references) {
        this.references = references;
    }

    /**
     * @param collections the collections to set
     */
    public void setCollections(Map collections) {
        this.collections = collections;
    }

    /**
     * used for comparison
     * @return the attributes
     */
    public String getMockAttributes() {
        String xml = "";
        Object[] key = attributes.keySet().toArray();
        Arrays.sort(key);
        for (int i = 0; i < key.length; i++) {
            xml += "\t<attribute name=\"" + key[i] + "\" value=\"" + attributes.get(key[i])
            + "\"/>" + ENDL;
        }
        return xml;
    }

    /**
     * used for comparison
     * @return the references
     */
    public String getMockReferences() {
        String xml = "";
        for (Map.Entry<String, MockItem> entry : references.entrySet()) {
            xml += "\t<reference name=\"" + entry.getKey() + "\">" + ENDL;
            MockItem item = entry.getValue();
            xml += item.referencedItemXML();
            xml += "\t</reference>" + ENDL;
        }
        return xml;
    }

    /**
     * used for comparison
     * @return the collections
     */
    public String getMockCollections() {
        String xml = "";
        Object[] key = collections.keySet().toArray();
        Arrays.sort(key);
        for (int i = 0; i < key.length; i++) {
            List<MockItem> c = collections.get(key[i]);
            xml += "\t<collection name=\"" + key[i] + "\">";
            for (MockItem item : c) {
                xml += item.referencedItemXML();
            }
            xml += "\t</collection>" + ENDL;
        }
        return xml;
    }

    /**
     * this string is used when comparing items in references and collections.  XML omits the
     * identifier and only includes attributes of the item.
     * @return string representing a reference for comparison
     */
    public String referencedItemXML() {
        String xml = "\t<item id=\"DUMMY\" class=\"" + className + "\">" + ENDL;
        xml += getMockAttributes();
        xml += "\t</item>" + ENDL;
        return xml;
    }


    /**
     * used for displaying collections
     * @return the references to display
     */
    public String getPrettyReferences() {
        String xml = "";
        for (Map.Entry<String, MockItem> entry : references.entrySet()) {
            xml += "\t<reference name=\"" + entry.getKey() + "\" ref_id=\"" + identifier + "\"\\>"
                + ENDL;
        }
        return xml;
    }

    /**
     * used for display
     * @return the collections
     */
    public String getPrettyCollections() {
        String xml = "";
        Object[] key = collections.keySet().toArray();
        Arrays.sort(key);
        for (int i = 0; i < key.length; i++) {
            List<MockItem> c = collections.get(key[i]);
            xml += "<collection name=\"" + key[i] + "\">" + ENDL;
            for (MockItem item : c) {
                xml += "\t<reference name=\"" + item.getMockClassName() + "\" ref_id=\""
                + item.getIdentifier() + "\">" + ENDL;
            }
            xml += "</collection>" + ENDL;
        }
        return xml;
    }

    /**
    *
    * @return string representing an item for display only
    */
   public String toXML() {
       String xml = "<item id=\"" + identifier + "\" class=\"" + className + "\">" + ENDL;
       xml += getMockAttributes();
       xml += getPrettyReferences();
       xml += getPrettyCollections();
       xml += "</item>" + ENDL;
       return xml;
   }

    public boolean equals(Object o) {
        MockItem i = (MockItem) o;
        return className.equals(i.getMockClassName())
        && getMockImplementations().equals(i.getMockImplementations())
        && attributes.equals(i.attributes)
        && getMockReferences().equals(i.getMockReferences())
        && getMockCollections().equals(i.getMockCollections());
    }
}
