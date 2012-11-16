<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- submissionDetailsDisplayer.jsp -->

<html:xhtml />

<style type="text/css">

</style>

<c:choose>
  <c:when test="${not empty expType}">
    <h2 style="font-weight: normal;">Technique: <strong>${expType}</strong></h2>
  </c:when>
  <c:otherwise>
    <h2 style="font-weight: normal;">Experiment Type: <i>not available</i></h2>
  </c:otherwise>
</c:choose>

<table id="submissionDetails">
    <tr>
        <td>
            <table id="left-table" style="margin-right: 25%;">
                <c:if test="${not empty notice}">
                <tr>
                  <td>NOTICE:</td>
                  <td><span style="border: 2px solid red; white-space: nowrap;"><strong>${notice}<strong></span></td>
                </tr>
                </c:if>
                 <tr>
                    <td style="padding-right: 130px;">Design:</td>
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
                        <td><span style="border: 2px solid red; white-space: nowrap;"><strong>${embargoDate}<strong></span></td>
                      </tr>
                  </c:if>
                  <c:if test="${empty embargoDate}">
                  <tr>
                    <td>Embargo Date:</td>
                    <td><span style="border: 2px solid green; white-space: nowrap;">This dataset is no longer embargoed</span></td>
                  </tr>
                  </c:if>
                  <c:if test="${not empty replacesSubmission}">
                  <tr>
                    <td>Replaces Submission(s):</td>
                    <td><strong>modENCODE_${fn:replace(replacesSubmission, ',', ', modENCODE_')}<strong></td>
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
                  <c:if test="${not empty multiplyMappedReadCount}">
                      <tr>
                        <td>MultiplyMappedReadCount:</td>
                        <td><strong>${multiplyMappedReadCount}<strong></td>
                      </tr>
                  </c:if>
                  <c:if test="${not empty uniquelyMappedReadCount}">
                      <tr>
                        <td>UniquelyMappedReadCount:</td>
                        <td><strong>${uniquelyMappedReadCount}<strong></td>
                      </tr>
                  </c:if>
                  <c:if test="${not empty totalReadCount}">
                      <tr>
                        <td>TotalReadCount:</td>
                        <td><strong>${totalReadCount}<strong></td>
                      </tr>
                  </c:if>
                  <c:if test="${not empty rnaSize}">
                      <tr>
                        <td>RNA size:</td>
                        <td><strong>${rnaSize}<strong></td>
                      </tr>
                  </c:if>
                  <c:if test="${not empty url}">
                      <tr>
                        <td>URL:</td>
                        <td><strong>${URL}<strong></td>
                      </tr>
                  </c:if>
            </table>
        </td>
        <td>
            <table id="right-table">
              <tr>
                <td style="padding-right: 130px;">Lab:</td>
                <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${labId}">${labName}</html:link> - ${labAffiliation}</td>
              </tr>
              <tr>
                <td>Project:</td>
                <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${labProjectId}">${labProjectName}</html:link> - ${labProjectSurnamePI}</td>
              </tr>
              <tr>
                <td>Experiment:</td>
                <c:set var="nameForURL"/>
                <str:encodeUrl var="nameForURL">${experimentName}</str:encodeUrl>

                <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${nameForURL}">${experimentName}</html:link></td>
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
        </td>
    </tr>
</table>

<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript">
    jQuery('#submissionDescriptionContent').expander({
        slicePoint: 300
      });
</script>

<!-- /submissionDetailsDisplayer.jsp -->