<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- geneLong.jsp -->
<fmt:setBundle basename="model"/>

<html:link action="/objectDetails?id=${object.organism.id}">
  <c:out value="${object.organism.name}"/>
</html:link>
<fmt:message key="gene.gene"/>
<c:if test="${!empty object.name}">
  <c:out value="${object.name}"/>
</c:if>
<br/>

<c:if test="${!empty object.seqlen}">
  <fmt:message key="gene.sequencelength"/>: <c:out value="${object.seqlen}"/><br/>
</c:if>

<c:if test="${!empty object.transcripts}">
  <fmt:message key="gene.transcipts"/>:
  <c:forEach items="${object.transcripts}" var="thisTranscript">
    <html:link action="/objectDetails?id=${object.id}&field=${fieldDescriptor.name}">
      <c:out value="${thisTranscript}"/>
    </html:link>
  </c:forEach>
</c:if>

<c:if test="${!empty object.synonyms}">
  <br/>
  <fmt:message key="gene.synonyms"/>:<br/>
  <ul>
  <c:forEach items="${object.synonyms}" var="thisSynonym">
    <c:set var="sourceTitle" value="${thisSynonym.source.title}"/>
    <c:set var="linkProperty" value="${sourceTitle}.${object.organism.genus}.${object.organism.species}.url.prefix"/>
    <li>
      <html:img src="model/${sourceTitle}_logo_small.png"/>
      <html:link href="${WEB_PROPERTIES[linkProperty]}${thisSynonym.synonym}"
                 title="${sourceTitle}: ${thisSynonym.synonym}"
                 target="view_window">
        <c:out value="${thisSynonym.synonym}"/>
      </html:link>
    </li>
  </c:forEach>
  </ul>
</c:if>
<!--
- add these:
wormBaseSequenceName: <c:out value="${object.wormBaseSequenceName}"/><br/>
regulatoryRegions: <c:out value="${object.regulatoryRegions}"/><br/>
transcripts: <c:out value="${object.transcripts}"/><br/>
nonTranscribedRegions: <c:out value="${object.nonTranscribedRegions}"/><br/>
gcgApprovedName: <c:out value="${object.gcgApprovedName}"/><br/>
pubs: <c:out value="${object.pubs}"/><br/>
feature_dbxrefs: <c:out value="${object.feature_dbxrefs}"/><br/>
uniquename: <c:out value="${object.uniquename}"/><br/>
residues: <c:out value="${object.residues}"/><br/>
objects: <c:out value="${object.objects}"/><br/>
evidence: <c:out value="${object.evidence}"/><br/>
subjects: <c:out value="${object.subjects}"/><br/>
properties: <c:out value="${object.properties}"/><br/>
-->
<!-- /geneLong.jsp -->
