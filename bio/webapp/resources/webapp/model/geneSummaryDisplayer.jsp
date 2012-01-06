<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<div id="gene-summary-displayer">
  <style>
  #gene-summary-displayer li { display:inline-block; margin:0; }
  #gene-summary-displayer li a.link { margin:0 10px 0 0; display:inline-block; vertical-align:top; text-align:center;
    background:#EDEDED; padding:5px; color:inherit; }
  #gene-summary-displayer li a.link:hover { background:#E0E0E0; text-decoration:none; color:inherit; }
  #gene-summary-displayer li div.label strong { margin-bottom:5px; display:block; }
  #gene-summary-displayer li ul li { display:block; margin:0; }
  #gene-summary-displayer li ul li div { display:inline-block; }
  </style>

  <ul>
    <c:forEach var="field" items="${summary.fields}">
      <c:if test="${field.value['data'] != null}">
        <li class="main">
          <a href="#${field.value['anchor']}" title="see more" class="link">
            <div class="label"><strong>${field.key}</strong></div>
            <div class="data">
             <c:choose>
               <c:when test="${field.value['type'] == 'integer'}">
                 ${field.value['data']}
               </c:when>
               <c:when test="${field.value['type'] == 'map'}">
                 <ul>
                 <c:forEach var="innerField" items="${field.value['data']}">
                   <li>
                     <div class="data">${innerField.value}</div> <div class="label">${fn:replace(innerField.key, '_', ' ')}</div>
                   </li>
                 </c:forEach>
                 </ul>
               </c:when>
               <c:when test="${field.value['type'] == 'image'}">
                  <span id="placeholder-for-${field.key}"></span>
                  <script type="text/javascript">
                    (function() {
                        jQuery(new Image())
                          .load(function() {
                            jQuery('span#placeholder-for-${field.key}').html(this);
                          })
                          .attr('src', "${field.value['data']}");
                    })();
                  </script>
               </c:when>
               <c:otherwise>
                 ${field.value['data']}
               </c:otherwise>
             </c:choose>
            </div>
          </a>
        </li>
      </c:if>
    </c:forEach>
  </ul>
  <div class="clear"></div>
  <script type="text/javascript">
  (function() {
    jQuery("#gene-summary-displayer li a").click(function(e){
      jQuery("a[name='" + jQuery(this).attr('href').replace('#', '') + "'].anchor").scrollTo('slow', 'swing', -20);
      e.preventDefault();
    });
  })();
  </script>
</div>