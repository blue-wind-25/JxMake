JxMake colour themes for Kate
=============================

../kate_jxMake.xml has no fixed colours: it names Kate's built-in styles, so a JxMake file is readable in any Kate
theme. Some themes give two styles the same colour, though. The themes here avoid that: they carry the JxMake colours
for every item, in light and dark. Other languages look the same as in the base theme.

    breeze-light-jxmake.theme        Breeze Light        the JxMake palette exactly
    tokyo-night-light-jxmake.theme   Tokyo Night Light   the palette, darkened where needed (contrast 3:1 on its grey)
    breeze-dark-jxmake.theme         Breeze Dark         lightness inverted, hue kept (contrast 4.5:1)


Install (KF5 / KF6)
-------------------
    mkdir -p ~/.local/share/org.kde.syntax-highlighting/themes ~/.local/share/org.kde.syntax-highlighting/syntax
    ln -sf "$PWD"/shf/kate_themes/*.theme ~/.local/share/org.kde.syntax-highlighting/themes/
    ln -sf "$PWD"/shf/kate_jxMake.xml     ~/.local/share/org.kde.syntax-highlighting/syntax/

(From the repo root. Links follow the repo, so a pull updates Kate; use cp instead for a copy that stays as it is.)

Restart Kate, then choose the theme in Settings > Configure Kate > Colour Themes. Kate's automatic selection (follow
the system's light / dark setting) picks a theme by itself and has no setting for which one: it may show a theme other
than these. JxMake files stay readable either way; to be sure of these colours, turn it off and choose the theme.


Which Kate
----------
    KF6, KF5 5.75 or newer   XML and themes
    KF5 before 5.75          XML only: Kate uses JSON themes since Frameworks 5.75 (October 2020), an older one should
                             just not list these files; colours come from its own schema
    KDE 4                    neither: use ../kate_jxMake-KDE4.xml, which keeps its fixed colours for light backgrounds

Only KF6 6.31 was tested. The rest follows KDE's documentation.


Changing colours
----------------
Do not edit the .theme files by hand: they are generated. Edit generate_kate_themes.py, then run it:

    python3 shf/kate_themes/generate_kate_themes.py            write the .theme files
    python3 shf/kate_themes/generate_kate_themes.py --check    only print the contrast figures

    PALETTE          the colour of each item on a light background
    DARK_OVERRIDES   one colour by hand on dark themes, where the calculated one looks wrong
    THEMES           which base themes to use, and each one's contrast floor

A new item in ../kate_jxMake.xml's itemDatas needs a line in PALETTE, under the same name. To add a theme, copy it to
base/ and add a line to THEMES. Raise a generated theme's "revision" in the script if Kate must prefer a new copy.


Files in base/
--------------
Unmodified copies of KDE's themes, MIT licence (copyright and licence in each file's metadata, kept in the generated
themes too), from https://invent.kde.org/frameworks/syntax-highlighting/-/tree/master/data/themes, fetched 2026-11-02:

    breeze-dark.theme         b69a392e8297cee7b10d8c7b7e0fd3e918dbfe646f6faa617a4a5fd396a978a1
    breeze-light.theme        d25beb302038e42d42e9857501701cea8b88b01c493457e7a166ae3654b9e366
    tokyo-night-light.theme   264a78df516f02f0d17282674097693fbcf6edf5423643c0875b5140047e4215
