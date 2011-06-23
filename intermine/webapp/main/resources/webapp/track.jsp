<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="title" ignore="true" />
<tiles:importAttribute name="description" ignore="true" />
<tiles:importAttribute name="id" ignore="false" />
<tiles:importAttribute name="mainColumn" ignore="true" />
<!-- track.jsp -->

<html:xhtml/>

<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8"/>
<link rel="stylesheet" href="css/tracks.css" type="text/css" />
<script type='text/javascript' src='https://www.google.com/jsapi'></script>
<script type='text/javascript' src='js/track.js'></script>
    <script type='text/javascript'>
    google.load('visualization', '1', {'packages':['annotatedtimeline']});
    google.setOnLoadCallback(function() {drawTimeChart('${id}', '${title}');});

    if ('${id}' != 'login') { //login tracks only annotatedtimeline chart
        google.load('visualization', '1', {packages:['table']});
      if('${id}' == 'template') {
        google.load("visualization", "1", {packages:["corechart"]});
        google.setOnLoadCallback(function() {drawPieAndTableChart("LASTMONTH", '${id}','${mainColumn}');});
      } else {
        google.setOnLoadCallback(function() {drawTableChart("LASTMONTH", '${id}','${mainColumn}');});
      }
    }
   </script>

<div class="body">

<div id="trackcontainer${id}" class="trackscontainer gradientbox">
<h2><c:out value="${title}"/></h2>
<i>${description}</i>
<div id='chart_div${id}' style='width: 1000px; height: 280px; margin-bottom: 50px'></div>

<c:if test="${id != 'login'}">
<div id='filter' style='float:left; margin-bottom: 20px'>
<c:choose>
<c:when test="${id == 'template'}">
<input type="radio" name="timeRange${id}" value="LAST2WEEKS" onChange="drawPieAndTableChart(this.value, '${id}','${mainColumn}')"/> Last 2 weeks
<input type="radio" name="timeRange${id}" value="LASTMONTH" checked onChange="drawPieAndTableChart(this.value, '${id}','${mainColumn}')"/> Last month
<input type="radio" name="timeRange${id}" value="LAST3MONTHES" onChange="drawPieAndTableChart(this.value, '${id}','${mainColumn}')"/> Last 3 months
<input type="radio" name="timeRange${id}" value="LASTYEAR" onChange="drawPieAndTableChart(this.value, '${id}','${mainColumn}')"/> Last year
</c:when>
<c:otherwise>
<input type="radio" name="timeRange${id}" value="LAST2WEEKS" onChange="drawTableChart(this.value, '${id}','${mainColumn}')"/> Last 2 weeks
<input type="radio" name="timeRange${id}" value="LASTMONTH" checked onChange="drawTableChart(this.value, '${id}','${mainColumn}')"/> Last month
<input type="radio" name="timeRange${id}" value="LAST3MONTHES" onChange="drawTableChart(this.value, '${id}','${mainColumn}')"/> Last 3 months
<input type="radio" name="timeRange${id}" value="LASTYEAR" onChange="drawTableChart(this.value, '${id}','${mainColumn}')"/> Last year
</c:otherwise>
</c:choose>
</div>
<div style='clear:both;'></div>
<div id='piechart_div${id}' style='float:left'></div>
<div id='table_div${id}' style='float:left'></div>
<div style='clear:both;'></div>
</c:if>
</div>
</div>  <!-- whole page body -->
<!-- /track.jsp -->