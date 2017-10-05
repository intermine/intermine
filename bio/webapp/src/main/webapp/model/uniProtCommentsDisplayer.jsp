<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>

<%-- one element hash map --%>
<c:forEach var="type" items="${response}">
  <c:choose>
    <c:when test="${not empty type.value}">
      <div id="uniprotCommentsDisplayer" class="collection-table">

    <style>
      #uniprotCommentsDisplayer .brotein { display:none; }
    </style>

        <h3><c:if test="${type.key == 'gene'}"><div class="right"><a href="#">Show proteins</a></div></c:if>Curated comments from UniProt</h3>
        <table>

            <c:choose>
              <%-- displayer for gene page --%>
              <c:when test="${type.key == 'gene'}">
          <thead>
              <tr>
                    <th>Type</th>
                    <th>Comment</th>
                    <th class="brotein">Proteins</th>
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
                              <td class="${tdStyle} brotein">
                                <c:forEach var="protein" items="${bag.value}" varStatus="looptyLoop">
                                  <!-- protein: id => primaryIdentifier -->
                                  <html:link action="/report?id=${protein.key}&amp;trail=%7C${protein.key}">
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

        <script type="text/javascript">
        (function() {
          jQuery('#uniprotCommentsDisplayer h3 div.right a').click(function(e) {
            if (jQuery('#uniprotCommentsDisplayer table th.brotein:visible').exists()) {
              jQuery('#uniprotCommentsDisplayer table .brotein').hide();
              jQuery(this).text('Show proteins');
            } else {
              jQuery('#uniprotCommentsDisplayer table .brotein').show();
              jQuery(this).text('Hide proteins');
            }

            e.preventDefault();
          });
        })();
        </script>

      </div>
    </c:when>
    <c:otherwise>
       <h3 class="goog">Curated comments from UniProt</h3>
       <p style="font-style:italic;">No comments found</p>
    </c:otherwise>
  </c:choose>
</c:forEach>