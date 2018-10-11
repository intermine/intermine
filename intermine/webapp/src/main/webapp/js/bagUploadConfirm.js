(function($) {
  var exports = window.BagUpload = {};

  exports.confirm = function (
      elem, // Where we put the confirmation
      jobId, // The job id
      root, // The webapp location.
      upgrading, // are we upgrading (instead of uploading)
      paths, // css and js resources to load
      extraFilter, // The extra filter to apply
      bagType // The type of this bag
    ) {
    // Show loading sign.
    var job, cleanup, cleaned = false;
    var notify = $('#error_msg');
    var $bagName = $('input#newBagName');
    notify.addClass('loading').show().append($('<div/>', { 'html': 'Please wait &hellip;' }));

    // if we do not have a name of the list generate one from user's time
    if ($bagName.val().length == 0) {
      $bagName.val(generateNewName(bagType, extraFilter));
    }

    var head = [];
    paths.css.forEach(function (path) {
      head.push("<link  href='" + path + "' media='all' rel='stylesheet' type='text/css'/>");
    });
    paths.js.forEach(function (path) {
      head.push("<script charset='UTF-8' src='" + path + "'><\/script>");
    });

    job = $SERVICE.resolutionJob(jobId);

    var pomme = new Pomme({
      'scope': 'apps-c',
      'target': '#iframe',
      template: pommeTemplate(head)
    });

    // Cleanup a job on success or error.
    cleanup = function() {
      if (cleaned) return;
      try {
        if (typeof job !== "undefined" && job !== null) {
          if (typeof job.del === "function") {
            job.del();
          }
        }
      } catch (e) {};
      cleaned = true;
    };

    var onError = function(err) {
      // Try to cleanup.
      cleanup();
      // Show error message.
      notify.removeClass('loading').show().text('Fatal error, cannot continue, sorry');
      // Hide the title.
      $('h1.title').remove();
      // Stop execution.
      throw err;
    };

    // Listen to thrown errors from iframe.
    pomme.on('error', onError);

    job.poll().then(function(results) {
      // Clean the mess up.
      cleanup();

      // No results?
      if (!results.stats.objects.all) {
        // Hide loader msg.
        notify.removeClass('loading').hide();
        // Show the title.
        $('h1.title').text('There are no matches');
        // Strike through the last step.
        $('#list-progress div:last-child span').css('text-decoration', 'line-through');
        return;
      }

      // Show the title.
      $('h1.title').text('Before we show you the results ...');

      // When we or the iframe calls.
      var onSubmit = function(selected) {
        // Inject.
        $('#matchIDs').val(selected.join(' '));
        // Confirm.
        if (upgrading) { // do not check list name
          $('#bagUploadConfirmForm').submit();
        } else {
          validateBagName('bagUploadConfirmForm');
        }
      };

      // Opts for the component-400.
      var opts = {
        'data': results,
        // When the user asked to save this list.
        cb: onSubmit,
        // Visit the portal in our mine (need to not be passing the second param!).
        portal: function(object) {
          // Point straight to the db identifier.
          var path = root + '/report.do?id=' + object.id;
          var popup = window.open(path, '');
          // Was it not blocked?
          if (popup) {
            popup.focus();
          }
        }
      };

      // Done/selected callback.
      pomme.trigger('load', opts, function() {
        // Hide loader msg.
        notify.removeClass('loading').hide();

        // Show the blocks.
        if (upgrading) { // do not show new list name
          $('#additionalMatches').show();
        } else {
          $('#chooseName, #additionalMatches').show();
        }

        // Focus on the input field and listen for Enter presses.
        $('#newBagName').focus().keypress(function(evt) {
          if (evt.which == 13) {
            // Call iframe submitting on callback.
            pomme.trigger('select', onSubmit);
            // Just to make sure...
            evt.preventDefault();
            return false;
          }
        });

        // Keep readjusting the iframe based on its content height.
        var body = pomme.iframe.el.document.body;
        setInterval(function() {
          pomme.iframe.node.style.height = body.scrollHeight + 'px';
        }, 1e2);
      });
    }, cleanup);
  };

  var MONTHS = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split(' ');

  function generateNewName (bagType, extraFilter) {
    var t = new Date();

    if (extraFilter == null || extraFilter == "") {
      extraFilter = "all organisms";
    }
    if (bagType == null || bagType == "") {
      bagType = "Any item";
    }
    return [
      bagType, "list for", extraFilter,
      t.getDate(), MONTHS[t.getMonth()], t.getFullYear(), t.getHours() + "." + t.getMinutes()
    ].join(' ');
  }

  function pommeTemplate (head) {
    var template = [
        "<!doctype html>",
        "<html>",
        "<head>",
        head.join(''),
        "</head>",
        "<body>",
          "<div id='target'></div>",
          "<script>",
            "var channel = new Pomme({ 'scope': 'apps-c' }),",
            "component = require('component-400');",
            "channel.on('load', function(opts, ready) {",
              // Add our target.
              "opts.target = '#target';",
              // Do not send our element, leads to circular references.
              "var orig = opts.portal;",
              "opts.portal = function(object) { orig(object) };",
              // Launch the app keeping the handle for getting the currently selected items.
              "var selected = component(opts);",
              "channel.on('select', function(cb) {",
                // Call back with currently selected items.
                "cb(selected());",
              "});",
              // Say we are ready.
              "ready();",
            "});",
          "<\/script>",
        "</body>",
        "</html>"
      ].join("\n");
  
    console.log("[POMME] template: " + template);
    return function () { return template; };
  }

})(jQuery);
