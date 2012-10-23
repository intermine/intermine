package org.intermine.install.project.source;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.junit.Before;
import org.junit.Test;


/**
 * Unit test for the SourceInfoLoader class.
 */
public class SourceInfoLoaderTest
{
    /**
     * Set up by initialising the SourceInfoLoader instance.
     *
     * @throws IOException thrown from {@link SourceInfoLoader#initialise()}
     * @throws JAXBException thrown from {@link SourceInfoLoader#initialise()}
     */
    @Before
    public void setup() throws IOException, JAXBException {
        try {
            SourceInfoLoader.getInstance().initialise();
        } catch (IllegalStateException e) {
            // Already loaded.
        }
    }

    /**
     * Test loading a FASTA data source.
     */
    @Test
    public void testLoadFastaSource() {

        SourceInfoLoader loader = SourceInfoLoader.getInstance();

        SourceInfo fastaInfo = loader.getSourceInfo("fasta");

        assertNotNull("No source info for 'fasta'", fastaInfo);

        assertEquals("Type wrong", "fasta", fastaInfo.getType());

        SourceDescriptor source = fastaInfo.getSource();
        assertEquals("SourceDescriptor type wrong", "fasta", source.getType());
        assertEquals("Incorrect number of properties in source", 9, source.getProperty().size());

        Map<String, PropertyDescriptor> propMap = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor p : source.getProperty()) {
            propMap.put(p.getName(), p);
        }

        PropertyDescriptor srcDataDir = propMap.get("src.data.dir");
        assertNotNull("No property 'src.data.dir'", srcDataDir);
        assertEquals("'src.data.dir' not required when it should be",
                     Boolean.TRUE, srcDataDir.isRequired());
        assertEquals("'src.data.dir' type wrong", PropertyType.DIRECTORY, srcDataDir.getType());
        assertNull("'src.data.dir' has validation", srcDataDir.getValidation());

        PropertyDescriptor taxonId = propMap.get("fasta.taxonId");
        assertNotNull("No property 'fasta.taxonId'", taxonId);
        assertEquals("'fasta.taxonId' required when it should not be",
                     Boolean.TRUE, taxonId.isRequired());
        assertEquals("'fasta.taxonId' type wrong", PropertyType.STRING, taxonId.getType());
        assertEquals("'fasta.taxonId' validation wrong",
                     "^\\d+(\\s\\d+)*$", taxonId.getValidation());
    }

    /**
     * Test loading a named derived source. Searches under the project
     * directory <code>src/test/sourcetest/project</code>, which in this
     * distribution has a dummy Intermine directory structure.
     *
     * @throws IOException if there is an I/O problem during the test.
     *
     * @see SourceInfoLoader#findDerivedSourceInfo(String, Project, File)
     */
    @Test
    public void testDerivedTypeLoad() throws IOException {

        File projectHome = new File("src/test/sourcetest/project");

        org.intermine.modelviewer.project.ObjectFactory factory =
            new org.intermine.modelviewer.project.ObjectFactory();

        Project project = factory.createProject();
        project.setSources(factory.createProjectSources());

        Property p = factory.createProperty();
        p.setName(SourceInfoLoader.SOURCE_PATH_PROPERTY);
        p.setLocation("../bio/sources");
        project.getProperty().add(p);

        p = factory.createProperty();
        p.setName(SourceInfoLoader.SOURCE_PATH_PROPERTY);
        p.setLocation("../bio/sources/example-sources");
        project.getProperty().add(p);

        //Source s = factory.createSource();
        //s.setName("malaria");
        //s.setType("malaria-gff");
        //project.getSources().getSource().add(s);

        SourceInfo info = SourceInfoLoader.getInstance().findDerivedSourceInfo(
                    "malaria-gff", project, projectHome);

        assertNotNull("Could not find derived source type for malaria-gff", info);

        assertEquals("Derived type is not gff", "malaria-gff", info.getType());
    }
}
