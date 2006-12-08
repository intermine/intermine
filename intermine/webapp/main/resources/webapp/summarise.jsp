<%@ include file="/shared/taglibs.jsp" %>

<!--summarise.jsp-->
<c:choose>
<c:when test="${isSummarised=='false'}"> | 
	<tiles:importAttribute name="templateName" ignore="false"/>
	<c:set var="templateName" value="${fn:replace(templateName,'\\'','#039;')}" />
	
	<span id="summarise_${templateName}">
	<html:link  href="javascript:summariseTemplate('${templateName}');" >
	   	  Summarise
	    </html:link>
	</span>
</c:when>
<c:when test="${isSummarised=='summarising'}">
	<span>&nbsp;|&nbsp;Summarising...</span>
</c:when>
<c:otherwise>
	&nbsp;|&nbsp;<span style="color:#777">Summarised</span>
</c:otherwise>
</c:choose>
<!--summarise.jsp-->
