/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

var OtherMines = (function ($, _, AjaxServices) {

  'use strict';

  function getLinks(selector, mine, request) {
    var $context = $(selector);
    if (!$context.length) return;
    var $loading = $('.loading-indicator', $context).addClass('loading');
    var handlers = {callback: handleResults, errorHandler: handleError};

    AjaxServices.getFriendlyMineLinks(mine.name, request.domain, request.identifiers, handlers);

    function handleResults (results) {
      $loading.removeClass('loading');
      if (results && results.length) {
        display(results);
      } else {
        handleError();
      }
    }

    function handleError (message, e) {
      $loading.remove();
      $('.apology', $context).show();
      console.error(message, e);
    }

    function display (results) {
      // A group is a domain (organism) and the matching identifiers
      var $resultsList = $('.results', $context);
      results.forEach(function (group, index) {
        var n = (group.objects && group.objects.length);
        if (!n) return;
        var $groupLi = $(createGroupLi(group));
        $resultsList.append($groupLi);
        var $entries = $('.entries', $groupLi);

        group.objects.forEach(function (obj) {
          var itemLi = createItemLi(group, obj, mine, request);
          $entries.append(itemLi);
        });
      });
    }
  }

  var createGroupLi = _.template('<li><%= domain %><ul class="entries"></ul></li>');

  var itemLiTempl = _.template(
    '<li>'
    + '<a target="_blank" href="<%- mineLink %>">'
    + '<%= name %>'
    + '</a>'
    + '</li>'
  );

  var queryString =  function (params) {
    var parts = [];
    params.forEach(function (param) {
      if (param.value == null) return;
      var part = param.name + '=' + _.escape(param.value);
      parts.push(part);
    });
    return parts.join('&');
  };

  var createItemLi = function (group, obj, mine, request) {
    var params = [
      {name: 'externalids', value: (obj.identifier || obj.name)},
      {name: 'class', value: obj.type},
      {name: 'origin', value: request.origin}
    ];
    if (group.domain === request.domain) {
      params.push({
        name: OtherMines.DOMAIN_PARAMETER_NAME, value: request.domain
      });
    }
    var data = {
      name: (obj.name || obj.identifier),
      mineLink: mine.url + '/portal.do?' + queryString(params)
    };
    return itemLiTempl(data);
  };

  if ($ && _ && AjaxServices) {
    return {
      getLinks: getLinks,
      TYPE_PARAMETER_VALUE: 'gene',
      DOMAIN_PARAMETER_NAME: 'orthologue'
    };
  } else {
    return {getLinks: function () {}};
  }

})(window.jQuery, window._, window.AjaxServices);
