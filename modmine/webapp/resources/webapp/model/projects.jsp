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

<script type="text/javascript" src="<html:rewrite page='/js/jquery.qtip-1.0.0-rc3.min.js'/>"></script>
<script type="text/javascript" src="model/jquery_contextMenu/jquery.contextMenu.js"></script>
<script type="text/javascript" src="js/tablesort.js"></script>
<link rel="stylesheet" type="text/css" href="css/sorting_experiments.css"/>
<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>
<link href="model/jquery_contextMenu/jquery.contextMenu.css" rel="stylesheet" type="text/css" />

<script type="text/javascript">

    jQuery(document).ready( function() {
        jQuery(".exportMenu").hide();
        //
        jQuery(".exportDiv").mouseover(function(e) {
          jQuery('.contextMenu').removeAttr('id');
          jQuery(e.target).parent().children(".contextMenu").attr('id', 'exportMenu');
          });

          jQuery(".exportDiv").contextMenu({ menu: 'exportMenu', leftButton: true },
            function(action, el, pos) { window.open(action, '_self');
             });
    });

</script>

<div class="body">

</table>
<table cellpadding="0" cellspacing="0" border="0" class="topBar hints" width="95%">
<tr>
<td align="left"><a href="/${WEB_PROPERTIES['webapp.path']}/dataCategories.do?">Additional Data Sources</a></td>
<td align="right"><a href="/${WEB_PROPERTIES['webapp.path']}/projectsSummary.do">Switch to Experimentss View</a></td></tr>
</table>

<table cellpadding="0" cellspacing="0" border="0" class="sortable-onload-2 rowstyle-alt no-arrow submission_table">

  <tr>
    <th width="10" class="sortable">PROJECTS</th>
    <th width="10" class="sortable">PI</th>
    <th width="10" class="sortable">LABS</th>
    <th width="5" class="sortable"></th>
    <th width="600" class="sortable" >EXPERIMENTS</th>
    <th colspan=2 >GBrowse Tracks & Submissions to Repositories</th>
    <th width="300">FEATURES</th>
  </tr>


<c:forEach items="${experiments}" var="exp" varStatus="exp_status">
<tr>
<td class="sorting">
   <html:link href="/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=${exp.piSurname}&class=Project" title="more info on ${exp.projectName}">${exp.projectName}</html:link>
</td>
<td class="sorting">
${exp.piSurname}<br>
<span class="tinylink">
<im:querylink text="[ALL submissions]" skipBuilder="true">
<query name="" model="genomic" view="Project.submissions.DCCid Project.submissions.description Project.submissions.design Project.submissions.embargoDate Project.submissions.experimentType Project.submissions.experiment.name" sortOrder="Project.submissions.DCCid asc">
  <node path="Project" type="Project">
  </node>
  <node path="Project.surnamePI" type="String">
    <constraint op="=" value="${exp.piSurname}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
</im:querylink>
</span>


</td>
<td class="sorting">
  <c:forEach items="${exp.labs}" var="lab">
    <html:link href="/${WEB_PROPERTIES['webapp.path']}/portal.do?externalid=*${lab}&class=Lab" title="more info on ${lab}'s lab">${lab}</html:link><br>
<span class="tinylink">
<im:querylink text="[ALL submissions]" skipBuilder="true">
<query name="" model="genomic" view="Lab.submissions.DCCid Lab.submissions.description Lab.submissions.design Lab.submissions.experiment.name Lab.submissions.experimentType Lab.submissions.embargoDate" sortOrder="Lab.submissions.DCCid asc">
  <node path="Lab" type="Lab">
  </node>
  <node path="Lab.name" type="String">
    <constraint op="=" value="*${lab}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
</im:querylink>
</span>

</c:forEach>

</td>

<td class="sorting">
    <c:forEach items="${exp.organisms}" var="organism" varStatus="orgStatus">
      <c:if test="${organism eq 'D. melanogaster'}"> <font color="#fff">fly</font>
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/>
            <c:set var="fly" value="1" />
          </c:if>
      <c:if test="${organism eq 'C. elegans'}">  <font color="#fff">worm</font>
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/>
            <c:set var="worm" value="1" />
          </c:if>
    </c:forEach>
</td>

<td class="sorting">

