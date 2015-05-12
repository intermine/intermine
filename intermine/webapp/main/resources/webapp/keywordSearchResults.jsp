<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<link rel="stylesheet" href="model/css/keywordSearch.css"
  type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<style type="text/css">
input.submit {
  color: #927f97;
  font: bold 84% 'trebuchet ms', helvetica, sans-serif;
  background-color: #fed;
  border: 1px solid;
  border-color: #696 #363 #363 #696;
}
</style>

<script>

  jQuery(document).ready(function(){
    // Uncheck all checkboxes everything the page is (re)loaded
    initCheck();

    // Do before the form submitted
    jQuery("#saveFromIdsToBagForm").submit(function() {
        var ids = new Array();
        jQuery(".item").each(function() {
          if (this.checked) {ids.push(this.value);}
       });

        if (ids.length < 1) {
          alert("Please select some ${searchFacetValues['Category']}s...");
            return false;
        } else {
          if (jQuery('#allItems').is(':checked')) {
              jQuery("#allChecked").val('true');
          } else {
            jQuery("#allChecked").val('false');
          }
          jQuery("#ids").val(ids);
            return true;
        }
    });
  });

     function initCheck()
     {
       jQuery('#allItems').removeAttr('checked');
       jQuery(".item").removeAttr('checked');
     }

     // (un)Check all ids checkboxes
     function checkAll()
     {
         // Removed code, not compatible with jQuery 1.6+: jQuery(".item").attr('checked', jQuery('#allItems').is(':checked'));
         jQuery(".item").prop('checked', jQuery('#allItems').is(':checked'));
         jQuery('#allItems').css("opacity", 1);
     }

     /* function updateCheckStatus(status)
     {
         var statTag;
         if (!status) { //unchecked
           jQuery(".item").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#allItems").attr('checked', true);
            jQuery("#allItems").css("opacity", 0.5); }
           else {
            jQuery("#allItems").removeAttr('checked');
            jQuery("#allItems").css("opacity", 1);}
         }
         else { //checked
           jQuery(".item").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#allItems").attr('checked', true);
           jQuery("#allItems").css("opacity", 0.5); }
         else {
           jQuery("#allItems").attr('checked', true);
           jQuery("#allItems").css("opacity", 1);}
         }
     } */

     function updateCheckStatus(status)
     {
         var statTag;
         if (!status) { //unchecked
           jQuery(".item").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#allItems").removeAttr('checked');
            jQuery("#allItems").css("opacity", 1);}
           else {
            jQuery("#allItems").removeAttr('checked');
            jQuery("#allItems").css("opacity", 1);}
         }
         else { //checked
           jQuery(".item").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#allItems").removeAttr('checked');
            jQuery("#allItems").css("opacity", 1);}
         else {
           jQuery("#allItems").attr('checked', true);
           jQuery("#allItems").css("opacity", 1);}
         }
     }

</script>

