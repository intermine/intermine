<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- locatedSequenceFeatureImage.jsp -->
<fmt:setBundle basename="model"/>

<c:if test="${object.organism.name == 'Anopheles gambiae' ||
            object.organism.name == 'Drosophila melanogaster'}">

  <%-- hacky fix: some things aren't located yet (eg. genes from Uniprot) so we
       check that the object has a location before linking --%>
  <c:forEach items="${object.objects}" var="thisRelation">

    <c:if test="${fn:startsWith(thisRelation.class.name, 'org.flymine.model.genomic.Location')}">
      <c:set var="hasLocation" value="true"/>
    </c:if>
  </c:forEach>

  <c:if test="${!empty hasLocation && cld.name != 'org.flymine.model.genomic.Chromosome'}">
    <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?name=${cld.unqualifiedName}:FlyMineInternalID_${object.id};width=750">
      <div>
        <fmt:message key="locatedSequenceFeature.GBrowse.message"/>
      </div>
      <div>
        <html:img src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?name=${cld.unqualifiedName}:FlyMineInternalID_${object.id};width=400"/>
      </div>
    </html:link>
  </c:if>
</c:if>
<!-- /locatedSequenceFeatureImage.jsp -->
