<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>

<!-- submissionProtocolsDisplayer.jsp -->

<tiles:importAttribute />

<html:xhtml />

<style type="text/css">
div#submissionProtocols h3 {
  color: black;
  margin-bottom: 20px;
}
</style>

<div class="body">

<%--========== --%>

<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        jQuery("#sis").click(function () {
           if(jQuery("#protocols").is(":hidden")) {
             jQuery("#co").attr("src", "images/disclosed.gif");
           } else {
             jQuery("#co").attr("src", "images/undisclosed.gif");
           }
           jQuery("#protocols").toggle("slow");
        });
    })
</script>

<html:link linkName="#" styleId="sis" style="cursor:pointer">
    <h3>
        Protocols used for this submission (click to toggle)
        <img src="images/undisclosed.gif" id="co">
    </h3>
</html:link>

<script type="text/javascript" charset="utf-8">

jQuery(document).ready(function () {
 jQuery(".tbox").children('doopen').show();
 jQuery(".tbox").children('doclose').hide();

  jQuery('.tbox').click(function () {
  var text = jQuery(this).children('doclose');

  if (text.is(':hidden')) {
       jQuery(this).children('doclose').show("slow");
     } else {
         jQuery(this).children('doopen').show("slow");
      }
   });

  jQuery("doopen").click(function(){
     jQuery(this).toggle("slow");
     return true;
    });

  jQuery("doclose").click(function(){
      jQuery(this).toggle("slow");
        return true;
    });


  });

</script>

<div id="protocols" style="display: block">

    <table width="100%" cellpadding="0" cellspacing="0" border="0" class="results">
        <tr>
            <th>Type</th>
            <th>Protocol</th>
            <th>Wiki</th>
            <th width="50%" >Description</th>
        </tr>
        <c:forEach items="${protocols}" var="prot" varStatus="p_status">
            <c:set var="pRowClass">
                <c:choose>
                    <c:when test="${p_status.count % 2 == 1}">
                        odd-alt
                    </c:when>
                    <c:otherwise>
                        even-alt
                    </c:otherwise>
                </c:choose>
            </c:set>

          <tr class="<c:out value="${pRowClass}"/>">
              <td>${prot.type}</td>
              <td>
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${prot.id}">
                    ${prot.name}
                </html:link>
              </td>
              <td>
                  <a href="${prot.wikiLink}" class="value extlink"></a>
              </td>
              <td class="description">
                  <div class="tbox">
                      <doopen>
                          <img src="images/undisclosed.gif">
                          <i>${fn:substring(prot.description,0,80)}... </i>
                      </doopen>
                      <doclose>
                          <img src="images/disclosed.gif">
                          <i>${prot.description}</i>
                      </doclose>
                  </div>
              </td>
          </tr>
        </c:forEach>
    </table>

</div>

<%---========= --%>

<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function () {
        jQuery("#bro").click(function () {
           if(jQuery("#submissionProtocols").is(":hidden")) {
             jQuery("#oc").attr("src", "images/disclosed.gif");
           } else {
             jQuery("#oc").attr("src", "images/undisclosed.gif");
           }
           jQuery("#submissionProtocols").toggle("slow");
        });
    })
</script>

