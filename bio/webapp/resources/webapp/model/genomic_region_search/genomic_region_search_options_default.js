//=== A mine specific script ===
//=== This is the default script for generic purpose ===

    jQuery(document).ready(function() {

        var htmlToInsert = '<li>' +
                           '<span>Select Organism:&nbsp;</span>' +
                           '<select id="organisms" name="organism">';

        // iterate through the object
        jQuery.each(webDataJSON.organisms, function() {
            htmlToInsert += '<option value="'+this+'">'+this+'</option>';
        });

        htmlToInsert += '</select>' + '<span id="genomeBuild" style="padding:10px;"></span>'
                        '</li><br>';

        htmlToInsert += '<li>' +
                        '<p id="selectFeatureTypes" style="padding-bottom:8px;"></p>' +
                        '<table id="featureTypes" cellpadding="0" cellspacing="0" border="0">' +
                        '</table>' +
                        '</li>' +
                        '<br>';

        jQuery(htmlToInsert).insertBefore('#genomicRegionInput');

        // when organism changes, the feature types will change accordingly
        jQuery("#organisms").change(function () {

            // Reset textarea and file input
            resetInputs();

            jQuery("#organisms option:selected").each(function () {
                appendGenomeBuild(jQuery(this).text());
                appendFeatureTypes(jQuery(this).text());
            });
        })
        .trigger('change');
    });

   function appendGenomeBuild(org) {
       for(i in webDataJSON.genomeBuilds){
           if (webDataJSON.genomeBuilds[i].organism == org) {
               jQuery("#genomeBuild").html("<i>genome build: <span id='current-genome-version'>" + webDataJSON.genomeBuilds[i].genomeBuild + "</span></i>");
           }
       }

   }

   function appendFeatureTypes(org) {

        var featureTypes = jQuery("#featureTypes").empty(),
            row = "<tr></tr>",
            input = "<input type='checkbox' checked='yes' class='featureType' name='featureTypes'>",
            cell = "<td width='300'></td>",
            br = "<br/>",
            sp = "&nbsp;",
            onClick = function() {uncheck(this.checked, 'featureTypes')},
            columns = 3;

         for(var i in webDataJSON.featureTypes){
               if (webDataJSON.featureTypes[i].organism == org) {
                     var feature_size = webDataJSON.featureTypes[i].features.length,
                         rows = Math.ceil(feature_size/columns);

                     for (j = 0; j < rows; j++)
                     {
                        var rowElem = jQuery(row);
                        for (k = 0; k < columns; k++)
                        {
                            var current_loc = j + k*rows;
                            if (!(current_loc >= feature_size)) {
                                var current = webDataJSON.featureTypes[i].features[current_loc].featureType;
                                var displayName = $MODEL_TRANSLATION_TABLE[current].displayName ? $MODEL_TRANSLATION_TABLE[current].displayName : current;
                                var desciption = webDataJSON.featureTypes[i].features[current_loc].description;
                                var desBox = "<a onclick=\"document.getElementById('ctxHelpTxt').innerHTML='" + displayName + ": " + desciption
                                             + "';document.getElementById('ctxHelpDiv').style.display=''; window.scrollTo(0, 0);return false\" title=\"" + desciption
                                             + "\"><img class=\"tinyQuestionMark\" src=\"images/icons/information-small-blue.png\" alt=\"?\" style=\"padding: 4px 3px\"></a>"
                                var cellElem = jQuery(cell);
                                var ckbx = jQuery(input).attr("value", current).click(onClick);
                                cellElem.append(ckbx).append(sp).append(displayName).append(desBox);
                                rowElem.append(cellElem);
                            }
                        }
                        featureTypes.append(rowElem);
                    }
               }
         }

         if (featureTypes.children.length) {
             jQuery("#selectFeatureTypes").html("<input id=\"check\" type=\"checkbox\" checked=\"yes\" onclick=\"checkAll(this.id)\"/>&nbsp;Select Feature Types:");
         }
         else {
             jQuery("#selectFeatureTypes").html("Select Feature Types:<br><i>"+org+" does not have any features</i>");
         }
   }

   // (un)Check all featureType checkboxes
   function checkAll(id)
   {
     jQuery(".featureType").attr('checked', jQuery('#' + id).is(':checked'));
     jQuery("#check").css("opacity", 1);
   }

   // check/uncheck any featureType checkbox
   function uncheck(status, name)
   {
         var statTag;
         if (!status) { //unchecked
           jQuery(".featureType").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#check").attr('checked', true);
            jQuery("#check").css("opacity", 0.5); }
           else {
            jQuery("#check").removeAttr('checked');
            jQuery("#check").css("opacity", 1);}
         }
         else { //checked
           jQuery(".featureType").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#check").attr('checked', true);
           jQuery("#check").css("opacity", 0.5); }
         else {
           jQuery("#check").attr('checked', true);
           jQuery("#check").css("opacity", 1);}
         }
   }

   function validateBeforeSubmit() {
       var checkedFeatureTypes = [];
       jQuery(".featureType").each(function() {
           if (this.checked) { checkedFeatureTypes.push(this.value); }
       });
       var checkedFeatureTypesToString = checkedFeatureTypes.join(",");

       if (jQuery(".featureType").val() == null || checkedFeatureTypesToString == "") {
           alert("Please select some feature types...");
           return false;
       }

       if (jQuery("#pasteInput").val() == "" && jQuery("#fileInput").val() == "") {
           alert("Please type/paste/upload some genome regions...");
           return false;
       }

       if (jQuery("#pasteInput").val() != "") {
             // Regex validation
             var ddotsRegex = /^[^:\t\s]+: ?\d+\.\.\d+$/;
             var tabRegex = /^[^\t\s]+(\t\d+){2}/; // this will match the line start with
             var dashRegex = /^[^:\t\s]+: ?\d+\-\d+$/;
             var snpRegex = /^[^:\t\s]+: ?\d+$/;
             var emptyLine = /^\s*$/;
             var ddotstagRegex = /^[^:]+: ?\d+\.\.\d+: ?\d+$/;

             var spanArray = jQuery.trim(jQuery("#pasteInput").val()).split("\n");
             var lineNum;
             for (i=0;i<spanArray.length;i++) {
               lineNum = i + 1;
               if (spanArray[i] == "") {
                   alert("Line " + lineNum + " is empty...");
                   return false;
               }
               if (!spanArray[i].match(ddotsRegex) &&
                   !spanArray[i].match(ddotstagRegex) &&
                   !spanArray[i].match(tabRegex) &&
                   !spanArray[i].match(dashRegex) &&
                   !spanArray[i].match(snpRegex) &&
                   !spanArray[i].match(emptyLine)
                   ) {
                      alert(spanArray[i] + " doesn't match any supported format...");
                      return false;
               }
         }
       }
       return true;
   }

   function loadExample(exampleSpans) {
      switchInputs('paste','file');
      jQuery('#pasteInput').focus();
      jQuery('#pasteInput').val(exampleSpans);

      return false;
    }
