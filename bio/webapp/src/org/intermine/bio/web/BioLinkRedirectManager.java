package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.LinkRedirectManager;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Organism;
import org.intermine.util.TypeUtil;

/**
 * Contructs an alternate object details link pointing to an external URL for use in results table.
 *
 * Returns NULL if there is no config for given object.
 *
 * @author Julie Sullivan
 */
public class BioLinkRedirectManager extends LinkRedirectManager
{
    protected static final Logger LOG = Logger.getLogger(BioLinkRedirectManager.class);

    /**
     * @param webProperties the web properties
     */
    public BioLinkRedirectManager(Properties webProperties) {
        super(webProperties);
    }

    @Override
    public String generateLink(InterMineAPI im, InterMineObject imo) {

        Model model = im.getModel();
        Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(imo.getClass());

        StringBuffer sb = new StringBuffer();
        for (ClassDescriptor cd : classDescriptors) {
            if (sb.length() <= 0) {
                sb.append("(");
            } else {
                sb.append("|");
            }
            sb.append(TypeUtil.unqualifiedName(cd.getName()));
        }
        sb.append(")");
        Organism organismReference = null;
        String geneOrgKey = sb.toString();

        try {
            organismReference = (Organism) imo.getFieldValue("organism");
        } catch (Exception e) {
            // no organism field
        }

        if (organismReference == null || organismReference.getTaxonId() == null) {
            geneOrgKey += "(\\.(\\*))";
        } else {
            // we need to check against * as well in case we want it to work for all taxonIds
            geneOrgKey += "(\\.(" + organismReference.getTaxonId() + "|\\*))";
        }
        String url = null;
        // externalLink.flybaseLink.Gene.7227.primaryIdentifier.url = http://google.com
        final String regexp = "externallink\\.([^.]+)\\." + geneOrgKey
            + "\\.([^.]+)(\\.list)?\\.(url)";
        Pattern p = Pattern.compile(regexp);
        for (Map.Entry<Object, Object> entry: webProperties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            Matcher matcher = p.matcher(key);
            if (matcher.matches()) {
                String attrName = matcher.group(5);
                Object attrValue = null;
                try {
                    attrValue = imo.getFieldValue(attrName);
                } catch (IllegalAccessException e) {
                    return null;
                }
                if (value.contains(ATTR_MARKER_RE)) {
                    url = value.replaceAll(ATTR_MARKER_RE, String.valueOf(attrValue));
                } else {
                    url = value + attrValue;
                }
            }
        }
        return url;
    }
}
