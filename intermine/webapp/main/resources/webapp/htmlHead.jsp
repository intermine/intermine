<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- htmlHead.jsp -->
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/webapp.css'/>"/>
<%
/* In Safari, loading a css that doesnt exist causes weirdness */
String pageName = (String) request.getAttribute("pageName");
if(new java.io.File(application.getRealPath("css")+"/"+pageName+".css").exists()) {
        request.setAttribute("pageCSS","true");
}
%>
<c:if test="${pageCSS == 'true'}">
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/css/${pageName}.css'/>"/>
</c:if>
<link rel="stylesheet" type="text/css" href="<html:rewrite page='/theme/theme.css'/>"/>

<script type="text/javascript" src="<html:rewrite page='/js/prototype.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/scriptaculous.js'/>"></script>

<script type="text/javascript" src="<html:rewrite page='/dwr/interface/AjaxServices.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/dwr/engine.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/dwr/util.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/imdwr.js'/>"></script>
<script type="text/javascript" src="<html:rewrite page='/js/imutils.js'/>"></script>

<meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
<meta content="Integrated queryable database for Drosophila and Anopheles genomics"
      name="description"/>
<meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>

<title>
  <c:choose>
    <c:when test="${empty pageName}">
      <c:out value="${WEB_PROPERTIES['project.title']}" escapeXml="false"/>
    </c:when>
    <c:otherwise>
      <fmt:message key="${pageName}.title" var="pageTitle">
        <fmt:param value="${param.name}"/>
      </fmt:message>
      <c:out value="${WEB_PROPERTIES['project.title']}: ${pageTitle}" escapeXml="false"/>
    </c:otherwise>
  </c:choose>
</title>

     

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
    new Ajax.Request('<html:rewrite action="/inlineTagEditorChange"/>',
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
    new Ajax.Updater('currentTags-'+uid, '<html:rewrite action="/inlineTagEditorChange"/>',
        {parameters:'method=currentTags&uid='+uid+'&type='+type, asynchronous:true});
  }
//-->
</script>
<!-- /htmlHead.jsp -->
