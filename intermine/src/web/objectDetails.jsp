<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- objectDetails.jsp -->
Details for
<c:forEach items="${object.clds}" var="cld">
  <b>${cld.unqualifiedName}</b>
</c:forEach>
object<br/><br/>

<c:if test="${!empty object.identifiers}">
  Identifiers:<br/>
  <table style="padding-left: 20px" cellspacing="10">
    <c:forEach items="${object.identifiers}" var="entry">
      <tr>
        <td valign="top"><b>${entry.key}</b></td>
        <td>${entry.value}</td>
      </tr>
    </c:forEach>
  </table>
<br/>
</c:if>

<c:if test="${!empty object.attributes}">
  Attributes:<br/>
  <table style="padding-left: 20px" cellspacing="10">
    <c:forEach items="${object.attributes}" var="entry">
      <tr>
        <td valign="top"><b>${entry.key}</b></td>
        <td>
          <c:set var="maxLength" value="32"/>
          <c:choose>
            <c:when test="${entry.value.class.name == 'java.lang.String' && fn:length(entry.value) > maxLength}">
              ${fn:substring(entry.value, 0, maxLength)}
              <html:link action="/getAttributeAsFile?object=${object.id}&field=${entry.key}">...</html:link>
            </c:when>
            <c:otherwise>
              ${entry.value}
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:forEach>
  </table>
  <br/>
</c:if>

<c:if test="${!empty object.references}">
  References:<br/>
  <table style="padding-left: 20px" cellspacing="10">
    <c:forEach items="${object.references}" var="entry">
      <tr>
        <td valign="top"><html:link href="#${entry.key}"/><b>${entry.key}</b></td>
        <td>
          <c:set var="reference" value="${entry.value}"/>
          <c:choose>
            <c:when test="${reference.verbose}">
              <html:link action="/modifyDetails?method=unverbosify&field=${entry.key}">
                <img border="0" src="images/minus.png" alt="-"/>
              </html:link>
            </c:when>
            <c:otherwise>
              <html:link action="/modifyDetails?method=verbosify&field=${entry.key}" anchor="${entry.key}">
                <img border="0" src="images/plus.png" alt="+"/>
              </html:link>
            </c:otherwise>
          </c:choose>
          <c:forEach items="${reference.clds}" var="cld">
            ${cld.unqualifiedName}
          </c:forEach>
          object
          [<html:link action="/objectDetails?id=${reference.id}">
             details...
           </html:link>]<br/>
          <c:if test="${reference.verbose}">
            <c:forEach items="${reference.identifiers}" var="entry">
              &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${entry.key}=${entry.value}<br/>
            </c:forEach>
          </c:if>
        </td>
      </tr>
    </c:forEach>
  </table>
  <br/>
</c:if>

<c:if test="${!empty object.collections}">
  Collections:<br/>
  <table style="padding-left: 20px" cellspacing="10">
    <c:forEach items="${object.collections}" var="entry">
      <c:set var="collection" value="${entry.value}"/>
      <tr>
        <td valign="top"><b>${entry.key}</b></td>
        <td>
          <c:choose>
            <c:when test="${collection.verbose}">
              <html:link action="/modifyDetails?method=unverbosify&field=${entry.key}">
                <img border="0" src="images/minus.png" alt="-"/>
              </html:link>
            </c:when>
            <c:when test="${collection.size > 0}">
              <html:link action="/modifyDetails?method=verbosify&field=${entry.key}">
                <img border="0" src="images/plus.png" alt="+"/>
              </html:link>
            </c:when>
            <c:otherwise>
              <img border="0" src="images/blank.png" alt=" "/>
            </c:otherwise>
          </c:choose>
          ${collection.size} ${collection.cld.unqualifiedName} object(s)
          <c:if test="${collection.size > 0}">
            [<html:link action="/collectionDetails?id=${object.id}&field=${entry.key}">
            view all
            </html:link>]<br/>
          </c:if>
        </td>
      </tr>
      <c:if test="${collection.verbose}">
        <tr>
          <td/>
          <td>
            <table class="results" cellspacing="0">
              <tr>
                <th/>
                <c:forEach items="${collection.table.columnNames}" var="columnName">
                  <th>${columnName}</th>
                </c:forEach>
              </tr>
              <c:forEach items="${collection.table.rows}" var="row" varStatus="status">
                <tr>
                  <td>
                    <c:forEach items="${collection.table.types[status.index]}" var="cld">
                      ${cld.unqualifiedName}
                    </c:forEach>
                  </td>
                  <c:forEach items="${row}" var="obj">
                    <td>${obj}</td>
                  </c:forEach>
                </tr>
              </c:forEach>
            </table>
          </td>
        </tr>
      </c:if>
    </c:forEach>
  </table>
  <br/>
</c:if>

<c:forEach items="${object.clds}" var="cld">
  <c:if test="${fn:length(DISPLAYERS[cld.name].longDisplayers) > 0}">
    ${cld.unqualifiedName} displayers:<br/>
    <table style="padding-left: 20px" cellspacing="10">
      <c:forEach items="${DISPLAYERS[cld.name].longDisplayers}" var="displayer">
        <tr>
          <td>
            <c:set var="object_bak" value="${object}"/>
            <c:set var="object" value="${object.object}" scope="request"/>
            <c:set var="cld" value="${cld}" scope="request"/>
            <tiles:insert beanName="displayer" beanProperty="src"/><br/>
            <c:set var="object" value="${object_bak}"/>
          </td>
        </tr>
      </c:forEach>
    </table>
  </c:if>
</c:forEach>
<br/>

<c:if test="${RESULTS_TABLE != null && RESULTS_TABLE.size > 0}">
  <html:link action="/changeResults?method=reset">
    <fmt:message key="results.return"/>
  </html:link>
</c:if>

<!-- /objectDetails.jsp -->
