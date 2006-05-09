<%@ include file="/shared/taglibs.jsp" %>

<!--precompute.jsp-->
<c:choose>
<c:when test="${isPrecomputed=='false'}"> | 
	<script type='text/javascript' src='dwr/interface/AjaxServices.js'></script>
	<script type='text/javascript' src='dwr/engine.js'></script>
	<script type='text/javascript' src='dwr/util.js'></script>
	<script language="javascript">
		function precomputeTemplate(templateName){
			document.getElementById('precompute_'+templateName).innerHTML="Precomputing..";
			AjaxServices.preCompute(templateName,function(str) { 
				document.getElementById('precompute_'+templateName).style.color="#777";
				document.getElementById('precompute_'+templateName).innerHTML="Precomputed";
			 });
		}
	</script>
	
	<tiles:importAttribute name="templateName" ignore="false"/>
	<c:set var="templateName" value="${fn:replace(templateName,'\\'','#039;')}" />
	
	<span id="precompute_${templateName}">
	<html:link  href="javascript:precomputeTemplate('${templateName}')" >
	   	  Precompute
	    </html:link>
	</span>
</c:when>
<c:when test="${isPrecomputed=='precomputing'}">
	<span>&nbsp;|&nbsp;Precomputing...</span>
</c:when>
<c:otherwise>
	&nbsp;|&nbsp;<span style="color:#777">Precomputed</span>
</c:otherwise>
</c:choose>
<!--precompute.jsp-->