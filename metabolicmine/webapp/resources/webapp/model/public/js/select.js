function selectReplacement(obj) {
    obj.className += ' replaced';
    var ul = document.createElement('ul');
    ul.className = 'selectReplacement';
    var opts = obj.options;
    var selectedOpt = (!obj.selectedIndex) ? 0 : obj.selectedIndex;
    for (var i=0; i<opts.length; i++) {
        var li = document.createElement('li');
        var txt = document.createTextNode(opts[i].text);
        li.appendChild(txt);
        li.selIndex = i;
        li.selectID = obj.id;
        li.onclick = function() {
            selectMe(this);
        };
        if (i == selectedOpt) {
            li.className = 'selected';
            li.onclick = function() {
                this.parentNode.className += ' selectOpen';
                this.onclick = function() {
                    selectMe(this);
                };
            };
        }
        if (window.attachEvent) {
            li.onmouseover = function() {
                this.className += ' hover';
            };
            li.onmouseout = function() {
                this.className =
                this.className.replace(new RegExp(" hover\\b"), '');
            };
        }
        ul.appendChild(li);
    }
    obj.onfocus = function() {
        ul.className += ' selectFocused';
    };
    obj.onblur = function() {
        ul.className = 'selectReplacement';
    };
    obj.onchange = function() {
        var idx = this.selectedIndex;
        selectMe(ul.childNodes[idx]);
    };
    obj.onkeypress = obj.onchange;
    obj.parentNode.insertBefore(ul,obj);
}

function selectMe(obj) {
    var lis = obj.parentNode.getElementsByTagName('li');
    for (var i=0; i<lis.length; i++) {
        if (lis[i] != obj) {
            lis[i].className='';
            lis[i].onclick = function() {
                selectMe(this);
            };
        } else {
            if (obj.selectID == "") alert("You need to provide select field ID!");
            setVal(obj.selectID, obj.selIndex);
            obj.className='selected';
            obj.parentNode.className =
            obj.parentNode.className.replace(new RegExp(" selectOpen\\b"), '');
            obj.onclick = function() {
                obj.parentNode.className += ' selectOpen';
                this.onclick = function() {
                    selectMe(this);
                };
            };
        }
    }
}

function setVal(objID,val) {
    var obj = document.getElementById(objID);
    obj.selectedIndex = val;
}

function setForm() {
    var s = document.getElementsByTagName('select');
    for (var i=0; i<s.length; i++) {
        selectReplacement(s[i]);
    }
}

window.onload = function() {
    (document.all && !window.print) ? null : setForm();
};