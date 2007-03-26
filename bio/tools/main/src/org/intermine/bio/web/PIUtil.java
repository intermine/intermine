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

import java.util.List;

import org.flymine.model.genomic.ProteinInteraction;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.web.results.Column;
import org.intermine.web.results.PagedTable;

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

    /**
     * methode to retrieve the index of the first column which contains
     * protein intaractions
     * @param columns list of columns to scan for protein interactions
     * @return the index of the first valid column
     */
    protected static int getValidColumnIndex(List columns) {
        int index = -1;

        // find and remember the first valid ProteinInteraction-containing column
        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);
            if (column.isVisible()) {
                Object columnType = column.getType();
                if (columnType instanceof ClassDescriptor) {
                    if (PIUtil.validType(((ClassDescriptor) columnType).getType())) {
                        index = column.getIndex();
                        break;
                    }
                }
            }
        }
        return index;
    }

    /**
     * @param pt the PagedTable containing the results 
     * @return true if exportable results were found
     * @see org.intermine.web.TableExporter#canExport
     */
    public static boolean canExport(PagedTable pt) {
        List columns = pt.getColumns();

        for (int i = 0; i < columns.size(); i++) {
            Column column = (Column) columns.get(i);
            if (column.isVisible()) {
                Object columnType = ((Column) columns.get(i)).getType();

                if (columnType instanceof ClassDescriptor) {
                    ClassDescriptor cd = (ClassDescriptor) columnType;
                    if (PIUtil.validType(cd.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check whether the argument is one of the types we handle
     * -> we are looking for ProteinInteraction
     * @param type the type
     * @return true if we handle the type
     */
    protected static boolean validType(Class type) {
        return (ProteinInteraction.class.isAssignableFrom(type)); //flo
    }

}
