<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- convertBag.jsp -->
<tiles:importAttribute />
    <c:forEach items="${conversionTypes}" var="type">
      <script type="text/javascript" charset="utf-8">
        getConvertCountForBag('${bag.name}','${type}','${idname}');
      </script>
      <c:set var="nameForURL"/>
      <str:encodeUrl var="nameForURL">${bag.name}</str:encodeUrl>
      <html:link action="/modifyBagDetailsAction.do?convert=${type}&bagName=${nameForURL}">${type}</html:link>&nbsp;&nbsp;<span id="${type}_convertcount_${idname}">&nbsp;</span><br>
    </c:forEach>
    <c:if test="${orientation!='h'}">
      <hr/>
    </c:if>
    <c:if test="${orientation=='h'}">
     <c:forEach items="${customConverters}" var="converterInfo">
    <h3 class="goog"><%--<c:out value="${converterInfo.key}" />--%>Orthologues</h3>
    <p>
    <html:select property="extraFieldValue" styleId="extraConstraintSelect" >
        <c:forEach items="${converterInfo.value}" var="value">
         <html:option value="${value}">${value}</html:option>
       </c:forEach>
    </html:select>
  &nbsp;
    <html:submit property="convertToThing">Convert</html:submit>
    </p>
    </c:forEach>
    </c:if>

<c:if test="${empty conversionTypes}">
    <div><i><fmt:message key="convert.noresults"/></i></div>
</c:if>

<!-- /convertBag.jsp -->
