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

  <c:set var="track" value="${cld.unqualifiedName}s"/>
  
  <c:if test="${cld.unqualifiedName == 'MRNA' || cld.unqualifiedName == 'Transcript'}">
    <c:set var="track" value="Genes"/>
  </c:if>

  <c:set var="track" value="${track}+Genes"/>

  <c:if test="${!empty hasLocation || cld.name == 'org.flymine.model.genomic.Chromosome'}">
    <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};type=${track};name=FlyMineInternalID_${object.id};width=750">
      <div>
        <fmt:message key="locatedSequenceFeature.GBrowse.message"/>
      </div>
    <c:if test="${cld.name != 'org.flymine.model.genomic.Chromosome'}">
      <div>
        <html:img src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?source=${WEB_PROPERTIES['gbrowse.database.source']};type=${track};name=FlyMineInternalID_${object.id};width=400"/>
      </div>
    </c:if>
    </html:link>
  </c:if>
<!-- /locatedSequenceFeatureImage.jsp -->
