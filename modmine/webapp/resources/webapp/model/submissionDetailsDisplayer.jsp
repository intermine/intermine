<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>

<!-- submissionDetailsDisplayer.jsp -->

<html:xhtml />

<style type="text/css"></style>

<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript">

    jQuery(document).ready(function($){
        $('#submissionDescriptionContent').expander();
    });

</script>


<table id="submissionDetails">
  <tr>
    <td>Lab:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${labId}">${labName}</html:link> - ${labAffiliation}</td>
  </tr>
  <tr>
    <td>Project:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${labProjectId}">${labProjectName}</html:link> - ${labProjectSurnamePI}</td>
  </tr>
  <tr>
    <td>Organism:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${organismId}">${organismShortName}</html:link></td>
  </tr>
  <tr>
    <td>Experiment:</td>
    <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${experimentName}">${experimentName}</html:link></td>
  </tr>
  <tr>
    <td valign="top">Description:</td>
    <td id="submissionDescriptionContent" align="justify"><html href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${subId}">${subDescription}</html></td>
  </tr>
</table>

<!-- /submissionDetailsDisplayer.jsp -->