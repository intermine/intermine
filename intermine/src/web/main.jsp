<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- main.jsp -->
<table class="query" width="100%" cellspacing="0">
  <tr>
    <td rowspan="2" valign="top" width="50%" class="modelbrowse">
      <div class="heading">
        <fmt:message key="query.currentclass"/>
      </div>
      <div class="body">
      <div> 
        <fmt:message key="query.currentclass.detail"/>
      </div>
      <br/>

      <c:if test="${!empty navigation}">
        <c:forEach items="${navigation}" var="entry" varStatus="status">
          <fmt:message key="query.changePath" var="changePathTitle">
            <fmt:param value="${entry.key}"/>
          </fmt:message>
          <im:viewable path="${entry.value}" viewPaths="${viewPaths}" idPrefix="browser">
            <html:link action="/mainChange?method=changePath&prefix=${entry.value}&path=${QUERY.nodes[entry.value].type}"
                       title="${changePathTitle}">
              <c:out value="${entry.key}"/>
            </html:link>
          </im:viewable>
          <c:if test="${!status.last}">&gt;</c:if>
        </c:forEach>
        <br/><br/>
      </c:if>
      <c:forEach var="node" items="${nodes}">
        <div id="browser">
          <nobr>
            <c:if test="${node.indentation > 0}">
              <c:forEach begin="1" end="${node.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
            </c:if>
            <c:choose>
              <c:when test="${node.button == '+'}">
                <html:link action="/mainChange?method=changePath&path=${node.path}">
                  <img border="0" src="images/plus.gif" alt="+"/>
                </html:link>
              </c:when>
              <c:when test="${node.button == '-'}">
                <html:link action="/mainChange?method=changePath&path=${node.prefix}">
                  <img border="0" src="images/minus.gif" alt="-"/>
                </html:link>
              </c:when>
              <c:otherwise>
                <img src="images/blank.gif" alt=" "/>
              </c:otherwise>
            </c:choose>
            <%-- construct the real path for this node --%>
            <c:choose>
              <c:when test="${prefix == null}">
                <c:set var="fullpath" value="${node.path}"/>
              </c:when>
              <c:when test="${prefix != null && node.indentation == 0}">
                <c:set var="fullpath" value="${prefix}"/>
              </c:when>
              <c:otherwise>
                <c:set var="fullpath" value="${prefix}.${fn:substringAfter(node.path,'.')}"/>
              </c:otherwise>
            </c:choose>
            <im:viewable path="${fullpath}" viewPaths="${viewPaths}" idPrefix="browser">
              <c:if test="${node.indentation > 0}">
                <c:choose>
                  <c:when test="${node.collection}">
                    <span class="collectionField">
                      <c:out value="${node.fieldName}"/>
                    </span>
                  </c:when>
                  <c:when test="${node.reference}">
                    <span class="referenceField">
                      <c:out value="${node.fieldName}"/>
                    </span>
                  </c:when>
                  <c:otherwise>
                    <span class="attributeField">
                      <c:out value="${node.fieldName}"/>
                    </span>
                  </c:otherwise>
                </c:choose>                  
              </c:if>            
              <span class="collectionDescription">
              <c:if test="${node.type != 'String' && node.type != 'Integer'}">
                <span class="type"><c:out value="${node.type}"/></span><c:if test="${!empty classDescriptions[node.type]}"><sup><im:help text="${node.type}: ${classDescriptions[node.type]}">?</im:help></sup>
                </c:if>
              </c:if>
              <c:if test="${node.collection}">
                <fmt:message key="query.collection"/>
              </c:if>
              </span>
              <c:choose>
                <c:when test="${node.indentation > 0}">
                  <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
                    <fmt:param value="${node.fieldName}"/>
                  </fmt:message>
                </c:when>
                <c:otherwise>
                  <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
                    <fmt:param value="${node.type}"/>
                  </fmt:message>
                </c:otherwise>
              </c:choose>
            </im:viewable>
            <c:if test="${viewPaths[fullpath] == null}">
              <html:link action="/mainChange?method=addToView&path=${node.path}"
                         title="${selectNodeTitle}">
                <fmt:message key="query.showNode"/>
              </html:link>
            </c:if>
            <c:if test="${QUERY.nodes[fullpath] == null}">
              <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                <fmt:param value="${node.fieldName}"/>
              </fmt:message>
              <html:link action="/mainChange?method=addPath&path=${node.path}"
                         title="${addConstraintToTitle}">
                <img class="arrow" src="images/right-arrow.gif" alt="->"/>
              </html:link>
            </c:if>
          </nobr>
        </div>
      </c:forEach>
      </div>
    </td>
    
    <%-- Query paths --%>
    
    <td valign="top">
      <div class="heading">
        <fmt:message key="query.currentquery"/>
      </div>
      <div class="body">
      <div> 
        <fmt:message key="query.currentquery.detail"/>
      </div>
      <br/>
      <c:choose>
        <c:when test="${empty QUERY.nodes}">
          <fmt:message key="query.empty"/>
        </c:when>
        <c:otherwise>
          <c:forEach var="entry" items="${QUERY.nodes}" varStatus="status">
            <div>
              <nobr>
                <div>
                  <c:set var="node" value="${entry.value}"/>
                  <c:if test="${node.indentation > 0}">
                    <c:forEach begin="1" end="${node.indentation}">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </c:forEach>
                  </c:if>
                  <im:viewable path="${node.path}" viewPaths="${viewPaths}" test="${!empty node.fieldName}" idPrefix="query">
                    <span class="attributeField"><c:out value="${node.fieldName}"/></span>
                  </im:viewable>
                  <span class="type">
                    <c:choose>
                      <c:when test="${node.attribute}">
                        <%--<c:out value="${node.type}"/>--%>
                      </c:when>
                      <c:otherwise>
                        <fmt:message key="query.changePath" var="changePathTitle">
                          <fmt:param value="${node.type}"/>
                        </fmt:message>
                        <html:link action="/mainChange?method=changePath&prefix=${node.path}&path=${node.type}"
                                   title="${changePathTitle}">
                          <im:viewable path="${node.path}" viewPaths="${viewPaths}" test="${empty node.fieldName}" idPrefix="query">
                            <span class="type"><c:out value="${node.type}"/></span>
                          </im:viewable>
                        </html:link>
                        <c:if test="${node.collection}">
                          <fmt:message key="query.collection"/>
                        </c:if>
                      </c:otherwise>
                    </c:choose>
                  </span>
                  <c:choose>
                    <c:when test="${node.indentation > 0}"> 
                      <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                        <fmt:param value="${node.fieldName}"/>
                      </fmt:message>
                    </c:when>
                    <c:otherwise>
                      <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                        <fmt:param value="${node.type}"/>
                      </fmt:message>
                    </c:otherwise>
                  </c:choose>
                  <html:link action="/mainChange?method=addConstraint&path=${node.path}"
                             title="${addConstraintToTitle}">
                    <fmt:message key="query.addConstraint"/>
                  </html:link>
                  <c:if test="${!lockedPaths[node.path]}">
                    <fmt:message key="query.removeNodeTitle" var="removeNodeTitle">
                      <fmt:param value="${node.fieldName}"/>
                    </fmt:message>
                    <html:link action="/mainChange?method=removeNode&path=${node.path}"
                               title="${removeNodeTitle}">
                      <img border="0" src="images/cross.gif" alt="x"/>
                    </html:link>
                  </c:if>
                  <c:if test="${lockedPaths[node.path]}">
                    <img border="0" src="images/discross.gif" alt="x" title="<fmt:message key="query.disabledRemoveNodeTitle"/>"/>
                  </c:if>
                </div>
                <c:forEach var="constraint" items="${node.constraints}" varStatus="status">
                  <div>
                    <c:forEach begin="0" end="${node.indentation}">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </c:forEach>
                    <span class="constraint">
                      <c:out value="${constraint.op}"/>
                      <c:choose>
                        <c:when test="${constraint.value.class.name == 'java.util.Date'}">
                          <fmt:formatDate dateStyle="SHORT" value="${constraint.value}"/>
                        </c:when>
                        <c:otherwise>
                          <c:out value=" ${constraintDisplayValues[constraint]}"/>
                        </c:otherwise>
                      </c:choose>
                    </span>
                    <fmt:message key="query.removeConstraintTitle" var="removeConstraintTitle"/>
                    <html:link action="/mainChange?method=removeConstraint&path=${node.path}&index=${status.index}"
                               title="${removeConstraintTitle}">
                      <img border="0" src="images/cross.gif" alt="x"/>
                    </html:link>
                  </div>
                </c:forEach>
              </nobr>
            </div>
          </c:forEach>
        </c:otherwise>
      </c:choose>
      </div>
    </td>
  </tr>

  
  <%-- Constraint editor --%>
  

  <c:if test="${editingNode != null}">
    <tr>
      <td valign="top">
        <div class="heading"><fmt:message key="query.constrain"/></div>
        <div class="body">
          <c:choose>
            <c:when test="${empty editingNode.fieldName}">
        <span class="type">
              <c:out value="${editingNode.path}"/>
        </span>
            </c:when>
            <c:otherwise>
        <span class="attributeField">
              <c:out value="${editingNode.fieldName}"/>
        </span>
            </c:otherwise>
          </c:choose>
          <c:choose>
            <c:when test="${editingNode.collection}">
              <fmt:message key="query.collection">
                <fmt:param value="${editingNode.type}"/>
              </fmt:message>
            </c:when>
            <c:otherwise>
            <%-- <c:out value="${editingNode.type}"/> --%>
            </c:otherwise>
          </c:choose>
        
        
        <script type="text/javascript">
        <!--
        
        var fixedOps = new Array();
        
        <c:forEach items="${fixedOptionsOps}" var="op" varStatus="status">
          fixedOps[${status.count}] = "<c:out value="${op}"/>";
        </c:forEach>
        
       
        /***********************************************************
         * Called when user chooses a constraint operator. If the
         * user picks an operator contained in fixedOptionsOps then
         * the input box is hidden and the user can only choose
         **********************************************************/
        function updateConstraintForm(form)
        {
          if (form.attributeOptions == null)
            return;
        
          for (var i=0 ; i<fixedOps.length ; i++)
          {
            if (form.attributeOp.value == fixedOps[i])
            {
              document.getElementById("operandEditSpan").style.display="none";
              //form.attributeValue.style.visibility='hidden';
              form.attributeValue.value = form.attributeOptions.value; // constrain value
              return;
            }
          }
          
          document.getElementById("operandEditSpan").style.display="";
          //form.attributeValue.style.visibility='visible';
        }
        
        /***********************************************************
         * Init attribute value with selected item and hide input box if
         * required
         **********************************************************/
        function initConstraintForm(form)
        {
          if (form.attributeOptions == null)
            return;
          
          form.attributeValue.value = form.attributeOptions.value;
          updateConstraintForm(form);
        }
        
        //-->
        </script>
        
        <html:form action="/mainAction">
          <html:hidden property="path" value="${editingNode.path}"/>
          <c:choose>
            <c:when test="${editingNode.attribute}">
              <table border="0" cellspacing="0" cellpadding="1" border="0" class="noborder" height="65">
                <tr>
                  <c:choose>
                    <c:when test="${editingNode.type == 'boolean'}">
                      <td valign="top">
                        <input type="hidden" name="attributeOp" value="0"/>
                        <input type="radio" name="attributeValue" value="true" checked /><fmt:message key="query.constraint.true"/>
                        <input type="radio" name="attributeValue" value="false"/><fmt:message key="query.constraint.false"/>
                      </td>
                    </c:when>
                    <c:otherwise>
                      <td valign="top">
                        <html:select property="attributeOp" onchange="updateConstraintForm(this.form)">
                          <c:forEach items="${attributeOps}" var="attributeOp">
                            <html:option value="${attributeOp.key}">
                              <c:out value="${attributeOp.value}"/>
                            </html:option>
                          </c:forEach>
                        </html:select>
                      </td>
                      <td valign="top" align="center">
                        <span id="operandEditSpan">
                          <html:text property="attributeValue"/><br/>
                          <%-- might want to show up arrow --%>
                          <c:if test="${!empty attributeOptions}">
                            <im:vspacer height="5"/><br/>
                            <img src="images/up-arrow.gif" alt="^^^" border="0"/><br/>
                            <im:vspacer height="5"/><br/>
                          </c:if>
                        </span>
                        <c:if test="${!empty attributeOptions}">
                          <select name="attributeOptions" onchange="this.form.attributeValue.value=this.value;">
                          <c:forEach items="${attributeOptions}" var="option">
                            <option value="${option}">
                              <c:out value="${option}"/>
                            </option>
                          </c:forEach>
                          </select>
                        </c:if>
                      </td>
                    </c:otherwise>
                  </c:choose>
                  <td valign="top">&nbsp;
                    <html:submit property="attribute">
                       <fmt:message key="query.submitConstraint"/>
                    </html:submit>
                  </td>
                </tr>
              </table>
            </c:when>
            <c:otherwise>
              <c:if test="${editingNode.indentation != 0 && !empty subclasses}">
                <p style="text-align: left;">
                  <fmt:message key="query.subclassConstraint"/>
                  <html:select property="subclassValue">
                    <c:forEach items="${subclasses}" var="subclass">
                      <html:option value="${subclass}">
                        <c:out value="${subclass}"/>
                      </html:option>
                    </c:forEach>
                  </html:select>
                  <html:submit property="subclass">
                    <fmt:message key="query.submitConstraint"/>
                  </html:submit>
                </p>
              </c:if>
              <c:if test="${!empty loopQueryPaths && !empty loopQueryOps}">
                <p style="text-align: left;">
                  <fmt:message key="query.loopQueryConstraint"/>
                  <html:select property="loopQueryOp">
                    <c:forEach items="${loopQueryOps}" var="loopOp">
                      <html:option value="${loopOp.key}">
                        <c:out value="${loopOp.value}"/>
                      </html:option>
                    </c:forEach>
                  </html:select>
                  <html:select property="loopQueryValue">
                    <c:forEach items="${loopQueryPaths}" var="loopPath">
                      <html:option value="${loopPath}">
                        <c:out value="${loopQueryPathsDisplay[loopPath]}"/>
                      </html:option>
                    </c:forEach>
                  </html:select>
                  <html:submit property="loop">
                    <fmt:message key="query.submitConstraint"/>
                  </html:submit>
                </p>
              </c:if>
            </c:otherwise>
          </c:choose>
          <c:if test="${!empty PROFILE.savedBags}">
            <p style="text-align: left;">
              <fmt:message key="query.bagConstraint"/>
              <html:select property="bagOp">
                <c:forEach items="${bagOps}" var="bagOp">
                  <html:option value="${bagOp.key}">
                    <c:out value="${bagOp.value}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:select property="bagValue">
                <c:forEach items="${PROFILE.savedBags}" var="bag">
                  <html:option value="${bag.key}">
                    <c:out value="${bag.key}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:submit property="bag">
                <fmt:message key="query.submitConstraint"/>
              </html:submit>
            </p>
          </c:if>
        </html:form>
        
        
        <script type="text/javascript">
        <!--
        initConstraintForm(document.mainForm);
        //-->
        </script>
        </div>
      </td>
    </tr>
    
  </c:if>


</table>
<!-- /main.jsp -->
