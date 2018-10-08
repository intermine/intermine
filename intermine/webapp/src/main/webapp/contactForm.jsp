<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- feedbackForm.jsp -->




<html:xhtml/>
<link rel="stylesheet" type="text/css" href="css/contactForm.css"/>
<div class="body">
  <html:form action="/contactAction">
  <table cellspacing="0" cellpadding="3" border="0">
  <tr>
    <td align="right"><fmt:message key="contact.name"/></td>
    <td>
      <html:text property="name" size="40" styleId="fbname"/>
    </td>
  </tr>
  <tr>
    <td align="right"><fmt:message key="contact.email"/></td>
    <td><span id="monkey"></span></td>
  </tr>
  <tr>
    <td align="right"><fmt:message key="contact.subject"/></td>
    <td><html:text property="subject" size="40"/></td>
  </tr>
  <tr>
    <td align="right" valign="top"><fmt:message key="contact.message"/></td>
    <td><html:textarea property="message" cols="80" rows="10" style="width: 100%" styleId="fbcomment"/></td>
  </tr>
  <tr>
    <td align="center"><html:submit/></td>
  </tr>
  </table>
  </html:form>
</div>

<script language="JavaScript">
<!--
jQuery('#monkey').html('<input type=\"text\" name=\"monkey\" size=\"40\"/>');

// append browser version into the message area
var browser = navigator.userAgent;
jQuery("form#contactForm textarea#fbcomment").val(jQuery("form#contactForm textarea#fbcomment").val() + "\n\n---- Browser: ----\n\n" + browser);

// JS submit handler for the form
jQuery("#contactForm").submit(function(e) {
  // no synchronous beans for you
  e.preventDefault();

  // values dict
  var form = {};
  form["name"] = jQuery(this).find('input[name="name"]').val();
  form["monkey"] = jQuery(this).find('input[name="monkey"]').val();
  form["subject"] = jQuery(this).find('input[name="subject"]').val();
  form["message"] = jQuery(this).find('textarea[name="message"]').val();

  // send the form through AJAX
  jQuery.post(jQuery(this).attr('action'), form,
    function (data) {
      // show the response message
      jQuery("#contactForm").parent().prepend(jQuery(data).find("#contactFormResponse").clone());
      jQuery("#contactFormResponse").show();
      
      // hide the form again (what a mess...)
      if (jQuery("#contactFormResponse #error_msg.topBar.messages").exists()) {
    	  jQuery("form#contactForm").hide();
      }
    }
  );
});
//-->
</script>


<!-- /feedbackForm.jsp -->
