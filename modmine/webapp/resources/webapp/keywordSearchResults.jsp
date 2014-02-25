<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<link rel="stylesheet" href="model/css/keywordSearch.css" type="text/css" media="screen" title="no title" charset="utf-8">

<tiles:importAttribute />

<html:xhtml />

<style type="text/css">
input.submit {
  color: #008AB8;
  font: bold 84% 'trebuchet ms',helvetica,sans-serif;
  background-color: #fed;
  border:1px solid;
  border-color: #696 #363 #363 #696;
}
</style>

<script>

  jQuery(document).ready(function(){
    // Unckeck all checkboxes everything the page is (re)loaded
    initCheck();

    // Do before the form submitted
    jQuery("#saveFromIdsToBagForm").submit(function() {
        var ids = new Array();
        jQuery(".item").each(function() {
          if (this.checked) {ids.push(this.value);}
       });

        if (ids.length < 1)
        { alert("Please select some ${searchFacetValues['Category']}s...");
          return false;
        } else {
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
           jQuery("#allItems").prop('checked', true);
           jQuery("#allItems").css("opacity", 1);}
         }
     }

</script>

<div class="body">

<tiles:insert name="keywordSearch.tile"/>

<c:if test="${!empty searchTerm || !empty searchFacets}">
<c:choose>
    <c:when test="${empty searchTerm && !empty searchFacets && (searchFacetValues == null || empty searchFacetValues)}">
     <div class="keywordSearchIndex" style="background: #008AB8">
     <c:forEach items="${searchFacets}" var="facet" varStatus="facetStatus">
        <c:if test="${facet.items != null && !empty facet.items}">
            <div class="keywordSearchIndexColumn" style="width: <c:out value="${(90 - 90 % fn:length(searchFacets)) / fn:length(searchFacets)}"></c:out>%;">
                <h2 class="overviewFacetHeader">Objects by ${facet.name}</h2>
                <div class="overviewFacetContents">
                <ul>
                    <c:forEach items="${facet.items}" var="facetItem">
                            <li>
                            <a href="<c:url value="/keywordSearchResults.do">
                                   <c:param name="searchTerm" value="${searchTerm}" />
                                   <c:param name="searchBag" value="${searchBag}" />
                                   <c:param name="facet_${facet.field}" value="${facetItem.value}" />
                            </c:url>" title="Click to show '<c:out value="${facetItem.value}" />'">
                               <c:out value="${facetItem.value}" />
                               (<c:out value="${facetItem.facetValueHitCount}"></c:out>)
                            </a>
                        </li>
                    </c:forEach>
                </ul>
                </div>
              </div>
        </c:if>
     </c:forEach>
     </div>
    </c:when>

  <c:otherwise>
    <div class="keywordSearchResults">

    <div>
    <c:choose>
      <c:when test="${!empty searchTerm && searchTerm != '*:*'}">
           Search Term: <b><c:out value="${searchTerm}"/></b>
        </c:when>
        <c:otherwise>
           <i>(showing all)</i>
        </c:otherwise>
      </c:choose>
    </div>

    <div>
    <c:forEach items="${searchFacets}" var="facet">
        <c:if test="${facet.value != null && facet.value != ''}">
          <div class="facetRestriction">
              ${facet.name} restricted to <b>${facet.value}</b>
              <a href="<c:url value="/keywordSearchResults.do">
                     <c:param name="searchTerm" value="${searchTerm}" />
                     <c:param name="searchBag" value="${searchBag}" />
                     <c:forEach items="${searchFacets}" var="facetOTHER">
                         <c:if test="${facetOTHER.field != facet.field && facetOTHER.value != null && facetOTHER.value != ''}">
                             <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                         </c:if>
                     </c:forEach>
              </c:url>">
                  <img border="0" src="images/cross.gif" alt="(x)" title="Remove restriction"/>
              </a>
          </div>
        </c:if>
    </c:forEach>

    <c:if test="${!empty searchBag}">
      <div class="facetRestriction">
          Searching only in list <b>${searchBag}</b>
          <a href="<c:url value="/keywordSearchResults.do">
                 <c:param name="searchTerm" value="${searchTerm}" />
                 <c:forEach items="${searchFacets}" var="facetOTHER">
                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                     </c:if>
                 </c:forEach>
          </c:url>">
              <img border="0" src="images/cross.gif" alt="(x)" title="Remove restriction"/>
          </a>
      </div>
    </c:if>
     </div>

    <c:if test="${searchTotalHits == 0}">
        <div style="margin-top: 2em; text-align: center;">
            <div style="font-weight: bold;">
              Unfortunately your search did not return any results!
            </div>

                <div style="margin-top: 1em;">
              Please try one of these steps to broaden your search:
                </div>

            <ul>
                <c:if test="${!empty searchFacetValues}">
                    <li>Remove restrictions by clicking the red <b>(x)</b> above</li>
                </c:if>
                <li>Add an asterisk (*) to the end of a word to search for partial matches, e.g. <i>dros*</i></li>
                <li>Search for synonyms using the OR operator, e.g. <i>(fly OR drosophila)</i></li>
            </ul>
        </div>
    </c:if>

    <c:if test="${searchTotalHits > 0}">
      <div style="margin-top: 1em;">
         <c:out value="Showing results ${searchOffset + 1} to ${fn:length(searchResults) + searchOffset} out of ${searchTotalHits}"/>
         <c:if test="${searchTotalHits > fn:length(searchResults)}">
              <div class="pages">
                  <c:choose>
                    <c:when test="${searchOffset > 0}">
                      <a href="<c:url value="/keywordSearchResults.do">
                           <c:param name="searchTerm" value="${searchTerm}" />
                           <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="0" /></c:url>">
                         &lt;&lt;&nbsp;First
                      </a>
                      &nbsp;&nbsp;
                        <a href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="${searchOffset - searchPerPage}" /></c:url>">
                           &lt;&nbsp;Previous
                        </a>
                    </c:when>
                    <c:otherwise>
                       &lt;&lt;&nbsp;First
                         &nbsp;&nbsp;
                       &lt;&nbsp;Previous
                    </c:otherwise>
                  </c:choose>
                  &nbsp;|&nbsp;
                  <c:choose>
                    <c:when test="${searchOffset + searchPerPage < searchTotalHits}">
                        <a href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="${searchOffset + searchPerPage}" /></c:url>">
                           Next&nbsp;&gt;
                        </a>
                        &nbsp;&nbsp;
                        <a href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.value != null && facetOTHER.value != ''}">
                                         <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                                 <c:param name="searchOffset" value="${searchTotalHits - searchTotalHits % searchPerPage}" /></c:url>">
                           Last&nbsp;&gt;&gt;
                        </a>
                    </c:when>
                    <c:otherwise>
                       Next&nbsp;&gt;
                         &nbsp;&nbsp;
                       Last&nbsp;&gt;&gt;
                    </c:otherwise>
                  </c:choose>
              </div>
         </c:if>
      </div>

      <div style="color: #ccc; font-size: 9px;">
       ${searchTime/1000}s
      </div>

      <div style="clear: both;">

      <div class="facets" style="background: #008AB8">
            <c:forEach items="${searchFacets}" var="facet">
              <c:if test="${facet.items != null && !empty facet.items}">
                  <c:choose>
                      <c:when test="${facet.value != null && facet.value != ''}">
                          <div class="facetHeader">${facet.name}: <i>${facet.value}</i></div>
                          <div class="facetContents">
                          <a href="<c:url value="/keywordSearchResults.do">
                                 <c:param name="searchTerm" value="${searchTerm}" />
                                 <c:param name="searchBag" value="${searchBag}" />
                                 <c:forEach items="${searchFacets}" var="facetOTHER">
                                     <c:if test="${facetOTHER.field != facet.field && facetOTHER.value != null && facetOTHER.value != ''}">
                                           <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                     </c:if>
                                 </c:forEach>
                          </c:url>">
                              &laquo; show all
                          </a>
                          </div>
                      </c:when>
                      <c:otherwise>
                          <div class="facetHeader">Hits by ${facet.name}</div>
                          <div class="facetContents">
                          <ol>
                                <c:forEach items="${facet.items}" var="facetItem" varStatus="facetItemStat">
                                    <c:if test="${facetItemStat.index == 10}">
                                        <li class="facetMoreLink">
                                            <a href="javascript:{}" onclick="jQuery(this).parent('li').next('ul').slideToggle('fast');">
                                               ... and <b>${fn:length(facet.items) - facetItemStat.index }</b> more values &raquo;
                                            </a>
                                        </li>
                                    </ul>

                                    <ul style="display: none;">
                                  </c:if>

                                        <li>
                                        <a href="<c:url value="/keywordSearchResults.do">
                                               <c:param name="searchTerm" value="${searchTerm}" />
                                               <c:param name="searchBag" value="${searchBag}" />
                                               <c:param name="facet_${facet.field}" value="${facetItem.value}" />
                                         <c:forEach items="${searchFacets}" var="facetOTHER">
                                             <c:if test="${facetOTHER.field != facet.field && facetOTHER.value != null && facetOTHER.value != ''}">
                                                 <c:param name="facet_${facetOTHER.field}" value="${facetOTHER.value}" />
                                             </c:if>
                                         </c:forEach>
                                        </c:url>" title="Click to only show '<c:out value="${facetItem.value}" />'">
                                           <c:out value="${facetItem.value}" />:
                                           <c:out value="${facetItem.facetValueHitCount}"></c:out>
                                        </a>
                                    </li>
                                </c:forEach>
                          </ol>
                          </div>
                      </c:otherwise>
                  </c:choose>
              </c:if>
            </c:forEach>
      </div>

  <div class="resultTableContainer">

    <c:if test="${!empty searchFacetValues['Category']}">
      <form action="/${WEB_PROPERTIES['webapp.path']}/saveFromIdsToBag.do" id="saveFromIdsToBagForm" method="POST">
          <input type="hidden" id="type" name="type" value="${searchFacetValues['Category']}"/>
          <input type="hidden" id="ids" name="ids" value=""/>
          <input type="hidden" name="source" value="keywordSearchResults"/>
          <input type="hidden" name="newBagName" value="new_${searchFacetValues['Category']}_list"/>
          <div align="left" style="position:relative; top:1em; padding-bottom:5px;"><input type="submit" class="submit" value="CREATE LIST"/></div>
      </form>
    </c:if>

        <table cellpadding="0" cellspacing="0" border="0" class="dbsources">
                  <c:choose>
                      <c:when test="${searchFacetValues['Category'] == 'Submission'}">
            <tr>
                <th>
                  <input type="checkbox" id="allItems" onclick="checkAll()"/>
                </th>
                <th>DCC id</th>
                <th>Organism</th>
                <th>Group</th>
                <th>Name</th>
                <th>Date</th>
                <th>Details</th>
                <th>Search score</th>
            </tr>
            <c:forEach items="${searchResults}" var="searchResult">
                  <c:set var="sub" value="${searchResult.object}"/>
              <tr>
                  <td>
                      <input type="checkbox" class="item" value="${sub.id}" onclick="updateCheckStatus(this.checked)"/>
                  </td>
                  <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${sub.id}"><c:out value="${sub.dCCid}"></c:out></html:link></td>
                  <td>
                  <c:if test="${sub.organism.genus eq 'Drosophila'}">
                    <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/>
                                    <c:set var="fly" value="1" />
                  </c:if>
                  <c:if test="${sub.organism.genus eq 'Caenorhabditis'}">
                    <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/>
                                    <c:set var="worm" value="1" />
                                </c:if>
                  </td>
                  <td>PI: <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${sub.project.id}"><c:out value="${sub.project.surnamePI}"/></html:link><br/>
                      Lab: <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${sub.lab.id}"><c:out value="${sub.lab.surname}"/></html:link><br/>
                  </td>
                  <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${sub.id}"><c:out value="${sub.title}"></c:out></html:link></td>
                  <td><fmt:formatDate value="${sub.publicReleaseDate}" type="date"/></td>
                  <td>
                    <c:set var="isPrimer" value="0"/>
                    <c:forEach items="${sub.properties}" var="prop" varStatus="status">
                     <c:choose>
                      <c:when test="${fn:contains(prop,'primer')}">
                      <c:set var="isPrimer" value="${isPrimer + 1}"/>
                      </c:when>
                      </c:choose>
                    <c:choose>
                    <c:when test="${isPrimer <= 5 || !fn:contains(prop,'primer')}">
                      <c:out value="${prop.type}: "/>
                      <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${prop.id}">
                      <c:out value="${prop.name}"/></html:link><br/>
                    </c:when>
                    <c:when test="${isPrimer > 5 && status.last}">
                    ...<br></br>
                    <im:querylink text="all ${isPrimer} ${prop.type}s" showArrow="true" skipBuilder="true"
                              title="View all ${isPrimer} ${prop.type}s factors of submission ${sub.dCCid}">

                  <query name="" model="genomic" view="SubmissionProperty.name SubmissionProperty.type" sortOrder="SubmissionProperty.type asc" constraintLogic="A and B">
                    <node path="SubmissionProperty" type="SubmissionProperty">
                    </node>
                    <node path="SubmissionProperty.submissions" type="Submission">
                      <constraint op="LOOKUP" value="${sub.dCCid}" description="" identifier="" code="A" extraValue="">
                      </constraint>
                    </node>
                    <node path="SubmissionProperty.type" type="String">
                      <constraint op="=" value="${prop.type}" description="" identifier="" code="B" extraValue="">
                      </constraint>
                    </node>
                  </query>

                    </im:querylink>

                    </c:when>
                    </c:choose>
                    </c:forEach>
                  </td>

                  <td><img height="10" width="${searchResult.points * 5}" src="images/heat${searchResult.points}.gif" alt="${searchResult.points}/10" title="${searchResult.points}/10"/></td>
            </tr>
                        </c:forEach>
              </c:when>
              <c:otherwise>
                        <tr>

                            <c:if test="${!empty searchFacetValues['Category']}">
                              <th>
                                <input type="checkbox" id="allItems" onclick="checkAll()"/>
                              </th>
                            </c:if>

                            <th>Type</th>
                            <th>Details</th>
                            <th>Search score</th>
                        </tr>
                        <c:forEach items="${searchResults}" var="searchResult">
              <tr class="keywordSearchResult">

                  <c:if test="${!empty searchFacetValues['Category']}">
                    <td>
                      <input type="checkbox" class="item" value="${searchResult.id}" onclick="updateCheckStatus(this.checked)"/>
                    </td>
                  </c:if>

                  <td>
                      <c:out value="${searchResult.type}"></c:out>
                  </td>
                  <td>
                      <div class="objectKeys">
                      <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${searchResult.id}">
                      <c:if test="${empty searchResult.keyFields}">
                          <c:out value="${searchResult.type}"></c:out>
                      </c:if>
                      <c:forEach items="${searchResult.keyFields}" var="field" varStatus="status">
                        <c:set var="fieldConfig" value="${searchResult.fieldConfigs[field]}"/>
                        <span title="<c:out value="${field}"/>" class="objectKey">
                           <c:choose>
                           <%-- print each field configured for this object --%>
                            <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
                              <c:set var="interMineObject" value="${searchResult.object}" scope="request"/>
                              <span class="value">
                                <tiles:insert page="${fieldConfig.displayer}">
                                  <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
                                </tiles:insert>
                              </span>
                            </c:when>
                            <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
                              <c:set var="outVal" value="${searchResult.fieldValues[fieldConfig.fieldExpr]}"/>
                              <span class="value">${outVal}</span>
                              <c:if test="${empty outVal}">
                                -
                              </c:if>
                            </c:when>
                            <c:otherwise>
                              -
                            </c:otherwise>
                          </c:choose>
                        </span>
                        <c:if test="${! status.last }">
                            <span class="objectKey">|</span>
                        </c:if>
                      </c:forEach>
                      </html:link>
                      </div>

                    <%-- print each field configured for this object --%>
                      <c:forEach items="${searchResult.additionalFields}" var="field">
                        <c:set var="fieldConfig" value="${searchResult.fieldConfigs[field]}"/>
                      <div class="objectField">
                         <c:choose>
                         <%-- print each field configured for this object --%>
                          <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
                                  <span class="objectFieldName"><c:out value="${field}"/>:</span>

                            <c:set var="interMineObject" value="${searchResult.object}" scope="request"/>
                            <span class="value">
                              <tiles:insert page="${fieldConfig.displayer}">
                                <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
                              </tiles:insert>
                            </span>
                          </c:when>

                          <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
                                  <c:set var="outVal" value="${searchResult.fieldValues[fieldConfig.fieldExpr]}"/>
                            <c:if test="${!empty outVal}">
                                    <span class="objectFieldName"><c:out value="${field}"/>:</span>
                                  </c:if>

                            <span class="value" style="font-weight: bold;">${outVal}</span>
                            <c:if test="${empty outVal}">
                              &nbsp;<%--for IE--%>
                            </c:if>
                          </c:when>
                          <c:otherwise>
                            &nbsp;<%--for IE--%>
                          </c:otherwise>
                        </c:choose>
                      </div>
                    </c:forEach>

                              <c:if test="${searchResult.templates != null && !empty searchResult.templates}">
                                  <c:forEach items="${searchResult.templates}" var="template">
                                        <c:if test="${template.value.valid}">
                                        <div>
                                             <html:link action="/template?name=${template.value.name}&amp;scope=global&amp;idForLookup=${searchResult.id}"
                                                   title="Click here to go to the template form">
                                               <span class="templateTitle">${template.value.title}</span>
                             <img border="0" class="arrow" src="images/template_t.gif"/>
                        </html:link>
                      </div>
                      </c:if>
                                  </c:forEach>
                              </c:if>
                  </td>
                  <td><img height="10" width="${searchResult.points * 5}" src="images/heat${searchResult.points}.gif" alt="${searchResult.points}/10" title="${searchResult.points}/10"/></td>
            </tr>
                        </c:forEach>
          </c:otherwise>
        </c:choose>

        </table>
      </div>

      </div>
    </c:if>

    </div>
  </c:otherwise>
</c:choose>
</c:if>

</div>