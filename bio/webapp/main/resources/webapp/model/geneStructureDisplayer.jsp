<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- geneStructureDisplayer.jsp -->

<div class="collection-of-collections" id="gene-structure-displayer">

<style>
#gene-structure-displayer div.switchers a.switcher:not(:last-child):after { content:",", display:inline-block; width:2px;height:10px; }
</style>

<div class="header">
  <c:choose>
  <c:when test="${gene.id == actualId}">
   <h3>Gene models - <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/></h3>
  </c:when>
  <c:otherwise>
    <h3>Gene models - <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/></h3>
  </c:otherwise>
  </c:choose>

  <p>
    <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
    Gene models
  </p>

  <c:choose>
    <c:when test="${!empty gene.transcripts}">
      <div class="switchers">
        <a href="#" id="transcripts" class="switcher">Transcripts</a>: ${fn:length(gene.transcripts)}&nbsp;
        <c:if test="${settings.hasExons}">
          <c:set var="count" value="0" />
          <c:forEach items="${geneModels}" var="geneModel">
            <c:set var="count" value="${fn:length(geneModel.exons) + count}" />
          </c:forEach>
          <c:if test="${count > 0}">
            <a href="#" id="exons" class="switcher">Exons</a>: ${count}&nbsp;
          </c:if>
        </c:if>
        <c:if test="${settings.hasIntrons}">
          <c:set var="count" value="0" />
          <c:forEach items="${geneModels}" var="geneModel">
            <c:set var="count" value="${fn:length(geneModel.introns) + count}" />
          </c:forEach>
          <c:if test="${count > 0}">
            <a href="#" id="introns" class="switcher">Introns</a>: ${count}&nbsp;
          </c:if>
        </c:if>
        <c:if test="${settings.hasFivePrimeUTRs}">
          <c:set var="count" value="0" />
          <c:forEach items="${geneModels}" var="geneModel">
            <c:set var="count" value="${fn:length(geneModel.fivePrimeUTR) + count}" />
          </c:forEach>
          <c:if test="${count > 0}">
            <a href="#" id="fiveprimeutrs" class="switcher">5' UTR</a>: ${count}&nbsp;
          </c:if>
        </c:if>
        <c:if test="${settings.hasThreePrimeUTRs}">
          <c:set var="count" value="0" />
          <c:forEach items="${geneModels}" var="geneModel">
            <c:set var="count" value="${fn:length(geneModel.threePrimeUTR) + count}" />
          </c:forEach>
          <c:if test="${count > 0}">
            <a href="#" id="threeprimeutrs" class="switcher">3' UTR</a>: ${count}&nbsp;
          </c:if>
        </c:if>
        <c:if test="${settings.hasCDSs}">
          <c:set var="count" value="0" />
          <c:forEach items="${geneModels}" var="geneModel">
            <c:set var="count" value="${fn:length(geneModel.CDSs) + count}" />
          </c:forEach>
          <c:if test="${count > 0}">
            <a href="#" id="cdss" class="switcher">CDSs</a>: ${count}&nbsp;
          </c:if>
        </c:if>
      </div>
    </c:when>
    <c:otherwise>
      <p style="font-style:italic;">No results</p>
    </c:otherwise>
  </c:choose>
  <script type="text/javascript">
    jQuery("#gene-structure-displayer div.switchers a").click(function(e) {
      jQuery("#gene-structure-displayer div.collection-table").toggle();
      e.preventDefault();
    });
  </script>
</div>

<c:if test="${!settings.hasTranscripts}">
  <em>No gene models loaded for ${settings.organism}</em>
</c:if>


