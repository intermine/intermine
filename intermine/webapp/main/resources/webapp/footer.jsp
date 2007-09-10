<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!-- footer.jsp -->

<br/><br/><br/>

<div class="body" align="center" style="clear:both">



      <c:if test="${pageName != 'feedback'}">
        <div id="feedbackFormDivButton">
          <im:vspacer height="11"/>
          <div class="feedbackButton">
             <a href="#" onclick="showFeedbackForm();return false">
               <b>${WEB_PROPERTIES["feedback.title"]}</b>
             </a>
          </div>
        </div>
      
      <div id="feedbackFormDiv" style="display:none;">
            <im:vspacer height="11"/>
            <im:box title="${WEB_PROPERTIES['feedback.title']}"
                    helpUrl="${WEB_PROPERTIES['project.helpLocation']}/manualFeedback.shtml">
              <tiles:get name="feedbackForm"/>
            </im:box>
        </div>
      </c:if>
           
<br/>

<font size="0.8em"

<html:link action="/begin">
Home
</html:link>
&nbsp;|&nbsp;
<html:link action="/templates">
Templates
</html:link>
&nbsp;|&nbsp;
<html:link action="/bag">
Lists
</html:link>
&nbsp;|&nbsp;
<html:link action="/customQuery">
QueryBuilder
</html:link>
&nbsp;|&nbsp;
<html:link action="/dataCategories">
Data
</html:link>
&nbsp;|&nbsp;
<html:link action="/mymine">
MyMine
</html:link>
&nbsp;|&nbsp;
<a href="${WEB_PROPERTIES['project.sitePrefix']}/help.shtml">
Help
</a>
&nbsp;|&nbsp;
<a href="${WEB_PROPERTIES['project.sitePrefix']}/about.shtml">
About
</a>
&nbsp;|&nbsp;
<a href="${WEB_PROPERTIES['project.sitePrefix']}/cite.shtml">
Citation
</a>
&nbsp;|&nbsp;
<a href="${WEB_PROPERTIES['project.sitePrefix']}/software.shtml">
Software
</a>
&nbsp;|&nbsp;
<html:link action="/login">
Login
</html:link>
</font>

<br/>

<div><fmt:message key="funding"/></div>
            
</div>

<!-- /footer.jsp -->
