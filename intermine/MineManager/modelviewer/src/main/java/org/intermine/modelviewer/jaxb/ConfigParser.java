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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Class to wrap the raw JAXB calls to read and write projects and models.
 * <p>Refer to {@linkplain org.intermine.modelviewer.jaxb the package level documentation}
 * for further notes.</p>
 */
public class ConfigParser
{
    /**
     * Internal enumeration for which type of object is currently being handled.
     */
    private enum Context { CORE, GENOMIC, PROJECT }

    /**
     * The base of the URL for the schema locations for the files.
     */
    private static final String SCHEMA_BASE = "http://www.flymine.org/download/schemas/";
    
    /**
     * The full URL of the common genomic core schema (common parts of the core and
     * additions).
     */
    @SuppressWarnings("unused")
    private static final String GENOMIC_CORE_URL = SCHEMA_BASE + "genomic-core.xsd";
    
    /**
     * The full URL of the core schema.
     */
    private static final String CORE_URL = SCHEMA_BASE + "core.xsd";
    
    /**
     * The full URL of the additions schema.
     */
    private static final String GENOMIC_URL = SCHEMA_BASE + "genomic.xsd";
    
    /**
     * The full URL of the project schema.
     */
    private static final String PROJECT_URL = SCHEMA_BASE + "project.xsd";

    /**
     * Namespace reference for the common genomic core schema.
     */
    static final String GENOMIC_CORE_NAMESPACE = "http://flymine.org/genomic-core/1.0";

    /**
     * Namespace reference for the core schema.
     */
    static final String CORE_NAMESPACE = "http://flymine.org/core/1.0";

    /**
     * Namespace reference for the additions schema.
     */
    static final String GENOMIC_NAMESPACE = "http://flymine.org/genomic/1.0";

    /**
     * Namespace reference for the project schema.
     */
    static final String PROJECT_NAMESPACE = "http://flymine.org/project/1.0";

    
    /**
     * Name of the property for setting the JAXB namespace mapper for marshalling
     * XML. This is a Sun implementation specific property and may not work if other
     * versions of JAXB are used.
     * 
     * @see KnownNamespacePrefixMatcher
     */
    private static final String PREFIX_MAPPER_PROPERTY = "com.sun.xml.bind.namespacePrefixMapper";
    
    /**
     * Logger.
     */
    private Log logger = LogFactory.getLog(getClass());
    
    /**
     * SAX parser factory.
     */
    private SAXParserFactory parserFactory;

    /**
     * A map of Context values to namespace declarations.
     */
    private Map<Context, String> namespaces = new HashMap<Context, String>();
    
    /**
     * A map of Context values to schema locations..
     */
    private Map<Context, String> xsdUrls = new HashMap<Context, String>();
    
    /**
     * A map of Context values to JAXB contexts.
     */
    private Map<Context, JAXBContext> jaxbContexts = new HashMap<Context, JAXBContext>();
    
    /**
     * A map of Context values to XML validation schema objects.
     */
    private Map<Context, Schema> schemas = new HashMap<Context, Schema>();

