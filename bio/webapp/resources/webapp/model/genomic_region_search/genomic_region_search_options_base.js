
   function switchInputs(open, close) {
      jQuery('#' + open + 'Input').attr("disabled", false);
      jQuery('#' + close + 'Input').attr("disabled", true);
      jQuery('#whichInput').val(open);
    }

    function resetInputs() {
       jQuery('#fileInput').attr("disabled", false);
       jQuery('#pasteInput').attr("disabled", false);
       jQuery('#fileInput').val('');
       jQuery('#pasteInput').val('');
    }

    function openInputs() {
       jQuery('#fileInput').attr("disabled", false);
       jQuery('#pasteInput').attr("disabled", false);
    }

