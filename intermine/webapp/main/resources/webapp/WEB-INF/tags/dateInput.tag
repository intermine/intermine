<%@ tag body-content="empty" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%@ attribute name="attributeType" required="true" rtexprvalue="true" %>
<%@ attribute name="property" required="true" rtexprvalue="true" %>
<%@ attribute name="styleId" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="onkeypress" required="false" rtexprvalue="true" %>
<%@ attribute name="visible" required="false" rtexprvalue="true" %>

<c:set var="datePickerClass" value="" />
<c:if test="${attributeType == 'Date'}">
    <c:set var="datePickerClass" value="date-pick" />
</c:if>
<c:set var="inputDisplay" value="display:none;"/>
 <c:if test="${visible}">
    <c:set var="inputDisplay" value="display:inline;"/>
 </c:if>
<html:text property="${property}" styleId="${styleId}" value="${value}"
    onkeypress="${onkeypress}"
    styleClass="${datePickerClass}"
    style="${inputDisplay}" />
    <c:if test="${attributeType == 'Date'}">
    <script type="text/javascript">
    jQuery('.date-pick').datepicker(
       {
       buttonImage: 'images/calendar.png',
       buttonImageOnly: true,
       dateFormat: 'yy-mm-dd',
       showOn: "both",
       showAnim: 'blind',
       showOptions: {speed: 'fast'}
       }
    );
    </script>
</c:if>
