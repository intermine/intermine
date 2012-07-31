package org.intermine.modelviewer.jaxb;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;

import org.apache.commons.collections15.CollectionUtils;
import org.intermine.modelviewer.genomic.Attribute;
import org.intermine.modelviewer.genomic.Class;
import org.intermine.modelviewer.genomic.ClassReference;
import org.intermine.modelviewer.genomic.Classes;
import org.intermine.modelviewer.genomic.Model;
import org.intermine.modelviewer.project.PostProcess;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.intermine.modelviewer.project.Source;
import org.junit.Before;
import org.junit.Test;


/**
 * JUnit 4 test class for ConfigParser.
 */
public class ConfigParserTest
{
    /**
     * The ConfigParser to use in the tests.
     */
    protected ConfigParser parser;
   
    /**
     * Set up a clean, new ConfigParser before each test.
     * 
     * @throws Exception if there is a problem creating the parser.
     */
    @Before
    public void setUp() throws Exception {
        parser = new ConfigParser();
    }
    
    /**
     * Test loading a core file that has proper schema headers.
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testLoadCoreFile1() throws Exception {
        
        // With schema declarations.
        File coreFile = new File("src/test/schema/core.xml");
        
        Model model = parser.loadCoreFile(coreFile);
        coreCorrect(model, coreFile);
    }
        
    /**
     * Test loading a core file that has no schema headers.
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testLoadCoreFile2() throws Exception {
            
        // Without schema declarations.
        File coreFile = new File("src/test/schema/noschema/core.xml");
        
        Model model = parser.loadCoreFile(coreFile);
        coreCorrect(model, coreFile);
    }
    
    /**
     * Test writing and reloading a core file and ensuring the result is the same.
     * <p>Will fail if {@link #testLoadCoreFile1()} fails.</p>
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testWriteCoreFile() throws Exception {
        
        File originalFile = new File("src/test/schema/core.xml");
        Model model = parser.loadCoreFile(originalFile);
        
        File tempFile = File.createTempFile("cpTest", ".xml");
        tempFile.deleteOnExit();
        
        Writer out = new FileWriter(tempFile);
        parser.writeCoreFile(model, out);
        out.close();
        
        Model reloaded = parser.loadCoreFile(tempFile);
        
        modelEquality(model, reloaded);
    }
    
    /**
     * Test whether the model read from <code>src/test/(no)schema/core.xml</code>
     * is correct.
     * 
     * @param model The core Model.
     * 
     * @param sourceFile The source file.
     */
    private void coreCorrect(Model model, File sourceFile) {
        
        try {
            assertEquals("Model name wrong", "genomic", model.getName());
            assertEquals("Package wrong", "org.intermine.model.bio", model.getPackage());
            
            assertEquals("Wrong number of classes", 34, model.getClazz().size());
            
            Map<String, Class> classMap = new HashMap<String, Class>();
            for (Class c : model.getClazz()) {
                classMap.put(c.getName(), c);
            }
            
            Class relation = classMap.get("Relation");
            assertNotNull("Class 'Relation' not found", relation);
            
            assertEquals("Relation extends wrong", "SymmetricalRelation", relation.getExtends());
            assertTrue("Relation interface wrong", relation.isIsInterface());
            assertEquals("Relation should have no attributes", 0, relation.getAttribute().size());
            
            assertNotNull("Relation should have 2 references (list unset)",
                          relation.getReference());
            assertEquals("Relation should have 2 references", 2, relation.getReference().size());
            ClassReference ref = relation.getReference().get(0);
            assertEquals("Reference name wrong", "subject", ref.getName());
            assertEquals("Reference type wrong", "BioEntity", ref.getReferencedType());
            assertEquals("Reference reverse wrong", "objects", ref.getReverseReference());
            
            assertNotNull("Relation should have 2 collections (list unset)",
                          relation.getCollection());
            assertEquals("Relation should have 2 collections", 2, relation.getCollection().size());
            ClassReference col = relation.getCollection().get(0);
            assertEquals("Collection name wrong", "evidence", col.getName());
            assertEquals("Collection type wrong", "Evidence", col.getReferencedType());
            assertEquals("Collection reverse wrong", "relations", col.getReverseReference());
            
            Class comment = classMap.get("Comment");
            assertNotNull("Class 'Comment' not found", comment);
            
            assertNull("Comment extends wrong", comment.getExtends());
            assertTrue("Comment interface wrong", comment.isIsInterface());
            
            assertEquals("Comment should have 2 attributes", 2, comment.getAttribute().size());
            Attribute att = comment.getAttribute().get(0);
            assertEquals("Attribute name wrong", "text", att.getName());
            assertEquals("Attribute type wrong", String.class.getName(), att.getType());
            
            assertNotNull("Comment should have 1 reference (list unset)", comment.getReference());
            assertEquals("Comment should have 1 reference", 1, comment.getReference().size());
            ref = comment.getReference().get(0);
            assertEquals("Reference name wrong", "source", ref.getName());
            assertEquals("Reference type wrong", "InfoSource", ref.getReferencedType());
            assertNull("Reference reverse wrong", ref.getReverseReference());
            
            assertEquals("Comment should have 0 collections", 0, comment.getCollection().size());

        } catch (AssertionFailedError e) {
            AssertionFailedError addition =
                new AssertionFailedError("Failure with file " + sourceFile.getAbsolutePath()
                                         + " :\n" + e.getMessage());
            addition.initCause(e.getCause());
            addition.setStackTrace(e.getStackTrace());
            throw addition;
        }
    }

