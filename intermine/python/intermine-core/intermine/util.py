def openAnything(source):
    # Try to open with urllib (http, ftp, file url)
    import urllib
    try:
        return urllib.urlopen(source)
    except (IOError, OSError):
        pass

    try:
        return open(source)
    except (IOError, OSError):
        pass

    import StringIO
    return StringIO.StringIO(str(source))

class ReadableException(Exception):
    def __init__(self, message, cause=None):
        self.message = message
        self.cause = cause

    def __str__(self):
        if self.cause is None:
            return repr(self.message)
        else:
            return repr(self.message) + repr(self.cause)
