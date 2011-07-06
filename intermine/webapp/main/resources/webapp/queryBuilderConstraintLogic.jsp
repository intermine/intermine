<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- queryBuilderConstraintLogic.jsp -->

<script language="javascript">
jQuery(document).ready(function(){
  jQuery('#constraintLogic').click(function() {
    jQuery('#constraintLogic').toggle();
    jQuery('#editConstraintLogic').toggle();
  });

    jQuery('#editconstraintlogic').click(function() {
      setConstraintLogic(jQuery('#expr').val());
      jQuery('#permalink div.popup').hide();
  });

    jQuery('#constraintLogic').text(jQuery('#expr').val());
});
</script>

<html:xhtml/>

<c:set var="constraintLogicExpr" value="${fn:replace(QUERY.groupedConstraintLogic,'[','')}" />
<c:set var="constraintLogicExpr" value="${fn:replace(constraintLogicExpr,']','')}" />

<div id="constraintLogicContainer">
<strong><fmt:message key="query.constraintLogic"/>:</strong>
  <c:choose>
    <c:when test="${fn:length(QUERY.constraintCodes) == 1}">
      <div class="smallnote altmessage"><fmt:message key="query.oneConstraint"/></div>
    </c:when>
    <c:when test="${fn:length(QUERY.constraintCodes) == 0}">
      <div class="smallnote altmessage"><fmt:message key="query.noConstraints"/></div>
    </c:when>
    <c:otherwise>
      <c:out value="${constraintLogicExpr}"/><br/><br/>
      <span id="constraintLogic" title="Click to Edit" alt="Click to Edit">${constraintLogicExpr}</span>
      <span id="editConstraintLogic" style="display: none">
        <input type="text" name="expr" id="expr" size="20" value="${constraintLogicExpr}"/>
          <input id="editconstraintlogic" type="button" style="font-size: 11px" value="<fmt:message key="query.logicUpdate"/>" />
      </span>
    </c:otherwise>
  </c:choose>
</div>
<!-- /queryBuilderConstraintLogic.jsp -->
