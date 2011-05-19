<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>

<!-- projectDetailsDisplayer.jsp -->

<html:xhtml />

<style type="text/css"></style>

<h2 style="font-weight: normal;">Title: <strong>${title}</strong></h2>

<table id="projectDetails" style="width:50%;">
  <tr>
      <td>Organism:</td>
      <td>
          <c:forEach items="${orgs}" var="org" varStatus="rstatus">
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${org.shortName}&class=Organism"><strong>${org.shortName}</strong></html:link>
                <c:if test="${!rstatus.last}">,  </c:if>
          </c:forEach>
      </td>
  </tr>
  <tr>
    <td>PI:</td>
    <td><strong>${PI}</strong></td>
  </tr>
  <tr>
    <td>URL:</td>
    <td><strong><a href="${url}" target="_blank">${url}</a></strong></td>
  </tr>
</table>

<!-- /projectDetailsDisplayer.jsp -->