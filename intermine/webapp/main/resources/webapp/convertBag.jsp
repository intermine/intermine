<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- convertBag.jsp -->
<tiles:importAttribute />

    <c:forEach items="${conversionTypes}" var="type">
      <script type="text/javascript" charset="utf-8">
        getConvertCountForBag('${bag.name}','${type}','${idname}');
      </script>
      <html:link action="/modifyBagDetailsAction.do?convert=${type}&bagName=${bag.name}">${type}</html:link>&nbsp;&nbsp;<span id="${type}_convertcount_${idname}">&nbsp;</span><br>
    </c:forEach>
    <c:if test="${orientation!='h'}">
      <hr/>
    </c:if>
    <c:if test="${orientation=='h'}">
     <c:forEach items="${customConverters}" var="converterInfo">
    <h3><%--<c:out value="${converterInfo.key}" />--%>Orthologues</h3>
    <p>
    <html:select property="extraFieldValue" styleId="extraConstraintSelect" >
        <c:forEach items="${converterInfo.value}" var="value">
         <html:option value="${value}">${value}</html:option>
       </c:forEach>
    </html:select>
    <html:submit property="convertToThing">Convert</html:submit>
    </p>
    </c:forEach> 
    </c:if>

<!-- /convertBag.jsp -->