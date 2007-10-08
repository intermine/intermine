#!/usr/bin/perl
#
# Copyright (C) 2006 Instituto Tecnologico y de Estudios Superiores de Monterrey.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

$VERSION = '1.0';
use 5.8.0;
use strict;

my $__usage__ = qq(A simple script to automatically produce sitemaps for a webserver,
in the Google Sitemap Protocol (GSP).

Usage: perl sitemap_gen.pl --config=config.xml [--help] [--testing]
            --config=config.xml, specifies config file location
            --help, displays usage message
            --testing, specified when user is experimenting
); 

# Avoid warnings from XML::SAX 
BEGIN { $ENV{HARNESS_ACTIVE} = 1 } 
my $DOWARN = 1;
my $WARNING = 0;
BEGIN { $SIG{'__WARN__'} = sub { warn $_[0] if $DOWARN; $WARNING = 1; } }

# Required Perl Modules
my @REQUIRED_MODULES = (
	"Digest::MD5",   # Perl interface to the MD5 Message-Digest Algorithm
	"Encode",        # Character encodings
	"File::Find",    # Traverse a directory tree 
	"File::Glob",    # Perl extension for BSD glob routine
	"File::Spec",    # Portably perform operations on file names
	"Getopt::Long",  # Extended processing of command line options
	"LWP::Simple",   # Simple procedural interface to LWP
	"URI::URL",      # Uniform Resource Locators
	"URI::Escape",   # Escape and unescape unsafe characters
	"XML::SAX"       # Simple API for XML
);
# Validate that required modules are installed
foreach my $module (@REQUIRED_MODULES) {
	eval "use $module";
	die "[ERROR] Perl Module '$module' is required but not installed.\nSee README for installation notes.\n" if $@;
}

# Boolean and other variables
my $True = 1;
my $False = 0;
my $None = 2;

# Command flags
my %flags;

# Text encodings
my $ENC_ASCII = 'ASCII';
my $ENC_UTF8 = 'UTF-8';
my @ENC_ASCII_LIST = ('ASCII', 'US-ASCII', 'US', 'IBM367', 'CP367', 'ISO646-US', 'ISO_646.IRV:1991', 'ISO-IR-6', 'ANSI_X3.4-1968', 'ANSI_X3.4-1986', 'CPASCII' );
my @ENC_DEFAULT_LIST = ('ISO-8859-1', 'ISO-8859-2', 'ISO-8859-5');

# Maximum number of urls in each sitemap, before next Sitemap is created
my $MAXURLS_PER_SITEMAP = 50000;

# Suffix on a Sitemap index file
my $SITEINDEX_SUFFIX = '_index.xml';

