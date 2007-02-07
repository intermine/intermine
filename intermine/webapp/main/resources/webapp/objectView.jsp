<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<tiles:importAttribute/>

<!-- objectView.jsp -->
<html:xhtml/>
<c:set var="object" value="${resultElement.field}"/>

<c:set var="leafClds" value="${LEAF_DESCRIPTORS_MAP[object]}"/>

<div class="objectView">
  <c:choose>
    <c:when test="${empty leafClds}">
      <fmt:message key="objectDetails.nullField" var="nullFieldText"/>
      <c:set var="maxLength" value="60"/>
      <c:choose>
        <c:when test="${object != null && object.class.name == 'java.lang.String' && fn:length(object) > maxLength}">
          <im:abbreviate value="${object}" length="${maxLength}"/>
        </c:when>
        <c:when test="${resultElement.keyField}">
          <html:link action="/objectDetails?id=${resultElement.id}&amp;trail=${param.trail}_${resultElement.id}">
            <c:out value="${object}" default="${nullFieldText}"/>
          </html:link>
          <c:if test="${(!empty columnType) && (resultElement.type != columnType)}">
 	        [<c:out value="${resultElement.type}" />]
 	      </c:if>
        </c:when>
        <c:otherwise>
          <c:out value="${object}" default="${nullFieldText}"/>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <c:if test="${fn:substring(param.table, 0, 7) == 'results'}">
        <c:set var="prepend" value="_${param.table}"/>
      </c:if>
      <c:set var="linkAction" value="/objectDetails?id=${resultElement.id}&amp;trail=${prepend}${param.trail}_${resultElement.id}" scope="request"/>
      <span style="white-space:nowrap">
        <c:forEach var="cld" items="${leafClds}">
          <span class="type"><c:out value="${cld.unqualifiedName}"/></span>
        </c:forEach>
        [<html:link action="${linkAction}">
          <fmt:message key="results.details"/>
        </html:link>]
      </span>
      <br/>
      <div style="margin-left: 8px">
        <c:set var="displayObject" value="${DISPLAY_OBJECT_CACHE[object]}"/>
        <c:forEach items="${displayObject.fieldExprs}" var="expr">
          <im:eval evalExpression="object.${expr}" evalVariable="outVal"/>
          <c:if test="${displayObject.fieldConfigMap[expr].showInResults}">
            <c:set var="style" value="white-space:nowrap"/>
            <c:if test="${outVal.class.name == 'java.lang.String' && fn:length(outVal) > 25}">
              <c:if test="${fn:length(outVal) > 65}">
                <c:set var="outVal" value="${fn:substring(outVal, 0, 60)}..." scope="request"/>
              </c:if>
              <c:set var="style" value=""/>
            </c:if>
            <div style="${style}">
              <span class="attributeField">${expr}</span>
              <im:value>${outVal}</im:value>
            </div>
          </c:if>
        </c:forEach>
        <c:forEach items="${leafClds}" var="cld">
          <c:if test="${WEBCONFIG.types[cld.name].tableDisplayer != null}">
            <div>
              <c:set var="cld" value="${cld}" scope="request"/>
              <tiles:insert page="${WEBCONFIG.types[cld.name].tableDisplayer.src}"/>
            </div>
          </c:if>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</div>
<!-- /objectView.jsp -->