<div class="body"><tiles:insert name="keywordSearch.tile" /> <c:if
  test="${!empty searchTerm || !empty searchFacets}">
  <c:choose>
    <c:when
      test="${empty searchTerm && !empty searchFacets && (searchFacetValues == null || empty searchFacetValues)}">
      <div class="keywordSearchIndex"><c:forEach
        items="${searchFacets}" var="facet" varStatus="facetStatus">
        <c:if test="${facet.items != null && !empty facet.items}">
          <div class="keywordSearchIndexColumn"
            style="width: <c:out value="${(90 - 90 % fn:length(searchFacets)) / fn:length(searchFacets)}"></c:out>%;">
          <h2 class="overviewFacetHeader">Objects by ${facet.name}</h2>
          <div class="overviewFacetContents">
          <ul>
            <c:forEach items="${facet.items}" var="facetItem">
              <li><a
                href="<c:url value="/keywordSearchResults.do">
                                   <c:param name="searchTerm" value="${searchTerm}" />
                                   <c:param name="searchBag" value="${searchBag}" />
                                   <c:param name="facet_${facet.field}" value="${facetItem.value}" />
                            </c:url>"
                title="Click to show '<c:out value="${facetItem.value}" />'">
              <c:out value="${imf:formatPathStr(facetItem.value, INTERMINE_API, WEBCONFIG)}" /> (<c:out
                value="${facetItem.facetValueHitCount}"></c:out>) </a></li>
            </c:forEach>
          </ul>
          </div>
          </div>
        </c:if>
      </c:forEach></div>
    </c:when>

    <c:otherwise>
      <div class="keywordSearchResults">

        <c:if test="${!empty searchTerm && searchTerm != '*:*'}">
          <c:choose>
            <c:when test="${searchTotalHits > 0}">
              <h1 class="title">Search results <c:out value="${searchOffset + 1}
                     to ${fn:length(searchResults) + searchOffset} out of ${searchTotalHits}" /> for
                       <i><c:out value="${searchTerm}" /></i></h1>
                   </c:when>
                   <c:otherwise>
                     <h1 class="title">Unfortunately, your search for "<c:out value="${searchTerm}" />" did not return
                     any results</h1>
                 Please try one of these steps to broaden your search:

        <ul>
          <c:if test="${!empty searchFacetValues}">
            <li>Remove restrictions by clicking the red <b>(x)</b> above</li>
          </c:if>
          <li>Add an asterisk (*) to the end of a word to search for
          partial matches, e.g. <i>dros*</i></li>
          <li>Search for synonyms using the OR operator, e.g. <i>(fly
          OR drosophila)</i></li>
        </ul>
                   </c:otherwise>
          </c:choose>
        </c:if>

      <div><c:forEach items="${searchFacets}" var="facet">
        <c:if test="${facet.value != null && facet.value != ''}">
          <h2 class="facetRestriction">${facet.name} restricted to <b>${facet.value}</b>
          <a
            href="<c:url value="/keywordSearchResults.do">
                     <c:param name="searchTerm" value="${searchTerm}" />
                     <c:param name="searchBag" value="${searchBag}" />
                     <c:forEach items="${searchFacets}" var="facetOTHER">
                         <c:if test="${facetOTHER.field != facet.field && facetOTHER.value != null && facetOTHER.value != ''}">
                             <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                         </c:if>
                     </c:forEach>
              </c:url>">
          <img border="0" src="images/cross.gif" alt="(x)"
            title="Remove restriction" /> </a></h2>
        </c:if>
      </c:forEach> <c:if test="${!empty searchBag}">
        <div class="facetRestriction">Searching only in list <b>${searchBag}</b>
        <a
          href="<c:url value="/keywordSearchResults.do">
                 <c:param name="searchTerm" value="${searchTerm}" />
                 <c:forEach items="${searchFacets}" var="facetOTHER">
                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                     </c:if>
                 </c:forEach>
          </c:url>">
        <img border="0" src="images/cross.gif" alt="(x)"
          title="Remove restriction" /> </a></div>
      </c:if></div><c:if test="${searchTotalHits > 0}">
        <c:if test="${searchTotalHits > fn:length(searchResults)}">
          <div class="pages"><c:choose>
            <c:when test="${searchOffset > 0}">
              <a
                href="<c:url value="/keywordSearchResults.do">
                           <c:param name="searchTerm" value="${searchTerm}" />
                           <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="0" /></c:url>">
              &lt;&lt;&nbsp;First </a>
                      &nbsp;&nbsp;
                        <a
                href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="${searchOffset - searchPerPage}" /></c:url>">
              &lt;&nbsp;Previous </a>
            </c:when>
            <c:otherwise>
                       &lt;&lt;&nbsp;First
                         &nbsp;&nbsp;
                       &lt;&nbsp;Previous
                    </c:otherwise>
          </c:choose> &nbsp;|&nbsp; <c:choose>
            <c:when test="${searchOffset + searchPerPage < searchTotalHits}">
              <a
                href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="${searchOffset + searchPerPage}" /></c:url>">
              Next&nbsp;&gt; </a>
                        &nbsp;&nbsp;
                        <a
                href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="${searchTotalHits - searchTotalHits % searchPerPage}" /></c:url>">
              Last&nbsp;&gt;&gt; </a>
            </c:when>
            <c:otherwise>
                       Next&nbsp;&gt;
                         &nbsp;&nbsp;
                       Last&nbsp;&gt;&gt;
                    </c:otherwise>
          </c:choose></div>
        </c:if>

        <div style="color: #ccc; font-size: 9px;">${searchTime/1000}s
        </div>

        <div style="clear: both;">

        <div class="facets">
        <h4>Categories</h4>
        <c:forEach items="${searchFacets}"
          var="facet">
          <c:if test="${facet.items != null && !empty facet.items}">
            <c:choose>
              <c:when test="${facet.value != null && facet.value != ''}">
                <div class="facetHeader">${facet.name}: <i>${facet.value}</i></div>
                <div class="facetContents"><a
                  href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.field != facet.field && facetOTHER.value != null && facetOTHER.value != ''}">
                                           <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                          </c:url>">
                &laquo; show all </a></div>
              </c:when>
              <c:otherwise>
                <h3 class="facetHeader">Hits by ${facet.name}</h3>
                <div class="facetContents">
                <ul>
                  <c:forEach items="${facet.items}" var="facetItem"
                    varStatus="facetItemStat">
                    <c:if test="${facetItemStat.index == 10}">
                      <script type="text/javascript">
                        // will show content of the next ul and destroy itself
                        function showMore(e) {
                          jQuery(e).parent('li').toggle ();
                          jQuery(e).parent('li').parent('ul').next('ul').slideToggle('fast');
                        }
                      </script>
                      <li class="facetMoreLink"><a href="#"
                        onclick="showMore(this);return false;">
                      ... and <b>${fn:length(facet.items) - facetItemStat.index
                      }</b> more values &raquo; </a></li>
                      </ul>

                      <ul style="display: none;">
                    </c:if>

                    <li><a
                      href="<c:url value="/keywordSearchResults.do">
                                               <c:param name="searchTerm" value="${searchTerm}" />
                                               <c:param name="searchBag" value="${searchBag}" />
                                               <c:param name="facet_${facet.field}" value="${facetItem.value}" />
                                         <c:forEach items="${searchFacets}" var="facetOTHER">
                                             <c:if test="${facetOTHER.field != facet.field && facetOTHER.value != null && facetOTHER.value != ''}">
                                                 <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                             </c:if>
                                         </c:forEach>
                                        </c:url>"
                      title="Click to only show '<c:out value="${facetItem.value}" />'">
                    <c:out value="${imf:formatPathStr(facetItem.value, INTERMINE_API, WEBCONFIG)}" />: <c:out
                      value="${facetItem.facetValueHitCount}"></c:out> </a></li>
                  </c:forEach>
                </ul>
                </div>
              </c:otherwise>
            </c:choose>
          </c:if>
        </c:forEach></div>

        <div class="resultTableContainer">
        <c:if test="${!empty searchFacetValues['Category']}">
          <form action="/${WEB_PROPERTIES['webapp.path']}/saveFromIdsToBag.do" id="saveFromIdsToBagForm" method="POST">
            <input type="hidden" id="type" name="type" value="${searchFacetValues['Category']}" />
            <input type="hidden" id="ids" name="ids" value="" />
            <input type="hidden" id="allChecked" name="allChecked" value="false" />
            <input type="hidden" name="source" value="keywordSearchResults" />
            <input type="hidden" id="totalHits" name="totalHits" value="${searchTotalHits}" />
            <input type="hidden" id="searchTerm" name="searchTerm" value="${fn:escapeXml(searchTerm)}" />
            <input type="hidden" id="jsonFacets" name="jsonFacets" value="<c:out value="${jsonFacets}"/>" />

            <input type="hidden" name="newBagName" value="new_${searchFacetValues['Category']}_list" />
            <div align="left" style="position: relative; top: 1em; padding-bottom: 5px;"><input type="submit" class="submit" value="CREATE LIST" /></div>
          </form>
        </c:if>

        <table cellpadding="0" cellspacing="0" border="0" class="dbsources">
          <tr>

            <c:if test="${!empty searchFacetValues['Category']}">
                <th style="width:1px">
                    <input type="checkbox" id="allItems" onclick="checkAll()" />
                </th>
            </c:if>

            <th class="type">Type</th>
            <th>Details</th>
            <th>Score</th>
          </tr>
          <c:forEach items="${searchResults}" var="searchResult" varStatus="status">
            <im:searchResult index="${status.count}"
                             showSelector="${!empty searchFacetValues['Category']}"
                             searchResult="${searchResult}">
            </im:searchResult>
          </c:forEach>
        </table>
        </div>

        </div>
      </c:if></div>
    </c:otherwise>
  </c:choose>
</c:if></div>

<script type="text/javascript">
  // placeholder value for search boxes
  var placeholder = '<c:out value="${WEB_PROPERTIES['begin.searchBox.example']}" />';
  // class used when toggling placeholder
  var inputToggleClass = 'eg';

  if (jQuery('input#keywordSearch').val() == '') {
    jQuery('input#keywordSearch').val(placeholder);
     // e.g. values only available when JavaScript is on
    jQuery('input#keywordSearch').toggleClass(inputToggleClass);
  }

  // register input elements with blur & focus
  jQuery('input#keywordSearch').blur(function() {
    if (jQuery(this).val() == '') {
      jQuery(this).toggleClass(inputToggleClass);
      jQuery(this).val(placeholder);
    }
  });
  jQuery('input#keywordSearch').focus(function() {
    if (jQuery(this).hasClass(inputToggleClass)) {
      jQuery(this).toggleClass(inputToggleClass);
      jQuery(this).val('');
    }
  });
</script>