<%-- CATEGORIES --%>
<b>
<c:choose>
<c:when test="${fn:contains(exp.name, '+')}">
<html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${fn:replace(exp.name, '+', '%2B')}"
title="View ${exp.name}">${exp.name}</html:link>
</c:when>
<c:otherwise>
<html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}"
title="View ${exp.name}">${exp.name}</html:link>
</c:otherwise>
</c:choose>
</b>
<br></br>

${exp.category}

<br></br>
<%-- SUBMISSIONS --%>
    <c:if test="${exp.submissionCount == 1}">
      <c:set var="submissions" value="${exp.submissionCount} Data submission."/>
    </c:if>
    <c:if test="${exp.submissionCount > 1}">
      <c:set var="submissions" value="${exp.submissionCount} Data submissions."/>
    </c:if>
<im:querylink text="${submissions}" skipBuilder="true">
<query name="" model="genomic" view="Submission.DCCid Submission.title Submission.experimentType Submission.description Submission.embargoDate" sortOrder="Submission.DCCid asc">
  <node path="Submission" type="Submission">
  </node>
  <node path="Submission.experiment" type="Experiment">
  </node>
  <node path="Submission.experiment.name" type="String">
    <constraint op="=" value="${exp.name}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
</im:querylink>

<%-- EXPERIMENTAL FACTORS --%>

     <c:if test="${fn:length(exp.factorTypes) > 0 }"><br></br>
       <c:choose>
         <c:when test="${ fn:length(exp.factorTypes) == 1}">
           <c:out value="Experimental factor: "/>
         </c:when>
         <c:otherwise>
           <c:out value="Experimental factors: "/>
         </c:otherwise>
       </c:choose>
       <%-- whole foreach on one line to avoid spaces before commas --%>
       <c:forEach items="${exp.factorTypes}" var="ft" varStatus="ft_status"><c:if test="${ft_status.count > 1 && !ft_status.last }">, </c:if><c:if test="${ft_status.count > 1 && ft_status.last }"> and </c:if><b>${ft}</b></c:forEach>.
     </c:if>

</td>

<%-- TRACKS --%>
<td class="sorting">
     <c:set var="urlabels" value=""/>
     <c:set var="flylabels" value=""/>
     <c:set var="wormlabels" value=""/>
<c:set var="wormTracksCounter" value="0" />
<c:set var="flyTracksCounter" value="0" />


     <c:forEach items="${tracks[exp.name]}" var="etrack"  varStatus="status">
     <%-- build the url for getting all the labels in this experiment --%>

     <c:set var="organism" value="${etrack.organism}"/>

<c:choose>
<c:when test="${fn:startsWith(organism,'worm')}">
<c:set var="wormTracksCounter" value="${wormTracksCounter +1 }" />
  <c:choose>
  <c:when test="${empty wormlabels}">
     <c:set var="wormlabels" value="${etrack.track}" />
  </c:when>
  <c:otherwise>
     <c:if test="${!fn:contains(wormlabels, etrack.track)}">
       <c:set var="wormlabels" value="${wormlabels}-${etrack.track}" />
     </c:if>
  </c:otherwise>
  </c:choose>
</c:when>
<c:when test="${fn:startsWith(organism,'fly')}">
<c:set var="flyTracksCounter" value="${flyTracksCounter +1}" />
  <c:choose>
  <c:when test="${empty flylabels}">
     <c:set var="flylabels" value="${etrack.track}" />
  </c:when>
  <c:otherwise>
     <c:if test="${!fn:contains(flylabels, etrack.track)}">
       <c:set var="flylabels" value="${flylabels}-${etrack.track}" />
     </c:if>
  </c:otherwise>
  </c:choose>
</c:when>
</c:choose>

</c:forEach>


<c:if test="${flyTracksCounter > 0 }">
<html:link
     href="${WEB_PROPERTIES['gbrowse.prefix']}/fly/?label=${flylabels}" target="_blank" title="View all the tracks for this experiment">
     ${flyTracksCounter}
        <img border="0" class="arrow" src="model/images/fly_gb.png"/>
</html:link>
</c:if>
<%--
<c:if test="${ flyTracksCounter== 1}">
<html:link
     href="${WEB_PROPERTIES['gbrowse.prefix']}/fly/?label=${flylabels}" target="_blank" title="View the track generated for this experiment">
     ${flyTracksCounter} GBrowse track
        <img border="0" class="arrow" src="model/images/fly_gb.png" title="fly"/>