<c:if test="${!empty gene.transcripts}">
<div class="collection-table" style="display:none;">
  <table class="tiny-font">
    <thead>
      <tr>
        <%--<th>Gene</th>--%>
        <th>Transcript</th>
        <c:if test="${settings.hasExons}">
          <th>Exons</th>
        </c:if>
        <c:if test="${settings.hasIntrons}">
          <th>Introns</th>
         </c:if>
        <c:if test="${settings.hasFivePrimeUTRs}">
          <th>5' UTR</th>
        </c:if>
        <c:if test="${settings.hasThreePrimeUTRs}">
          <th>3' UTR</th>
        </c:if>
        <c:if test="${settings.hasCDSs}">
          <th>CDSs</th>
        </c:if>
      </tr>
    </thead>
    <c:set var="transcriptCount" value="${fn:length(gene.transcripts)}" />

    <tbody>
      <c:forEach items="${geneModels}" var="geneModel" varStatus="rowStatus">
        <c:set var="transcript" value="${geneModel.transcript}"/>

        <c:choose>
          <c:when test="${actualId == transcript.id}">
            <tr class="mainRow highlight">
          </c:when>
          <c:otherwise>
            <tr class="mainRow ${rowClass}">
          </c:otherwise>
        </c:choose>
            <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="transcript"/>
            </tiles:insert>

          <%-- background color switch --%>
          <c:set var="color_switch" value="true"/>

          <c:if test="${settings.hasExons}">

            <td class='main <c:if test="${color_switch}">alt</c:if>'>
              <c:set var="count" value="0" scope="page" />
              <c:if test="${!empty geneModel.exons}">

                  <table cellspacing="0">

                  <c:forEach items="${geneModel.exons}" var="exon">
                    <tr>
                      <tiles:insert page="/model/displaySequenceFeature.jsp">
                        <tiles:put name="feature" beanName="exon"/>
                        <tiles:put name="idToHighlight" beanName="actualId"/>
                        <tiles:put name="singleLine" value="true"/>
                      </tiles:insert>
                    </tr>
                  </c:forEach>
                </table>
              </c:if>

                <c:choose>
                  <c:when test="${color_switch}">
                     <c:set var="color_switch" value="false"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="color_switch" value="true"/>
                  </c:otherwise>
                </c:choose>

            </td>
          </c:if>
          <c:if test="${settings.hasIntrons}">
            <td class='<c:if test="${color_switch}">alt</c:if>'>
              <c:if test="${!empty geneModel.introns}">

                <table cellspacing="0">

                  <c:forEach items="${geneModel.introns}" var="intron">
                    <tr>
                      <tiles:insert page="/model/displaySequenceFeature.jsp">
                        <tiles:put name="feature" beanName="intron"/>
                        <tiles:put name="singleLine" value="true"/>
                      </tiles:insert>
                    </tr>
                  </c:forEach>
                </table>
              </c:if>

                <c:choose>
                  <c:when test="${color_switch}">
                     <c:set var="color_switch" value="false"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="color_switch" value="true"/>
                  </c:otherwise>
                </c:choose>

            </td>
          </c:if>
          <c:if test="${settings.hasFivePrimeUTRs}">
            <td class='main<c:if test="${color_switch}"> alt</c:if>'>

                <c:choose>
                  <c:when test="${color_switch}">
                     <c:set var="color_switch" value="false"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="color_switch" value="true"/>
                  </c:otherwise>
                </c:choose>

                <c:choose>
                  <c:when test="${!empty geneModel.fivePrimeUTR}">
                    <c:set var="fivePrimeUTR" value="${geneModel.fivePrimeUTR}"/>
                    <tiles:insert page="/model/displaySequenceFeature.jsp">
                      <tiles:put name="feature" beanName="fivePrimeUTR"/>
                    </tiles:insert>
                  </c:when>
                  <c:otherwise><td>&nbsp;</td></c:otherwise>
                </c:choose>
              </table>
            </td>
          </c:if>
          <c:if test="${settings.hasThreePrimeUTRs}">
            <td class='<c:if test="${color_switch}">alt</c:if>'>

                <c:choose>
                  <c:when test="${color_switch}">
                     <c:set var="color_switch" value="false"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="color_switch" value="true"/>
                  </c:otherwise>
                </c:choose>

                    <table cellspacing="0">

                <c:choose>
                  <c:when test="${!empty geneModel.threePrimeUTR}">
                    <c:set var="threePrimeUTR" value="${geneModel.threePrimeUTR}"/>
                    <tiles:insert page="/model/displaySequenceFeature.jsp">
                      <tiles:put name="feature" beanName="threePrimeUTR"/>
                    </tiles:insert>
                  </c:when>
                  <c:otherwise><td>&nbsp;</td></c:otherwise>
                </c:choose>
              </table>
            </td>
          </c:if>
          <c:if test="${settings.hasCDSs}">
            <td class='main<c:if test="${color_switch}"> alt</c:if>'>

                <c:choose>
                  <c:when test="${color_switch}">
                     <c:set var="color_switch" value="false"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="color_switch" value="true"/>
                  </c:otherwise>
                </c:choose>

                    <table cellspacing="0">

                <c:choose>
                  <c:when test="${!empty geneModel.CDSs}">
                    <c:forEach items="${geneModel.CDSs}" var="cds">
                      <tiles:insert page="/model/displaySequenceFeature.jsp">
                        <tiles:put name="feature" beanName="cds"/>
                      </tiles:insert>
                    </c:forEach>
                  </c:when>
                  <c:otherwise><td>&nbsp;</td></c:otherwise>
                </c:choose>
              </table>
            </td>
          </c:if>
        </tr>
      </c:forEach>
    </tbody>

  </table>
<p style="display:none;" class="toggle"><a class="toggler"><span>Show all rows</span></a></p>

<script type="text/javascript">
// hide over 2 rows from the features table
var geneStructureDisplayerSize = jQuery("#GenomicsCategory div.feature table.compact-table tr.mainRow").size();
if (geneStructureDisplayerSize > 1) {
  jQuery('#GenomicsCategory div.feature p').show();
  jQuery('#GenomicsCategory div.feature p a span').html("Show " + geneStructureDisplayerSize + " rows");

  //show more and destroy itself
  jQuery('#GenomicsCategory div.feature p a.toggler').click(function() {
    jQuery('#GenomicsCategory div.feature table.compact-table tr.mainRow:hidden').show();
    jQuery(this).remove();
  });

  jQuery("#GenomicsCategory div.feature table.compact-table tr.mainRow").each(function (i, row) {
    if (i > 1) {
      jQuery(row).hide();
    }
  });
}
</script>

</div>
</c:if>

</div>

<!-- /geneStructureDisplayer.jsp -->
