<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<tiles:importAttribute/>

<!-- queryBuilderViewElement.jsp -->
<html:xhtml/>

<c:set var="iePre7" value='<%= new Boolean(request.getHeader("user-agent").matches(".*MSIE [123456].*")) %>'/>

<im:viewableDiv path="${pathString}" viewPaths="${viewPaths}" idPrefix="showing" idPostfix="_${viewIndex}">

  <%-- class name --%>
  <div>
    <im:displaypath path="${pathString}"/>
    <%-- (x) img --%>
    <fmt:message key="view.removeFromViewHelp" var="removeFromViewTitle">
      <fmt:param value="${pathString}"/>
    </fmt:message>
    <fmt:message key="view.removeFromViewSymbol" var="removeFromViewString"/>&nbsp;<html:link action="/queryBuilderViewChange?method=removeFromView&amp;path=${pathString}"
               title="${removeFromViewTitle}">
      <img border="0" align="top"
           src="images/cross.gif" width="13" height="13"
           title="${removeFromViewString}" style="margin-top: 3px;"/>
    </html:link>

  </div>

  <c:if test="${!empty path}">
    <im:prefixSubstring str="${pathString}" outVar="pathPrefix" delimiter="."/>
    <tiles:insert name="queryBuilderViewDescription.jsp">
      <tiles:put name="pathString" value="${pathString}"/>
      <tiles:put name="description" value="${QUERY.descriptions[pathPrefix]}"/>
    </tiles:insert>
    </c:if>

  <div style="white-space:nowrap;">
    <c:if test="${!iePre7}">
      <c:out value="<noscript>" escapeXml="false"/>
    </c:if>
      <c:choose>
        <c:when test="${isFirst}">
          <img style="margin-right: 5px" border="0" align="middle"
               src="images/blank13x13.gif" title=" " width="13" height="13"/>
        </c:when>
        <c:otherwise>
          <fmt:message key="view.moveLeftHelp" var="moveLeftTitle">
            <fmt:param value="${pathString}"/>
          </fmt:message>
          <fmt:message key="view.moveLeftSymbol" var="moveLeftString"/>

          <html:link action="/queryBuilderViewChange?method=moveLeft&amp;index=${viewIndex}"
                     title="${moveLeftTitle}">
            <img style="margin-right: 5px" border="0" align="middle"
                 src="images/left-arrow-square.gif" width="13" height="13"
                 alt="${moveLeftString}" title="${moveLeftTitle}"/>
          </html:link>

        </c:otherwise>
      </c:choose>
    <c:if test="${!iePre7}">
      <c:out value="</noscript>" escapeXml="false"/>
    </c:if>

    <%-- sort button --%>
    <c:set var="btnName" value="${fn:replace(pathString,':','_')}"/>
    <img id="btn_${fn:replace(btnName,'.','_')}" onclick="javascript:updateSortOrder('${pathString}');"
         width="17" height="16" title="Sort by this column" src="images/sort_${viewStrings[pathString]}.png"
         title="Click to sort results by this field" class="sortbutton"/>

    <c:if test="${!iePre7}">
      <c:out value="<noscript>" escapeXml="false"/>
    </c:if>
    <c:choose>
      <c:when test="${isLast || errorPath}">
        <img style="margin-left: 5px" border="0" align="middle"
             src="images/blank13x13.gif" width="13" height="13" />
      </c:when>
      <c:otherwise>
        <fmt:message key="view.moveRightHelp" var="moveRightTitle">
          <fmt:param value="${pathString}"/>
        </fmt:message>
        <fmt:message key="view.moveRightSymbol" var="moveRightString"/>

        <html:link action="/queryBuilderViewChange?method=moveRight&amp;index=${viewIndex}"
                   title="${moveRightTitle}">
          <img style="margin-left: 5px" border="0" align="middle"
               src="images/right-arrow-square.gif" width="13" height="13"
               alt="${moveRightString}" title="${moveRightTitle}"/>
        </html:link>

      </c:otherwise>
    </c:choose>
    <c:if test="${!iePre7}">
      <c:out value="</noscript>" escapeXml="false"/>
    </c:if>
  </div>
</im:viewableDiv>

<!-- /queryBuilderViewElement.jsp -->
