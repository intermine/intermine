<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<%-- one element hash map --%>
<c:forEach var="type" items="${response}">
  <c:if test="${not empty type}">
    <div class="collection-table">
      <h3>Curated comments from UniProt</h3>
      <table>

          <c:choose>
            <%-- displayer for gene page --%>
            <c:when test="${type.key == 'gene'}">
        <thead>
            <tr>
                  <th>Type</th>
                  <th>Comment</th>
                  <th>Proteins</th>
                </tr>
                </thead>
        <tbody>
                  <%-- traverse the comments --%>
                  <c:forEach var="comment" items="${type.value}" varStatus="status">
                    <tr>
                      <%-- comment type and proteins objects --%>
                      <c:forEach var="bag" items="${comment.value}">
                        <c:set var="tdStyle" value="${status.count mod 2 == 0 ? 'alt' : ''}" />
                        <c:choose>
                          <c:when test="${bag.key == 'type'}">
                            <!-- comment 'type' -->
                            <td class="class ${tdStyle}">
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
        </tbody>
            </c:when>

            <%-- displayer for protein page --%>
            <c:when test="${type.key == 'protein'}">
               <thead>
                <tr>
                  <th>Type</th>
                  <th>Comment</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="comment" items="${type.value}">
                  <c:set var="tdStyle" value="${status.count mod 2 == 0 ? 'alt' : ''}" />
                  <tr>
                    <td class="type ${tdStyle}">${comment.value}</td>
                    <td class="${tdStyle}">${comment.key}</td>
                  </tr>
                </c:forEach>
              </tbody>
            </c:when>

            <c:otherwise></c:otherwise>

          </c:choose>

      </table>
    </div>
  </c:if>
</c:forEach>