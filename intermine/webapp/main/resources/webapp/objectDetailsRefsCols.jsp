<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%--in order to filter out chars from strings --%>


<!-- objectDetailsRefsCols.jsp -->

<html:xhtml />

<tiles:importAttribute name="object" />
<tiles:importAttribute name="placement" />
<tiles:importAttribute name="showTitle" ignore="true" />
<c:if test="${!empty placementRefsAndCollections[placement]}">

  <c:if test="${!empty showTitle}"><h2>${showTitle}</h2></c:if>

  <c:forEach items="${placementRefsAndCollections[placement]}" var="entry">
    <c:set var="collection" value="${entry.value}" />
    <c:set var="fieldName" value="${entry.key}" />
    <c:set var="placementAndField" value="${placement}_${fieldName}" />
    <c:set var="verbose"
      value="${!empty object.verbosity[placementAndField]}" />
    <c:choose>
      <c:when test="${verbose && collection.size > 0}">
        <%-- ############# --%>
        <div class="table grid_12 loadOnScroll" id="${fn:replace(placement, ":", "_")}${fieldName}_table">
        <h3 class="theme-1-border theme-5-background">
          <c:if test="${IS_SUPERUSER}">
            <span class="tag-editor">
              <c:set var="descriptor" value="${collection.descriptor}" />
              <tiles:insert name="inlineTagEditor.tile">
                <tiles:put name="taggable" beanName="descriptor" />
                <tiles:put name="show" value="true" />
              </tiles:insert>
            </span>
          </c:if>
        <html:link
          styleClass="getTable"
          linkName="${placement}_${fieldName}"
          onclick="return toggleCollectionVisibilityJQuery('${placement}', '${fieldName}', '${object.object.id}', '${param.trail}')"
          action="/modifyDetails?method=unverbosify&amp;field=${fieldName}&amp;placement=${placement}&amp;id=${object.id}&amp;trail=${param.trail}">
          <span class="collectionField theme-1-color">
            ${collection.size} ${fieldName}<!-- of type ${collection.descriptor.referencedClassDescriptor.unqualifiedName}-->
          </span>
          <c:forEach items="${object.clds}" var="cld">
            <im:typehelp type="${cld.unqualifiedName}.${fieldName}" />
          </c:forEach>
        </html:link></h3>
        <%-- ############# --%>
      </c:when>
      <c:when test="${collection.size > 0}">
        <%-- ############# --%>
        <div class="table grid_12 loadOnScroll" id="${fn:replace(placement, ":", "_")}${fieldName}_table">
        <h3 class="theme-1-border theme-5-background">
          <c:if test="${IS_SUPERUSER}">
            <span class="tag-editor">
              <c:set var="descriptor" value="${collection.descriptor}" />
              <tiles:insert name="inlineTagEditor.tile">
                <tiles:put name="taggable" beanName="descriptor" />
                <tiles:put name="show" value="true" />
              </tiles:insert>
            </span>
          </c:if>
        <html:link
          styleClass="getTable"
          linkName="${placement}_${fieldName}"
          onclick="return toggleCollectionVisibilityJQuery('${placement}', '${fieldName}', '${object.object.id}', '${param.trail}')"
          action="/modifyDetails?method=verbosify&amp;field=${fieldName}&amp;placement=${placement}&amp;id=${object.id}&amp;trail=${param.trail}">
          <span class="collectionField theme-1-color">
            ${collection.size} ${fieldName}<!--  of type ${collection.descriptor.referencedClassDescriptor.unqualifiedName}-->
          </span>
          <c:forEach items="${object.clds}" var="cld">
            <im:typehelp type="${cld.unqualifiedName}.${fieldName}" />
          </c:forEach>
        </html:link></h3>
        <%-- ############# --%>
        <%--
        <c:if test="${collection.size == 1}">
          <c:forEach
            items="${LEAF_DESCRIPTORS_MAP[collection.table.rowObjects[0]]}"
            var="cld2">
            <c:if test="${WEBCONFIG.types[cld2.name].tableDisplayer != null}">
              <c:set var="cld2" value="${cld2}" scope="request" />
              <c:set var="backup" value="${object}" />
              <c:set var="object" value="${collection.table.rowObjects[0]}"
                scope="request" />
              <tiles:insert page="${WEBCONFIG.types[cld2.name].tableDisplayer.src}" />
              <c:set var="object" value="${backup}" scope="request" />
            </c:if>
          </c:forEach>
        </c:if>
        --%>
      </c:when>
      <%--
      <c:otherwise>
                  <span class="nullStrike">
                    <img border="0" src="images/plus-disabled.gif" title=" " width="11" height="11"/>
                    <span class="collectionField nullReferenceField">${fieldName}</span>
                    <c:forEach items="${object.clds}" var="cld">
                      <im:typehelp type="${cld.unqualifiedName}.${fieldName}"/>
                    </c:forEach>
                  </span>
      </c:otherwise>
    </c:choose>
          <span class="collectionDescription ${collection.size == 0 ? 'nullReferenceField' : ''}">
            ${collection.size} <span class="type">${collection.descriptor.referencedClassDescriptor.unqualifiedName}</span>
          </span>
          <c:if test="${collection.size == 1 && !verbose}">
            <c:if test="${collection.table.ids[0] != null}">
              [<html:link action="/objectDetails?id=${collection.table.ids[0]}&amp;trail=${param.trail}|${collection.table.ids[0]}">
                <fmt:message key="results.details"/>
              </html:link>]
            </c:if>
          </c:if>
         <c:if test="${collection.size == 0}">
            </span>
          </c:if> &nbsp; <c:choose>
          --%>
    </c:choose>
    <c:if test="${collection.size > 0}">
      <c:choose>
        <c:when test="${verbose}">
          <%-- ############# --%>
          <div id="coll_${fn:replace(placement, ":", "_")}${fieldName}">
          <div id="coll_${fn:replace(placement, ":", "_")}${fieldName}_inner" style="overflow-x:auto;"><c:if
            test="${verbose}">
            <tiles:insert page="/objectDetailsCollectionTable.jsp">
              <tiles:put name="collection" beanName="collection" />
              <tiles:put name="object" beanName="object" />
              <tiles:put name="fieldName" value="${fieldName}" />
            </tiles:insert>

            <script type="text/javascript">trimTable('#coll_${fn:replace(placement, ":", "_")}${fieldName}_inner');</script>

          </c:if></div>

          <p class="in_table" style="display:none;">
            <html:link styleClass="theme-1-color" action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;trail=${param.trail}">
              Show all in a table
            </html:link>
          </p>

          </div>
          </div>
          <div class="clear"></div>
          <%-- ############# --%>
        </c:when>
        <c:otherwise>
          <%-- ############# --%>
          <div id="coll_${fn:replace(placement, ":", "_")}${fieldName}">
          <div id="coll_${fn:replace(placement, ":", "_")}${fieldName}_inner" style="overflow-x:auto;"></div>

          <p class="in_table" style="display:none;">
            <html:link styleClass="theme-1-color" action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;trail=${param.trail}">
              Show all in a table
            </html:link>
          </p>

          </div>
          </div>
          <div class="clear"></div>
          <%-- ############# --%>
        </c:otherwise>
      </c:choose>
    </c:if>

  </c:forEach>
</c:if>

<script type="text/javascript">
// on load
jQuery(document).ready(function() {
  loadInView();
});

// on scroll
jQuery(window).scroll(function() {
  loadInView();
});

//script that will load tables as they get into the viewport
function loadInView() {
    // for all divs that are to be loaded on scroll
    jQuery("div.loadOnScroll").each(function(index) {
      // fetch the identifier
      var id = jQuery(this).attr("id")
      // can we see the element?
      if (isElementInView("#" + id)) {
        // find our SPECIFIC link
        var a = jQuery(this).find('a.getTable');
        if (a.length !== 0) {
          // trigger the associated onlick handler
          a.triggerHandler("click");
        }
      }
    });

    function isElementInView(e) {
        // fetch the dimensions of the viewport
        var docViewTop = jQuery(window).scrollTop();
        var docViewBottom = docViewTop + jQuery(window).height();

        // fetch the element dimensions
        var eTop = jQuery(e).offset().top;
        var eBottom = eTop + jQuery(e).height();

        return ((eBottom >= docViewTop) && (eTop <= docViewBottom));
    }
}

</script>

<!-- /objectDetailsRefsCols.jsp -->