</html:link>
</c:if>
--%>
<c:if test="${ flyTracksCounter > 0 && wormTracksCounter > 0}">
<br></br>
</c:if>

<c:if test="${wormTracksCounter > 0 }">
<html:link
     href="${WEB_PROPERTIES['gbrowse.prefix']}/worm/?label=${wormlabels}" target="_blank" title="View all the tracks for this experiment">
     ${wormTracksCounter}
        <img border="0" class="arrow" src="model/images/worm_gb.png" />
</html:link>
</c:if>

<%--
<c:if test="${ wormTracksCounter== 1}">
<html:link
     href="${WEB_PROPERTIES['gbrowse.prefix']}/worm/?label=${wormlabels}" target="_blank" title="View the track generated for this experiment">
     ${wormTracksCounter} GBrowse track
        <img border="0" class="arrow" src="model/images/worm_gb.png" title="worm"/>
</html:link>
</c:if>
--%>
</td>


<%-- REPOSITORY ENTRIES --%>
<td class="sorting">

     <c:if test="${exp.repositedCount > 0}">

      <c:forEach items="${exp.reposited}" var="rep" varStatus="rep_status">
      <c:choose>
        <c:when test="${rep.value == 1}">
<c:set var="repo" value="${rep.value} entry"> </c:set>
        </c:when>
        <c:otherwise>
<c:set var="repo" value="${rep.value} entries"> </c:set>
        </c:otherwise>
      </c:choose>


<b>${rep.key}</b>:
<im:querylink text="${repo}" skipBuilder="true">
<query name="" model="genomic" view="DatabaseRecord.database DatabaseRecord.accession DatabaseRecord.description DatabaseRecord.url DatabaseRecord.submissions.DCCid" sortOrder="DatabaseRecord.database asc" constraintLogic="A and B">
  <node path="DatabaseRecord" type="DatabaseRecord">
  </node>
  <node path="DatabaseRecord.submissions" type="Submission">
  </node>
  <node path="DatabaseRecord.submissions.experiment" type="Experiment">
  </node>
  <node path="DatabaseRecord.submissions.experiment.name" type="String">
    <constraint op="=" value="${exp.name}" description="" identifier="" code="A">
    </constraint>
  </node>
  <node path="DatabaseRecord.database" type="String">
    <constraint op="=" value="${rep.key}" description="" identifier="" code="B">
    </constraint>
  </node>
</query>
</im:querylink>

      <br></br>
      </c:forEach>
     </c:if>
</td>



<%-- FEATURES --%>
<td class="sorting">
      <c:forEach items="${exp.featureCountsRecords}" var="fc" varStatus="fc_status">
     <nobr>
       <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=results&experiment=${exp.name}&feature=${fc.featureType}"
        title="View all ${fc.featureType}s">${fc.featureCounts}&nbsp;${fc.featureType}
            </html:link>

    <img class="exportDiv" style="position:relative; top:3px;" border="0" src="model/images/download.png" title="export data" height="16" width="16"/>

    <ul class="contextMenu">
        <li class="tab"><a href="#/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.featureType}&format=tab"
            title="Download in tab separated value format">TAB</a></li>
        <li class="csv"><a href="#/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.featureType}&format=csv"
            title="Download in comma separated value format">CSV</a></li>

        <c:choose>
        <c:when test="${!empty exp.unlocated && fn:contains(exp.unlocated, fc.featureType)}">
        </c:when>
        <c:otherwise>

            <li class="gff"><a href="#/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.featureType}&format=gff3"
                title="Download in GFF3 format">GFF3</a></li>
            <li class="gff">(<a href="#/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.featureType}&format=gff3&UCSC"
                title="Download in GFF3 format">for UCSC</a>)</li>
            <li class="seq"><a href="#/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.featureType}&format=sequence"
                title="Download the sequences">FASTA</a></li>

        </c:otherwise>
        </c:choose>
    </ul>

&nbsp;
     </nobr>
      </c:forEach>
<p/>
</td>


</tr>


</c:forEach>
</table>
<table cellpadding="0" cellspacing="0" border="0" class="topBar hints" width="95%">
<tr><td align="right"><a href="/${WEB_PROPERTIES['webapp.path']}/projectsSummary.do">Switch to Experimentss View</a></td></tr>
</table>

</div>