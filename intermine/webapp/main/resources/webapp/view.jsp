<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- view.jsp -->
<html:xhtml/>

<a name="showing"></a>

<script type="text/javascript" src="js/view.js"></script>
<script type="text/javascript">
  jQuery(document).ready(function () {
   	AjaxServices.getSortOrderMap(function(sortMap) {
   		reDrawSorters(sortMap);
   	});
  });
</script>

<div class="heading viewTitle">
  <fmt:message key="view.notEmpty.description"/>
</div>


<div class="body">
    <h3><fmt:message key="view.heading"/></h3>

      <fmt:message key="view.instructions"/>

      <c:if test="${fn:length(viewStrings) > 1 && iePre7 != 'true'}">
        <noscript>
          <fmt:message key="view.intro"/>
        </noscript>
        <script type="text/javascript">
          <!--
            document.write('<fmt:message key="view.intro.jscript"/>');
          // -->
        </script>
      </c:if>

  <div class="bodyPeekaboo" id="viewDrop" style="float:left">

  <br/>

  <c:choose>
    <c:when test="${empty viewStrings}">
      <div class="body">
  <p><i><fmt:message key="view.empty.description"/></i>&nbsp;</p>
      </div>
    </c:when>
    <c:otherwise>
      <tiles:insert page="/viewLine.jsp"/>
    </c:otherwise>
  </c:choose>


  <c:if test="${fn:length(viewStrings) > 0}">
<%--    <div>
      <h3><fmt:message key="sortOrder.heading"/></h3>
      <fmt:message key="sortOrder.instructions"/>
    </div>

    <br/>

    <!-- sort by -->
    <c:if test="${!empty viewStrings}">
      <tiles:insert page="/sortOrderLine.jsp"/>
    </c:if>

    <br/>
    <br/>
--%>
  </c:if>
</div>

<!-- /view.jsp -->
