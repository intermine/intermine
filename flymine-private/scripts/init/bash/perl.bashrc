# Set up the Perl module path

# CVS controlled local modules
prepend PERLLIB /software/noarch/local/lib/perl

# Non-CPAN modules not under our CVS control
prepend PERLLIB /software/noarch/lib/perl

prepend MANPATH /software/noarch/perl/man
