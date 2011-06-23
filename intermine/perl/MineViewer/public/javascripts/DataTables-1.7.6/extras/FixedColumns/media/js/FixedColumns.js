/*
 * File:        FixedColumns.js
 * Version:     1.1.0
 * Description: "Fix" columns on the left of a scrolling DataTable
 * Author:      Allan Jardine (www.sprymedia.co.uk)
 * Created:     Sat Sep 18 09:28:54 BST 2010
 * Language:    Javascript
 * License:     GPL v2 or BSD 3 point style
 * Project:     Just a little bit of fun - enjoy :-)
 * Contact:     www.sprymedia.co.uk/contact
 * 
 * Copyright 2010-2011 Allan Jardine, all rights reserved.
 */

var FixedColumns = function ( oDT, oInit ) {
	/* Sanity check - you just know it will happen */
	if ( typeof this._fnConstruct != 'function' )
	{
		alert( "FixedColumns warning: FixedColumns must be initialised with the 'new' keyword." );
		return;
	}
	
	if ( typeof oInit == 'undefined' )
	{
		oInit = {};
	}
	
	/**
	 * @namespace Settings object which contains customisable information for FixedColumns instance
	 */
	this.s = {
		/** 
		 * DataTables settings objects
     *  @property dt
     *  @type     object
     *  @default  null
		 */
		"dt": oDT.fnSettings(),
		
		/** 
		 * Number of left hand columns to fix in position
     *  @property leftColumns
     *  @type     int
     *  @default  1
		 */
		"leftColumns": 1,
		
		/** 
		 * Number of right hand columns to fix in position
     *  @property rightColumns
     *  @type     int
     *  @default  0
		 */
		"rightColumns": 0,
		
		/** 
		 * Store the heights of the rows for a draw. This can significantly speed up a draw where both
		 * left and right columns are fixed
     *  @property heights
     *  @type     array int
     *  @default  0
		 */
		"heights": []
	};
	
	
	/**
	 * @namespace Common and useful DOM elements for the class instance
	 */
	this.dom = {
		/**
		 * DataTables scrolling element
		 *  @property scroller
		 *  @type     node
		 *  @default  null
		 */
		"scroller": null,
		  	
		/**
		 * Scroll container that DataTables has added
		 *  @property scrollContainer
		 *  @type     node
		 *  @default  null
		 */
		"scrollContainer": null,
		
		/**
		 * DataTables header table
		 *  @property header
		 *  @type     node
		 *  @default  null
		 */
		"header": null,
		
		/**
		 * DataTables body table
		 *  @property body
		 *  @type     node
		 *  @default  null
		 */
		"body": null,
		
		/**
		 * DataTables footer table
		 *  @property footer
		 *  @type     node
		 *  @default  null
		 */
		"footer": null,
		
		/**
		 * @namespace Cloned table nodes
		 */
		"clone": {
			/**
			 * @namespace Left column cloned table nodes
			 */
			"left": {
				/**
				 * Cloned header table
				 *  @property header
				 *  @type     node
				 *  @default  null
				 */
				"header": null,
		  	
				/**
				 * Cloned body table
				 *  @property body
				 *  @type     node
				 *  @default  null
				 */
				"body": null,
		  	
				/**
				 * Cloned footer table
				 *  @property footer
				 *  @type     node
				 *  @default  null
				 */
				"footer": null
			},
			
			/**
			 * @namespace Right column cloned table nodes
			 */
			"right": {
				/**
				 * Cloned header table
				 *  @property header
				 *  @type     node
				 *  @default  null
				 */
				"header": null,
		  	
				/**
				 * Cloned body table
				 *  @property body
				 *  @type     node
				 *  @default  null
				 */
				"body": null,
		  	
				/**
				 * Cloned footer table
				 *  @property footer
				 *  @type     node
				 *  @default  null
				 */
				"footer": null
			}
		}
	};
	
	/* Let's do it */
	this._fnConstruct( oInit );
};


