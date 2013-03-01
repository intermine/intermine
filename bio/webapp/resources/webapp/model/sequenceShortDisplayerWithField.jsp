<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="expr" ignore="false"/>
<tiles:importAttribute name="objectClass" ignore="false"/>
<im:eval evalExpression="interMineObject.${expr}" evalVariable="outVal"/>
<c:choose>
  <c:when test="${empty outVal}">
    &nbsp;
  </c:when>
  <c:otherwise>
    <c:choose>
      <c:when test="${!empty interMineObject.sequence}">
          <im:value>${outVal}</im:value>&nbsp;<html:link action="sequenceExporter?object=${interMineObject.id}" target="_new"><html:img styleClass="fasta" src="model/images/fasta.gif" title="FASTA" />
        </html:link>
        <span style="margin-left: 5px;"><a href='javascript: sendSequenceToGalaxy();'><html:img styleClass="galaxy" src="images/formats/galaxy.gif" title="Galaxy" /></a></span>
      </c:when>
      <c:otherwise>
        <im:value>${outVal}</im:value>
      </c:otherwise>
    </c:choose>
  </c:otherwise>
</c:choose>

<script type="text/javascript">
    var s = $SERVICE; // globally available in the webapp.
    var openWindowWithPost = function(uri, name, params) {
        var name = name + new Date().getTime();
        // var form = jQuery('<form method="POST" action="' + uri + '" target="' + name + '"></form>');

        var form = jQuery('<form>', {
            'method': 'POST',
            'action': uri,
            'target': name
        })

        jQuery.each(params, function(v, k) {
            var input = jQuery('<input name="' + v + '" + type="hidden">');
            form.append(input);
            input.val(k)
            // console.log(k)
        });
        form.appendTo('body');
        var w = window.open("someNonExistantPathToSomeWhere", name);
        form.submit();
        form.remove();
    };

    function sendSequenceToGalaxy() {
        jQuery.when(s.query({select: ["id"], from: "${objectClass}", where: {id: ${interMineObject.id}}}), s.findById("${objectClass}", ${interMineObject.id}))
        .done(function(q, feature) {
           var dataForGalaxy = {
             tool_id: 'flymine',
             organism: feature.organism.name,
             URL: q.getFASTAURI(),
             URL_method: 'post',
             name: "Sequence for " + feature.symbol,
             data_type: 'fasta',
             info: "FASTA sequence for the " + feature.organism.shortName + " feature " + feature.symbol,
           };
           openWindowWithPost("http://main.g2.bx.psu.edu/tool_runner", "Galaxy", dataForGalaxy);
          });
    }
</script>
