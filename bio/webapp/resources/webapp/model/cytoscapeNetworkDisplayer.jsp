<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- cytoscapeNetworkDisplayer.jsp -->

<div id="cwhead">
  <h3 class="goog">Interaction Network</h3>
  <div id="geneInteractionDisplayer"></div>

  <c:choose>
    <c:when test="${interactionSize < 500}">

      <script>

        var paths = {
          js: {},
          css: {}
        };

        <c:set var = "section" value = "gene-interaction-displayer" />
        <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
          paths["${res.type}"]["${res.key}".split(".").pop()] = "${res.url}";
        </c:forEach>

        var imload = function () {

          link = document.createElement("link");
          link.href = paths.css.main;
          link.type = "text/css";
          link.rel = "stylesheet";
          link.media = "screen,print";

          document.getElementsByTagName("head")[0].appendChild(link);

          intermine.load({
            'js': {
              'main': {
                'path': paths.js.main
              }
            }
          }, function (err) {

            element = document.getElementById('geneInteractionDisplayer');

            jQuery('document').ready(function () {
              cymine({
                parentElem: element,
                service: $SERVICE,
                queryOn: {
                  "value": "${cytoscapeInteractionObjectId}",
                  "path": "id",
                  "op": "="
                },
                nodeType: imSummaryFields.type
              }).then(function (hasValues) {
                if (!hasValues) {
                  element.parentElement.style.display = "none";
                }
              });
            });
          });
        };

        imload()
      </script>

    </c:when>
    <c:otherwise>
      <%-- Let's display a sane error message if there are too many results.
This was implemented due to https://github.com/intermine/intermine/issues/1284, where > 2000 interactions caused the browser to halt. --%>
      <p>${interactionSize} interactions found. This viewer is not displayed with this high number of interactions in order to enhance your browser's performance.</p>
    </c:otherwise>
  </c:choose>

</div>

<!-- /cytoscapeNetworkDisplayer.jsp -->
