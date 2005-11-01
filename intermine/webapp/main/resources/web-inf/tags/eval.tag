<%@ tag body-content="empty" %>
<%@ attribute name="evalExpression" required="true" %>
<%@ attribute name="evalVariable" required="true" %>

<%-- evaluate the contents of the evalExpression attribute in the request scope
     and put the value in the evalVariable attribute in the request scope --%>

<%
   String expr = (String) jspContext.getAttribute("evalExpression");
   if (expr.indexOf("${") == -1 && expr.indexOf("}") == -1) {
     expr = "${" + expr + "}";
   }

   String evalVariable = (String) jspContext.getAttribute("evalVariable");

   try {
      Object ex = org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager.evaluate(null,
                           expr, Object.class, null, (PageContext) jspContext);
      request.setAttribute(evalVariable, ex);
   } catch (Exception e) {
      application.log("eval.tag: no value found for \"" + expr + "\": " + e.getClass()); 
      request.setAttribute(evalVariable, "");
   }
%>
