<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- tagSelect.jsp -->

<%--Tile usage: 
    type parameter - is type of objects for which you want to display tags like 'bag', 'template' 
    onChangeFunction parameter - is name of function that you want to be called when the select is changed, 
        you must define this function with one parameter - value of new select 
    call reloadTagSelect function if you want select to be reloaded --%>

<tiles:importAttribute name="type" ignore="false" />
<tiles:importAttribute name="onChangeFunction" ignore="true" />

<script type="text/javascript" src="js/imdwr.js" ></script>

<%-- Select to which options are appended --%>
<select id="tagSelect" onchange="javacript:callOnChangeFunction()"></select>

<script type="text/javascript">

function reloadTagSelect() {
    displayTagSelect();
}

function callOnChangeFunction() {
    var select = document.getElementById('tagSelect');
    var value = select[select.selectedIndex].value;
    if ("${onChangeFunction}".length > 0 && 
        window.${onChangeFunction} && 
        (typeof window.${onChangeFunction} == "function")) {
            ${onChangeFunction}(value);
    }
}

function addOption(value, name, select) {
    var option = document.createElement('option');
    option.setAttribute('value', value);
    option.innerHTML = name;
    select.appendChild(option);
}

function displayTagSelect() {
    var callBack = function(tags) {
		setSelectElement('tagSelect', '-- select tag --', tags);        
    }
    AjaxServices.getTags("${type}", callBack);
}

displayTagSelect();
</script>

<!-- /tagSelect.jsp -->