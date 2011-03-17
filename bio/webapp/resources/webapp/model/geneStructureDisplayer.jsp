<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- geneStructureDisplayer.jsp -->

<style type="text/css">
.compact-table td{
  font-size: 0.9em;
  padding-right: 6px;
}

.highlight {
  background-color: yellow;
}


</style>

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

  <table class="compact-table">
    <tr>
      <%--<th>Gene</th>--%>
      <th>Transcript</th>
      <th>Exons</th>
      <th>Introns</th>
      <th>5' UTR</th>
      <th>3' UTR</th>
      <th>CDSs</th>
    </tr>
    <c:set var="transcriptCount" value="${fn:length(gene.transcripts)}" />

    <c:forEach items="${geneModels}" var="geneModel" varStatus="rowCount">
      <c:set var="transcript" value="${geneModel.transcript}"/>
      <c:choose>
        <c:when test="${actualId == transcript.id}">
          <tr class="highlight">
        </c:when>
        <c:otherwise>
          <tr>
        </c:otherwise>
      </c:choose>
        <%--<c:if test="${rowCount.first}">
          <td rowSpan="${transcriptCount}"/>
            <c:out value="${gene.symbol} ${gene.primaryIdentifier}"/>
          </td>
        </c:if>--%>
        <td>
          <tiles:insert page="/model/displaySequenceFeature.jsp">
              <tiles:put name="feature" beanName="transcript"/>
          </tiles:insert>
        </td>
        <td>
          <c:if test="${!empty geneModel.exons}">
            <c:forEach items="${geneModel.exons}" var="exon">
              <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="exon"/>
                <tiles:put name="idToHighlight" beanName="actualId"/>
                <tiles:put name="singleLine" value="true"/>
              </tiles:insert>
              <br/>
            </c:forEach>
          </c:if>
        </td>
        <td>
          <c:if test="${!empty geneModel.introns}">
            <c:forEach items="${geneModel.introns}" var="intron">
              <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="intron"/>
                <tiles:put name="singleLine" value="true"/>
              </tiles:insert>
            </c:forEach>
          </c:if>
        </td>
        <td>
          <c:if test="${!empty geneModel.fivePrimeUTR}">
            <c:set var="fivePrimeUTR" value="${geneModel.fivePrimeUTR}"/>
            <tiles:insert page="/model/displaySequenceFeature.jsp">
              <tiles:put name="feature" beanName="fivePrimeUTR"/>
            </tiles:insert>
          </c:if>
        </td>
        <td>
          <c:if test="${!empty geneModel.threePrimeUTR}">
            <c:set var="threePrimeUTR" value="${geneModel.threePrimeUTR}"/>
            <tiles:insert page="/model/displaySequenceFeature.jsp">
              <tiles:put name="feature" beanName="threePrimeUTR"/>
            </tiles:insert>
          </c:if>
        </td>
        <td>
          <c:if test="${!empty geneModel.CDSs}">
            <c:forEach items="${geneModel.CDSs}" var="cds">
              <tiles:insert page="/model/displaySequenceFeature.jsp">
                <tiles:put name="feature" beanName="cds"/>
              </tiles:insert>
            </c:forEach>
          </c:if>
        </td>

      </tr>


    </c:forEach>

  </table>
</c:if>
<hr />

</div>

<!-- /geneStructureDisplayer.jsp -->
