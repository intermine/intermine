<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<%-- one element hash map --%>
<c:forEach var="type" items="${response}">
  <c:if test="${not empty type}">
    <div class="report-displayer">
      <h3 class="uniprot">Curated comments from UniProt</h3>
      <table cellspacing="0" class="displayer">

          <c:choose>
            <%-- displayer for gene page --%>
            <c:when test="${type.key == 'gene'}">
              <tr>
                <th class="theme-5-background theme-3-border">Type</th>
                <th class="comment theme-5-background theme-3-border">Comment</th>
                <th class="proteins theme-5-background theme-3-border">Proteins</th>
              </tr>

              <%-- traverse the comments --%>
              <c:forEach var="comment" items="${type.value}" varStatus="status">
                <tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
                  <%-- comment type and proteins objects --%>
                  <c:forEach var="bag" items="${comment.value}">
                    <c:set var="tdStyle" value="${status.count mod 2 == 0 ? 'theme-3-border theme-6-background' : ''}" />
                    <c:choose>
                      <c:when test="${bag.key == 'type'}">
                        <!-- comment 'type' -->
                        <td class="type ${tdStyle}">
                          ${bag.value}
                        </td>
                        <td class="text ${tdStyle}">${comment.key}</td>
                      </c:when>
                      <c:when test="${bag.key == 'proteins'}">
                        <!-- comment 'proteins' List -->
                        <td class="${tdStyle}">
                          <c:forEach var="protein" items="${bag.value}" varStatus="looptyLoop">
                            <!-- protein: id => primaryIdentifier -->
                            <html:link action="/report?id=${protein.key}&amp;trail=|${protein.key}">
                              ${protein.value}
                            </html:link>
                            <!-- ${!looptyLoop.last ? ', ' : ''} -->
                          </c:forEach>
                        </td>
                      </c:when>
                      <c:otherwise>
                        <!-- big fat fail -->
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>

                </tr>
              </c:forEach>

            </c:when>

            <%-- displayer for protein page --%>
            <c:when test="${type.key == 'protein'}">
              <tr>
                <th class="type theme-5-background theme-3-border">Type</th>
                <th class="comment theme-5-background theme-3-border">Comment</th>
              </tr>
              <c:forEach var="comment" items="${type.value}" varStatus="status">
                <c:set var="tdStyle" value="${status.count mod 2 == 0 ? 'theme-3-border theme-6-background' : ''}" />
                <tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
                  <td class="type ${tdStyle}">${comment.value}</td>
                  <td class="${tdStyle}">${comment.key}</td>
                </tr>
              </c:forEach>
            </c:when>

            <c:otherwise></c:otherwise>

          </c:choose>

      </table>
    </div>
  </c:if>
</c:forEach>