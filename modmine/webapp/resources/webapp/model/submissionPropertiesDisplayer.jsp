<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- submissionPropertiesDisplayer.jsp -->

<div class="collection-table column-border-by-2" id="submission-properties-div">

<table id="submission-properties-table">
<tbody>
 <tr>
   <td style="width:17%;">Organism:</td>
   <td>
     <c:forEach var="organism" items="${organismMap}" varStatus="status">
       <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${organism.key}" style="text-decoration: none;"><strong>${organism.value}</strong></a>
     </c:forEach>
   </td>


   <td valign="top">Strain/Target:</td>
   <td id="strainContent" >
     <c:choose>
       <c:when test="${not empty strainInfoList}">
         <c:forEach var="strain" items="${strainInfoList}" varStatus="status">
           <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${strain.id}" style="text-decoration: none;">
           <strong>${strain.name}</strong></a>
           <c:choose>
           <c:when test="${not empty strain.target.name}">/
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${strain.target.id}"
                title="More about this target">
            <c:out value="${strain.target.name}"/></html:link>
           </c:when>
           <c:otherwise>
            <c:if test="{not empty strain.targetName}">/
             <a href="${strain.wikiLink}" style="text-decoration: none;" class="value extlink"><strong>${strain.targetName}</strong></a>
            </c:if>
           </c:otherwise>
           </c:choose>
           <c:if test="${!status.last}">,  </c:if>
         </c:forEach>
       </c:when>
       <c:otherwise>
         <i>not available</i>
       </c:otherwise>
     </c:choose>
   </td>

</tr>
<tr>
   <td valign="top">Cell Line:</td>
   <td>
     <c:choose>
       <c:when test="${not empty cellLineMap}">
         <c:forEach var="celline" items="${cellLineMap}" varStatus="status">
           <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${celline.key}" style="text-decoration: none;"><strong>${celline.value}</strong></a>
           <c:if test="${!status.last}">,  </c:if>
         </c:forEach>
       </c:when>
       <c:otherwise>
         <i>not available</i>
       </c:otherwise>
     </c:choose>
   </td>
   <td valign="top">Tissue:</td>
   <td>
     <c:choose>
       <c:when test="${not empty tissueMap}">
         <c:forEach var="tissue" items="${tissueMap}" varStatus="status">
           <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${tissue.key}" style="text-decoration: none;"><strong>${tissue.value}</strong></a>
           <c:if test="${!status.last}">,  </c:if>
         </c:forEach>
       </c:when>
       <c:otherwise>
         <i>not available</i>
       </c:otherwise>
     </c:choose>
   </td>
   </tr>
   <tr>
     <td valign="top">Developmental Stage:</td>
     <td>
       <c:choose>
         <c:when test="${not empty developmentalStageMap}">
           <c:forEach var="developmentalstage" items="${developmentalStageMap}" varStatus="status">
             <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${developmentalstage.key}" style="text-decoration: none;"><strong>${developmentalstage.value}</strong></a>
             <c:if test="${!status.last}">,  </c:if>
           </c:forEach>
         </c:when>
         <c:otherwise>
           <i>not available</i>
         </c:otherwise>
       </c:choose>
     </td>
     <td valign="top">Array:</td>
     <td>
       <c:choose>
         <c:when test="${not empty arrayMap}">
           <c:forEach var="array" items="${arrayMap}" varStatus="status">
             <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${array.key}" style="text-decoration: none;"><strong>${array.value}</strong></a>
             <c:if test="${!status.last}">,  </c:if>
           </c:forEach>
         </c:when>
         <c:otherwise>
           <i>not available</i>
         </c:otherwise>
       </c:choose>
     </td>
     </tr>
     <tr>

     <td valign="top">Antibody/Target:</td>
     <td id="antibodyContent" >
       <c:choose>
         <c:when test="${not empty antibodyInfoList}">
           <c:forEach var="antibody" items="${antibodyInfoList}" varStatus="status">
           <c:if test="${!fn:containsIgnoreCase(antibody.name, 'No Antibody')}">

             <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.id}" style="text-decoration: none;">
             <strong>${antibody.name}</strong></a>

             <c:choose>
             <c:when test="${not empty antibody.target.name}">/
             <strong>
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.target.id}"
                  title="More about this target">
              <c:out value="${antibody.target.name}"/></html:link>
              </strong>
             </c:when>
             <c:otherwise>
             <c:choose>
             <c:when test="${not empty antibody.target.symbol}">/
             <strong>
              <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.target.id}"
                  title="More about this target">
              <c:out value="${antibody.target.symbol}"/></html:link>
              </strong>
             </c:when>
             <c:otherwise>
             <c:if test="${not empty antibody.targetName}">/
               <a href="${antibody.wikiLink}" style="text-decoration: none;" class="value extlink">
               <strong>${antibody.targetName}</strong></a>
               </c:if>
             </c:otherwise>
             </c:choose>
             </c:otherwise>
             </c:choose>


             <c:if test="${!status.last}">,  </c:if>
             </c:if>

             </c:forEach>
         </c:when>
         <c:otherwise>
           <i>not available</i>
         </c:otherwise>
       </c:choose>
     </td>
     <td valign="top">Technique:</td>
     <td><strong>${technique}</strong>
