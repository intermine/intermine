
   function selectColumnCheckbox(form, type) {
           var deleteButton = document.getElementById('delete_button');
           var removeButton = document.getElementById('remove_button');
           var exportButton = document.getElementById('export_button');

       var columnCheckBox = 'selected_' + type;
       var checked = document.getElementById(columnCheckBox).checked;

       if (deleteButton != null) {
           deleteButton.disabled = !checked;
       }
       if (removeButton != null) {
           removeButton.disabled = !checked;
       }
       if (exportButton != null) {
           exportButton.disabled = !checked;
       }
       with(form) {
           for(var i=0;i < elements.length;i++) {
               var thiselm = elements[i];
               var testString = columnCheckBox + '_';
               if(thiselm.id.indexOf(testString) != -1)
                   thiselm.checked = checked;
           }
       }
   }


   function setDeleteDisabledness(form, type) {

           var deleteButton = document.getElementById('delete_button');
           var removeButton = document.getElementById('remove_button');
           var exportButton = document.getElementById('export_button');

       var checkBoxPrefix = 'selected_' + type + '_';
       var deleteDisable = true;
       var columnCheckBoxChecked = true;
       with(form) {

           for(var i=0;i < elements.length;i++) {
               var thiselm = elements[i];

               if (thiselm.id.indexOf(checkBoxPrefix) != -1) {
                   if (thiselm.checked) {
                       deleteDisable = false;
                   } else {
                       columnCheckBoxChecked = false;
                   }
               }
           }
       }
       if (deleteButton != null) {
           deleteButton.disabled = deleteDisable;
       }
       if (removeButton != null) {
           removeButton.disabled = deleteDisable;
       }
       if (exportButton != null) {
           exportButton.disabled = deleteDisable;
       }
       document.getElementById('selected_' + type).checked = columnCheckBoxChecked;
       return true;
   }


  function noenter() {
    return !(window.event && window.event.keyCode == 13);
  }

  jQuery(function() {
      jQuery('#newApiKeyButton').unbind("click").click(function() {
        AjaxServices.generateApiKey($CURRENT_USER, function(response) {
            jQuery('span.apikey').text(response).removeClass("nokey");
            jQuery('#deleteApiKeyButton').show();
        })
    })
    jQuery('#deleteApiKeyButton').unbind("click").click(function() {
        AjaxServices.deleteApiKey($CURRENT_USER, function(response) {
            if (response === "deleted") {
                jQuery('span.apikey').text("Key Deleted").addClass("nokey");
                jQuery('#deleteApiKeyButton').hide();
            }
        })
    })
  });

  jQuery(function() {
      var $ = jQuery, fail = function(xhr, err, desc) {
          var explanation = desc;
          if (desc == "Bad Request") {
              try {
                  explanation = JSON.parse(xhr.responseText).error;
              } catch (e) {
              }
          }
          new FailureNotification({message: "Could not change preference - " + explanation}).render();
      };
      $('table.user-preferences').delegate('button', 'click', function(evt) {
          evt.preventDefault();
          evt.stopImmediatePropagation();
          var remove, notify, clear
              $btn = $(this),
              $row = $btn.closest('tr'),
              $input = $btn.siblings('input'),
              name = $input.attr('name'), value = $input.val();
          remove = function() {
              $row.slideUp('fast', function() {$row.remove()});
          };
          notify = function() {
              new Notification({message: "Saved change successfully", autoRemove: true}).render();
          };
          clear = function() {
              $input.val('');
          };
          if ($btn.hasClass('delete')) {
              $SERVICE.manageUserPreferences('DELETE', {key: name}).then(remove, fail);
          } else if ($btn.hasClass('update')) {
              $SERVICE.manageUserPreferences('POST', [[name, value]]).then(notify, fail);
          } else if ($btn.hasClass('unset')) {
              $SERVICE.manageUserPreferences('DELETE', {key: name}).then(clear, fail);
          }
      });
      $('form.editable-user-preference input[type="checkbox"]').change(function(evt) {
          evt.preventDefault();
          evt.stopImmediatePropagation();
          var $box = $(this), newState = !$box.attr('checked'), name = $box.attr('name'),
              method = (newState ? 'POST' : 'DELETE'),
              data = (newState ? [[name, 'true']] : {key: name});
          $SERVICE.manageUserPreferences(method, data).fail(fail);
      });
  });
  
  jQuery(function() {
      var $ = jQuery, toName = function(i, e) {return $(e).attr('name')};
      $('button.add-user-preference').click(function(evt) {
          evt.preventDefault();
          evt.stopImmediatePropagation();
          var $btn = $(this), $frm = $btn.next();
          $frm.slideDown('fast', function() { $btn.attr({disabled: true}); });
      });
      $('form.add-user-preference').submit(function(evt) {
          evt.preventDefault();
          evt.stopImmediatePropagation();
          var reset, fail, $frm = $(this), $btn = $frm.prev(),
              name = $frm.find('.prefname').val(), val = $frm.find('.prefvalue').val(),
              currentNames = $('table.user-preferences tbody tr input').map(toName).get();

          reset = function() {
              $frm.find('input').val('');
              // Reasonably high up there on the ugly scale...
              var $tr = $('<tr>'), $form = $('<form class="editable-user-preference">'), $right = $('<td>');
              $('<td>').text(name).appendTo($tr);
              $('<input type="text">').attr({name: name, value: val}).appendTo($form);
              $form.append(" ");
              $('<button class="update">Update</button>').appendTo($form);
              $form.append(" ");
              $('<button class="delete">Delete</button>').appendTo($form);
              $form.appendTo($right);
              $right.appendTo($tr);
              $('table.user-preferences tbody').append($tr); 
          };
          fail = function(msg) { return function() {
              new FailureNotification({message: msg}).render();
          }};
          $frm.slideUp();
          $btn.attr({disabled: false});
          if (_.contains(currentNames, name)) {
              fail(name + " is already set. Please update or delete the current value")();
          } else if (!name || !val) {
              fail("Both a name and a value are required")();
          } else {
              $SERVICE.manageUserPreferences('POST', [[name, val]])
                      .then(reset, fail("Could not add preference"));
          }
      });
      $('form.add-user-preference button.cancel').click(function(evt) {
          evt.preventDefault();
          evt.stopImmediatePropagation();
          var $btn = $(this), $frm = $btn.parent(), $inputs = $frm.find('input'), $adder = $frm.prev();
          $inputs.val('');
          $frm.slideUp();
          $adder.attr({disabled: false});
      });
  });

  jQuery(function() {
      var selector = jQuery('#classNameSelector');
      selector.unbind("change").change(function() {
          var classname = selector.val();
          jQuery('div.classLabels').each(function() {
              if (this.id === 'labelsFor' + classname) {
                  jQuery(this).show();
              } else {
                  jQuery(this).hide();
              }
          });
      });
  });