<c:choose>
    <c:when test="${fn:length(pagedResults.rows) >= 1}">
        <table cellspacing="0" width="100%">
            <tr>
                <TD colspan=2 align="left" style="padding-bottom:10px">
                    <c:set var="dccNumber" value="${fn:substringAfter(DCCid,'modENCODE_')}"/>
                    <c:set var="geoUrl" value="http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" />
                    <c:set var="srrUrl" value="http://www.ncbi.nlm.nih.gov/sites/entrez?db=sra&report=full&term=" />

                    <html:link linkName="#" styleId="bro" style="cursor:pointer">
                        <h3>Browse metadata for this submission (click to toggle)<img src="images/undisclosed.gif" id="oc"></h3>
                    </html:link>

                    <div id="submissionProtocols" style="display: block">
                        <table cellpadding="0" cellspacing="0" border="0" class="results">
                            <tr>
                              <th>Step</th>
                              <th colspan="2">Inputs</th>
                              <th>Applied Protocol</th>
                              <th colspan="2">Outputs</th>
                            </tr>
                            <c:set var="prevStep" value="0" />

                            <tbody>
                                <c:forEach var="row" items="${pagedResults.rows}" varStatus="status">
                                    <c:set var="rowClass">
                                      <c:choose>
                                        <c:when test="${status.count % 2 == 1}">odd</c:when>
                                        <c:otherwise>even</c:otherwise>
                                      </c:choose>
                                    </c:set>

                                    <c:forEach var="subRow" items="${row}" varStatus="multiRowStatus">
                                        <im:instanceof instanceofObject="${subRow[0]}" instanceofClass="org.intermine.api.results.flatouterjoins.MultiRowFirstValue" instanceofVariable="isFirstValue"/>
                                        <c:if test="${isFirstValue == 'true'}">
                                            <c:set var="step" value="${subRow[0].value.field}" scope="request"/>
                                        </c:if>
                                        <c:set var="stepClass">
                                            <c:choose>
                                                <c:when test="${step % 2 == 1}">stepO</c:when>
                                                <c:otherwise>stepE</c:otherwise>
                                            </c:choose>
                                        </c:set>

                                        <tr class="<c:out value="${stepClass}${rowClass}"/>">
                                            <c:set var="output" value="true"/>
                                            <c:forEach var="column" items="${pagedResults.columns}" varStatus="status2">
                                                <im:instanceof instanceofObject="${subRow[column.index]}" instanceofClass="org.intermine.api.results.flatouterjoins.MultiRowFirstValue" instanceofVariable="isFirstValue"/>
                                                <c:if test="${isFirstValue == 'true'}">
                                                    <c:set var="resultElement" value="${subRow[column.index].value}" scope="request"/>
                                                    <c:choose>
                                                        <c:when test="${column.index == 0}">
                                                            <c:choose>
                                                                <c:when test="${resultElement.field != prevStep}">
                                                                    <td rowspan="${subRow[column.index].rowspan}" >${resultElement.field}</td>
                                                                    <c:set var="prevStep" value="${resultElement.field}"/>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <c:set var="output" value="true"/>
                                                                    <td rowspan="${subRow[column.index].rowspan}" >${resultElement.field}</td>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </c:when>
                                                        <c:when test="${column.index == 1  || column.index == 5}">
                                                            <c:if test="${fn:startsWith(resultElement.field,'Anonymous Datum')}">
                                                                <td colspan="2" rowspan="${subRow[column.index].rowspan}" >
                                                                    <c:choose>
                                                                        <c:when test="${column.index == 1}">
                                                                            <i><c:out value="output from step ${prevStep -1} -->"/></i>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <i><c:out value="--> next Step"/></i>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </td>
                                                                <c:set var="output" value="false"/>
                                                            </c:if>
                                                            <c:if test="${fn:length(fn:substringBefore(resultElement.field,'File')) gt 0}">
                                                                <c:set var="output" value="true"/>
                                                                <c:set var="isFile" value="true" />
                                                            </c:if>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:if test="${column.index == 4}">
                                                                <c:set var="output" value="true"/>
                                                            </c:if>
                                                            <c:if test="${output}">
                                                                <td id="cell,${status2.index},${status.index},${subRow[column.index].value.type}" rowspan="${subRow[column.index].rowspan}" class="<c:out value="${stepClass}${rowClass}"/>">
                                                                    <c:choose>
                                                                        <c:when test="${fn:startsWith(fn:trim(resultElement.field), 'http://') || fn:startsWith(fn:trim(resultElement.field), 'ftp://')}">
                                                                            <a href="${resultElement.field}" class="value extlink">
                                                                                <c:set var="elements" value="${fn:split(resultElement.field,'/')}" />
                                                                                <c:out value="${elements[fn:length(elements) - 1]}" />
                                                                            </a>
                                                                        </c:when>
                                                                        <c:when test="${fn:startsWith(fn:trim(resultElement.field), 'GSM')}">
                                                                            <a href="${geoUrl}${resultElement.field}" class="value extlink">
                                                                            <c:out value="${resultElement.field}" />
                                                                        </c:when>
                                                                        <c:when test="${fn:startsWith(fn:trim(resultElement.field), 'SRR')}">
                                                                         <a href="${srrUrl}${resultElement.field}" class="value extlink">
                                                                         <c:out value="${resultElement.field}" />
                                                                        </c:when>
                                                                        <c:when test="${isFile}">
                                                                            <c:out value="${resultElement.field}" />
                                                                  <%-- </td> --%>
                                                                            <c:set var="isFile" value="false" />
                                                                            <c:set var="doLink" value="true" />
                                                                        </c:when>
                                                                        <c:when test="${doLink}">
                                                                            <c:forEach items="${files}" var="file" varStatus="f_status">
                                                                                <c:if test="${resultElement.field == file.name}">
                                                                                    <c:set var="url" value="${file.url}" />
                                                                                </c:if>
                                                                            </c:forEach>
                                                                            <a href="${url}" title="Download file ${resultElement.field}" class="value extlink">
                                                                                <c:out value="${resultElement.field}" />
                                                                            </a>
                                                                    <%-- </td> --%>
                                                                            <c:set var="doLink" value="false" />
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <tiles:insert name="objectView.tile" />
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </td>
                                                            </c:if>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:if>
                                            </c:forEach>
                                        </tr>
                                    </c:forEach>
                                </c:forEach>
                            </tbody>
                        </table>
                        <br/>
                    </div>
                </TD>
            </tr>
        </table>
    </c:when>
    <c:otherwise>
        <%-- too many rows: just do a normal query --%>
        <im:querylink text="<h3>Browse metadata for this submission (click to view)</h3>"  skipBuilder="true">
        <%--<im:querylink text="${nrSubs} submissions " skipBuilder="true">--%>
            <query name="" model="genomic" view="AppliedProtocol.step AppliedProtocol:inputs.name AppliedProtocol:protocol.name AppliedProtocol:inputs.value AppliedProtocol:outputs.name AppliedProtocol:outputs.value" sortOrder="AppliedProtocol.step asc">
              <node path="AppliedProtocol" type="AppliedProtocol">
              </node>
              <node path="AppliedProtocol.submission" type="Submission">
              </node>
              <node path="AppliedProtocol.submission.DCCid" type="String">
                <constraint op="=" value="${DCCid}" description="" identifier="" code="A"></constraint>
              </node>
            </query>
        </im:querylink>
    </c:otherwise>
</c:choose>

</div>

<!-- /submissionProtocolsDisplayer.jsp -->
