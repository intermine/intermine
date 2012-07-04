<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- footer.jsp -->

<br/><br/><br/>

<div class="body" align="center" style="clear:both">

      <c:if test="${pageName != 'contact'}">
        <div id="contactFormDivButton">
          <im:vspacer height="11"/>
          <div class="contactButton">
             <a href="#" onclick="showContactForm();return false">
               <b><fmt:message key="feedback.title"/></b>
             </a>
          </div>
        </div>

      <div id="contactFormDiv" style="display:none;">
            <im:vspacer height="11"/>
              <tiles:get name="contactForm"/>
        </div>
      </c:if>

<br/>
<div id="funding-footer">
    <fmt:message key="funding"/>
    <br/><br/>
    <p>Powered by</p>
    <a target="new" href="http://intermine.org" title="InterMine">
        <img src="images/icons/intermine-footer-logo.png" alt="InterMine logo" />
    </a>
</div>

</div>

<!-- /footer.jsp -->
