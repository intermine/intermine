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

<td>
<table cellpadding="0" cellspacing="0" border="0" class="internal">
        <c:forEach items="${sub.value}" var="fc" varStatus="status">
          <tr>
<%--done here because not sure if possible to do outer join in java --%>
            <td><c:choose>

<%-- UNCOMMENT to see the chromosome

              <c:when test='${fc.key eq "Chromosome"}'>
                <im:querylink text="${fc.value}" skipBuilder="true">
                  <query name="" model="genomic"
                    view="Chromosome.dataSets.title Chromosome.primaryIdentifier"
                    sortOrder="Chromosome.primaryIdentifier asc">
                  <node path="Chromosome" type="Chromosome">
                  </node>
                  <node path="Chromosome.dataSets" type="DataSet">
                  </node>
                  <node path="Chromosome.dataSets.title" type="String">
                  <constraint op="=" value="${sub.key.title}" description=""
                    identifier="" code="A">
                  </constraint>
                  </node>
                  </query>
                </im:querylink>
              </c:when>
--%>

              <c:when test='${fc.key eq "Chromosome"}'>
                <td>
                <td align="right">
              </c:when>

              <c:when test='${fc.key eq "-"}'>
              <!-- added because at the moment these features are without chromosome location-->
                <td>${fc.key}
                <td align="right">
              </c:when>

              <c:when test='${fc.key eq "EST" || fc.key eq "MNRA"}'>
                <td>${fc.key}
                <td align="right">
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
                  <constraint op="=" value="${sub.key.title}" description=""
                    identifier="" code="A">
                  </constraint>
                  </node>
                  </query>
                </im:querylink>
              </c:when>




              <c:otherwise>
                <td>${fc.key}
                <td align="right">
                <im:querylink text="${fc.value}" skipBuilder="true">
                  <query name="" model="genomic"
                    view="${fc.key}.dataSets.title ${fc.key}.secondaryIdentifier ${fc.key}.length 
                  ${fc.key}.chromosomeLocation.object.primaryIdentifier ${fc.key}.chromosomeLocation.start ${fc.key}.chromosomeLocation.end"
                    sortOrder="${fc.key}.secondaryIdentifier asc">
                  <node path="${fc.key}" type="${fc.key}">
                  </node>
                  <node path="${fc.key}.dataSets" type="DataSet">
                  </node>
                  <node path="${fc.key}.dataSets.title" type="String">
                  <constraint op="=" value="${sub.key.title}" description=""
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
      <td><html:link
        href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>
    --%>
    