    /**
     * Test loading a genomic additions file that has proper schema headers.
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testLoadGenomicFile1() throws Exception {

        // With schema declarations.
        File genomicFile = new File("src/test/schema/genomic_additions.xml");
        
        Classes classes = parser.loadGenomicFile(genomicFile);
        genomicAdditionsCorrect(classes, genomicFile);
    }

    /**
     * Test loading a genomic additions file that has no schema headers.
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testLoadGenomicFile2() throws Exception {

        // Without schema declarations.
        File genomicFile = new File("src/test/schema/noschema/genomic_additions.xml");
        
        Classes classes = parser.loadGenomicFile(genomicFile);
        genomicAdditionsCorrect(classes, genomicFile);
    }
    
    /**
     * Test writing and reloading a genomic additions file and ensuring the result is the same.
     * <p>Will fail if {@link #testLoadGenomicFile1()} fails.</p>
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testWriteGenomicFile() throws Exception {
        
        File originalFile = new File("src/test/schema/genomic_additions.xml");
        Classes classes = parser.loadGenomicFile(originalFile);
        
        File tempFile = File.createTempFile("cpTest", ".xml");
        tempFile.deleteOnExit();
        
        Writer out = new FileWriter(tempFile);
        parser.writeGenomicFile(classes, out);
        out.close();
        
        Classes reloaded = parser.loadGenomicFile(tempFile);
        
        genomicEquality(classes, reloaded);
    }
    
    /**
     * Test whether the model read from <code>src/test/(no)schema/genomic_additions.xml</code>
     * is correct.
     * 
     * @param classes The top level Classes object.
     * 
     * @param sourceFile The source file.
     */
    private void genomicAdditionsCorrect(Classes classes, File sourceFile) {
        try {
            
            assertEquals("Wrong number of classes", 23, classes.getClazz().size());
            
            Map<String, Class> classMap = new HashMap<String, Class>();
            for (Class c : classes.getClazz()) {
                classMap.put(c.getName(), c);
            }
            
            Class transcript = classMap.get("Transcript");
            assertNotNull("Class 'Transcript' not found", transcript);
            
            assertNull("Transcript extends wrong", transcript.getExtends());
            assertTrue("Transcript interface wrong", transcript.isIsInterface());
            
            assertEquals("Transcript should have 1 attribute", 1, transcript.getAttribute().size());
            Attribute att = transcript.getAttribute().get(0);
            assertEquals("Attribute name wrong", "exonCount", att.getName());
            assertEquals("Attribute type wrong", Integer.class.getName(), att.getType());
            
            assertEquals("Transcript should have 1 reference", 1, transcript.getReference().size());
            ClassReference ref = transcript.getReference().get(0);
            assertEquals("Reference name wrong", "protein", ref.getName());
            assertEquals("Reference type wrong", "Protein", ref.getReferencedType());
            assertEquals("Reference reverse wrong", "transcripts", ref.getReverseReference());
            
            assertNotNull("Transcript should have 2 collections (list unset)",
                          transcript.getCollection());
            assertEquals("Transcript should have 2 collections", 2,
                         transcript.getCollection().size());
            ClassReference col = transcript.getCollection().get(0);
            assertEquals("Collection name wrong", "introns", col.getName());
            assertEquals("Collection type wrong", "Intron", col.getReferencedType());
            assertEquals("Collection reverse wrong", "transcripts", col.getReverseReference());

        } catch (AssertionFailedError e) {
            AssertionFailedError addition =
                new AssertionFailedError("Failure with file " + sourceFile.getAbsolutePath()
                                         + " :\n" + e.getMessage());
            addition.initCause(e.getCause());
            addition.setStackTrace(e.getStackTrace());
            throw addition;
        }
    }

