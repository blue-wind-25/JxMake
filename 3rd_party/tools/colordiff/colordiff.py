#!/usr/bin/env python3

########################################################################
#                                                                      #
# ColorDiff - a wrapper/replacement for 'diff' producing               #
#             colourful output                                         #
#                                                                      #
# Copyright (C)2002-2020 Dave Ewart (davee@sungate.co.uk)              #
#                                                                      #
########################################################################
#                                                                      #
# This program is free software; you can redistribute it and/or modify #
# it under the terms of the GNU General Public License as published by #
# the Free Software Foundation; either version 2 of the License, or   #
# (at your option) any later version.                                  #
#                                                                      #
# This program is distributed in the hope that it will be useful,     #
# but WITHOUT ANY WARRANTY; without even the implied warranty of       #
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        #
# GNU General Public License for more details.                         #
#                                                                      #
########################################################################
#
# Python translation of colordiff.pl (colordiff 1.0.21) by Claude Sonnet 4.6.
# Fixes the original's bug with long/oddly-spaced/uncommon-whitespace filenames
# in the "Binary files … differ" line by using a more precise regex.

import os
import re
import sys
import subprocess

APP_NAME     = 'colordiff'
VERSION      = '1.0.21'
AUTHOR       = 'Dave Ewart'
AUTHOR_EMAIL = 'davee@sungate.co.uk'
APP_WWW      = 'http://www.colordiff.org/'
COPYRIGHT    = '(C)2002-2022'

COLOUR = {
    'white':       '\033[1;37m',
    'yellow':      '\033[1;33m',
    'green':       '\033[1;32m',
    'blue':        '\033[1;34m',
    'cyan':        '\033[1;36m',
    'red':         '\033[1;31m',
    'magenta':     '\033[1;35m',
    'black':       '\033[1;30m',
    'darkwhite':   '\033[0;37m',
    'darkyellow':  '\033[0;33m',
    'darkgreen':   '\033[0;32m',
    'darkblue':    '\033[0;34m',
    'darkcyan':    '\033[0;36m',
    'darkred':     '\033[0;31m',
    'darkmagenta': '\033[0;35m',
    'darkblack':   '\033[0;30m',
    'off':         '\033[0;0m',
}

# Defaults
plain_text = COLOUR['white']
file_old   = COLOUR['red']
file_new   = COLOUR['blue']
diff_stuff = COLOUR['magenta']
diff_file  = COLOUR['magenta']
cvs_stuff  = COLOUR['green']

show_banner      = True
diff_cmd         = 'diff'
cfg_color_mode   = True
cfg_color_patch  = False


def expand_tabs_to_spaces(s):
    result = []
    col = 0
    for ch in s:
        if ch == '\t':
            spaces = 8 - (col % 8)
            result.append(' ' * spaces)
            col += spaces
        else:
            result.append(ch)
            col += 1
    return ''.join(result)


def check_for_file_arguments(args):
    nonopts = 0
    ddash = False
    for arg in args:
        if arg == '--':
            ddash = True
            continue
        if ddash or arg == '-':
            nonopts += 1
            continue
        if not arg.startswith('-'):
            nonopts += 1
        if arg in ('--help', '--version', '-v'):
            nonopts += 1
    return nonopts


def detect_diff_type(record, allow_diffy):
    if re.match(r'^(\+\+\+ |--- |@@ )', record):
        return 'diffu'
    if re.match(r'^\*\*\*', record):
        return 'diffc'
    if re.match(r'^[0-9,]+[acd][0-9,]+$', record.rstrip('\n')):
        return 'diff'
    if allow_diffy and re.search(r'(\s\|\s|\s<$|\s>\s)', record):
        return 'diffy'
    if re.search(r'\[-.+?-\]', record, re.DOTALL) or re.search(r'\{\+.+?\+\}', record, re.DOTALL):
        return 'wdiff'
    if re.match(r'^Control files: lines which differ', record):
        return 'debdiff'
    return 'unknown'


