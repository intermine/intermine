<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>


<html:xhtml/>

<div class="body">

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Lab</th>
    <th>Affiliation</th>
    <th>Project</th>
    <th>Submissions</th>
  </tr>
<c:forEach items="${experiments}" var="item">
 <tr><td>
    <html:link  href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>
 <td>${item.key.affiliation}
 <td>
      <c:forEach items="${project}" var="proj">
        <c:if test="${proj.key eq item.key}">
          <c:set var="pName" value="${proj.value.name}" />
          <c:set var="pId" value="${proj.value.id}" />
        </c:if>
      </c:forEach>
     <html:link  href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pId}">
 ${pName}
    </html:link>


 <td>
 <c:forEach items="${item.value}" var="sub">

    <html:link  href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${sub.id}">
 ${sub.title}
    </html:link>
  <br>
</c:forEach>
</c:forEach>


<%-- to access a map directly

${experiments['key']}

 <tr><td> ${item.key}<td>${item.value}

 <tr><td> ${item.key.name}<td>${item.key.affiliation}<td>${item.value.title}

  --%>

</tr>
</table>


<%--
<c:forEach items="${experiments}" var="item">
  <c:if test="${item.identifier == 'E-FLYC-6'}">
  <p>
    <html:link  action="/chartRenderer?method=microarray&amp;gene=${object.primaryIdentifier}&amp;experiment=${item.identifier}&amp;width=800&amp;height=160" target="_new">
      <im:abbreviate value="${item.name}" length="65"/>
    </html:link>
    <c:if test="${item.publication.pubMedId != null}">
      <html:img src="model/images/PubMed_logo_mini.png" title="PubMed" />
      <html:link href="${WEB_PROPERTIES['attributelink.PubMed.Publication.*.pubMedId.url']}${item.publication.pubMedId}" target="_new">
        ${item.publication.pubMedId}
      </html:link><br/>
    </c:if>
    <img src="<html:rewrite action="/chartRenderer?method=microarray&amp;gene=${object.primaryIdentifier}&amp;experiment=${item.identifier}&amp;width=600&amp;height=140"/>" width="600" height="140" title="${object.primaryIdentifier}" />
  </p>
  </c:if>
</c:forEach>
--%>

