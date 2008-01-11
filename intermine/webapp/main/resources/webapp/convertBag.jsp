<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- convertBag.jsp -->

<div>
    <c:forEach items="${conversionTypes}" var="type">
      <script type="text/javascript" charset="utf-8">
        getConvertCountForBag('${bag.name}','${type}');
      </script>
      <html:link action="/modifyBagDetailsAction.do?convert=${type}&bagName=${bag.name}">${type}</html:link>&nbsp;&nbsp;<span id="${type}_convertcount">&nbsp;</span><br>
    </c:forEach>
    <hr/>
    <c:forEach items="${customConverters}" var="converterInfo">
    <c:out value="${converterInfo.key}:" /><br>
    <html:select property="extraFieldValue" styleId="extraConstraintSelect" disabled="false" >
   	   <c:forEach items="${converterInfo.value}" var="value">
         <html:option value="${value}">${value}</html:option>
       </c:forEach>
        <html:submit property="convertToThing">
			Convert >>
       </html:submit>
    </html:select>
    </c:forEach>
</div>

<!-- /convertBag.jsp -->