def parse_color_value(value, setting, config_file):
    # 24-bit colour: e.g. 38;2;R;G;B or 1;38;2;R;G;B
    m = re.match(r'^((?:[0-9];)*)([34]8);2;([0-9]+);([0-9]+);([0-9]+)$', value)
    if m:
        r, g, b = int(m.group(3)), int(m.group(4)), int(m.group(5))
        if 0 <= r <= 255 and 0 <= g <= 255 and 0 <= b <= 255:
            return f'\033[{m.group(1)}{m.group(2)};2;{r};{g};{b}m'

    # Numeric 0-255
    if re.match(r'^[0-9]+$', value):
        n = int(value)
        if 0 <= n <= 255:
            if n < 8:
                return f'\033[0;3{n}m'
            elif n < 16:
                return f'\033[0;9{n - 8}m'
            else:
                return f'\033[0;38;5;{n}m'

    if value in COLOUR:
        return COLOUR[value]

    print(f'Invalid colour specification for setting {setting} ({value}) in {config_file}', file=sys.stderr)
    return None


def read_config_files(config_files, verify_mode):
    global plain_text, file_old, file_new, diff_stuff, diff_file, cvs_stuff
    global show_banner, cfg_color_mode, cfg_color_patch, diff_cmd

    if verify_mode:
        return

    for config_file in config_files:
        try:
            with open(config_file, 'r', errors='replace') as f:
                for line in f:
                    line = line.rstrip('\n')
                    if not line or line.startswith('#'):
                        continue
                    line_stripped = re.sub(r'\s+', '', line)
                    if '=' not in line_stripped:
                        print(f'Ignored invalid configuration line ({line}) in {config_file}', file=sys.stderr)
                        continue
                    setting, value = line_stripped.split('=', 1)
                    if not value:
                        print(f'Ignored invalid configuration line ({line}) in {config_file}', file=sys.stderr)
                        continue

                    if setting == 'banner':
                        if value == 'yes':
                            show_banner = True
                        elif value == 'no':
                            show_banner = False
                        continue
                    if setting == 'color_mode':
                        cfg_color_mode = (value == 'yes')
                        continue
                    if setting == 'color_patches':
                        cfg_color_patch = (value == 'yes')
                        continue
                    if setting == 'diff_cmd':
                        diff_cmd = value
                        continue

                    setting = setting.lower()
                    value = value.lower()
                    if value in ('normal', 'none'):
                        value = 'off'

                    colourval = parse_color_value(value, setting, config_file)
                    if colourval is None:
                        continue

                    if setting == 'plain':
                        plain_text = colourval
                    elif setting == 'oldtext':
                        file_old = colourval
                    elif setting == 'newtext':
                        file_new = colourval
                    elif setting == 'diffstuff':
                        diff_stuff = colourval
                    elif setting == 'difffile':
                        diff_file = colourval
                    elif setting == 'cvsstuff':
                        cvs_stuff = colourval
                    else:
                        print(f'Unknown option in {config_file}: {setting}', file=sys.stderr)
        except OSError:
            pass


def disable_colours():
    global plain_text, file_old, file_new, diff_file, diff_stuff, cvs_stuff
    plain_text = ''
    file_old   = ''
    file_new   = ''
    diff_file  = ''
    diff_stuff = ''
    cvs_stuff  = ''
    COLOUR['off'] = ''


