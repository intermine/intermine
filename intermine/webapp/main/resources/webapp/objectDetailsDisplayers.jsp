<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetailsDisplayers.jsp -->

<html:xhtml/>

<tiles:importAttribute name="displayObject"/>
<tiles:importAttribute name="placement"/>
<tiles:importAttribute name="heading" ignore="true"/>
<tiles:importAttribute name="showOnLeft" ignore="true"/>

<c:forEach items="${displayObject.clds}" var="cld">
  <c:if test="${fn:length(WEBCONFIG.types[cld.name].longDisplayers) > 0}">
    <c:if test="${heading == true}">
        <h3>Further information for this ${cld.unqualifiedName}</h3>
    </c:if>

    <c:forEach items="${WEBCONFIG.types[cld.name].aspectDisplayers[placement]}" var="displayer">
      <c:if test="${(empty showOnLeft && displayer.showOnLeft == 'false') || (showOnLeft == displayer.showOnLeft)}">
        <c:set var="object_bk" value="${object}"/>
        <c:set var="object" value="${displayObject.object}" scope="request"/>
        <c:set var="cld" value="${cld}" scope="request"/>
        <tiles:insert beanName="displayer" beanProperty="src"/>
        <c:set var="object" value="${object_bk}" scope="request"/>
      </c:if>
    </c:forEach>
  </c:if>
</c:forEach>

<!-- /objectDetailsDisplayers.jsp -->