FixedColumns.prototype = {
	/**
	 * Update the fixed columns - including headers and footers
	 *  @method  fnUpdate
	 *  @returns void
	 */
	"fnUpdate": function ()
	{
		this._fnDraw( true );
	},
	
	
	/**
	 * Initialisation for FixedColumns
	 *  @method  _fnConstruct
	 *  @param   {Object} oInit User settings for initialisation
	 *  @returns void
	 */
	"_fnConstruct": function ( oInit )
	{
		var that = this;
		
		/* Sanity checking */
		if ( typeof this.s.dt.oInstance.fnVersionCheck != 'function' ||
		     this.s.dt.oInstance.fnVersionCheck( '1.7.0' ) !== true )
		{
			alert( "FixedColumns 2 required DataTables 1.7.0 or later. "+
				"Please upgrade your DataTables installation" );
			return;
		}
		
		if ( this.s.dt.oScroll.sX === "" )
		{
			this.s.dt.oInstance.oApi._fnLog( this.s.dt, 1, "FixedColumns is not needed (no "+
				"x-scrolling in DataTables enabled), so no action will be taken. Use 'FixedHeader' for "+
				"column fixing when scrolling is not enabled" );
			return;
		}
		
		if ( typeof oInit.columns != 'undefined' )
		{
			/* Support for FixedColumns 1.0.x initialisation parameter */
			this.s.leftColumns = oInit.columns;
		}
		
		if ( typeof oInit.iColumns != 'undefined' )
		{
			this.s.leftColumns = oInit.iColumns;
		}
		
		if ( typeof oInit.iRightColumns != 'undefined' )
		{
			this.s.rightColumns = oInit.iRightColumns;
		}
		
		/* Set up the DOM as we need it and cache nodes */
		this.dom.scrollContainer = $(this.s.dt.nTable).parents('div.dataTables_scroll')[0];
		this.dom.scrollContainer.style.position = "relative";
		
		this.dom.body = this.s.dt.nTable;
		this.dom.scroller = this.dom.body.parentNode;
		this.dom.scroller.style.position = "relative";
		
		this.dom.header = this.s.dt.nTHead.parentNode;
		this.dom.header.parentNode.parentNode.style.position = "relative";
		
		if ( this.s.dt.nTFoot )
		{
			this.dom.footer = this.s.dt.nTFoot.parentNode;
			this.dom.footer.parentNode.parentNode.style.position = "relative";
		}
		
		this.s.position = this.s.dt.oScroll.sY === "" ? 'absolute' : 'relative';
		
		/* Event handlers */
		if ( this.s.position != "absolute" )
		{
			$(this.dom.scroller).scroll( function () {
				that._fnPosition.call( that );
			} );
		}
		
		this.s.dt.aoDrawCallback.push( {
			"fn": function () {
				that._fnDraw.call( that, false );
			},
			"sName": "FixedColumns"
		} );
		
		/* Get things right to start with */
		this._fnDraw( true );
	},
	
	
	/**
	 * Clone and position the fixed columns
	 *  @method  _fnDraw
	 *  @returns void
	 *  @param   {Boolean} bAll Indicate if the headre and footer should be updated as well (true)
	 *  @private
	 */
	"_fnDraw": function ( bAll )
	{
		this._fnCloneLeft( bAll );
		this._fnCloneRight( bAll );
		this._fnPosition();
		
		this.s.heights.splice( 0, this.s.heights.length );
	},
	
	
	/**
	 * Clone the right columns
	 *  @method  _fnCloneRight
	 *  @returns void
	 *  @param   {Boolean} bAll Indicate if the headre and footer should be updated as well (true)
	 *  @private
	 */
	"_fnCloneRight": function ( bAll )
	{
		if ( this.s.rightColumns <= 0 )
		{
			return;
		}
		
		var
			that = this,
			iTableWidth = 0,
			aiCellWidth = [],
			i, jq,
			iColumns = $('thead tr:eq(0)', this.dom.header).children().length;
		
		/* Grab the widths that we are going to need */
		for ( i=this.s.rightColumns-1 ; i>=0 ; i-- )
		{
			jq = $('thead tr:eq(0)', this.dom.header).children(':eq('+(iColumns-i-1)+')');
			iTableWidth += jq.outerWidth();
			aiCellWidth.push( jq.width() );
		}
		aiCellWidth.reverse();
		
		this._fnClone( this.dom.clone.right, bAll, aiCellWidth, iTableWidth, 
			':last', ':lt('+(iColumns-this.s.rightColumns)+')' );
	},
	
	
	/**
	 * Clone the left columns
	 *  @method  _fnCloneLeft
	 *  @returns void
	 *  @param   {Boolean} bAll Indicate if the headre and footer should be updated as well (true)
	 *  @private
	 */
	"_fnCloneLeft": function ( bAll )
	{
		if ( this.s.leftColumns <= 0 )
		{
			return;
		}
		
		var
			that = this,
			iTableWidth = 0,
			aiCellWidth = [],
			i, jq;
		
		/* Grab the widths that we are going to need */
		for ( i=0, iLen=this.s.leftColumns ; i<iLen ; i++ )
		{
			jq = $('thead tr:eq(0)', this.dom.header).children(':eq('+i+')');
			iTableWidth += jq.outerWidth();
			aiCellWidth.push( jq.width() );
		}
		
		this._fnClone( this.dom.clone.left, bAll, aiCellWidth, iTableWidth, 
			':first', ':gt('+(this.s.leftColumns-1)+')' );
	},
		
	
	
	/**
	 * Clone the DataTable nodes and place them in the DOM (sized correctly)
	 *  @method  _fnClone
	 *  @returns void
	 *  @param   {Object} oClone Object containing the header, footer and body cloned DOM elements
	 *  @param   {Boolean} bAll Indicate if the headre and footer should be updated as well (true)
	 *  @param   {array} aiCellWidth Array of integers with the width's to use for the cloned columns
	 *  @param   {int} iTableWidth Calculated table width
	 *  @param   {string} sBoxHackSelector Selector to pick which TD element to copy styles from
	 *  @param   {string} sRemoveSelector Which elements to remove
	 *  @private
	 */
	"_fnClone": function ( oClone, bAll, aiCellWidth, iTableWidth, sBoxHackSelector, sRemoveSelector )
	{
		var
			that = this,
			i, iLen, jq, nTarget;
		
		/* Header */
		if ( bAll )
		{
			if ( oClone.header !== null )
			{
				oClone.header.parentNode.removeChild( oClone.header );
			}
			oClone.header = $(this.dom.header).clone(true)[0];
			oClone.header.className += " FixedColumns_Cloned";
			
			oClone.header.style.position = "absolute";
			oClone.header.style.top = "0px";
			oClone.header.style.left = "0px";
			oClone.header.style.width = iTableWidth+"px";
			
			nTarget = this.s.position == "absolute" ? this.dom.scrollContainer :
				this.dom.header.parentNode;
			nTarget.appendChild( oClone.header );
		
			this._fnEqualiseHeights( 'thead', this.dom.header, oClone.header, 
				sBoxHackSelector, sRemoveSelector );
		
			$('thead tr:eq(0)', oClone.header).children().each( function (i) {
				this.style.width = aiCellWidth[i]+"px";
			} );
		}
		else
		{
			this._fnCopyClasses(oClone.header, this.dom.header);
		}
		
		/* Body */
		/* Remove any heights which have been applied already and let the browser figure it out */
		$('tbody tr', that.dom.body).css('height', 'auto');
		
		if ( oClone.body !== null )
		{
			oClone.body.parentNode.removeChild( oClone.body );
			oClone.body = null;
		}
		
		if ( this.s.dt.aiDisplay.length > 0 )
		{
			oClone.body = $(this.dom.body).clone(true)[0];
			oClone.body.className += " FixedColumns_Cloned";
			if ( oClone.body.getAttribute('id') !== null )
			{
				oClone.body.removeAttribute('id');
			}
			
			$('thead tr:eq(0)', oClone.body).each( function () {
				$('th'+sRemoveSelector, this).remove();
			} );
			
			$('thead tr:gt(0)', oClone.body).remove();
			
			this._fnEqualiseHeights( 'tbody', that.dom.body, oClone.body, 
				sBoxHackSelector, sRemoveSelector );
			
			$('tfoot tr:eq(0)', oClone.body).each( function () {
				$('th'+sRemoveSelector, this).remove();
			} );
			
			$('tfoot tr:gt(0)', oClone.body).remove();
			
			
			oClone.body.style.position = "absolute";
			oClone.body.style.top = "0px";
			oClone.body.style.left = "0px";
			oClone.body.style.width = iTableWidth+"px";
			
			nTarget = this.s.position == "absolute" ? this.dom.scrollContainer :
				this.dom.body.parentNode;
			nTarget.appendChild( oClone.body );
		}
		
		/* Footer */
		if ( this.s.dt.nTFoot !== null )
		{
			if ( bAll )
			{
				if ( oClone.footer !== null )
				{
					oClone.footer.parentNode.removeChild( oClone.footer );
				}
				oClone.footer = $(this.dom.footer).clone(true)[0];
				oClone.footer.className += " FixedColumns_Cloned";
				
				oClone.footer.style.position = "absolute";
				oClone.footer.style.top = "0px";
				oClone.footer.style.left = "0px";
				oClone.footer.style.width = iTableWidth+"px";
				
				nTarget = this.s.position == "absolute" ? this.dom.scrollContainer :
					this.dom.footer.parentNode;
				nTarget.appendChild( oClone.footer );
			
				this._fnEqualiseHeights( 'tfoot', this.dom.footer, oClone.footer, 
					sBoxHackSelector, sRemoveSelector );
				
				$('tfoot tr:eq(0)', oClone.footer).children().each( function (i) {
					this.style.width = aiCellWidth[i]+"px";
				} );
			}
		}
	},
	
	
	/**
	 * Clone classes from one DOM node to another with (IMPORTANT) IDENTICAL structures
	 *  @method  _fnCopyClasses
	 *  @returns void
	 *  @param   {element} clone Node to copy classes to
	 *  @param   {element} original Original node to take the classes from
	 *  @private
	 */
	"_fnCopyClasses": function ( clone, original )
	{
		clone.className = original.className;
		for ( var i=0, iLen=clone.children.length ; i<iLen ; i++ )
		{
			if ( original.children[i].nodeType == 1 )
			{
				this._fnCopyClasses( clone.children[i], original.children[i] );
			}
		}
	},
	
	
	/**
	 * Equalise the heights of the rows in a given table node in a cross browser way
	 *  @method  _fnEqualiseHeights
	 *  @returns void
	 *  @param   {string} parent Node type - thead, tbody or tfoot
	 *  @param   {element} original Original node to take the heights from
	 *  @param   {element} clone Copy the heights to
	 *  @param   {string} boxHackSelector Selector to pick which TD element to copy styles from
	 *  @param   {string} removeSelector Which elements to remove
	 *  @private
	 */
	"_fnEqualiseHeights": function ( parent, original, clone, boxHackSelector, removeSelector )
	{
		var that = this,
			iHeight,
			iCalculateHeights = (parent == "tbody" && this.s.heights.length > 0) ? false : true,
			jqBoxHack = $(parent+' tr:eq(0)', original).children(boxHackSelector),
			iBoxHack = jqBoxHack.outerHeight() - jqBoxHack.height(),
			bRubbishOldIE = ($.browser.msie && ($.browser.version == "6.0" || $.browser.version == "7.0"));
		
		if ( $(parent+' tr:eq(0) th', clone).attr('rowspan') > 1 )
		{
			$(parent+' tr:gt(0)', clone).remove();
		}
		
		/* Remove cells which are not needed and copy the height from the original table */
		$(parent+' tr', clone).each( function (k) {
			$(this).children(removeSelector, this).remove();
			
			/* We can store the heights of the rows calculated on the first pass of a draw, to be used
			 * on the second pass (i.e. the right hand column). This significantly speeds up a draw 
			 * where both the left and right columns are fixed since we don't need to get the height of
			 * each row twice
			 */
			if ( iCalculateHeights )
			{
				iHeight = $(parent+' tr:eq('+k+')', original).children(':first').height();
				if ( parent == 'tbody' )
				{
					that.s.heights.push( iHeight );
				}
			}
			else
			{
				iHeight = that.s.heights[k];
			}
			
			/* Can we use some kind of object detection here?! This is very nasty - damn browsers */
			if ( $.browser.mozilla || $.browser.opera )
			{
				$(this).children().height( iHeight+iBoxHack );
				$(parent+' tr:eq('+k+')', original).height( iHeight+iBoxHack );	
			}
			else if ( $.browser.msie && !bRubbishOldIE )
			{
				$(this).children().height( iHeight-1 ); /* wtf... */
			}
			else
			{
				$(this).children().height( iHeight );
			}
		} );
	},
	
	
	/**
	 * Set the absolute position of the fixed column tables when scrolling the DataTable
	 *  @method  _fnPosition
	 *  @returns void
	 *  @private
	 */
	"_fnPosition": function ()
	{
		var
			iScrollLeft = this.s.position == 'absolute' ? 0 : $(this.dom.scroller).scrollLeft(),
			oCloneLeft = this.dom.clone.left,
			oCloneRight = this.dom.clone.right,
			iTableWidth = $(this.s.dt.nTable.parentNode).width();
			
		if ( this.s.position == 'absolute' )
		{
			var iBodyTop = $(this.dom.body.parentNode).position().top;
			if ( this.dom.footer )
			{
				var iFooterTop = $(this.dom.footer.parentNode.parentNode).position().top;
			}
		}
		
		if ( this.s.leftColumns > 0 )
		{
			oCloneLeft.header.style.left = iScrollLeft+"px";
			if ( oCloneLeft.body !== null )
			{
				oCloneLeft.body.style.left = iScrollLeft+"px";
				if (  this.s.position == 'absolute' )
				{
					oCloneLeft.body.style.top = iBodyTop+"px";
				}
			}
			if ( this.dom.footer )
			{
				oCloneLeft.footer.style.left = iScrollLeft+"px";
				if (  this.s.position == 'absolute' )
				{
					oCloneLeft.footer.style.top = iFooterTop+"px";
				}
			}
		}
		
		if ( this.s.rightColumns > 0 )
		{
			var iPoint = iTableWidth - $(oCloneRight.body).width() + iScrollLeft;
			
			oCloneRight.header.style.left = iPoint+"px";
			if ( oCloneRight.body !== null )
			{
				oCloneRight.body.style.left = iPoint+"px";
				if (  this.s.position == 'absolute' )
				{
					oCloneRight.body.style.top = iBodyTop+"px";
				}
			}
			if ( this.dom.footer )
			{
				oCloneRight.footer.style.left = iPoint+"px";
				if (  this.s.position == 'absolute' )
				{
					oCloneRight.footer.style.top = iFooterTop+"px";
				}
			}
		}
	}
};
