    var WHOLE_LIST;   // boolean if long or short list
    var SHORT_LIST;   // array of max 31. results
    var LONG_LIST;    // array of all results if spezific request
    var PREFIX;       // string of input before request for more results
    var CUT_LIST;     // cutted results list of Long List
    var INDEX;        // which topic is marked
    var IE;			  // boolean if Internet Explorer
    var MOUSE_OVER;   // if the mouse is over the input or list area
    var FIELD;		  // field name from the table for the lucene search
    var CLASS_NAME;   // class name (table in the database) for lucene search
    var INPUT;		  // input field id in the jsp file
      var DISPLAY;      // id of the div for the the list
      var ERROR;        // id of the div for the error message
      var FRAME;        // a frame which is displayed under the result list to fix bugs in IE


      setWholeList(false);
    setPrefix("");
    SHORT_LIST = new Array();
    LONG_LIST = new Array();
    CUT_LIST = new Array();
    setIndex(-1);
    IE = false;
    MOUSE_OVER = 0;

      function setMouseOver(mouseOver) {
          MOUSE_OVER = mouseOver;
      }

      function setWholeList(wholeList) {
          WHOLE_LIST = wholeList;
      }

      function getWholeList() {
          return WHOLE_LIST;
      }

      function setCutList( cutList) {
          CUT_LIST = cutList;
      }

      function setShortList( list) {
          SHORT_LIST = list;
      }

      function setLongList( longList) {
          LONG_LIST = longList;
      }

      function setPrefix( prefix) {
          PREFIX = prefix;
      }

      function setIndex( index) {
          INDEX = index;
      }

      function getId(input) {
          INPUT = input;
          DISPLAY = input + "_display";
          ERROR = input + "_error";
          FRAME = input + "_IEbugFixFrame";
      }

      // AJAX request to load a long or short array
      function loadList() {

        (function() {

          var suffix = document.getElementById(INPUT).value;
          var wholeList = getWholeList();
          var field = FIELD;
          var className = CLASS_NAME;

          /**
           * @param suffix string of input before request for more results
           * @param wholeList whether or not to show the entire list or a truncated version
           * @param field field name from the table for the lucene search
           * @param className class name (table in the database) for lucene search
           * @return an array of values for this classname.field
           */
          AjaxServices.getContent(suffix, wholeList, field, className,
            function (array) {
              if (array == null) return;
              
              jQuery('#'+INPUT).focus();
              if (array[0] == "true") {
                if (getWholeList()) {
                  setCutList(getElementList(array));
                  setLongList(CUT_LIST);
                } else {
                  setShortList(getElementList(array));
                }
                setPrefix(document.getElementById(INPUT).value);
                printList(getElementList(array));
              } else if (array[0] != '') {
                printError(array[0]);
              }
            });
        })();
      }

    // output if an error appears
    function printError(error) {
        removeList();
        $(ERROR).style.visibility = "visible";
        getCoordinates($(ERROR), $(INPUT));
        $(ERROR).appendChild(document.createTextNode(error));
    }

    // cut off the first element which could containts error messages
    function getElementList(array) {
        var elemList = new Array(array.length - 1);

        for (var i = 0; i < elemList.length; i++) {
                elemList[i] = array[i+1];
        }
        return elemList;
    }

    // after every Keyboard hit
    function readInput(e, className, field) {
        CLASS_NAME = className;
        FIELD = field;
        var curKey;
        if (e.which) {   			// FF
            curKey = e.which;
            IE = false;
          } else if (e.keyCode) {     // IE
              curKey = e.keyCode;
              IE = true;
          }

        // do not use autocompleter if *
        if($(INPUT).value.match('\\*')) {
            removeList();
            return;
        }

        // ascii code from "a" till  "z" or
        // ascii code from "0" till "9"
        // backspace
        // delete
        if ((curKey >= 65 && curKey <= 90) ||        // load list
            (curKey >= 48 && curKey <= 57) ||
            (curKey >= 96 && curKey <= 105) ||
            curKey == 8 || curKey == 46) {
            setIndex(-1);
            if (WHOLE_LIST && $(INPUT).value.indexOf(PREFIX) != 0) {
                setWholeList(false);
                   loadList();
            } else if (WHOLE_LIST) {
                cutLongList();
                printList(CUT_LIST);
            } else if (!WHOLE_LIST) {
                  loadList();
            }
        } else if (curKey == 38) {                    // up arrow

            var len;
            if (WHOLE_LIST) {
                len = CUT_LIST.length -1;
            } else {
                len = SHORT_LIST.length -1;
            }

            remarkIndex();

            if (INDEX > -1) {
                setIndex(INDEX - 1);
              } else {
                  setIndex(len);
              }
              scrollUp(len);
              markIndex();
        } else if (curKey == 40) {                     // down arrow

            var len;
            if (WHOLE_LIST) {
                len = CUT_LIST.length -1;
            } else {
                   len = SHORT_LIST.length -1;
            }

            remarkIndex();

            if (INDEX < len) {
                setIndex(INDEX + 1);
            } else {
                   setIndex(-1);
            }

            scrollDown(len);
            markIndex();
        } else {
            setIndex(-1);
        }
    }

    // spezific function for Querybuilder
    function isSubmit(e) {
        // enter
        if ((e.which == 13 && INDEX > -1) || (e.keyCode == 13 && INDEX > -1)) {

            Event.stop(e);
            if (WHOLE_LIST) {
                  $(INPUT).value = CUT_LIST[INDEX];
            } else {
                  $(INPUT).value = SHORT_LIST[INDEX];
            }
            removeList();
            $('attribute').click(); // submit the constraint
          }
    }

    // specific function for submit in template
    function isEnter(e) {
        var curKey;
        if (e.which) {   			// FF
            curKey = e.which;
          } else if (e.keyCode) {     // IE
              curKey = e.keyCode;
          }
        //enter
        if (curKey == 13) {
            Event.stop(e);
            if (INDEX > -1) {
                if (WHOLE_LIST) {
                      $(INPUT).value = CUT_LIST[INDEX];
                } else {
                      $(INPUT).value = SHORT_LIST[INDEX];
                }
            }
            removeList();
        }
        // tabstop
        if (curKey == 9) {
            removeList();
        }
    }

    // highlight the actual topic
    function markIndex() {
        if (INDEX != -1) {
            jQuery("#li" + INDEX.toString()).css("background","#8AECFF");
          }
    }

    // remove the higlight of the actual topic
    function remarkIndex() {
        if (INDEX != -1) {
            jQuery("#li" + INDEX.toString()).css("background", "none");
        }
    }

    // paint a specific list
    function printList(array) {
        removeList();
        if ($(INPUT).value != "" && array.length > 0) {
            getCoordinates($(DISPLAY), $(INPUT));

            if (IE) {
                getCoordinates($(FRAME), $(INPUT));
                $(FRAME).height = "150";
            }

            var ul = document.createElement('ul');
            ul.setAttribute("id", "ulList");
            var li;

            for (var i = 0; i < array.length; i++) {
                // IE & FF
                li = document.createElement('li');
                li.setAttribute("val", array[i].toString());
                li.setAttribute("id", "li" + i.toString());
                if (IE) {
                    li.setAttribute("onmousedown", function() { $(INPUT).value = this.getAttribute('val'); removeList();});
                    li.setAttribute("onmouseover", function() {  remarkIndex();
                                                                 setIndex(parseInt(this.getAttribute('id').replace("li","")));
                                                                 markIndex();
                                                               });
                    li.setAttribute("onmouseout", function() { remarkIndex(); setIndex(-1); } );
                } else { // FF
                    li.setAttribute("onMouseDown", "$(INPUT).value = this.getAttribute('val'); removeList();" );
                    li.setAttribute("onMouseOver", "remarkIndex(); setIndex(" + i + "); markIndex();" );
                    li.setAttribute("onMouseOut", "remarkIndex(); setIndex(-1);" );
                }
                // IE & FF
                $(DISPLAY).style.visibility = "visible";
                li.appendChild(document.createTextNode(array[i]));
                ul.appendChild(li);
            }

            // should the link for more results shown
            if (!WHOLE_LIST && array.length == 31 && $(INPUT).value.length > 3) {
                // IE & FF
                var input = document.createElement('input');
                input.setAttribute("type", "button");
                if (IE) {
                    input.setAttribute("onmousedown", function() { setWholeList(true); loadList(); });
                    input.setAttribute("onmouseout", function() { this.style.background = '#b2cdbf'; });
                    input.setAttribute("onmouseover", function() { this.style.background = '#8AECFF'; });
                    input.style.background = "#b2cdbf";
                    input.style.color = "#4c4d6b";
                    input.style.border = "none";
                    input.style.width = "100%";
                } else { // FF
                    input.setAttribute("class", "more_results");
                    input.setAttribute("onMouseDown", "setWholeList(true); loadList();");
                    input.setAttribute("onMouseOver", "this.style.background = '#8AECFF';");
                    input.setAttribute("onMouseOut", "this.style.background = '#b2cdbf';");
                }
                 // IE & FF
                input.setAttribute("value", "MORE RESULTS...");
                input.setAttribute("TABINDEX","0");
                ul.appendChild(input);
            }
            $(DISPLAY).appendChild(ul);
        } else {
            $(FRAME).height = "0"; // IE
        }
    }

    // remove the list
    function removeList () {
        setIndex(-1);
        for (var i = 0; i < 2; i++) {
            if ($(DISPLAY).hasChildNodes()) {
                  $(DISPLAY).removeChild($(DISPLAY).firstChild);
              }
          }
          if ($(ERROR).hasChildNodes()) {
              $(ERROR).removeChild($(ERROR).firstChild);
          }
          if (IE) {
              $(FRAME).height = "0";
              $(DISPLAY).style.visibility = "hidden";
              $(ERROR).style.visibility = "hidden";
          }
    }

    // search and store the position from source element to the
    // target element
    function getCoordinates(target, source) {
        target.clonePosition( source, {
          setHeight:false,
          offsetTop: source.offsetHeight
        });
    }

    // created a specific cutted list of the long list
    // and store it to CUT_LIST
    function cutLongList() {
        var tmp = new Array();
        var wordList = new Array();
        var input =  $(INPUT).value;

        tmp = input.split(' ');
        // all other searchwords added to the wordList
        for (var i = 0; i < tmp.length; i++) {
              if (tmp[i].length > 0 ) {
                  wordList.push(tmp[i]);
            }
        }

        // create the short list which is displayed
        CUT_LIST = new Array();
        for (var i = 0; i < LONG_LIST.length; i++) {
            var isContent = true;
            for (var j = 0; j < wordList.length; j++) {
                if ( LONG_LIST[i].toLowerCase().indexOf(wordList[j].toLowerCase()) == -1) {
                    isContent = false;
                  }
            }
            if (isContent) {
                CUT_LIST.push(LONG_LIST[i]);
            }
        }
    }

    function scrollDown( max) {
        if ($(DISPLAY).hasChildNodes()){
            if (INDEX <= 2) {
                $('ulList').scrollTop = 0;
            } else if(INDEX > 2 && INDEX < (max - 2)) {
                var now = 0;
                var after  = $('li'+ (INDEX + 2).toString()).offsetHeight;

                for ( var i = 1; i <= (INDEX + 1); i++) {
                    now += $('li'+ i.toString()).offsetHeight;
                }

                after += now;

                if ((now % 150) > (after % 150)) {
                    $('ulList').scrollTop = now - $('li'+ (INDEX + 1).toString()).offsetHeight;
                }
            } else if (INDEX >= (max-2)) {
                var jumpDown = 0;
                for ( var i = 1; i < max; i++) {
                    jumpDown += $('li'+ i.toString()).offsetHeight;
                }
                $('ulList').scrollTop = jumpDown;
            }
        }
    }

    function scrollUp( max) {
        if ($(DISPLAY).hasChildNodes()){
            if (INDEX == max) {
                var jumpDown = 0;
                for ( var i = 1; i < max; i++) {
                    jumpDown += $('li'+ i.toString()).offsetHeight;
                }
                $('ulList').scrollTop = jumpDown;
            } else if ( INDEX > -1) {
                var now = 0;
                var after  = $('li'+ INDEX.toString()).offsetHeight;
                for (var i = max; i > INDEX; i--) {
                    now += $('li'+ i.toString()).offsetHeight;
                }
                after += now;
                if ((now % 150) > (after % 150)) {
                    var jumpUp = 1;
                    var before = 0;
                    var i = INDEX;
                    while (i > 0 && ((before % 150) < (jumpUp % 150))) {
                         before += $('li'+ i.toString()).offsetHeight;
                         i--;
                         jumpUp = before + $('li'+ i.toString()).offsetHeight;
                    }
                    var top = 0;
                    for ( var i = 1; i <= (INDEX + 1); i++) {
                        top += $('li'+ i.toString()).offsetHeight;
                    }
                    $('ulList').scrollTop = top - jumpUp;
                }
            }
        }
    }

