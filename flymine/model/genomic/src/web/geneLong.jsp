<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- geneLong.jsp -->
<c:if test="${!empty object.name}">
  Gene name: <c:out value="${object.name}"/><br/>
</c:if>
<c:if test="${!empty object.seqlen}">
  Sequence length: <c:out value="${object.seqlen}"/><br/>
</c:if>
<c:if test="${!empty object.synonyms}">
  Synonyms: <c:out value="${object.synonyms}"/><br/>
</c:if>
<!-- /geneLong.jsp -->
