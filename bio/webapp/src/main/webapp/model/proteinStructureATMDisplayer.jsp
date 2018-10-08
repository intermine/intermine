<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:if test="${!empty(atm)}">
  <div id="protein-structure-atm-displayer" class="inline-list">
    <h3>ATM</h3>
    <ul>
      <li><a class="external" href="#">Show</a></li>
    </ul>

    <div class="value" style="display:none;">${atm}</div>

    <script type="text/javascript">
    jQuery("#protein-structure-atm-displayer a").click(function(e) {
        var w = window.open('','ATM','width=600,height=600');
        w.document.open();
        w.document.write(function() {
          return jQuery('<html/>')
          .append(jQuery('<body/>')
              .append(jQuery('<div/>')
                  .append(jQuery('<pre/>', {
                    'text': jQuery("#protein-structure-atm-displayer div.value").text()
                  })))).html();
        }());
        w.document.close();
        e.preventDefault();
    });
    </script>
  </div>
</c:if>