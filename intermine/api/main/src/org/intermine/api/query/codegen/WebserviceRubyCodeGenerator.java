package org.intermine.api.query.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.template.SwitchOffAbility;
import org.intermine.api.template.TemplateQuery;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

public class WebserviceRubyCodeGenerator implements WebserviceCodeGenerator 
{

    protected static final String INVALID_QUERY           = "Invalid query: ";
    protected static final String NULL_QUERY              = "Invalid query. Query can not be null.";

    protected static final String INDENT                  = "    ";
    protected static final String SPACE                   = " ";
    protected static final String LEFT_BRACE              = "{";
    protected static final String RIGHT_BRACE             = "}";
    protected static final String ENDL                    = System.getProperty("line.separator");

    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
                                                              + "constraint, which is currently not supported."; 

    
    private void appendBoilerPlate(StringBuffer sb, WebserviceCodeGenInfo info) {
        
        sb.append("#!/usr/bin/env ruby" + ENDL + ENDL);
        sb.append("# This is an automatically generated script to run your query" + ENDL);
        sb.append("# to use it you will require the intermine ruby client." + ENDL);
        sb.append("# To install the client, run the following command from a terminal:" + ENDL);
        sb.append("#" + ENDL);
        sb.append("#     sudo gem install intermine" + ENDL);
        sb.append("#" + ENDL);
        sb.append("# For further documentation you can visit:" + ENDL);
        sb.append("#          http://intermine.org/docs/ruby-docs/" + ENDL);
        sb.append("#     and: http://intermine.org/docs/ruby-bio-docs/" + ENDL);
        sb.append("#" + ENDL);
        sb.append("# The following two lines will be needed in every script:" + ENDL);
        sb.append("require \"rubygems\"" + ENDL);
        sb.append("require \"intermine/service\"" + ENDL);
        sb.append("service = Service.new(\"" + info.getServiceBaseURL() + "\"");
        if (!info.isPublic()) {
            sb.append(", \"YOUR-API-KEY\"");
        }
        sb.append(")" +  ENDL + ENDL);
    }
    
    @Override
    public String generate(WebserviceCodeGenInfo wsCodeGeninfo) {

        PathQuery query = wsCodeGeninfo.getQuery();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        StringBuffer sb = new StringBuffer();
        appendBoilerPlate(sb, wsCodeGeninfo);

        try {
            if (query.getClass().toString().endsWith("TemplateQuery")) {
                appendTemplate(sb, (TemplateQuery) query);
            } else {
                appendPathQuery(sb, query);
            }
        } catch (InvalidQueryException e) {
            return INVALID_QUERY + e.getMessage();
        }
        
        return sb.toString();
    }
    
    private class PresentedList<T> {

        StringBuffer presented = new StringBuffer();

        public PresentedList(Collection<T> things) {
            for (Iterator<T> i = things.iterator(); i.hasNext();) {
                addElement(i.next());
            }
        }

        public PresentedList() {
        }

        public void addElement(T elem) {
            if (presented.length() > 0) {
                presented.append(", ");
            }
            presented.append("\"" + elem.toString() + "\"");
        }

        public String toString() {
            return "[" + presented.toString() + "]";
        }
    }
    
    private Collection<String> deheadify(Collection<String> withHeads) {
        List<String> deheadeds = new ArrayList<String>();
        for (String x: withHeads) {
            deheadeds.add(decapitate(x));
        }
        return deheadeds;
    }
    
    private String decapitate(String x) {
        return x.substring(x.indexOf('.') + 1);
    }
    
    private String dblQuote(String x) {
        return "\"" + x + "\"";
    }

    private class RubyWhereClause {

        PathConstraint pc;
        String wc;
        
        public RubyWhereClause(PathConstraint pc) throws InvalidQueryException {
            this.pc = pc;
            parse();
        }
        
        public RubyWhereClause(String path, String type) throws InvalidQueryException {
            this.pc = new PathConstraintSubclass(path, type);
            parse();
        }
        
