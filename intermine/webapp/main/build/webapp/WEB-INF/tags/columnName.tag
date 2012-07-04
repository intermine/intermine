<%@ tag body-content="scriptless"  %>
<%@ attribute name="columnName" required="true" %>
<%@ attribute name="tableId" required="false" %>
<%@ attribute name="colNo" required="false" %>
<%@ attribute name="maxLength" required="false" %>
<%@ attribute name="noHead" required="false" %>
<%@ attribute name="onHover" required="false" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<c:set var="columnDisplayNameList" value="${fn:split(columnName,'>')}"/>
<c:set var="begin" value="0"/>

<im:debug message="${columnName}"/>

<c:if test="${empty maxLength}">
    <c:set var="maxLength" value="3"/>
</c:if>

<c:if test="${fn:length(columnDisplayNameList) > maxLength}">
    <c:if test="${!((fn:length(columnDisplayNameList) == (maxLength + 1)) && noHead)}">
        ...
    </c:if>
    <c:set var="begin" value="${fn:length(columnDisplayNameList) - maxLength}"/>
</c:if>

<c:choose>
    <c:when test="${!empty tableId && !empty colNo}">
        <span title="${onHover}" id="header_${fn:replace(tableId,'.','_')}_${colNo}" style="cursor:default;">
    </c:when>
    <c:otherwise>
        <span title="${onHover}">
    </c:otherwise>
</c:choose>

<em style="font-size:9px;">

<c:forEach items="${columnDisplayNameList}" var="columnNameItem" varStatus="colNameStat" begin="${begin}">
    <c:set var="wouldBePos" value="${colNameStat.index + begin}"/>
    <c:choose>
        <c:when test="${colNameStat.last}">
            </em><c:if test="${fn:length(columnDisplayNameList) > 1}"><br/></c:if>${columnNameItem}
            <c:set var="fieldName" value="${columnNameItem}"/>
        </c:when>
        <c:otherwise>
            <c:if test="${!(wouldBePos == 0 && noHead)}">
                ${columnNameItem}&nbsp;&gt;
            </c:if>
        </c:otherwise>
    </c:choose>
</c:forEach>

</span>
