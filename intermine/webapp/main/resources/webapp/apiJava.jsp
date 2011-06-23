<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<html:xhtml/>

<!-- apiJava.jsp -->

<im:boxarea titleKey="api.java.titleKey" stylename="gradientbox" fixedWidth="90%" htmlId="apiJava">
  <form id="apiJavaForm" action="fileDownload.do" method="post">
    <input type="hidden" value="${path}" name="path" />
    <input type="hidden" value="${fileName}" name="fileName" />
    <input type="hidden" value="${mimeType}" name="mimeType" />
    <input type="hidden" value="${mimeExtension}" name="mimeExtension" />
  </form>
<div>
  The Java web service client library makes it easy to run queries in <c:out value="${WEB_PROPERTIES['project.title']}"/> directly from Java programs.
  You can use this libray to construct any query you could run from web interface and fetch the results
  in as either tables of values, or JSON data structures (see <a href="http://json.org">json.org</a>).
  <br/>
  Like all our code, these client libraries are open-source, licensed under the LGPL. For information
  on the API visit our <a href="http://www.intermine.org/wiki/WebService">wiki pages</a>.
</div>
<br>
<div>
  <ul>
    <li>
      <div onclick="javascript:showText('prerequisite')"><h3 style="font-weight: bold;">Prerequisites</h3></div>
      <div id="prerequisite" style="padding: 5px">
        <p>All you need to is java 1.5+ and our package
    (<a href="javascript: jQuery('#apiJavaForm').submit();">download</a>)
        which contains the client library and all dependencies.
      </p>
      </div>
    </li>
    <li>
      <div onclick="javascript:showText('codegen')"><h3 style="font-weight: bold;">Getting Started</h3></div>
      <div id="codegen" style="padding: 5px">
        <span>You can get example Java web service API code either from a Template Query or the QueryBuilder:</span>
          <ul style="padding:0px">
            <li>
              From the QueryBuilder - after creating or editing a query, click the "Java" link in
        the <b>Actions</b> section below the query details.
            </li>
            <li>
              From a Template Query form - click the "Java" link below the template form.
            </li>
          </ul>
      </div>
    </li>
    <li>
      <div onclick="javascript:showText('editsrc')"><h3 style="font-weight: bold;">Run the program</h3></div>
      <div id="editsrc" style="padding: 5px">
        <span>You can run the program either from the command line or within an IDE (e.g. Eclipse).
    To run the program in your favorite IDE, make sure to import all the libs from the distribution package. To run from command line:</span>
           <ol style="padding:0px">
            <li>
              Unzip the distribution package (see above):<br>
                <pre>&gt; unzip ${fileName}</pre>
            </li>
            <li>
              <c:set var="dirName" value="${javasieProjectTitle}"/>

              In the intermine-client-x.x directory that has been created make a new directory called
              <code><c:out value="${dirName}"/></code> (this is the package name in the generated Java).<br>
        <pre>&gt; cd ${fn:substringBefore(fileName, ".zip")}
&gt; mkdir ${dirName}</pre>
            </li>
      <li>
              Save the source code in a Java source file (*.java) in the <c:out value="${dirName}"/>
        directory. The file name and the class name
        have to be the same. The generated class names might be duplicated (if you have generated
        code for a query before) - feel free to change them if necessary. For example, the
    default query class name is <code>QueryClient</code>, so:<br>
    <pre>&gt; cp [downloaded-file] ${dirName}/QueryClient.java</pre>
            </li>
            <li>
              Run the script from the command line under the package directory:<br>
      <pre>&gt; sh compile-run.sh <c:out value="${dirName}"/>.QueryClient</pre>
            </li>
          </ol>
      </div>
    </li>
  </ul>
</div>

</im:boxarea>
<!-- /apiJava.jsp -->
