package DataDownloader::Role::HTTP;

use MooseX::Declare;

role DataDownloader::Role::HTTP {

    use autodie qw(symlink);
    use Path::Class qw(file dir);
    use File::Copy qw(move copy);
    use Fatal qw(move copy);
    use IO::All;
    use IO::All::LWP;
    use TryCatch;

    requires qw(get_temp_file get_file_name get_title debug die);


    method download_file {
        my $source = $self->get_server() . '/' . $self->get_file_name();
        $self->debug("Downloading $source to " . $self->get_temp_file);
        my $response = io($source)->get();
        if ($response->is_error()) {
            $self->die($response->status_line());
        } else {
            my $fh = $self->get_temp_file->openw();
            $fh->print($response->content);
        }
    }

    method download_data {

        $self->log_start();
        
        try {
            $self->download_file();
            if ($self->files_are_different($self->get_old_file, $self->get_temp_file)) {
                $self->debug("keeping downloaded files");
                $self->update_files();
            } else {
                $self->debug("deleting downloaded file (", $self->get_temp_file, ").");
                $self->get_temp_file->remove();
                $self->info($self->get_title, "current data up to date");
            }
            $self->log_result_success();
        } catch($e) {
            $self->log_result_error($e);
        };
    }

    method update_files {
        # The downloaded file is now our reference old file. Move it into place, unless the old file is
        # the same location as the current file. When files need post-processing, the current file and the old
        # file are not the same thing.
        if ($self->get_old_file ne $self->get_current_file) {
            $self->debug(sprintf "Moving %s to %s", $self->get_temp_file, $self->get_old_file);
            move($self->get_temp_file, $self->get_old_file);
        } else {
            $self->set_old_file($self->get_temp_file);
        }

        # Perform any requested post-processing, or just copy the new file into place.
        $self->make_new_file_from($self->old_file);

        $self->clean_up();

        #Link the most recent download directory to current
        $self->update_link();

        $self->update_version();

    }

};

1;











    


