from intermine.util import ReadableException

class UnimplementedError(Exception):
    pass

class ServiceError(ReadableException):
    """Errors in the creation and use of the Service object"""
    pass

class WebserviceError(IOError):
    """Errors from interaction with the webservice"""
    pass