# Regular expressions tried for extracting URLs from access logs.
my $ACCESSLOG_CLF_PATTERN = qr{.+\s+"([^\s]+)\s+([^\s]+)\s+HTTP/\d+\.\d+"\s+200\s+.*};
# Match patterns for lastmod attributes
my @LASTMOD_PATTERNS = (
	qr{^\d\d\d\d$},
	qr{^\d\d\d\d-\d\d$},
	qr{^\d\d\d\d-\d\d-\d\d$},
	qr{^\d\d\d\d-\d\d-\d\dT\d\d:\d\dZ$},
	qr{^\d\d\d\d-\d\d-\d\dT\d\d:\d\d[+-]\d\d:\d\d$'},
	qr{^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?Z$},
	qr{^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?[+-]\d\d:\d\d$}
	);
# Match patterns for changefreq attributes
my @CHANGEFREQ_PATTERNS = ('always', 'hourly', 'daily', 'weekly', 'monthly', 'yearly', 'never');

# XML formats
my $SITEINDEX_HEADER = qq(<?xml version="1.0" encoding="UTF-8"?>
<sitemapindex
  xmlns="http://www.google.com/schemas/sitemap/0.84"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.google.com/schemas/sitemap/0.84
                      http://www.google.com/schemas/sitemap/0.84/siteindex.xsd">\n);
my $SITEINDEX_FOOTER = "</sitemapindex>\n";
my $SITEINDEX_ENTRY = qq( <sitemap>
  <loc>LOC</loc>
  <lastmod>LASTMOD</lastmod>
 </sitemap>\n);
my $SITEMAP_HEADER = qq(<?xml version="1.0" encoding="UTF-8"?>
<urlset
  xmlns="http://www.google.com/schemas/sitemap/0.84"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.google.com/schemas/sitemap/0.84
                      http://www.google.com/schemas/sitemap/0.84/sitemap.xsd">\n);
my $SITEMAP_FOOTER = "</urlset>\n";
my $SITEURL_XML_PREFIX = " <url>\n";
my $SITEURL_XML_SUFFIX = " </url>\n";

# Search engines to notify with the updated sitemaps
# This list is very non-obvious in what's going on.  Here's the gist:
my @NOTIFICATION_SITES = ({
	scheme 	 => 'http', 
	netloc 	 => 'www.google.com', 
	path   	 => 'webmasters/sitemaps/ping', 
	query	 => {}, 		# <-- EXCEPTION: specify a query map rather than a string
	fragment => '', 
	sitemap	 => 'sitemap'	# - query attribute that should be set to the new Sitemap URL
	});

my $encoder = Encoder->new();
my $output = Output->new() ;

{package Encoder;
    #  Manages wide-character/narrow-character conversions.

    #  General high-level methodologies used in sitemap_gen:

    #  [PATHS]
    #  File system paths may be wide or narrow, depending on platform.
    #  This class has MaybeNarrowPath() which should be called on every
    #  file system path you deal with.

    #  [URLS]
    #  URL locations are stored in Narrow form, already escaped.  This has the
    #  benefit of keeping escaping and encoding as close as possible to the format
    #  we read them in.

    #  [OTHER]
    #  Other text, such as attributes of the URL class, configuration options,
    #  etc, are generally stored in Unicode for simplicity.
	  
	sub new {
		my($class) = @_;
	    my $self = {};
	    $self->{_user} = undef;				# User-specified default encoding
	    $self->{_learned} = ();				# Learned default encodings
	    $self->{_widefiles} = $False;		# File system can be wide
		bless ($self, $class);
			
		# Can the file system be Unicode?
		if($^O eq "MSWin32"){				#Windows
			my $version_Windows_NT = 2;
			$self->{_widefiles} = Win32::GetOSVersion() == $version_Windows_NT;
		}		
		elsif($^O eq "darwin"){				#Mac OS
			$self->{_widefiles} = $True;
		} 
	  
	    # Try to guess a working default
		my $encoding = undef;
		eval{
		    #Windows
			if($^O eq "MSWin32"){
				$encoding = "mbcs";
			}		
			#Mac OS
			elsif($^O eq "darwin"){
				$encoding = "utf-8";
			}
			#Unix and Unix-Like platforms
			elsif($^O eq "aix" || $^O eq "bsdos" || $^O eq "dgux" || $^O eq "dynixptx" || $^O eq "freebsd" ){
				$encoding = 'nl_langinfo(CODESET)';
			}
			elsif($^O eq "linux" || $^O eq "hpux" || $^O eq "irix" || $^O eq "machten" || $^O eq "next" || $^O eq "openbsd" ){
				$encoding = 'nl_langinfo(CODESET)';
			}
			elsif($^O eq "dec_osf" || $^O eq "svr4" || $^O eq "sco_sv" || $^O eq "svr4" || $^O eq "unicos" || $^O eq "unicosmk" ){
				$encoding = 'nl_langinfo(CODESET)';
			}
			elsif($^O eq "solaris" || $^O eq "sunos"){
				$encoding = 'nl_langinfo(CODESET)';
			}
			
			my $encoding_in_ascii_list = $False;
			if($encoding){
				foreach my $myencoding (@ENC_ASCII_LIST) {
						if($myencoding eq uc($encoding)) {
							$encoding_in_ascii_list = $True;
							last;
						}
				}
				if(!$encoding_in_ascii_list) {
					@{$self->{_learned}} = ($encoding);
				}
			}
		};
		if($@){}

	    # If we had no guesses, start with some European defaults
		if(!@{$self->{_learned}}){
			@{$self->{_learned}} = @ENC_DEFAULT_LIST;
		}

		return $self;
	}
	# end new
	
	sub SetUserEncoding {
		my($self, $encoding) = @_;
		$self->{_user} = $encoding;
	}
	# end SetUserEncoding
	
	sub NarrowText {
	    my($self, $text, $encoding) = @_;
	    # Narrow a piece of arbitrary text 
		
		# Return if not Unicode		
		$DOWARN = 0;
		my @chars = unpack("U0U*", $text);	
		$DOWARN = 1;
		if($WARNING) {
			$WARNING = 0;
			return $text;
		}
		if($^O ne "MSWin32") {
			return $text;
		}
		
		# Look through learned defaults, knock any failing ones out of the list
		while(@{$self->{_learned}}){
			my $result = undef;
			eval{
				if($self->{_learned}->[0] eq "mbcs"){
					eval "use Win32::MBCS";
					if($@){
						$output->Error("Perl Module Win32::MBCS is not installed in your computer and it is required to run this script.");
						return $text;
					}
					Win32::MBCS::Utf8ToLocal($text);
					$result = $text;
				}
				else{
					$result = encode($self->{_learned}->[0], $text);
				}
			};
			if($@){
				shift @{$self->{_learned}};
				$result = undef;
			}
			if($result){
				return $result;
			}
		}
		
		# Try the passed in preference
		if($encoding) {
			my $result = undef;
			eval {
				$result = encode($encoding, $text);
				my $enc_in_learned = $False;
				foreach my $enc (@{$self->{_learned}}) {
					if($enc eq $encoding) {
						$enc_in_learned = $True;
						last;
					}
				}
				if(!$enc_in_learned){ 
					push(@{$self->{_learned}}, $encoding);
				}
			};
			if($@) {
				$output->Warn('Unknown encoding: '. $encoding);
			}
			if($result){
				return $result;
			}
		}
		
		# Try the user preference
	    if($self->{_user}){
			my $result = undef;
			eval{
				$result = encode($self->{_user}, $text);
			};
			if($@){
				my $temp = $self->{_user};
				$self->{_user} = undef;
				$output->Warn('Unknown default_encoding:'. $temp);
			}
			if($result){
				return $result;
			}
		}
			
		# When all other defaults are exhausted, use UTF-8
		my $result = undef;
		eval{
			$result = Encode::encode_utf8($text);
		};
		if($@){
		}
		if($result){
			return $result;
		}
		
		# Something is seriously wrong if we get to here
		return encode($ENC_ASCII, $text, undef);
	}
	# end NarrowText
	
	sub MaybeNarrowPath { 
	    my($self, $text) = @_;
	    #Paths may be allowed to stay wide 
		
	    return $self->NarrowText($text, undef);
	}
	# end MaybeNarrowPath
}
#end package Encoder

{package Output;
    # Exposes logging functionality, and tracks how many errors
    # we have thus output.

    # Logging levels should be used as thus:
	# 	Fatal     -- extremely sparingly
    # 	Error    -- config errors, entire blocks of user 'intention' lost
    # 	Warn    -- individual URLs lost
    # 	Log(,0)  -- Un-suppressable text that's not an error
    # 	Log(,1)   -- touched files, major actions
    # 	Log(,2)   -- parsing notes, filtered or duplicated URLs
    # 	Log(,3)   -- each accepted URL
	  
	sub new { 
		my $class = shift;
	    my $self = {};
	 
	    $self->{num_errors} = 0;			# Count of errors
	    $self->{num_warns} = 0;				# Count of warnings
	    $self->{_errors_shown} = {};		# Shown errors
	    $self->{_warns_shown} = {};			# Shown warnings
	    $self->{_verbose} = 0;				# Level of verbosity
		
		bless($self, $class);
	    return $self;
	}
	# end new

	sub Log {
	    my($self, $text, $level) = @_;
	    # Output a blurb of diagnostic text, if the verbose level allows it.
		
		if($text) {
	        if($self->{_verbose} >= $level) {
				print $text."\n";
			}
	    }
	}
	# end Log

	sub Warn {  
	    my($self, $text) = @_;
	    # Output and count a warning.  Suppress duplicate warnings.
		
	    if($text) {
	        my $md5 = Digest::MD5->new();
			$md5->add($text);
			my $hash = $md5->digest();
	        if(!exists($self->{_warns_shown}->{$hash})) {
	            $self->{_warns_shown}->{$hash} = 1;
	            print '[WARNING] '.$text."\n";
	        }
	        else {
	            $self->Log('(suppressed) [WARNING] '.$text, 3);
	        }
	        $self->{num_warns}++;
	    }
	}
	# end Warn
	
	sub Error {  
	    my($self, $text) = @_;
	    # Output and count an error.  Suppress duplicate errors.
		
	    if($text) {
	        my $md5 = Digest::MD5->new();
			$md5->add($text);
			my $hash = $md5->digest();
	        if(!exists($self->{_errors_shown}->{$hash})) {
	            $self->{_errors_shown}->{$hash} = 1;
	            print '[ERROR] ' . $text . "\n";
	        }
	        else {
	            $self->Log('(suppressed) [ERROR] ' . $text, 3);
	        }
	        $self->{num_errors}++;
	    }
	}
	# end Error
	
	sub Fatal {  
	    my($self, $text) = @_;
	    # Output an error and terminate the program.
		
	    if($text) {
	        die '[FATAL] ' . $text . "\n";
	    }
	    else {
	        die "Fatal error.\n";
	    }
	}
	# end Fatal

    sub SetVerbose {  
		my ($self, $level) = @_;
		# Sets the verbose level.
		
		if (($level >= 0) && ($level <= 3)) {
			$self->{_verbose} = $level;
			return;
		}
		else {
			$self->Error("Verbose level $level must be between 0 and 3 inclusive.");
		}
	}
	#end SetVerbose
}
# end package Output

{package URL;
    # URL is a smart structure grouping together the properties we
    # care about for a single web reference. );
	my @__slots__ = ('loc', 'lastmod', 'changefreq', 'priority');
	
	sub new {  
		my $class = shift;
	    my $self = {};
	    
	    $self->{loc} 		= undef;		# URL -- in Narrow characters
	    $self->{lastmod} 	= undef;		# ISO8601 timestamp of last modify
	    $self->{changefreq} = undef;		# Text term for update frequency
	    $self->{priority} 	= undef;		# Float between 0 and 1 (inc)
		
		bless($self, $class);
	    return $self;
	}
	# end new

	sub __cmp__ {  
	    my($self, $other) = @_;
		
	    if($self->{loc} < $other->{loc}) {
	        return -1;
	    }
	    if($self->{loc} > $other->{loc}) {
	        return 1;
	    }
	    return 0;
	}	
	# end __cmp__
	
	sub TrySetAttribute {  
		my ($self, $attribute, $value) = @_;
		# Attempt to set the attribute to the value, with a pretty try
		# block around it.
		
		if($attribute eq 'loc') {
			$self->{loc} = $self->Canonicalize($value);
		}
		else {
			if(exists($self->{$attribute})) {
				$self->{$attribute} = $value;
			}
			else {
				$output->Warn("Unknown URL attribute: $attribute");
			}
		}
	}
	# end TrySetAttribute
	
	sub IsAbsolute {  
	    my($self, $loc) = @_;
	    #Decide if the URL is absolute or not 
		
		if(!$loc){
			return $False;
		}
		my $narrow = $loc;
		
		my ($volume, $directories, $file) = File::Spec->splitpath($narrow);
		my $scheme_pos = index($directories, "://");
		if($scheme_pos == -1) {
			return $False;
		}
		
		return $True;
	}
	# end IsAbsolute
	
	sub Canonicalize {
		my($self, $loc) = @_;
		# Do encoding and canonicalization on a URL string
		
		if(!$loc){
			return $loc;
		}	
		
		# Let the encoder try to narrow it
		my $narrow = $encoder->NarrowText($loc, undef);
		
		# Do URL escaping to URL components
		use URI::URL;
		my $url = new URI::URL $narrow;
		
		# Make IDNA encoding on the netloc
		my $netloc = URI::Escape::uri_unescape($url->netloc);
		my @netloc = split(//, $netloc);
		my $IDN_hostname = $False;
		foreach my $char (@netloc) {
			if($char ge chr(128)) {
				$IDN_hostname = $True;
				last;
			}
		}
		if($IDN_hostname) {
			eval "use IDNA::Punycode";
			if($@) {
				$output->Error("An International Domain Name (IDN) is being used. Perl Module IDNA::Punycode is required to encode this kind of hostnames, but it is not installed in your computer. See installation notes in README file.");
				return $loc;
			}
			my @hostname_labels = split(/\./, $netloc);
			$netloc = "";
			foreach my $label (@hostname_labels) {
				$label = IDNA::Punycode::encode_punycode($label);
				$netloc .= $label.".";
			}
			# Remove last '.' added
			$netloc = substr($netloc, 0, length($netloc) - 1);
		}
		$url->netloc($netloc);
		
		my $bad_netloc = $False;
		if(index($url->netloc, '%') != -1) {
			$bad_netloc = $True;
		}
		
		# Put it all back together
		if($netloc) {
			$narrow = $url->as_string;
		}
		
		# I let '%' through.  Fix any that aren't pre-existing escapes.
		my $HEXDIG = '0123456789abcdefABCDEF';
		my @list = split('%', $narrow);
		$narrow = shift(@list);
	    foreach my $item (@list){
			if((length($item) >= 2) && 
			   (index($HEXDIG, substr($item, 0, 1)) != -1) && 
			   (index($HEXDIG, substr($item, 1, 1)) != -1)) {
				$narrow = $narrow ."%". $item;
			}
			else {
				$narrow = $narrow ."%25". $item;
			}
		}	
		
		# Issue a warning if this is a bad URL
		if($bad_netloc){
			$output->Warn("Invalid characters in the host or domain portion of a URL: ". $narrow);
		}
		
		return $narrow;
	}
	# end Canonicalize
	
	sub Validate {  
		my($self, $base_url, $allow_fragment) = @_;
		#Verify the data in this URL is well-formed, and override if not. 
		
		# Test (and normalize) the ref
		if(!$self->{loc}) {
			$output->Warn("Empty URL");
			return $False;
		}
		if($allow_fragment) {
			my $endswith_slash = (substr($base_url, length($base_url) - 1) eq "/");
			my $startswith_slash = (substr($self->{loc}, 0, 1) eq "/");
			if($endswith_slash && $startswith_slash) {
				$self->{loc} = substr($self->{loc}, 1);
			}
			if(substr($self->{loc}, 0, length($base_url)) ne $base_url) {
				$self->{loc} = join "", $base_url, $self->{loc};
			}
		}
		if(substr($self->{loc}, 0, length($base_url)) ne $base_url) {
	        $output->Warn("Discarded URL for not starting with the base_url: " . $self->{loc});
	        $self->{loc} = undef;
	        return $False;
	    }
		
		# Test the lastmod
		if ($self->{lastmod}) {
			my $match = $False;
			$self->{lastmod} = uc($self->{lastmod});
			foreach my $pattern (@LASTMOD_PATTERNS){
				if($self->{lastmod} =~ /$pattern/) {
					$match = $True;
					last; 
				}
			}
			if(!$match) {
				$output->Warn( "Lastmod \"". $self->{lastmod}. "\" does not appear to be in ISO8601 format on URL: " . $self->{loc});
				$self->{lastmod} = undef;
			}
		}
		
		# Test the changefreq
		if($self->{changefreq}) {
			my $match = $False;
			$self->{changefreq} = lc($self->{changefreq});
			foreach my $pattern (@CHANGEFREQ_PATTERNS) {
				if($self->{changefreq} eq $pattern){
					$match = $True;
					last;
				}
			}
			if(!$match) {
				$output->Warn("Changefreq \"" . $self->{changefreq} . "\" is not a valid change frequency on URL: " . $self->{loc});
				$self->{changefreq} = undef;
			}
		}
		
		# Test the priority
		if($self->{priority}){
			my $priority = -1.0;
			my $test_priority = $self->{priority};
			
			if(!($test_priority =~ /^-?(?:\d+(?:\.\d*)?|\.\d+)$/)) {
					$output->Warn("Priority \"" . $self->{priority} . "\" is not a valid number inclusive on URL: " . $self->{loc});
				$self->{priority} = undef;
			}		
		    elsif(($self->{priority} < 0.0) || ($self->{priority} > 1.0)){
				$output->Warn("Priority \"" . $self->{priority} . "\" is not a number between 0 and 1 inclusive on URL: " . $self->{loc}); 
				$self->{priority} = undef;
			}
		}  
		
		return $True;
	}
	# end Validate
	
	sub MakeHash {  
		my($self) = @_;	
		# Provides a uniform way of hashing URLs
		
		if(!$self->{loc}) {
			return undef;
		}
		my $md5 = Digest::MD5->new();
		if(substr($self->{loc}, length($self->{loc}) - 1) eq '/') {
			$md5->add(substr($self->{loc}, 0, length($self->{loc}) - 1));
			return $md5->digest();
		}
		$md5->add($self->{loc});
		return $md5->digest();
	}
	# end MakeHash
	
	sub Log {  
		my($self, $prefix, $level) = @_;
		#Dump the contents, empty or not, to the log
		
		if(!$prefix) {
			$prefix = "URL";
		}
		if(!$level) {
			$level = 3;
		}
		
		my $out = $prefix . ':';
		
		foreach my $attribute (@__slots__){
			my $value = $self->{$attribute};
			if(!$value) {
				$value = '';
			}
			$out .= "  $attribute=[$value]";
		}
		$output->Log($out, $level);
	}
	# end Log
	
	sub WriteXML {
		my($self, $file, $is_gzip) = @_;
		
		if(!$self->{loc}){
			return;
		}
		my $out = $SITEURL_XML_PREFIX;
		
		foreach my $attribute (@__slots__){
			my $value = $self->{$attribute};
			if($value) {
				#Entity escaping if necessary. Other characters were escaped using URL escaping
				$value =~ s/&/&amp;/g;
				$out .= "  <$attribute>$value</$attribute>\n";
			}
		}
		$out = $out . $SITEURL_XML_SUFFIX;
		if($is_gzip) {
			eval "use Compress::Zlib";
			if($@) {
				$output->Error("Perl Module 'Compress::Zlib' required for compression/decompression.\nSee README installation notes.\n");
				return $False;
			}
			my $gz = $file;
			$gz->gzwrite($out);
		}
		else {
			print $file $out;
		}
	}
	# end WriteXML
}
# end package URL

{package Filter;
    # A filter on the stream of URLs we find.  A filter is, in essence,
    # a wildcard applied to the stream.  You can think of this as an
    # operator that returns a tri-state when given a URL:
	#
	#	(0) False -- this URL is to be dropped from the sitemap
    #	(1) True  -- this URL is to be included in the sitemap
	#	(2) None -- this URL is undecided
	  
	sub new {  
		my($class, $attributes) = @_;	
		my $self = {};
		bless($self, $class);
		
		$self->{_wildcard}	= undef;		# Pattern for wildcard match
		$self->{_regexp} 	= undef;		# Pattern for regexp match
		$self->{_pass} 		= $False;		# "Drop" filter vs. "Pass" filter
	
		my @attributes = keys %{$attributes};
		foreach my $attr (@attributes) {
			# Attributes have the format '{}pattern', '{}type', etc. Remove '{}' from them.
			$attr = substr($attr, 2); 
		}
		my @goodattributes = ('pattern', 'type', 'action');
		if(!::ValidateAttributes('FILTER', \@attributes, \@goodattributes)) {
			return;
		}
		
		# Check error count on the way in
		my $num_errors = $output->{num_errors};
		
		# Fetch the attributes
		my $pattern = $attributes->{'{}pattern'}->{Value};
		my $type = $attributes->{'{}type'}->{Value};
		if(!$type) {
			$type = 'wildcard';
		}
		my $action = $attributes->{'{}action'}->{Value};
		if(!$action) {
			$action = 'drop';
		}
		$type = lc($type);
		$action = lc($action);

		# Verify the attributes
		if(!$pattern) {
			$output->Error("On a filter you must specify a \"pattern\" to match");
		}
		elsif((!$type) || (($type ne 'wildcard') && ($type ne 'regexp'))) {
			$output->Error('On a filter you must specify either \'type = "wildcard"\' or \'type = "regexp"\'');
		}
		elsif(($action ne 'pass') && ($action ne 'drop')) {
			$output->Error('If you specify a filter action, it must be either \'action = "pass"\' or \'action = "drop"\'');
		}
	    
		# Set the rule
		if($action eq 'drop') {
			$self->{_pass} = $False;
		}
		elsif($action eq 'pass') {
			$self->{_pass} = $True;
		}
		
		if($type eq 'wildcard') {
			# Convert wildcard to regular expression
			my %patmap = (
		        '*' => '.*',
		        '?' => '.',
		        '[' => '[',
		        ']' => ']',
		    );
			my $glob = $pattern;
		    $glob =~ s{(.)} { $patmap{$1} || "\Q$1" }ge;
		    $self->{_wildcard} = '^'.$glob.'$';
		}
		elsif($type eq 'regexp') {
			eval {
				$self->{_regexp} = qr{$pattern};
			};
			if($@) {
				$output->Error("Bad regular expression: $pattern");
			}
		}

		# Log the final results if we didn't add any errors
		if($num_errors == $output->{num_errors}){
			$output->Log("Filter: $action any URL that matches $type \"$pattern\"", 2);
		}
		
		return $self;
	}
	# end new

	sub Apply {
		my($self, $url) = @_;
		# Process the URL, as above.
		
		if(!$url || !$url->{loc}) {
			return undef;
		}
				
		if($self->{_wildcard}) {
			if($url->{loc} =~ m/$self->{_wildcard}/) {
				return $self->{_pass};
			}
			return $None;
		}
		
		if($self->{_regexp}) {
			my $pattern = $self->{_regexp};
			if($url->{loc} =~ /$pattern/){
				return $self->{_pass};
			}
			return $None;
		}
		
		die if not $False;
	}
	# end Apply
}
# end package Filter

{package InputURL;
    # Each Input class knows how to yield a set of URLs from a data source.
	
    # This one handles a single URL, manually specified in the config file.

	sub new {  
	    my($class, $attributes) = @_;
	    my $self = {};
	    bless($self, $class);
		
	    $self->{_url} = undef;						# The lonely URL
		
		my @attributes = keys %{$attributes};
		foreach my $attr (@attributes) {
			# Attributes have the format '{}href', '{}lastmod', etc. Remove '{}' from them.
			$attr = substr($attr, 2); 
		}
		my @goodattributes = ('href', 'lastmod', 'changefreq', 'priority');
		if(!::ValidateAttributes('URL', \@attributes, \@goodattributes)) {
			return;
		}
			
		my $url = URL->new();
		foreach my $attr (@attributes){
			if($attr eq 'href') {
				$url->TrySetAttribute('loc', $attributes->{"{}".$attr}->{Value});
			}
			else { 
				$url->TrySetAttribute($attr, $attributes->{"{}".$attr}->{Value});
			}
		}
		  
		if(!$url->{loc}){
			$output->Error('Url entries must have href attribute.');
			return;
		}
		
		$self->{_url} = $url;
		$output->Log("Input From URL \"" . $self->{_url}->{loc} . "\"", 2);
		
		return $self;
	}
	# end new
		  
	sub ProduceURLs {  
	    my($self, $sitemap_obj, $consumer) = @_;
	    # Produces URLs from our data source, hands them in to the consumer.
		
	    if($self->{_url}) {
	        $sitemap_obj->$consumer($self->{_url}, $True);
	    }
	}
	# end ProduceURLs
}
# end package InputURL

{package InputURLList;
    # Each Input class knows how to yield a set of URLs from a data source.

    # This one handles a text file with a list of URLs
	  
	sub new {  
		my($class, $attributes) = @_;
		my $self = {};
		bless($self, $class);
		
		$self->{_path} = undef;
		$self->{_encoding} = undef;
	
		my @attributes = keys %{$attributes};
		foreach my $attr (@attributes) {
			# Attributes have the format '{}path', '{}encoding', etc. Remove '{}' from them.
			$attr = substr($attr, 2); 
		}
		my @goodattributes = ('path','encoding');
		if(!::ValidateAttributes('URLLIST', \@attributes, \@goodattributes)) {
			return;
		}
		
		$self->{_path}     = $attributes->{'{}path'};
		$self->{_encoding} = $attributes->{'{}encoding'};
		if(!$self->{_encoding}) {
			$self->{_encoding} = $ENC_UTF8;
		}
		if($self->{_path}){
			$self->{_path} = $encoder->MaybeNarrowPath($self->{_path});
			if(-e $self->{_path}) { 
				$output->Log("Input: From URLLIST \"". $self->{_path} . "\"", 2);
			}
			else{
				$output->Error("Can not locate file: " . $self->{_path});
				$self->{_path} = undef;
			}
		}
		else{
			$output->Error("Urllist entries must habe a \"path\" attribute");
		}
		
		return $self;
	}
	# end new
	
	sub ProduceURLs {
	    my($self, $sitemap_obj, $consumer) = @_;
	    # Produces URLs from our data source, hands them in to the consumer.
		
		# Open the file
		my $opened_file = ::OpenFileForRead($self->{_path}, 'URLLIST');
		if(!$opened_file) {
			return;
		}
		my $file = $opened_file->{file};
		
		# Iterate lines
		my @file = ();
		if(!$opened_file->{is_gzip}) {
			@file = <$file>;
		}
		else {
			eval "use Compress::Zlib";
			if($@) {
				$output->Error("Perl Module 'Compress::Zlib' required for compression/decompression.\nSee README installation notes.");
				return $False;
			}
			local *FILE = $file;
			my $gz = gzopen(*FILE, "rb");
			my $line;
			while($gz->gzreadline($line)) {
				push(@file, $line);
			}
			$gz->gzclose;
		}
		my $linenum = 0; 
		
		foreach my $line (@file){
			$linenum++;
			
			# Strip comments and empty lines
			$line = ::StripString($line);
		    if((!$line) || substr($line, 0, 1) eq '#') {
				next;
			}
			
			# Split the line on space
			my $url = URL->new();
			my @cols = split(/ /, $line);
			my $size = @cols;
			for(my $i = 0; $i < $size; $i++){
				$cols[$i] = ::StripString($cols[$i]);
			}
			$url->TrySetAttribute('loc', $cols[0]);
			
			# Extract attributes from the other columns
		    for(my $i = 1; $i < $size; $i++){
				if($cols[$i]) {
					my($attr_name, @attr_values);
					eval {
						($attr_name, @attr_values) = split('=', $cols[$i]);
						my $attr_val = shift @attr_values;
						my $attr_values_len = @attr_values;
						if($attr_values_len > 0) {
							foreach my $val (@attr_values) {
								$attr_val .= "=" . $val;
							}
						}
						$url->TrySetAttribute($attr_name, $attr_val);
					};
					if($@) {
						$output->Warn("Line $linenum: Unable to parse attribute: " . $cols[$i]);
					}
				}
			}
			
			# Pass it on
			$sitemap_obj->$consumer($url, $False);
		}
		
		close($file);
	}
	# end ProduceURLs
}
# end package InputURLList

{package InputDirectory;
    # Each Input class knows how to yield a set of URLs from a data source.

    # This one handles a directory that acts as base for walking the filesystem.
	  
	 sub new {  
	    my($class, $attributes, $base_url) = @_;
	    my $self = {};
	    bless($self, $class);
		
	    $self->{_path} 			= undef;		# The directory
	    $self->{_url} 			= undef;		# The URL equivalent
	    $self->{_default_file} 	= undef;
				
		my @attributes = keys %{$attributes};
		foreach my $attr (@attributes) {
			# Attributes have the format '{}path', '{}url', etc. Remove '{}' from them.
			$attr = substr($attr, 2); 
		}
		my @goodattributes = ('path', 'url', 'default_file');
		if(!::ValidateAttributes('DIRECTORY', \@attributes, \@goodattributes)) {
			return;
		}		
		
		# Prep the path -- it MUST end in a sep
		my $path = $attributes->{"{}path"}->{Value};
		if(!$path){
			$output->Error('Directory entries must have both "path" and "url" atributes');
			return;
		}
		$path = $encoder->MaybeNarrowPath($path);
		my $os_sep;
		if($^O eq "MSWin32") {
			$os_sep = "\\";
		}
		elsif($^O eq "darwin") {
			$os_sep = ":";
		}
		else {
			$os_sep = "/";
		}
		
		if(substr($path, length($path) - 1) ne $os_sep){
			$path .= $os_sep;
		}
		if(!(-d $path)){
			$output->Error("Can not locate directory: " . $path);
			return;
		}
		 
		# Prep the URL -- it MUST end in a sep
		my $url = $attributes->{"{}url"}->{Value};
		if(!$url){
			$output->Error('Directory entries must have both "path" and "url" attributes');
			return;
		}
		$url = URL->Canonicalize($url);
		if(substr($url, length($url) - 1) ne '/'){
			$url .= '/';
		}
		if(substr($url, 0, length($base_url)) ne $base_url) {
			if(substr($url, 0, length($base_url)) ne $base_url) {
				$output->Error("The directory URL \"" . $url . "\" is not relative to the base_url: " . $base_url);
				return;
			}
		}
		
		# Prep the default file -- it MUST be just a filename
		my $file = $attributes->{"{}default_file"}->{Value};
		if($file) {
			$file = $encoder->MaybeNarrowPath($file);
			if(index($file, $os_sep) != -1) {
				$output->Error("The default_file \"" . $file . "\" can not include path information.");
				$file = undef;
			}
		}
		
		$self->{_path} = $path;
		$self->{_url} = $url;
		$self->{_default_file} = $file;
		
		if($file) {
			$output->Log("Input: From DIRECTORY \"$path\" ($url) with default file \"$file\"", 2);
		}
		else {
			$output->Log("Input: From DIRECTORY \"$path\" ($url) with no default file", 2);
		}
		
		return $self;
	}
	# end new
	
	sub ProduceURLs {  
		our($self, $sitemap_obj, $consumer) = @_;
		# Produces URLs from our data source, hands them in to the consumer.
			
	    if(!$self->{_path}) {
	        return;
	    }
	    our $root_path = $self->{_path};
	    our $root_URL = $self->{_url};
	    our $root_file = $self->{_default_file};
		
		sub PerFile {
			# Called once per file
			
			# Pull a timestamp
			my $url = URL->new();
			my $isdir = $False;	
			my $time = undef;	
			my $dirpath = $File::Find::dir;
			my $name = $_;
			my $path = undef;
			eval {
				if($name ne '.') {
					$path = join '/', $dirpath, $name;
				}
				else {
					$path = $dirpath;
				}
				$isdir = -d $path;
				
				if($isdir) { 
					if(!PerDirectory($dirpath)) {
						return;
					}
				}
				
				if($isdir && $root_file) {
					my $file = join '/', $path, $root_file;
					eval {
						$time = (stat($file))[9];
					};
					if($@) {
					}
				}
				if(!$time) {
					$time = (stat($path))[9];
				}
				$url->{lastmod} = ::TimestampISO8601($time);
			};
			if($@) {
			}
			
			# Build a URL
			my $middle = '';
			if(length($path) > length($root_path)) {
				my $is_win = $^O eq "MSWin32";
				$middle = substr($path, length($root_path) + $is_win);
			}
			my $os_sep;
			if($^O eq "MSWin32") {
				$os_sep = "\\";
			}
			elsif($^O eq "darwin") {
				$os_sep = ":";
			}
			else {
				$os_sep = "/";
			}
			if($os_sep ne '/') {
				$middle =~ tr|:|/|;
				$middle =~ tr|\\|/|;
			}
			if($isdir && $name ne '.'){
				$middle .= '/';
			}
			$url->TrySetAttribute('loc', $root_URL . $middle);
			
			# Suppress default files.  (All the way down here so we can log it.)
			if(($name ne '.') && ($root_file eq $name)){
				$url->Log('IGNORED(default file)', 2);
				return;
			}
			
			$sitemap_obj->$consumer($url, $False);
		}
		# end PerFile
		
		sub PerDirectory {
			my($dirpath) = @_;
			# Called once per directory
			
			my $os_sep;
			if($^O eq "MSWin32") {
				$os_sep = "\\";
			}
			elsif($^O eq "darwin") {
				$os_sep = ":";
			}
			else {
				$os_sep = "/";
			}
			
			if(substr($dirpath, length($dirpath) - 1) ne $os_sep){
				$dirpath .= $os_sep;
			}
			if(substr($dirpath, 0, length($root_path)) ne $root_path) {
				$output->Warn('Unable to decide what the root path is for directory ' . $dirpath);
				return $False;
			}
			return $True;
		}
		# end PerDirectory
		
		$output->Log("Walking DIRECTORY \"" . $self->{_path} . "\"", 1);
		::find(\&PerFile, $self->{_path});
	}
	# end ProduceURLs
}
# end package InputDirectory	

{package InputAccessLog;
    # Each Input class knows how to yield a set of URLs from a data source.

    # This one handles access logs.  It's non-trivial in that we want to
    # auto-detect log files in the Common Logfile Format (as used by Apache,
    # for instance) and the Extended Log File Format (as used by IIS, for
    # instance).
	  
	sub new {  
	    my($class, $attributes) = @_;
	    my $self = {};
	    bless($self, $class);
		
	    $self->{_path} 			= undef;		# The file path
	    $self->{_encoding} 		= undef;		# Encoding of that file
	    $self->{_is_elf} 		= $False;		# Extended Log File Format?
	    $self->{_is_clf} 		= $False;		# Common Logfile Format?
	    $self->{_elf_status} 	= -1;			# ELF field: '200'
	    $self->{_elf_method} 	= -1;			# ELF field: 'HEAD'
	    $self->{_elf_uri} 		= -1;			# ELF field: '/foo?bar=1'
	    $self->{_elf_urifrag1} 	= -1;			# ELF field: '/foo'
	    $self->{_elf_urifrag2} 	= -1;			# ELF field: 'bar=1'
	    		
		my @attributes = keys %{$attributes};
		foreach my $attr (@attributes) {
			# Attributes have the format '{}path', '{}encoding', etc. Remove '{}' from them.
			$attr = substr($attr, 2); 
		}
		my @goodattributes = ('path', 'encoding');
		if(!::ValidateAttributes('ACCESSLOG', \@attributes, \@goodattributes)) {
			return;
		}
		
		$self->{_path} = $attributes->{'{}path'};
		$self->{_encoding} = $attributes->{'{}encoding'};
		if(!$self->{_encoding}) {
			$self->{_encoding} = $ENC_UTF8;
		}

	    if($self->{_path}) {
		    $self->{_path} = $encoder->MaybeNarrowPath($self->{_path});
		    if(-e $self->{_path}) {
				$output->Log("Input: From ACCESSLOG \"" .  $self->{_path} . "\"", 2);
			}
		    else {
				$output->Error("Can not locate file: " . $self->{_path});
				$self->{_path} = undef;
			}
		}
	    else {
			$output->Error("Accesslog entries must have a \"path\" attribute.");
		}
		
		return $self;
	} 
	# end new
	
	sub RecognizeELFLine {  
		my($self, $line) = @_;
		# Recognize the Fields directive that heads an ELF file
		
		if(substr($line, 0, 8) ne '#Fields:'){
			return $False;
		}
		my @fields = split(' ', $line);
	    shift(@fields);
		my $fields_len = @fields;
		
	    for(my $i = 0; $i < $fields_len; $i++){
			my $field = $fields[$i];
			
			# Strip field
			$field = ::StripString($field);
			
			if ($field eq 'sc-status'){
				$self->{_elf_status} = $i;
			}
			elsif ($field eq 'cs-method'){
				$self->{_elf_method} = $i;
			}
			elsif ($field eq 'cs-uri'){
				$self->{_elf_uri} = $i;
			}
			elsif ($field eq 'cs-uri-stem'){
				$self->{_elf_urifrag1} = $i;
			}
			elsif ($field eq 'uri-query'){
				$self->{_elf_urifrag2} = $i;
			}
		}
		$output->Log('Recognized an Extended Log File Format file.', 2);
		return $True;
	}
	# end RecognizeELFLine

	sub GetELFLine{  
		my($self, $line) = @_;
		# Fetch the requested URL from an ELF line 
		
		my @fields = split(' ', $line);
		my $count = @fields;
		
		# Strip fields
		for(my $i = 0; $i < $count; $i++) {
			$fields[$i] = ::StripString($fields[$i]);
		}
		
		# Verify status was Ok
		if($self->{_elf_status} >= 0){
			if($self->{_elf_status} >= $count){
				return undef;
			}
			my $field_strip_status = $fields[$self->{_elf_status}];
			if($field_strip_status ne '200'){
				return undef;
			}
		}
		
	    # Verify method was HEAD or GET
		if($self->{_elf_method} >= 0){
			if($self->{_elf_method} >= $count){
				return undef;
			}
			my $field_strip_method = $fields[$self->{_elf_method}];
			if(($field_strip_method ne 'HEAD') && ($field_strip_method ne 'GET')){
				return undef;
			}
		}
		
		# Pull the full URL if we can
		if($self->{_elf_uri} >= 0){
			if($self->{_elf_uri} >= $count){
				return undef;
			}
			my $url = $fields[$self->{_elf_uri}];
			if($url ne '-'){
				return $url;
			}
		}
	    
		# Put together a fragmentary URL
		if($self->{_elf_urifrag1} >= 0){
			if(($self->{_elf_urifrag1} >= $count) || ($self->{_elf_urifrag2} >= $count)){
				return undef;
			}
			my $urlfrag1 = $fields[$self->{_elf_urifrag1}];
			my $urlfrag2 = undef;
			if($self->{_elf_urifrag2} >= 0){
				$urlfrag2 = $fields[$self->{_elf_urifrag2}];
			}
			if($urlfrag1 && ($urlfrag1 ne '-')){
				if($urlfrag2 && ($urlfrag2 ne '-')){
					$urlfrag1 .= '?' . $urlfrag2;
				}
				return $urlfrag1;
			}
		}
		return undef;
	}
	# end GetELFLine
	
	sub RecognizeCLFLine{  
		my($self, $line) = @_;
		# Try to tokenize a log file line according to CLF pattern and see if it works
		
		my $recognize = $False;
		$_ = $line;
		
		if(/$ACCESSLOG_CLF_PATTERN/) {
			$recognize = (($1 eq 'HEAD') || ($1 eq 'GET'));
			if($recognize){
				$output->Log('Recognized a Common Logfile Format file', 2);
			}
		}
		
		return $recognize;
	}
	# end RecognizeCLFLine
	
	sub GetCLFLine{  
		my($self, $line) = @_;
		# Fetch the requested URL from a CLf line
		
		my $recognize = $False;
		$_ = $line;
		
		if(/$ACCESSLOG_CLF_PATTERN/) {
			my $request = $1;
		    if(($request eq 'HEAD') || ($request eq 'GET')) {
				return $2;
			}
		}
		return undef;
	}
	# end GetCLFLine
		
	sub ProduceURLs {  
		my($self, $sitemap_obj, $consumer) = @_;
		# Produces URLs from our data source, hands them in to the consumer.
		
		# Open the file
		my $opened_file = ::OpenFileForRead($self->{_path}, 'ACCESSLOG');
		if(!$opened_file) {
			return;
		}
		my $file = $opened_file->{file};
		
		# Iterate lines
		my @file = ();
		if(!$opened_file->{is_gzip}) {
			@file = <$file>;
		}
		else {
			eval "use Compress::Zlib";
			if($@) {
				$output->Error("Perl Module 'Compress::Zlib' required for compression/decompression.\nSee README installation notes.");
				return $False;
			}
			local *FILE = $file;
			my $gz = gzopen(*FILE, "rb");
			my $line;
			while($gz->gzreadline($line)) {
				push(@file, $line);
			}
			$gz->gzclose;
		}
		my $linenum = 0;
		
		foreach my $line (@file){
			$linenum++;
		
			$line = ::StripString($line);
			
			# If we don't know the format yet, try them both
			if((!$self->{_is_clf}) && (!$self->{_is_elf})){
				$self->{_is_elf} = $self->RecognizeELFLine($line);
				$self->{_is_clf} = $self->RecognizeCLFLine($line);		
			}
			
			# Digest the line
			my $match = undef;
			if($self->{_is_elf}){
				$match = $self->GetELFLine($line);
			}
			elsif($self->{_is_clf}){
				$match = $self->GetCLFLine($line);
			}
			if(!$match){
				next;
			}
			
			# Pass it on
			my $url = URL->new();
			$url->TrySetAttribute('loc', $match);
			$sitemap_obj->$consumer($url, $True);
		}
		
		close($file);
	}
	# end ProduceURLs
}
# end package InputAccessLog

{package InputSitemap;
	# Each Input class knows how to yield a set of URLs from a data source.

    # This one handles Sitemap files and Sitemap index files.  For the sake
    # of simplicity in design (and simplicity in interfacing with the SAX
    # package), we do not handle these at the same time, recursively.  Instead
    # we read an index file completely and make a list of Sitemap files, then
    # go back and process each Sitemap.
    
	{package _ContextBase;
	    # Base class for context handlers in our SAX processing.  A context
	    # handler is a class that is responsible for understanding one level of
	    # depth in the XML schema.  The class knows what sub-tags are allowed,
	    # and doing any processing specific for the tag we're in.

	    # This base class is the API filled in by specific context handlers,
	    # all defined below.
	    
	    sub new {
			my($class, $subtags) = @_;
			$class = ref($class) || $class;
			my $self = {};
			bless($self, $class);
			# Initialize with a sequence of the sub-tags that would be valid in
			# this context.
			
			$self->{_allowed_tags} = $subtags;          # Sequence of sub-tags we can have
			$self->{_last_tag}     = undef;             # Most recent seen sub-tag
			
			return $self;
		}
	    # end new
		
		sub AcceptTag {
			my($self, $tag) = @_;
		    # Returns True if opening a sub-tag is valid in this context.
			
			my $valid = $False;
			foreach my $allowed_tag (@{$self->{_allowed_tags}}) {
				if($tag eq $allowed_tag) {
					$valid = $True;
					last;
				}
			}
			if($valid) {
				$self->{_last_tag} = $tag;
			}
			else {
				$self->{_last_tag} = undef;
			}
			return $valid;
		}
	    # end AcceptTag
		
		sub AcceptText {
			my($self, $text) = @_;
			# Returns True if a blurb of text is valid in this context.
			return $False;
		}
	    # end AcceptText
		
		sub Open {
			my($self) = @_;
			# The context is opening.  Do initialization.
		}
		# end Open
		
		sub Close {
			my($self) = @_;
			# The context is closing.  Return our result, if any.
		}
		# end Close
		
		sub Return {
			my($self, $result) = @_;
			# We're returning to this context after handling a sub-tag.  This
			# method is called with the result data from the sub-tag that just
			# closed.  Here in _ContextBase, if we ever see a result it means
			# the derived child class forgot to override this method.
			if($result) {
				#raise NotImplementedError
			}
		}
	    # end Return
	}
	# end package _ContextBase
	
	{package _ContextUrlSet;
		use base '_ContextBase';
		# Context handler for the document node in a Sitemap.
    
		sub new {
			my($class) = @_;
			
			my @subtags = ('url');
			my $self = $class->SUPER::new(\@subtags);
			
			bless($self, $class);
			return $self;
		}
		# end new
	}
	#end class _ContextUrlSet
	
	{package _ContextUrl;
		use base '_ContextBase';
		# Context handler for a URL node in a Sitemap.
		my @URL__slots__ = ('loc', 'lastmod', 'changefreq', 'priority');
		
		sub new {
			my($class, $sitemap_obj, $consumer) = @_;
			# Initialize this context handler with the callable consumer that
			# wants our URLs.
			
			my $self = $class->SUPER::new(\@URL__slots__);
			$self->{_url}			= undef;            # The URL object we're building
			$self->{_sitemap_obj}	= $sitemap_obj;		# The sitemap object to call the consumer	
			$self->{_consumer}		= $consumer;        # Who wants to consume it
			
			bless($self, $class);
			return $self;
		}
		# end new
		
		sub Open {
			my($self) = @_;
			# Initialize the URL.
			
			die if $self->{_url};
			$self->{_url} = URL->new();
		}
		# end Open
		
		sub Close {
			my($self) = @_;
			# Pass the URL to the consumer and reset it to undef.
			
			die if not $self->{_url};
			my $sitemap_obj = $self->{_sitemap_obj};
			my $consumer = $self->{_consumer};
			$sitemap_obj->$consumer($self->{_url}, $False);
			$self->{_url} = undef;
		}
		# end Close
		
		sub Return {
			my($self, $result) = @_;
			# A value context has closed, absorb the data it gave us.
			die if not $self->{_url};
			if($result) {
				$self->{_url}->TrySetAttribute($self->{_last_tag}, $result);
			}
		}
	    # end Return
	}
	# end package _ContextUrl
	
	{package _ContextSitemapIndex;
		use base '_ContextBase';
		# Context handler for the document node in an index file.
		
		sub new {
			my($class) = @_;
			
			my @subtags = ('sitemap',);
			my $self = $class->SUPER::new(\@subtags);
			$self->{_loclist} = ();                    # List of accumulated Sitemap URLs
			
			bless($self, $class);
			return $self;
		}
		# end new
		
		sub Open {
			my($self) = @_;
			# Just a quick verify of state.
			die if $self->{_loclist};
		}
		# end Open
		
		sub Close {
			my($self) = @_;
			# Return our list of accumulated URLs.
			
			if($self->{_loclist}) {
				my @temp = @{$self->{_loclist}};
				$self->{_loclist} = ();
				return @temp;
			}
		}
		# end Close
		
		sub Return {
			my($self, $result) = @_;
			# Getting a new loc URL, add it to the collection.
			
			if($result) {
				push(@{$self->{_loclist}}, $result);
			}
		}
	    # end Return
	}
	# end package _ContextSitemapIndex
	
	{package _ContextSitemap;
		use base '_ContextBase';
		# Context handler for a Sitemap entry in an index file.
		
		sub new {
			my($class) = @_;
	      
			my @subtags = ('loc', 'lastmod');
			my $self = $class->SUPER::new(\@subtags);
			$self->{_loc} = undef;			# The URL to the Sitemap
			
			bless($self, $class);
			return $self;
		}
	    # end new
		
		sub Open {
			my($self) = @_;
			# Just a quick verify of state.
			die if $self->{_loc};
		}
	    # end Open
		
		sub Close {
			my($self) = @_;
			# Return our URL to our parent.
			
			if($self->{_loc}) {
				my $temp = $self->{_loc};
				$self->{_loc} = undef;
				return $temp;
			}
			$output->Warn("In the Sitemap index file, a \"sitemap\" entry had no \"loc\".");
		}
	    # end Close
		
		sub Return {
			my($self, $result) = @_;
			# A value has closed.  If it was a 'loc', absorb it.
			if($result && ($self->{_last_tag} eq 'loc')) {
				$self->{_loc} = $result;
			}
		}
	    # end Return
	}
	# end package _ContextSitemap
	
	{package _ContextValue;
		use base '_ContextBase';
		# Context handler for a single value.  We return just the value.  The
		# higher level context has to remember what tag led into us.
		
		sub new {
			my($class) = @_;
			
			my @subtags = ();
			my $self = $class->SUPER::new(\@subtags);
			$self->{_text} = undef;
		  
			bless($self, $class);
			return $self;
		}
	    # end new
		
		sub AcceptText {
			my($self, $text) = @_;
			# Allow all text, adding it to our buffer.
			
			if($self->{_text}) {
				$self->{_text} .= $text;
			}
			else {
				$self->{_text} = $text;
			}
			return $True;
		}
		# end AcceptText

		sub Open {
			my($self) = @_;
			# Initialize our buffer.
			$self->{_text} = undef;
		}
		# end Open
		
		sub Close {
			my($self) = @_;
			# Return what's in our buffer.
			my $text = $self->{_text};
			$self->{_text} = undef;
			if($text) {
				# Remove spaces
				while(substr($text, 0, 1) eq ' ') {
					$text = substr($text, 1);
				}
				while(substr($text, length($text) - 1, 1) eq ' ') {
					$text = substr($text, 0, length($text) - 1);
				}
			}
			return $text;
		}
		# end Close
	}
	# end package _ContextValue
	
	sub new {  
        my($class, $attributes) = @_;
        my $self = {};
		bless($self, $class);
        # Initialize with a hash of attributes from our entry in the config file.
				
        $self->{_pathlist} = undef;			# A list of files
        $self->{_current} = -1;				# Current context in _contexts
        $self->{_contexts} = undef;			# The stack of contexts we allow
        $self->{_contexts_idx} = undef;		# ...contexts for index files
        $self->{_contexts_stm} = undef;		# ...contexts for Sitemap files
		
		my @attributes = keys %{$attributes};
		foreach my $attr (@attributes) {
			# Attributes have the format '{}path'. Remove '{}' from them.
			$attr = substr($attr, 2); 
		}
		my @goodattributes = ('path');
        if(!::ValidateAttributes('SITEMAP', \@attributes, \@goodattributes)) {
            return;
        }
		
		# Init the first file path
        my $path = $attributes->{'{}path'};
        if($path) {
            $path = $encoder->MaybeNarrowPath($path);
            if(-e $path) { 
				$output->Log("Input: From SITEMAP \"$path\"", 2);
                @{$self->{_pathlist}} = ($path);
            }
            else {
				$output->Error("Can not locate file $path");
            }
        }
        else {
			$output->Error("Sitemap entries must have a \"path\" attribute.");
        }
		
        return $self;
    }
	#end new
	
	sub ProduceURLs {  
        my($self, $sitemap_obj, $consumer) = @_;
        # In general: Produces URLs from our data source, hand them to the callable consumer.

	    # In specific: Iterate over our list of paths and delegate the actual
	    # processing to helper methods.  This is a complexity no other data source
	    # needs to suffer.  We are unique in that we can have files that tell us
	    # to bring in other files.

	    # Note the decision to allow an index file or not is made in this method.
	    # If we call our parser with (self._contexts == None) the parser will
	    # grab whichever context stack can handle the file.  IE: index is allowed.
	    # If instead we set (self._contexts = ...) before parsing, the parser
	    # will only use the stack we specify.  IE: index not allowed.
		
		# Set up two stacks of contexts
        @{$self->{_contexts_idx}} = (_ContextSitemapIndex->new(), 
									_ContextSitemap->new(), 
									_ContextValue->new());
        @{$self->{_contexts_stm}} = (_ContextUrlSet->new(), 
									_ContextUrl->new($sitemap_obj, $consumer), 
									_ContextValue->new());
		
		# Process the first file							
        die if not @{$self->{_pathlist}};
        my $path = $self->{_pathlist}->[0];
        $self->{_contexts} = undef;							# We allow an index file here
        $self->_ProcessFile($path);
		
		# Iterate over remaining files
        @{$self->{_contexts}} = @{$self->{_contexts_stm}};	# No index files allowed
		my @pathlist = @{$self->{_pathlist}};
		shift @pathlist;
        foreach my $path (@pathlist) {
			$self->_ProcessFile($path);
        }
    }
	#end ProduceURLs
	
	sub _ProcessFile { 
	    my($self, $path) = @_;
	    # Do per-file reading/parsing/consuming for the file path passed in.
		
	    die if not $path;
		
		# Open our file
		my $opened_file = ::OpenFileForRead($path, 'SITEMAP');
		if(!$opened_file) {
			return;
		}
		my $file = $opened_file->{file};
		
		if($opened_file->{is_gzip}) {
			eval "use Compress::Zlib";
			if($@) {
				$output->Error("Perl Module 'Compress::Zlib' required for compression/decompression.\nSee README installation notes.");
				return $False;
			}
			$path = substr($path, 0, length($path) - 3);
			open OUTPUT, "> $path";
			
			local *FILE = $file;
			my $gz = gzopen(*FILE, "rb");
			
			my($line, $out) = ("", "");
			while($gz->gzreadline($line)) {
				$line =~ s/\n+$//;
				$line =~ s/\r+$//;
				$out .= $line;
			}
			print OUTPUT $out;
			
			close $file;
			close OUTPUT;
		}
		
		# Rev up the SAX engine
	    eval {
			$self->{_current} = -1;
			my $xml_parser = XML::SAX::ParserFactory->parser( ContentHandler => $self );
			$xml_parser->parse_uri($path);
		};
		if($@) {
			if(index($@, 'LWP Request Failed') != -1 || index($@, 'Permission denied') != -1) {
				$output->Error("Cannot read from file $path");
			}
			else {
				$@ =~ s/\n+$//;
				$@ =~ s/\r+$//;
				$output->Error("XML error in the file $path: $@");
			}
		}
		
		# Clean up
		close($file);	
		if($opened_file->{is_gzip}) {
			unlink($path);
		}
	}
	#end  _ProcessFile
	
	sub _MungeLocationListIntoFiles { 
	    my($self, @urllist) = @_;
	    # Given a list of URLs, munge them into our self._pathlist property.
	    # We do this by assuming all the files live in the same directory as
	    # the first file in the existing pathlist.  That is, we assume a
	    # Sitemap index points to Sitemaps only in the same directory.  This
	    # is not true in general, but will be true for any output produced
	    # by this script.
	    
	    die if not @{$self->{_pathlist}};
	    my $path = $self->{_pathlist}->[0];
	    $path = File::Spec->canonpath($path);
		my($volume, $dir, $ignore);
		($volume, $dir, $ignore) = File::Spec->splitpath($path);
		
		foreach my $url (@urllist) {
            $url = URL->Canonicalize($url);
            $output->Log("Index points to Sitemap file at: $url", 2);
			my $file;
			($ignore, $ignore, $file) = File::Spec->splitpath($url);
            if($dir) {
                $file = File::Spec->catpath($volume, $dir, $file);
            }
            if($file) {
                push(@{$self->{_pathlist}}, $file);
                $output->Log("Will attempt to read Sitemap file: $file", 1);
            }
        }
	}
	#end _MungeLocationListIntoFiles
	
	sub start_element {  
	    my($self, $element) = @_;
	    # SAX processing, called per node in the config stream.
	    # As long as the new tag is legal in our current context, this
	    # becomes an Open call on one context deeper.
	    		
		# If this is the document node, we may have to look for a context stack
	    if(($self->{_current} < 0) && !$self->{_contexts}) {
		    die if not($self->{_contexts_idx} && $self->{_contexts_stm});
		    if($element->{Name} eq 'urlset') {
				@{$self->{_contexts}} = @{$self->{_contexts_stm}};
			}
		    elsif($element->{Name} eq 'sitemapindex') {
				@{$self->{_contexts}} = @{$self->{_contexts_idx}};
				$output->Log("File is a Sitemap index.", 2);
			}
		    else {
				$output->Error("The document appears to be neither a Sitemap nor a Sitemap index.");
			}
		}
		
		# Compare stacks
		my $is_idx = $False;
		my $is_stm = $False;
		if($self->{_contexts} && (exists($self->{_contexts}->[1]->{_loc}))) {
			$is_idx = $True;
		}
		if($self->{_contexts} && (exists($self->{_contexts}->[1]->{_url}))) {
			$is_stm = $True;
		}
		
		# Display a kinder error on a common mistake
	    if(($self->{_current} < 0) && $is_stm && ($element->{Name} eq 'sitemapindex')) {
			$output->Error("A Sitemap index can not refer to another Sitemap index.");
		}
		
		# Normalize hash of attributes
		my %attributes;
		foreach my $attr (keys %{$element->{Attributes}}) {
			$attributes{$attr} = $element->{Attributes}->{$attr}->{Value};
		}
		
		# Verify no unexpected attributes
	    if($element->{Attributes}) {
		    my $text = "";
		    foreach my $attr (keys %attributes) {
				# The document node will probably have namespaces
			    if($self->{_current} < 0) {
					if(index($attr, 'xmlns') >= 0) {
						next;
					}
					if(index($attr, 'xsi') >= 0) {
						next;
					}
					if($element->{Attributes}->{$attr}->{NamespaceURI}) {
						next;
					}
				}
			    if($text) {
					$text .= ', ';
				}
				$text .= $attr;
			}
		    if($text) {
				$output->Warn("Did not expect any attributes on any tag, instead tag \"" . $element->{Name} . "\" had attributes: $text");
			}
		}
		
		#  contexts
	    if(($self->{_current} < 0) || ($self->{_contexts}->[$self->{_current}]->AcceptTag($element->{Name}))) {
		    $self->{_current}++;
			my $contexts_len = @{$self->{_contexts}};
		    die if not($self->{_current} < $contexts_len);
		    $self->{_contexts}->[$self->{_current}]->Open();
		}
	    else {
			$output->Error("Can not accept tag \"" . $element->{Name} . "\" where it appears.");
		}
	}
	#end start_element
	
	sub end_element {  
	    my($self, $element) = @_;
	    # SAX processing, called per node in the config stream.
	    # This becomes a call to Close on one context followed by a call
	    # to Return on the previous.
		
	    die if not $self->{_current} >= 0;
		
		# Compare stacks
		my $is_idx = $False;
		my $is_stm = $False;
		if($self->{_contexts} && (exists($self->{_contexts}->[1]->{_loc}))) {
			$is_idx = $True;
		}
		elsif($self->{_contexts} && (exists($self->{_contexts}->[1]->{_url}))) {
			$is_stm = $True;
		}
		
		my $retval = undef; 
		my @retval = undef;
		if($is_idx && $self->{_current} == 0) {
			@retval = $self->{_contexts}->[$self->{_current}]->Close();
		}
		else {
			$retval = $self->{_contexts}->[$self->{_current}]->Close();
		}
	    $self->{_current}--;
		
	    if($self->{_current} >= 0) {
			$self->{_contexts}->[$self->{_current}]->Return($retval);
		}
	    elsif($retval && ($is_idx)) {
			$self->_MungeLocationListIntoFiles($retval);	
		}
		elsif(@retval && ($is_idx)) {
			$self->_MungeLocationListIntoFiles(@retval);	
		}
	}
	#end end_element
	
	sub characters {  
	    my($self, $characters) = @_;
	    # SAX processing, called when text values are read.  Important to
	    # note that one single text value may be split across multiple calls
	    # of this method.
		if(($self->{_current} < 0) || 
		   (!$self->{_contexts}->[$self->{_current}]->AcceptText($characters->{Data}))) {
		   
		    # Strip text
		    my $text_strip = $characters->{Data};
			$text_strip = ::StripString($text_strip);
			
			if($text_strip) {
				$output->Error("Can not accept text \"" . $characters->{Data} . "\" where it appears.");
			}
		}
	}
	#end characters
}
# end package InputSitemap

{package FilePathGenerator;
    #  This class generates filenames in a series, upon request.
    #  You can request any iteration number at any time, you don't
    #  have to go in order.

    #  Example of iterations for '/path/foo.xml.gz':
    #    0           --> /path/foo.xml.gz
    #    1           --> /path/foo1.xml.gz
    #    2           --> /path/foo2.xml.gz
    #    _index.xml  --> /path/foo_index.xml
  
	sub new {  
		my $class = shift;
        my $self = {};
        $self->{is_gzip} 	= 0; 				# Is this a  GZIP file?
        $self->{_path} 		= undef;			# '/path/'
		$self->{_abs_path}  = undef;			# absolute form of 'path'
        $self->{_prefix} 	= undef;			# 'foo'
        $self->{_suffix} 	= undef;			# '.xml.gz'
		bless($self, $class);
        return $self;
    }
	#end new
	
	sub Preload {  
	    my($self, $path) = @_;
	    # Splits up a path into forms ready for recombination.
		
	    $path = $encoder->MaybeNarrowPath($path);
		
		# Get down to a base name
	    $path = File::Spec->canonpath($path);
		my($volume,$directories,$base) = File::Spec->splitpath($path);
	    if(!$base) {
			$output->Error("Couldn\'t parse the file path: $path");
	        return $False;
	    }
	    my $lenbase = length($base);
		
		# Get absolute form for the given path -- which is relative to the config file
		my $absolute = $path;
		if(!File::Spec->file_name_is_absolute($path)){
			my($volume, $abspath, $ignore) = File::Spec->splitpath(File::Spec->rel2abs($flags{'config'}));
			$directories = $abspath.$directories;
			$absolute = File::Spec->catpath($volume, $directories, $base);
		}
	    $self->{_abs_path} = $absolute;
		
		# Recognize extension
		my $lensuffix = 0;
		my @compare_suffix = ('.xml', '.xml.gz', '.gz');
		foreach my $suffix (@compare_suffix) {
			# if base ends with suffix:
			if(substr($base, length($base) - length($suffix), length($suffix)) eq $suffix) {
				$lensuffix = length($suffix);
				last;
			}
		}
	    if(!$lensuffix) {
			$output->Error("The path $path doesn\'t end in a supported file extension.");
			return $False;
		}
	    
		# Find out if base ends with '.gz':
	    $self->{is_gzip} = substr($base, length($base) - 3) eq '.gz';
	
		# Split the original path
		my $lenpath        = length($path);
        $self->{_path}     = substr($path, 0, $lenpath - $lenbase);
        $self->{_prefix}   = substr($path, $lenpath - $lenbase, $lenbase - $lensuffix);
        $self->{_suffix}   = substr($path, $lenpath - $lensuffix);
	
		return $True;
	}
	#end Preload
	
	sub GeneratePath {  
	    my($self, $instance) = @_;
	    # Generates the iterations, as described above.
		
	    my $prefix = $self->{_path} . $self->{_prefix};
		if($instance =~ /^\d+$/) { 
			if($instance) {
				return $prefix . $instance . $self->{_suffix};
			}
	        return $prefix . $self->{_suffix};
	    }
	    return $prefix . $instance;
	}
	#end GeneratePath
	
	sub AbsolutePath {  
	    my($self, $path) = @_;
	    # Gets the absolute form of a path that is relative to the config file.
		
		my($volume,$directories,$base) = File::Spec->splitpath($path);
		if(!File::Spec->file_name_is_absolute($path)){
			my($volume, $abspath, $ignore) = File::Spec->splitpath($self->{_abs_path});
			$directories = $abspath.$directories;
			$path = File::Spec->catpath($volume, $directories, $base);
		}
	    return $path;
	}
	#end AbsolutePath
	
	sub GenerateURL {  
	    my($self, $instance, $root_url) = @_;
	    # Generates iterations, but as a URL instead of a path.
		
	    my $prefix = $root_url . $self->{_prefix};
	    my $retval = undef;
		if($instance =~ /^\d+$/) { 
			if($instance) {
				$retval = $prefix . $instance . $self->{_suffix};
			}
			else {
				$retval = $prefix . $self->{_suffix};
			}
	    }
		else {
			$retval = $prefix . $instance;
		}
		
	    return URL->Canonicalize($retval);
	}
	#end GenerateURL
	
	sub GenerateWildURL {
	    my($self, $root_url) = @_;
	    # Generates a wildcard that should match all our iterations
		
	    my $prefix = URL->Canonicalize($root_url . $self->{_prefix});
	    my $temp   = URL->Canonicalize($prefix . $self->{_suffix});
	    my $suffix = substr($temp, length($prefix));
	    my $wild   = $prefix . '*' . $suffix;
		# Convert wildcard to regular expression
		my %patmap = (
		    '*' => '.*',
		    '?' => '.',
		    '[' => '[',
		    ']' => ']',
		);
		$wild =~ s{(.)} { $patmap{$1} || "\Q$1" }ge;
		return '^'.$wild.'$';
	}
	#end def GenerateWildURL
}
# end package FilePathGenerator

{package PerURLStatistics;
	# Keep track of some simple per-URL statistics, like file extension.
	
	sub new {  
		my $class = shift;
        my $self = {};
        $self->{_extensions} = {};			# Count of extension instances
		bless($self, $class);
        return $self;
    }
	#end new
	
	sub Consume {  
		my($self, $url) = @_;
		# Log some stats for the URL.  At the moment, that means extension.
		
		my $path;
		if($url && $url->{loc}){
			use URI::URL;
			my $uri_url = URI::URL->new($url->{loc});
			$path = $uri_url->path;
			if(!$path) {
				return;
			}
		}

		# Recognize directories
		if(substr($path, length($path) - 1) eq '/') {
			if(exists($self->{_extensions}->{'/'})) {
				$self->{_extensions}->{'/'}++;
			}
			else {
				$self->{_extensions}->{'/'} = 1;
			}
			return;
		}
		
		# Strip to a filename
		my $i = rindex($path, '/');
		if($i >= 0) {
			$path = substr($path, $i);
		}
		
		# Find extension
		$i = rindex($path, '.');
		if($i > 0) {
			my $ext_without_case_shift = substr($path, $i);
			my $ext = lc($ext_without_case_shift);
			if(exists($self->{_extensions}->{$ext})) {
				$self->{_extensions}->{$ext}++;
			}
			else {
				$self->{_extensions}->{$ext} = 1;
			}
		}
		else {
			if(exists($self->{_extensions}->{'(no extension)'})) {
				$self->{_extensions}->{'(no extension)'}++;
			}
			else {
				$self->{_extensions}->{'(no extension)'} = 1;
			}
		}
	}
	#end Consume
	
	sub Log {  
	    my($self) = @_;
	    # Dump out stats to the output.
		
	    if($self->{_extensions}) {
			$output->Log("Count of file extensions on URLs:", 1);
			foreach my $ext (sort keys %{$self->{_extensions}}) {
				my $formatted = sprintf(" %7d", $self->{_extensions}->{$ext});
				$output->Log($formatted . " $ext", 1);
			}
	    }
	}
	#end Log
}
# end package PerURLStatistics

{package Sitemap;
	# This is the big workhorse class that processes your inputs and spits
    # out sitemap files.  It is built as a SAX handler for set up purposes.
    # That is, it processes an XML stream to bring itself up.
	
	sub new {  
		my($class, $suppress_notify) = @_;
        my $self = {};
		bless($self, $class);
				
		$self->{_filters} = ();						# Filter objects
        $self->{_inputs} = ();						# Input objects
        $self->{_urls} = {};						# Maps URLs to count of dups
        $self->{_set} = ();							# Current set of URLs
        $self->{_filegen} = undef;					# Path generator for output files
        $self->{_wildurl1} = undef;					# Sitemap URLs to filter out
        $self->{_wildurl2} = undef;					# Sitemap URLs to filter out
        $self->{_sitemaps} = 0;						# Number of output files
		# We init _dup_max to 2 so the default priority is 0.5 instead of 1.0
        $self->{_dup_max} = 2;						# Max number of duplicate URLs
        $self->{_stat} = PerURLStatistics->new();	# Some simple stats
        $self->{_in_site} = $False; 				# SAX: are we in a Site node?
        $self->{_in_site_ever} = $False;			# SAX: were we ever in a Site?	
        $self->{_default_enc} = undef;				# Best encoding to try on URLs
        $self->{_base_url} = undef;					# Prefix to all valid URLs
        $self->{_store_into} = undef;				# Output filepath
        $self->{_suppress} = $suppress_notify;		# Suppress notify of servers
	
		return $self;
	}
	# end new
	
	sub ValidateBasicConfig {  
		my($self) = @_;
		# Verifies (and cleans up) the basic user-configurable options.
		
		my $all_good = $True;
		
		if($self->{_default_enc}) {
			$encoder->SetUserEncoding($self->{_default_enc});
		}
				
		# Canonicalize the base_url
		if($all_good && !$self->{_base_url}) {
			$output->Error("A site needs a \"base_url\" attribute.");
            $all_good = $False;
        }
		if($all_good && !URL->IsAbsolute($self->{_base_url})) {
			$output->Error("The \"base_url\" must be absolute, not relative: " . $self->{_base_url});
            $all_good = $False;
        }
		if($all_good) {
            $self->{_base_url} = URL->Canonicalize($self->{_base_url});
            if(substr($self->{_base_url}, length($self->{_base_url}) - 1) ne '/') {
                $self->{_base_url} .= '/';
            }
			$output->Log("BaseURL is set to: " . $self->{_base_url}, 2);
        }
		
		# Load store_into into a generator
		if($all_good) {
            if($self->{_store_into}) {
                $self->{_filegen} = FilePathGenerator->new();
                if(!$self->{_filegen}->Preload($self->{_store_into})) {
                    $all_good = $False;
                }
            }
            else {
				$output->Error("A site needs a \"store_into\" attribute.");
                $all_good = $False;
            }
        }
		
		# Ask the generator for patterns on what its output will look like
		if($all_good) {
            $self->{_wildurl1} = $self->{_filegen}->GenerateWildURL($self->{_base_url});
            $self->{_wildurl2} = $self->{_filegen}->GenerateURL($SITEINDEX_SUFFIX, $self->{_base_url});
        }
		
		# Done
		if(!$all_good) {
			$output->Log("See \"example_config.xml\" for more information.", 0);
        }
		
		return $all_good;
	}
	# end ValidateBasicConfig
	
	sub Generate {  
	    my($self) = @_;
	    # Run over all the Inputs and ask them to Produce.
		
		# Run the inputs
	    foreach my $input (@{$self->{_inputs}}) {
	        $input->ProduceURLs($self, \&ConsumeURL);
	    }
		
		# Do last flushes
		if($self->{_set}) {
			if(@{$self->{_set}}) {
				$self->FlushSet();
			}
		}	    
	    if(!$self->{_sitemaps}) {
	        $output->Warn("No URLs were recorded, writing an empty sitemap.");
	        $self->FlushSet();
	    }
		
		# Write an index as needed
	    if($self->{_sitemaps} > 1) {
	        $self->WriteIndex();
	    }
		
		# Notify
	    $self->NotifySearch();
		
		# Dump stats
	    $self->{_stat}->Log();
	}
	#end Generate
	
	sub ConsumeURL {
		my($self, $url, $allow_fragment) = @_;
	    # All per-URL processing comes together here, regardless of Input.
	    # Here we run filters, remove duplicates, spill to disk as needed, etc.
		
		if(!$url) {
			return;
		}	
		
		# Validate
		if(!$url->Validate($self->{_base_url}, $allow_fragment)) {
			return;
		}
		
		# Run filters
		my $accept = $None;
	    foreach my $filter (@{$self->{_filters}}) {
			$accept = $filter->Apply($url);
			if($accept != $None) {
				last;
			}
		}
	    if(!($accept || ($accept == $None))) {
			$url->Log('FILTERED', 2);
			return;
		}
		
		# Ignore our out output URLs
	    if($url->{loc} =~ $self->{_wildurl1} || $url->{loc} =~ $self->{_wildurl2}) {
			$url->Log('IGNORED (output file)', 2);
			return;
		}

	    # Note the sighting
	    my $hash = $url->MakeHash();
	    if(exists($self->{_urls}->{$hash})) {
			my $dup = $self->{_urls}->{$hash};
			if($dup > 0) {
			    $dup++;
			    $self->{_urls}->{$hash} = $dup;
			    if($self->{_dup_max} < $dup) {
					$self->{_dup_max} = $dup;
				}
			}
		    $url->Log('DUPLICATE');
		    return;
		}
		
		#Acceptance -- add to set
		$self->{_urls}->{$hash} = 1;
		
		push(@{$self->{_set}}, $url);
		$self->{_stat}->Consume($url);
		$url->Log();
		
		# Flush the set if needed
	    my $set_length = @{$self->{_set}};
		if($set_length >= $MAXURLS_PER_SITEMAP) {
			$self->FlushSet();
		}
	}
	#end ConsumeURL
	
	sub FlushSet {  
	    my($self) = @_;
	    # Flush the current set of URLs to the output.  This is a little
	    # slow because we like to sort them all and normalize the priorities
	    # before dumping.
	    		
		# Sort and normalize
	    $output->Log("Sorting and normalizing collected URLs.", 1);
		if($self->{_set}) {
			@{$self->{_set}} = sort { $a->{loc} cmp $b->{loc} } @{$self->{_set}};			
		}
	    foreach my $url (@{$self->{_set}}) {
			my $hash = $url->MakeHash();
		    my $dup = $self->{_urls}->{$hash};
			if($dup > 0) {
			    $self->{_urls}->{$hash} = -1;
			    if(!$url->{priority}) {
				    $url->{priority} = $dup / $self->{_dup_max}; 
					$url->{priority} = substr($url->{priority}, 0, 6); 
				}
			}
		}
		
		# Get the filename we're going to write to
	    my $relative_filename = $self->{_filegen}->GeneratePath($self->{_sitemaps});
		my $filename = $self->{_filegen}->AbsolutePath($relative_filename);
		if(!$filename) {
			$output->Fatal("Unexpected: Couldn't generate output filename.");
		}
		
	    $self->{_sitemaps}++;
		my $num_urls;
		if(@{$self->{_set}}) {
			$num_urls = @{$self->{_set}};
		}
		else {
			$num_urls = 0;
		}
	    $output->Log("Writing Sitemap file \"$relative_filename\" with " . $num_urls . " URLs", 1);
		
		# Write to it
		local *SITEMAP;
		my $success = open SITEMAP, "> $filename";
		unless($success) {
			$output->Fatal("Couldn't write out to file: $filename");
			return $False; 
		}
		if(substr($filename, length($filename) - 3) eq '.gz') {
			eval "use Compress::Zlib";
			if($@) {
				$output->Fatal("Perl Module 'Compress::Zlib' required for compression/decompression.\nSee README installation notes.");
				return $False;
			}
			my $gz = gzopen(*SITEMAP, "wb");
			$gz->gzwrite($SITEMAP_HEADER);
			
			foreach my $url (@{$self->{_set}}) {
				$url->WriteXML($gz, $True);
			}
			
			$gz->gzwrite($SITEMAP_FOOTER);	
			$gz->gzclose();
		}
		else {				
			print SITEMAP $SITEMAP_HEADER;
			foreach my $url (@{$self->{_set}}) {
				$url->WriteXML(*SITEMAP{IO}, $False);
			}
			print SITEMAP $SITEMAP_FOOTER;				
			close SITEMAP;
		}
		
	    chmod(0644, $filename);
		
		# Flush
	    @{$self->{_set}} = ();
	}
	#end FlushSet
	
	sub WriteIndex {
	    my($self) = @_;
	    # Write the master index of all Sitemap files 
		
		# Make a filename
	    my $filename = $self->{_filegen}->GeneratePath($SITEINDEX_SUFFIX);
	    if(!$filename) {
			$output->Fatal("Unexpected: Couldn't generate output index filename.");
		}
	    $output->Log("Writing index file \"$filename\" with " . $self->{_sitemaps} . " Sitemaps", 1);
		
		# Make a lastmod time
		my $lastmod = ::TimestampISO8601(time());
		
		# Write to it
		local *SITEINDEX;
		my $success = open SITEINDEX, "> $filename";
		unless($success) {
			$output->Error("Can not open file: $filename");
			return $False; 
		}
		eval {	
			print SITEINDEX $SITEINDEX_HEADER;
			foreach my $mapnumber (0 .. $self->{_sitemaps} - 1) {
				# Write the entry
				my $mapurl = $self->{_filegen}->GenerateURL($mapnumber, $self->{_base_url});
				my $siteindex_entry = $SITEINDEX_ENTRY;
				$siteindex_entry =~ s/LOC/$mapurl/;
				$siteindex_entry =~ s/LASTMOD/$lastmod/;
				print SITEINDEX $siteindex_entry;
			}
			print SITEINDEX $SITEINDEX_FOOTER;
				
			close SITEINDEX;
		};
		if($@) {
			$output->Fatal("Couldn't write out to file: $filename");
		}
	    chmod(0644, $filename);
	}
	#end WriteIndex
	
	sub NotifySearch {
	    my($self) = @_;
	    # Send notification of the new Sitemap(s) to the search engines.
		
		if($self->{_suppress}) {
	        $output->Log("Search engine notification is suppressed.", 1);
	        return;
	    }
	    $output->Log("Notifying search engines.", 1);
		
        {package ExceptionURLopener;
			# Handle HTTP response code errors
		
            sub http_error_default {
				my($self, $url) = @_;
				
				my $file;
                my $http_code; 
				$http_code = LWP::Simple::getstore($url,$file);
				
				my $errmsg;
				if($http_code == 400){
					$errmsg = "Bad request";
				}
				elsif($http_code == 401){
					$errmsg = "Not Authorised";
				}
				elsif($http_code == 402){
					$errmsg = "Payment required";
				}
				elsif($http_code == 403){
					$errmsg = "Forbidden";
				}
				elsif($http_code == 404){
					$errmsg = "Not Found";
				}
				elsif($http_code == 405){
					$errmsg = "Method Not Allowed";
				}
				elsif($http_code == 406){
					$errmsg = "Not Acceptable";
				}
				elsif($http_code == 407){
					$errmsg = "Proxy Authentication Required";
				}
				elsif($http_code == 408){
					$errmsg = "Request Timeout";
				}
				elsif($http_code == 409){
					$errmsg = "Conflict";
				}
				elsif($http_code == 410){
					$errmsg = "Gone";
				}
				elsif($http_code == 411){
					$errmsg = "Lenght required";
				}
				elsif($http_code == 412){
					$errmsg = "Precondition Failed";
				}
				elsif($http_code == 413){
					$errmsg = "Request Entity Too Large";
				}
				elsif($http_code == 414){
					$errmsg = "Request URI Too Long";
				}
				elsif($http_code == 415){
					$errmsg = "Unsupported Media Type";
				}
				elsif($http_code == 416){
					$errmsg = "Request Range Not Satisfiable";
				}
				elsif($http_code == 417){
					$errmsg = "Expectation Failed";
				}
				if($http_code != 200){
					$output->Log("HTTP error " . $http_code . " : " . $errmsg, 2);
				}
				
				return $http_code;
            }
		}
		
		# Build the URL we want to send in
		my $url;
		if($self->{_sitemaps} > 1) {
	        $url = $self->{_filegen}->GenerateURL($SITEINDEX_SUFFIX, $self->{_base_url});
	    }
	    else {
	        $url = $self->{_filegen}->GenerateURL(0, $self->{_base_url});
	    }
				
		# Test if we can hit it ourselves
		my $http_code = ExceptionURLopener->http_error_default($url);
		if($http_code != 200) {
			$output->Error("When attempting to access our generated Sitemap at the following URL: \n". $url . "\nwe failed to read it.   Please verify the store_into path you specified in \nyour configuration file is a web-accessable. Consult the FAQ for more \ninformation.");
			$output->Warn("Proceeding to notify with an unverifyable URL.");
		}
		
		# Cycle through notifications. To understand this, see the comment near the NOTIFICATION_SITES comment
		my $ping;
		my @query_map;
		foreach my $ping (@NOTIFICATION_SITES){
			my $query_map  = $ping->{query};
			my $query_attr = $ping->{sitemap};
			$query_map->{$query_attr} = $url;
			
			# Build notification URL
			use URI::URL;
			my $notify = new URI::URL;
			$notify->scheme		($ping->{scheme}); 
			$notify->netloc		($ping->{netloc}); 
			$notify->path		($ping->{path}); 
			$notify->query_form	($query_map); 
			if($ping->{fragment}) {
				$notify->frag	($ping->{fragment});
			}

			#  Send the notification
			$output->Log("Notifying: ".$ping->{netloc}, 1);
			$output->Log("Notification URL: ".$notify, 2);
			if(ExceptionURLopener->http_error_default($notify) != 200) {
				$output->Warn("Cannot contact: ".$ping->{netloc});
			}
		}
	}
	#end NotifySearch
	
	sub start_element {  
	    my($self, $element) = @_;
	    # SAX processing, called per node in the config stream.
		
		# Replace existing character references in attribute values
		my %attributes;
		foreach my $attr (keys %{$element->{Attributes}}) {
			if($element->{Attributes}->{$attr}->{Value} =~ m/&#(\d*);/) {
				my $char_ref = $1;
				my $char = chr($char_ref);
				$element->{Attributes}->{$attr}->{Value} = Encode::decode("UTF-8", $element->{Attributes}->{$attr}->{Value});
				$element->{Attributes}->{$attr}->{Value} =~ s/&#\d*;/$char/;
			}
			elsif($element->{Attributes}->{$attr}->{Value} =~ m/&#x([a-zA-Z]*\d*);/) {
				my $char_ref = $1;
				my $char = chr(hex($char_ref));
				$element->{Attributes}->{$attr}->{Value} = Encode::decode("UTF-8", $element->{Attributes}->{$attr}->{Value});
				$element->{Attributes}->{$attr}->{Value} =~ s/&#x[a-zA-Z]*\d*;/$char/;
			}
			$attributes{$attr} = $element->{Attributes}->{$attr}->{Value};
		}
		
		if($element->{Name} eq 'site') {
			if($self->{_in_site}) {
				$output->Error("Can not nest Site entries in the configuration.");
			}
			else {
				$self->{_in_site} = $True;
			}

			my @attributes = keys %{$element->{Attributes}};
			foreach my $attr (@attributes) {
				# Attributes have the format '{}base_url', '{}store_into', etc. Remove '{}' from them.
				$attr = substr($attr, 2); 
			}
			my @goodattributes = ('verbose', 'default_encoding', 'base_url', 'store_into', 'suppress_search_engine_notify');						
		    if(!::ValidateAttributes('SITE', \@attributes, \@goodattributes)) {
				return;
			}

		    my $verbose = $element->{Attributes}->{"{}verbose"}->{Value};
		    if($verbose) {
				$output->SetVerbose($verbose)
			}

		    $self->{_default_enc}  = $element->{Attributes}->{"{}default_encoding"}->{Value};
		    $self->{_base_url} 	   = $element->{Attributes}->{"{}base_url"}->{Value};
			$self->{_store_into}   = $element->{Attributes}->{"{}store_into"}->{Value};
		    if(!$self->{_suppress}) {
				$self->{_suppress} = $element->{Attributes}->{"{}suppress_search_engine_notify"}->{Value};
			}
		    $self->ValidateBasicConfig();
		}
		
		elsif($element->{Name} eq 'filter') {
			push(@{$self->{_filters}}, Filter->new($element->{Attributes}));
		}
		
		elsif($element->{Name} eq 'url') {
			push(@{$self->{_inputs}}, InputURL->new($element->{Attributes}));
		}
		
		elsif($element->{Name} eq 'urllist') {
			foreach my $attributeset (::ExpandPathAttribute('{}path', %attributes)) {
				push(@{$self->{_inputs}}, InputURLList->new($attributeset));
			}
		}
		
		elsif($element->{Name} eq 'directory') {
			push(@{$self->{_inputs}}, InputDirectory->new($element->{Attributes}, $self->{_base_url}));
		}

		elsif($element->{Name} eq 'accesslog') {
			foreach my $attributeset (::ExpandPathAttribute('{}path', %attributes)) {
				push(@{$self->{_inputs}}, InputAccessLog->new($attributeset));
			}
		}

	    elsif($element->{Name} eq 'sitemap') {
			foreach my $attributeset (::ExpandPathAttribute('{}path', %attributes)) {
				push(@{$self->{_inputs}}, InputSitemap->new($attributeset));
			}
		}
		
	    else {
			$output->Error("Unrecognized tag in the configuration: " . $element->{Name});
		}
	}
	#end start_element
	
	sub end_element {  
	    my($self, $element) = @_;
	    # SAX processing, called per node in the config stream.
		
	    if($element->{Name} eq 'site') {
	        die if not $self->{_in_site};
	        $self->{_in_site} = $False; 
	        $self->{_in_site_ever} = $True;		
	    }
	}
	#end end_element
	
	sub end_document {  
	    my($self) = @_;
	    # End of SAX, verify we can proceed.
		
	    if(!$self->{_in_site_ever}) {
			$output->Error("The configuration must specify a \"site\" element.");
	    }
	    else {
	        if(!$self->{_inputs}) {
				$output->Warn("There were no inputs to generate a sitemap from.");
	        }
		}	
    }
	#end end_document
}
# end package Sitemap

sub StripString {
	my($string) = @_;
	# Remove spaces at the beginning and end of a string
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
# end StripString

sub ValidateAttributes {
    my($tag, $attributes, $goodattributes) = @_;
	# Makes sure 'attributes' does not contain any attribute not
	# listed in 'goodattributes'
	
    my $all_good = $True;
	foreach my $attr (@$attributes) {
		my $is_element = $False;
		foreach my $element (@$goodattributes) {
			if($attr eq $element) {
				$is_element = $True;
			}
		}
		if(!$is_element) {
			$output->Error("Uknown $tag attribute: $attr"); 
			$all_good = $False;
		}
	}
    return $all_good;
}
# end ValidateAttributes

sub ExpandPathAttribute {
    my($attrib, %src) = @_;
	# Given a hash of attributes, return an array of hashes with all the same attributes except for the one named attrib.
	# That one, we treat as a file path and expand into all its possible variations.
	
	# Do the path expansion.  On any error, just return the source hash.
    if(!exists($src{$attrib})) {
        return %src;
    }
	my $path = $src{$attrib};
	$path = $encoder->MaybeNarrowPath($path);
	my @pathlist = File::Glob::bsd_glob($path);
	if(!@pathlist) {
        return %src;
    }
		
	# Create N new hashes and store into array
    my @retval = ();
    foreach my $path (@pathlist) {
        my %dst = %src;
        $dst{$attrib} = $path;
        push(@retval, \%dst);
    }
    return @retval;
}
# end ExpandPathAttribute

sub OpenFileForRead {
    my($path, $logtext) = @_;
	# Opens a text file, be it GZip or plain 
	
	if(!$path) {
		return undef;
	}
	
	# Open the file
	local *FILE;
	my $success = open FILE, "< $path";
	unless($success) {
		$output->Error("Can not open file: $path");
		return $False;
	}
	
	# Check if we have a GZip file
	my $is_gzip_file = $False;
	if(substr($path, length($path) - 3) eq '.gz') {
		$is_gzip_file = $True;
	}
	
	if($logtext) {
		$output->Log("Opened $logtext file: $path", 1);
	}
	else {
		$output->Log("Opened file: $path", 1);
	}
	
	return {file => *FILE{IO}, is_gzip => $is_gzip_file};
}
# end OpenFileForRead

sub TimestampISO8601 {
    my($t) = @_;
	# Seconds since epoch (1970-01-01) --> ISO 8601 time string.
	
	my($sec, $min, $hour, $day, $mon, $year) = gmtime($t);
	$year += 1900;
	$mon += 1;

	foreach my $datetime_element ($sec, $min, $hour, $day, $mon) {
		if(length($datetime_element) < 2) {
			$datetime_element = '0'.$datetime_element;
		}
	}
    return "$year-$mon-$day".'T'."$hour:$min:$sec".'Z';
}
# end TimestampISO8601

sub CreateSitemapFromFile {
    my($configpath, $suppress_notify) = @_;
	# Sets up a new Sitemap object from the specified configuration file.
	
	# Remember error count on the way in
    my $num_errors = $output->{num_errors};
	
	# Rev up SAX to parse the config
	my $sitemap = Sitemap->new($suppress_notify);
	
	eval {
		$output->Log("Reading configuration file: " . $configpath, 0);
		my $xml_parser = XML::SAX::ParserFactory->parser( ContentHandler => $sitemap );
		$xml_parser->parse_uri($configpath);
	};
	if($@) {
		if(index($@, 'LWP Request Failed') != -1 || index($@, 'Permission denied') != -1) {
			$output->Error("Cannot read configuration file: $configpath");
		}
		else {
			$@ =~ s/\n+$//;
			$@ =~ s/\r+$//;
			$output->Error("XML error in the config file: $@");
		}
	}
				 
	# If we added any errors, return no sitemap			 
    if($num_errors == $output->{num_errors}) {
		return $sitemap;
    }
    return undef;
}
# end CreateSitemapFromFile

sub ProcessCommandFlags {
	# Parse command line flags per specified usage, pick off key, value pairs
	
    my %flags = ();
	if(GetOptions(\%flags, 'config=s', 'help', 'testing')) {
		return %flags;	
	}
}
# end ProcessCommandFlags

%flags = ProcessCommandFlags();
if(!%flags || !exists($flags{'config'}) || exists($flags{'help'})) {
	$output->Log($__usage__, 0);
}
else {
    my $suppress_notify = exists($flags{'testing'});
	my $sitemap = CreateSitemapFromFile($flags{'config'}, $suppress_notify);
    if(!$sitemap) {
		$output->Log('Configuration file errors -- exiting.', 0);
    }
    else {
		$sitemap->Generate();
		$output->Log("Number of errors: " . $output->{num_errors}, 1);
		$output->Log("Number of warnings: " . $output->{num_warns}, 1);
    }
}
1;