    /**
     * Test loading a project file that has proper schema headers.
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testLoadProjectFile1() throws Exception {

        // With schema declarations.
        File projectFile = new File("src/test/schema/project.xml");
        
        Project project = parser.loadProjectFile(projectFile);
        
        projectCorrect(project, projectFile);
    }

    /**
     * Test loading a project file that has no schema headers.
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testLoadProjectFile2() throws Exception {

        // Without schema declarations.
        File projectFile = new File("src/test/schema/noschema/project.xml");
        
        Project project = parser.loadProjectFile(projectFile);
        
        projectCorrect(project, projectFile);
    }

    /**
     * Test whether the project read from <code>src/test/(no)schema/project.xml</code>
     * is correct.
     * 
     * @param project The Project.
     * 
     * @param sourceFile The source file.
     */
    private void projectCorrect(Project project, File sourceFile) {
        
        try {
            assertEquals("Project type wrong", "bio", project.getType());
            
            assertEquals("Wrong number of project properties", 6, project.getProperty().size());

            // Ignore duplicate source.location
            Map<String, Property> propMap = new HashMap<String, Property>();
            for (Property p : project.getProperty()) {
                propMap.put(p.getName(), p);
            }
            
            Property propsFile = propMap.get("default.intermine.properties.file");
            assertNotNull("Property 'default.intermine.properties.file' missing", propsFile);
            assertEquals("'default.intermine.properties.file' location wrong",
                         "../default.intermine.integrate.properties", propsFile.getLocation());
            assertNull("'default.intermine.properties.file' value set", propsFile.getValue());
            
            Property targetModel = propMap.get("target.model");
            assertNotNull("Property 'target.model' missing", targetModel);
            assertEquals("'target.model' value wrong", "genomic", targetModel.getValue());
            assertNull("'target.model' location set", targetModel.getLocation());
            
            assertEquals("Wrong number of project sources",
                         8, project.getSources().getSource().size());
            
            Map<String, Source> sourceMap = new HashMap<String, Source>();
            for (Source s : project.getSources().getSource()) {
                sourceMap.put(s.getName(), s);
            }
            
            Source chromoFasta = sourceMap.get("malaria-chromosome-fasta");
            assertNotNull("Source 'malaria-chromosome-fasta' missing", chromoFasta);
            assertEquals("'malaria-chromosome-fasta' type wrong", "fasta", chromoFasta.getType());
            assertEquals("'malaria-chromosome-fasta' dump wrong",
                         Boolean.TRUE, chromoFasta.isDump());
            
            assertEquals("'malaria-chromosome-fasta' source has wrong number of properties",
                         6, chromoFasta.getProperty().size());
            
            propMap.clear();
            for (Property p : chromoFasta.getProperty()) {
                propMap.put(p.getName(), p);
            }
            
            Property srcDataDir = propMap.get("src.data.dir");
            assertNotNull("Property 'src.data.dir' missing from source 'malaria-chromosome-fasta'",
                          srcDataDir);
            assertEquals("'src.data.dir' location wrong",
                         "/home/richard/malaria/genome/fasta", srcDataDir.getLocation());
            assertNull("'src.data.dir' value set", srcDataDir.getValue());
            
            Property fastaTitle = propMap.get("fasta.dataSourceName");
            assertNotNull("Property 'fasta.dataSourceName' missing from source "
                          + "'malaria-chromosome-fasta'", fastaTitle);
            assertEquals("'fasta.dataSourceName' value wrong", "PlasmoDB", fastaTitle.getValue());
            assertNull("'fasta.dataSourceName' location set", fastaTitle.getLocation());
            
            Source gff = sourceMap.get("malaria-gff");
            assertNotNull("Source 'malaria-gff' missing", gff);
            assertEquals("'malaria-gff' type wrong", "malaria-gff", gff.getType());
            assertEquals("'malaria-gff' dump wrong", Boolean.FALSE, gff.isDump());
            
            
            assertEquals("Wrong number of post processors",
                         5, project.getPostProcessing().getPostProcess().size());
            
            Map<String, PostProcess> postProcessMap = new HashMap<String, PostProcess>();
            for (PostProcess pp : project.getPostProcessing().getPostProcess()) {
                postProcessMap.put(pp.getName(), pp);
            }
            
            PostProcess transfer = postProcessMap.get("transfer-sequences");
            assertNotNull("Post processor 'transfer-sequences' missing", transfer);
            assertEquals("'transfer-sequences' dump flag wrong", Boolean.TRUE, transfer.isDump());
            
            PostProcess doSources = postProcessMap.get("do-sources");
            assertNotNull("Post processor 'do-sources' missing", doSources);
            assertEquals("'do-sources' dump flag wrong", Boolean.FALSE, doSources.isDump());
            
        } catch (AssertionFailedError e) {
            AssertionFailedError addition =
                new AssertionFailedError("Failure with file " + sourceFile.getAbsolutePath()
                                         + " :\n" + e.getMessage());
            addition.initCause(e.getCause());
            addition.setStackTrace(e.getStackTrace());
            throw addition;
        }
    }
    
