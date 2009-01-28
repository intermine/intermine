<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- mainLogic.jsp -->

<script language="javascript">
jQuery(document).ready(function(){
  jQuery('#constraintLogic').click(function() {
    jQuery('#constraintLogic').toggle();
    jQuery('#editConstraintLogic').toggle();
  });
  jQuery('#editconstraintlogic').click(function() {
    setConstraintLogic(jQuery('#expr').val());
  });
});
</script>

<html:xhtml/>

<c:set var="constraintLogicExpr" value="${fn:replace(QUERY.groupedConstraintLogic,'[','')}" />
<c:set var="constraintLogicExpr" value="${fn:replace(constraintLogicExpr,']','')}" />

<div id="constraintLogicContainer">
<strong><fmt:message key="query.constraintLogic"/>:</strong>
  <c:choose>
    <c:when test="${fn:length(QUERY.allConstraints) == 1}">
      <div class="smallnote altmessage"><fmt:message key="query.oneConstraint"/></div>
    </c:when>
    <c:when test="${fn:length(QUERY.allConstraints) == 0}">
      <div class="smallnote altmessage"><fmt:message key="query.noConstraints"/></div>
    </c:when>
    <c:otherwise>
	    <span id="constraintLogic" title="Click to Edit" alt="Click to Edit">${constraintLogicExpr}</span>
	    <span id="editConstraintLogic" style="display: none">
	        <%--<html:link action="/query?editExpression" style="font-size: 11px">
	          <fmt:message key="query.logicEdit"/>
	        </html:link>--%>
		    <input type="test" name="expr" id="expr" size="20" value="${constraintLogicExpr}"/>
		    <button id="editconstraintlogic" type="button" style="font-size: 11px"><fmt:message key="query.logicUpdate"/></button>
	    </span>
    </c:otherwise>
  </c:choose>
</div>
<!-- /mainLogic.jsp -->
