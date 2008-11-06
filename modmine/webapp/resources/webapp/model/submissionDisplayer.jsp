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
  margin-bottom: 20px;
}

</style>

<div class="body">
  <div id="submissionLabName">
    <h3>
      <b>Lab:</b> <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${object.lab.id}">${object.lab.name}</html:link>
      ${object.lab.affiliation}
    </h3>
    <div id="submissionProject">
      <b>Project:</b> <html:link href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${object.lab.project.id}">${object.lab.project.name}</html:link>
    </div>
    <table cellpadding="0" cellspacing="0" border="0" class="results">
      <tr>
        <th>Feature type</th>
        <th>count</th>
      </tr>
      <c:forEach items="${featureCounts}" var="fc" varStatus="status">
        <c:if test='${fc.key != "Chromosome"}'>
          <tr>
            <td>${fc.key}</td>
            <td align="right">
              <c:choose>
                <c:when test='${fc.key eq "EST" || fc.key eq "MNRA"}'>
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
                        <constraint op="=" value="${object.title}" description=""
                                    identifier="" code="A">
                        </constraint>
                      </node>
                    </query>
                  </im:querylink>
                </c:when>

                <c:otherwise>
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
                        <constraint op="=" value="${object.title}" description=""
                                    identifier="" code="A">
                        </constraint>
                      </node>
                    </query>
                  </im:querylink>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </c:if>
      </c:forEach>
      <!-- end submission loop -->
    </table>
  </div>
</div>
