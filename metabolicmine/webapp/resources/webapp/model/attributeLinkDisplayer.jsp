<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- attributeLinkDisplayer.jsp -->

<div class="externalLinks">
  <h3>External links</h3>
    <ul>

    <c:forEach var="confMapEntry" items="${attributeLinkConfiguration}">
    <li>
    <c:set var="href" value="${confMapEntry.value.url}"/>
    <c:set var="imageName" value="${confMapEntry.value.imageName}"/>
    <c:set var="text" value="${confMapEntry.value.text}"/>
    <c:set var="title" value="${confMapEntry.value.title}"/>
    <c:set var="foot" value="${confMapEntry.value.foot}"/>
    <c:set var="parameters" value="${confMapEntry.value.parameters}"/>
    <c:set var="usePost" value="${confMapEntry.value.usePost}"/>
    <c:set var="linkId" value="${confMapEntry.value.linkId}"/>
    <c:set var="enctype" value="${confMapEntry.value.enctype}"/>

    <c:if test="${!empty confMapEntry.value.valid && !empty confMapEntry.value.attributeValue}">
        <c:choose>

          <%-- GET form --%>
          <c:when test="${empty usePost}">
            <c:if test="${!empty imageName}">
              <a href="${href}" class="ext_link image" target="_new">
                <center><html:img src="model/images/${imageName}" title="${text}"/><br /></center>
                <span>${title}</span>
              </a>
            </c:if>
            <a href="${href}" class="ext_link" target="_new">${text}</a>
          </c:when>

          <%-- POST form --%>
          <c:otherwise>
                <td align="right">
                  <c:if test="${!empty imageName}">
                    <a href="javascript:document.getElementById('${linkId}Form').submit();" class="ext_link" >
                             <html:img src="model/images/${imageName}" title="${text}" />
                    </a>
                  </c:if>
              </td>
                <td>
                  <c:if test="${!empty text}">
                    <a href="javascript:document.getElementById('${linkId}Form').submit();" class="ext_link" >
                          ${text}&nbsp;<img src="images/ext_link.png" title="${text}"/>
                    </a>
                  </c:if>
                </td>

                <form action="${href}" method="post" id="${linkId}Form" target="_blank" enctype="${enctype}">
                     <c:forEach var="par" items="${parameters}">
                         <input type="hidden" value="${par.value}" name="${par.key}" />
                     </c:forEach>
                </form>
          </c:otherwise>
        </c:choose>
    </c:if>
    </li>
  </c:forEach>
  </ul>
</div>
<div id="externalLinksClear"></div>

<%-- show xrefs --%>
<table id="xrefTable" class="lookupReport" cellspacing="5" cellpadding="0">
  <c:forEach var="xrefCol" items="${reportObject.refsAndCollections}">
    <c:if test='${(xrefCol.key == "crossReferences") && (xrefCol.value.size > 0)}'>
     <c:forEach var="xref" items="${xrefCol.value.table.resultsAsList}">
       <c:forEach var="xrefMapItem" items="${xrefMap}">
         <c:if test="${xrefMapItem.key == xref.source.name}">
           <tr>
            <td>
              <a target="_new" href="${xrefMapItem.value.url}${xref.identifier}">${xref.source.name}: ${xref.identifier}
                <img title="${xref.source.name}: ${xref.identifier}" src="images/ext_link.png">
              </a>
            </td>
           </tr>
         </c:if>
       </c:forEach>
     </c:forEach>
    </c:if>
  </c:forEach>
</table>
<!-- /attributeLinkDisplayer.jsp -->
