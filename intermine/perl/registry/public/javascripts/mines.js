jQuery(document).ready(function($) {
    var startingTip = 
        'You can register your InterMine implementation using the form below - '
        + 'but there is an even easier way: simply set up your mine to automatically '
        + 'register with us as part of your deployment cycle. '
        + '<a href="http://intermine.org/wiki/RegisteringYourMine#automatically">'
        + 'Click here</a> to see how to set this up.';

    var name = $( "#name" ),
        url = $( "#url" ),
        webservice = $( "#webservice" ),
        description = $( '#description' ),
        logo = $( '#logo' ),
        organisms = $( '#organisms' ),
        authToken = $( '#authtoken'),
        dataSources = $( '#dataSources' ),
        tips = $( ".validateTips" );
        allFields = $( [] ).add( name ).add( url ).add( webservice ).add(description)
                            .add(logo).add(organisms).add(dataSources);
    tips.html(startingTip);
    function updateTips( t ) {
        if (! tips.children('ul').length ) {
            tips.empty();
            tips.append("Sorry, there are some problems with your entry:")
                .append("<ul></ul>");
        }
        tips.children("ul").append( "<li>" + t + "</li>" );
        tips.addClass( "ui-state-highlight" );
        setTimeout(function() {
            tips.removeClass( "ui-state-highlight", 2000 );
        }, 500 );
    }
    var successPopup = $( '#success-popup' ).dialog({
        autoOpen: false,
        modal: true,
        buttons: {
            Dismiss: function() {
                $( this ).dialog( "close" );
            }
        }
    });
    var submittedPopup = $( '#submitted-popup' ).dialog({
        autoOpen: false,
        modal: true,
        buttons: {
            Dismiss: function() {
                $( this ).dialog( "close" );
            }
        }
    });
    var errorPopup = $( '#error-popup' ).dialog({
        autoOpen: false,
        modal: true,
        buttons: {
            Dismiss: function() {
                $( this ).dialog( "close" );
            }
        }
    });

    function handleResult(data) {
        tips.html(startingTip);
        submittedPopup.dialog( "close" );
        successPopup.dialog( "open" );
        if ("authToken" in data) {
            $('#created-successfully').show();
            $('#updated-successfully').hide();
            $('#new-mine-name').text(data.mineName);
            $('#reg-key').text(data.authToken).addClass( "ui-state-highlight" );;
        } else {
            $('#created-successfully').hide();
            $('#updated-successfully').show().text(data.text);
        }
        $('#minelist-container').load($('#my-address').text() + " #minelist", function() {
            $('div.mine-details').hide();
        });
    }
    function handleError(req) {
        tips.html(startingTip);
        var response = jQuery.parseJSON(req.responseText);
        $( '#failure-reason' ).text(response.text);
        submittedPopup.dialog( "close" );
        errorPopup.dialog( "open" );
    }

    function checkForSpaces(o, n) {
        if ( o.val().match(/\s/) ) {
            o.addClass( "problem" );
            updateTips( "the " + n + " must not contain any spaces" );
            return false;
        } else {
            return true;
        }
    }
    function checkIsNotEmpty(o, n) {
        if ( o.val().length < 1 ) {
            o.addClass( "problem" );
            updateTips( "the " + n + " must not be empty" );
            return false;
        } else {
            return true;
        }
    }
    function checkRegex(o, r, n) {
        if ( ! o.val().match(r)) {
            o.addClass( "problem" );
            updateTips( n );
            return false;
        } else {
            return true;
        }
    }
    function checkMaxLength(o, max, n) {
        if ( o.val().length > max ) {
            o.addClass( "problem" );
            updateTips( 
                "the " + n + " must be shorter than " 
                + max + " characters: you gave me " 
                + o.val().length 
            );
            return false;
        } else {
            return true;
        }
    }

    var urlRegex = /^(http|https|ftp)\:\/\/[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,3}(\:[a-zA-Z0-9]*)?(\/([a-zA-Z0-9\-\._\?\,\'/\\\+&amp;%\$#\=~])*)?$/;

    var registerPopup = $( "#register-popup" ).dialog({
        autoOpen: false,
        height: 650,
        width: 500,
        modal: true,
        buttons: {
            "Register Your Mine": function() {
                tips.html(startingTip);
                allFields.removeClass( "problem" );
                var bValid = true;
                bValid = checkIsNotEmpty(name, "mine name") 
                            && checkForSpaces(name, "mine name")
                            && checkRegex(name, /^[A-Za-z0-9]+$/, "the mine name can only contain upper and lower case letters, and numbers") 
                            && bValid;
                bValid = checkForSpaces(url, "home URL")
                            && checkRegex(url, urlRegex, "the home url doesn't look like a url to me")
                            && bValid;
                bValid = checkMaxLength(description, 140, "description") && bValid;
                allFields.removeClass( "ui-state-error" );

                if ( bValid ) {
                    $('#register-form').slideToggle();
                    var primaryOrganism;
                    if (organisms.val().length) {
                        primaryOrganism = organisms.val().split(",")[0];
                    }
                    var data = {
                        name: name.val(),
                        url: url.val(),
                        webservice: webservice.val(),
                        logo: logo.val(),
                        organisms: organisms.val(),
                        primaryOrganism: primaryOrganism,
                        dataSources: dataSources.val(),
                        authToken: authToken.val(),
                        description: description.val(),
                        format: "json"
                    };
                    console.log(data);
                    jQuery.ajax({
                        url: $('#register-address').text(),
                        type: 'POST',
                        data: data, 
                        success: handleResult, 
                        error: handleError,
                        dataType: "json"
                    });

                    $( this ).dialog( "close" );
                    submittedPopup.dialog( "open" );
                }
            },
            Cancel: function() {
                tips.html(startingTip);
                $( this ).dialog( "close" );
            }
        },
        close: function() {
            allFields.val( "" ).removeClass( "problem" );
        }
    });
    $( "#register-button" ).button().click(function() {
        $( '#register-form' ).show();
        $( "#register-popup" ).dialog( "open" );
        name.focus();
    });
});
// Fallback for lack of javascript
$(function() {$('div.mine-details').hide();});
