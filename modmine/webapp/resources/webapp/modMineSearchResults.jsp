<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<!--  modMineSearchResults.jsp -->
<tiles:importAttribute />

<html:xhtml />

<style type="text/css">
input.submit {
  color: #008AB8;
  font: bold 84% 'trebuchet ms',helvetica,sans-serif;
  background-color: #fed;
  border: 1px solid;
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
        jQuery(".aSub").each(function() {
          if (this.checked) {ids.push(this.value);}
       });

        if (ids.length < 1)
        { alert("Please select some submissions...");
          return false;dCC
        } else {
          jQuery("#ids").val(ids);
          return true;
          }
    });
  });

     function initCheck()
     {
       jQuery('#allSub').removeAttr('checked');
       jQuery(".aSub").removeAttr('checked');
     }

     // (un)Check all ids checkboxes
     function checkAll()
     {
         jQuery(".aSub").prop('checked', jQuery('#allSub').is(':checked'));
         jQuery('#allSub').css("opacity", 1);dCC
     }

     /* function updateCheckStatus(status)
     {
         var statTag;
         if (!status) { //unchecked
           jQuery(".aSub").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#allSub").attr('checked', true);
            jQuery("#allSub").css("opacity", 0.5); }
           else {
            jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
         }
         else { //checked
           jQuery(".aSub").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#allSub").attr('checked', true);
           jQuery("#allSub").css("opacity", 0.5); }
         else {
           jQuery("#allSub").attr('checked', true);
           jQuery("#allSub").css("opacity", 1);}
         }
     } */

     function updateCheckStatus(status)
     {
         var statTag;
         if (!status) { //unchecked
           jQuery(".aSub").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
           else {
            jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
         }
         else { //checked
           jQuery(".aSub").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
         else {
           jQuery("#allSub").prop('checked', true);
           jQuery("#allSub").css("opacity", 1);}
         }
     }

</script>

<div class="body">

<tiles:insert name="modMineSearch.tile"/>

Search Term: <c:out value="${searchTerm}"/>

<div>

<c:if test="${empty displayMax}"><c:out value="Matching submissions: ${fn:length(submissions)}"/></c:if>
<c:if test="${!empty displayMax}">Matching submissions: more than <c:out value="${displayMax}" /> (only the top <c:out value="${displayMax}" /> matches are displayed)</c:if>
<table cellpadding="0" cellspacing="0" border="0" class="dbsources">

<c:if test="${fn:length(submissions) > 0}">
    <form action="/${WEB_PROPERTIES['webapp.path']}/saveFromIdsToBag.do" id="saveFromIdsToBagForm" method="POST">
      <input type="hidden" id="type" name="type" value="Submission"/>
      <input type="hidden" id="ids" name="ids" value=""/>
      <input type="hidden" name="source" value="modMineSearchResults"/>
      <input type="hidden" name="newBagName" value="new_submission_list"/>
      <div style="position:relative; top:15px; padding:20px;"><input type="submit" class="submit" value="CREATE LIST"/></div>
    </form>
</c:if>

<tr>
    <c:if test="${fn:length(submissions) > 0}">
      <th><input type="checkbox" id="allSub" onclick="checkAll()"/></th>
    </c:if>
    <th>DCC id</th>
    <th>Organism</th>
    <th>Group</th>
    <th>Name</th>
    <th>Date</th>
    <th>Details</th>
    <th>Search score</th>
</tr>
<c:forEach items="${submissions}" var="subResult">
  <c:set var="sub" value="${subResult.key}"/>
  <tr>
      <td><input type="checkbox" class="aSub" value="${sub.id}" onclick="updateCheckStatus(this.checked)"/></td>
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

      <td><img height="10" width="${subResult.value * 5}" src="images/heat${subResult.value}.gif" alt="${subResult.value}" title="${subResult.value}"/></td>
</tr>
</c:forEach>
</table>



</div>

</div>
<!--  /modMineSearchResults.jsp -->