    /**
     * Initialise this parser, setting up the JAXB contexts, validation schemas,
     * XML namespaces and relative schema locations.
     * 
     * @throws JAXBException if there is a problem initialising the JAXB system.
     * @throws SAXException if there is a problem initialising the SAX system.
     */
    public ConfigParser() throws JAXBException, SAXException {

        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        parserFactory.setValidating(true);

        // Get local versions of the schemas that are included in the JAR.
        
        URL genomicCoreUrl = getClass().getResource("/xsd/genomic-core.xsd");
        if (genomicCoreUrl == null) {
            throw new SAXException("Cannot locate schema /xsd/genomic-core.xsd on class path.");
        }
        URL coreUrl = getClass().getResource("/xsd/core.xsd");
        if (coreUrl == null) {
            throw new SAXException("Cannot locate schema /xsd/core.xsd on class path.");
        }
        URL genomicUrl = getClass().getResource("/xsd/genomic.xsd");
        if (genomicUrl == null) {
            throw new SAXException("Cannot locate schema /xsd/genomic.xsd on class path.");
        }
        URL projectUrl = getClass().getResource("/xsd/project.xsd");
        if (projectUrl == null) {
            throw new SAXException("Cannot locate schema /xsd/project.xsd on class path.");
        }
        
        // Set up the XML validation (javax.xml.validation) schemas.
        
        SchemaFactory sfact = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemas.put(Context.CORE,
            sfact.newSchema(new Source[] {
                new SAXSource(new InputSource(genomicCoreUrl.toExternalForm())),
                new SAXSource(new InputSource(coreUrl.toExternalForm()))
        }));
        schemas.put(Context.GENOMIC,
            sfact.newSchema(new Source[] {
                    new SAXSource(new InputSource(genomicCoreUrl.toExternalForm())),
                    new SAXSource(new InputSource(genomicUrl.toExternalForm()))
        }));
        schemas.put(Context.PROJECT,
            sfact.newSchema(new SAXSource(new InputSource(projectUrl.toExternalForm()))));

        namespaces.put(Context.CORE, CORE_NAMESPACE);
        namespaces.put(Context.GENOMIC, GENOMIC_NAMESPACE);
        namespaces.put(Context.PROJECT, PROJECT_NAMESPACE);

        xsdUrls.put(Context.CORE, CORE_URL);
        xsdUrls.put(Context.GENOMIC, GENOMIC_URL);
        xsdUrls.put(Context.PROJECT, PROJECT_URL);

        // Fetch and store the JAXB contexts for each type of XML document.
        
        jaxbContexts.put(Context.GENOMIC,
                JAXBContext.newInstance("org.intermine.modelviewer.genomic"));
        jaxbContexts.put(Context.CORE, jaxbContexts.get(Context.GENOMIC));
        //jaxbContexts.put(Context.CORE,
        //        JAXBContext.newInstance("org.intermine.modelviewer.core"));
        jaxbContexts.put(Context.PROJECT,
                JAXBContext.newInstance("org.intermine.modelviewer.project"));
    }

