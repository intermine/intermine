<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-bean-el.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- oneField.jsp -->
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
              <nobr>
                <c:out value="${fieldDescriptor.name}"/>:
                <c:out value="${object[fieldDescriptor.name]}" default="null"/>
              </nobr>
            </div>
          </c:if>
        </c:when>

        <c:when test="${fieldDescriptor.reference}">
          <c:if test="${object[fieldDescriptor.name] != null}">
            <div>
              <nobr>
                <c:out value="${fieldDescriptor.name}"/>:
                <html:link action="/objectDetails?id=${object.id}&field=${fieldDescriptor.name}">
                  <c:out value="${fieldDescriptor.referencedClassDescriptor.unqualifiedName}"/>
                </html:link>
              </nobr>
            </div>
          </c:if>
        </c:when>

        <c:when test="${fieldDescriptor.collection}">
          <bean:size collection="${object[fieldDescriptor.name]}" id="listSize"/>
          <c:if test="${listSize > 0}">
            <div>
              <nobr>
                <c:out value="${fieldDescriptor.name}"/>:
                <html:link action="/collectionDetails?id=${object.id}&field=${fieldDescriptor.name}">
                  <c:out value="${fieldDescriptor.referencedClassDescriptor.unqualifiedName}[${listSize}]"/>
                </html:link>
              </nobr>
            </div>
          </c:if>
        </c:when>
      </c:choose>
    </c:otherwise>
  </c:choose>
</div>
<!-- /oneField.jsp -->
