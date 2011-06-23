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

     <html:form action="/submissions" method="post" enctype="multipart/form-data" >

     <h3>Filter submission:</h3>

Organism:    <html:select styleId="typeSelector" property="organism">
          <html:option value="fly">fly</html:option>
          <html:option value="worm">worm</html:option>
        </html:select>

    <html:submit styleId="submitBag" property="submissions">Filter</html:submit>

       <br />

     </html:form>


<table cellpadding="0" cellspacing="0" border="0" class="sortable-onload-2 rowstyle-alt no-arrow">
<tr>
    <th class="sortable">DCC id</th>
    <th class="sortable">name</th>
    <th>date</th>
    <th class="sortable">Dev stage</th>
    <th>features</th>
    <th></th>
  </tr>

  <c:forEach items="${subs}" var="subCounts">
    <c:set var="sub" value="${subCounts.key}"></c:set>
    <tr>
      <td class="sorting"><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${subCounts.key.id}"><c:out value="${sub.dCCid}"></c:out></html:link></td>
      <td class="sorting"><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${subCounts.key.id}"><c:out value="${sub.title}"></c:out></html:link></td>
      <td class="sorting"><fmt:formatDate value="${sub.publicReleaseDate}" type="date"/></td>

       <td class="sorting">
      <c:forEach items="${sub.developmentalStages}" var="devStage">
                        <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${devStage.id}"><c:out value="${devStage.name}"/></html:link>
                        <span class="tinylink">
                        <im:querylink text="ALL" skipBuilder="true">
                         <query name="" model="genomic"
                           view="Submission.DCCid Submission.project.surnamePI Submission.title Submission.experimentType Submission.properties.type Submission.properties.name"
                           sortOrder="Submission.experimentType asc">
                      <node path="Submission.properties.type" type="String">
                        <constraint op="=" value="${devStage.type}" description=""
                                    identifier="" code="A">
                        </constraint>
                      </node>
                      <node path="Submission.properties.name" type="String">
                        <constraint op="=" value="${devStage.name}" description=""
                                    identifier="" code="B">
                        </constraint>
                      </node>
                      <node path="Submission.organism.taxonId" type="Integer">
                        <constraint op="=" value="${sub.organism.taxonId}" description=""
                                    identifier="" code="C">
                        </constraint>
                      </node>
                    </query>
                  </im:querylink>
                  </span>
            </c:forEach>
      </td>
      <td class="sorting">
        <c:if test="${!empty subCounts.value}">
            <div class="submissionFeatures">
            <table cellpadding="0" cellspacing="0" border="0" class="features">
            <c:forEach items="${subCounts.value}" var="fc" varStatus="rowNumber">
                <c:set var="class" value=""/>
                <c:if test="${rowNumber.first}">
                    <c:set var="class" value="firstrow"/>
                </c:if>
                <tr>
                    <td class="firstcolumn ${class}">${fc.key}:<html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=results&submission=${sub.dCCid}&feature=${fc.key}">${fc.value}</html:link></td>
                    <td class="${class}" align="right">export:
               <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=tab">TAB</html:link>
               <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=csv">CSV</html:link>
               <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=gff3">GFF3</html:link>
               (<html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=export&submission=${sub.dCCid}&feature=${fc.key}&format=gff3&UCSC">for UCSC</html:link>)
                    </td>
                    <td class="${class}" align="right">
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/features.do?type=submission&action=list&submission=${sub.dCCid}&feature=${fc.key}"> CREATE LIST</html:link>
                    </td>
                </tr>
            </c:forEach>
            </table>
            </div>
        </c:if>
    </td>

        <td>
        <c:if test="${!empty fly}">
                        <html:link
                            href="${WEB_PROPERTIES['gbrowse.prefix']}/fly/?ds=${sub.dCCid}"
                            target="_blank">
                            <html:img src="model/images/dgb_vs.png" title="View in GBrowse" />
                        </html:link></c:if>

        <c:if test="${!empty worm}">
                        <html:link
                            href="${WEB_PROPERTIES['gbrowse.prefix']}/worm/?ds=${sub.dCCid}"
                            target="_blank">
                            <html:img src="model/images/wgb_vs.png" title="View in GBrowse" />
                        </html:link>
                    </c:if>

          </td>



  </tr>
  </c:forEach>

</table>
</div>

