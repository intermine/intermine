
   function switchInputs(open, close) {
      jQuery('#' + open + 'Input').attr("disabled","");
      jQuery('#' + close + 'Input').attr("disabled","disabled");
      jQuery('#whichInput').val(open);
    }

    function resetInputs() {
       jQuery('#fileInput').attr("disabled","");
       jQuery('#pasteInput').attr("disabled","");
       jQuery('#fileInput').val('');
       jQuery('#pasteInput').val('');
    }

    function openInputs() {
       jQuery('#fileInput').attr("disabled","");
       jQuery('#pasteInput').attr("disabled","");
    }

