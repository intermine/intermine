<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- ComplexDisplayer.jsp -->

<div id="cwhead">
    <h3 class="goog">Complex Viewer</h3>
    <div id="complexDisplayer"></div>
</div>

<script>

  var paths = {js: {}, css: {}};

  <c:set var="section" value="complex-viewer"/>

  <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
      paths["${res.type}"]["${res.key}".split(".").pop()] = "${res.url}";
  </c:forEach>


  var imload = function(){

  // debugger;
  // link = document.createElement( "link" );
  // link.href = paths.css;
  // link.type = "text/css";
  // link.rel = "stylesheet";
  // link.media = "screen,print";
  //
  // document.getElementsByTagName( "head" )[0].appendChild( link );

    var root = window.location.origin + "/${WEB_PROPERTIES['webapp.path']}";

    var complexIdentifier = imSummaryFields["identifier"];
    var path = root + "/service/complexes/export/" + complexIdentifier;

    intermine.load({
      'js': {
          'd3': {
            'path': paths.js.d3
          },
          'main': {
            'path': paths.js.main,
            'depends': ["d3"]
          }
      }
      // 'css': {
      //   'css': {
      //     'path': paths.css.css
      //   }
      // }
    }, function(err) {

      element = document.getElementById('complexDisplayer');

      jQuery('document').ready(function(){

          xlv = new xiNET(element);
          xlv.clear()

          var matrixExpansion = true;
          jQuery.getJSON(path, function(data) {
            xlv.readMIJSON(data, matrixExpansion);
          });
      });
      });
    };

  imload();

</script>


<!-- /ComplexDisplayer.jsp -->
