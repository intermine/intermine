function setFavouriteTemplate(templateName, image){
	AjaxServices.setFavouriteTemplate(templateName);
	image.src='images/star_active.gif';
	image.onclick='';
	image.style.cursor='';
	image.title='This template is a favourite';
}

function precomputeTemplate(templateName){
	document.getElementById('precompute_'+templateName).innerHTML="Precomputing..";
	AjaxServices.preCompute(templateName,function(str) {
		document.getElementById('precompute_'+templateName).style.color="#777";
		document.getElementById('precompute_'+templateName).innerHTML="Precomputed";
	 });
}

function summariseTemplate(templateName){
    document.getElementById('summarise_'+templateName).innerHTML="Summarising..";
    AjaxServices.summarise(templateName,function(str) {
        document.getElementById('summarise_'+templateName).style.color="#777";
        document.getElementById('summarise_'+templateName).innerHTML="Summarised";
    });
}

function editName(name){
  document.getElementById('form_'+name).style.display="block";
  document.getElementById('name_'+name).style.display="none";
}

function renameElement(name, type, index){
	document.getElementById('form_'+name).style.display="none";
	document.getElementById('name_'+name).innerHTML="<i>saving...</i>";
	document.getElementById('name_'+name).style.display="block";
	AjaxServices.rename(name,type, (document.getElementById('newName_'+name).value).replace(/^\s*|\s*$/g,""), function(str){
		document.getElementById('name_'+name).innerHTML=str;
		document.getElementById('selected_' + type + '_' + index).value=str;
	});
}

function changeViewPathDescription(pathName){
    var pathString = pathName.replace(/\_/g,".");
    var pathEnd = pathString.substring(pathString.lastIndexOf('.') + 1);
    document.getElementById('form_'+pathName).style.display = "none";
    document.getElementById('name_'+pathName+'_inner').innerHTML = "<i>saving...</i>";
    document.getElementById('name_'+pathName).style.display = "block";
    var newDescription = document.getElementById('newName_' + pathName).value;
    var callBack = function(prefixDescription){
        if (prefixDescription == null) {
            prefixDescription = '(no description)';
        }
        document.getElementById('name_' + pathName + '_inner').innerHTML =
          '<span  class="viewPathDescription">' + prefixDescription +
          '</span> &gt; ' + pathEnd;
    }
    AjaxServices.changeViewPathDescription(pathString, newDescription, callBack);
}

function saveBagDescription(bagName){
    var textarea = document.getElementById('textarea').value;
	textarea = textarea.replace(/[\n\r]+/g, "\n\r<br/>");
	document.getElementById('bagDescriptionDiv').innerHTML = '<i>Saving...</i>';
	AjaxServices.saveBagDescription(bagName,textarea, function(str){
		document.getElementById('bagDescriptionDiv').innerHTML = str;
	});
	swapDivs('bagDescriptionTextarea','bagDescriptionDiv');
}

function swapDivs(div1,div2){
	document.getElementById(div1).style.display = 'none';
	document.getElementById(div2).style.display = 'block';
}


function roundToSignificantDecimal(n) {
	var log10n = Math.log(Math.abs(n)) / Math.log(10);
	if(log10n<1 && n!=0) {
		var rf = 100*Math.pow(10,-Math.round(log10n));
		return rf;
	} else return 100;
}

function getColumnSummary(columnName) {
	DWRUtil.removeAllRows('summary_table');
    document.getElementById('summary_loaded').style.display = "none";
    document.getElementById('summary_loading').style.display = "block";
    Effect.Appear('summary');
	AjaxServices.getColumnSummary(columnName, function(str){
	    var rows = str;
	    var cellFuncs = new Array();
	    for (var i=0;i<rows[0].length;i++){
	      cellFuncs[i] = eval('function(data) {var cell = data['+i+']; if (! isNaN(cell)) {var rf = roundToSignificantDecimal(cell+0);return Math.round(cell*rf)/rf;} else return cell; }');
	    }
	    if(cellFuncs.length == 2){
	      document.getElementById('summary_head').innerHTML = '<tr><th>Value</th><th>Count</th></tr>';
	    } else if (cellFuncs.length == 4){
	      document.getElementById('summary_head').innerHTML = '<tr><th>Min</th><th>Max</th><th>Average</th><th>Deviation</th></tr>';
	    }
		DWRUtil.addRows("summary_table", rows, cellFuncs);
		document.getElementById('summary_loading').style.display = "none";
		document.getElementById('summary_loaded').style.display = "block";
	});
}
