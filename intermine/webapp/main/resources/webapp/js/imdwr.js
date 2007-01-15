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
	AjaxServices.rename(name,type, document.getElementById('newName_'+name).value, function(str){
		document.getElementById('name_'+name).innerHTML=str;
		document.getElementById('selected_' + type + '_' + index).value=str;
	});
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