        private void parse() throws InvalidQueryException {
            String className = TypeUtil.unqualifiedName(pc.getClass().toString());
            String path = pc.getPath();
            ConstraintOp op = pc.getOp();
            
            String subk = (op == null) ? null : op.toString().toLowerCase().replace(' ', '_');
            String subv = null;
            String rhs = null;
            
            if ("PathConstraintSubclass".equals(className)) {
                // can not test from webapp
                subv = dblQuote(((PathConstraintSubclass) pc).getType());
                subk = "sub_class";
            } else if ("PathConstraintLoop".equals(className)) {
                if (op.equals(ConstraintOp.EQUALS)) {
                    subk = "is";
                } else {
                    subk = "is_not";
                }
                subv = dblQuote(((PathConstraintLoop) pc).getLoopPath());
            } else if ("PathConstraintMultiValue".equals(className)) {
                subv = new PresentedList(((PathConstraintMultiValue) pc).getValues()).toString();
            } else if ("PathConstraintBag".equals(className)) {
                subv = dblQuote(((PathConstraintBag) pc).getBag());
            } else if ("PathConstraintLookup".equals(className)) {
                String extraValue = ((PathConstraintLookup) pc).getExtraValue();
                if (StringUtils.isBlank(extraValue)) {
                    subv = dblQuote(((PathConstraintLookup) pc).getValue());
                } else {
                    subv = dblQuote(((PathConstraintLookup) pc).getValue() + "\", => :with => " + extraValue);
                }
            } else if ("PathConstraintAttribute".equals(className)) {
                String val = dblQuote(((PathConstraintAttribute) pc).getValue()); 
                if (op.equals(ConstraintOp.EQUALS)) {
                    rhs = val;
                } else {
                    subv = val;
                }
            } else if ("PathConstraintNull".equals(className)) {
                if (op.equals(ConstraintOp.IS_NULL))  {
                    rhs = "nil";
                } else if (op.equals(ConstraintOp.IS_NOT_NULL)) {
                    subk = "!=";
                    subv = "nil";
                } else {
                    throw new InvalidQueryException("Unknown null-constraint op (" + op + ")");
                }
            } else {
                throw new InvalidQueryException("Unknown constraint class (" + className +")");
            }
            if (rhs == null) {
                rhs = "{" 
                      + ((subk.startsWith("!") || subk.startsWith("=")) ? dblQuote(subk) : ":" + subk) 
                      + " => " 
                      + subv 
                      + "}";
            }
            wc = "where(" + dblQuote(path) + " => " + rhs + ")";
        }

        public String toString() {
            return wc;
        }

    }

    private void appendPathQuery(StringBuffer sb, PathQuery query)  throws InvalidQueryException {
        if (query.getDescription() != null && !"".equals(query.getDescription())) {
            sb.append("# query description - " + query.getDescription() + ENDL + ENDL);
        }

        sb.append("# Get a new query from the service you will be querying:"  + ENDL);
        try {
            sb.append("service.new_query(\"" + query.getRootClass() + "\")." + ENDL );
        } catch (PathException e1) {
            throw new InvalidQueryException(e1.getMessage());
        }


        // Put on subclasses first.
        try {
            for (Entry<String, String> entry: query.getSubclasses().entrySet()) {
                sb.append(INDENT + new RubyWhereClause(entry.getKey(), entry.getValue()) + "." + ENDL);
            }
        } catch (PathException e) {
            throw new InvalidQueryException(e.getMessage());
        }

        if (query.getView() == null || query.getView().isEmpty()) {
            throw new InvalidQueryException("No fields selected for output (view is empty)");
        }
        sb.append(INDENT + "select(");
        sb.append(new PresentedList<String>(deheadify(query.getView())));
        sb.append(")." + ENDL);
        
        // Add constraints
        if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
            // Add comments for constraints
            sb.append(INDENT + "# You can edit the constraint values below" + ENDL);

            int coded_cons = 0;
            for (PathConstraint pc : query.getConstraints().keySet()) {
                if (query.getConstraints().get(pc) == null) {
                    continue;
                }
                coded_cons++;
                sb.append(INDENT  + new RubyWhereClause(pc) + "." + ENDL);
            }

            // Add constraintLogic
            String logic = query.getConstraintLogic();
            if (   (coded_cons > 0) 
                && (logic != null)
                && (!"".equals(logic)) 
                && (logic.indexOf("or") != -1)) {
                sb.append(INDENT + "set_logic(\"" + logic + "\")." + ENDL);
            }
        }

