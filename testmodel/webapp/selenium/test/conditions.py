def in_title(substr):
    """Make sure the substring is in the title"""
    return lambda d: substr in d.title.lower()

def find_by_id(elemId):
    """Find an element by its id"""
    return lambda d: d.find_element_by_id(elemId)
