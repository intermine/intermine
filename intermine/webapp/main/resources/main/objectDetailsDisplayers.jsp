<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetailsDisplayers.jsp -->

<html:xhtml/>

<tiles:importAttribute name="displayObject"/>
<tiles:importAttribute name="aspect"/>

<c:forEach items="${displayObject.clds}" var="cld">
  <c:if test="${fn:length(WEBCONFIG.types[cld.name].aspectDisplayers[aspect]) > 0}">
    <c:forEach items="${WEBCONFIG.types[cld.name].aspectDisplayers[aspect]}" var="displayer">
      <c:set var="object_bk" value="${object}"/>
      <c:set var="object" value="${displayObject.object}" scope="request"/>
      <c:set var="cld" value="${cld}" scope="request"/>
      <tiles:insert beanName="displayer" beanProperty="src"/><br/>
      <c:set var="object" value="${object_bk}" scope="request"/>
    </c:forEach>
  </c:if>
</c:forEach>

<!-- /objectDetailsDisplayers.jsp -->