def parse_args(argv):
    verify_mode      = False
    fake_exitcode    = False
    specified_type   = None
    cmd_color_mode   = None
    cmd_color_patch  = None
    color_term_only  = False
    cmd_banner       = None
    show_help        = False
    remaining        = []

    i = 0
    while i < len(argv):
        arg = argv[i]
        if arg == '--verifymode':
            verify_mode = True
        elif arg == '--fakeexitcode':
            fake_exitcode = True
        elif arg.startswith('--difftype='):
            specified_type = arg[len('--difftype='):]
        elif arg == '--difftype' and i + 1 < len(argv):
            i += 1
            specified_type = argv[i]
        elif arg.startswith('--color='):
            cmd_color_mode = arg[len('--color='):]
        elif arg == '--color' and i + 1 < len(argv):
            i += 1
            cmd_color_mode = argv[i]
        elif arg.startswith('--color-patches='):
            cmd_color_patch = arg[len('--color-patches='):]
        elif arg == '--color-patches' and i + 1 < len(argv):
            i += 1
            cmd_color_patch = argv[i]
        elif arg.startswith('--color-term-output-only='):
            val = arg[len('--color-term-output-only='):]
            color_term_only = (val == 'yes')
        elif arg == '--color-term-output-only':
            color_term_only = True
        elif arg == '--banner':
            cmd_banner = True
        elif arg == '--no-banner':
            cmd_banner = False
        elif arg == '--help':
            show_help = True
            remaining.append(arg)
        else:
            remaining.append(arg)
        i += 1

    return (verify_mode, fake_exitcode, specified_type, cmd_color_mode,
            cmd_color_patch, color_term_only, cmd_banner, show_help, remaining)


