package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManager.TagNameException;
import org.intermine.api.profile.TagManager.TagNamePermissionException;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.CorrectionCoefficient;
import org.intermine.web.logic.widget.EnrichmentWidget;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

/**
 * Web service that returns the result of an enrichment calculation over a list
 * of objects.
 *
 * {@link WidgetsRequestProcessor} for parameter description
 *
 * @author Alex Kalderimis
 * @author Xavier Watkins
 * @author Daniela Butano
 */
public class EnrichmentWidgetResultService extends WidgetService
{
    private class EnrichmentXMLFormatter extends XMLFormatter
    {

        @Override
        public String formatResult(List<String> resultRow) {
            // The EnrichmentXMLProcessor produces XML elements.
            return StringUtils.join(resultRow, "");
        }

    }

    private static final Logger LOG = Logger.getLogger(EnrichmentWidgetResultService.class);

    private final WidgetsRequestParser requestParser;

    /** @param im The InterMine state object. **/
    public EnrichmentWidgetResultService(InterMineAPI im) {
        super(im);
        requestParser = new WidgetsRequestParser();
        requestParser.parameterIsRequired(WidgetsRequestParser.MAXP);
        requestParser.parameterIsRequired(WidgetsRequestParser.ERROR_CORRECTION);
        requestParser.setDefaultValue(
                WidgetsRequestParser.ERROR_CORRECTION,
                getProperty("widgets.correction.default"));
        requestParser.setDefaultValue(
                WidgetsRequestParser.MAXP,
                getProperty("widgets.maxp.default"));
    }

    @Override
    protected boolean canServe(Format format) {
        if (format == Format.JSON
                || format == Format.XML
                || Format.FLAT_FILES.contains(format)) {
            return true;
        }
        return false;
    }

    private static final String BAD_POPULATION_MSG =
            "One or more of the %1$ss in this list are missing from your background population."
            + " The background population should include all %1$ss that were tested as part of"
            + " your experiment.";

    /**
     * Executes service specific logic.
     *
     * @throws Exception an error has occurred
     */
    @Override
    protected void execute() throws Exception {
        WidgetsServiceInput input = getInput();
        InterMineBag imBag = retrieveBag(input.getBagName());
        addOutputListInfo(imBag);
        String ids = input.getIds();
        String populationIds = input.getPopulationIds();
        addOutputIdsInfo(ids, populationIds);
        LOG.debug("Enriching with " + input);

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.getWidgetId());

        if (widgetConfig == null || !(widgetConfig instanceof EnrichmentWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \""
                                                + input.getWidgetId() + "\"");
        }
        addOutputConfig(widgetConfig);

        //filters
        String filterSelectedValue = input.getFilter();
        if (StringUtils.isBlank(filterSelectedValue)) {
            filterSelectedValue = getDefaultFilterValue(widgetConfig, imBag, ids);
        }
        addOutputFilter(widgetConfig, filterSelectedValue, imBag, ids);

        addOutputUserLogged();

        //reference population
        InterMineBag populationBag = getReferencePopulationBag(input);
        if (populationBag != null && !verifyPopulationContainsBag(imBag, populationBag)) {
            if (input.shouldSavePopulation()) {
                deleteReferencePopulationPreference(input);
            }
            addOutputAttribute("message", String.format(BAD_POPULATION_MSG, imBag.getType()));
            return;
        }

