<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!--  geneMicroArrayDisplayer.jsp -->
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
<!--  /geneMicroArrayDisplayer.jsp -->
