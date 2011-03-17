<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:if test="${gogogo != null}">
  <script type="text/javascript" charset="utf-8">
  function getInterMineLinks(organismShortName, identifier, symbol) {
      AjaxServices.getInterMineLinks(organismShortName, identifier, symbol, function(mines) {
          jQuery('#intermine_links').toggleClass('loading');
          alert(jQuery.parseJSON(mines));
      });
  }
  </script>

  <h3>Link to other InterMines</h3>
      <div id="intermine_links" class="loading">&nbsp;</div><br>
        <script type="text/javascript" charset="utf-8">
          getInterMineLinks('${object.organism.shortName}','${object.primaryIdentifier}','${object.symbol}');
        </script>
</c:if>