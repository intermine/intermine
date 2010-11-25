<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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
    jQuery("#" + pid).slideToggle("slow");
  }

//]]>-->
</script>

<im:boxarea titleKey="api.java.titleKey" stylename="gradientbox" minWidth="800px" htmlId="apiJava">
  <form id="apiJavaForm" action="fileDownload.do" method="post">
    <input type="hidden" value="${path}" name="path" />
    <input type="hidden" value="${fileName}" name="fileName" />
    <input type="hidden" value="${mimeType}" name="mimeType" />
    <input type="hidden" value="${mimeExtension}" name="mimeExtension" />
  </form>
<div>
  Web Service Java API makes it easy for users to query data from a Java program.
  The Java source code now will be automatically generated when a user edits template or creates a query from QueryBuilder.
  Users can send query request from the program and get the results as tab-separated values.
</div>
<br>
<div>
  <ul>
    <li>
      <div onclick="javascript:showText('prerequisite')"><span class="fakelink">Prerequisites</span></div>
      <div id="prerequisite"  style="padding: 5px">
        All you need is to <a href="javascript: jQuery('#apiJavaForm').submit();">download</a> the distribution package which contains all the libraries to run the program.
      </div>
    </li>
    <li>
      <div onclick="javascript:showText('codegen')"><span class="fakelink">Start to use</span></div>
      <div id="codegen"  style="padding: 5px">
        <span>You can use the web service API either from editing a template or QueryBuilder:</span>
          <ul style="padding:0px">
            <li>
              From QueryBuilder - After creating or editing a query, click "java" link in the <b>Actions</b> section.
            </li>
            <li>
              From Template - After editing a template, click "java" link under the bottom of template.
            </li>
          </ul>
      </div>
    </li>
    <li>
      <div onclick="javascript:showText('editsrc')"><span class="fakelink">Run the program</span></div>
      <div id="editsrc"  style="padding: 5px">
        <span>You can run the program either from command line or within an IDE (e.g. Eclipse). To run the program in your favorite IDE, make sure to import all the libs from the distribution package. To run from command line:</span>
           <ol style="padding:0px">
            <li>
              Save the source code in a Java source file (*.java), the file name and the class name have to be the same. The generated class names might be duplicated, change it if necessary.
            </li>
            <li>
              Unzip the distribution package.
            </li>
            <li>
              Create a directory with the same name as the package name in the java source code under the package directory. Copy the source file to the new directory.
            </li>
            <li>
              Run the script from the command line under the package directory. Firstly make sure you have the permission to execute by using the command <span><i>chmod +x compile-run.sh</i></span>, then run <span><i>./compile-run.sh [package name]/[class name]</i></span>.
            </li>
          </ol>
      </div>
    </li>
  </ul>
</div>

</im:boxarea>
<!-- /apiPerl.jsp -->