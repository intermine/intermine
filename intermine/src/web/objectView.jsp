<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- objectView.jsp -->
<html:xhtml/>
<div class="objectView">
  <c:choose>
    <c:when test="${empty leafClds}">
      <fmt:message key="objectDetails.nullField" var="nullFieldText"/>
      <c:set var="maxLength" value="60"/>
      <c:choose>
        <c:when test="${object != null && object.class.name == 'java.lang.String' && fn:length(object) > maxLength}">
          ${fn:substring(object, 0, maxLength)} ...
        </c:when>
        <c:otherwise>
          <c:out value="${object}" default="${nullFieldText}"/>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <span style="white-space:nowrap">
        <c:forEach var="cld" items="${leafClds}">
          <span class="type"><c:out value="${cld.unqualifiedName}"/></span>
        </c:forEach>
        [<html:link action="/objectDetails?id=${object.id}&amp;trail=${param.trail}_${object.id}">
          <fmt:message key="results.details"/>
        </html:link>]
      </span>
      <br/>
      <div style="margin-left: 8px">
        <c:forEach var="cld" items="${leafClds}">
          <c:set var="cld" value="${cld}" scope="request"/>
          <c:set var="fieldDescriptor" value="${fieldDescriptor}" scope="request"/>
          <c:set var="object" value="${object}" scope="request"/>
          <c:set var="primaryKeyFields" value="${primaryKeyFields}" scope="request"/>
          <tiles:insert name="/objectFields.jsp"/>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</div>
<!-- /objectView.jsp -->
