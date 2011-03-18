<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- geneStructureDisplayer.jsp -->

<div class="feature" style="overflow-x: auto">

<c:choose>
<c:when test="${gene.id == actualId}">
 <h3>Gene models - <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/></h3>
</c:when>
<c:otherwise>
  <h3>Gene models - <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/></h3>
</c:otherwise>
</c:choose>


<c:if test="${!empty gene.transcripts}">

  <table class="compact-table" cellspacing="0">
    <thead>
      <tr>
        <%--<th>Gene</th>--%>
        <th class="theme-5-background theme-3-border">Transcript</th>
        <th class="theme-5-background theme-3-border">Exons</th>
        <th class="theme-5-background theme-3-border">Introns</th>
        <th class="theme-5-background theme-3-border">5' UTR</th>
        <th class="theme-5-background theme-3-border">3' UTR</th>
        <th class="theme-5-background theme-3-border">CDSs</th>
      </tr>
    </thead>
    <c:set var="transcriptCount" value="${fn:length(gene.transcripts)}" />

    <tbody>
      <c:forEach items="${geneModels}" var="geneModel" varStatus="rowStatus">
        <c:set var="transcript" value="${geneModel.transcript}"/>

        <c:choose>
          <c:when test="${rowStatus.count % 2 == 0}">
            <c:set var="rowClass" value="even"/>
          </c:when>
          <c:otherwise>
            <c:set var="rowClass" value="odd"/>
          </c:otherwise>
        </c:choose>

        <c:choose>
          <c:when test="${actualId == transcript.id}">
            <tr class="highlight ${rowClass}">
          </c:when>
          <c:otherwise>
            <tr class="${rowClass}">
          </c:otherwise>
        </c:choose>
          <%--<c:if test="${rowCount.first}">
            <td rowSpan="${transcriptCount}"/>
              <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/>
            </td>
          </c:if>--%>
          <%--<td>--%>
            <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="transcript"/>
            </tiles:insert>
          <%--</td>--%>
          <td>
            <c:if test="${!empty geneModel.exons}">
              <table>
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
          </td>
          <td>
            <c:if test="${!empty geneModel.introns}">
              <table>
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
          </td>
          <%--<td>--%>
          <c:choose>
            <c:when test="${!empty geneModel.fivePrimeUTR}">
              <c:set var="fivePrimeUTR" value="${geneModel.fivePrimeUTR}"/>
              <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="fivePrimeUTR"/>
              </tiles:insert>
            </c:when>
            <c:otherwise><td colspan="2"></td></c:otherwise>
          </c:choose>
          <%--</td>--%>
          <%--<td>--%>
          <c:choose>
            <c:when test="${!empty geneModel.threePrimeUTR}">
              <c:set var="threePrimeUTR" value="${geneModel.threePrimeUTR}"/>
              <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="threePrimeUTR"/>
              </tiles:insert>
            </c:when>
            <c:otherwise><td colspan="2"></td></c:otherwise>
          </c:choose>
          <%--</td>--%>
          <%--<td>--%>
          <c:choose>
            <c:when test="${!empty geneModel.CDSs}">
              <c:forEach items="${geneModel.CDSs}" var="cds">
                <tiles:insert page="/model/displaySequenceFeature.jsp">
                  <tiles:put name="feature" beanName="cds"/>
                </tiles:insert>
              </c:forEach>
            </c:when>
            <c:otherwise><td colspan="2"></td></c:otherwise>
          </c:choose>
          <%--</td>--%>

        </tr>


      </c:forEach>
    </tbody>

  </table>
</c:if>

</div>

<!-- /geneStructureDisplayer.jsp -->
