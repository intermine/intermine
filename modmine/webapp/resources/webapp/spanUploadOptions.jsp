<%--
  - Author: Fengyuan Hu
  - Date: 11-06-2010
  - Copyright Notice:
  - @(#)  Copyright (C) 2002-2010 FlyMine

          This code may be freely distributed and modified under the
          terms of the GNU Lesser General Public Licence.  This should
          be distributed with the code.  See the LICENSE file for more
          information or http://www.gnu.org/copyleft/lesser.html.

  - Description: In this page, users have different options to constrain
                 their query for overlapping located sequence features with
                 the spans they upload.
  --%>

<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>

<!--  spanUploadOptions.jsp -->

<html:xhtml />

<div>
  <html:form action="/spanUploadAction" focus="orgName">
    <html:text property="orgName"/>
    <html:submit>Search</html:submit>
  </html:form>
</div>

<!--  /spanUploadOptions.jsp -->