<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!-- footer.jsp -->

<br/><br/><br/>

<div class="body" align="center" style="clear:both">



      <c:if test="${pageName != 'contact'}">
        <div id="contactFormDivButton">
          <im:vspacer height="11"/>
          <div class="contactButton">
             <a href="#" onclick="showContactForm();return false">
               <b>${WEB_PROPERTIES["contact.title"]}</b>
             </a>
          </div>
        </div>
      
      <div id="contactFormDiv" style="display:none;">
            <im:vspacer height="11"/>
              <tiles:get name="contactForm"/>
        </div>
      </c:if>
           
<br/>


<div>${WEB_PROPERTIES["funding"]}</div>
            
</div>

<!-- /footer.jsp -->
