<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%--in order to filter out chars from strings --%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>


<!-- reportRefsCols.jsp -->

<html:xhtml />

<tiles:importAttribute name="object" />
<tiles:importAttribute name="placement" />
<tiles:importAttribute name="showTitle" ignore="true" />
<c:if test="${!empty placementRefsAndCollections[placement]}">

  <c:if test="${!empty showTitle && fn:length(placementRefsAndCollections[placement]) > 0}">
    <a name="miscellaneous"><h2>${showTitle}</h2></a>
  </c:if>

  <c:forEach items="${placementRefsAndCollections[placement]}" var="entry">
    <c:set var="collection" value="${entry.value}" />
    <% pageContext.setAttribute("spaceChar", " "); %>
    <c:set var="fieldName" value="${fn:replace(entry.key, spaceChar, '_')}" />
    <c:set var="pathString" value="${object.classDescriptor.unqualifiedName}.${fieldName}"/>
    <c:set var="fieldDisplayName"
        value="${imf:formatFieldStr(pathString, INTERMINE_API, WEBCONFIG)}"/>

    <c:set var="placementAndField" value="${placement}_${fieldName}" />
        <%-- ############# --%>
        <div id="${fn:replace(placement, ":", "_")}${fieldName}_table" class="collection-table">
        <a name="${fieldName}" class="anchor"></a>
        <h3>
          <c:if test="${IS_SUPERUSER}">
            <div class="right">
              <c:set var="descriptor" value="${collection.descriptor}" />
              <tiles:insert name="inlineTagEditor.tile">
                <tiles:put name="taggable" beanName="descriptor" />
                <tiles:put name="show" value="true" />
              </tiles:insert>
            </div>
          </c:if>
          ${collection.size}&nbsp;${fieldDisplayName}
          <im:typehelp type="${pathString}" />
        </h3>
        <div class="clear"></div>
        <%-- ############# --%>
    <c:choose>
      <c:when test="${collection.size > 0}">
          <div id="coll_${fn:replace(placement, ":", "_")}${fieldName}">
          <div id="coll_${fn:replace(placement, ":", "_")}${fieldName}_inner" style="overflow-x:auto;">

            <c:set var="inlineResultsTable" value="${collection.table}"/>

            <tiles:insert page="/reportCollectionTable.jsp">
              <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
              <tiles:put name="object" beanName="object" />
              <tiles:put name="fieldName" value="${fieldName}" />
            </tiles:insert>
            <script type="text/javascript">trimTable('#coll_${fn:replace(placement, ":", "_")}${fieldName}_inner');</script>
          </div>

          <div class="show-in-table" style="display:none;">
            <html:link action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;trail=${param.trail}">
              Show all in a table »
            </html:link>
          </div>

          </div>
          <div class="clear"></div>
        <%-- ############# --%>
      </c:when>
      <c:otherwise>
        <script type="text/javascript">
          jQuery("#${fn:replace(placement, ":", "_")}${fieldName}_table.collection-table").addClass('gray');
        </script>
      </c:otherwise>
    </c:choose>
    </div>

  </c:forEach>
</c:if>

<!-- /reportRefsCols.jsp -->
