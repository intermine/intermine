<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- goStatDisplayer.jsp -->


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
  
  <head>
    <base href="http://eowyn:8080/flymine/layout.jsp" />

    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    
    <script type="text/javascript" src="js/prototype.js"></script>
    <script type="text/javascript" src="js/scriptaculous.js"></script>
    
	<script type='text/javascript' src='dwr/interface/AjaxServices.js'></script>
	<script type='text/javascript' src='dwr/engine.js'></script>
	<script type='text/javascript' src='dwr/util.js'></script>

    <script type="text/javascript" src="js/imdwr.js"></script>
    
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    
    <title>FlyMine: Bag details page</title>
    
    <script type="text/javascript">
    <!--
      function showFeedbackForm()
      {
        document.getElementById('feedbackFormDiv').style.display='';
        document.getElementById('feedbackFormDivButton').style.display='none';
        window.scrollTo(0, 99999);
        document.getElementById("fbname").focus();
      }

      var editingTag;

      function addTag(uid, type) {
        var tag = $('tagValue-'+uid).value;
        new Ajax.Request('/flymine/inlineTagEditorChange.do',
            {parameters:'method=add&uid='+uid+'&type='+type+'&tag='+tag, asynchronous:false});
        refreshTags(uid, type);
        $('tagValue-'+uid).value='';
      }
      
      function startEditingTag(uid) {
        if (editingTag) {
          stopEditingTag();
        }
        editingTag = uid;
        $('tagsEdit-'+editingTag).style.display='';
        $('addLink-'+editingTag).style.display='none';
        $('tagValue-'+editingTag).focus();
      }

      function stopEditingTag() {
        if (editingTag) {
          $('tagsEdit-'+editingTag).style.display='none';
          $('addLink-'+editingTag).style.display='';
        }
        editingTag = '';
      }

      function refreshTags(uid, type) {
        new Ajax.Updater('currentTags-'+uid, '/flymine/inlineTagEditorChange.do',
            {parameters:'method=currentTags&uid='+uid+'&type='+type, asynchronous:true});
      }
    //-->
    </script>
    <script type="text/javascript" src="js/bagDetails.js" ></script>
  </head>

  
  <body>
  

<html:xhtml/>

<!-- request getParameter bagName -->


<c:if test="${ontology == 'biological_process'}">
	<c:set var="bioSELECTED">selected</c:set>
</c:if>	
<c:if test="${ontology == 'cellular_component'}">
	<c:set var="cellSELECTED">selected</c:set>
</c:if>	
<c:if test="${ontology == 'molecular_function'}">
	<c:set var="moleSELECTED">selected</c:set>
</c:if>	



<div class="body">

<table>
<tr>
	<td>	
	
<table cellpadding="5" border="0" cellspacing="0" class="results">	
  	<tr>	
  		<th>GO Term</td>
  		<th>p-value</td>
  		<th>&nbsp;</td>
	</tr>
  	<c:forEach items="${pvalues}" var="results"  varStatus="status">
  	    <c:set var="object" value="${row[0]}" scope="request"/>
       		<c:set var="rowClass">
       		<c:choose>
          		<c:when test="${status.count % 2 == 1}">odd</c:when>
				<c:otherwise>even</c:otherwise>
			</c:choose>
		</c:set>

    <tr class="${rowClass}">  	
  	<tr>	
  		<td align="left"><c:out value="${results.key}" /></td>
  		<td align="left"><!-- <c:out value="${results.value}" />-->
  		<fmt:formatNumber value="${results.value}" minFractionDigits="7" maxFractionDigits="7" />
  		</td> 		
  		<td align="left">
  		   		<html:link action="/goStatAction?key=${results.key}&bag=${bagName}" target="_top">
  		   			[view genes ...]
       			</html:link>  	
       	</td>
	</tr>
	</c:forEach>
</table>
</td><td valign="top">
	<form action="initGoStatDisplayer.do" method="get" name="goStatForm">
	<table>
      <tr>
      	<td>Ontology</td>
      	<td>
      		<select name="ontology">
           		<option value="biological_process" ${bioSELECTED}>Biological process</option>
           		<option value="cellular_component" ${cellSELECTED}>Cellular component</option>
           		<option value="molecular_function" ${moleSELECTED}>Molecular function</option>
      		</select>
      	</td>
      </tr>
      <tr>
      	<td>&nbsp;</td>
      	<td>
      		<input type="hidden" name="bagName" value="${bagName}"/>
			<input type="submit" name="filterSubmit" value="Update results">
	    </td>
	   </tr>
	</table>
	</form>
	
</td>
</tr>
</table>


 </div>
 </body>
 </html>
 
<!-- /goStatDisplayer.jsp -->