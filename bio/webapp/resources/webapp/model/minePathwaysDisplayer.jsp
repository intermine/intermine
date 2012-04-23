<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- minePathwaysDisplayer.jsp -->
<div id="mine-pathway-displayer" class="collection-table column-border">

<c:choose>
<c:when test="${gene != null && !empty(gene)}">

  <style>
    #mine-pathway-displayer div.wrapper { height:300px; overflow-y:auto; overflow-x:hidden }
  </style>
  <c:forEach items="${minesForPathways}" var="entry">
    /* might fail, does not use JS slugify! */
    #mine-pathway-displayer table th.<c:out value="${fn:toLowerCase(entry.key.name)}"/> span { padding:1px 4px; border-radius:2px;
      background:${entry.key.bgcolor}; color:${entry.key.frontcolor} !important }
  </c:forEach>

  <script type="text/javascript" charset="utf-8">
  (function() {
    function getFriendlyMinePathways(mine, orthologues, callback) {
      AjaxServices.getFriendlyMinePathways(mine, orthologues, function(pathways) {
        callback(pathways);
      });
    }

    var Grid,
    __indexOf = Array.prototype.indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

    Grid = (function() {

      Grid.prototype.columns = [];

      Grid.prototype.rows = [];

      Grid.prototype.grid = {};

      function Grid(el, head) {
        var column, columnS, row, _i, _len;
        jQuery(el).append(this.body = jQuery('<tbody/>'));
        row = jQuery('<tr/>');
        row.append(jQuery('<th/>'));
        for (_i = 0, _len = head.length; _i < _len; _i++) {
          column = head[_i];
          this.columns.push(columnS = this.slugify(column));
          row.append(jQuery('<th/>', {
            'html': jQuery('<span/>', { 'text': column }),
            'class': columnS + ' loading'
          }));
        }
        row.appendTo(jQuery('<thead/>').appendTo(jQuery(el)));
      }

      Grid.prototype.add = function(row, column, data) {
        var columnS, rowEl, rowS,
          _this = this;
        rowS = this.slugify(row);
        columnS = this.slugify(column);
        if (__indexOf.call(this.rows, rowS) < 0) {
          rowEl = jQuery("<tr/>", {
            'class': rowS
          }).append(jQuery("<td/>", {
            'text': row
          }));
          if (!this.rows.length) {
            this.body.append(rowEl);
            this.rows = [rowS];
          } else {
            (function() {
              var index, row, _ref;
              _ref = _this.rows;
              for (index in _ref) {
                row = _ref[index];
                if (rowS.localeCompare(row) < 0) {
                  _this.rows.splice(index, 0, rowS);
                  _this.grid[row]['el'].before(rowEl);
                  return;
                }
              }
              _this.rows.push(rowS);
              return _this.body.append(rowEl);
            })();
          }
          (function() {
            var columnS, _i, _len, _ref, _results;
            _this.grid[rowS] = {
              'el': rowEl,
              'columns': {}
            };
            _ref = _this.columns;
            _results = [];
            for (_i = 0, _len = _ref.length; _i < _len; _i++) {
              columnS = _ref[_i];
              _results.push(_this.grid[rowS]['columns'][columnS] = (function() {
                var el;
                rowEl.append(el = jQuery('<td/>', {
                  'class': columnS
                }));
                return el;
              })());
            }
            return _results;
          })();
        }
        return this.grid[rowS]['columns'][columnS].html(data);
      };

      Grid.prototype.slugify = function(text) {
        return text.replace(/[^-a-zA-Z0-9,&\s]+/ig, '').replace(/-/gi, "_").replace(/\s/gi, "-").toLowerCase();
      };

      return Grid;

    })();

    // Who?
    var thisMine = '${WEB_PROPERTIES["project.title"]}';

    // Create the listing of all the mines we will have.
    var mines = [];
    // This mine.
    mines.push(thisMine);
    // All the other mines.
    <c:forEach items="${minesForPathways}" var="entry">
      mines.push('${entry.key.name}');
    </c:forEach>

    // Create a new Grid.
    var target = '#mine-pathway-displayer table';
    var grid = new Grid(target, mines);

    // Stop the loading sign.
    jQuery(target).find('thead th.' + grid.slugify(thisMine)).removeClass('loading');

    // Add all pathways for this mine.
    <c:forEach items="${gene.pathways}" var="pathway">
      // Add the results to the grid.
      grid.add('${pathway.name}', thisMine, function() {
        return jQuery('<a/>', {
          'text':  'Yes',
          'href':  "report.do?id=${pathway.id}"
        });
      });
    </c:forEach>

    // Fetch pathways for other mines.
    <c:forEach items="${minesForPathways}" var="entry">
      getFriendlyMinePathways('${entry.key.name}', '${entry.value}', function(data) {
        // Stop the loading sign.
        jQuery(target).find('thead th.' + grid.slugify('${entry.key.name}')).removeClass('loading');
        
        if (data !== null) {
          var results = jQuery.parseJSON(data)['results'];

          // Add the results to the grid.
          jQuery.each(results, function(index, pathway) {
            grid.add(pathway['name'], '${entry.key.name}', function() {
              return jQuery('<a/>', {
                'class':  'external',
                'text':   'Yes',
                'target': '_blank',
                'href':   '${entry.key.url}' + '/report.do?id=' + pathway['id']
              });
            });
          });
        }
      });
    </c:forEach>
  }).call(this);
  </script>

  <div class="header">
    <h3>Pathways from Other Mines</h3>
    <p>
      <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
      Pathway data from other Mines for homologues of this gene.
    </p>
  </div>

  <!-- target for Grid -->
  <div class="wrapper">
    <table></table>
  </div>
</c:when>
<c:otherwise>
  <div class="header">
    <h3>Pathways from Other Mines</h3>
  </div>
  <p>No pathways found.</p>
</c:otherwise>
</c:choose>
</div>
<!-- /minePathwaysDisplayer.jsp -->