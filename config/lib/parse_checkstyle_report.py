#!/usr/bin/env python

from __future__ import print_function

import sys
import os
import os.path as path
from collections import defaultdict

import xml.sax
from xml.sax.handler import ContentHandler, ErrorHandler

class Error(object):

    def __init__(self, severity, line, column, message):
        self.severity = severity
        self.line = line
        self.column = column
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

    def can_ignore(self, err):
        return 'Unable to get class information' in err.message:

    def handleError(self, attrs):
        err = Error(
                attrs.get('severity'),
                attrs.get('line'),
                attrs.get('column'),
                attrs.get('message'))
        if not self.can_ignore(err):
            self.errors[self.currentFile].append(err)

class NervousErrorHandler(object):

    def error(self, exception):
        raise exception

    def warning(self, exception):
        raise exception

ERROR_FMT = '  line: {0.line:>4}, column: {0.column:>3} - {0.severity} - {0.message}'

report = sys.argv[1]

error_count = 0

with open(report) as f:
    handler = CheckStyleHandler()
    xml.sax.parse(f, handler, NervousErrorHandler())
    for file_name, errors in handler.errors.items():
        print("FILE:", file_name)
        for e in errors:
            print(ERROR_FMT.format(e))
            error_count += 1

sys.exit(error_count)

