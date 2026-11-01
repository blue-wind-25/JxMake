#!/usr/bin/env python3
# Generates the JxMake Kate colour themes (see README.txt): base/<theme>.theme + the JxMake colours below, as the
# theme's "custom-styles" for the JxMake language. Dark themes get each colour's lightness inverted (hue kept), then
# raised until it is readable on the theme's background. Light themes get PALETTE as it is, darkened only up to the
# theme's contrast floor (Breeze Light has none: exactly PALETTE).
#
#   python3 generate_kate_themes.py          write the .theme files next to this script
#   python3 generate_kate_themes.py --check  only print each dark colour's contrast and the closest pair
import colorsys
import json
import os
import sys

LANGUAGE = 'JxMake Specification File'

# itemData name in ../kate_jxMake.xml -> colour on a light background.
PALETTE = {
    'NormalText': '#000000', 'ErrorText': '#FF0000', 'CombineNL': '#808080', 'ForceNL': '#FF0000',
    'Comment': '#808080', 'Keyword': '#000000', 'SKeyword': '#000000', 'MacroDef': '#000000',
    'MacroUseMark': '#FF3F3F', 'MacroUse': '#000000', 'IncLibName': '#000000', 'Number': '#B08000',
    'String': '#BF0000', 'StringMLMarker': '#BF0000', 'StringDQF': '#007F7F', 'ODString': '#007F7F',
    'EscapeSeq': '#005AB3', 'PROperEval': '#D08290', 'SROperEval': '#B300B3', 'RefVarMark': '#B300B3',
    'RVariableEval': '#006A00', 'SVariableEval': '#5A00B3', 'SVarShortcut': '#000000', 'VariableEvalN': '#FF3F3F',
    'ANSIEscapeCode': '#404040', 'SuppressError': '#0000BF', 'BFunctionCall': '#0000BF', 'UFunctionName': '#007F7F',
    'ROperator': '#CF6E00', 'SOperator': '#B300B3', 'SOperatorIF': '#B300B3', 'Command': '#C8A4A2',
    'CommandSpcXDir': '#C8A4A2', 'RegExprM': '#50C0C0', 'RegExpr1': '#5F5FBF', 'RegExpr2': '#BF5F5F',
}

# Set one colour by hand on dark themes, in place of the calculated one: {'String': '#FF8080'}.
DARK_OVERRIDES = {}

# base file -> (new theme's name, is it dark, contrast floor; 4.5 = WCAG AA for normal text, 1 = none).
THEMES = {
    'breeze-light': ('Breeze Light JxMake', False, 1.0),
    'tokyo-night-light': ('Tokyo Night Light JxMake', False, 3.0),
    'breeze-dark': ('Breeze Dark JxMake', True, 4.5),
}

MAX_LIGHTNESS = 0.92  # black must not become glaring white


def rgb(h):
    return tuple(int(h[i:i + 2], 16) / 255.0 for i in (1, 3, 5))


def hexs(c):
    return '#%02x%02x%02x' % tuple(int(round(x * 255)) for x in c)


def luminance(h):
    c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in rgb(h)]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def contrast(a, b):
    la, lb = sorted((luminance(a), luminance(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)


def to_dark(colour, background, floor):
    hue, light, sat = colorsys.rgb_to_hls(*rgb(colour))
    light = min(1.0 - light, MAX_LIGHTNESS)
    out = hexs(colorsys.hls_to_rgb(hue, light, sat))
    while contrast(out, background) < floor and light < 1.0:
        light = min(light + 0.01, 1.0)
        out = hexs(colorsys.hls_to_rgb(hue, light, sat))
    return out


def to_light(colour, background, floor):
    hue, light, sat = colorsys.rgb_to_hls(*rgb(colour))
    out = colour
    while contrast(out, background) < floor and light > 0.0:
        light = max(light - 0.01, 0.0)
        out = hexs(colorsys.hls_to_rgb(hue, light, sat))
    return out


def colours_for(dark, background, floor):
    if not dark:
        return {n: to_light(c, background, floor) for n, c in PALETTE.items()}
    return {n: DARK_OVERRIDES.get(n) or to_dark(c, background, floor) for n, c in PALETTE.items()}


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    check = '--check' in sys.argv[1:]
    for base, (name, dark, floor) in THEMES.items():
        with open(os.path.join(here, 'base', base + '.theme'), encoding='utf-8') as f:
            theme = json.load(f)
        background = theme['editor-colors']['BackgroundColor']
        colours = colours_for(dark, background, floor)
        low = sorted((round(contrast(c, background), 1), n) for n, c in colours.items())
        print('%-26s background %s, %d distinct colours, lowest contrast %s' % (name, background, len(set(colours.values())), low[:3]))
        if check:
            continue
        theme['metadata']['name'] = name
        theme['metadata']['revision'] = 1
        styles = {n: {'text-color': c, 'selected-text-color': c} for n, c in colours.items()}
        theme.setdefault('custom-styles', {})[LANGUAGE] = styles
        with open(os.path.join(here, name.lower().replace(' ', '-') + '.theme'), 'w', encoding='utf-8') as f:
            json.dump(theme, f, indent=4, sort_keys=False)
            f.write('\n')


if __name__ == '__main__':
    main()
