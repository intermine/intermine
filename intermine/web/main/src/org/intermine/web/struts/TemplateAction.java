package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.codegen.WebserviceCodeGenInfo;
import org.intermine.api.query.codegen.WebserviceCodeGenerator;
import org.intermine.api.query.codegen.WebserviceJavaScriptCodeGenerator;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.util.NameUtil;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.query.DisplayConstraintFactory;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.template.result.TemplateResultLinkGenerator;

/**
 * Action to handle submit from the template page. <code>setSavingQueries</code>
 * can be used to set whether or not queries run by this action are
 * automatically saved in the user's query history. This property is true by
 * default.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateAction extends InterMineAction
{
    /** Name of skipBuilder parameter **/
    public static final String SKIP_BUILDER_PARAMETER = "skipBuilder";

    /** path of TemplateAction action **/
    public static final String TEMPLATE_ACTION_PATH = "templateAction.do";

    /**
     * Build a query based on the template and the input from the user. There
     * are some request parameters that, if present, effect the behaviour of the
     * action. These are:
     *
     * <dl>
     * <dt>skipBuilder</dt>
     * <dd>If this attribute is specifed (with any value) then the action will
     * forward directly to the object details page if the results contain just
     * one object.</dd>
     * <dt>noSaveQuery</dt>
     * <dd>If this attribute is specifed (with any value) then the query is not
     * automatically saved in the user's query history.</dd>
     * </dl>
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception
     *                if the application business logic throws an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        TemplateForm tf = (TemplateForm) form;
        String templateName = tf.getName();
        boolean saveQuery = (request.getParameter("noSaveQuery") == null);
        boolean skipBuilder = (request.getParameter(SKIP_BUILDER_PARAMETER) != null);
        boolean editTemplate = (request.getParameter("editTemplate") != null);
        boolean editQuery = (request.getParameter("editQuery") != null);

        // Note this is a workaround and will fail if a user has a template with the same name as
        // a public template but has executed the public one.  TemplateManager always give
        // precedence to user templates when there is a naming clash.
        String scope = tf.getScope();
        if (StringUtils.isBlank(scope)) {
            scope = Scope.ALL;
        }

        SessionMethods.logTemplateQueryUse(session, scope, templateName);

        Profile profile = SessionMethods.getProfile(session);
        TemplateManager templateManager = im.getTemplateManager();

        TemplateQuery template = templateManager.getTemplate(profile, templateName, scope);
        //If I'm browsing from the history or from saved query the template is in the session
        //with the values edited by the user, from this template we retrieve the original name
        //that we use to call the TemplateManager
        if (template == null) {
            PathQuery query = SessionMethods.getQuery(session);
            if (query instanceof TemplateQuery) {
                TemplateQuery currentTemplate = (TemplateQuery) query;
                template = templateManager.getTemplate(profile, currentTemplate.getName(), scope);
            } else {
            //from the template click edit query and then back (in this case in the session
            //there is a pathquery)
                template = templateManager.getTemplate(profile,
                                           (String) session.getAttribute("templateName"), scope);
            }
        }
        if (template == null) {
            if (editTemplate) {
                recordMessage(new ActionMessage("errors.edittemplate.empty"),
                        request);
                return mapping.findForward("template");
            } else {
                throw new RuntimeException("Could not find a template called "
                    + session.getAttribute("templateName"));
            }
        }
        TemplateQuery populatedTemplate = TemplatePopulator.getPopulatedTemplate(
                template, templateFormToTemplateValues(tf, template));

        //displayconstraint list used to display  constraints edited by the user
        DisplayConstraintFactory factory =  new DisplayConstraintFactory(im, null);
        DisplayConstraint displayConstraint = null;
        List<DisplayConstraint> displayConstraintList = new ArrayList<DisplayConstraint>();
        for (PathConstraint pathConstraint : populatedTemplate.getEditableConstraints()) {
            displayConstraint = factory.get(pathConstraint, profile, populatedTemplate);
            displayConstraintList.add(displayConstraint);
        }
        session.setAttribute("dcl", displayConstraintList);

        String url = new URLGenerator(request).getPermanentBaseURL();

        if (!populatedTemplate.isValid()) {
            recordError(new ActionMessage("errors.template.badtemplate",
                    StringUtil.prettyList(populatedTemplate.verifyQuery())), request);
            return mapping.findForward("template");
        }


        if (!editQuery && !skipBuilder && !editTemplate && forwardToLinksPage(request)) {


            Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                    .getServletContext());

            WebserviceCodeGenInfo info = new WebserviceCodeGenInfo(
                    populatedTemplate,
                    url,
                    webProperties.getProperty("project.title"),
                    webProperties.getProperty("perl.wsModuleVer"),
                    WebserviceCodeGenAction.templateIsPublic(template, im, profile),
                    profile);
            WebserviceCodeGenerator codeGen = new WebserviceJavaScriptCodeGenerator();
            String code = codeGen.generate(info);
            session.setAttribute("realCode", code);
            String escapedCode = StringEscapeUtils.escapeHtml(code);
            session.setAttribute("jsCode", escapedCode);

            return mapping.findForward("serviceLink");
        }

        if (!editQuery && !skipBuilder && !editTemplate && wantsWebserviceURL(request)) {
            TemplateResultLinkGenerator gen = new TemplateResultLinkGenerator();
            String webserviceUrl = gen.getTabLink(url, populatedTemplate);

            response.setContentType("text/plain");

            PrintStream out;
            try {
                out = new PrintStream(response.getOutputStream());
                out.print(webserviceUrl);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        if (!editQuery && !skipBuilder && !editTemplate && exportTemplate(request)) {
            SessionMethods.loadQuery(populatedTemplate, request.getSession(), response);
            return mapping.findForward("export");
        }

        if (!editQuery && !skipBuilder && !editTemplate && codeGenTemplate(request)) {
            SessionMethods.loadQuery(populatedTemplate, request.getSession(), response);
            return new ForwardParameters(mapping.findForward("codeGen")).addParameter("method",
                request.getParameter("actionType")).addParameter("source", "templateQuery")
                .forward();
        }

        // We're editing the query: load as a PathQuery
        if (!skipBuilder && !editTemplate) {
            SessionMethods.loadQuery(new PathQuery(populatedTemplate), request.getSession(),
                                     response);
            session.setAttribute("templateName", populatedTemplate.getName());
            session.removeAttribute(Constants.NEW_TEMPLATE);
            session.removeAttribute(Constants.EDITING_TEMPLATE);
            form.reset(mapping, request);
            return mapping.findForward("query");
        } else if (editTemplate) {
            // We want to edit the template: Load the query as a TemplateQuery
            // Don't care about the form
            // Reload the initial template
            session.removeAttribute(Constants.NEW_TEMPLATE);
            session.setAttribute(Constants.EDITING_TEMPLATE, Boolean.TRUE);
            SessionMethods.loadQuery(template, request.getSession(), response);
            if (!template.isValid()) {
                recordError(new ActionMessage("errors.template.badtemplate",
                        StringUtil.prettyList(template.verifyQuery())),
                        request);
            }

            return mapping.findForward("query");
        }

        // Otherwise show the results: load the modified query from the template
        if (saveQuery) {
            SessionMethods.loadQuery(populatedTemplate, request.getSession(), response);
        }
        form.reset(mapping, request);

        //tracks the template execution
        im.getTrackerDelegate().trackTemplate(populatedTemplate.getName(), profile,
                                              session.getId());

        String trail = "";
        // only put query on the trail if we are saving the query
        // otherwise its a "super top secret" query, e.g. quick search
        // also, note we are not saving any previous trails. trail resets at
        // queries and bags
        if (saveQuery) {
            trail = "|query";
        } else {
            trail = "";
            // session.removeAttribute(Constants.QUERY);
        }

        String queryName = NameUtil.findNewQueryName(profile.getHistory().keySet());
        SessionMethods.saveQueryToHistory(
                session,
                queryName,
                populatedTemplate.getQueryToExecute());

        return new ForwardParameters(mapping.findForward("results"))
                .addParameter("trail", trail)
                .forward();
    }

    /**
     * Methods looks at request parameters if should forward to web service
     * links page.
     *
     * @param request the request
     * @return true if should be forwarded
     */
    private boolean forwardToLinksPage(HttpServletRequest request) {
        return "links".equalsIgnoreCase(request.getParameter("actionType"));
    }

    private boolean wantsWebserviceURL(HttpServletRequest request) {
        return "webserviceURL".equals(request.getParameter("actionType"));
    }

    private boolean exportTemplate(HttpServletRequest request) {
        return "exportTemplate".equals(request.getParameter("actionType"));
    }

    private boolean codeGenTemplate(HttpServletRequest request) {
        String codeGenTemplate = request.getParameter("actionType");
        if (codeGenTemplate != null
                && ("perl".equalsIgnoreCase(codeGenTemplate)
                     || "java".equalsIgnoreCase(codeGenTemplate)
                     || "python".equalsIgnoreCase(codeGenTemplate)
                     || "ruby".equalsIgnoreCase(codeGenTemplate)
                     || "javascript".equalsIgnoreCase(codeGenTemplate)
                    )) {
            return true;
        }
        return false;
    }

    /**
     * The method returns a map of TemplateValue for each editable constraint. The map is obtained
     * matching the values retrieved from the request through the TemplateForm with the editable
     * constraint defined in the current template
     * @param tf the actionform containing the value from the requst
     * @param template the current template
     * @return Map<String, List<TemplateValue>
     */
    protected Map<String, List<TemplateValue>> templateFormToTemplateValues(TemplateForm tf,
            TemplateQuery template) {
        Map<String, List<TemplateValue>> templateValues =
            new HashMap<String, List<TemplateValue>>();
        for (String node : template.getEditablePaths()) {
            List<TemplateValue> nodeValues = new ArrayList<TemplateValue>();
            templateValues.put(node, nodeValues);
            for (PathConstraint c : template.getEditableConstraints(node)) {
                String key = Integer.toString(template.getEditableConstraints().indexOf(c) + 1);
                TemplateValue value = null;

                SwitchOffAbility switchOffAbility = template.getSwitchOffAbility(c);
                if (tf.getSwitchOff(key) != null) {
                    switchOffAbility = parseSwitchOffAbility(tf.getSwitchOff(key));
                }

                if (tf.getUseBagConstraint(key)) {
                    ConstraintOp constraintOp = ConstraintOp
                            .getOpForIndex(Integer.valueOf(tf.getBagOp(key)));
                    String constraintValue = (String) tf.getBag(key);
                    value = new TemplateValue(c, constraintOp, constraintValue,
                            TemplateValue.ValueType.BAG_VALUE, switchOffAbility);
                } else {
                    if (tf.getSwitchOff(key) != null
                        && tf.getSwitchOff(key).equalsIgnoreCase(SwitchOffAbility.OFF.toString())) {
                        if (c instanceof PathConstraintMultiValue) {
                            String multiValueAttribute = constraintStringValue(c);
                            if (multiValueAttribute != null && !("".equals(multiValueAttribute))) {
                                List<String> multiValues = new ArrayList<String>();
                                multiValues.addAll(Arrays.asList(multiValueAttribute.split(",")));
                                value = new TemplateValue(c, c.getOp(),
                                        TemplateValue.ValueType.SIMPLE_VALUE, multiValues,
                                        switchOffAbility);
                            }
                        } else {
                            String constraintValue = constraintStringValue(c);
                            value = new TemplateValue(c, c.getOp(), constraintValue,
                                    TemplateValue.ValueType.SIMPLE_VALUE, switchOffAbility);
                        }
                    } else {
                        String op = (String) tf.getAttributeOps(key);
                        if (op == null) {
                            if (c instanceof PathConstraintLookup) {
                                value = new TemplateValue(c, ConstraintOp.LOOKUP,
                                        (String) tf.getAttributeValues(key),
                                        TemplateValue.ValueType.SIMPLE_VALUE,
                                        extraValueToString(tf.getExtraValues(key)),
                                        switchOffAbility);
                            } else if (tf.getNullConstraint(key) != null) {
                                if (ConstraintOp.IS_NULL.toString()
                                    .equals(tf.getNullConstraint(key))) {
                                    value = new TemplateValue(c, ConstraintOp.IS_NULL,
                                            ConstraintOp.IS_NULL.toString(),
                                        TemplateValue.ValueType.SIMPLE_VALUE, switchOffAbility);
                                } else {
                                    value = new TemplateValue(c, ConstraintOp.IS_NOT_NULL,
                                            ConstraintOp.IS_NOT_NULL.toString(),
                                            TemplateValue.ValueType.SIMPLE_VALUE, switchOffAbility);
                                }
                            } else {
                                continue;
                            }
                        } else {
                            ConstraintOp constraintOp = ConstraintOp
                                    .getOpForIndex(Integer.valueOf(op));
                            String constraintValue = "";
                            String multiValueAttribute = tf.getMultiValueAttribute(key);
                            if (multiValueAttribute != null && !("".equals(multiValueAttribute))) {
                                constraintValue = tf.getMultiValueAttribute(key);
                                List<String> multiValues = new ArrayList<String>();
                                multiValues.addAll(Arrays.asList(constraintValue.split(",")));
                                value = new TemplateValue(c, constraintOp,
                                        TemplateValue.ValueType.SIMPLE_VALUE, multiValues,
                                        switchOffAbility);
                            } else {
                                constraintValue = (String) tf.getAttributeValues(key);
                                String extraValue = (String) tf.getExtraValues(key);
                                value = new TemplateValue(c, constraintOp, constraintValue,
                                        TemplateValue.ValueType.SIMPLE_VALUE, extraValue,
                                        switchOffAbility);
                            }
                        }
                    }
                }
                nodeValues.add(value);
            }
        }
        return templateValues;
    }

    private String constraintStringValue(PathConstraint con) {
        if (con instanceof PathConstraintAttribute) {
            return ((PathConstraintAttribute) con).getValue();
        } else if (con instanceof PathConstraintBag) {
            return ((PathConstraintBag) con).getBag();
        } else if (con instanceof PathConstraintLookup) {
            return ((PathConstraintLookup) con).getValue();
        } else if (con instanceof PathConstraintSubclass) {
            return ((PathConstraintSubclass) con).getType();
        } else if (con instanceof PathConstraintLoop) {
            return ((PathConstraintLoop) con).getLoopPath();
        } else if (con instanceof PathConstraintNull) {
            return ((PathConstraintNull) con).getOp().toString();
        } else if (con instanceof PathConstraintMultiValue) {
            Collection<String> multiValuesCollection = ((PathConstraintMultiValue) con).getValues();
            String multiValuesAsString = "";
            if (multiValuesCollection != null) {
                for (String value : multiValuesCollection) {
                    multiValuesAsString += value + ",";
                }
            }
            return multiValuesAsString;
        }
        return null;
    }

    private String extraValueToString(Object extraValue) {
        if (extraValue == null) {
            return null;
        }
        return extraValue.toString();
    }

    private SwitchOffAbility parseSwitchOffAbility(String value) {
        for (SwitchOffAbility switchOffAbility : SwitchOffAbility.values()) {
            if (switchOffAbility.toString().equalsIgnoreCase(value)) {
                return switchOffAbility;
            }
        }
        throw new IllegalArgumentException("Invalid value specified for constraint"
                + " switchOffAbility '" + value + "', if this happens there is a bug. ");
    }
}
