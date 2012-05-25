package DataDownloader::Resource::ABC;

use strict;
use warnings;

use Moose;
use MooseX::ABC;

with 'DataDownloader::Role::SystemCommand';

requires 'fetch';

use MooseX::FollowPBP;
use MooseX::FileAttribute;
use Moose::Util::TypeConstraints;
use File::Temp;
use Scalar::Util qw(refaddr);
use File::Copy qw(copy move);
use Ouch qw(:traditional);
use feature 'switch';

has title => (
    isa => 'Str',
    is  => 'ro',
    required => 1,
);

has subtitle => (
    init_arg => 'SUBTITLE',
    isa => 'Str',
    is  => 'ro',
    lazy_build => 1,
);

sub _build_subtitle {
    my $self = shift;
    if ($self->can("SUBTITLE")) {
        return $self->SUBTITLE;
    } else {
        return "";
    }
}

has file => (
    init_arg => 'FILE',
    isa => 'Str',
    is => 'ro',
    required => 1,
);

has sub_directory => (
    init_arg => 'SUB_DIR',
    isa => 'ArrayRef',
    is => 'ro', 
    default => sub { [] },
    auto_deref => 1,
);

sub as_string {
    my $self = shift;
    return sprintf("%s (%s) %s => %s", $self->get_title, $self->get_subtitle, $self->get_file, $self->get_destination);
}

has logger => (
    isa        => class_type('Log::Handler'),
    is         => 'ro',
    handles    => [qw/debug info notice warning error die/],
    required   => 1,
);

has_file temp_file => (
    documentation => 'location for file to be downloaded',
    is            => 'ro',
    lazy_build    => 1,
);

sub _build_temp_file {
    my $self = shift;
    return File::Temp->new()->filename;
}

has_file destination => (
    documentation => 'The eventual location of the new file',
    lazy_build    => 1,
    is            => 'rw',
);

sub _build_destination {
    my $self = shift;
    return $self->get_destination_dir->file($self->get_file);
}

has_directory destination_dir => (lazy_build => 1);

sub _build_destination_dir {
    my $self = shift;
    my $dir;
    if (my @sub_dirs = $self->get_sub_directory) {
        $dir = $self->get_main_dir->subdir(@sub_dirs);
    } else {
        $dir = $self->get_main_dir;
    }
    mkpath $dir unless (-d $dir);
    return $dir;
}

has_directory main_dir => (
    documentation => 'The new directory to be created',
    required      => 1,
);

has extract => (
    init_arg => 'EXTRACT',
    reader => 'needs_unzipping',
    isa => 'Bool', 
    default => 0,
);

my $default_post_processor = sub {
    my ($self, $given_file, $new_file) = @_;
    $self->debug(sprintf "Copying '%s' => '%s'", $given_file, $new_file);
    move($given_file, $new_file);
};

has post_processor => (
    init_arg  => 'POST_PROCESSOR',
    isa       => 'CodeRef',
    traits    => ['Code'],
    is        => 'ro',
    predicate => 'has_post_processor',
    lazy_build => 1,
    handles   => { make_destination => 'execute_method', },
);

sub _build_post_processor {
    my $self = shift;
    return $default_post_processor;
}

before make_destination => sub {
    my ($self, $temp_file, $new_file) = @_;
    $self->debug(sprintf "Executing post-process for: %s %s. %s --> %s",
        $self->get_title, $self->get_subtitle, $temp_file, $new_file) 
        unless (refaddr($default_post_processor) == refaddr($self->get_post_processor));
};

my $default_cleaner = sub {
    my $self = shift;
    if ($self->needs_unzipping) {
        $self->debug("Extracting " . $self->get_destination);
        given ($self->get_destination->stringify) {
            when (/\.tar\.gz$/) {$self->untarball;}
            when (/\.gz$/)      {$self->unzip_gz; }
            when (/\.zip$/)     {$self->unzip_zip;}
            default {throw "DownloadError", "Can't extract $_"}
        };
    }
};

has cleaner => (
    init_arg => 'CLEANER',
    isa     => 'CodeRef',
    traits  => ['Code'],
    is      => 'ro',
    predicate => 'has_cleaner',
    handles => { clean_up => 'execute_method', },
    lazy_build => 1,
);

sub _build_cleaner {
    return $default_cleaner;
}

before clean_up => sub {
    my $self = shift;
    $self->debug("Executing clean up for:", $self->get_title, $self->get_subtitle)
        unless (refaddr $default_cleaner == refaddr $self->get_cleaner);
};

1;