    /**
     * Test writing and reloading a project file and ensuring the result is the same.
     * <p>Will fail if {@link #testLoadProjectFile1()} fails.</p>
     * 
     * @throws Exception if there is a failure.
     */
    @Test
    public void testWriteProjectFile() throws Exception {
        File projectFile = new File("src/test/schema/project.xml");
        
        Project project = parser.loadProjectFile(projectFile);
        
        File tempFile = File.createTempFile("cpTest", ".xml");
        tempFile.deleteOnExit();
        
        Writer out = new FileWriter(tempFile);
        parser.writeProjectFile(project, out);
        out.close();
        
        Project reloaded = parser.loadProjectFile(tempFile);
        
        projectEquality(project, reloaded);
    }
    
    /**
     * Check that the two models share the same names and packages
     * before descending to check the genomic core classes.
     * 
     * @param m1 Model one.
     * @param m2 Model two.
     * 
     * @see #genomicCoreEquality(List, List)
     */
    private void modelEquality(Model m1, Model m2) {
        
        assertEquals("Project name wrong", m1.getName(), m2.getName());
        assertEquals("Package wrong", m1.getPackage(), m2.getPackage());
        
        genomicCoreEquality(m1.getClazz(), m2.getClazz());
    }
    
    /**
     * Check that the two genomic additions are the same. Simply descends
     * to check the genomic core classes.
     * 
     * @param c1 Classes one.
     * @param c2 Classes two.
     * 
     * @see #genomicCoreEquality(List, List)
     */
    private void genomicEquality(Classes c1, Classes c2) {
        genomicCoreEquality(c1.getClazz(), c2.getClazz());
    }
    
