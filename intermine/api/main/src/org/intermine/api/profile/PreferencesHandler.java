package org.intermine.api.profile;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PreferencesHandler extends DefaultHandler {

    private final static Logger LOG = Logger.getLogger(PreferencesHandler.class);

    private final Map<String, String> preferences;

    private String currentName = null;
    private StringBuffer currentValue = null;

    public PreferencesHandler(Map<String, String> preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("preferences cannot be null");
        }
        this.preferences = preferences;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("preferences".equals(qName)) {
            currentName = null;
            currentValue = null;
        } else {
            currentName = qName;
            currentValue = new StringBuffer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (currentValue != null) currentValue.append(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (currentName != null && currentValue != null) {
            String value = currentValue.toString().trim();
            if (StringUtils.isNotBlank(value)) {
                preferences.put(currentName, value);
            }
        }
        currentValue = null;
        currentName = null;
    }
}
