<%@ tag body-content="empty" %>
<%@ attribute name="text" required="false" rtexprvalue="true"%>
<%@ attribute name="type" required="false" %>
<%@ attribute name="key" required="false" %>
<%@ attribute name="big" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<%-- outputs a superscript question mark which, when clicked shows some context help --%>

<c:if test="${!empty key}">
  <fmt:message var="text" key="${key}"/>
</c:if>

<c:if test="${!empty text}">
  <c:choose>
    <c:when test="${!empty big}">
      <im:help text="${text}">
        <img src="images/icons/information.png" alt="?">
      </im:help>
    </c:when>
    <c:otherwise>
      <im:help text="${text}">
        <img class="tinyQuestionMark" style="padding-bottom:4px;" src="images/icons/information-small-blue.png" alt="?">
      </im:help>
    </c:otherwise>
  </c:choose>
</c:if>