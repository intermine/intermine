<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pathwayLinkDisplayer.jsp -->

<%-- get dataset name --%>
<c:forEach items="${object.dataSets}" var="dataset">
    <c:set var="datasetTitle" value="${dataset.name}"/>
</c:forEach>

<%-- set variables ~~ based on dataset, set link and image to KEGG or Reactome --%>
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

<%-- display logo and link --%>
<table class="lookupReport" cellspacing="5" cellpadding="0">
<tr>
    <td align="right">
            <a href="${href}" class="ext_link" target="_new"><html:img src="model/images/${imageName}" width="28" height="20" title="${text}"/>${text}: ${object.identifier}</a>
    </td>
</tr>
</table>
<!-- /pathwayLinkDisplayer.jsp -->
