<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- geneLong.jsp -->
<c:out value="${object.organism.name}"/> gene 
<c:if test="${!empty object.name}">
  <c:out value="${object.name}"/>
</c:if>
<br/>
<c:if test="${!empty object.seqlen}">
  Sequence length: <c:out value="${object.seqlen}"/><br/>
</c:if>
<c:if test="${!empty object.transcripts}">
  Transcripts:
  <c:forEach items="${object.transcripts}" var="thisTranscript">
    <html:link action="/objectDetails?id=${object.id}&field=${fieldDescriptor.name}">
      <c:out value="${thisTranscript}"/>
    </html:link>
  </c:forEach>
</c:if>
<c:if test="${!empty object.synonyms}">
  Synonyms:
  <c:forEach items="${object.synonyms}" var="thisSynonym">
    <c:if test="${thisSynonym.source.title == 'ensembl'}">
      <html:link href="http://www.ensembl.org/Drosophila_melanogaster/geneview?db=core&gene=${thisSynonym.synonym}" 
                 title="Ensembl: ${thisSynonym.synonym}"
                 target="view_window">
        <html:img src="model/ensembl_logo_small.png"/>
      </html:link>
      <html:link href="http://www.ensembl.org/Drosophila_melanogaster/geneview?db=core&gene=${thisSynonym.synonym}" 
                 title="Ensembl: ${thisSynonym.synonym}"
                 target="view_window">
        <c:out value="${thisSynonym.synonym}"/>
      </html:link>
    </c:if>
    <c:if test="${thisSynonym.source.title == 'flybase'}">
      <html:link href="http://www.flybase.org/.bin/fbidq.html?${thisSynonym.synonym}"
                 title="FlyBase:: ${thisSynonym.synonym}"
                 target="view_window">
        <html:img src="model/flybase_logo_small.png"/>
      </html:link>
      <html:link href="http://www.flybase.org/.bin/fbidq.html?${thisSynonym.synonym}"
                 title="FlyBase: ${thisSynonym.synonym}"
                 target="view_window">
        <c:out value="${thisSynonym.synonym}"/>
      </html:link> 
   </c:if>
  </c:forEach>
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
