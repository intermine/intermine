<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- ComplexDisplayer.jsp -->

<div id="cwhead">
    <h3 class="goog">Complex Viewer</h3>
    <button id="opener">Legend</button>
    <button id="expand-all">Expand All</button>
    <button id="reset">Reset</button>
    <button id="export-svg">Export SVG</button>
    <div id="complexDisplayer"></div>
</div>

<div id="dialog" style="display: none;" title="Legend (Complex Viewer)">
  <div id="networkCaption">
      <table style="border: hidden;">
          <tr>
              <td>
                  <div style="float:right">
                      <img src="images/complexviewer/smallMol.svg">
                  </div>
              <td>Bioactive entity</td>
          </tr>
          <tr>
              <td>
                  <div style="float:right">
                      <img src="images/complexviewer/proteinBlob.svg">
                  </div>
              </td>
              <td>Protein</td>
          </tr>
          <tr>
              <td>
                  <div style="float:right">
                      <img src="images/complexviewer/proteinBar.svg">
                  </div>
              </td>
              <td> - click or tap to toggle between circle and bar (bar shows binding sites, if
                  known)
              </td>
          </tr>
          <tr>
              <td>
                  <div style="float:right">
                      <img src="images/complexviewer/gene.svg">
                  </div>
              </td>
              <td>Gene</td>
          </tr>
          <tr>
              <td>
                  <div style="float:right">
                      <img src="images/complexviewer/DNA.svg">
                  </div>
              </td>
              <td>
                  DNA
              </td>
          </tr>
          <tr>
              <td>
                  <div style="float:right">
                      <img src="images/complexviewer/RNA.svg">
                  </div>
              </td>
              <td>RNA</td>
          </tr>
      </table>
      <div id="colours"></div>
  </div>
</div>

<script>

  var paths = {js: {}, css: {}};

  <c:set var="section" value="complex-viewer"/>

  <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
      paths["${res.type}"]["${res.key}".split(".").pop()] = "${res.url}";
  </c:forEach>

  var imload = function(){

    var root = window.location.origin + "/${WEB_PROPERTIES['webapp.path']}";

    var complexIdentifier = imSummaryFields["identifier"];
    var path = root + "/service/complexes/export/" + complexIdentifier;





    intermine.load({
      'css': {
        'css': {
          'path': paths.css.style
        },
        'jquerycss': {
          'path': paths.css.jquerystyle
        }
      },
      'js': {
          'd3': {
            'path': paths.js.d3
          },
          'jqueryui': {
            'path': paths.js.jqueryui
          },
          'main': {
            'path': paths.js.main,
            'depends': ["d3"]
          }
      }

    }, function(err) {

      ////////// Legend
      jQuery('#dialog').dialog({
          autoOpen: false,
          title: 'Legend (Complex Viewer)'
      });

      jQuery('#opener').click(function() {
          jQuery('#dialog').dialog('open');
          return false;
      });

      jQuery('#expand-all').click(function() {
          xlv.expandAll();
          return false;
      });

      jQuery('#reset').click(function() {
          xlv.reset();
          return false;
      });

      jQuery('#export-svg').click(function() {
        var xml = xlv.getSVG();
        xmlAsUrl = 'data:image/svg;filename=xiNET-output.svg,';
        xmlAsUrl += encodeURIComponent(xml);
        var win = window.open(xmlAsUrl, 'xiNET-output.svg');
      });
      //////////

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
