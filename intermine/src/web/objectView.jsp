<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- objectView.jsp -->
<div class="objectView">
  <c:choose>
    <c:when test="${empty leafClds}">
      <fmt:message key="objectDetails.nullField" var="nullFieldText"/>
      <c:out value="${object}" default="${nullFieldText}"/>
    </c:when>
    <c:otherwise>
      <nobr>
        <html:link action="/objectDetails?id=${object.id}">
          <c:forEach var="cld" items="${leafClds}">
            <c:out value="${cld.unqualifiedName}"/>
          </c:forEach>
        </html:link>
      </nobr>
      <br/><br/>
      <c:forEach var="cld" items="${leafClds}">
        <c:set var="cld" value="${cld}" scope="request"/>
        <c:set var="fieldDescriptor" value="${fieldDescriptor}" scope="request"/>
        <c:set var="object" value="${object}" scope="request"/>
        <c:set var="primaryKeyFields" value="${primaryKeyFields}" scope="request"/>
        <c:choose>

          <c:when test="${viewType == 'summary' &&
                        !empty DISPLAYERS[cld.name].shortDisplayers}">
            <c:forEach items="${DISPLAYERS[cld.name].shortDisplayers}" var="displayer">
              <tiles:insert beanName="displayer" beanProperty="src"/>
            </c:forEach>
          </c:when>

          <c:when test="${viewType == 'detail' &&
                        !empty DISPLAYERS[cld.name].longDisplayers}">
            <c:forEach items="${DISPLAYERS[cld.name].longDisplayers}" var="displayer">
              <tiles:insert beanName="displayer" beanProperty="src"/>
            </c:forEach>
          </c:when>

          <c:otherwise>
            <tiles:insert name="/objectFields.jsp"/>
          </c:otherwise>
          
        </c:choose>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</div>
<!-- /objectView.jsp -->
