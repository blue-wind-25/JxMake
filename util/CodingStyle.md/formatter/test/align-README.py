#!/usr/bin/env python3


import textwrap


INPUT_FILE = "README.txt"

# Maximum output line width
LINE_WIDTH = 110

SOURCE_EXTS = (
    ".h",
    ".c",
    ".hpp",
    ".cpp",
    ".java",
    ".kt",
    ".json",
    ".json5",
    ".css",
    ".yaml",
    ".toml",
    ".xml",
    ".xsl",
    ".xsd",
    ".svg",
    ".html",
    ".js",
    ".jsx",
    ".ts",
    ".tsx",
    ".py",
    ".ini",
    ".jxm",
    ".mk",
    ".sh",
    ".ps1"
)


def parse_entry(line):
    """
    Returns (indent, filename, description) if this is a test entry,
    otherwise None
    """
    if " -- " not in line:
        return None

    left, right = line.split(" -- ", 1)

    if not left.strip().endswith(SOURCE_EXTS):
        return None

    indent = left[:len(left) - len(left.lstrip())]
    filename = left.strip()

    return indent, filename, right.strip()


def format_entries(lines):
    entries = []

    # Compute the column for "-- " based on the longest filename.
    # This ensures the separator can shift left if filenames become shorter.
    max_filename_len = 0
    for line in lines:
        entry = parse_entry(line)
        if entry is None:
            continue
        _, filename, _ = entry
        max_filename_len = max(max_filename_len, len(filename))

    desc_col = max_filename_len + 1  # +1 for the space before "--"

    i = 0
    while i < len(lines):

        entry = parse_entry(lines[i])

        if entry is None:
            entries.append(lines[i])
            i += 1
            continue

        indent, filename, desc = entry

        j = i + 1

        while j < len(lines):

            # Blank line ends the description
            if lines[j].strip() == "":
                break

            # Next test entry starts here
            if parse_entry(lines[j]) is not None:
                break

            desc += " " + lines[j].strip()
            j += 1

        entries.append((indent, filename, desc))
        i = j

    out = []

    for item in entries:

        if isinstance(item, str):
            out.append(item)
            continue

        indent, filename, desc = item

        prefix = indent + filename.ljust(desc_col) + "-- "
        subsequent = " " * len(prefix)

        wrapped = textwrap.fill(
            desc,
            width=LINE_WIDTH,
            initial_indent=prefix,
            subsequent_indent=subsequent,
            break_long_words=False,
            break_on_hyphens=False,
        )

        out.extend(wrapped.splitlines())

    return out


def main():

    with open(INPUT_FILE, encoding="utf-8") as f:
        lines = f.read().splitlines()

    out = format_entries(lines)

    with open(INPUT_FILE, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")

    print(f"Formatted {INPUT_FILE}")


if __name__ == "__main__":
    main()
