package org.intermine.model.testmodel.web;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.metadata.TypeUtil;

import org.intermine.model.testmodel.Bank;


public class RedirectManager extends LinkRedirectManager
{

    protected static final Logger LOG = Logger.getLogger(RedirectManager.class);

    /**
     * @param webProperties the web properties
     */
    public RedirectManager(Properties webProperties) {
        super(webProperties);
    }

    @Override
    public String generateLink(InterMineAPI im, InterMineObject imo) {
        if (imo instanceof Bank) {
            try {
                return String.format("http://www.%s.com",
                    TypeUtil.getFieldValue(imo, "name").toString().trim().toLowerCase().replaceAll("\\s", "-"));
            } catch (Exception e) {
                LOG.error("When constructing url", e);
            }
        }
        return null;
    }
}
