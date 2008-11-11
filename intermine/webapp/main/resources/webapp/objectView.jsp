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

<c:set var="detailsLink" value="/objectDetails?id=${resultElement.id}&amp;trail=${param.trail}|${resultElement.id}" scope="request"/>

<span class="objectView">
  <c:choose>
    <c:when test="${empty leafClds}">
      <fmt:message key="objectDetails.nullField" var="nullFieldText"/>
      <c:set var="maxLength" value="60"/>
      <c:choose>
        <c:when test="${!empty object && fn:startsWith(fn:trim(object), 'http://')}">
          <a href="${object}" class="value extlink">
            ${object}
          </a>
        </c:when>
        <c:when test="${object != null && object.class.name == 'java.lang.String' && fn:length(object) > maxLength && resultElement.keyField}">
           <html:link action="${detailsLink}">
             <im:abbreviate value="${object}" length="${maxLength}"/>...
           </html:link>
        </c:when>
        <c:when test="${object != null && object.class.name == 'java.lang.String' && fn:length(object) > maxLength && !resultElement.keyField}">
          <im:abbreviate value="${object}" length="${maxLength}"/>...
        </c:when>
        <c:when test="${resultElement.keyField}">
          <html:link action="${detailsLink}">
            <c:out value="${object}" default="${nullFieldText}"/>
          </html:link>
          <c:if test="${(!empty columnType) && (resultElement.typeClass != columnType)}">
             [<c:out value="${resultElement.type}" />]
          </c:if>
        </c:when>
        <c:when test="${empty object}">
          ${nullFieldText}
        </c:when>
        <c:otherwise>
            <c:out escapeXml="false" value="${object}" default="${nullFieldText}"/>
            <%-- for IE 6: --%> &nbsp;
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <span style="white-space:nowrap">
        <c:forEach var="cld" items="${leafClds}">
          <span class="type"><c:out value="${cld.unqualifiedName}"/></span>
        </c:forEach>
        [<html:link action="${detailsLink}">
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
</span>
<!-- /objectView.jsp -->
