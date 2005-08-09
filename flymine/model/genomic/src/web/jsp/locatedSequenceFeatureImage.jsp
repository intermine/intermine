<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- locatedSequenceFeatureImage.jsp -->
<fmt:setBundle basename="model"/>

<%-- hacky fix: some things aren't located yet (eg. genes from Uniprot) so we
     check that the object has a location before linking --%>
<c:forEach items="${object.objects}" var="thisRelation">
  
  <c:if test="${fn:startsWith(thisRelation.class.name, 'org.flymine.model.genomic.Location')}">
    <c:set var="hasLocation" value="true"/>
  </c:if>
</c:forEach>

<%-- This should be changed to check chromosomeLocation --%>

<c:if test="${!empty hasLocation || cld.unqualifiedName == 'Chromosome'}">
  <c:set var="type" value="${cld.unqualifiedName}s"/>
  
  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript' 
              || cld.unqualifiedName == 'Pseudogene'}">
    <c:set var="type" value="Genes"/>
  </c:if>
  
  <c:set var="label" value="${type}"/>
  
  <c:if test="${type != 'Genes'}">
    <c:set var="type" value="${type}+Genes"/>
    <c:set var="label" value="${label}-Genes"/>
  </c:if>
  
  <c:if test="${type != 'Pseudogenes+Genes'}">
    <c:set var="type" value="${type}+Pseudogenes"/>
    <c:set var="label" value="${label}-Pseudogenes"/>
  </c:if>
  
  <c:set var="name" value="FlyMineInternalID_${object.id}"/>

  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'}">
    <c:set var="name" value="mRNA:${name}"/>
  </c:if>

  <c:if test="${cld.unqualifiedName == 'Chromosome'}">
    <c:set var="name" value="${object.organism.genus}_${object.organism.species}_chr_${object.identifier}"/>
  </c:if>

  <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};label=${label};name=${name};width=750">
    <div>
      <fmt:message key="locatedSequenceFeature.GBrowse.message"/>
    </div>
    <div>
      <html:img src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};type=${type};name=${name};width=400"/>
    </div>
  </html:link>
</c:if>
<!-- /locatedSequenceFeatureImage.jsp -->
