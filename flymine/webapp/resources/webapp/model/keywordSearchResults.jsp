<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<link rel="stylesheet" href="model/css/keywordSearch.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<div class="body">

<tiles:insert name="keywordSearch.tile"/>

<c:if test="${!empty searchTerm}">
<div class="keywordSearchResults">

<div>
    Search Term: <c:out value="${searchTerm}"/>
</div>
<div>
	<c:if test="${empty displayMax}"><c:out value="Matching submissions: ${fn:length(searchResults)}"/></c:if>
	<c:if test="${!empty displayMax}">Matching submissions: more than <c:out value="${displayMax}" /> (only the top <c:out value="${displayMax}" /> matches are displayed)</c:if>
</div>

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
<tr>
    <th>Type</th>
    <th>Details</th>
    <th>Search score</th>
</tr>
<c:forEach items="${searchResults}" var="searchResult">
  <tr class="keywordSearchResult">
      <td><c:out value="${searchResult.type}"></c:out></td>
      <td>
          <div class="objectKeys">
          <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${searchResult.id}">
          <c:forEach items="${searchResult.keyFields}" var="field" varStatus="status">
            <c:set var="fieldConfig" value="${searchResult.fieldConfigs[field]}"/>
            <span title="<c:out value="${field}"/>" class="objectKey">
               <c:choose>    
               <%-- print each field configured for this object --%>
                <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
                  <c:set var="interMineObject" value="${searchResult.object}" scope="request"/>
                  <span class="value">
                    <tiles:insert page="${fieldConfig.displayer}">
                      <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
                    </tiles:insert>
                  </span>
                </c:when>
                <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
                  <c:set var="outVal" value="${searchResult.fieldValues[fieldConfig.fieldExpr]}"/>
                  <span class="value">${outVal}</span>
                  <c:if test="${empty outVal}">
                    -
                  </c:if>
                </c:when>
                <c:otherwise>
                  -
                </c:otherwise>
              </c:choose>
            </span>
            <c:if test="${! status.last }">
                <span class="objectKey">|</span>
            </c:if>
          </c:forEach>
          </html:link>
          </div>
          
	      <%-- print each field configured for this object --%>
          <c:forEach items="${searchResult.additionalFields}" var="field">
            <c:set var="fieldConfig" value="${searchResult.fieldConfigs[field]}"/>
	        <div class="objectField">
	           <span class="objectFieldName"><c:out value="${field}"/>:</span>
	           <c:choose>	
	           <%-- print each field configured for this object --%>
	            <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
	              <c:set var="interMineObject" value="${searchResult.object}" scope="request"/>
	              <span class="value">
	                <tiles:insert page="${fieldConfig.displayer}">
	                  <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
	                </tiles:insert>
	              </span>
	            </c:when>
	            <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
	              <c:set var="outVal" value="${searchResult.fieldValues[fieldConfig.fieldExpr]}"/>
	              <span class="value" style="font-weight: bold;">${outVal}</span>
	              <c:if test="${empty outVal}">
	                &nbsp;<%--for IE--%>
	              </c:if>
	            </c:when>
	            <c:otherwise>
	              &nbsp;<%--for IE--%>
	            </c:otherwise>
	          </c:choose>
	        </div>
	      </c:forEach>
      </td>      
      <td><img height="10" width="${searchResult.points * 5}" src="images/heat${searchResult.points}.gif" alt="${searchResult.points}/10" title="${searchResult.points}/10"/></td>
</tr>
</c:forEach>
</table>



</div>
</c:if>

</div>