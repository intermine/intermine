package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;

import psidev.psi.mi.jami.commons.MIDataSourceOptionFactory;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.commons.PsiJami;
import psidev.psi.mi.jami.datasource.InteractionStream;
import psidev.psi.mi.jami.factory.MIDataSourceFactory;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.Interaction;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.model.ModelledInteraction;


/**
 * Converter to parse complexes. May expand to handle others later.
 *
 * @author Julie Sullivan
 */
public class PsiComplexesConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "Complexes";
    private static final String DATA_SOURCE_NAME = "EBI IntAct";

    private static final Logger LOG = Logger.getLogger(PsiComplexesConverter.class);
    private static final String PROP_FILE = "psi-complexes_config.properties";
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, Object> experimentNames = new HashMap<String, Object>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, String> regions = new HashMap<String, String>();
    private String termId = null;
    private static final String INTERACTION_TYPE = "physical";
    private Map<String, String[]> config = new HashMap<String, String[]>();
    private Set<String> taxonIds = null;
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<MultiKey, Item> interactions = new HashMap<MultiKey, Item>();
    private static final String ALIAS_TYPE = "gene name";
    private static final String SPOKE_MODEL = "prey";   // don't store if all roles prey
    private static final String DEFAULT_IDENTIFIER = "symbol";
    private static final String DEFAULT_DATASOURCE = "";
    private static final String BINDING_SITE = "MI:0117";
    private static final Set<String> INTERESTING_COMMENTS = new HashSet<String>();


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PsiComplexesConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setIntactOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // initialise default factories for reading and writing MITAB/PSI-MI XML files
        PsiJami.initialiseAllFactories();

        // reading MITAB and PSI-MI XML files

        // the option factory for reading files and other datasources
        MIDataSourceOptionFactory optionfactory = MIDataSourceOptionFactory.getInstance();
        // the datasource factory for reading MITAB/PSI-MI XML files and other datasources
        MIDataSourceFactory dataSourceFactory = MIDataSourceFactory.getInstance();


        // get default options for a file. It will identify if the file is MITAB or PSI-MI XML file
        // and then it will load the appropriate options.
        // By default, the datasource will be streaming (only returns an iterator of interactions),
        // and returns a source of Interaction objects.
        // The default options can be overridden using the optionfactory or by manually adding
        // options listed in MitabDataSourceOptions or PsiXmlDataSourceOptions
        Map<String, Object> parsingOptions = optionfactory.getDefaultOptions(reader);

        InteractionStream interactionSource = null;
        try {
            // Get the stream of interactions knowing the default options for this file
            interactionSource = dataSourceFactory.
                    getInteractionSourceWith(parsingOptions);

            // writing MITAB and PSI-XML files

            // the option factory for reading files and other datasources
            MIWriterOptionFactory optionwriterFactory = MIWriterOptionFactory.getInstance();

            // parse the stream and write as we parse
            // the interactionSource can be null if the file is not recognized or the provided
            // options are not matching any existing/registered datasources
            if (interactionSource != null) {
                Iterator interactionIterator = interactionSource.getInteractionsIterator();

                while (interactionIterator.hasNext()) {
                    Interaction interaction = (Interaction) interactionIterator.next();

                    // most of the interactions will have experimental data attached to them
                    // so they will be of type InteractionEvidence
                    if (interaction instanceof InteractionEvidence) {
                        InteractionEvidence interactionEvidence = (InteractionEvidence) interaction;
                        // process the interaction evidence

                    // modelled interactions are equivalent to abstractInteractions in PSI-MI XML
                    // 3.0. They are returned when the interaction is not an
                    // experimental interaction but a 'modelled' one extracted from any
                    // experimental context
//                    else if (interaction instanceof ModelledInteraction) {
//                        ModelledInteraction modelledInteraction
                    // = (ModelledInteraction) interaction;
//                        // process the modelled interaction
                    } else if (interaction instanceof Complex) {
                        Complex complex = (Complex) interaction;

                        Item item = createItem("Complex");
                        item.setAttribute("name", complex.getRecommendedName());
                        item.setAttribute("systemicName", complex.getSystematicName());
                        item.setAttribute("properties", complex.getPhysicalProperties());
                        store(item);
                    }
                }

            }
        } finally {
            // always close the opened interaction stream
            if (interactionSource != null) {
                interactionSource.close();
            }
        }
    }
}
