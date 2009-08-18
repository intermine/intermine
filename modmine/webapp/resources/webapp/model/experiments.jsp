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

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Experiment</th>
    <th>Project</th>
    <th>Submissions</th>
  </tr>
  
  
  
  
  <c:forEach items="${subs}" var="item">
    <tr>
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>
      <td><html:link href="${item.key.project.url}">
 ${item.key.project.surnamePI}
    </html:link>

<td>

      <c:forEach items="${counts}" var="nr">
        <c:if test="${nr.key.name eq item.key.name}">
          <c:set var="nrSubs" value="${nr.value}" />
        </c:if>
      </c:forEach> 

      <c:choose>
        <c:when test="${nrSubs eq 0}">
        -
        </c:when>
        <c:when test="${nrSubs gt 0}">
          <im:querylink text="${nrSubs} submissions " showArrow="true" skipBuilder="true">
<query name="" model="genomic" view="Experiment.submissions.DCCid Experiment.submissions.title Experiment.submissions:experimentalFactors.name Experiment.submissions:experimentalFactors.type">
  <node path="Experiment" type="Experiment">
  </node>
  <node path="Experiment.submissions" type="Submission">
  </node>
  <node path="Experiment.name" type="String">
    <constraint op="=" value="${item.key.name}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
          </im:querylink>
        </c:when>
      </c:choose>
      
  </c:forEach>



<%--
      <table cellpadding="0" cellspacing="0" border="0" class="internal">
      <c:forEach items="${item.value}" var="prov">
        
        <tr><td>
        <html:link
          href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${prov.id}">
 ${prov.dCCid}
    </html:link>
    <td>
    ${prov.title}
      </c:forEach>
      </table>
--%>
            
  </tr>


</table>
</div>

<%--
<div class="body">

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
	<tr>
		<th>Project</th>
		<th>Title</th>
		<th>Principal Investigator</th>
    <th>Labs</th>
    <th>Submissions</th>
	</tr>
	<c:forEach items="${labs}" var="item">
		<tr>
			<td><html:link
				href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>
			<td><html:link href="${item.key.url}">
 ${item.key.title}
    </html:link>
			<td>${item.key.namePI} ${item.key.surnamePI} <br>


			<td><table cellpadding="0" cellspacing="0" border="0" class="internal">
			<c:forEach items="${item.value}" var="prov">
				
				<tr><td>
				<html:link
					href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${prov.id}">
 ${prov.name}
    </html:link>
    <td>
    ${prov.affiliation}
			</c:forEach>
			</table>
			
      <td>
			
			<c:forEach items="${counts}" var="nr">
				<c:if test="${nr.key.surnamePI eq item.key.surnamePI}">
					<c:set var="nrSubs" value="${nr.value}" />
				</c:if>
			</c:forEach> 

			<c:choose>
				<c:when test="${nrSubs eq 0}">
        -
        </c:when>
				<c:when test="${nrSubs gt 0}">
					<im:querylink text="${nrSubs} submissions " skipBuilder="true">
						<query name="" model="genomic"
							view="Project.labs.submissions.title 
							Project.labs.submissions.design 
							Project.labs.submissions.experimentalFactors.type 
							Project.labs.submissions.experimentalFactors.name"
							sortOrder="Project.labs.submissions.title">
						<node path="Project" type="Project">
						</node>
						<node path="Project.surnamePI" type="String">
						<constraint op="=" value="${item.key.surnamePI}" description=""
							identifier="" code="A">
						</constraint>
						</node>
						</query>
					</im:querylink>
				</c:when>
			</c:choose>
			
	</c:forEach>
	</tr>

</table>
</div>
--%>