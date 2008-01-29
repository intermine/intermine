<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- convertBag.jsp -->
<tiles:importAttribute />

    <c:if test="${orientation == 'h'}">
    </c:if>
    <c:forEach items="${conversionTypes}" var="type">
      <script type="text/javascript" charset="utf-8">
        getConvertCountForBag('${bag.name}','${type}','${idname}');
      </script>
      <html:link action="/modifyBagDetailsAction.do?convert=${type}&bagName=${bag.name}">${type}</html:link>&nbsp;&nbsp;<span id="${type}_convertcount_${idname}">&nbsp;</span><br>
    </c:forEach>
    <c:choose>
      <c:when test="${orientation == 'h'}">

      </c:when>
      <c:otherwise>
        <hr/>
      </c:otherwise>
    </c:choose>
    <c:forEach items="${customConverters}" var="converterInfo">
    <h3><c:out value="${converterInfo.key}" /></h3>
    <html:select property="extraFieldValue" styleId="extraConstraintSelect" disabled="false" >
        <c:forEach items="${converterInfo.value}" var="value">
         <html:option value="${value}">${value}</html:option>
       </c:forEach>
        <html:submit property="convertToThing">
      Convert >>
       </html:submit>
    </html:select>
    </c:forEach>
    <c:if test="${orientation == 'h'}">
    </c:if>

<!-- /convertBag.jsp -->