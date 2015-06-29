/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

// request links from the server, which look like:
//   [
//    { 
//      objects:[{name, identifier, type}],
//      domain": "R. norvegicus"
//    }
//   ]
var FriendlyMines = (function ($, AjaxServices) {

    'use strict';

    // TODO: make DOMAIN_PARAMETER and OBJECT_TYPE configurable
    // Exported object
    return {
      getLinks: getLinks,
      DOMAIN_PARAMETER: 'orthologue',
      OBJECT_TYPE: 'gene'
    };

    // Hoisted private functions.

    function getLinks(context, mine, request) {
        var loading = $('.loading-indicator', context).addClass('loading');
        var handlers = {callback: success, errorHandler: failure};

        AjaxServices.getFriendlyMineLinks(mine.name, request.domain, request.identifiers, handlers);

        function success(results) {
            // switch off loading img
            loading.removeClass('loading');
            if (results && results.length) {
                display(results);
            } else {
                loading.html('<span class="error">No results found</span>');
            }
        }

        function failure(message, err) {
            loading.removeClass('loading')
                   .html('<span class="error">Could not retrieve results</span>');
            console.error('Error retrieving results', message, err);
        }

        function display(results) {
            var resultsList = $('.results', context);

            // for each organism for which the mine has orthologues
            // show links to each set of matching identifiers, only showing
            // the top 3 at first, and providing an option to see all.
            results.forEach(function(group, index) {
              var show = (index <= 2); // Show only first 3.
              if (group.objects && group.objects.length) {
                  var activate = function (e) {
                    e.preventDefault();
                    sendUserToPortal(mine, group, request);
                  };
                  var li = createGroupLi(group, activate, show);
                  resultsList.append(li);
              }
            });
            // If we need one, add a 'Show all' link.
            if (results.length > 3) {
              var toggler = createToggler(function (e) {
                  e.preventDefault();
                  $('li', resultsList).show();
                  toggler.remove();
              });
              resultsList.append(toggler);
            }
        }
    }

    // Send a user off to the given mine.
    function sendUserToPortal (mine, group, request) {
        var types = _.uniq(_.pluck(group.objects, 'type'));
        if (types.length !== 1) {
          console.error("Multiple types found in group - selecting first one of " + types.join(', '));
        }

        var data = [
          {
            name: 'externalids',
            value: _.pluck(group.objects, 'identifier').join(',')
          },
          {
            name: 'class',
            value: types[0]
          },
          {
            name: 'origin',
            value: request.origin
          }
        ];
        im.log('Making portal request with:', data);

        makePostRequest(mine.url + '/portal.do', data);
    }

    // Construct a request in the shape of a form.
    function makePostRequest(url, data) {
        var form = $('<form/>', {
          action: url,
          target: '_blank',
          method: 'post',
          style: 'display:none;'
        });
        data.forEach(function (paramDef) {
          form.append($('<input/>', paramDef));
        });
        form.appendTo('body');

        // Send off the user
        form.submit()
        // Remove our form.
        form.remove();
    }

    // Render a group element.
    function createGroupLi (group, activate, show) {
      return $('<li/>', {
        'class': 'partner-link-result-group',
        style: (show ? '' : 'display:none;'),
        html: $('<a/>', {
          href: '#',
          text: group.domain,
          target: '_blank',
          click: activate
        })
      });
    }

    // Render a toggler.
    function createToggler (toggle) {
      return $('<li/>', {
        'class': 'toggler',
        html: $('<a/>', {
          text: 'Show all',
          href: '#',
          click: toggle
        })
      });
    }

})(jQuery, AjaxServices);

