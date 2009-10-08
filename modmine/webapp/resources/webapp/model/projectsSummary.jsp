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

<div class="body">
<em>modENCODE projects and related experiments, submissions and features generated:</em>

  <table cellpadding="0" cellspacing="0" border="0" class="dbsources">

<c:forEach items="${experiments}" var="proj">
 <c:forEach items="${proj.value}" var="exp"  varStatus="status">
<c:set var="expCount" value="${fn:length(proj.value)}"></c:set>

  <tr>
<c:if test="${status.first}">
  <td rowspan="${expCount}">
    <c:forEach items="${exp.organisms}" var="organism" varStatus="orgStatus">
      <c:if test="${organism eq 'D. melanogaster'}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
      </c:if>
      <c:if test="${organism eq 'C. elegans'}"> 
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
      </c:if>
    </c:forEach> 
  </td>
  <td rowspan="${expCount}">
  <b>${proj.key}</b>
  <br></br>PI: <c:out value="${exp.pi}"></c:out>
  </td>
  </c:if>
  
  <td><b><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}">${exp.name}</html:link>
</b>
</td>
<td>
This experiment is described in 

          <im:querylink text="${exp.submissionCount} submissions " showArrow="false" skipBuilder="true">
<query name="" model="genomic" view="Experiment.submissions.DCCid Experiment.submissions.title Experiment.submissions:experimentalFactors.name Experiment.submissions:experimentalFactors.type">
  <node path="Experiment" type="Experiment">
  </node>
  <node path="Experiment.submissions" type="Submission">
  </node>
  <node path="Experiment.name" type="String">
    <constraint op="=" value="${exp.name}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
</im:querylink>.

     <c:if test="${fn:length(exp.featureCounts) > 0 }"> 
It generates 
      <c:forEach items="${exp.featureCounts}" var="fc" varStatus="status">
     <c:if test="${status.count > 1 && !status.last }">, </c:if> 
     <c:if test="${status.count > 1 && status.last }"> and </c:if> 
     <html:link
href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=results&experiment=${exp.name}&feature=${fc.key}">${fc.value} ${fc.key}s
</html:link>
      </c:forEach>
.
      </c:if>     

<html:link
        href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}">More details...</html:link>

</td>





  </tr>
  
</c:forEach>
</c:forEach>
  </table>


</div>









<%--
<div class="body">
<em>modENCODE projects and related experiments, submissions and features generated:</em>

  <table cellpadding="0" cellspacing="0" border="0" class="dbsources">

<c:forEach items="${experiments}" var="proj">
 <c:forEach items="${proj.value}" var="exp"  varStatus="status">
<c:set var="expCount" value="${fn:length(proj.value)}"></c:set>

  <tr>
<c:if test="${status.first}">
  <td rowspan="${expCount}">
    <c:forEach items="${exp.organisms}" var="organism" varStatus="orgStatus">
      <c:if test="${organism eq 'D. melanogaster'}"> 
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
      </c:if>
      <c:if test="${organism eq 'C. elegans'}"> 
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
      </c:if>
    </c:forEach> 
  </td>
  <td rowspan="${expCount}">
  <b>${proj.key}</b>
  <br></br>PI: <c:out value="${exp.pi}"></c:out>
  </td>
  </c:if>
  <td><b><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}">${exp.name}</html:link>
</b>
<br>
          <im:querylink text="${exp.submissionCount} submissions " showArrow="true" skipBuilder="true">
<query name="" model="genomic" view="Experiment.submissions.DCCid Experiment.submissions.title Experiment.submissions:experimentalFactors.name Experiment.submissions:experimentalFactors.type">
  <node path="Experiment" type="Experiment">
  </node>
  <node path="Experiment.submissions" type="Submission">
  </node>
  <node path="Experiment.name" type="String">
    <constraint op="=" value="${exp.name}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
          </im:querylink>
</td>
<td>
     <table cellpadding="0" cellspacing="0" border="0" class="internal" width="95%">
      <c:forEach items="${exp.featureCounts}" var="fc" varStatus="status">
          <tr>
            <td>${fc.key}</td>
            <td align="right">
              <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=results&experiment=${exp.name}&feature=${fc.key}">${fc.value}</html:link>
            </td>
            <td align="right">
              export: 
               <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.key}&format=tab">TAB</html:link>            
              <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.key}&format=csv">CSV</html:link>           
             <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=experiment&action=export&experiment=${exp.name}&feature=${fc.key}&format=gff3">GFF3</html:link>
            </td>
          </tr>
      </c:forEach>
    </table>
</td>
  </tr>
</c:forEach>
</c:forEach>
  </table>
</div>
--%>


<!-- links to all subs -->

<table cellspacing="4"><tr>

<td>    
<im:querylink text="Fly" showArrow="true" skipBuilder="true">
  <query name="" model="genomic"
    view="Submission.title Submission.DCCid Submission.design Submission.factorName Submission.publicReleaseDate"
    sortOrder="Submission.title">
  <node path="Submission" type="Submission">
  </node>
  <node path="Submission.organism" type="Organism">
  <constraint op="LOOKUP" value="Drosophila melanogaster" description=""
    identifier="" code="A" extraValue="">
  </constraint>
  </node>
  </query>
</im:querylink>
    </td>

<td>
<im:querylink text="Worm" showArrow="true" skipBuilder="true">
  <query name="" model="genomic"
    view="Submission.title Submission.DCCid Submission.design Submission.factorName Submission.publicReleaseDate"
    sortOrder="Submission.title">
  <node path="Submission" type="Submission">
  </node>
  <node path="Submission.organism" type="Organism">
  <constraint op="LOOKUP" value="Caenorhabditis elegans" description=""
    identifier="" code="A" extraValue="">
  </constraint>
  </node>
  </query>
</im:querylink>
</td>

<td>    
<im:querylink text="All submissions" showArrow="true" skipBuilder="true">
  <query name="" model="genomic"
    view="Submission.title Submission.DCCid Submission.design Submission.factorName Submission.publicReleaseDate"
    sortOrder="Submission.title">
  <node path="Submission" type="Submission">
  </node>
  </query>
</im:querylink>
    </td>


  </tr></table>

