<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- layout.jsp -->
<html:xhtml/>
<html:html locale="true" xhtml="true">
  <%-- from the tiles config file for description.jsp --%>
  <tiles:importAttribute name="pageName" scope="request"/>

  <head>
    <html:base/>
    <link rel="stylesheet" type="text/css" href="webapp.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    
    <script type="text/javascript" src="js/prototype.js"></script>
    <script type="text/javascript" src="js/scriptaculous.js"></script>
    
	<script type='text/javascript' src='dwr/interface/AjaxServices.js'></script>
	<script type='text/javascript' src='dwr/engine.js'></script>
	<script type='text/javascript' src='dwr/util.js'></script>
    <script type="text/javascript" src="js/imdwr.js"></script>
    <script type="text/javascript" src="js/imutils.js" ></script>
    
    <meta content="microarray, bioinformatics, drosophila, genomics" name="keywords"/>
    <meta content="Integrated queryable database for Drosophila and Anopheles genomics" 
          name="description"/>
    <meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type"/>
    
    <title>
      <tiles:get name="title"/>
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
    
  </head>
  
  <body>
  
    <tiles:get name="header"/>
    <tiles:get name="menu"/>
    <div id="pagecontent">
    
      <%-- Render messages --%>
      <tiles:get name="errorMessages"/>
      <%-- Context help bar --%>
      <tiles:insert page="/contextHelp.jsp"/>
      
      <%-- Construct help page key --%>
      <fmt:message key="${pageName}.help.link" var="helplink"/>

      <c:if test="${!empty helplink && !fn:startsWith(helplink, '???')}">
        <c:set var="helpUrl" value="${WEB_PROPERTIES['project.helpLocation']}${helplink}" 
               scope="request"/>
      </c:if>
      <im:box titleKey="${pageName}.description" helpUrl="${helpUrl}">
        <tiles:get name="body"/>
      </im:box>
      
      <c:if test="${pageName != 'feedback'}">
        <div id="feedbackFormDivButton">
          <im:vspacer height="11"/>
          <div class="expandButton">
             <a href="#" onclick="showFeedbackForm();return false">
               <b>${WEB_PROPERTIES["feedback.title"]}</b>
             </a>
        	</div>
        </div>
      
        <div id="feedbackFormDiv" style="display:none">
            <im:vspacer height="11"/>
            <im:box title="${WEB_PROPERTIES['feedback.title']}"
                    helpUrl="${WEB_PROPERTIES['project.helpLocation']}/manualFeedback.shtml">
              <tiles:get name="feedbackForm"/>
            </im:box>
        </div>
      </c:if>
      
      <c:if test="${param.debug != null}">
        <im:vspacer height="11"/>
        <im:box title="Debug">
          <tiles:insert page="/session.jsp"/>
        </im:box>
      </c:if>
      
    </div>
    
    <c:if test="${IS_SUPERUSER}">
      <div class="admin-msg">
        <span class="smallnote">
          <fmt:message key="intermine.superuser.msg"/>
        </span>
      </div>
    </c:if>
    

<script src="http://www.google-analytics.com/urchin.js" type="text/javascript">
</script>
<script type="text/javascript">
_uacct = "UA-1566492-2";
urchinTracker();
</script>

  </body>
</html:html>
<!-- /layout.jsp -->

