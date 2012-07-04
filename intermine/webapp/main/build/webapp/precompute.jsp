<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/shared/taglibs.jsp" %>

<!--precompute.jsp-->
<tiles:importAttribute name="templateName" ignore="false"/>
<tiles:importAttribute name="precomputedTemplateMap" ignore="false"/>

<c:set var="templateName" value="${fn:replace(templateName,'\\'','#039;')}" />
<c:set var="isPrecomputed" value="${precomputedTemplateMap[templateName]}" />

<c:choose>
<c:when test="${isPrecomputed=='false'}"> | 
  <span id="precompute_${templateName}">
  <html:link  href="javascript:precomputeTemplate('${templateName}');" >
         Precompute
      </html:link>
  </span>
</c:when>
<c:when test="${isPrecomputed=='precomputing'}">
  <span>&nbsp;|&nbsp;Precomputing...</span>
</c:when>
<c:otherwise>
  &nbsp;|&nbsp;<span style="color:#777">Precomputed</span>
</c:otherwise>
</c:choose>
<!--precompute.jsp-->
