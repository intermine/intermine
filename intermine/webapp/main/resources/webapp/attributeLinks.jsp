<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- attributeLinkDisplayer.jsp -->

  <h3 class="goog">External Links</h3>
  <c:choose>
  <c:when test="${!empty attributeLinkConfiguration}">
  <ul>
    <c:forEach var="confMapEntry" items="${attributeLinkConfiguration}">
      <c:set var="href" value="${confMapEntry.value.url}" />
      <c:set var="imageName" value="${confMapEntry.value.imageName}" />
      <c:set var="text" value="${confMapEntry.value.text}" />
      <c:set var="parameters" value="${confMapEntry.value.parameters}" />
      <c:set var="usePost" value="${confMapEntry.value.usePost}" />
      <c:set var="linkId" value="${confMapEntry.value.linkId}" />
      <c:set var="enctype" value="${confMapEntry.value.enctype}" />

      <c:if
        test="${!empty confMapEntry.value.valid && !empty confMapEntry.value.attributeValue}">
        <li class="external"><c:choose>

          <%-- GET form --%>
          <c:when test="${empty usePost}">
            <c:if test="${!empty text}">
              <a href="${href}" class="ext_link" target="_new">${text}</a>
            </c:if>
          </c:when>

          <%-- POST form --%>
          <c:otherwise>
            <c:if test="${!empty text}">
              <a
                href="javascript:document.getElementById('${linkId}Form').submit();"
                class="ext_link">${text}</a>
            </c:if>

            <form action="${href}" method="post" id="${linkId}Form"
              target="_blank" enctype="${enctype}"><c:forEach var="par"
              items="${parameters}">
              <input type="hidden" value="${par.value}" name="${par.key}" />
            </c:forEach></form>
          </c:otherwise>
        </c:choose></li>
      </c:if>
    </c:forEach>
  </ul>
  </c:when>
  <c:otherwise>
  <p class="no-external-links"><i>No external links.</i></p>
  </c:otherwise>
  </c:choose>

<%-- show xrefs --%>
<c:if test="${reportObject.refsAndCollections.count > 0}">
    <h3>Lookup Report</h3>
    <ul>
      <c:forEach var="xrefCol" items="${reportObject.refsAndCollections}">
        <c:if test='${(xrefCol.key == "crossReferences") && (xrefCol.value.size > 0)}'>
         <c:forEach var="xref" items="${xrefCol.value.table.resultsAsList}">
           <c:forEach var="xrefMapItem" items="${xrefMap}">
             <c:if test="${xrefMapItem.key == xref.source.name}">
               <li>
                 <a target="_new" href="${xrefMapItem.value.url}${xref.identifier}">${xref.source.name}: ${xref.identifier}
                   <img title="${xref.source.name}: ${xref.identifier}" src="images/ext_link.png">
                 </a>
               </li>
             </c:if>
           </c:forEach>
         </c:forEach>
        </c:if>
      </c:forEach>
    </ul>
</c:if>
<!-- /attributeLinkDisplayer.jsp -->
