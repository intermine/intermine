<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<table class="footer">
  <tr>
    <td align="left" width="10%">
      <table class="footer">
        <tr>
          <td align="center">
            <a href="http://www.wellcome.ac.uk">
              <img src="<html:rewrite page="/images/wellcome.gif"/>" border="0" hspace="10" alt="Wellcome Trust Logo"/>
            </a>
          </td>
          <td>
            <bean:message key="flymine.funding"/>
          </td>
        </tr>
      </table>
    </td>
    <td align="right" width="10%">
      <table class="footer" cellpadding="10">
        <tr>
          <td>
            FlyMine<br/>Department of Genetics<br/>Downing Street<br/>Cambridge, CB2 3EH, UK
          </td>
          <td>
            Tel: +44 (0)1223 333377<br/>Fax: +44 (0)1223 333992<br/><a href="mailto:info[at]flymine.org">info[at]flymine.org</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

