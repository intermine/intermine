<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<tiles:useAttribute id="name" name="name"/>
<tiles:useAttribute id="type" name="type"/>
<tiles:useAttribute id="state" name="state" ignore="true"/>
<tiles:useAttribute id="index" name="index"/>
<tiles:useAttribute id="currentValue" name="currentValue" ignore="true"/>

<c:if test="${empty currentValue}">
  <c:set var="currentValue" value="${name}"/>
</c:if>

<c:set var="uid" value="${name}${type}"/>

<!-- renamableElement.jsp -->
<html:xhtml/>
 <span id="form_${uid}" style="display:none;">
    <input type="text" id="newName_${uid}" name="newName_${uid}" value="${currentValue}" size="10" onkeypress="return noenter();" />
    <input type="button" name="rename" value="Rename" onclick="renameElement('${name}','${type}','${index}')"/>
  </span>
  <span id="name_${uid}">
    <c:set var="nameForURL"/>
    <str:encodeUrl var="nameForURL">${name}</str:encodeUrl>
  <span id="linkBag_${name}">
  <c:choose>
    <c:when test="${type == 'bag' && (state == 'CURRENT' || state == 'NOT_CURRENT')}">
      <html:link action="/bagDetails?bagName=${nameForURL}">
        <c:out value="${currentValue}"/>
      </html:link>
    </c:when>
    <c:when test="${type == 'bag' && state =='TO_UPGRADE'}">
      <html:link action="/bagUpgrade?bagName=${nameForURL}" styleClass="bagToUpgrade">
          <c:out value="${currentValue}"/>
      </html:link>
    </c:when>
    <c:otherwise>
      <c:out value="${currentValue}"/>
    </c:otherwise>
  </c:choose>
  </span>
 </span>
 <span id="editName_${uid}">
 <c:if test="${(type != 'bag' || (type == 'bag' && (state == 'CURRENT' || state == 'NOT_CURRENT')))}">
  <a href="javascript:editName('${uid}');">
    <img border="0" src="images/edit.gif" width="13" height="13" title="Click here to rename this item"/>
  </a>
 </c:if>
 </span>
<!-- /renamableElement.jsp -->
