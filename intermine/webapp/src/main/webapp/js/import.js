   function switchInputs(open, close) {
      jQuery('#' + open).attr("disabled", false);
      jQuery('#' + close).attr("disabled", true);
    }
   function openInputs(input1, input2) {
       jQuery('#' + input1).attr("disabled", false);
       jQuery('#' + input2).attr("disabled", false);
    }
   function resetInputs(input1, input2) {
       jQuery('#' + input1).attr("disabled", false);
       jQuery('#' + input2).attr("disabled", false);
       jQuery('#' + input1).val('');
       jQuery('#' + input2).val('');
    }