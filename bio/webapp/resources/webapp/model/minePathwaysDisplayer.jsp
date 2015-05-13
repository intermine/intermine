<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>



<!-- minePathwaysDisplayer.jsp -->

<div id="minepathwaysdisplayer-wrapper" class="wrapper" style="display: block;">

  <div id="mine-pathway-displayer" class="collection-table column-border">

    <div class="header">
      <h3>Pathways from Other Mines</h3>
      <p>Pathway data from other Mines for homologues of this gene.</p>
    </div>

    <div id="pathwaysappcontainer"></div>


  </div>
</div>

<script type="text/javascript">

(function(){

  var $ = jQuery;

  var paths = {js: {}, css: {}};

  <c:set var="section" value="pathways-displayer"/>

  <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
      paths["${res.type}"]["${res.key}".split(".").pop()] = "${res.url}";
  </c:forEach>

  var imload = function(){
    intermine.load({
      'js': {
          // Q, jQuery@2.x, Backbone and Underscore are part of
          // the default set of dependencies guaranteed to us.
          // If that ever changes, the following might fail.
          'PathwaysDisplayer': {
            'path': paths.js.PathwaysDisplayer
          }
      },
      'css': {
        'pathwaysDisplayerCSS': {
          'path': paths.css.pathwaysDisplayerCSS
        }
      }
    }, function(err) {

      if (err) throw err;

      friendlyMines = {};

      <c:forEach items="${minesForPathways}" var="entry">
        friendlyMines["${entry.key.name}"] = "${entry.key.url}";
        </c:forEach>

        //friendlyMines['${WEB_PROPERTIES["project.title"]}'] = $SERVICE.root;
        friendlyMines['${localMine.name}'] = "${localMine.url}";

        require('PathwaysDisplayer')(
      {
             
              friendlyMines: friendlyMines,                        
              gene: "${gene.primaryIdentifier}",
              target: "#pathwaysappcontainer",
              themeColor: "${localMine.bgcolor}"
      });

    });
  };

  try {

    imload();

  } catch (error) {

    $('#pathwaysappcontainer').html(
      $('<div/>', {'text': 'This app requires jQuery 2.x.x', 'style': 'padding-left: 14px; font-weight: bold'})
    );

    $('#mine-pathway-displayer').addClass("warning");
  }
})();





</script>

    



<!-- /minePathwaysDisplayer.jsp -->
