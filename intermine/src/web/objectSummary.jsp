<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<tiles:importAttribute/>

<!-- objectSummary.jsp -->
<div class="objectSummary">
  <c:choose>
    <c:when test="${empty leafClds}">
      <c:out value="${object}" default="null"/>
    </c:when>
    <c:otherwise>
      <nobr>
        <html:link action="/objectDetails?id=${object.id}">
          <font class="resultsCellTitle">
            <c:forEach var="cld" items="${leafClds}">
              <c:out value="${cld.unqualifiedName}"/>
            </c:forEach>
          </font>
        </html:link>
      </nobr>
      <br/>
      <c:forEach var="cld" items="${leafClds}">
        <c:choose>
          <c:when test="${!empty webconfig.types[cld.name].shortDisplayers}">
            <c:forEach items="${webconfig.types[cld.name].shortDisplayers}" var="displayer">
              <c:set var="cld" value="${cld}" scope="request"/>
              <tiles:insert beanName="displayer" beanProperty="src"/>
            </c:forEach>
          </c:when>
          <c:otherwise>
            <c:set var="cld" value="${cld}" scope="request"/>
            <c:choose>
              <c:when test="${!empty primaryKeyFields}"> 
                <c:forEach items="${cld.allFieldDescriptors}" var="fieldDescriptor">
                  <c:set var="fieldName" value="${fieldDescriptor.name}"/>
                  <c:if test="${primaryKeyFields[fieldName] == fieldName}">
                    <div>     
                      <c:set var="fieldDescriptor" value="${fieldDescriptor}" scope="request"/>
                      <tiles:insert name="/oneField.jsp"/>
                    </div>
                  </c:if>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <tiles:insert name="/allFields.jsp"/>
              </c:otherwise>
            </c:choose>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</div>
<!-- /objectSummary.jsp -->
