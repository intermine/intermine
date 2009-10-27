<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pathwayLinkDisplayer.jsp -->

<c:forEach items="${object.dataSets}" var="dataset">
	<c:set var="datasetTitle" value="${dataset.title}"/>
</c:forEach>

<c:choose>        
<c:when test="${datasetTitle == 'KEGG pathways data set'}">
	<%-- KEGG --%>
	<c:set var="imageName" value="KEGG_logo_small.gif"/>
	<c:set var="text" value="KEGG"/>
	<c:set var="href" value="http://www.genome.jp/dbget-bin/show_pathway?dme${object.identifier}"/>
</c:when>
<c:otherwise>
	<%-- reactome --%>
	<c:set var="imageName" value="reactome_logo.png"/>
	<c:set var="text" value="Reactome"/>
	<c:set var="href" value="http://www.reactome.org"/>
	
	<c:if test="${object.curated}">
       	<c:set var="href" value="http://fly.reactome.org/cgi-bin/eventbrowser?DB=test_fly_reactome_release_1_myisam&ID=${object.identifier}"/>
    </c:if>
</c:otherwise>
</c:choose>
          
<table class="lookupReport" cellspacing="5" cellpadding="0">
<tr>
	<td align="right">
    	<c:if test="${!empty imageName}">
        	<a href="${href}" class="ext_link" target="_new"><html:img src="model/images/${imageName}" width="28" height="20" title="${text}"/></a>
        </c:if>
    </td>
    <td>
         <c:if test="${!empty text}">
            <a href="${href}" class="ext_link" target="_new">${text}&nbsp;<img src="images/ext_link.png" title="${text}"/></a>
         </c:if>
    </td>
</tr>
</table>
<!-- /pathwayLinkDisplayer.jsp -->
