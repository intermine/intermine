<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>


<tiles:importAttribute />


<html:xhtml />

<style type="text/css">
div#submissionLabName h3, div#submissionLabName div#submissionProject {
  color: black;
  margin-bottom: 10px;
}
div#submissionOrganism {
  color: black;
  margin-bottom: 20px;
}
div#submissionResults {
  color: black;
  margin-bottom: 20px;
  border: 1px;
  border-style: solid;
  border-color: green;
  background-color: #DFA;
  padding: 5px;
  width: 500px;
}

div#submissionResults h2 {
  font-size: 1.2em;
  color: black;
  font-style: bold;
}

div#submissionResults h3 {
  font-size: 1.1em;
  color: black;
}

div#submissionDescription {
  color: black;
  margin-bottom: 20px;
  border: 1px;
  border-style: solid;
  border-color: green;
  font-size: 1em;
  background-color: white;
  padding: 5px;
  width: 500px;
}

</style>

<div class="body">
  <div id="submissionLabName">
    <h3>
      <b>Lab:</b> <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${object.lab.id}">${object.lab.name}</html:link>
      - ${object.lab.affiliation}
    </h3>
  </div>
  <div id="submissionProject">
      <b>Project:</b> <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${object.lab.project.id}">${object.lab.project.name}</html:link> - ${object.lab.project.surnamePI}
  </div>
  <div id="submissionOrganism">
      <b>Organism:</b> <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${object.organism.id}">${object.organism.shortName}</html:link>
  </div>
   <div id="submissionExperiment">
      <b>Experiment:</b> <html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${object.experiment.name}">${object.experiment.name}</html:link>
  </div>


  <div id="submissionDescription">
    <p><b>Submission description</b></p>
    <br/>
    <p>
  <html href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${object.id}">${object.description}</html>
  </div>



<div id="relatedSubmissions">
   <c:forEach items="${object.relatedSubmissions}" var="relSubs" varStatus="rstatus">
   <c:if test="${rstatus.first}"><p><b>Related Submissions:</b> </c:if>
   <html:link href="/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${relSubs.dCCid}&class=Submission">${relSubs.dCCid}</html:link>
   <c:if test="${!rstatus.last}">,  </c:if>
   </c:forEach>
</div>
