<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:xhtml/>

<!-- apiPerl.jsp -->
<script type="text/javascript">
<!--//<![CDATA[

  jQuery(document).ready(function() {
     // jQuery("p").hide();
  });

  function showText(pid) {
    // jQuery("#" + pid).slideToggle("slow");
  }

//]]>-->
</script>

<im:boxarea titleKey="api.java.titleKey" stylename="gradientbox" fixedWidth="90%" htmlId="apiJava">
  <form id="apiJavaForm" action="fileDownload.do" method="post">
    <input type="hidden" value="${path}" name="path" />
    <input type="hidden" value="${fileName}" name="fileName" />
    <input type="hidden" value="${mimeType}" name="mimeType" />
    <input type="hidden" value="${mimeExtension}" name="mimeExtension" />
  </form>
<div>
  The Java web service API makes it easy to run queries in <c:out value="${WEB_PROPERTIES['project.title']}"/> directly from Java programs.
  You can use the Java API to construct any query you could run from web interface and fetch the results as tab-separated values.
</div>
<br>
<div>
  <ul>
    <li>
      <div onclick="javascript:showText('prerequisite')"><h3 style="font-weight: bold;">Prerequisites</h3></div>
      <div id="prerequisite" style="padding: 5px">
        All you need is to <a href="javascript: jQuery('#apiJavaForm').submit();">download</a> the distribution package which contains all libraries required to run the program.
      </div>
    </li>
    <li>
      <div onclick="javascript:showText('codegen')"><h3 style="font-weight: bold;">Start to use</h3></div>
      <div id="codegen" style="padding: 5px">
        <span>You can get example Java web service API code either from a Template Query or the QueryBuilder:</span>
          <ul style="padding:0px">
            <li>
              From the QueryBuilder - after creating or editing a query, click "Java" link in the <b>Actions</b> section.
            </li>
            <li>
              From a Template Query form - click "Java" link below the template form.
            </li>
          </ul>
      </div>
    </li>
    <li>
      <div onclick="javascript:showText('editsrc')"><h3 style="font-weight: bold;">Run the program</h3></div>
      <div id="editsrc" style="padding: 5px">
        <span>You can run the program either from command line or within an IDE (e.g. Eclipse). To run the program in your favorite IDE, make sure to import all the libs from the distribution package. To run from command line:</span>
           <ol style="padding:0px">
            <li>
              Save the source code in a Java source file (*.java), the file name and the class name have to be the same. The generated class names might be duplicated, change it if necessary.
            </li>
            <li>
              Unzip the distribution package (see above).
            </li>
            <li>
              <c:set var="dirName" value="${javasieProjectTitle}"/>

              In the intermine-client-x.x directory created make a new directory called
              <c:out value="${dirName}"/> (this is the package name in the generated Java).
              Copy the source file to the new directory.
            </li>
            <li>
              Run the script from the command line under the package directory.
              Firstly make sure you have the permission to execute by using the command <span><i>chmod +x compile-run.sh</i></span>,
              then run <span><i>./compile-run.sh <c:out value="${dirName}"/>/[class name]</i></span>
            </li>
          </ol>
      </div>
    </li>
  </ul>
</div>

</im:boxarea>
<!-- /apiPerl.jsp -->