        //instantiate the widget
        EnrichmentWidget widget = null;
        try {
            widget = (EnrichmentWidget) widgetConfig.getWidget(
                    imBag, populationBag, im.getObjectStore(), input, ids, populationIds);
            if (filterSelectedValue != null) {
                widget.setFilter(filterSelectedValue);
            }
            widget.process();
        } catch (IllegalArgumentException e) {
            addOutputAttribute("message", e.getMessage());
            deleteReferencePopulationPreference(input);
            return;
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \""
                                               + input.getWidgetId() + "\"");
        }
        addOutputInfo("notAnalysed", Integer.toString(widget.getNotAnalysed()));
        addOutputPathQuery(widget, widgetConfig);
        addOutputExtraAttribute(input, widget);

        addOutputResult(widget);
    }



    @Override
    protected void addOutputConfig(WidgetConfig config) {
        super.addOutputConfig(config);
        addOutputAttribute("label", ((EnrichmentWidgetConfig) config).getLabel());
        addOutputAttribute("externalLink", ((EnrichmentWidgetConfig) config).getExternalLink());
    }

    private void addOutputPathQuery(EnrichmentWidget widget, WidgetConfig config) {
        // TODO: Make this a) not an effing string, and b) work equally well in XML.
        addOutputInfo("pathQuery", widget.getPathQuery().toJson());
        addOutputInfo("pathConstraint", widget.getPathConstraint());
        addOutputInfo("pathQueryForMatches", widget.getPathQueryForMatches().toJson());
    }

    private void addOutputUserLogged() {
        if (isProfileLoggedIn()) {
            addOutputAttribute("is_logged", "true");
        } else {
            addOutputAttribute("is_logged", "false");
        }
    }

    /*
     * Set in the output the gene_lenth_correction attribute.
     * The attribute is null if the gene length coefficient correction can't be applicable
     * true if the gene length coefficient correction is selected, false if not selected
     */
    private void addOutputExtraAttribute(WidgetsServiceInput input,
        EnrichmentWidget widget) throws Exception {
        WidgetResultProcessor processor = getProcessor();
        String extra = input.getExtraAttribute();
        CorrectionCoefficient cc = widget.getExtraCorrectionCoefficient();
        Map<String, Map<String, Object>> extraAttributes;
        if (cc != null) {
            extraAttributes = cc.getOutputInfo(extra);
            if (processor instanceof EnrichmentJSONProcessor) {
                String jsonExtraAttribute = ((EnrichmentJSONProcessor) processor)
                                         .formatExtraAttributes(extraAttributes);
                addOutputInfo("extraAttribute", jsonExtraAttribute);
            }
        } else {
            addOutputInfo("extraAttribute", null);
        }
    }

    /**
     * @return A WidgetResultProcessor of some type, capable of serializing the output.
     */
    protected WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return EnrichmentJSONProcessor.instance();
        } else if (formatIsXML()) {
            return EnrichmentXMLProcessor.instance();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new EnrichmentXMLFormatter(), separator);
    }

    private WidgetsServiceInput getInput() {
        return requestParser.getInput(request);
    }

    private boolean isProfileLoggedIn() {
        Profile profile = getPermission().getProfile();
        if (profile.isLoggedIn()) {
            return true;
        }
        return false;
    }

    private InterMineBag getReferencePopulationBag(WidgetsServiceInput input)
        throws TagNamePermissionException, TagNameException {
        String populationBagName = input.getPopulationBagName();
        if (populationBagName == null) {
            //get preferences
            populationBagName = getReferencePopulationPreference(input);
        }
        if ("".equals(populationBagName)) {
            //json formatter doesn't format empty string
            addOutputInfo(WidgetsRequestParser.POPULATION_BAG_NAME, null);
        } else {
            addOutputInfo(WidgetsRequestParser.POPULATION_BAG_NAME, populationBagName);
        }
        InterMineBag populationBag = null;
        populationBag = retrieveBag(populationBagName);
        saveReferencePopulationPreference(input);
        return populationBag;
    }

    private void saveReferencePopulationPreference(WidgetsServiceInput input)
        throws TagNamePermissionException, TagNameException {
        if (input.shouldSavePopulation()) {
            if (isProfileLoggedIn()) {
                TagManager tm = im.getTagManager();
                String tagName = TagNames.IM_WIDGET
                        + TagNames.SEPARATOR + input.getWidgetId()
                        + TagNames.SEPARATOR + input.getPopulationBagName();
                deleteReferencePopulationPreference(input);
                if (!"".equals(input.getPopulationBagName())) {
                    tm.addTag(tagName, input.getBagName(), TagTypes.BAG,
                              getPermission().getProfile());
                }
            }
        }
    }

    private void deleteReferencePopulationPreference(WidgetsServiceInput input) {
        if (isProfileLoggedIn()) {
            TagManager tm = im.getTagManager();
            List<Tag> currentTags = getReferencePopulationTags(input);
            for (Tag tag : currentTags) {
                tm.deleteTag(tag);
            }
        }
    }

    private List<Tag> getReferencePopulationTags(WidgetsServiceInput input) {
        List<Tag> populationTags = new ArrayList<Tag>();
        if (isProfileLoggedIn()) {
            Profile profile = getPermission().getProfile();
            TagManager tm = im.getTagManager();
            String prefixTagPopulation = TagNames.IM_WIDGET + TagNames.SEPARATOR
                                       + input.getWidgetId() + TagNames.SEPARATOR;
            @SuppressWarnings("deprecation")
            List<Tag> tags = tm.getTags(null, null,
                             TagTypes.BAG, profile.getUsername());
            for (Tag tag : tags) {
                if (tag.getObjectIdentifier().equals(input.getBagName())
                    && tag.getTagName().startsWith(prefixTagPopulation)) {
                    populationTags.add(tag);
                }
            }
        }
        return populationTags;
    }

    private String getReferencePopulationPreference(WidgetsServiceInput input) {
        if (isProfileLoggedIn()) {
            List<Tag> populationTags = getReferencePopulationTags(input);
            if (!populationTags.isEmpty()) {
                String prefixTagPopulation = TagNames.IM_WIDGET + TagNames.SEPARATOR
                                           + input.getWidgetId() + TagNames.SEPARATOR;
                String tagName = populationTags.get(0).getTagName();
                return tagName.replace(prefixTagPopulation, "");
            }
        }
        return "";
    }

    private boolean verifyPopulationContainsBag(InterMineBag bag, InterMineBag populationBag) {
        //verify the population Bag contains all elements of imBag
        List<Integer> populationBagContentdIds =
            new ArrayList<Integer>(populationBag.getContentsAsIds());
        List<Integer> bagContentdIds =
            new ArrayList<Integer>(bag.getContentsAsIds());
        if (populationBagContentdIds.containsAll(bagContentdIds)) {
            return true;
        }
        return false;
    }
}
