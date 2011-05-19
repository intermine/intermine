<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- geneStructureDisplayer.jsp -->

<div class="feature">

<c:choose>
<c:when test="${gene.id == actualId}">
 <h3>Gene models - <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/></h3>
</c:when>
<c:otherwise>
  <h3>Gene models - <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/></h3>
</c:otherwise>
</c:choose>


<c:if test="${!settings.hasTranscripts}">
  <em>No gene models loaded for ${settings.organism}</em>
</c:if>

<c:if test="${!empty gene.transcripts}">

  <table class="compact-table" cellspacing="0">
    <thead>
      <tr>
        <%--<th>Gene</th>--%>
        <th class="theme-5-background theme-3-border">Transcript</th>
        <c:if test="${settings.hasExons}">
          <th class="theme-5-background theme-3-border">Exons</th>
        </c:if>
        <c:if test="${settings.hasIntrons}">
          <th class="theme-5-background theme-3-border">Introns</th>
         </c:if>
        <c:if test="${settings.hasFivePrimeUTRs}">
          <th class="theme-5-background theme-3-border">5' UTR</th>
        </c:if>
        <c:if test="${settings.hasThreePrimeUTRs}">
          <th class="theme-5-background theme-3-border">3' UTR</th>
        </c:if>
        <c:if test="${settings.hasCDSs}">
          <th class="theme-5-background theme-3-border">CDSs</th>
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

            <td class='main theme-3-border <c:if test="${color_switch}">theme-6-background</c:if>'>
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
            <td class='<c:if test="${color_switch}">theme-6-background</c:if>'>
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
            <td class='main theme-3-border <c:if test="${color_switch}">theme-6-background</c:if>'>

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
            <td class='<c:if test="${color_switch}">theme-6-background</c:if>'>

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
            <td class='main theme-3-border <c:if test="${color_switch}">theme-6-background</c:if>'>

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


<p class="toggle">
<a class="toggler"><span>Show all rows</span></a>
</p>
<script type="text/javascript">
// hide over 2 rows from the features table
jQuery("#GenomicsCategory div.feature table.compact-table tr.mainRow").each(function (i, row) {
  if (i > 1) {
    jQuery(row).hide();
  }
});
// show more and destroy itself
jQuery('#GenomicsCategory div.feature p a.toggler').click(function() {
  jQuery('#GenomicsCategory div.feature table.compact-table tr.mainRow:hidden').show();
  jQuery(this).remove();
});
</script>

</c:if>

</div>

<!-- /geneStructureDisplayer.jsp -->