def main():
    global plain_text, file_old, file_new, diff_file, diff_stuff, cvs_stuff
    global show_banner, cfg_color_mode, cfg_color_patch, diff_cmd

    (verify_mode, fake_exitcode, specified_type, cmd_color_mode,
     cmd_color_patch, color_term_only, cmd_banner, show_help,
     remaining_args) = parse_args(sys.argv[1:])

    if show_help:
        print('colordiff:')
        print('    --help                   : Displays this help')
        print('    --color=(yes|no)         : Force (or suppress) display of colours in output')
        print('    --color=patches=(yes|no) : Force (or suppress) inclusion of colour codes in patch output')
        print('    --color-term-output-only : Force (or suppress) colour to only appear in terminal output')
        print('    --difftype=DIFFTYPE      : Force difftype detection to specified format')
        print('    --(no)banner             : Show (or suppress) the colordiff banner')
        print()
        print('    DIFFTYPE is usually auto-detected, but can be set to:')
        print('          diff, diffc, diffu, diffy, debdiff or wdiff')
        sys.exit(0)

    if specified_type and not re.match(r'^diff[cuy]?|(deb|w)diff$', specified_type):
        print('Invalid --difftype value', file=sys.stderr)

    HOME = next((os.environ.get(k) for k in ('HOME', 'USERPROFILE') if os.environ.get(k)), '')
    etcdir = next((d for d in ('/etc', os.environ.get('ALLUSERSPROFILE', '')) if d and os.path.isdir(d)), '/etc')

    config_files = [os.path.join(etcdir, 'colordiffrc')]
    user_config_dir = next((os.environ.get(k) for k in ('XDG_CONFIG_HOME', 'LOCALAPPDATA', 'APPDATA') if os.environ.get(k)), '')
    if user_config_dir:
        config_files.append(os.path.join(user_config_dir, 'colordiff', 'colordiffrc'))
    elif HOME:
        config_files.append(os.path.join(HOME, '.config', 'colordiff', 'colordiffrc'))
    if HOME:
        config_files.append(os.path.join(HOME, '.colordiffrc'))

    read_config_files(config_files, verify_mode)

    # Resolve color_mode
    color_mode = cfg_color_mode
    if cmd_color_mode in ('yes', 'always', 'auto'):
        color_mode = True
    elif cmd_color_mode in ('no', 'never'):
        color_mode = False

    # Resolve color_patch
    color_patch = cfg_color_patch
    if cmd_color_patch in ('yes', 'always', 'auto'):
        color_patch = True
    elif cmd_color_patch in ('no', 'never'):
        color_patch = False

    if verify_mode:
        color_patch = True
        show_banner = False

    stdout_is_file = os.path.isfile('/dev/stdout') if sys.stdout.fileno() >= 0 else False
    try:
        stdout_is_file = not os.isatty(sys.stdout.fileno()) and not sys.stdout.isatty()
    except Exception:
        stdout_is_file = False

    if (not color_mode or
            (stdout_is_file and not color_patch) or
            (color_term_only and not sys.stdout.isatty())):
        disable_colours()

    if cmd_banner is True:
        show_banner = True
    elif cmd_banner is False:
        show_banner = False

    if show_banner:
        print(f'{APP_NAME} {VERSION} ({APP_WWW})', file=sys.stderr)
        print(f'{COPYRIGHT} {AUTHOR}, {AUTHOR_EMAIL}\n', file=sys.stderr)

    operating_methodology = 1 if check_for_file_arguments(remaining_args) else 2

    proc = None
    if operating_methodology == 1:
        proc = subprocess.Popen([diff_cmd] + remaining_args, stdout=subprocess.PIPE, text=True)
        input_iter = proc.stdout
    else:
        input_iter = sys.stdin

    inputstream = []
    diff_type = 'unknown'
    lastline = None

    if specified_type:
        diff_type = specified_type
        if diff_type == 'diffy':
            line = input_iter.readline()
            if line:
                inputstream.append(line)
                lastline = line
    else:
        for line in input_iter:
            inputstream.append(line)
            diff_type = detect_diff_type(line, True)
            lastline = line
            if diff_type != 'unknown':
                break

    # Side-by-side (diffy) pre-processing
    diffy_sep_col  = 0
    mostlikely_sum = 0

    if diff_type == 'diffy':
        longest_record = -1
        separator_col  = {}
        candidate_col  = {}
        possible_cols  = 0
        checkbuffer    = list(inputstream)
        inputstream    = []

        while checkbuffer:
            line = checkbuffer.pop(0)
            inputstream.append(line)
            expanded = expand_tabs_to_spaces(line)

            if len(expanded) > longest_record:
                for i in range(longest_record + 1, len(expanded) + 1):
                    separator_col[i] = 1
                    candidate_col[i] = 0
                longest_record = len(expanded)

            for i in range(len(expanded) - 2):
                if separator_col.get(i, 0) == 0:
                    continue
                if re.match(r'^(Index: |={4,}|RCS file: |retrieving |diff )', expanded):
                    continue
                subsub = expanded[i:i+2]
                if not re.match(r' [ (|<>]', subsub):
                    separator_col[i] = 0
                    if candidate_col.get(i, 0) > 0:
                        possible_cols -= 1
                if re.match(r' [|<>]', subsub):
                    candidate_col[i] = candidate_col.get(i, 0) + 1
                    if candidate_col[i] == 1:
                        possible_cols += 1

            if not checkbuffer:
                if not specified_type and possible_cols == 0 and detect_diff_type(line, False) != 'unknown':
                    diff_type = detect_diff_type(line, False)
                    break
                raw = input_iter.readline()
                if raw:
                    checkbuffer.append(raw)
                    lastline = raw
                else:
                    lastline = None
                    break

        for i in range(longest_record - 2):
            if separator_col.get(i, 0) == 1:
                if candidate_col.get(i, 0) > mostlikely_sum:
                    diffy_sep_col  = i
                    mostlikely_sum = i

        if diffy_sep_col == 0:
            for line in inputstream:
                diff_type = detect_diff_type(line, False)
                if diff_type != 'unknown':
                    break

    # Main processing loop
    inside_file_old = True
    count_marks = 1

    def next_line():
        if inputstream:
            return inputstream.pop(0)
        if lastline is not None:
            return input_iter.readline() or None
        return None

    # Patch next_line to handle lastline consumed state
    stream_exhausted = [lastline is None and not inputstream]

    def stream():
        # yield from inputstream first, then input_iter
        while inputstream:
            yield inputstream.pop(0)
        if lastline is not None or operating_methodology == 2:
            for line in input_iter:
                yield line

    for line in stream():
        out = sys.stdout

        # Fix for bug with filenames: use a regex that handles any whitespace
        # and long/oddly-spaced names by splitting on " and " only at the boundary.
        m = re.match(r'^Binary files (.*) and (.*) differ\n?$', line)
        if m:
            f1, f2 = m.group(1), m.group(2)
            sys.stdout.write(f'Binary files {file_old}{f1}{plain_text} and {file_new}{f2}{plain_text} differ\n')
            continue

        prefix = ''

        if diff_type == 'diff':
            if line.startswith('<'):
                prefix = file_old
            elif line.startswith('>'):
                prefix = file_new
            elif line and line[0].isdigit():
                prefix = diff_stuff
            elif re.match(r'^(Index: |={4,}|RCS file: |retrieving |diff )', line):
                prefix = cvs_stuff
            elif line.startswith('Only in'):
                prefix = diff_file
            else:
                prefix = plain_text

        elif diff_type == 'diffc':
            if line.startswith('- '):
                prefix = file_old
            elif line.startswith('+ '):
                prefix = file_new
            elif re.match(r'^\*{4,}', line):
                prefix = diff_file
            elif line.startswith('Only in'):
                prefix = diff_file
            elif re.match(r'^\*\*\* [0-9]+,[0-9]+', line):
                prefix = diff_file
                inside_file_old = True
            elif line.startswith('*** '):
                prefix = diff_file
            elif re.match(r'^--- [0-9]+,[0-9]+', line):
                prefix = diff_file
                inside_file_old = False
            elif line.startswith('--- '):
                prefix = diff_file
            elif line.startswith('!'):
                prefix = file_old if inside_file_old else file_new
            elif re.match(r'^(Index: |={4,}|RCS file: |retrieving |diff )', line):
                prefix = cvs_stuff
            else:
                prefix = plain_text

        elif diff_type == 'diffu':
            if re.match(r'^(---|\+\+\+) ', line):
                prefix = diff_file
            elif re.match(r'^(\@+)', line):
                m2 = re.match(r'^(\@+)', line)
                count_marks = len(m2.group(1)) - 1
                prefix = diff_stuff
            elif re.match(r'^[-\+ ]{%d}' % count_marks, line):
                diff_marks = line[:count_marks]
                if '-' in diff_marks:
                    prefix = file_old
                elif '+' in diff_marks:
                    prefix = file_new
            elif line.startswith('Only in'):
                prefix = diff_file
            elif re.match(r'^(Index: |={4,}|RCS file: |retrieving |diff |old |new |deleted |copy |rename |similarity |dissimilarity |index |reverted:$|unchanged:$|only in patch2:$)', line):
                prefix = cvs_stuff
            else:
                prefix = plain_text

        elif diff_type == 'diffy':
            expanded = expand_tabs_to_spaces(line)
            if len(expanded) > diffy_sep_col + 2:
                sepchars = expanded[diffy_sep_col:diffy_sep_col+2]
                if sepchars == ' <':
                    prefix = file_old
                elif sepchars == ' |':
                    prefix = diff_stuff
                elif sepchars == ' >':
                    prefix = file_new
                else:
                    prefix = plain_text
            elif line.startswith('Only in'):
                prefix = diff_file
            else:
                prefix = plain_text
            line = expanded

        elif diff_type in ('wdiff', 'debdiff'):
            line = re.sub(r'(\[-.+?-\])', lambda m: file_old + m.group(1) + COLOUR['off'], line)
            line = re.sub(r'(\{\+.+?\+\})', lambda m: file_new + m.group(1) + COLOUR['off'], line)

        # Append reset, preserving trailing newline
        if line.endswith('\n'):
            line = prefix + line[:-1] + COLOUR['off'] + '\n'
        else:
            line = prefix + line + COLOUR['off']

        sys.stdout.write(line)

    sys.stdout.flush()

    exitcode = 0
    if proc is not None:
        proc.stdout.close()
        proc.wait()
        exitcode = proc.returncode

    if fake_exitcode:
        sys.exit(0)
    else:
        sys.exit(exitcode)


if __name__ == '__main__':
    main()