    /**
     * Tests that the two lists of classes contain the same classes,
     * and that all of those classes are the same as their counterparts in the
     * other list.
     * 
     * @param c1 The first list of Classes.
     * @param c2 The second list of Classes.
     */
    private void genomicCoreEquality(List<Class> c1, List<Class> c2) {
        
        if (c1 == null) {
            assertNull("First classes null, second classes not null", c2);
            return;
        }
        if (c2 == null) {
            assertNull("First classes not null, second classes null", c1);
            return;
        }
        
        assertEquals("Wrong number of classes", c1.size(), c2.size());
        
        Map<String, Class> c1Classes = new HashMap<String, Class>();
        for (Class c : c1) {
            c1Classes.put(c.getName(), c);
        }
        Map<String, Class> c2Classes = new HashMap<String, Class>();
        for (Class c : c2) {
            c2Classes.put(c.getName(), c);
        }

        assertTrue("Class names don't match",
                CollectionUtils.isEqualCollection(c1Classes.keySet(), c2Classes.keySet()));
        
        Map<String, Attribute> c1Attributes = new HashMap<String, Attribute>();
        Map<String, Attribute> c2Attributes = new HashMap<String, Attribute>();
        Map<String, ClassReference> c1Refs = new HashMap<String, ClassReference>();
        Map<String, ClassReference> c2Refs = new HashMap<String, ClassReference>();
        
        for (String cname : c1Classes.keySet()) {
            Class c1c = c1Classes.get(cname);
            Class c2c = c2Classes.get(cname);
            
            assertEquals("Superclass of class " + cname + " wrong",
                         c1c.getExtends(), c2c.getExtends());
            assertEquals("Interface flag of " + cname + " wrong",
                         c1c.isIsInterface(), c2c.isIsInterface());
            
            
            assertEquals("Wrong number of attributes on class " + cname,
                    c1c.getAttribute().size(), c2c.getAttribute().size());
            
            c1Attributes.clear();
            for (Attribute a : c1c.getAttribute()) {
                c1Attributes.put(a.getName(), a);
            }
            c2Attributes.clear();
            for (Attribute a : c2c.getAttribute()) {
                c2Attributes.put(a.getName(), a);
            }
            
            assertTrue("Attribute names on class " + cname + " don't match",
                    CollectionUtils.isEqualCollection(c1Attributes.keySet(),
                                                      c2Attributes.keySet()));
            
            for (String aname : c1Attributes.keySet()) {
                Attribute a1 = c1Attributes.get(aname);
                Attribute a2 = c2Attributes.get(aname);
                
                assertEquals("Type of attribute " + aname + " on class " + cname + " mismatches",
                             a1.getType(), a2.getType());
            }

        
            assertEquals("Wrong number of collections on class " + cname,
                    c1c.getCollection().size(), c2c.getCollection().size());
            
            c1Refs.clear();
            for (ClassReference r : c1c.getCollection()) {
                c1Refs.put(r.getName(), r);
            }
            c2Refs.clear();
            for (ClassReference r : c2c.getCollection()) {
                c2Refs.put(r.getName(), r);
            }
            
            assertTrue("Collection names on class " + cname + " don't match",
                    CollectionUtils.isEqualCollection(c1Refs.keySet(), c2Refs.keySet()));
            
            for (String rname : c1Refs.keySet()) {
                ClassReference r1 = c1Refs.get(rname);
                ClassReference r2 = c2Refs.get(rname);
                
                assertEquals("Referenced type of collection " + rname + " on class "
                             + cname + " mismatches",
                             r1.getReferencedType(), r2.getReferencedType());
                assertEquals("Reverse reference of collection " + rname + " on class "
                             + cname + " mismatches",
                             r1.getReverseReference(), r2.getReverseReference());
            }

            
            assertEquals("Wrong number of refer4ences on class " + cname,
                    c1c.getReference().size(), c2c.getReference().size());
            
            c1Refs.clear();
            for (ClassReference r : c1c.getReference()) {
                c1Refs.put(r.getName(), r);
            }
            c2Refs.clear();
            for (ClassReference r : c2c.getReference()) {
                c2Refs.put(r.getName(), r);
            }
            
            assertTrue("Reference names on class " + cname + " don't match",
                    CollectionUtils.isEqualCollection(c1Refs.keySet(), c2Refs.keySet()));
            
            for (String rname : c1Refs.keySet()) {
                ClassReference r1 = c1Refs.get(rname);
                ClassReference r2 = c2Refs.get(rname);
                
                assertEquals("Referenced type of reference " + rname + " on class "
                             + cname + " mismatches",
                             r1.getReferencedType(), r2.getReferencedType());
                assertEquals("Reverse reference of reference " + rname + " on class "
                             + cname + " mismatches",
                             r1.getReverseReference(), r2.getReverseReference());
            }
        }
    }

