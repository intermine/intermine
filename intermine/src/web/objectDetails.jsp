<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- objectDetails.jsp -->
Summary for selected
<c:forEach items="${object.clds}" var="cld">
  <b>${cld.unqualifiedName}</b>
</c:forEach>
<br/><br/>


<table rules="all" cellpadding="5">
  <thead style="text-align: center">
    <tr>
      <c:forEach items="${object.keyAttributes}" var="entry">
        <td rowspan="2"><b>${entry.key}</b></td>
      </c:forEach>
      <c:forEach items="${object.keyReferences}" var="entry">
        <td colspan="${fn:length(entry.value.identifiers)}"><html:link action="/objectDetails?id=${entry.value.id}"><b>${entry.key}</b></html:link></td>
      </c:forEach>
    </tr>
    <tr>
      <c:forEach items="${object.keyReferences}" var="entry">
        <c:forEach items="${entry.value.identifiers}" var="entry">
          <td><b>${entry.key}</b></td>
        </c:forEach>
      </c:forEach>
    </tr>
  </thead>
  <tbody>
    <tr>
      <c:forEach items="${object.keyAttributes}" var="entry">
        <td>${entry.value}</td>
      </c:forEach>
      <c:forEach items="${object.keyReferences}" var="entry">
        <c:forEach items="${entry.value.identifiers}" var="entry">
          <td>${entry.value}</td>
        </c:forEach>
      </c:forEach>
    </tr>
  </tbody>
</table>
<br/>

<%--c:if test="${!empty object.identifiers}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.identifiers}" var="entry">
      <c:choose>
        <c:when test="${entry.value.class.name == 'org.intermine.web.results.DisplayReference'}">
          <c:set var="reference" value="${entry.value}"/>
          <html:link action="/objectDetails?id=${reference.id}"><b>${entry.key}</b></html:link>
          <span class="type">
            - <c:forEach items="${reference.clds}" var="cld">${cld.unqualifiedName} </c:forEach>
          </span>
          <br/>
          <div style="margin-left: 20px">
            <c:forEach items="${reference.identifiers}" var="entry">
              <b>${entry.key}</b> ${entry.value}<br/>
            </c:forEach>
          </div>
        </c:when>
        <c:otherwise>
          <b>${entry.key}</b> ${entry.value}<br/>
        </c:otherwise>
      </c:choose>
      <br/>
    </c:forEach>
  </div>
</c:if--%>

Full details:<br/><br/>

<c:if test="${!empty object.attributes}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.attributes}" var="entry">
      <img border="0" src="images/blank.png" alt=" "/><b>${entry.key}</b>
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
    <br/><br/>
    </c:forEach>
  </div>
</c:if>

<c:if test="${!empty object.references}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.references}" var="entry">
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
      <html:link action="/objectDetails?id=${reference.id}"><b>${entry.key}</b></html:link>

      <span class="type">
        - <c:forEach items="${reference.clds}" var="cld">${cld.unqualifiedName} </c:forEach>
      </span>
      <br/>
      <c:if test="${reference.verbose}">
        <div style="margin-left: 20px">
          <c:forEach items="${reference.identifiers}" var="entry">
            <b>${entry.key}</b> ${entry.value}<br/>
          </c:forEach>
        </div>
      </c:if>
      <br/>
    </c:forEach>
  </div>
</c:if>

<c:if test="${!empty object.collections}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.collections}" var="entry">
      <c:set var="collection" value="${entry.value}"/>
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
      <html:link action="/collectionDetails?id=${object.id}&field=${entry.key}"><b>${entry.key}</b></html:link>
      <span class="type">- ${collection.size} ${collection.cld.unqualifiedName} objects</span>
      <c:if test="${collection.verbose}">
        <table rules="all" style="margin-left: 20px" cellpadding="5">
          <thead style="text-align: center">
            <tr>
              <td/>
              <c:forEach items="${collection.table.columnNames}" var="columnName">
                <td><b>${columnName}</b></td>
              </c:forEach>
            </tr>
          </thead>
          <tbody>
            <c:forEach items="${collection.table.rows}" var="row" varStatus="status">
              <tr>
                <td>
                  <html:link action="/objectDetails?id=${collection.table.ids[status.index]}">
                    <c:forEach items="${collection.table.types[status.index]}" var="cld">
                      ${cld.unqualifiedName}
                    </c:forEach>
                  </html:link>
                </td>
                <c:forEach items="${row}" var="obj">
                  <td>${obj}</td>
                </c:forEach>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:if>
      <br/><br/>
    </c:forEach>
  </div>
</c:if>

<c:forEach items="${object.clds}" var="cld">
  <c:if test="${fn:length(DISPLAYERS[cld.name].longDisplayers) > 0}">
    Further information for this <b>${cld.unqualifiedName}</b><br/><br/>
    <div style="margin-left: 20px">
      <c:forEach items="${DISPLAYERS[cld.name].longDisplayers}" var="displayer">
        <c:set var="object_bak" value="${object}"/>
        <c:set var="object" value="${object.object}" scope="request"/>
        <c:set var="cld" value="${cld}" scope="request"/>
        <tiles:insert beanName="displayer" beanProperty="src"/><br/>
        <c:set var="object" value="${object_bak}"/>
      </c:forEach>
    </div>
  </c:if>
</c:forEach>
<br/>

<c:if test="${RESULTS_TABLE != null && RESULTS_TABLE.size > 0}">
  <html:link action="/changeResults?method=reset">
    <fmt:message key="results.return"/>
  </html:link>
</c:if>
<!-- /objectDetails.jsp -->
