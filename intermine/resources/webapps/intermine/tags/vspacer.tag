<%@ tag body-content="empty" %>
<%@ attribute name="width" type="java.lang.Integer" required="false" %>
<%@ attribute name="height" type="java.lang.Integer" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- default gap is 10x10 pixels --%>

<c:if test="${empty width}">
  <c:set var="width" value="1"/>
</c:if>
<c:if test="${empty height}">
  <c:set var="height" value="1"/>
</c:if>
<img src="images/blank.gif" border="0" height="${height}" width="${width}" alt=" "/><br/>