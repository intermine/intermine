<!-- geoFormatter.jsp -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<%-- A formatter for a double value to degrees-minutes-seconds. If this is a
longitude or latitude, replace the minus sign with a N/S/E/W suffix. Otherwise, convert
to a signed degree-minute-seconds. --%>


<%-- check the  value --%>
<tiles:importAttribute name="expr" ignore="false" />
<im:eval evalExpression="interMineObject.${expr}" evalVariable="theValue" />
<c:choose>
  <c:when test="${empty theValue}">
  <%-- no value passed. show a space --%>
    &nbsp;
  </c:when>
  <c:otherwise>
    <c:choose>
      <c:when test="${expr == 'longitude'}" >
       <%-- got a longitude --%>
        <c:set var="prefix" value="" />
        <im:instanceof instanceofObject="${interMineObject}"
            instanceofClass="org.intermine.model.bio.Source"
            instanceofVariable="longitude"/>
        <c:if test="${interMineObject.longitude > 0}" >
          <c:set var="latlong" value = "${interMineObject.longitude}" />
          <c:set var="suffix" value="E" />
        </c:if>
        <c:if test="${interMineObject.longitude < 0}" >
          <c:set var="latlong" value = "${-interMineObject.longitude}" />
          <c:set var="suffix" value="W" />
        </c:if>
      </c:when>
      <c:when test="${expr == 'latitude'}" >
       <%-- got a latitude --%>
        <c:set var="prefix" value="" />
        <im:instanceof instanceofObject="${interMineObject}"
            instanceofClass="org.intermine.model.bio.Source"
            instanceofVariable="latitude"/>
        <c:if test="${interMineObject.latitude > 0}" >
          <c:set var="latlong" value = "${interMineObject.latitude}" />
          <c:set var="suffix" value="N" />
        </c:if>
        <c:if test="${interMineObject.latitude < 0}" >
          <c:set var="latlong" value = "${-interMineObject.latitude}" />
          <c:set var="suffix" value="S" />
        </c:if>
      </c:when>
      <c:otherwise>
       <%-- neither a longitude nor latitude. Format as straight degree-minutes-second with sign. --%>
        <c:set var="suffix" value="" />
        <c:if test="${theValue < 0}" >
          <c:set var="prefix" value="-" />
          <c:set var="latlong" value="${-theValue}" />
        </c:if>
        <c:if test="${theValue >= 0}" >
          <c:set var="prefix" value="" />
          <c:set var="latlong" value="${theValue}" />
        </c:if>
      </c:otherwise>
    </c:choose>
  </c:otherwise>
</c:choose>

<c:set var="degrees"    value = "${latlong - latlong%1}" />
<c:set var="minutesecs" value = "${60*(latlong-degrees)}" />
<c:set var="minutes"    value = "${minutesecs - minutesecs%1}" />
<c:set var="seconds"    value = "${60*(minutesecs%1)}" />

${prefix}<fmt:formatNumber maxFractionDigits="0" minIntegerDigits="1" maxIntegerDigits="3"
value="${degrees}" />&deg;
<fmt:formatNumber maxFractionDigits="0" minIntegerDigits="1" maxIntegerDigits="2"
value="${minutes}" />&prime;
<fmt:formatNumber maxFractionDigits="2" minIntegerDigits="1" maxIntegerDigits="2"
value="${seconds}" />&Prime; <c:out value="${suffix}" />

<!-- /geoFormatter.jsp -->
