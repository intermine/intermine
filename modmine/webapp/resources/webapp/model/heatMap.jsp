<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
    prefix="str"%>

<!-- heatMap.jsp -->

<style type="text/css">
input.submit {
  color:#050;
  font: bold 84% 'trebuchet ms',helvetica,sans-serif;
  background-color:#fed;
  border:1px solid;
  border-color: #696 #363 #363 #696;
}
</style>

<script>

  jQuery(document).ready(function(){
    // Unckeck all checkboxes everything the page is (re)loaded
    initCheck();

    // Do before the form submitted
    jQuery("#saveFromIdsToBagForm").submit(function() {
        var ids = new Array();
        jQuery(".aSub").each(function() {
          if (this.checked) {ids.push(this.value);}
       });

        if (ids.length < 1)
        { alert("Please select some submissions...");
          return false;
        } else {
          jQuery("#ids").val(ids);
          return true;
          }
    });
  });

     function initCheck()
     {
       jQuery('#allSub').removeAttr('checked');
       jQuery(".aSub").removeAttr('checked');
     }

     // (un)Check all ids checkboxes
     function checkAll()
     {
         jQuery(".aSub").attr('checked', jQuery('#allSub').is(':checked'));
         jQuery('#allSub').css("opacity", 1);
     }


     function updateCheckStatus(status)
     {
         var statTag;
         if (!status) { //unchecked
           jQuery(".aSub").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
           else {
            jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
         }
         else { //checked
           jQuery(".aSub").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#allSub").removeAttr('checked');
            jQuery("#allSub").css("opacity", 1);}
         else {
           jQuery("#allSub").attr('checked', true);
           jQuery("#allSub").css("opacity", 1);}
         }
     }

</script>



        
<script type="text/javascript" src="http://www.google.com/jsapi"></script>
<script type="text/javascript">
    google.load("visualization", "1", {});
    google.load("prototype", "1.6");
  </script>
  
  <script type="text/javascript" src="http://systemsbiology-visualizations.googlecode.com/svn/trunk/src/main/js/load.js"></script>
  <script type="text/javascript">
      systemsbiology.load("visualization", "1.0", {packages:["bioheatmap"]});
  </script>

  <tiles:importAttribute />


<script type="text/javascript">
 // Set callback to run when API is loaded
  google.setOnLoadCallback(drawVisualization);

  function drawVisualization() {
  
      // ------------------------
      // EXAMPLE 3
      // ------------------------
      var heatmap3 = new org.systemsbiology.visualization.BioHeatMap(document.getElementById('heatmapContainer_ex3'));
      var data3 = BioHeatMapExampleData.example3();
      heatmap3.draw(data3, {cellHeight: 10, cellWidth: 10, fontHeight: 7, drawBorder: false});
}


  // methods that return example datatables and display options
  var BioHeatMapExampleData = {

      // ---------------------------------------------------------------
      // Example 3 DATA: large random matrix (can use for stress testing)
      // ---------------------------------------------------------------
      example3 : function() {
         var data = new google.visualization.DataTable();
          
         data.addColumn('string', 'Gene Name');
    
         <c:forEach items="${geneCellLines}" var="gcl" varStatus="gcl_status">
         <c:if test="${gcl_status.first}">
         <c:forEach items="${gcl.value}" var="gScores" varStatus="gScores_status" >
         data.addColumn('number', "${gScores.cellLine}");
         </c:forEach>
         </c:if>
         </c:forEach>
    

         <c:forEach items="${geneCellLines}" var="gcl" varStatus="gcl_status">

         data.addRows(1);
         data.setCell(${gcl_status.count - 1}, 0, "${gcl.key}");
         
                  <c:forEach items="${gcl.value}" var="gScores" varStatus="gScores_status" >
                  data.setCell(${gcl_status.count - 1}, ${gScores_status.count }, ${gScores.score});
                  </c:forEach>
                  </c:forEach>
          return data;
      }
  }
</script>
  

  

<html:xhtml />
<script type="text/javascript" src="<html:rewrite page='/js/jquery.qtip-1.0.0-rc3.min.js'/>"></script>
<script type="text/javascript" src="js/tablesort.js"></script>
<link rel="stylesheet" type="text/css" href="css/sorting_experiments.css"/>
<link rel="stylesheet" type="text/css" href="model/css/experiment.css"/>


<div class="body">

<div id="heatmapContainer">
<h1>Heat Map</h1>
<p><b>hurra!</b></p>
<br/>

<div id="heatmapContainer_ex3">
<hr>
</div>


  
  
  

</div>


<!-- heatMap.jsp -->
</div>

<%--

// methods that return example datatables and display options
var BioHeatMapExampleData = {

    // ---------------------------------------------------------------
    // Example 3 DATA: large random matrix (can use for stress testing)
    // ---------------------------------------------------------------
    example3 : function() {
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Gene Name');

       var nCols = 50;
       var nRows = 20;
       for (col = 1; col < nCols; col++) {
           data.addColumn('number', 'Col' + col);

       }
       var max = 10;
       var min = -10;
       for (var i = 0; i < nRows; i++) {
           data.addRows(1);
           data.setCell(i, 0, 'Row' + i);
           for (col = 1; col < nCols; col++) {
               var value = (Math.random() * (max - min + 1)) + min;
               data.setCell(i, col, value);
           }
       }

        return data;
    }
}
--%>






