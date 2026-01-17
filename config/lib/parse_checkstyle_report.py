#!/usr/bin/env python

from collections import defaultdict
import glob
import os
import sys
import xml.sax
from xml.sax.handler import ContentHandler, ErrorHandler


class Error(object):
    def __init__(self, severity, line, column, message):
        self.severity = severity
        self.line = line
        self.column = column if column is not None else "-"
        self.message = message


class CheckStyleHandler(ContentHandler):
    def __init__(self):
        self.errors = defaultdict(list)
        self.currentFile = None

    def startElement(self, name, attrs):
        if name == "file":
            self.currentFile = attrs.get("name")
        elif name == "error":
            self.handleError(attrs)

    # TODO - compile the classes so we don't have to do this.
    def can_ignore(self, err):
        if "Unable to get class information" in err.message:
            return True
        if "Redundant throws" in err.message:
            return True
        if "Unused @throws tag" in err.message:
            return True
        return False

    def handleError(self, attrs):
        err = Error(
            attrs.get("severity"),
            attrs.get("line"),
            attrs.get("column"),
            attrs.get("message"),
        )
        if not self.can_ignore(err):
            self.errors[self.currentFile].append(err)


class NervousErrorHandler(ErrorHandler):
    def error(self, exception):
        raise exception

    def warning(self, exception):
        raise exception


ERROR_FMT = (
    "  line: {0.line:>4}, column: {0.column:>3} - {0.severity} - {0.message}"
)

workspace_dir = sys.argv[1]

error_count = 0
report_count = 0

for report in glob.glob(
    "**/reports/checkstyle/main.xml", root_dir=workspace_dir, recursive=True
):
    report_path = os.path.join(workspace_dir, report)
    with open(report_path) as f:
        report_count += 1
        handler = CheckStyleHandler()
        xml.sax.parse(f, handler, NervousErrorHandler())
        for file_name, errors in handler.errors.items():
            print("FILE:", file_name)
            for e in errors:
                print(ERROR_FMT.format(e))
                error_count += 1

if error_count:
    print(f"{error_count} STYLE CHECKS FAILED")
    sys.exit(1)

if not report_count:
    print("NO STYLE REPORTS FOUND!")
    sys.exit(1)
