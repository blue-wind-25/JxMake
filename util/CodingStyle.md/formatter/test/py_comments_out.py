# Module setup
import os
import sys

# local helper
from . import sibling

flags  = 0x01
flags |= 0x02
# Comment breaks the group
timeout = 100

# Build the lookup table
lookup   = { k: v for k, v in items.items() } # Inline note
filtered = [ y for x in data if( y := transform(x) ) is not None ] # Keep truthy values

@dataclass
class Point:
    x : int  # Horizontal position
    # Vertical position
    y : int

def process(
    extra,
    x : int,  # Required
    y : "List[int]"
) -> Optional[str]:
    ...

@app.route("/status") # Health check endpoint
def status():
    """
    Health check endpoint.
        Always returns "ok" for now.
    """
    return "ok"

def dispatch(event):
    match event:
        case "start":
            begin()
        # Stopping the process
        case "stop":
            end()
        case _:
            pass

def check(x):
    if x < 0:
        # Negative case
        return None

    # Zero case
    if x == 0:
        return 0

    return x * 2

def classify(code):
    match code:
        case 1: return "one" # First
        case 2: return "two"
        # Fallback
        case _: return "unknown"
    # match code
