<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- sequenceFeatureDisplayer.jsp -->

<div class="feature">

<c:set var="feature" value="${reportObject.object}"/>

<c:choose>
  <c:when test="${locationsCollection != null}">
    <h3>This feature maps to ${locationsCollectionSize} genome locations</h3>
  </c:when>
  <c:otherwise>
    <h3>Genome feature</h3>
  </c:otherwise>
</c:choose>

<table border="0" cellspacing="0">
  <tr>
    <td>Feature:</td>
    <td><strong><c:out value="${feature.symbol} ${feature.primaryIdentifier}"/></strong></td>
    <c:choose>
      <c:when test="${!empty feature.sequenceOntologyTerm}">
        <td class="border-left">Sequence ontology type:</td>
        <td>
          <strong><c:out value="${feature.sequenceOntologyTerm.name}"/></strong>
          <img alt="?" title="${feature.sequenceOntologyTerm.description}"
          src="images/icons/information-small-blue.png" style="padding-bottom: 4px;"
          class="tinyQuestionMark" />
        </td>
      </c:when>
      <c:otherwise>
        <td colspan="2"></td>
      </c:otherwise>
    </c:choose>
  </tr>
  <tr class="even">
    <td class="theme-3-border theme-6-background">Location:</td>
      <c:choose>
        <c:when test="${!empty feature.chromosomeLocation}">
          <td class="theme-3-border theme-6-background">
            <strong>
              <c:set var="loc" value="${feature.chromosomeLocation}"/>
              <c:out value="${loc.locatedOn.primaryIdentifier}:${loc.start}-${loc.end}"/>
            </strong>
            <c:if test="${!empty loc.strand}">
              <span class="smallnote">
                <c:choose>
                  <c:when test="${loc.strand == 1}">
                    forward strand
                  </c:when>
                  <c:when test="${loc.strand == -1}">
                    reverse strand
                  </c:when>
                </c:choose>
              </span>
            </c:if>
        </c:when>
        <c:when test="${locationsCollection != null}">
          <td class="theme-3-border theme-6-background">
            <div id="locations-collection">
              <table class="noborder" cellspacing="0" border="0"><tbody><tr>
                <c:forEach items="${locationsCollection}" var="loc" varStatus="statei">
                  <td <c:if test="${(statei.count + 1) % 3 == 0}">class="centered"</c:if>>
                    <strong>
                      <c:out value="${loc.locatedOn.primaryIdentifier}:${loc.start}-${loc.end}"/>
                    </strong>
                    <c:if test="${!empty loc.strand}">
                      <span class="smallnote">
                        <c:choose>
                          <c:when test="${loc.strand == 1}">
                            forward strand
                          </c:when>
                          <c:when test="${loc.strand == -1}">
                            reverse strand
                          </c:when>
                        </c:choose>
                      </span>
                    </c:if>
                  </td>
                  <c:if test="${statei.count % 3 == 0}"></tr>
                    <c:choose>
                        <c:when test="${statei.count >= 9}"><tr style="display:none;"></c:when>
                        <c:otherwise><tr></c:otherwise>
                    </c:choose>
                  </c:if>
                </c:forEach>
                </tr></tbody></table>
                <c:if test="${locationsCollectionSize >= 9}">
                    <p class="toggle"><a class="theme-1-color" href="#"
                    onclick="return showMoreRows('#locations-collection', 1, 3);">Show more rows</a></p>
                </c:if>
              </div>
              <p style="display:none;" class="in_table">
                <html:link styleClass="theme-1-color" action="/collectionDetails?id=${feature.id}&amp;field=locations&amp;trail=${param.trail}">
                  Show all in a table
                </html:link>
              </p>
        </c:when>
        <c:otherwise>
          <td class="theme-3-border theme-6-background" colspan="3">
            No location information in ${WEB_PROPERTIES['project.title']}
        </c:otherwise>
      </c:choose>
    </td>
    <c:choose>
      <c:when test="${!empty feature.length}">
        <td class="theme-3-border theme-6-background border-left">Length:</td>
        <td class="theme-3-border theme-6-background">
          <c:set var="interMineObject" value="${reportObject.object}" scope="request" />
          <tiles:insert page="/model/sequenceShortDisplayerWithField.jsp">
            <tiles:put name="expr" value="length" />
          </tiles:insert>
        </td>
      </c:when>
      <c:otherwise></c:otherwise>
    </c:choose>
  </tr>
</table>

</div>

<!-- /sequenceFeatureDisplayer.jsp -->