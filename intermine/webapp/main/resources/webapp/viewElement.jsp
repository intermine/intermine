<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- viewElement.jsp -->
<html:xhtml/>
   <im:viewableDiv path="${pathString}" viewPaths="${viewPaths}" idPrefix="showing" idPostfix="_${status.index}">
   
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
          <img border="0" align="middle"
               src="images/cross.gif" width="13" height="13"
               alt="${removeFromViewString}" style="margin-top: 3px;"/>
        </html:link>
        
	</div>
	
  <c:if test="${!empty path}">
    <im:prefixSubstring str="${pathString}" outVar="pathPrefix" delimiter="."/>
    <tiles:insert name="viewElementDescription.jsp">
      <tiles:put name="pathString" value="${pathString}"/>
      <tiles:put name="description" value="${QUERY.pathStringDescriptions[pathPrefix]}"/>
    </tiles:insert>
  </c:if>

      <div style="white-space:nowrap;">
        <noscript>
          <c:choose>
            <c:when test="${status.first}">
              <img style="margin-right: 5px" border="0" align="middle"
                   src="images/blank13x13.gif" alt=" " width="13" height="13"/>
            </c:when>
            <c:otherwise>
              <fmt:message key="view.moveLeftHelp" var="moveLeftTitle">
                <fmt:param value="${pathString}"/>
              </fmt:message>
              <fmt:message key="view.moveLeftSymbol" var="moveLeftString"/>

              <html:link action="/viewChange?method=moveLeft&amp;index=${status.index}"
                         title="${moveLeftTitle}">
                <img style="margin-right: 5px" border="0" align="middle"
                     src="images/left-arrow-square.gif" width="13" height="13"
                     alt="${moveRightString}"/>
              </html:link>

            </c:otherwise>
          </c:choose>
        </noscript>

		<%-- sort button --%>
           <c:choose>
            <c:when test="${sortOrderString != pathString}">
                <a href="javascript:updateSortOrder('${pathString}');">
                	<img style="margin-left: 5px" border="0" align="middle"
                    	 src="images/sort.gif" width="43" height="13"
                     	alt="${pathString}"/>
              </a>
		
            </c:when>
            <c:otherwise>    
                <a href="javascript:updateSortOrder('${pathString}');">
                	<img style="margin-left: 5px" border="0" align="middle"
                    	 src="images/sort.gif" width="43" height="13"
                     	alt="${pathString}"/>
              </a>
			</c:otherwise>
          </c:choose>
		

      </div>
    </im:viewableDiv>
<!-- /viewElement.jsp -->
