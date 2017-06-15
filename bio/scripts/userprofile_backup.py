#!/usr/bin/env python
#
# check if dump differs from previous one, in which case is saved
#
# now also
# - alert mail if dump empty or not there
# - remove previous dump if on the same day
# - save a gzip dump per day
#

import argparse, subprocess, time, os, filecmp

parser = argparse.ArgumentParser()
parser.add_argument("mine_name")
parser.add_argument("dump_file_destination")
parser.add_argument("username")
args = parser.parse_args()
format = "%a-%d-%b-%Y-%H:%M:%S"
t = time.strftime(format)
d = time.strftime("%a-%d-%b-%Y")

new_dump_file_name = args.dump_file_destination  + "-" + t
current_dump_file_name = args.dump_file_destination
recipients = "all@intermine.org"
no_file = "Dump failed, no file created"
empty_file = "Dump failed, empty backup file"

command = "pg_dump -c -h localhost -U " + args.username + " -f " + new_dump_file_name + " userprofile-" + args.mine_name

# dump the userprofile
subprocess.call(command,shell=True)

# if no new dump file -> alarm & exit
if not os.path.isfile(new_dump_file_name):
  mailcommand = "echo " + no_file + " | mail -s \"ALERT: userprofile backup for " + args.mine_name + " failed!\" " + recipients
  subprocess.call(mailcommand, shell=True)
  exit()

# if dump has size 0 -> alarm & exit
if os.path.getsize(new_dump_file_name) == 0:
  mailcommand = "echo " + empty_file + " | mail -s \"ALERT: userprofile backup for " + args.mine_name + " failed!\" " + recipients
  subprocess.call(mailcommand, shell=True)
  exit()

previous_dump = os.path.realpath(current_dump_file_name)

# diff the filecmp
if os.path.exists(current_dump_file_name) and filecmp.cmp(new_dump_file_name, current_dump_file_name):
    # no diff, delete
    os.remove(new_dump_file_name)
else:
    os.remove(current_dump_file_name)
    # new symlink if different
    os.symlink(new_dump_file_name, current_dump_file_name)
    if d in previous_dump:
      # same day, just remove
      os.remove(previous_dump)
    else:
      # keep one for the day before, and gzip it
      subprocess.check_call(['gzip', previous_dump])
