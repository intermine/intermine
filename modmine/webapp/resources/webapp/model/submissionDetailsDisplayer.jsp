<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>

<!-- submissionDetailsDisplayer.jsp -->

<html:xhtml />

<style type="text/css"></style>



<c:choose>
  <c:when test="${not empty expType}">
    <h2 style="font-weight: normal;">Experiment Type: <strong>${expType}</strong></h2>
  </c:when>
  <c:otherwise>
    <h2 style="font-weight: normal;">Experiment Type: <i>not available</i></h2>
  </c:otherwise>
</c:choose>

<table id="submissionDetails" style="width:50%;">
  <tr>
    <td style="width:20%;">Design:</td>
    <td><strong>${design}<strong></td>
  </tr>
  <tr>
    <td>Organism:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${organismId}"><strong>${organismShortName}</strong></html:link></td>
  </tr>
  <tr>
    <td>DCCid:</td>
    <td><strong>${dccId}<strong></td>
  </tr>
  <tr>
    <td>Public Release Date:</td>
    <td><strong>${publicReleaseDate}<strong></td>
  </tr>
  <c:if test="${not empty embargoDate}">
      <tr>
        <td>Embargo Date:</td>
        <td><strong>${embargoDate}<strong></td>
      </tr>
      <tr>
        <th COLSPAN=2 BGCOLOR="#FF3333" style="margin:5 0 5 0;">Embargo date is in the future...</th>
      </tr>
  </c:if>
  <c:if test="${not empty qualityControl}">
      <tr>
        <td>Quality Control:</td>
        <td><strong>${qualityControl}<strong></td>
      </tr>
  </c:if>
  <c:if test="${not empty replicate}">
      <tr>
        <td>Replicate:</td>
        <td><strong>${replicate}<strong></td>
      </tr>
  </c:if>
  <tr>
    <td>Lab:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${labId}">${labName}</html:link> - ${labAffiliation}</td>
  </tr>
  <tr>
    <td>Project:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${labProjectId}">${labProjectName}</html:link> - ${labProjectSurnamePI}</td>
  </tr>
  <tr>
    <td>Experiment:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${experimentName}">${experimentName}</html:link></td>
  </tr>
  <tr>
    <td valign="top">Description:</td>
    <td id="submissionDescriptionContent" align="justify"><html href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${subId}">${subDescription}</html></td>
  </tr>
  <c:if test="${not empty relatedSubmissions}">
      <tr>
        <td>Related Submissions:</td>
        <td id="relatedSubmissions">
        <c:choose>
            <c:when test="${not empty relatedSubmissions}">
              <c:forEach items="${relatedSubmissions}" var="relSubDCCid" varStatus="rstatus">
                    <html:link href="/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${relSubDCCid}&class=Submission">${relSubDCCid}</html:link>
                    <c:if test="${!rstatus.last}">,  </c:if>
              </c:forEach>
            </c:when>
            <c:otherwise>
                <i>no related submissions</i>
            </c:otherwise>
        </c:choose>

        </td>
      </tr>
  </c:if>
</table>

<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript">
    jQuery('#submissionDescriptionContent').expander({
        slicePoint: 300
      });
</script>

<!-- /submissionDetailsDisplayer.jsp -->