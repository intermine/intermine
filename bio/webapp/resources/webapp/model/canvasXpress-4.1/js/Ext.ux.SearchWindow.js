Ext.ux.SearchWindow = Ext.extend(Ext.Window, {
  searchCriteria: [],
  searchItemIdx: 0,
  notOpFn: null, // must be implemented if you want to support 'NOT' operation for the set operations. The function takes in an array of objects and should return the complementary array back
  mouseHighlight: true, // true to highlight result set when mouse over
  inputStyle: 'background-color:#0F0;', // coloring for textfield supporting only Sets from other search criteria as input
  partInputStyle: 'background-color:#FA0;', // coloring for textfield supporting both normal string and Sets from other search criteria as input

  constructor: function(config) {
    if(!config) config = {};
    if(config.searchCriteria)
      this.searchCriteria = config.searchCriteria;

    // searchCriteria has fields: [ 'displayF', 'valueF', 'typeF', 'defaultVals', 'searchOpts' ]
    //   'typeF' could be 'number', 'string', 'custom'
    //   where 'defaultVals' could be either a simple array or array of arrays like
    //   [['val','displayVal'],...] used as store of comboBox
    //   if 'typeF' = 'custom' then 'defaultVals' should be an array containing
    //     function(inputSet, restrictSet) and its (optional) scope
    //   'searchOpts' takes:
    //     'exactMatch': match search term exactly
    //     'takeInput': supports 'INPUT:1' as search term (then the 'search' eventHandler
    //                  must be able to handle valueTestFunc == null and Set!)
    // 'search' event takes (attr, valueTestFunc, callbackFunc, inputSet, Set)
    //   Note that when Set is defined, valueTestFunc is null. eventHandler must handle Set
    // 'highlight' event takes (resultArray)
    // 'clear' event takes nothing
    // 'list' event takes (resultArray)
    this.addEvents('search','highlight','clear','list');
    this.eTerm = 'Example: (1 and 2) or not 3';
    Ext.apply(this, config, {
      title: 'Search Dialog',
      autoScroll: true,
      width: 650,
      height: 400,
      resizable: false,
      defaults: { width: '98%' },
      bodyStyle: { padding: 3 },
      constrainHeader: true,
      layout: 'fit',
      items: {
        xtype: 'form',
        labelWidth: 15,
        defaults: { style: 'margin: 3px;' },
        items: [
          { xtype: 'displayfield', style: 'font-size:14; margin: 3px;', hideLabel: true,
            value: 'Enter logical operations between search criteria 1, 2, 3... below:' },
          { xtype: 'compositefield', style: 'margin-left: 10px;', anchor:'-20', hideLabel: true,
            items: [
              { xtype: 'textfield', width: 400, emptyText: this.eTerm,
                style: this.inputStyle,
                enableKeyEvents: true,
                listeners: {
                  scope: this,
                  change: this.getResults,
                  afterrender: function(f) {
                    new Ext.ToolTip({
                      target: f.id,
                      html: '(1 AND 2) OR NOT 3<p><b>Note:</b><ul><li>NOT,AND,OR are NOT case-sensitive.</li></ul>',
                      title: 'Acceptable Search Terms'
                    });
                  }
                }
              },
              { xtype: 'button', text: 'Search', scope: this,
                toolTip: 'Click to highlight the search results',
                handler: function(b) {
                  if(this.currentResults)
                    this.fireEvent('highlight', this.currentResults);
                }
              },
              { xtype: 'button', text: 'Clear', scope: this,
                toolTip: 'Click to clear the highlight on search results',
                handler: function(b) {
                  b.nextSibling().setValue('');
                  b.previousSibling().previousSibling().setValue('');
                  this.currentResults = null;
                  this.fireEvent('clear');
                }
              },
              {
                xtype:'displayfield', style:'color:red; margin-left:2px;',
                listeners: {
                  scope: this,
                  afterrender: function(f) {
                    if(this.hasListener('list'))
                    {
                      f.el.applyStyles('cursor:hand;cursor:pointer');
                      f.el.on('click', function() {
                        var res = this.currentResults;
                        if(res && res.length) this.fireEvent('list', res);
                      }.createDelegate(this));
                    }
                    if(this.mouseHighlight)
                    {
                      f.el.on('mouseover', function() {
                        var res = this.currentResults;
                        if(res && res.length) this.fireEvent('highlight', res);
                      }.createDelegate(this));
                      f.el.on('mouseout', function() {
                        this.fireEvent('clear');
                      }.createDelegate(this));
                    }
                  }
                }
              }
            ]
          }
        ],
        listeners: {
          scope: this,
          afterrender: function() { this.items.items[0].add(this.getNextItem()) }
        }
      }
    });
    Ext.ux.SearchWindow.superclass.constructor.apply(this);
  },
  getResults: function(f, term) {
    if(!f)
    {
      f = this.items.items[0].items.items[1].items.items[0];
      term = f.getValue();
    }
    if(term == this.eTerm) return;
    var p = f.ownerCt.items.items;
    var res = this.processSets([], term.replace(/\s+/g, '').replace(/OR/gi, '|').replace(/AND/gi, '&').replace(/NOT/gi, '!'));
    p[p.length - 1].setValue(res? res.length:'0');
    this.currentResults = res;
  },
  getItem: function(type, vals, opts) {
    var items = [];
    if(type == 'number')
    {
      items.push({
        xtype: 'textfield',
        width: 180,
        style: 'margin-left: 5px',
        emptyText: 'Mouse over for tips',
        listeners: {
          scope: this,
          afterrender: function(f) {
            new Ext.ToolTip({
              target: f.id,
              html: '23.5<br>1-5 (Same as "X>=1 and X<=5")<br>IN (3, 5, 7)<br>X != 1<br>X=1 OR (X >0.9 AND X <= 0.95)<p><b>Note:</b><ul><li>IN,AND,OR,X are NOT case-sensitive.</li><li>X will be replaced by the value to be tested against your search term</li></ul>',
              title: 'Acceptable Search Terms'
            });
          },
          change: this.matchNumber
        }
      });
    }
    else if(type == 'custom')
    {
      var o = {
        xtype: 'textfield',
        width: 80,
        style: 'margin-left: 5px;' + this.inputStyle,
        listeners: {
          scope: this,
          change: this.matchCustom
        }
      };
      if(vals)
      {
        o.emptyText = 'Mouse over for tips';
        o.listeners.afterrender = function(f) {
          new Ext.ToolTip({
            target: f.id,
            html: '(1 AND 2) OR NOT 3<p><b>Note:</b><ul><li>NOT,AND,OR are NOT case-sensitive.</li><li>Results from the logical operations on criteria #1,2,3 will become the input of this custom criteria</li></ul>',
            title: 'Acceptable Search Terms'
          });
        }
      }
      items.push(o);
    }
    else if(type == 'string')
    {
      items.push({
        xtype: 'textfield',
        width: 150,
        style: 'margin-left: 5px;' + (opts.takeInput? this.partInputStyle : ''),
        emptyText: 'Mouse over for tips',
        listeners: {
          scope: this,
          afterrender: function(f) {
            new Ext.ToolTip({
              target: f.id,
              html: 'IN (GPCR, Kinase)<br>(Kinase OR transmembrane) AND domain<br>REGEX:/(Kinase|transmembrance).+domain/i<br>NOT kinase'+(opts.takeInput? '<br>INPUT:2' : '')+'<p><b>Note:</b><ul><li>IN,OR,AND,NOT,REGEX'+(opts.takeInput? 'INPUT' : '')+': are all case-sensitive.</li><li>Actual search keyword is NOT case-sensitive (kinase = Kinase)</li>'+(opts.takeInput? '<li>INPUT allows using the results from another criteria as input for this critera</li>' : '')+'</ul>',
              title: 'Acceptable Search Terms'
            });
          },
          change: this.matchString
        }
      });
    }
    if(type != 'custom' && vals && vals.length)
    {
      items.push({
        xtype: 'combo',
        width: 130,
        store: vals,
        triggerAction: 'all',
        mode: 'local',
        style: 'margin-left: 10px',
        listeners: {
          scope: this,
          select: function(c, r) {
            var tf = c.previousSibling(), val = tf.getValue();
            tf.setValue((val.match(/\S/)? val + ' ': val) + c.getValue());
            this.matchString(tf);
          }
        }
      });
    }
    var l = this.hasListener('list');
    items.push({
      xtype:'displayfield', style:'color:red; margin-left:15px;' + (l? 'cursor:hand;cursor:pointer;' : ''),
      listeners: {
        scope: this,
        afterrender: function(f) {
          if(l) f.el.on('click', function() {
            var res = Ext.getCmp(f.cfId).currentResults;
            if(res && res.length) this.fireEvent('list', res);
          }.createDelegate(this));
          if(this.mouseHighlight)
          {
            f.el.on('mouseover', function() {
              var res = Ext.getCmp(f.cfId).currentResults;
              if(res && res.length) this.fireEvent('highlight', res);
            }.createDelegate(this));
            f.el.on('mouseout', function() {
              this.fireEvent('clear');
            }.createDelegate(this));
          }
        }
      }
    });
    return items;
  },
  getParanthesis: function(exp) {
    if(exp.match(/^\(/))
    {
      var t = exp.split(''), cnt = 1, exp1 = [], exp2 = [];
      for(var i = 1; i < t.length; i++)
      {
        if(t[i] == '(') cnt++;
        if(t[i] == ')') cnt--;
        if(!cnt) break;
        exp1.push(t[i]);
      }
      if(i < t.length - 1)
        for(var j = i + 1; j < t.length; j++)
          exp2.push(t[j]);
      if(exp1.length)
        return [exp1.join(''), exp2.join('')];
      else
        Ext.Msg.alert('Error', 'Unmatched paranthesis!');
    }
  },
  processSets: function(res, exp) {
    if(!exp || !exp.match(/\S/)) return res;
    var op;
    if(exp.match(/^!(.+)/)) // process 'NOT'
    {
      exp = RegExp.$1;
      if(exp.match(/^(\d+)(.*)/))
      {
        exp = RegExp.$2;
        return this.processSets(this.setOperations(this.getSet(RegExp.$1), null, '!'), exp);
      }
      else if(exp.match(/^\(/))
      {
        var exps = this.getParanthesis(exp);
        if(exps)
          return this.processSets(this.setOperations(this.processSets([], exps[0]), null, '!'), exps[1]);
        else
          return res;
      }
    }
    if(exp.match(/^\(/)) // process paranthesis first
    {
      var exps = this.getParanthesis(exp);
      if(exps)
        return this.processSets(this.processSets([], exps[0]), exps[1]);
      else
        return res;
    }
    var op;
    if(exp.match(/^([|&])(.*)/)) // happens after paranthesis on the left is processed first
    {
      op = RegExp.$1;
      exp = RegExp.$2;
    }
    else if(exp.match(/^(.+?)([|&])(.*)/)) // op1 op op2
    {
      exp = RegExp.$3;
      op = RegExp.$2;
      res = this.getSet(RegExp.$1);
    }
    else if(exp.match(/^(\d+)$/))
      return this.getSet(RegExp.$1);
    else
      return [];
    return this.setOperations(res, this.processSets(res, exp), op);
  },
  getSet: function(num) {
    var cf = Ext.getCmp(this.id + '-cf' + num);
    if(cf) return cf.currentResults;
    else Ext.Msg.alert('Error', 'Unknown search criteria:' + num);
  },
  setOperations: function(res, res1, op) {
    if(!res) res = [];
    if(!res1) res1 = [];
    var combined = [];
    if(op == '!')
    {
      if(res.length)
      {
        if(this.notOpFn)
        {
          var tmp = this.notOpFn(res);
          return tmp? tmp : [];
        }
        else
        {
          Ext.Msg.alert('Error', '"NOT" operation is NOT supported!');
          return [];
        }
      }
      else
      {
        Ext.Msg.alert('Error', '"NOT" operation on empty set is disabled!');
        return [];
      }
    }
    else if(op == '|') // if we have guaranteed unique ids, we don't need double loops (I tested using obj as hash keys and it didn't work)
    {
      for(var i = 0; i < res.length; i++)
        combined.push(res[i]);
      for(var i = 0; i < res1.length; i++)
      {
        for(var j = 0; j < combined.length; j++)
          if(combined[j] == res1[i]) break;
        if(j >= combined.length) combined.push(res1[i]);
      }
    }
    else
    {
      for(var i = 0; i < res1.length; i++)
      {
        var found = 0;
        for(var j = 0; j < res.length; j++)
          if(res[j] == res1[i])
          {
            found++;
            break;
          }
        if(found) combined.push(res1[i]);
      }
    }
    return combined;
  },
  matchNumber: function(f, n, o, noUpdate) {
    var exp, p = f.ownerCt.items.items, df = p[p.length - 1],
        attr = p[1].getValue(), input = p[0].getValue().trim(), inpset;

    if(!n) n = f.getValue();
    if(input.match(/\d/))
      inpset = this.processSets([], input);

    if(n.match(/^\s*[0-9.]+\s*$/))
      exp = 'X == ' + n;
    else if(n.match(/^\s*([0-9.]+)\s*-\s*([0-9.]+)\s*$/))
      exp = 'X >= ' + RegExp.$1 + ' && X <= ' + RegExp.$2;
    else if(n.match(/^\s*IN\s*\(([0-9,]+)\)/i))
      exp = 'X == ' + RegExp.$1.split(/[ ,]+/).join(' || X == ');
    else if(n.match(/X/i))
      exp = n.replace(/([^<>]|^)=/g, '$1==').replace(/\bOR\b/gi, '||').replace(/\bAND\b/gi, '&&');
    if(exp)
      this.fireEvent('search', attr, function(val) {
        if(!val) return false;
        try { return eval(exp.replace(/\bX\b/gi, val.toString())) }
        catch(err) { return false; }
      }, function(res) {
        // update display
        df.setValue(res.length? res.length : '0');
        Ext.getCmp(f.cfId).currentResults = res;
        if(!noUpdate) this.resetResults(f.searchItemIdx);
      }.createDelegate(this), inpset);
    else
      df.setValue('0');
  },
  matchString: function(f, n, o, noUpdate) {
    var res = [], exp, it = f.ownerCt.items.items, df = it[it.length - 1],
        attr = it[1].getValue(), input = it[0].getValue().trim(), inpset;
    if(!n) n = f.getValue();
    var callback = function(res) {
      // update display
      df.setValue(res.length? res.length : '0');
      Ext.getCmp(f.cfId).currentResults = res;
      if(!noUpdate) this.resetResults(f.searchItemIdx);
    }.createDelegate(this);

    if(input.match(/\d/))
      inpset = this.processSets([], input);

    var exact = f.searchOpts && f.searchOpts.exactMatch, eB = exact? '^' : '', eE = exact? '$' : '';
    if(n.match(/^\s*INPUT:(.+)/))
    {
      exp = RegExp.$1, supportInput = f.searchOpts && f.searchOpts.takeInput;
      if(!supportInput)
      {
        Ext.Msg.alert('Error', 'This search field does NOT support INPUT');
        f.focus();
        return;
      }
      var set = this.processSets([], exp.replace(/\s+/g, '').replace(/OR/gi, '|').replace(/AND/gi, '&').replace(/NOT/gi, '!'));
      if(set)
        this.fireEvent('search', attr, null, callback, inpset, set);
    }
    else if(n.match(/^\s*REGEX:(.+)/))
    {
      exp = eB + RegExp.$1 + eE;
      this.fireEvent('search', attr, function(val) {
          return val? val.toString().match(exp) : false;
        }, callback, inpset);
    }
    else
    {
      if(n.match(/^\s*IN\s*\((.+?)\)/))
        exp = 'X.match(/' + eB + RegExp.$1.split(/[ ,]+/).join(eE + '/i) || X.match(/' + eB) + eE + '/i)';
      else if(n.match(/\S/))
        exp = n.replace(/\bOR\b/g, '||').replace(/\bAND\b/g, '&&').replace(/\bNOT\b/g, '!').replace(/(.*?)(\(|\)|\|\||&&|!|$)/g,
                   function(m,n,o){return (n.match(/\S/)?' X.match(/' + eB + n.trim() + eE + '/i) ':' ') + o});
      if(exp)
        this.fireEvent('search', attr, function(val) {
          if(!val) return false;
          var a = exp.replace(/X\.match/g, '\'' + val.toString() + '\'.match');
          try { return eval(a); }
          catch(err) { return false }
        }, callback, inpset);
    }
  },
  matchCustom: function(f, n, o, noUpdate) {
    var p = f.ownerCt.items.items, df = p[p.length - 1],
        input = p[0].getValue().trim(), inpset, set;
    var combo = p[1], rec = combo.findRecord(combo.valueField, combo.getValue()),
        vals = rec.data.defaultVals;
    if(!n) n = f.getValue().trim();

    if(input.match(/\d/))
      inpset = this.processSets([], input);
    if(n.match(/\d/))
      set = this.processSets([], n);

    var res = vals[0].call(vals[1] || this, inpset, set);
    // update display
    df.setValue(res.length? res.length : '0');
    Ext.getCmp(f.cfId).currentResults = res;
    if(!noUpdate) this.resetResults(f.searchItemIdx);
  },
  resetResults: function(num, isRemove) {
    var formItems = this.items.items[0].items.items, msgs = [];
    if(formItems[1].items.items[0].getValue().indexOf(num) > -1)
    {
      if(isRemove) msgs.push('search query above');
      else this.getResults();
    }
    // now refresh every item that needs updated
    var items = {}, changed = [num], seen = {};
    for(var i = 2; i < formItems.length; i++)
    {
      var fi = formItems[i].innerCt.items.items, id = formItems[i].cfId,
          it1 = fi[0].getValue(), it2 = fi[4]? fi[4].getValue() : '';
      items[id] = fi;
      if(id == num) continue;
      var matches = it1.match(/\d+/g), matches1 = null;
      if(!matches) matches = [];
      var combo = fi[1], rec = combo.findRecord(combo.valueField, combo.getValue());
     
      if(it2.match(/^\s*INPUT:(.+)/))
        matches1 = RegExp.$1.match(/\d+/g);
      else if(rec.data.typeF == 'custom')
        matches1 = it2.match(/\d+/g);
      if(matches1)
        for(var k = 0; k < matches1.length; k++)
          matches.push(matches1[k]);

      for(var k = 0; k < matches.length; k++)
      {
        if(!seen[matches[k]]) seen[matches[k]] = {};
        seen[matches[k]][id] = 1;
      }
    }
    if(isRemove)
    {
      var msg = 'Remember to remove the deleted search criteria ' + num + ' from your ', remove = [];
      if(seen[num])
      {
        for(var i in seen[num])
          remove.push(i);
        msgs.push('search criteria ' + remove.join(','));
      }
      if(msgs.length)
        Ext.Msg.alert('Warning', msg + msgs.join(' and '));
      return;
    }
    var processed = {}, oldChange = {};
    oldChange[num] = 1;
    for(var j = 0; j < changed.length; j++)
    {
      var a = seen[changed[j]], newChange = {};
      if(a)
      {
        if(processed[changed[j]])
        {
          Ext.Msg.alert('Error', 'You have a logical loop involving criteria #' + changed[j]);
          continue;
        }
        for(var i in a)
        {
          this.resetItemResults(items[i], true);
          newChange[i] = 1;
        }
        processed[changed[j]] = 1;
        for(var i in newChange)
        {
          if(!oldChange[i])
          {
            changed.push(i);
            oldChange[i] = 1;
          }
        }
      }
    }
  },
  resetItemResults: function(p, noUpdate) { // p = items array of current item
    if(p.length < 5 || p[4].getValue().match(/^\s*$/)) return;

    var combo = p[1], rec = combo.findRecord(combo.valueField, combo.getValue());

    if(rec.data.typeF == 'string') this.matchString(p[4], null, null, noUpdate);
    else if(rec.data.typeF == 'number') this.matchNumber(p[4], null, null, noUpdate);
    else if(rec.data.typeF == 'custom') this.matchCustom(p[4], null, null, noUpdate);
  },
  getNextItem: function() {
    this.searchItemIdx++;
    var store = new Ext.data.ArrayStore({
            fields: [ 'displayF', 'valueF', 'typeF', 'defaultVals', 'searchOpts' ],
            data: this.searchCriteria
          }), id1 = this.searchItemIdx, id = this.id + '-cf' + this.searchItemIdx;
    return {
      xtype: 'compositefield',
      id: id,
      cfId: id1,
      fieldLabel: this.searchItemIdx,
      labelStyle: 'margin: 3px;',
      width: '95%',
      items: [
        {
          xtype: 'textfield',
          width: 80,
          style: this.inputStyle,
          listeners: {
            scope: this,
            afterrender: function(f) {
              new Ext.ToolTip({
                target: f.id,
                html: '(1 AND 2) OR NOT 3<p><b>Note:</b><ul><li>NOT,AND,OR are NOT case-sensitive.</li><li>Results from the logical operations on criteria #1,2,3 will become the starting point of the current criteria</li></ul>',
                title: 'Starting Set for This Critera'
              });
            },
            change: function(f) {
              this.resetItemResults(f.ownerCt.items.items);
            }
          }
        },
        {
          xtype:'combo',
          width: 150,
          store: store,
          triggerAction: 'all',
          mode: 'local',
          valueField: 'valueF',
          displayField: 'displayF',
          listeners: {
            scope: this,
            select: function(c, r) {
              var p = c.ownerCt;
              for(var i = p.items.items.length - 1; i > 3; i--)
                p.items.items[i].destroy();
              var items = this.getItem(r.data.typeF, r.data.defaultVals, r.data.searchOpts);
              for(var i = 0; i < items.length; i++)
              {
                items[i].cfId = id;
                items[i].searchItemIdx = id1;
                items[i].searchOpts = r.data.searchOpts;
              }
              p.add(items);
              p.doLayout();
              Ext.getCmp(id).currentResults = null;
            }
          }
        },
        { xtype:'button', width: 20, text:'+',
          tooltip: 'Add another row of search criteria', scope: this,
          handler: function() {
            var form = this.items.items[0];
            form.add(this.getNextItem());
            form.doLayout();
          }
        },
        { xtype:'button', width: 20, text:'-',
          tooltip: 'Remove the current row of search criteria',
          scope: this,
          handler: function(b) {
            this.items.items[0].remove(Ext.getCmp(id));
            this.resetResults(id1, true);
          }
        }
      ]
    };
  }
});
Ext.reg('searchwin', Ext.ux.SearchWindow);
