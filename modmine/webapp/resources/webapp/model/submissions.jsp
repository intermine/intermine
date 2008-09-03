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
    <th>Submission</th>
    <th>Date</th>
    <th>Lab</th>
    <th>Affiliation</th>
    <th>Project</th>
    <th>Features count</th>
  </tr>
  
  <c:forEach items="${subs}" var="sub">
    <tr>
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.key.id}">
 ${sub.key.title}
    </html:link>

			<td><fmt:formatDate value="${sub.key.publicReleaseDate}"
				type="date"/>

<td> <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.key.lab.id}">
 ${sub.key.lab.name}
    </html:link>

<td> ${sub.key.lab.affiliation}
<td> <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${sub.key.lab.project.id}">
${sub.key.lab.project.name}
    </html:link>

<%--
<html:link
        href="${sub.key.lab.project.url}">
->
    </html:link>
--%>

<%--

        type="date" pattern="yyyy-MM-dd" />


        <td>${sub.key.publicReleaseDate}

          <im:querylink text="${sub.key.title}" skipBuilder="true">
            <query name="" model="genomic"
              view="Submission.title Submission.design Submission.factorName Submission.factorType"
              >
            <node path="Submission" type="Submission">
            </node>
            <node path="Submission.title" type="String">
            <constraint op="=" value="${sub.key.title}" description=""
              identifier="" code="A">
            </constraint>
            </node>
            </query>
          </im:querylink>
          --%>
          

<td>
<table cellpadding="0" cellspacing="0" border="0" class="internal">
   <c:forEach items="${sub.value}" var="fc" varStatus="status">
<tr><td>${fc.key} 

<td align="right"><c:choose>
							<c:when test="${fc.key eq \"Chromosome\"}">
								<im:querylink text="${fc.value}" skipBuilder="true">
									<query name="" model="genomic"
										view="Chromosome.dataSets.title Chromosome.primaryIdentifier"
										sortOrder="Chromosome.primaryIdentifier asc">
									<node path="Chromosome" type="Chromosome">
									</node>
									<node path="Chromosome.dataSets" type="DataSet">
									</node>
									<node path="Chromosome.dataSets.title" type="String">
									<constraint op="=" value="${sub.key}" description=""
										identifier="" code="A">
									</constraint>
									</node>
									</query>
								</im:querylink>
							</c:when>

							<c:otherwise>
									<im:querylink text="${fc.value}" skipBuilder="true">
										<query name="" model="genomic"
											view="${fc.key}.dataSets.title ${fc.key}.primaryIdentifier ${fc.key}.secondaryIdentifier ${fc.key}.length 
                  ${fc.key}.chromosomeLocation.object.primaryIdentifier ${fc.key}.chromosomeLocation.start ${fc.key}.chromosomeLocation.end"
											sortOrder="${fc.key}.primaryIdentifier asc">
										<node path="${fc.key}" type="${fc.key}">
										</node>
										<node path="${fc.key}.dataSets" type="DataSet">
										</node>
										<node path="${fc.key}.dataSets.title" type="String">
										<constraint op="=" value="${sub.key}" description=""
											identifier="" code="A">
										</constraint>
										</node>
										</query>
									</im:querylink>

</c:otherwise>
						</c:choose>

</td>


</c:forEach>
</td>
</tr>
</table>
</c:forEach>
</table>

</div>


<%--

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Submission</th>
    <th>Feature type</th>
    <th>Number of objects</th>
  </tr>
  
<tr><td>${sub}
</td></tr>

  <c:forEach items="${features}" var="item">
    <tr>
<td>${sub}
      <td>${item.key} </td>
      <td>${item.value} </td>
  </tr>
  </c:forEach>
</table>


<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Submission</th>
    <th>Features count</th>
  </tr>
  
  <c:forEach items="${subs}" var="sub">
    <tr>
<td>
          <im:querylink text="${sub.key}" skipBuilder="true">
            <query name="" model="genomic"
              view="Submission.title Submission.design Submission.factorName Submission.factorType"
              >
            <node path="Submission" type="Submission">
            </node>
            <node path="Submission.title" type="String">
            <constraint op="=" value="${sub.key}" description=""
              identifier="" code="A">
            </constraint>
            </node>
            </query>
          </im:querylink>
<td>
<table cellpadding="0" cellspacing="0" border="0" >
   <c:forEach items="${sub.value}" var="fc" varStatus="status">
<tr><td>${fc.key} 

<td>
      <c:choose>
        <c:when test="${fc.key eq \"EST\"}">
          <im:querylink text="${fc.value}" skipBuilder="true">
                <query name="" model="genomic"
                  view="EST.dataSets.title EST.primaryIdentifier EST.secondaryIdentifier EST.length 
                  EST.chromosomeLocation.object.primaryIdentifier EST.chromosomeLocation.start EST.chromosomeLocation.end"
                  sortOrder="EST.primaryIdentifier asc">
                <node path="EST" type="EST">
                </node>
                <node path="EST.dataSets" type="DataSet">
                </node>
                <node path="EST.dataSets.title" type="String">
                <constraint op="=" value="${sub.key}"
                  description="" identifier="" code="A">
                </constraint>
                </node>
                </query>
          </im:querylink>
        </c:when>
        <c:when test="${fc.key eq \"MRNA\"}">
          <im:querylink text="${fc.value}" skipBuilder="true">
                <query name="" model="genomic"
                  view="MRNA.dataSets.title MRNA.primaryIdentifier MRNA.secondaryIdentifier MRNA.length 
                  MRNA.chromosomeLocation.object.primaryIdentifier MRNA.chromosomeLocation.start MRNA.chromosomeLocation.end"
                  sortOrder="MRNA.primaryIdentifier asc">
                <node path="MRNA" type="MRNA">
                </node>
                <node path="MRNA.dataSets" type="DataSet">
                </node>
                <node path="MRNA.dataSets.title" type="String">
                <constraint op="=" value="${sub.key}"
                  description="" identifier="" code="A">
                </constraint>
                </node>
                </query>
          </im:querylink>
        </c:when>


        <c:when test="${fc.key eq \"Chromosome\"}">
          <im:querylink text="${fc.value}" skipBuilder="true">
<query name="" model="genomic" view="Chromosome.dataSets.title Chromosome.primaryIdentifier" sortOrder="Chromosome.primaryIdentifier asc">
  <node path="Chromosome" type="Chromosome">
  </node>
  <node path="Chromosome.dataSets" type="DataSet">
  </node>
  <node path="Chromosome.dataSets.title" type="String">
    <constraint op="=" value="${sub.key}" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
          </im:querylink>
        </c:when>

<c:otherwise>
${fc.value}
</c:otherwise>
</c:choose>

</td>


</c:forEach>
</td>
</tr>
</table>
</c:forEach>
</table>










--%>


<%--
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>
    --%>
