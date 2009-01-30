package org.intermine.dataconversion;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.util.SortableMap;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

public class MockItem {
    private String identifier = "DUMMY";
    private String className = "";
    private String implementations = "";
    private SortableMap attributes = new SortableMap();
    private Map<String, MockItem> references = new LinkedHashMap();
    private Map<String, List<MockItem>> collections = new SortableMap();
    public static final String ENDL = System.getProperty("line.separator");

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
     * @param item item
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
     * @return the attributes
     */
    public String getMockAttributes() {
        String xml = "";
        attributes.sortKeys();
        Map<String, String> sortedAttributes = new HashMap(attributes);
        for (Map.Entry a : sortedAttributes.entrySet()) {
            xml += "\t<attribute name=\"" + a.getKey() + "\" value=\"" + a.getValue() + "\"/>" + ENDL;
        }
        return xml;
    }

    /**
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
                //xml += "<reference ref_id=\"" + item.getIdentifier() + "\"/>";
                xml += item.referencedItemXML();
            }
            xml += "\t</collection>" + ENDL;
        }
        return xml;
    }

    /**
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

    public String toXML() {
        String xml = "<item id=\"" + identifier + "\" class=\"" + className + "\">" + ENDL;
        xml += getMockAttributes();
        xml += getMockReferences();
        xml += getMockCollections();
        xml += "</item>" + ENDL;
        return xml;
    }

    public String referencedItemXML() {
        String xml = "\t<item id=\"DUMMY\" class=\"" + className + "\">" + ENDL;
        xml += getMockAttributes();
        xml += "\t</item>" + ENDL;
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
