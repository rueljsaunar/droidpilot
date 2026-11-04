def id(value):
    """Select by resource-id."""
    return f"id:{value}"

def text(value):
    """Select by exact text."""
    return f"text:{value}"

def text_contains(value):
    """Select by text containing value."""
    return f"text_contains:{value}"

def text_matches(pattern):
    """Select by text matching regex pattern."""
    return f"text_matches:{pattern}"

def desc(value):
    """Select by content-description."""
    return f"desc:{value}"

def xpath(expr):
    """Select by XPath expression."""
    return f"xpath:{expr}"

def cls(name):
    """Select by class name."""
    return f"class:{name}"

def pkg(name):
    """Select by package name."""
    return f"pkg:{name}"
