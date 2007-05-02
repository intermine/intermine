package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * This class contains various methods used by the protein interaction exporters
 * @author Florian Reisinger
 */
public class PIUtil
{
    /**
     * create the jnlp string
     * @param sif name of the sif network file
     * @return String representing the jnlp file
     */
    public static String buildJNLP(String sif) {
        String tmp = "" 
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" 
            + "<jnlp\n"
              + "\tcodebase=\"http://aragorn:8080/cytoscape-production\">\n"
              + "\t<information>\n"
                + "\t\t<title>Cytoscape WebStart</title>\n"
                + "\t\t<vendor>Cytoscape Collaboration</vendor>\n"
                + "\t\t<offline-allowed/>\n" 
              + "\t</information>\n" 
              + "\t<security>\n"
                + "\t\t<all-permissions/>\n" 
              + "\t</security>\n" 
              + "\t<resources>\n"
                + "\t\t<j2se version=\"1.4+\" max-heap-size=\"400M\"/>\n"
                + "\t\t<jar href=\"cytoscape.jar\"/>\n"
                + "\t\t<jar href=\"lib/coltginy.jar\"/>\n"
                + "\t\t<jar href=\"lib/colt.jar\"/>\n"
                + "\t\t<jar href=\"lib/com-nerius-math-xform.jar\"/>\n"
                + "\t\t<jar href=\"lib/concurrent.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-cruft-obo.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-graph-dynamic.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-graph-fixed.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-graph-layout.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-graph-legacy.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-task.jar\"/>\n"
                + "\t\t<jar href=\"lib/cytoscape-util-intr.jar\"/>\n"
                + "\t\t<jar href=\"lib/fing.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-base.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphics2d.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphicsio-gif.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphicsio.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphicsio-pdf.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphicsio-ps.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphicsio-svg.jar\"/>\n"
                + "\t\t<jar href=\"lib/freehep-graphicsio-swf.jar\"/>\n"
                + "\t\t<jar href=\"lib/getopt.jar\"/>\n"
                + "\t\t<jar href=\"lib/giny.jar\"/>\n"
                + "\t\t<jar href=\"lib/glf.jar\"/>\n"
                + "\t\t<jar href=\"lib/jdom.jar\"/>\n"
                + "\t\t<jar href=\"lib/jfreechart-0.9.20.jar\"/>\n"
                + "\t\t<jar href=\"lib/jfreechart-common-0.9.5.jar\"/>\n"
                + "\t\t<jar href=\"lib/jhall.jar\"/>\n"
                + "\t\t<jar href=\"lib/jnlp.jar\"/>\n"
                + "\t\t<jar href=\"lib/junit.jar\"/>\n"
                + "\t\t<jar href=\"lib/looks-1.1.3.jar\"/>\n"
                + "\t\t<jar href=\"lib/phoebe.jar\"/>\n"
                + "\t\t<jar href=\"lib/piccolo.jar\"/>\n"
                + "\t\t<jar href=\"lib/piccolox.jar\"/>\n"
                + "\t\t<jar href=\"lib/tclib.jar\"/>\n"
                + "\t\t<jar href=\"lib/violinstrings-1.0.2.jar\"/>\n"
                + "\t\t<jar href=\"lib/wizard.jar\"/>\n"
                + "\t\t<jar href=\"lib/xercesImpl-2.6.1.jar\"/>\n"
                + "\t\t<jar href=\"lib/xml-apis.jar\"/>\n"
                + "\t\t<jar href=\"plugins/filter.jar\"/>\n"
                + "\t\t<jar href=\"plugins/yLayouts.jar\"/>\n"
                + "\t\t<jar href=\"plugins/browser.jar\"/>\n"
                + "\t\t<jar href=\"plugins/exesto.jar\"/>\n"
                + "\t\t<jar href=\"plugins/rowan.jar\"/>\n"
                + "\t\t<extension name=\"Info\" href=\"info.jnlp\"/>\n"
              + "\t</resources>\n"
              + "\t<application-desc main-class=\"cytoscape.CyMain\">\n"
                + "\t\t<argument>-i</argument>\n" 
                + "\t\t<argument>jar://" + sif + "</argument>\n" 
                + "\t\t<argument>-rp</argument>\n"
                + "\t\t<argument>filter.cytoscape.CsFilter</argument>\n"
                + "\t\t<argument>-rp</argument>\n"
                + "\t\t<argument>org.flymine.plugin.FlyMinePlugin</argument>\n"
                + "\t\t<argument>-rp</argument>\n"
                + "\t\t<argument>yfiles.YFilesLayoutPlugin</argument>\n"
                + "\t\t<argument>-rp</argument>\n"
                + "\t\t<argument>browser.BrowserPlugin</argument>\n"
                + "\t\t<argument>-rp</argument>\n"
                + "\t\t<argument>rowan.RowanPlugin</argument>\n"
              + "\t</application-desc>\n" 
            + "</jnlp>";
        return tmp;

    }
}
