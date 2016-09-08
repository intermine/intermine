#!/usr/bin/env python

import argparse, subprocess, time, os, filecmp

parser = argparse.ArgumentParser()
parser.add_argument("mine_name")
parser.add_argument("dump_file_destination")
parser.add_argument("username")
args = parser.parse_args()
format = "%a-%d-%b-%Y-%H:%M:%S"
t = time.strftime(format)

new_dump_file_name = args.dump_file_destination  + "-" + t
current_dump_file_name = args.dump_file_destination

command = "pg_dump -c -h localhost -U " + args.username + " -f " + new_dump_file_name + " userprofile-" + args.mine_name

# dump the userprofile
subprocess.call(command,shell=True)

# diff the filecmp
if os.path.exists(current_dump_file_name) and filecmp.cmp(new_dump_file_name, current_dump_file_name):
    # no diff, delete
    os.remove(new_dump_file_name)
else:
    os.remove(current_dump_file_name)
    # new symlink if different
    os.symlink(new_dump_file_name, current_dump_file_name)

