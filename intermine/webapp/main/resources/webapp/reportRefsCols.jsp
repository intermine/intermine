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


  <c:set var="spaceChar" value=" "/>
  <c:set var="aspectPlacement" value="${fn:replace(placement, spaceChar, '')}" />

  <c:if test="${!empty showTitle && fn:length(placementRefsAndCollections[placement]) > 0}">
    <a name="miscellaneous"><h2>${showTitle}</h2></a>
  </c:if>

  <c:forEach items="${placementRefsAndCollections[placement]}" var="entry">
    <c:set var="collection" value="${entry.value}" />
    <c:set var="fieldName" value="${fn:replace(entry.key, spaceChar, '_')}" />
    <c:set var="pathString" value="${object.classDescriptor.unqualifiedName}.${fieldName}"/>
    <c:set var="fieldDisplayName"
        value="${imf:formatFieldStr(pathString, INTERMINE_API, WEBCONFIG)}"/>

    <c:set var="placementAndField" value="${aspectPlacement}_${fieldName}" />
    <c:set var="divName" value="${fn:replace(aspectPlacement, ':', '_')}${fieldName}_table" /> 

        <div id="${fn:replace(aspectPlacement, ":", "_")}${fieldName}_table" class="collection-table">
        <a name="${fieldName}" class="anchor"></a>
        <h3 id="${divName}_h3">
          <c:if test="${SHOW_TAGS}">
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
          <div id="coll_${fn:replace(aspectPlacement, ":", "_")}${fieldName}">
          <div id="coll_${fn:replace(aspectPlacement, ":", "_")}${fieldName}_inner" style="overflow-x:hidden;">
          <c:set var="innerDivName" value="coll_${fn:replace(aspectPlacement, ':', '_')}${fieldName}" /> 
          <c:set var="inlineResultsTable" value="${collection.table}"/>
          <c:set var="useTableWidget" value="${WEB_PROPERTIES['inline.collections.in.tables']=='true'}" />
          <c:set var="useLocalStorage" value="${WEB_PROPERTIES['use.localstorage']=='true'}" />
          <c:choose>
            <c:when test="${useTableWidget}">
              <tiles:insert page="/collectionToTable.do?field=${fieldName}&id=${object.id}&trail=${param.trail}&pathString=${object.classDescriptor.unqualifiedName}.${fieldName}"> 
              </tiles:insert>
            </c:when>
            <c:otherwise>
             <tiles:insert page="/reportCollectionTable.jsp"> 
              <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
              <tiles:put name="object" beanName="object" />
              <tiles:put name="fieldName" value="${fieldName}" />
             </tiles:insert>
            </c:otherwise> 
          </c:choose>
          <script type="text/javascript">
            trimTable('#coll_${fn:replace(aspectPlacement, ":", "_")}${fieldName}_inner');
            (function($) {
              $(function(){
                if(${useLocalStorage} && typeof(Storage)!=="undefined"){
                 if(localStorage.${innerDivName}==undefined || localStorage.${innerDivName} == "hide"){
                   jQuery('#${innerDivName}').hide();
                   localStorage.${innerDivName}="hide";
                 }
              }
              jQuery('#${divName}_h3').click(function(e){
               jQuery('#${innerDivName}').slideToggle('fast');
               if(${useLocalStorage} && typeof(Storage)!=="undefined"){
                 if(localStorage.${innerDivName}=="hide"){
                     localStorage.${innerDivName}="show";
                 }else{
                     localStorage.${innerDivName}="hide";
                 }
               }
               });
            });
            })(window.jQuery);
          </script>
          </div>
          <c:choose>
            <c:when test="${!useTableWidget}">
              <div class="show-in-table" style="display:none;">
              <html:link action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;trail=${param.trail}">
                Show all in a table
              </html:link>
              </div> 
            </c:when>
          </c:choose>
          </div>
          <div class="clear"></div>
        <%-- ############# --%>
      </c:when>
      <c:otherwise>
        <script type="text/javascript">
          jQuery("#${fn:replace(aspectPlacement, ":", "_")}${fieldName}_table.collection-table").addClass('gray');
        </script>
      </c:otherwise>
    </c:choose>
    </div>

  </c:forEach>
</c:if>

<!-- /reportRefsCols.jsp -->
