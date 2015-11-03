<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- cytoscapeNetworkDisplayer.jsp -->

<<<<<<< HEAD
<html:xhtml/>

<script type="text/javascript">
(function() {
    project_title = "${WEB_PROPERTIES['project.title']}";
    project_baseurl = "${WEB_PROPERTIES['webapp.baseurl']}";
    project_path = "${WEB_PROPERTIES['webapp.path']}";

    // from controller
    fullInteractingGeneSet = '${fullInteractingGeneSet}'; // a string arrray of gene object store ids
    dataNotIncludedMessage = '${dataNotIncludedMessage}'; // case: interaction data is not integrated
    orgWithNoDataMessage = '${orgWithNoDataMessage}'; // case: no interaction data for the whole species
})();
</script>

<style type="text/css">
    /* The Cytoscape Web container must have its dimensions set. */
    html, body { height: 100%; width: 100%; padding: 0; margin: 0; }
    /* use absolute value */
    #cwcontent { width: 600px; height: 485px; border: 2px solid #CCCCCC; padding: 0 1em 1em 0;
        -moz-border-radius: 5px 5px 5px 5px; border-radius: 5px 5px 5px 5px; float:left; }
    #cwtabsbyside { float: left; width: 300px; display:none; }
    #cwinlinetable { display: none; padding: 5px 0 0 0;}
    label { vertical-align: middle; }
    #powerby { padding: 5px; text-align: center; }
    #powerby a { color: rgb(136, 136, 136); text-decoration: none; background-color: white; }
    #powerby img { vertical-align: middle; }

    #legendall { width:100%; height:50px; }

    #interactions-wrap div.inside { min-width:1040px; }

    <%-- sidebar toolbar bar --%>
    #cwtabsbyside ul { border-bottom-width:1px; border-bottom-style:solid; font-size:14px; margin:0; }
    #cwtabsbyside ul li { display:inline; border-right-width:1px; border-right-style:solid; }
    #cwtabsbyside ul li a { padding:4px; }
    #cwtabsbyside ul li a.active { font-weight:bold; }
    #cwtabsbyside ul li.last { border:none; }
    #cwtabsbyside fieldset, #cwtabsbyside #legend { border:none; border-bottom-width:1px; border-bottom-style:solid; padding:8px; }

    #cwtabsbyside #formats ul li { display:block; font-size:12px; margin-bottom:4px; }
</style>
=======
>>>>>>> intermine-1.6

<div id="cwhead">
    <h3 class="goog">Interaction Network</h3>
    <div id="geneInteractionDisplayer"></div>
</div>


<<<<<<< HEAD
        <fieldset class="alt">
              <label>Export network as:</label>
              <select id="exportoptions">
                  <option value="xgmml" selected>XGMML</option>
                  <option value="sif">SIF</option>
                  <option value="png">PNG</option>
                  <option value="svg">SVG</option>
                  <option value="pdf">PDF</option>
                  <option value="tab">TSV</option>
                  <option value="csv">CSV</option>
              </select>
              <input type="button" id="exportbutton" value="Export">
        </fieldset>
=======
>>>>>>> intermine-1.6

<script>

  var paths = {js: {}, css: {}};

  <c:set var="section" value="gene-interaction-displayer"/>

  <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
      paths["${res.type}"]["${res.key}".split(".").pop()] = "${res.url}";
  </c:forEach>

  var imload = function(){

  link = document.createElement( "link" );
  link.href = paths.css.main;
  link.type = "text/css";
  link.rel = "stylesheet";
  link.media = "screen,print";

  document.getElementsByTagName( "head" )[0].appendChild( link );

    intermine.load({
      'js': {
          'main': {
            'path': paths.js.main
          }
      }
    }, function(err) {

      element = document.getElementById('geneInteractionDisplayer');

      jQuery('document').ready(function(){
        cymine({
          parentElem : element,
          service : $SERVICE,
          queryOn : {
            "value" : "${cytoscapeInteractionObjectId}",
            "path": "id",
            "op": "="
          }
        }).then(function(hasValues) {
          if (!hasValues) {
            element.parentElement.style.display = "none";
          }
        });
      });
    });
  };

  imload()

</script>



<<<<<<< HEAD
    // 'export' is reserved! (http://www.quackit.com/javascript/javascript_reserved_words.cfm)
    function exportNet(type) {
        if (type=="tab" || type=="csv") {

          window.location.href = 'cytoscapeNetworkExport.do?type=' + type + '&fullInteractingGeneSet='+fullInteractingGeneSet
          // This no longer works due to a Flash security update:
            // vis.exportNetwork(type, 'cytoscapeNetworkExport.do?type=' + type + '&fullInteractingGeneSet='+fullInteractingGeneSet);
        } else {

          function base64toBlob(base64Data, contentType) {
              contentType = contentType || '';
              var sliceSize = 1024;
              var byteCharacters = atob(base64Data);
              var bytesLength = byteCharacters.length;
              var slicesCount = Math.ceil(bytesLength / sliceSize);
              var byteArrays = new Array(slicesCount);

              for (var sliceIndex = 0; sliceIndex < slicesCount; ++sliceIndex) {
                  var begin = sliceIndex * sliceSize;
                  var end = Math.min(begin + sliceSize, bytesLength);

                  var bytes = new Array(end - begin);
                  for (var offset = begin, i = 0 ; offset < end; ++i, ++offset) {
                      bytes[i] = byteCharacters[offset].charCodeAt(0);
                  }
                  byteArrays[sliceIndex] = new Uint8Array(bytes);
              }
              return new Blob(byteArrays, { type: contentType });
          }

          var exportFile = function(data, name, contentType, binary) {

              try {

                contentType = 'application/octet-stream';
                var a = document.createElement('a');
                var blob;
                if (binary) {
                  blob = base64toBlob(data, contentType);
                } else {
                  blob = new Blob([data], {'type':contentType, });
                }
                a.href = window.URL.createObjectURL(blob);
                a.download = name;
                document.querySelector('body').appendChild(a);
                a.click();
                document.querySelector('body').removeChild(a);

              } catch(e) {
                  console.log("Exporting from the interaction network is not supported by your browser.");
              }

          }


          switch (type) {

            case "pdf": exportFile(vis.pdf(), "network.pdf", "application/octet-stream", true); break;
            case "xgmml": exportFile(vis.xgmml(), "network.xgmml", "application/octet-stream", false); break;
            case "svg": exportFile(vis.svg(), "network.svg", "application/octet-stream", false); break;
            case "sif": exportFile(vis.sif(), "network.sif", "application/octet-stream", false); break;
            case "png": exportFile(vis.png(), "network.png", "image/png", true); break;

          }

        }
    }

    jQuery("#exportbutton").click(function () {
        exportNet(jQuery("#exportoptions option:selected").val());
    });

    try {
      var testblob = new Blob();
    } catch(e) {
      jQuery("#exportoptions > option").each(function() {
          var nextoption = jQuery(this);
          if (nextoption.val() != "csv" && nextoption.val() != "tab") {
            nextoption.attr("disabled", "disabled");
          }
      });
      jQuery("#exportoptions option[value=csv]").attr("selected", "selected");
    }

    jQuery("#exportbutton").click(function () {
        exportNet(jQuery("#exportoptions option:selected").val());
    });

})();
</script>
=======


>>>>>>> intermine-1.6

<!-- /cytoscapeNetworkDisplayer.jsp -->
