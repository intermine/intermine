<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%-- Page displaying link to webservice. --%>

<style type="text/css">
.highlighted {
    color: red;
}

</style>

<script type="text/javascript">
<!--
    function switchLink(controllerId, linkId) {
        var link = document.getElementById(linkId);
        var con = document.getElementById(controllerId);
        if (link.style.display == 'none') {
            link.style.display = 'block';
            con.innerHTML = 'Hide';
        } else {
            link.style.display = 'none';
            con.innerHTML = 'Show';        
        }
    }
//-->
</script>

<!-- serviceLink.jsp -->
<div align="center" ><div class="plainbox" style="width:700px; font-size:14px; overflow: auto">

	<c:choose>
		<c:when test="${pageTitle != null}">
		    <c:set var="pageTitle" value="${pageTitle}"></c:set>
		</c:when>
		<c:otherwise>
		    <c:set var="pageTitle" value="Resource link"></c:set>
		</c:otherwise>
	</c:choose>

	<h1>${pageTitle}</h1>
	<div>${pageDescription}</div>
	
	<form action="">
	
	    <div  style="margin-top: 10px;">
	       <%-- Replacing & with unique string with almost 0 probability that it will be in url --%>
           <c:set var="encodedHtmlLink" value="${fn:replace(htmlLink, '&', 'qwertyui')}"></c:set>
           <c:set var="tmpLink" value="${tabLink}&size=10"></c:set> 
           <c:set var="encodedTabLink" value="${fn:replace(tmpLink, '&', 'qwertyui')}"></c:set> 
	       <table style="width:100%;">
	           <tr>
	               <td style="font-size:14px;">
	                   Add following HTML to embed the results of this template in your web page. 
	               </td>
	               <td valign="bottom" style="font-size:14px;" align="right">
                       <a href="javascript:openPopWindow('linkPreview.do?link=${encodedHtmlLink}', 800, 800)" >See results</a>  in the example page.
                   </td>
	       </table>
	    </div>
	    <%-- Don't split following line --%>
	    <textarea style="width:100%;height:100px;"><iframe width=&quot;700&quot; height=&quot;500&quot; frameborder=&quot;1&quot; scrolling=&quot;yes&quot; marginheight=&quot;0&quot; marginwidth=&quot;0&quot; src=&quot;${htmlLink}&amp;format=html&quot;></iframe></textarea>
	
	   <c:if test="${highlightedLink != null}">
	        <div style="margin-top:10px;">
	           If you want to get results for different parameters, edit the highlighted constraints 
	           in the URL to the values you need. <a href="javascript:switchLink('showLinkId', 'highLinkId')" id="showLinkId">Show</a> link with constraint values highlighted in red.
	        </div>
	        <div id="highLinkId" style="display: none;">
	           ${highlightedLink}
	        </div>
        </c:if>
	
	    <div style="margin-top: 10px;">
	       <table style="width:100%;">
	           <tr>
	               <td style="font-size:14px;">
	                   Web service link to this template. Use following URL to fetch tab delimited results for this template in your own program. 
	               </td>
                    <td valign="bottom" style="font-size:14px;" align="right">
                       <a href="javascript:openPopWindow('tabLinkPreview.do?link=${encodedTabLink}', 800, 1000)">See first 10 results</a>
                   </td>
	           </tr>
	       </table>
	    </div>
	    <%-- Don't split following line --%>
	    <textarea style="width:100%;height:100px;">${tabLink}&amp;format=tab</textarea>
	    
	</form>
		
	<span style="float:left;">For other options and detailed help <a href="http://intermine.org/wiki/TemplateWebService">click here</a>.</span> 
    <span style="float:right;"><a href="javascript:history.go(-1)">Go back</a></span>

</div></div>
<!-- /serviceLink.jsp -->

