import threading
import time
import os
import posixpath
import urllib
from socket import socket
from SimpleHTTPServer import SimpleHTTPRequestHandler
from BaseHTTPServer import HTTPServer

class SilentRequestHandler(SimpleHTTPRequestHandler): # pragma: no cover

    silent = True

    def translate_path(self, path):
        """Use the file's location instead of cwd"""
        # abandon query parameters
        path = path.split('?',1)[0]
        path = path.split('#',1)[0]
        path = posixpath.normpath(urllib.unquote(path))
        words = path.split('/')
        words = filter(None, words)
        path = os.path.dirname(__file__)
        for word in words:
            drive, word = os.path.splitdrive(word)
            head, word = os.path.split(word)
            if word in (os.curdir, os.pardir): continue
            path = os.path.join(path, word)
        return path

    def log_message(self, *args):
        """Don't log anything, unless you say so"""
        if not self.silent:
            SimpleHTTPRequestHandler.log_message(self, *args)

    def do_POST(self):
        self.do_GET()

class TestServer( threading.Thread ): # pragma: no cover
    def __init__(self, daemonise=True, silent=True):
        super(TestServer, self).__init__()
        self.daemon = daemonise
        self.silent = silent
        self.http = None
        # Try and get a free port number
        sock = socket()
        sock.bind(('', 0))
        self.port = sock.getsockname()[1]
        sock.close()
    def run(self):
        protocol="HTTP/1.0"
        server_address = ('', self.port)

        SilentRequestHandler.protocol_version = protocol
        SilentRequestHandler.silent = self.silent
        #if not self.silent:
        #    print "Starting", protocol, "server on port", self.port
        self.http = HTTPServer(server_address, SilentRequestHandler)
        self.http.serve_forever()

    def shutdown(self):
        self.join()

if __name__ == '__main__': # pragma: no cover
    server = TestServer(silent=False)
    server.start()
    for number in range(1, 20):
        time.sleep(2)
