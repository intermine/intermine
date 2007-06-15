<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- viewElement.jsp -->
<html:xhtml/>

<c:set var="iePre7" value='<%= request.getHeader("user-agent").matches(".*MSIE [123456].*") %>'/>

<im:viewableDiv path="${pathString}" viewPaths="${viewPaths}" idPrefix="showing" idPostfix="_${viewIndex}">

  <%-- class name --%>
  <div>
    <html:link action="/mainChange?method=changePath&amp;prefix=${viewPathLinkPrefixes[pathString]}&amp;path=${viewPathLinkPaths[viewPathLinkPrefixes[pathString]]}">
      ${fn:replace(pathString, ".", " > ")}
    </html:link>

    <%-- (x) img --%>
    <fmt:message key="view.removeFromViewHelp" var="removeFromViewTitle">
      <fmt:param value="${pathString}"/>
    </fmt:message>
    <fmt:message key="view.removeFromViewSymbol" var="removeFromViewString"/>

    <html:link action="/viewChange?method=removeFromView&amp;path=${pathString}"
               title="${removeFromViewTitle}">
      <img border="0" align="top"
           src="images/cross.gif" width="13" height="13"
           alt="${removeFromViewString}" style="margin-top: 3px;"/>
    </html:link>

  </div>

  <c:if test="${!empty path && IS_SUPERUSER}">
    <im:prefixSubstring str="${pathString}" outVar="pathPrefix" delimiter="."/>
    <tiles:insert name="viewElementDescription.jsp">
      <tiles:put name="pathString" value="${pathString}"/>
      <tiles:put name="description" value="${QUERY.pathStringDescriptions[pathPrefix]}"/>
    </tiles:insert>
  </c:if>


  <div style="white-space:nowrap;">
    <c:if test="${!iePre7}">
      <c:out value="<noscript>" escapeXml="false"/>
    </c:if>
      <c:choose>
        <c:when test="${isFirst}">
          <img style="margin-right: 5px" border="0" align="middle"
               src="images/blank13x13.gif" alt=" " width="13" height="13"/>
        </c:when>
        <c:otherwise>
          <fmt:message key="view.moveLeftHelp" var="moveLeftTitle">
            <fmt:param value="${pathString}"/>
          </fmt:message>
          <fmt:message key="view.moveLeftSymbol" var="moveLeftString"/>

          <html:link action="/viewChange?method=moveLeft&amp;index=${viewIndex}"
                     title="${moveLeftTitle}">
            <img style="margin-right: 5px" border="0" align="middle"
                 src="images/left-arrow-square.gif" width="13" height="13"
                 alt="${moveRightString}"/>
          </html:link>

        </c:otherwise>
      </c:choose>
    <c:if test="${!iePre7}">
      <c:out value="</noscript>" escapeXml="false"/>
    </c:if>

    <%-- sort button --%>
    <input type="image" id="btn_${viewIndex}" onclick="javascript:updateSortOrder('${pathString}', '${viewIndex}');"
           width="39" height="11" alt="sort" src="images/sort.gif"
           title="Click to sort results by this field"/>

    <c:if test="${!iePre7}">
      <c:out value="<noscript>" escapeXml="false"/>
    </c:if>
    <c:choose>
      <c:when test="${isLast || errorPath}">
        <img style="margin-left: 5px" border="0" align="middle"
             src="images/blank13x13.gif" alt=" " width="13" height="13" />
      </c:when>
      <c:otherwise>
        <fmt:message key="view.moveRightHelp" var="moveRightTitle">
          <fmt:param value="${pathString}"/>
        </fmt:message>
        <fmt:message key="view.moveRightSymbol" var="moveRightString"/>

        <html:link action="/viewChange?method=moveRight&amp;index=${viewIndex}"
                   title="${moveRightTitle}">
          <img style="margin-left: 5px" border="0" align="middle"
               src="images/right-arrow-square.gif" width="13" height="13"
               alt="${moveRightString}"/>
        </html:link>

      </c:otherwise>
    </c:choose>
    <c:if test="${!iePre7}">
      <c:out value="</noscript>" escapeXml="false"/>
    </c:if>
  </div>
</im:viewableDiv>
<!-- /viewElement.jsp -->