        // Add orderBy
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) { // no sort order
            if ( // The default
                query.getOrderBy().size() == 1
                && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0))
                && query.getOrderBy().get(0).getDirection() == OrderDirection.ASC) {
                // The default
            
                for (OrderElement oe : query.getOrderBy()) {
                    sb.append(INDENT + "order_by(");
                    sb.append("\"" + decapitate(oe.getOrderPath()) + "\", \"" + oe.getDirection() + "\"");
                    sb.append(")." + ENDL);
                }
            }
        }

        
        if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
            for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                sb.append(INDENT + "join(\"" + entry.getKey() + "\", \"" + entry.getValue() + "\")." + ENDL);
            }
        }
        sb.append(INDENT + "limit(10)." + ENDL);
        sb.append(INDENT + "each_row { |r| puts r}" + ENDL);
    }

    private class TemplateComment {
        
        private String code;
        private String path;
        private String description;

        public TemplateComment(String code, String path, String description) {
            this.code = code;
            this.path = path;
            this.description = description;
        }

        @Override
        public String toString() {
            return "#" + INDENT + code + INDENT + path + (StringUtils.isBlank(description) ? "" : INDENT + description);
        }
    }
    
    private class RubyTemplateConstraint {
        private String str;

        public RubyTemplateConstraint(String code, PathConstraint pc) throws InvalidQueryException {
            str = parse(code, pc);
        }
        
        private String parse(String code, PathConstraint pc) throws InvalidQueryException {
         // We can't handle these in templates.
            String className = TypeUtil.unqualifiedName(pc.getClass().toString());
            if ("PathConstraintBag".equals(className)) {
                throw new InvalidQueryException("The webservice API cannot handle templates with list constraints " +
                		"- convert this template to a query instead");
            }
            if ("PathConstraintLoop".equals(className)) {
                throw new InvalidQueryException("The webservice API cannot handle template with loop constraints " + 
                        "- convert this template to a query instead");
            }
            
            String key1 = pc.getOp().toString().toLowerCase().replace(" ", "_");
            String val1 = null;
            String key2 = null;
            String val2 = null;
            
            if ("PathConstraintAttribute".equals(className)) {
                val1 = dblQuote(((PathConstraintAttribute) pc).getValue());
            } else if ("PathConstraintMultiValue".equals(className)) {
                val1 = new PresentedList(((PathConstraintMultiValue) pc).getValues()).toString();
            } else if ("PathConstraintNull".equals(className)) {
                if (ConstraintOp.IS_NULL.equals(pc.getOp())) {
                    
                } else if (ConstraintOp.IS_NOT_NULL.equals(pc.getOp())) {
                    
                } else {
                    throw new InvalidQueryException("Unknown null-constraint operator (" + pc.getOp() + ")");
                }
                val1 = "nil";
            } else if ("PathConstraintSubclass".equals(className)) {
                // Skip these
            } else if ("PathConstraintLoop".equals(className)) {
                // Skip these
            } else if ("PathConstraintLookup".equals(className)) {
                val1 = dblQuote(((PathConstraintLookup) pc).getValue());
                String extraValue = ((PathConstraintLookup) pc).getExtraValue();
                if (!StringUtils.isBlank(extraValue)) {
                    key2 = "extra";
                    val2 = dblQuote(extraValue);
                }
            } else {
                throw new InvalidQueryException("Unknown constraint class (" + className + ")");
            }
            
            return dblQuote(code) + " => {" + dblQuote(key1) + " => " + val1  
                    + ((key2 == null) ? "" : (", " + key2 + " => " + val2)) 
                    + "}";
        }
        
        @Override
        public String toString() {
            return str;
        }
    }
    
    private void appendTemplate(StringBuffer sb, TemplateQuery query) throws InvalidQueryException {
        String templateName = query.getName();
        String description = query.getDescription();
        Map<PathConstraint, String> allConstraints = query.getConstraints();
        List<PathConstraint> editableConstraints = query.getEditableConstraints();
        List<TemplateComment> templateComments = new ArrayList<TemplateComment>();
        List<RubyTemplateConstraint> templateConstraints = new ArrayList<RubyTemplateConstraint>();

        for (PathConstraint pc: editableConstraints) {
            if (query.getSwitchOffAbility(pc).equals(SwitchOffAbility.OFF)) {
                continue;
            }
            String code = allConstraints.get(pc);
            String path = pc.getPath();
            String constraintDes = query.getConstraintDescription(pc);

            templateComments.add(new TemplateComment(code, path, constraintDes));
            templateConstraints.add(new RubyTemplateConstraint(code, pc));
        }

        if (!StringUtils.isBlank(description)) {
            sb.append("# " + description + ENDL);
        }
        
        for (TemplateComment tc : templateComments) {
            sb.append(tc.toString() + ENDL);
        }
        sb.append("params = {" + ENDL);
        for (Iterator<RubyTemplateConstraint> i = templateConstraints.iterator(); i.hasNext();) {
            sb.append(INDENT + i.next());
            if (i.hasNext()) 
                sb.append(",");
            sb.append(ENDL);
        }
        sb.append("}" + ENDL);
        sb.append("service.template('" + templateName + "').limit(10).each_row(params) { |r| puts r }" + ENDL);
    }

}
