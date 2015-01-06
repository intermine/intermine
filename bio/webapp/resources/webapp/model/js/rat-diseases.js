var Rat = (function($, _, AjaxServices) {
  'use strict';

  var $context = $('#mine-rat-disease');
  if (!$context.length) return {getDiseases: function () {}};
  var container = $('.intermine_rat_disease', $context);
  if (!container.length) return {getDiseases: function () {}};

  // requires link and name
  var diseaseTemplate = _.template('<li class="<%= className %>"><a href="<%- link %>" target="_blank"><%= name %></a></li>');
  var clearer = '<div style="clear:both"></div>';
  var morer = '<div class="toggle" style="margin-top: 5px"><a class="more">Show <span class="more-count"></span> more diseases</a></div>';

  return {getDiseases: main};
  
  // For use here the results are Array<Array<String>>, where the rows are
  // of order 2 representing the query view: "Gene.doAnnotation.ontologyTerm.id Gene.doAnnotation.ontologyTerm.name"
  function display(response, container) {

    var $morer
      , len = response.results.length
      , maxRows = 12
      , overspill = len - maxRows
      , url = response.mineURL
      , $ul = $('<ul/>').appendTo(container);

    response.results.forEach(function (row, index) {
      var diseaseTerm = {id: row[0], name: row[1]};
      diseaseTerm.link = url + '/report.do?id=' + diseaseTerm.id;
      diseaseTerm.className = (index >= 12 ? 'less' : '');
      var li = diseaseTemplate(diseaseTerm);
      $ul.append(li);
    });
    container.append(clearer);

    if (overspill > 0) {
      $morer = $(morer).appendTo(container).click(function(e) {
        e.preventDefault();
        $('.less', container).show();
        $(this).remove();
      });
      $('.more-count', $morer).text(overspill);
    }
  }

  function main (ratGenes) { 
    AjaxServices.getRatDiseases(ratGenes, function(response) {
      $('.loading', $context).removeClass('loading');
      if (response && response.status === 'online') {
        if (response.results && response.results.length) {
          display(response, container);
        } else {
          container.html("<p>No diseases found.</p>");
        }
      } else {
        container.html("<p>RatMine offline.</p>");
      }
    });
  }
})(window.jQuery, window._, window.AjaxServices);

