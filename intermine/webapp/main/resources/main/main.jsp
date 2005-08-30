<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute/>

<!-- main.jsp -->
<html:xhtml/>
<table class="query" width="100%" cellspacing="0">
  <tr>
    <td rowspan="2" valign="top" width="50%" class="modelbrowse">
      <div class="heading">
        <fmt:message key="query.currentclass"/><im:helplink key="query.help.browser"/>
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
          <im:viewableSpan path="${entry.value}" viewPaths="${viewPaths}" idPrefix="nav">
            <html:link action="/mainChange?method=changePath&amp;prefix=${entry.value}&amp;path=${navigationPaths[entry.key]}"
                       title="${changePathTitle}">
              <c:out value="${entry.key}"/>
            </html:link>
          </im:viewableSpan>
          <c:if test="${!status.last}">&gt;</c:if>
        </c:forEach>
        <br/><br/>
      </c:if>
      <c:forEach var="node" items="${nodes}">
        
          <div class="browserline">
            <c:if test="${node.indentation > 0}">
              <c:forEach begin="1" end="${node.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
            </c:if>
            <a name="${node.path}"></a>
            <c:set var="isNull" value="${EMPTY_FIELD_MAP[node.parentType][node.fieldName]}"/>
            <c:if test="${isNull}">
              <span class="nullStrike">
            </c:if>
            <c:choose>
              <c:when test="${isNull}">
                <img border="0" src="images/plus-disabled.gif" width="11" height="11" alt="+"/>
              </c:when>
              <c:when test="${node.button == '+'}">
                <html:link action="/mainChange?method=changePath&amp;path=${node.path}">
                  <img border="0" src="images/plus.gif" width="11" height="11" alt="+"/>
                </html:link>
              </c:when>
              <c:when test="${node.button == '-'}">
                <html:link action="/mainChange?method=changePath&amp;path=${node.prefix}">
                  <img border="0" src="images/minus.gif" width="11" height="11" alt="-"/>
                </html:link>
              </c:when>
              <c:otherwise>
                <img src="images/blank.gif" width="11" height="11" alt=" "/>
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
            <im:viewableSpan path="${fullpath}" viewPaths="${viewPaths}" idPrefix="browser">
              <c:if test="${node.indentation > 0}">
                <c:choose>
                  <c:when test="${node.collection}">
                    <c:set var="fieldNameClass" value="collectionField"/>
                  </c:when>
                  <c:when test="${node.reference}">
                    <c:set var="fieldNameClass" value="referenceField"/>
                  </c:when>
                  <c:otherwise>
                    <c:set var="fieldNameClass" value="attributeField"/>
                  </c:otherwise>
                </c:choose>
                <c:if test="${isNull}">
                  <c:set var="fieldNameClass" value="${fieldNameClass} nullReferenceField"/>
                </c:if>
                <span class="${fieldNameClass}">
                  <c:out value="${node.fieldName}"/>
                </span>
                <im:typehelp type="${node.parentType}.${node.fieldName}"/>
              </c:if>
              <span class="collectionDescription ${isNull ? 'nullReferenceField' : ''}">
              <c:if test="${node.type != 'String' && node.type != 'Integer'}">
                <span class="type">${node.type}</span><c:if test="${!isNull}"><im:typehelp type="${node.type}"/></c:if>
              </c:if>
              <c:if test="${node.collection}">
                <fmt:message key="query.collection"/>
              </c:if>
              </span>
            </im:viewableSpan>
            <c:choose>
              <c:when test="${node.indentation > 0}">
                <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
                  <fmt:param value="${node.fieldName}"/>
                </fmt:message>
                <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                  <fmt:param value="${node.fieldName}"/>
                </fmt:message>
              </c:when>
              <c:otherwise>
                <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
                  <fmt:param value="${node.type}"/>
                </fmt:message>
                <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                  <fmt:param value="${node.type}"/>
                </fmt:message>
              </c:otherwise>
            </c:choose>
            <c:choose>
              <c:when test="${viewPaths[fullpath] == null && !isNull}">
                <html:link action="/mainChange?method=addToView&amp;path=${node.path}"
                           title="${selectNodeTitle}">
                  <img class="arrow" src="images/show.gif" width="43" height="13" alt="show" style="margin-right:-0.5ex"/>
                </html:link>
              </c:when>
              <c:otherwise>
                <img class="arrow" src="images/show-disabled.gif" width="43" height="13" alt="show" style="margin-right:-0.5ex"/>
              </c:otherwise>
            </c:choose>
            <c:choose>
              <c:when test="${isNull}">
                <img class="arrow" src="images/constrain-disabled.gif" width="70" height="13" alt="constrain"/>
              </c:when>
              <c:otherwise>
                <html:link action="/mainChange?method=addPath&amp;path=${node.path}"
                           title="${addConstraintToTitle}">
                  <img class="arrow" src="images/constrain.gif" width="70" height="13" alt="constrain"/>
                </html:link>
              </c:otherwise>
            </c:choose>
            <c:if test="${isNull}">
              </span>
              <c:choose>
                <c:when test="${node.reference}">
                  <fmt:message key="query.nullRefHelp" var="strikeThruHelp">
                    <fmt:param value="${node.parentType}"/>
                    <fmt:param value="${node.fieldName}"/>
                  </fmt:message>
                </c:when>
                <c:when test="${node.collection}">
                  <fmt:message key="query.emptyCollHelp" var="strikeThruHelp">
                    <fmt:param value="${node.parentType}"/>
                    <fmt:param value="${node.fieldName}"/>
                  </fmt:message>
                </c:when>
                <c:otherwise>
                  <%-- null attribute help? --%>
                </c:otherwise>
              </c:choose>
              <im:helplink text="${strikeThruHelp}"/>
            </c:if>
          </div>
      </c:forEach>
      </div>
    </td>
    
    <%-- Query paths --%>
    
    <td valign="top">
      <div class="heading">
        <fmt:message key="query.currentquery"/><im:helplink key="query.help.constraints"/>
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
              <div style="white-space: nowrap">
                <div>
                  <c:set var="node" value="${entry.value}"/>
                  <c:if test="${node.indentation > 0}">
                    <c:forEach begin="1" end="${node.indentation}">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </c:forEach>
                  </c:if>
                  <im:viewableSpan path="${node.path}" viewPaths="${viewPaths}" test="${!empty node.fieldName}" idPrefix="query">
                    <span class="attributeField"><c:out value="${node.fieldName}"/></span>
                  </im:viewableSpan>
                  <span class="type">
                    <c:choose>
                      <c:when test="${node.attribute}">
                        <%--<c:out value="${node.type}"/>--%>
                      </c:when>
                      <c:otherwise>
                        <fmt:message key="query.changePath" var="changePathTitle">
                          <fmt:param value="${node.type}"/>
                        </fmt:message>
                        <html:link action="/mainChange?method=changePath&amp;prefix=${node.path}&amp;path=${node.type}"
                                   title="${changePathTitle}">
                          <im:viewableSpan path="${node.path}" viewPaths="${viewPaths}" test="${empty node.fieldName}" idPrefix="query">
                            <span class="type"><c:out value="${node.type}"/></span>
                          </im:viewableSpan>
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
                  <%--
                  <html:link action="/mainChange?method=addConstraint&amp;path=${node.path}"
                             title="${addConstraintToTitle}">
                    <img class="arrow" src="images/constrain.gif" width="70" height="13" alt="constrain" style="vertical-align:middle"/>
                  </html:link>
                  --%>
                  <c:if test="${!lockedPaths[node.path]}">
                    <fmt:message key="query.removeNodeTitle" var="removeNodeTitle">
                      <fmt:param value="${node.fieldName}"/>
                    </fmt:message>
                    <html:link action="/mainChange?method=removeNode&amp;path=${node.path}"
                               title="${removeNodeTitle}">
                      <img border="0" src="images/cross.gif" width="13" height="13" alt="x"/>
                    </html:link>
                  </c:if>
                  <c:if test="${lockedPaths[node.path]}">
                    <img border="0" src="images/discross.gif" width="13" height="13" 
                         alt="x" title="<fmt:message key="query.disabledRemoveNodeTitle"/>"/>
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
                        <c:when test="${node.reference}">
                          <c:out value=" ${fn:replace(constraintDisplayValues[constraint], '.', ' > ')}"/>
                        </c:when>
                        <c:when test="${constraint.value.class.name == 'java.util.Date'}">
                          <fmt:formatDate dateStyle="SHORT" value="${constraint.value}"/>
                        </c:when>
                        <c:otherwise>
                          <c:out value=" ${constraintDisplayValues[constraint]}"/>
                        </c:otherwise>
                      </c:choose>
                    </span>
                    <fmt:message key="query.removeConstraintTitle" var="removeConstraintTitle"/>
                    <html:link action="/mainChange?method=removeConstraint&amp;path=${node.path}&amp;index=${status.index}"
                               title="${removeConstraintTitle}">
                      <img border="0" src="images/cross.gif" width="13" height="13" alt="x"/>
                    </html:link>
                    <fmt:message key="query.editConstraintTitle" var="editConstraintTitle"/>
                    <html:link action="/mainChange?method=editConstraint&amp;path=${node.path}&amp;index=${status.index}"
                               title="${editConstraintTitle}">
                      <img border="0" src="images/edit.gif" width="13" height="13" alt="x"/>
                    </html:link>
                  </div>
                </c:forEach>
              </div>
            </div>
          </c:forEach>
        </c:otherwise>
      </c:choose>
      </div>
    </td>
  </tr>

  
  <%-- Constraint editor --%>
  
  <a name="constraint-editor"></a>
  <c:if test="${editingNode != null}">
    <tr>
      <td valign="top">
        <div class="heading"><fmt:message key="query.constrain"/><im:helplink key="query.help.constrain"/></div>
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
       
        /***********************************************************
         * Called when user chooses a constraint operator. If the
         * user picks an operator contained in fixedOptionsOps then
         * the input box is hidden and the user can only choose
         **********************************************************/
        function updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement)
        {
          if (attrOptsElement == null)
            return;
        
          for (var i=0 ; i<fixedOps[index].length ; i++)
          {
            if (attrOpElement.value == fixedOps[index][i])
            {
              document.getElementById("operandEditSpan" + index).style.display = "none";
              attrValElement.value = attrOptsElement.value; // constrain value
              return;
            }
          }
          
          document.getElementById("operandEditSpan" + index).style.display = "";
        }
        
        /***********************************************************
         * Init attribute value with selected item and hide input box if
         * required
         **********************************************************/
        function initConstraintForm(index, attrOpElement, attrOptsElement, attrValElement)
        {
          if (attrOptsElement == null)
            return;
          
          var init = '${editingConstraintValue}';
          attrValElement.value = (init != '') ? init : attrOptsElement.value;
            
          updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement);
        }
        
        //-->
        </script>
        
        <c:set var="validOps" value="${displayConstraint.validOps}"/>
        <c:set var="fixedOps" value="${displayConstraint.fixedOpIndices}"/>
        <c:set var="options" value="${displayConstraint.optionsList}"/>
        
        <script type="text/javascript">
        <!--

          fixedOps[0] = new Array();
          <c:forEach items="${fixedOps}" var="op" varStatus="status">
            fixedOps[0][${status.count}] = "<c:out value="${op}"/>";
          </c:forEach>
        
        //-->
        </script>
        
        <html:form action="/mainAction">
          <html:hidden property="path" value="${editingNode.path}"/>
          <c:if test="${editingConstraintIndex != null}">
            <html:hidden property="cindex" value="${editingConstraintIndex}"/>
          </c:if>
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
                        <html:select property="attributeOp" onchange="updateConstraintForm(0, this.form.attributeOp, this.form.attributeOptions, this.form.attributeValue)">
                          <c:forEach items="${validOps}" var="op">
                            <option value="${op.key}"
                              <c:if test="${editingConstraintOperand == op.key}">
                                selected
                              </c:if>
                            >
                              <c:out value="${op.value}"/>
                            </option>
                          </c:forEach>
                        </html:select>
                      </td>
                      <td valign="top" align="center">
                        <span id="operandEditSpan0">
                          <html:text property="attributeValue" value="${editingConstraintValue}"/>
                          <%-- might want to show up arrow --%>
                          <c:if test="${!empty options}">
                            <br/><im:vspacer height="2"/>
                            <img src="images/up-arrow.gif" alt="^^^" border="0" height="13" width="13"/>
                            <im:vspacer height="2"/>
                          </c:if>
                        </span>
                        <c:if test="${!empty options}">
                          <select name="attributeOptions" onchange="this.form.attributeValue.value=this.value;">
                          <c:forEach items="${options}" var="option">
                            <option value="${option}"
                              <c:if test="${editingConstraintValue == option}">
                                selected
                              </c:if>
                            >
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
              <c:if test="${editingNode.indentation != 0 && !empty SUBCLASSES[editingNode.type]}">
                <p style="text-align: left;">
                  <fmt:message key="query.subclassConstraint"/>
                  <html:select property="subclassValue">
                    <c:forEach items="${SUBCLASSES[editingNode.type]}" var="subclass">
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
                        <c:out value="${fn:replace(loopPath, '.', ' > ')}"/>
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
          <%-- Find the right set of bags --%>
          <c:choose>
            <c:when test="${editingNode.attribute}">
              <c:set var="bags" value="${PROFILE.primitiveBags}"/>
            </c:when>
            <c:otherwise>
              <c:set var="bags" value="${PROFILE.objectBags}"/>
            </c:otherwise>
          </c:choose>
          <%-- Display popup if bags exist --%>
          <c:if test="${!empty bags}">
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
                <c:forEach items="${bags}" var="bag">
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
          <c:if test="${!editingNode.collection}">
            <p style="text-align: left;">
              <html:radio property="nullConstraint" value="NULL"/><fmt:message key="query.constraint.null"/>
              <html:radio property="nullConstraint" value="NotNULL"/><fmt:message key="query.constraint.notnull"/>
              &nbsp;
              <html:submit property="nullnotnull">
                 <fmt:message key="query.submitConstraint"/>
              </html:submit>
            </p>
          </c:if>
        </html:form>
        
        
        <script type="text/javascript">
        <!--
        initConstraintForm(0, document.mainForm.attributeOp, document.mainForm.attributeOptions, document.mainForm.attributeValue);
        //-->
        </script>
        </div>
        <div style="text-align:right">
          <script language="JavaScript">
          <!--
            document.write('<input type="submit" onclick="window.location.href=\'<html:rewrite action="/query"/>?cancel\'" value="<fmt:message key="query.cancelConstraint"/>"/>');
          //-->
          </script>
        </div>
      </td>
    </tr>
    
  </c:if>


</table>
<!-- /main.jsp -->
