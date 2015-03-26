// Modified A LOT from Ext.ux.ColorField (http://www.sencha.com/learn/Extension:ColorField)
//   now supports both 'hex' and 'rgb' formats and more (see config below)
// example: { xtype: 'colorfield', allowBlank: false, value: '#F00', colorFormat:'hex', setColorOnChange: true } or
//          { xtype: 'colorfield', value: 'rgb(255,0,0)' }

Ext.ux.ColorField = Ext.extend(Ext.form.TriggerField,  {
    triggerClass : 'x-form-color-trigger',
    colorFormat: 'rgb', // 'rgb' supports 'rgb(255,0,0)' format, 'rgba' supports both rgb and 'rgba(255,0,0,0.5)' format, 'hex' supports "#3FA" or "#00FF57"
    setColorOnChange: false, // default is updating color on 'keyup' event, set this to true to only update on 'change' event

    constructor: function(config) {
      if(config.colorFormat == 'hex')
      {
        this.maskRe = /[#a-f0-9]/i;
        this.regex = /^$|^#?\w{3}$|^#?\w{6}$/i;
        this.invalidText = 'Allowed format:"#3FA" or "#00FF57" ("#" is optional)';
        this.defaultAutoCreate = {tag: "input", type: "text", size: "10", maxlength: "7", autocomplete: "off"};
      }
      else if(config.colorFormat == 'rgb')
      {
        this.maskRe = /[rgb\d,.()]/i;
        this.regex = /^$|^rgb\(\d{1,3},\d{1,3},\d{1,3}\)$/i;
        this.invalidText = 'Allowed format:"rgb(255,0,0)"';
        this.defaultAutoCreate = {tag: "input", type: "text", size: "16", autocomplete: "off"};
      }
      else
      {
        this.maskRe = /[rgba\d,.()]/i;
        this.regex = /^$|^rgb\(\d{1,3},\d{1,3},\d{1,3}\)$|^rgba\(\d{1,3},\d{1,3},\d{1,3}(?:,(1(\.0+)?|0(\.[0-9]+)?))?\)$/i;
        this.invalidText = 'Allowed format:"rgb(255,0,0) or rgba(255,0,0,0.5)"';
        this.defaultAutoCreate = {tag: "input", type: "text", size: "21", autocomplete: "off"};
      }
      Ext.apply(this, config);
      Ext.ux.ColorField.superclass.constructor.call(this);
      if(this.setColorOnChange)
        this.on('change', function(f, nv) { f.setColor(nv); });
      else
      {
        this.enableKeyEvents = true;
        this.on('keyup', function(f) { f.setColor(f.getValue()); });
      }
    },

  /**
   * Sets the current color and changes the background.
   * Does *not* change the value of the field.
   * @param {String} hex The color value.
   */
  setColor : function(color) {
    if (this.trigger)
    {
      var c = this.convertFormat(color, true);
      color = (c && c.color)? c.color : 'transparent';
      var op = c? c.alpha : 1;
      this.trigger.setStyle({
        'background-color': color,
        filter: 'alpha(opacity='+Math.floor(op*100)+')',
        '-moz-opacity': op,
        '-khtml-opacity': op,
        opacity: op
      });
    }
  },

    /**
     * Sets the value of the color field.  You can pass a string that can be parsed into a valid HTML color
     * <br />Usage:
     * <pre><code>
    colorField.setValue('RGB(255,0,0)');
       </code></pre>
     * @param {String} color The color string
     */
    setValue : function(color){
      Ext.ux.ColorField.superclass.setValue.call(this, color);
      this.setColor(color);
    },

    // private, converts #FFF or FFFFFF or rgb(255,0,0) etc. formats to 'rgb(0,255,0)' or '#00FF00'
    convertFormat: function(color, toHex) {
      if(!color) return;
      if(color.match(/^#?(\w)(\w)(\w)$/))
      {
        var a = RegExp.$1, b = RegExp.$2, c = RegExp.$3;
        if(toHex) return {color:'#' + a + b + c,alpha:1};
        else return 'rgb(' + parseInt(a+a, 16) + ','  + parseInt(b+b, 16) + ','  + parseInt(c+c, 16) + ')';
      }
      if(color.match(/^#?(\w\w)(\w\w)(\w\w)$/))
      {
        var a = RegExp.$1, b = RegExp.$2, c = RegExp.$3;
        if(toHex) return {color:'#' + a + b + c,alpha:1};
        return 'rgb(' + parseInt(a, 16) + ','  + parseInt(b, 16) + ','  + parseInt(c, 16) + ')';
      }
      if(color.match(/^rgba?\((\d+),(\d+),(\d+)(?:,(1(?:\.0)?|0(?:\.[0-9]+)?))?\)$/i))
      {
        var a = RegExp.$1, b = RegExp.$2, c = RegExp.$3, d = RegExp.$4;
        if(toHex) return {color:'#' + this.toHex(a) + this.toHex(b) + this.toHex(c),alpha:d==''?1:d};
        else return color;
      }
      return '';
    },

    toHex: function(num) {
      var n = num * 1;
      return (n < 16? '0':'') + n.toString(16);
    },

    // private
    // Implements the default empty TriggerField.onTriggerClick function to display the ColorPalette
    onTriggerClick : function(){
      if(this.disabled) return;

      if(!this.menu)
      {
        this.menu = new Ext.menu.ColorMenu({
          listeners: {
            scope: this,
            select: function(e, c) {
              // convoluted way to deal with editorGrid problem (there might be better event-based ways but couldn't find it anywhere)
              var edit = this.editable && this.gridEditor, grid;
              if(edit)
              {
                var t = Ext.get(this.gridEditor.el).findParent('div.x-grid-panel');
                if(t)
                {
                  grid = Ext.getCmp(t.id);
                  grid.startEditing(this.gridEditor.row, this.gridEditor.col);
                }
              }
              this.setValue(this.convertFormat(c, this.colorFormat == 'hex'));
              if(edit && grid) grid.stopEditing();
            },
            show : function(){ // retain focus styling
                this.onFocus();
            },
            hide : function(){
                this.focus.defer(10, this);
            }
          }
        });
      }

      this.menu.show(this.el, "tl-bl?");
    }
});

Ext.reg('colorfield',Ext.ux.ColorField);
