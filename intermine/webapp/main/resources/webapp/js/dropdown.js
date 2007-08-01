/***********************************************
* AnyLink Drop Down Menu- © Dynamic Drive (www.dynamicdrive.com)
* This notice MUST stay intact for legal use
* Visit http://www.dynamicdrive.com/ for full source code
***********************************************/

var menuwidth='100px' //default menu width
var menubgcolor='#F5F0FF'  //menu bgcolor
var disappeardelay=250  //menu disappear speed onMouseout (in miliseconds)
var hidemenu_onclick="yes" //hide menu when user clicks within menu?


//Contents for help menu
var helpMenu=new Array()
helpMenu[0]='<a href="http://www.flymine.org/what.shtml">get started</a>'
helpMenu[1]='<a href="http://www.flymine.org/doc/tutorials/index.shtml">tutorials</a>'
helpMenu[2]='<a href="http://www.flymine.org/doc/manual/index.shtml">user manual</a>'
helpMenu[3]='<a href="http://www.flymine.org/doc/presentations/index.shtml">presentations</a>'


//Contents for about menu
var aboutMenu=new Array()
aboutMenu[0]='<a href="http://www.flymine.org/project/index.shtml">about</a>'
aboutMenu[1]='<a href="http://www.flymine.org/project/team.shtml">team</a>'
aboutMenu[2]='<a href="http://www.flymine.org/project/funding.shtml">funding</a>'
aboutMenu[3]='<a href="http://www.flymine.org/project/jobs.shtml">careers</a>'
aboutMenu[4]='<a href="http://mailman.flymine.org/listinfo/">mailing lists</a>'
aboutMenu[5]='<a href="http://www.flymine.org/doc/links.shtml">resources</a>'
aboutMenu[6]='<a href="http://www.flymine.org/stats.shtml">statistics</a>'
aboutMenu[7]='<a href="http://www.flymine.org/release-notes.shtml">notes</a>'
aboutMenu[8]='<a href="http://www.flymine.org/archive.shtml">archives</a>'
aboutMenu[9]='<a href="http://www.flymine.org/news.shtml">news</a>'
aboutMenu[10]='<a href="http://www.flymine.org/link.shtml">link to us</a>'
aboutMenu[11]='<a href="http://www.flymine.org/cite.shtml">cite us</a>'
aboutMenu[12]='<a href="http://www.intermine.org/">intermine</a>'
aboutMenu[13]='<a href="http://preview.stemcellmine.org/">stemcell</a>'
aboutMenu[14]='<a href="http://www.flymine.org/contact/index.shtml">contact</a>'


//Contents for data menu
var dataMenu=new Array()
dataMenu[0]='<a href="http://www.flymine.org/sources.shtml">datasources</a>'


//Contents for software menu
var softwareMenu=new Array()
softwareMenu[0]='<a href="http://www.flymine.org/download/index.shtml">svn checkout</a>'
softwareMenu[1]='<a href="http://subversion.flymine.org/">browse</a>'

/////No further editing needed

var ie4=document.all
var ns6=document.getElementById&&!document.all

if (ie4||ns6)
document.write('<div id="dropmenudiv" style="visibility:hidden;width:'+menuwidth+';background-color:'+menubgcolor+'" onMouseover="clearhidemenu()" onMouseout="dynamichide(event)"></div>')

function getposOffset(what, offsettype){
var totaloffset=(offsettype=="left")? what.offsetLeft : what.offsetTop;
var parentEl=what.offsetParent;
while (parentEl!=null){
totaloffset=(offsettype=="left")? totaloffset+parentEl.offsetLeft : totaloffset+parentEl.offsetTop;
parentEl=parentEl.offsetParent;
}
return totaloffset;
}


function showhide(obj, e, visible, hidden, menuwidth){
if (ie4||ns6)
dropmenuobj.style.left=dropmenuobj.style.top="-500px"
if (menuwidth!=""){
dropmenuobj.widthobj=dropmenuobj.style
dropmenuobj.widthobj.width=menuwidth
}
if (e.type=="click" && obj.visibility==hidden || e.type=="mouseover")
obj.visibility=visible
else if (e.type=="click")
obj.visibility=hidden
}

function iecompattest(){
return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body
}

function clearbrowseredge(obj, whichedge){
var edgeoffset=0
if (whichedge=="rightedge"){
var windowedge=ie4 && !window.opera? iecompattest().scrollLeft+iecompattest().clientWidth-15 : window.pageXOffset+window.innerWidth-15
dropmenuobj.contentmeasure=dropmenuobj.offsetWidth
if (windowedge-dropmenuobj.x < dropmenuobj.contentmeasure)
edgeoffset=dropmenuobj.contentmeasure-obj.offsetWidth
}
else{
var topedge=ie4 && !window.opera? iecompattest().scrollTop : window.pageYOffset
var windowedge=ie4 && !window.opera? iecompattest().scrollTop+iecompattest().clientHeight-15 : window.pageYOffset+window.innerHeight-18
dropmenuobj.contentmeasure=dropmenuobj.offsetHeight
if (windowedge-dropmenuobj.y < dropmenuobj.contentmeasure){ //move up?
edgeoffset=dropmenuobj.contentmeasure+obj.offsetHeight
if ((dropmenuobj.y-topedge)<dropmenuobj.contentmeasure) //up no good either?
edgeoffset=dropmenuobj.y+obj.offsetHeight-topedge
}
}
return edgeoffset
}

function populatemenu(what){
if (ie4||ns6)
dropmenuobj.innerHTML=what.join("")
}


function dropdownmenu(obj, e, menucontents, menuwidth){
if (window.event) event.cancelBubble=true
else if (e.stopPropagation) e.stopPropagation()
clearhidemenu()
dropmenuobj=document.getElementById? document.getElementById("dropmenudiv") : dropmenudiv
populatemenu(menucontents)

if (ie4||ns6){
showhide(dropmenuobj.style, e, "visible", "hidden", menuwidth)
dropmenuobj.x=getposOffset(obj, "left")
dropmenuobj.y=getposOffset(obj, "top")
dropmenuobj.style.left=dropmenuobj.x-clearbrowseredge(obj, "rightedge")+"px"
dropmenuobj.style.top=dropmenuobj.y-clearbrowseredge(obj, "bottomedge")+obj.offsetHeight+"px"
}

return clickreturnvalue()
}

function clickreturnvalue(){
if (ie4||ns6) return false
else return true
}

function contains_ns6(a, b) {
while (b.parentNode)
if ((b = b.parentNode) == a)
return true;
return false;
}

function dynamichide(e){
if (ie4&&!dropmenuobj.contains(e.toElement))
delayhidemenu()
else if (ns6&&e.currentTarget!= e.relatedTarget&& !contains_ns6(e.currentTarget, e.relatedTarget))
delayhidemenu()
}

function hidemenu(e){
if (typeof dropmenuobj!="undefined"){
if (ie4||ns6)
dropmenuobj.style.visibility="hidden"
}
}

function delayhidemenu(){
if (ie4||ns6)
delayhide=setTimeout("hidemenu()",disappeardelay)
}

function clearhidemenu(){
if (typeof delayhide!="undefined")
clearTimeout(delayhide)
}

if (hidemenu_onclick=="yes")
document.onclick=hidemenu
