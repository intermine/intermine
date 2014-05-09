<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>

<html:form action="/bagUploadConfirm" focus="newBagName" method="post" enctype="multipart/form-data">
  <input type="hidden" name="matchIDs" id="matchIDs">
  <input type="hidden" name="bagType" value="${bagType}">

  <div class="body">
    <!-- title -->
    <h1 class="title">Verifying identifiers</h1>

    <!-- progress -->
    <!--
    <div id="list-progress">
        <div class="gray"><strong>1</strong> <span>Upload list of identifiers</span></div
        ><div class="gray-to-white">&nbsp;</div
        ><div class="white"><strong>2</strong> <span>Verify identifier matches</span></div><div class="white-to-gray">&nbsp;</div
        ><div class="gray"><img src="images/icons/lists-16.png" alt="list" />
          <span>List analysis</span>
        </div>
    </div>
    <div class="clear">&nbsp;</div>
    -->
    
    <!-- choose name -->
    <div id="chooseName" style="display:none">
      <h2>Choose a name for the list</h2>
      <div style="clear:both;"></div>
      <div class="formik">
        <input id="newBagName" type="text" name="newBagName" value="${bagName}">
        <span>(e.g. Smith 2013)</span>
      </div>
      <div style="clear:both;"></div>
    </div>
    
    <!-- upgrading a list scenario -->
    <c:if test="${empty buildNewBag}">
      <input type="hidden" name="upgradeBagName" value="${bagName}"/>
    </c:if>

    <!-- additional matches -->
    <div id="additionalMatches" class="body" style="display:none">
      <div class="oneline">
        <h2>Add additional matches</h2>
        <div id="iframe"></div>
      </div>
      <div style="clear:both;"></div>
    </div>
  </div>

</html:form>

<style>
iframe { border:0; width: 100%; }
</style>

<script type="text/javascript">
(function($) {
    // Are we upgrading a list?
    var upgrading = false;
    <c:if test="${empty buildNewBag}">upgrading = true;</c:if>

    // Show loading sign.
    var notify = $('#error_msg');
    notify.addClass('loading').show().append($('<div/>', { 'html': 'Please wait &hellip;' }));

    // if we do not have a name of the list generate one from user's time
    if ($('input#newBagName').val().length == 0) {
      var extraFilter = "all organisms".toLowerCase(),
        t = new Date(),
        m = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
      $('input#newBagName').val("Gene list for " + extraFilter + " " + t.getDate() + " " + m[t.getMonth()] + " " + t.getFullYear() + " " + t.getHours() + "." + t.getMinutes());
    }

    // Get the paths to libraries.
    var paths = { js: {}, css: {} };
    <c:set var="section" value="component-400"/>
    <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">      
        paths["${res.type}"]["${res.key}".split(".").pop()] = "${res.url}";
    </c:forEach>

    // Apple lives here.
    var Pomme = require('pomme'),
      pomme = new Pomme({
        'scope': 'apps-c',
        'target': '#iframe',
        template: function() {
          return [
            "<!doctype html>",
            "<html>",
            "<head>",
              "<link  href='" + paths.css.all + "' medial='all' rel='stylesheet' type='text/css'/>",
              "<script src='" + paths.js.app + "'><\/script>",
              "<script src='" + paths.js.pomme + "'><\/script>",
            "</head>",
            "<body>",
              "<div id='target'></div>",
              "<script>",
                "var Pomme = require('pomme'),",
                "channel = new Pomme({ 'scope': 'apps-c' }),",
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
        }
      });

    var job, cleanup, cleaned = false;
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

    // Point here.
    var root = window.location.protocol + "//" + window.location.host + "/${WEB_PROPERTIES['webapp.path']}";

    // Poll & retrieve the results of the job.
    job = new intermine.IDResolutionJob("${jobUid}", new intermine.Service({
      "root": root,
      "token": "${PROFILE.dayToken}",
      "help": "${WEB_PROPERTIES['feedback.destination']}",
      "errorHandler": onError
    }));
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

      // console.log(JSON.stringify(results, null, 4));

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
    });

})(jQuery);
</script>
<!-- /bagUploadConfirm.jsp -->