    /**
     * Load an object model from a core XML file.
     * 
     * @param coreFile The XML file to load.
     * 
     * @return The Model object forming the basis of the final model.
     * 
     * @throws JAXBException if there is a problem when unmarshalling with JAXB.
     * @throws SAXException if there is a problem parsing with SAX.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     */
    public org.intermine.modelviewer.genomic.Model loadCoreFile(File coreFile)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        return (org.intermine.modelviewer.genomic.Model)
                onePassUnmarshall(Context.CORE, coreFile);
    }

    /**
     * Load an object model from a genomic additions XML file.
     * 
     * @param genomicFile The XML file to load.
     * 
     * @return The Classes object containing the additions.
     * 
     * @throws JAXBException if there is a problem when unmarshalling with JAXB.
     * @throws SAXException if there is a problem parsing with SAX.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     */
    public org.intermine.modelviewer.genomic.Classes loadGenomicFile(File genomicFile)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        return (org.intermine.modelviewer.genomic.Classes)
                onePassUnmarshall(Context.GENOMIC, genomicFile);
    }

    /**
     * Load a project from a project XML file.
     * 
     * @param projectFile The XML file to load.
     * 
     * @return The Project object.
     * 
     * @throws JAXBException if there is a problem when unmarshalling with JAXB.
     * @throws SAXException if there is a problem parsing with SAX.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     */
    public org.intermine.modelviewer.project.Project loadProjectFile(File projectFile)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        return (org.intermine.modelviewer.project.Project)
                onePassUnmarshall(Context.PROJECT, projectFile);
    }

    /**
     * Provide an XML input source for the given file that filters out the given
     * name space if necessary.
     * <p>This method was part of a solution for unmarshalling with JAXB that
     * never quite worked, but is kept for reference.</p>
     * 
     * @param file The file to load.
     * @param namespace The name space to filter out.
     * 
     * @return The XML Source object to be used for reading.
     * 
     * @throws SAXException if there is a problem with the SAX system.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     * 
     * @see NamespaceFilter
     * @see #twoPassUnmarshall
     */
    private Source getSource(File file, String namespace)
    throws SAXException, ParserConfigurationException, IOException {
        XMLReader reader = parserFactory.newSAXParser().getXMLReader();
        if (namespace != null) {
            reader = new NamespaceFilter(namespace, reader);
        }
        return new SAXSource(reader, new InputSource(new FileReader(file)));
    }

    /**
     * Read the given file known to be of the given context.
     * 
     * @param context The context of the file.
     * @param file The file to read.
     * 
     * @return The Object created as a result of reading the file.
     * Its type will depend on <code>context</code>.
     * 
     * @throws SAXException if there is a problem parsing with SAX.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     */
    protected Object onePassUnmarshall(Context context, File file)
    throws SAXException, ParserConfigurationException, IOException {
        return saxParse(context, file);
    }

    /**
     * This method is a legacy of trying to persuade JAXB to read Intermine
     * files of all types, using all sorts of tricks to get it to work with
     * documents without schema declarations, documents with schema declarations,
     * documents without proper namespace declarations etc.
     * <p>It is no longer used, but remains here for the lessons learned.</p>
     * 
     * @param context The type of objects expected from the XML file.
     * @param file The file to load.
     * 
     * @return The Object created as a result of reading the file.
     * Its type will depend on <code>context</code>.
     * 
     * @throws JAXBException if there is a problem when unmarshalling.
     * @throws SAXException if there is a problem parsing with SAX.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     */
    protected Object twoPassUnmarshall(Context context, File file)
    throws JAXBException, SAXException, ParserConfigurationException, IOException {
        Unmarshaller unmarshall = jaxbContexts.get(context).createUnmarshaller();
        //unmarshall.setSchema(schemas.get(context));

        try {
            Source source = getSource(file, null);
            return unmarshall.unmarshal(source);
        } catch (UnmarshalException e) {
            if (e.getCause() == null) {
                logger.warn("Failed to unmarshall " + file + ": " + e.getMessage());
            } else {
                try {
                    throw e.getCause();
                } catch (SAXParseException e2) {
                    logger.warn("Failed to unmarshall " + file + ": " + e2.getMessage());
                    logger.debug("", e2);
                } catch (Throwable e2) {
                    logger.warn("Unexpected root exception while unmarshalling "
                            + file + ": " + e2.getClass().getName());
                    throw e;
                }
            }
            
            /*
             * This one would try to replace namespaces for JAXB. Unfortunately this
             * too is too strict.
             * 
            String namespace = namespaces.get(context);

            // Try filtering the XML by adding the appropriate namespace.
            try {
                Source source = getSource(file, namespace);
                return unmarshall.unmarshal(source);
            } catch (UnmarshalException e2) {
                // Throw the original exception - it's really the one that nags
                // about the namespace.
                throw e;
            }
            */
            
            return saxParse(context, file);
        }
    }
    
    /**
     * Unmarshalls an XML document using the very forgiving event driven parser provided
     * by SAX. No issues with namespaces and so forth here.
     *  
     * @param context The type of objects expected from the XML file.
     * @param file The file to load.
     * 
     * @return The Object created as a result of reading the file.
     * Its type will depend on <code>context</code>.
     * 
     * @throws SAXException if there is a problem parsing with SAX.
     * @throws ParserConfigurationException if there is a configuration problem with
     * the SAX system.
     * @throws IOException if there is a low level I/O problem.
     * 
     * @see CoreHandler
     * @see GenomicHandler
     * @see ProjectHandler
     * @see GenomicCoreHandler
     */
    private Object saxParse(Context context, File file)
    throws SAXException, ParserConfigurationException, IOException {
        //Schema schema = schemas.get(context);
        //schema.newValidator().validate(new StreamSource(file));
        
        BackupContentHandler handler;
        switch (context) {
            case CORE:
                handler = new CoreHandler();
                break;
                
            case GENOMIC:
                handler = new GenomicHandler();
                break;
            
            case PROJECT:
                handler = new ProjectHandler();
                break;

            default:
                throw new UnsupportedOperationException("Cannot load things of type " + context);
        }
        
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(new FileReader(file)));
        return handler.getResult();
    }

    /**
     * Marshall the given core model to the given writer.
     * 
     * @param model The core model.
     * @param out The writer to write to.
     * 
     * @throws JAXBException if JAXB marshalling fails.
     * @throws XMLStreamException if there is a problem with lower level XML streaming.
     */
    public void writeCoreFile(org.intermine.modelviewer.genomic.Model model, Writer out)
    throws JAXBException, XMLStreamException {
        writeFileJaxb(Context.CORE, model, out);
    }

    /**
     * Marshall the given genomic additions to the given writer.
     * 
     * @param classes The additions.
     * @param out The writer to write to.
     * 
     * @throws JAXBException if JAXB marshalling fails.
     * @throws XMLStreamException if there is a problem with lower level XML streaming.
     */
    public void writeGenomicFile(org.intermine.modelviewer.genomic.Classes classes, Writer out)
    throws JAXBException, XMLStreamException {
        writeFileJaxb(Context.GENOMIC, classes, out);
    }

    /**
     * Marshall the given Project to the given writer.
     * 
     * @param project The project.
     * @param out The writer to write to.
     * 
     * @throws JAXBException if JAXB marshalling fails.
     * @throws XMLStreamException if there is a problem with lower level XML streaming.
     */
    public void writeProjectFile(org.intermine.modelviewer.project.Project project, Writer out)
    throws JAXBException, XMLStreamException {
        writeFileJaxb(Context.PROJECT, project, out);
    }

    /**
     * Marshalls an object of the given context to the given writer.
     * <p>All sorts of niceties regarding schema locations, name spaces and so forth
     * are set up to embellish the output and make is as correct as possible.</p> 
     * 
     * @param context The type of object being written.
     * @param object The object to marshall.
     * @param out The writer to marshall to.
     * 
     * @throws JAXBException if JAXB marshalling fails.
     * @throws XMLStreamException if there is a problem with lower level XML streaming.
     * 
     * @see KnownNamespacePrefixMatcher
     */
    protected void writeFileJaxb(Context context, Object object, Writer out)
    throws JAXBException, XMLStreamException {

        String namespace = namespaces.get(context);
        
        StringBuilder schemaLocations = new StringBuilder();
        /*
        switch (context) {
            case CORE:
            case GENOMIC:
                schemaLocations.append(GENOMIC_CORE_NAMESPACE).append(' ');
                schemaLocations.append(GENOMIC_CORE_URL).append(' ');
                break;
        }
        */
        schemaLocations.append(namespace).append(' ').append(xsdUrls.get(context));

        /*
        XMLStreamWriter xmlStreamWriter =
            XMLOutputFactory.newInstance().createXMLStreamWriter(out);
        xmlStreamWriter.setPrefix("gc", GENOMIC_CORE_NAMESPACE);
        xmlStreamWriter.setPrefix("core", CORE_NAMESPACE);
        xmlStreamWriter.setPrefix("genomic", GENOMIC_NAMESPACE);
        xmlStreamWriter.setPrefix("project", PROJECT_NAMESPACE);
        //xmlStreamWriter.setDefaultNamespace(namespace);
        xmlStreamWriter = new IndentingXMLStreamWriter(xmlStreamWriter);
        */

        Marshaller marshall = jaxbContexts.get(context).createMarshaller();
        //marshall.setSchema(schemas.get(context));
        marshall.setProperty(PREFIX_MAPPER_PROPERTY, new KnownNamespacePrefixMatcher());

        marshall.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshall.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocations.toString());
        marshall.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        //marshall.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, xsdUrls.get(context));

        //marshall.marshal(object, xmlStreamWriter);
        marshall.marshal(object, out);
    }
}