    /**
     * Tests that two projects are the same as each other.
     * 
     * @param p1 Project one.
     * @param p2 Project two.
     */
    private void projectEquality(Project p1, Project p2) {
        
        assertEquals("Project type wrong", p1.getType(), p2.getType());
        
        assertEquals("Wrong number of properties",
                p1.getProperty().size(),
                p2.getProperty().size());
        
        Map<String, Property> p1Properties = new HashMap<String, Property>();
        for (Property p : p1.getProperty()) {
            p1Properties.put(p.getName(), p);
        }
        Map<String, Property> p2Properties = new HashMap<String, Property>();
        for (Property p : p2.getProperty()) {
            p2Properties.put(p.getName(), p);
        }

        assertTrue("Property names don't match",
                CollectionUtils.isEqualCollection(p1Properties.keySet(), p2Properties.keySet()));
        for (String pname : p1Properties.keySet()) {
            Property p1p = p1Properties.get(pname);
            Property p2p = p2Properties.get(pname);
            
            assertEquals("Value of property " + pname + " wrong",
                         p1p.getValue(), p2p.getValue());
            assertEquals("Location of property " + pname + " wrong",
                         p1p.getLocation(), p2p.getLocation());
        }
        
        
        assertEquals("Wrong number of sources",
                p1.getSources().getSource().size(),
                p2.getSources().getSource().size());
        
        Map<String, Source> p1Sources = new HashMap<String, Source>();
        for (Source s : p1.getSources().getSource()) {
            p1Sources.put(s.getName(), s);
        }
        Map<String, Source> p2Sources = new HashMap<String, Source>();
        for (Source s : p2.getSources().getSource()) {
            p2Sources.put(s.getName(), s);
        }
        assertTrue("Source names don't match",
                CollectionUtils.isEqualCollection(p1Sources.keySet(), p2Sources.keySet()));
        
        for (String sname : p1Sources.keySet()) {
            Source p1s = p1Sources.get(sname);
            Source p2s = p2Sources.get(sname);
            
            assertEquals("Type of source " + sname + " wrong", p1s.getType(), p2s.getType());
            
            p1Properties.clear();
            for (Property p : p1.getProperty()) {
                p1Properties.put(p.getName(), p);
            }
            p2Properties.clear();
            for (Property p : p2.getProperty()) {
                p2Properties.put(p.getName(), p);
            }

            assertTrue("Property names don't match",
                    CollectionUtils.isEqualCollection(p1Properties.keySet(),
                                                      p2Properties.keySet()));
            for (String pname : p1Properties.keySet()) {
                Property p1p = p1Properties.get(pname);
                Property p2p = p2Properties.get(pname);
                
                assertEquals("Value of source " + sname + " property " + pname + " wrong",
                        p1p.getValue(), p2p.getValue());
                assertEquals("Location of source " + sname + " property " + pname + " wrong",
                        p1p.getLocation(), p2p.getLocation());
            }
        }
        
        assertEquals("Wrong number of post processors",
                p1.getPostProcessing().getPostProcess().size(),
                p2.getPostProcessing().getPostProcess().size());
        
        Map<String, PostProcess> p1Post = new HashMap<String, PostProcess>();
        for (PostProcess pp : p1.getPostProcessing().getPostProcess()) {
            p1Post.put(pp.getName(), pp);
        }
        Map<String, PostProcess> p2Post = new HashMap<String, PostProcess>();
        for (PostProcess pp : p2.getPostProcessing().getPostProcess()) {
            p2Post.put(pp.getName(), pp);
        }
        
        assertTrue("Post-processor names don't match",
                CollectionUtils.isEqualCollection(p1Post.keySet(), p2Post.keySet()));
        
        for (String ppname : p1Post.keySet()) {
            PostProcess p1pp = p1Post.get(ppname);
            PostProcess p2pp = p2Post.get(ppname);
            
            assertEquals("Post-process " + ppname + " dump is wrong", p1pp.isDump(), p2pp.isDump());
        }
    }
}
