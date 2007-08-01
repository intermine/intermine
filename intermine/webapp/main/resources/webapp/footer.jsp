<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- footer.jsp -->

<br/><br/><br/>

<div class="body" align="center">

<div>Help | About | Data | Download | Citation</div>



      <c:if test="${pageName != 'feedback'}">
        <div id="feedbackFormDivButton">
          <im:vspacer height="11"/>
          <div class="feedbackButton">
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
      
      FlyMine is funded by <a href="http://www.wellcome.ac.uk/" target="_new"><img src="images/wellcome_logo.png" height="17px" width="120px" border="0" /></a>
            
</div>

<!-- /footer.jsp -->
