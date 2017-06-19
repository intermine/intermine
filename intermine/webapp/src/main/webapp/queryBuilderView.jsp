<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- queryBuilderView.jsp -->
<html:xhtml/>

<a name="showing"></a>

<script type="text/javascript" src="js/queryBuilderView.js"></script>

<div class="heading viewTitle">
  <fmt:message key="view.notEmpty.description"/>
</div>


<div class="body">
    <h3><fmt:message key="view.heading"/></h3>

      <fmt:message key="view.instructions"/>

      <c:if test="${fn:length(viewStrings) == 1 && iePre7 != 'true'}">
        <script type="text/javascript">
          <!--
            document.write('<fmt:message key="view.sort.instructions.onebox"/>');
          // -->
        </script>
      </c:if>

      <c:if test="${fn:length(viewStrings) > 1 && iePre7 != 'true'}">
        <noscript>
          <fmt:message key="view.intro"/>
        <fmt:message key="view.removeAllFromView.instructions"/>
        </noscript>
        <script type="text/javascript">
          <!--
            document.write('<fmt:message key="view.intro.jscript"/>');
            document.write('<fmt:message key="view.sort.instructions"/>');
            document.write('<fmt:message key="view.removeAllFromView.instructions"/>');
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
      <tiles:insert page="/queryBuilderViewLine.jsp"/>
    </c:otherwise>
  </c:choose>
</div>

<!-- /queryBuilderView.jsp -->
