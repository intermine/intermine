package DataDownloader::Role::Source;

use Moose::Role;
use Moose::Util::TypeConstraints;
use MooseX::FollowPBP;
use MooseX::FileAttribute;
use DataDownloader::Util qw(make_logger get_ymd make_link files_are_identical);
use Ouch qw(:traditional);
use Scalar::Util qw(blessed);
use File::Path qw(mkpath);

requires 'fetch_all_data';

has_directory data_dir => (required => 1, must_exist => 1);

has logger => (
    isa        => class_type('Log::Handler'),
    is         => 'ro',
    handles    => [qw/debug info notice warning error die/],
    lazy_build => 1,
);

=head2 version

The default version is a date-stamp in Y-M-D format.

=cut

has version => (
    is => 'rw',
    isa => 'Str',
    lazy_build => 1,
    builder => 'generate_version',
);

sub generate_version {
    return get_ymd();
}

=head2 generate_version_string

The default version string is: "Version: $version".

=cut

sub generate_version_string {
    my $self = shift;
    return "Version: " . $self->get_version;
}

sub _build_logger {
    my $self = shift;
    return make_logger();
}

sub log_start {
    my $self = shift;
    $self->info("Starting data download for " . $self->get_title);
}

sub log_result_success {
    my $self = shift;
    $self->info("Finished data download for " . $self->get_title);
}

sub log_result_error {
    my $self = shift;
    my $error = shift;
    $self->error("Error downloading data for " . $self->get_title . ": " . $error);
}

sub log_skip {
    my $self = shift;
    $self->info("Data for",  $self->get_title, "is up to date - skipping");
}

has compare_to_old => (
    init_arg => 'COMPARE',
    is => 'ro', 
    isa => 'Bool',
    lazy_build => 1,
    reader => 'compare_to_current_version',
);

sub _build_compare_to_old {
    my $self = shift;
    if ($self->can('COMPARE')) {
        return $self->COMPARE;
    } else {
        return 0;
    }
}

sub update_version {
    my $self = shift;

    my $current_dir = $self->get_source_dir->subdir("current");

    if ($self->compare_to_current_version) {
        if ($self->is_same_as($current_dir)) {
            $self->info("Downloaded data for", $self->get_title, "is identical to current data");
            $self->remove_downloaded_data;
            # so we don't have to do this again
            make_link("$current_dir", $self->get_destination_dir); 
            return;
        }
    }

    make_link($self->get_destination_dir, "$current_dir");

    my $version_file = $self->get_source_dir->file("VERSION");
    $version_file->openw->print(join("\n",
            $self->get_title, 
            $self->generate_version_string,
            $self->get_description, 
            $self->get_source_link), "\n");

    $self->info("New data available in " . $self->get_destination_dir);
}

sub is_same_as {
    my $self = shift;
    my $current = shift;
    my $downloaded = $self->get_destination_dir;
    $self->debug("Comparing $downloaded to $current");
    return 0 unless (-d $current);
    if ($current->children != $downloaded->children) {
        return 0;
    }
    for my $file ($downloaded->children) {
        return 0 unless files_are_identical($file, $current->file($file->basename));
    }
    return 1;
}

has_directory destination_dir => (lazy_build => 1, builder => 'build_destination_dir');

sub build_destination_dir {
    my $self = shift;
    my $dir = $self->get_source_dir->subdir($self->get_version);
    mkpath("$dir") unless ( -d $dir );
    return $dir;
}

=head2 data_is_up_to_date

By default returns true if the new directory to be created
exists and has content. 

=cut

sub data_is_up_to_date {
    my $self = shift;
    my $new_dir = $self->get_destination_dir;
    if (-d $new_dir and $new_dir->children) {
        return 1;
    } else {
        return 0;
    }
}

=head2 clean_up

No-op hook for post download actions

=cut

sub clean_up {};


sub get_data {
    my $self = shift;
    if ($self->data_is_up_to_date) {
        $self->log_skip();
        return;
    } else {
        $self->log_start();
    }
    my $e = try {
        $self->fetch_all_data();
        $self->clean_up();
        $self->update_version();
    };
    if (catch_all) {
        if (blessed $e and $e->can('trace')) {
            $self->debug($e->trace);
        }
        $self->log_result_error($e);
        $self->remove_downloaded_data;
    } else {
        $self->log_result_success();
    }
}

sub remove_downloaded_data {
    my $self = shift;
    $self->debug("Removing " . $self->get_destination_dir);
    $self->get_destination_dir->recurse(
        callback => sub {my $x = shift; $x->remove() or warn "Did not remove $x"}, 
        depthfirst => 1,
        preorder => 0,
    );
}

has_directory source_dir => (lazy_build => 1);

sub _build_source_dir {
    my $self = shift;
    return $self->get_data_dir->subdir($self->get_source_dir_name);
}


has ["title", "subtitle", "description", "source_link", "source_dir_name"] => (
    is => 'ro',
    isa => 'Str', 
    lazy_build => 1,
);

sub _build_title {
    my $self = shift;
    if ($self->can("TITLE")) {
        return $self->TITLE;
    }
}

sub _build_description {
    my $self = shift;
    if ($self->can("DESCRIPTION")) {
        return $self->DESCRIPTION;
    }
}

sub _build_source_link {
    my $self = shift;
    if ($self->can("SOURCE_LINK")) {
        return $self->SOURCE_LINK;
    }
}

sub _build_source_dir_name {
    my $self = shift;
    if ($self->can("SOURCE_DIR")) {
        return $self->SOURCE_DIR;
    }
}

1;