</td>

 </tr>
</tbody>
</table>

</div>


<script type="text/javascript">

     var submissionPropertyJSON;
     if ('${submissionPropertyJSON}' != "" || '${submissionPropertyJSON}' != "[]") {
         submissionPropertyJSON = jQuery.parseJSON('${submissionPropertyJSON}');

         for(var i in submissionPropertyJSON){

            var size = submissionPropertyJSON[i].value.length;
            var type = submissionPropertyJSON[i].type;
            var value = submissionPropertyJSON[i].value;

            var html = "<tr><td valign='top'>" + type + ":</td><td id='" + type.replace(/\s+/g, "-") + "-content'>";

            if (size > 2) {
                for (var j=0;j<2;j++){
                    var id = value[j].id;
                    var name = value[j].name;

                    html = html + "<a href=\"/${WEB_PROPERTIES['webapp.path']}/report.do?id=" + id + "\" style='text-decoration: none;'><strong>" + name + "</strong></a>, ";
                }

                html = html + "<span class='fakelink' onclick='expand_submission_property(\"" + type.replace(/\s+/g, "-") + "-content" + "\");'>... display all " + size + " records</span>";

            } else {
                for (var j in value){

                    var id = value[j].id;
                    var name = value[j].name;
                    // if the prop is target gene, let's link to the gene
                    if (type == "target gene") {
                        html = html + "<a href=\"/${WEB_PROPERTIES['webapp.path']}/portal.do?class=Gene&externalids=" + name + "\" style='text-decoration: none;'><strong>" + name + "</strong></a>";
                    } else {
                        html = html + "<a href=\"/${WEB_PROPERTIES['webapp.path']}/report.do?id=" + id + "\" style='text-decoration: none;'><strong>" + name + "</strong></a>";
                    }

                    if (j < size-1) {
                        html = html + ", ";
                    }
                }
            }

            html = html + "</td></tr>";
            jQuery("#submission-properties-table > tbody").append(html);
         }
     }

     function expand_submission_property(tdid) {
        var html = "";

        for (var i in submissionPropertyJSON){
          var size = submissionPropertyJSON[i].value.length;
          var type = submissionPropertyJSON[i].type;
          var value = submissionPropertyJSON[i].value;

            if (type.replace(/\s+/g, "-") + "-content" == tdid) {
              for (var j in value){
                  var id = value[j].id;
                  var name = value[j].name;

                    if (j == size-1) {
                        html = html + "<a href=\"/${WEB_PROPERTIES['webapp.path']}/report.do?id=" + id + "\" style='text-decoration: none;'><strong>" + name + "</strong></a>";
                    } else {
                        html = html + "<a href=\"/${WEB_PROPERTIES['webapp.path']}/report.do?id=" + id + "\" style='text-decoration: none;'><strong>" + name + "</strong></a>, ";
                    }
                 }
                html = html + "<span class='fakelink' onclick='collapse_submission_property(\"" + tdid + "\");'> [collapse]</span>";
            }
        }

        jQuery("#" + tdid).html(html);
     }


     function collapse_submission_property(tdid) {
        var html = "";

        for (var i in submissionPropertyJSON){
            var size = submissionPropertyJSON[i].value.length;
            var type = submissionPropertyJSON[i].type;
            var value = submissionPropertyJSON[i].value;

            if (type.replace(/\s+/g, "-") + "-content" == tdid) {
                for (var j=0;j<2;j++){
                    var id = value[j].id;
                    var name = value[j].name;

                    html = html + "<a href=\"/${WEB_PROPERTIES['webapp.path']}/report.do?id=" + id + "\" style='text-decoration: none;'><strong>" + name + "</strong></a>, ";
                }

                html = html + "<span class='fakelink' onclick='expand_submission_property(\"" + tdid + "\");'>... display all " + size + " records</span>";
            }
        }

        jQuery("#" + tdid).html(html);
     }

</script>

<!-- /submissionPropertiesDisplayer.jsp -->