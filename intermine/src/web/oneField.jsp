<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- oneField.jsp -->
<html:xhtml/>
<div class="oneField">
  <c:set var="key" value="${cld.name} ${fieldDescriptor.name}"/>

  <c:choose>
    <c:when test="${!empty DISPLAYERS[key].longDisplayers}">
      <c:forEach items="${DISPLAYERS[key].longDisplayers}" var="displayer">
        <c:set var="cld" value="${cld}" scope="request"/>
        <tiles:insert beanName="displayer" beanProperty="src"/>
      </c:forEach>
    </c:when>
    <c:otherwise>

      <c:choose>
        <c:when test="${fieldDescriptor.attribute}">
          <c:if test="${fieldDescriptor.name != 'id'}">
            <div>
              <fmt:message key="objectDetails.nullField" var="nullFieldText"/>
              <span class="attributeField"><c:out value="${fieldDescriptor.name}"/></span>:
              <span class="value">
                <c:out value="${object[fieldDescriptor.name]}" default="${nullFieldText}"/>
              </span>
            </div>
          </c:if>
        </c:when>

        <c:when test="${fieldDescriptor.reference}">
          <c:if test="${object[fieldDescriptor.name] != null}">
            <div>
              <span class="referenceField"><c:out value="${fieldDescriptor.name}"/></span>:
              <html:link action="/objectDetails?id=${object.id}&amp;field=${fieldDescriptor.name}">
                <c:out value="${fieldDescriptor.referencedClassDescriptor.unqualifiedName}"/>
              </html:link>
            </div>
          </c:if>
        </c:when>

        <c:when test="${fieldDescriptor.collection}">
          <c:set var="listSize" value="${fn:length(object[fieldDescriptor.name])}"/>
          <c:if test="${listSize > 0}">
            <div>
              <span class="fieldName"><c:out value="${fieldDescriptor.name}"/></span>:
              <html:link action="/collectionDetails?id=${object.id}&amp;field=${fieldDescriptor.name}">
                <c:out value="${fieldDescriptor.referencedClassDescriptor.unqualifiedName}[${listSize}]"/>
              </html:link>
            </div>
          </c:if>
        </c:when>
      </c:choose>
    </c:otherwise>
  </c:choose>
</div>
<!-- /oneField.jsp -->
