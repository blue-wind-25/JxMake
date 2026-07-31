/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.Config;
import com.jxmake.formatter.FormatterCore;
import com.jxmake.formatter.Lang;

/**
 * Real formatting logic for XML (STYLE_DATA_FORMATS.md §2), shared with HTML5 (gate internally on
 * {@code lang.isHtml5} for HTML5-only additions -- void elements, the `<script>`/`<style>`
 * embedded-content dispatcher -- neither implemented yet), per RDD_KEY_188. A from-scratch
 * character-cursor recursive-descent parser -- XML's tag/attribute grammar has no natural line
 * boundary the way YAML's indentation-significant grammar does, so (unlike {@link YamlSpecificRule})
 * this is neither line-based nor a reuse of {@code TokenizerCore}/{@code Token} (brace-delimited,
 * imperative-language shaped -- not a fit for tag nesting). Implements its own {@code <!--%}-based
 * {@code JXM_CFMT_DIS}/{@code ENA} frozen-span detection (line-anchored, mirroring
 * {@link YamlSpecificRule}/{@link TomlSpecificRule}'s independent implementations) and its own
 * comment-start-case normalization (comment inner text has no leading delimiter char to skip, unlike
 * {@code #}/{@code //}, so it is simpler than either sibling's {@code normComment}).
 */
public final class XmlSpecificRule {

    public static final class XmlParseException extends RuntimeException {

        public XmlParseException(final String message)
        {
            super(message);
        }

    } // class XmlParseException

    private enum NodeType {

        PI, DOCTYPE, COMMENT, ELEMENT, TEXT, CDATA, FROZEN, RAW, OPAQUE

    } // enum NodeType

    /**
     * HTML5 void elements (never a closing tag; any self-closing `/` is normalized away),
     *  per STYLE_DATA_FORMATS.md §4.1.
     */
    private static final java.util.Set<String> VOID_ELEMENTS = new java.util.HashSet<>( java.util.Arrays.asList(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param",
        "source", "track", "wbr"
    ) );

    /**
     * `<script>` MIME types that mean "this is JavaScript" per HTML5 semantics -- anything else
     *  (or a recognized non-executable type such as `application/json`) is left fully opaque
     */
    private static final java.util.Set<String> JS_SCRIPT_TYPES = new java.util.HashSet<>( java.util.Arrays.asList(
        "text/javascript", "application/javascript", "application/ecmascript",
        "text/ecmascript", "module"
    ) );

    /**
     * HTML5 tree-construction spec tag-name rewrites: a start tag whose name (lowercased) is a key
     *  here is renamed to the mapped value and reprocessed as that element instead -- currently just
     *  `image` -> `img` (a real, spec-mandated quirk, confirmed via real WPT dogfood input,
     *  `speculative-parsing/**\/resources/image-src-framed.sub.html` etc.). Map (not a single constant)
     *  so any future tag-name rewrite the spec turns out to need is one entry, no new code -- per the
     *  spec's tree-construction algorithm this is currently the only such rewrite ("isindex" is a
     *  different, much larger obsolete-element-expansion quirk, not a simple rename, and out of scope).
     *  Only applies to HTML content -- callers must additionally gate on not being inside real foreign
     *  content (see {@link #svgDepth}) since e.g. a real `<image>` inside `<svg>` is a legitimate SVG
     *  element, not this quirk.
     */
    private static final java.util.Map<String, String> TAG_NAME_REWRITES = new java.util.HashMap<>();
    static {
        TAG_NAME_REWRITES.put("image", "img");
    }

    /**
     * HTML5 tree-construction spec's "Adjust SVG tag names" step: inside real SVG foreign content,
     *  a start tag whose lowercased name is a key here is corrected to the mapped mixed-case spec
     *  name (SVG is XML-based/case-sensitive even though HTML5 parsing is otherwise case-insensitive).
     *  Opposite gating from {@link #TAG_NAME_REWRITES}: that map applies only OUTSIDE svg
     *  ({@code svgDepth == 0}), this one applies only INSIDE svg ({@code svgDepth > 0}) -- kept as a
     *  separate map/gate rather than merged, since the two conditions are mutually exclusive. Table is
     *  the WHATWG HTML5 spec's stable "Adjust SVG tag names" list (unchanged for years). MathML has an
     *  analogous small case-fixup (e.g. `definitionurl` -> `definitionURL`, attribute-only) but this
     *  parser has no MathML-depth-equivalent tracking today (only `svgDepth`), so per the standalone
     *  scoping of this task, MathML fixup is intentionally left open rather than building new MathML
     *  foreign-content tracking from scratch -- see STATE_DATA_FORMATS.md.
     */
    private static final java.util.Map<String, String> SVG_TAG_NAME_CASE_FIXUP = new java.util.HashMap<>();
    static {
        SVG_TAG_NAME_CASE_FIXUP.put("altglyph", "altGlyph");
        SVG_TAG_NAME_CASE_FIXUP.put("altglyphdef", "altGlyphDef");
        SVG_TAG_NAME_CASE_FIXUP.put("altglyphitem", "altGlyphItem");
        SVG_TAG_NAME_CASE_FIXUP.put("animatecolor", "animateColor");
        SVG_TAG_NAME_CASE_FIXUP.put("animatemotion", "animateMotion");
        SVG_TAG_NAME_CASE_FIXUP.put("animatetransform", "animateTransform");
        SVG_TAG_NAME_CASE_FIXUP.put("clippath", "clipPath");
        SVG_TAG_NAME_CASE_FIXUP.put("feblend", "feBlend");
        SVG_TAG_NAME_CASE_FIXUP.put("fecolormatrix", "feColorMatrix");
        SVG_TAG_NAME_CASE_FIXUP.put("fecomponenttransfer", "feComponentTransfer");
        SVG_TAG_NAME_CASE_FIXUP.put("fecomposite", "feComposite");
        SVG_TAG_NAME_CASE_FIXUP.put("feconvolvematrix", "feConvolveMatrix");
        SVG_TAG_NAME_CASE_FIXUP.put("fediffuselighting", "feDiffuseLighting");
        SVG_TAG_NAME_CASE_FIXUP.put("fedisplacementmap", "feDisplacementMap");
        SVG_TAG_NAME_CASE_FIXUP.put("fedistantlight", "feDistantLight");
        SVG_TAG_NAME_CASE_FIXUP.put("fedropshadow", "feDropShadow");
        SVG_TAG_NAME_CASE_FIXUP.put("feflood", "feFlood");
        SVG_TAG_NAME_CASE_FIXUP.put("fefunca", "feFuncA");
        SVG_TAG_NAME_CASE_FIXUP.put("fefuncb", "feFuncB");
        SVG_TAG_NAME_CASE_FIXUP.put("fefuncg", "feFuncG");
        SVG_TAG_NAME_CASE_FIXUP.put("fefuncr", "feFuncR");
        SVG_TAG_NAME_CASE_FIXUP.put("fegaussianblur", "feGaussianBlur");
        SVG_TAG_NAME_CASE_FIXUP.put("feimage", "feImage");
        SVG_TAG_NAME_CASE_FIXUP.put("femerge", "feMerge");
        SVG_TAG_NAME_CASE_FIXUP.put("femergenode", "feMergeNode");
        SVG_TAG_NAME_CASE_FIXUP.put("femorphology", "feMorphology");
        SVG_TAG_NAME_CASE_FIXUP.put("feoffset", "feOffset");
        SVG_TAG_NAME_CASE_FIXUP.put("fepointlight", "fePointLight");
        SVG_TAG_NAME_CASE_FIXUP.put("fespecularlighting", "feSpecularLighting");
        SVG_TAG_NAME_CASE_FIXUP.put("fespotlight", "feSpotLight");
        SVG_TAG_NAME_CASE_FIXUP.put("fetile", "feTile");
        SVG_TAG_NAME_CASE_FIXUP.put("feturbulence", "feTurbulence");
        SVG_TAG_NAME_CASE_FIXUP.put("foreignobject", "foreignObject");
        SVG_TAG_NAME_CASE_FIXUP.put("glyphref", "glyphRef");
        SVG_TAG_NAME_CASE_FIXUP.put("lineargradient", "linearGradient");
        SVG_TAG_NAME_CASE_FIXUP.put("radialgradient", "radialGradient");
        SVG_TAG_NAME_CASE_FIXUP.put("textpath", "textPath");
    }

    /**
     * HTML5 elements whose children rely on the spec's implied-end-tag tree-construction rule
     *  (e.g. `<rb>`/`<rt>`/`<rp>`/`<rtc>` inside `<ruby>` never carry an explicit closing tag in
     *  valid markup) -- rather than modeling the full per-element-family implied-closing-trigger
     *  spec (a large feature, RDD_KEY_198), each name here is instead scanned as one opaque,
     *  byte-for-byte-verbatim span from its opening tag to its own MATCHING closing tag (nested
     *  same-name opens/closes tracked), reusing the same "don't parse the interior, just find the
     *  matching close" pattern {@link #finishRawTextElement}/{@link #finishRawElement} already use
     *  for `<script>`/`<style>`/`<pre>`. Extend by adding a name here only -- no other code change
     *  needed for a simple case; do not add per-element implied-closing-trigger logic.
     */
    private static final java.util.Set<String> OPAQUE_IMPLIED_END_TAG_ELEMENTS = new java.util.HashSet<>(
            java.util.Arrays.asList("ruby") );

    /**
     * Real HTML5 tag names, used only by {@link #isMarkupFragmentDirective} to recognize a comment
     *  whose content is a leftover markup fragment (e.g. a commented-out {@code <tr>...</tr>} or
     *  {@code <p>...</p>} block where the author's {@code <!--}/{@code <} boundary landed mid-tag,
     *  leaving the fragment's first "word" a bare tag-name-open token like {@code tr>}/{@code p>}
     *  rather than a capitalizable English sentence). Deliberately a closed set of real tag names
     *  (not "any lowercase word immediately followed by {@code >}") -- see that method's Javadoc for
     *  the corpus evidence this restriction is based on.
     */
    private static final java.util.Set<String> MARKUP_FRAGMENT_TAG_NAMES = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "html", "head", "body", "div", "span", "p", "a", "ul", "ol", "li", "dl", "dt", "dd",
            "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption", "colgroup", "col",
            "form", "input", "button", "select", "option", "optgroup", "label", "textarea",
            "fieldset", "legend", "img", "br", "hr", "script", "style", "link", "meta", "title",
            "base", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "code", "blockquote", "section",
            "article", "header", "footer", "nav", "main", "aside", "figure", "figcaption", "em",
            "strong", "b", "i", "u", "small", "sub", "sup", "q", "kbd", "var", "samp", "cite",
            "abbr", "dfn", "time", "mark", "ruby", "rt", "rp", "rb", "rtc", "template", "dialog",
            "details", "summary", "canvas", "svg", "iframe", "embed", "object", "param", "video",
            "audio", "source", "track", "map", "area"
        )
    );

    /**
     * General, reusable "implied-closing trigger" table: an element name registered here is parsed
     *  as a REAL node (attributes/children/normal rendering, unlike {@link #OPAQUE_IMPLIED_END_TAG_ELEMENTS}'
     *  whole-span opaque capture) whose children stop -- implying an unwritten closing tag -- as soon
     *  as one of the mapped sibling start-tag names begins, in addition to the existing "parent's own
     *  closing tag also ends me" behavior every element already gets via {@code parseNodes}'s
     *  {@code stopAtCloseTag}. Populate with one entry per element only once real dogfood input needs
     *  it -- currently `option` (closes on a sibling `<option>`/`<optgroup>` start, or when its
     *  parent `<select>`/`<datalist>`/`<optgroup>` closes), `head` (closes on a sibling `<body>`
     *  start, confirmed via real WPT dogfood input, `meta-inhead-insertion-mode.html`), and `p`
     *  (RDD_KEY_204 -- closes on any of the HTML5 spec's fixed "close a p element" trigger-tag
     *  list, confirmed via real `apache/ant` `manual/` dogfood input: a `<p>...` paragraph with no
     *  explicit `</p>` before a following `<h3>` caused the parser to swallow the rest of the
     *  document as that `<p>`'s children until the first unrelated closing tag anywhere downstream,
     *  producing a spurious duplicate `</p>` at the very end). Do NOT add `li`/`td`/`tr`/etc.
     *  speculatively without similar real dogfood evidence -- see STATE_DATA_FORMATS.md's Open
     *  Questions/RDD_LOG.md for the rationale.
     */
    private static final java.util.Map<String, java.util.Set<String>> IMPLIED_CLOSE_TRIGGERS = new java.util.HashMap<>();
    static {
        IMPLIED_CLOSE_TRIGGERS.put(
            "option", new java.util.HashSet<>( java.util.Arrays.asList("option", "optgroup") )
        );
        IMPLIED_CLOSE_TRIGGERS.put(
            "head", new java.util.HashSet<>( java.util.Arrays.asList("body") )
        );
        // HTML5 spec's fixed "close a p element" trigger-tag list (the set of start tags that
        // implicitly close an open <p> with no explicit </p>)
        IMPLIED_CLOSE_TRIGGERS.put(
            "p", new java.util.HashSet<>( java.util.Arrays.asList("address", "article", "aside", "blockquote", "details", "div", "dl", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "main", "menu", "nav", "ol", "p", "pre", "section", "table", "ul") )
        );
    }

    private static final class Node {

        NodeType     type;
        String       raw;                       // PI / DOCTYPE / CDATA / TEXT: verbatim content
        String       commentText;               // COMMENT: normalized inner text
        List<String> frozenLines;               // FROZEN: raw lines, verbatim, DIS..ENA inclusive
        String       tagName;
        List<String> attrs = new ArrayList<>();
        boolean      selfClosing;
        List<Node>   children;                  // null if self-closing; empty list if open/close-with-nothing
        String       trailingComment;           // Normalized text of a same-line trailing comment, or null

    } // class Node

    private final Lang    lang;
    private final int     lineLengthLimit;
    private final int     indentWidth;
    private final boolean useTabs;
    private final boolean normalizeCommentStartCase;
    /**
     * Real resolved Config of the enclosing HTML file, threaded through so a spliced
     *  {@code <script>} block inherits every JS/TS-specific config key (e.g. `js-import-order`),
     *  not just the 4 primitive fields above. May be null (legacy/test constructors) -- in that
     *  case {@link #renderScriptOrStyle} falls back to a throwaway {@code Config.resolve(null, ...)}
     *  built from those 4 fields, same as before this was threaded through.
     */
    private final Config enclosingConfig;

    private String s;
    private int    pos;
    /**
     * Depth counter for `<svg>` ancestors, tracked only so the HTML5 "image" -> "img" tag-name
     *  rewrite (see {@link #parseElement}) can be correctly scoped to HTML content only -- inside
     *  real SVG foreign content, `<image>` is a legitimate SVG element with its own closing tag, not
     *  a quirk alias for `<img>`. Confirmed via real WPT dogfood input (`svg-image-href.tentative.html`
     *  etc., which nest a real `<image>`/`</image>` pair inside `<svg>`).
     */
    private int svgDepth;

    /**
     * Lightweight name-only stack of currently-open element tag names (lowercased), pushed in
     *  {@link #parseElement} right after a start tag is recognized and popped on every return path
     *  (matched close, implied-close-trigger path, and the tolerant-close fallback alike). Lets
     *  {@link #parseNodes} distinguish, at a closing tag encountered mid-children-parse, "this name
     *  matches something actually open on the path from the document root to here" (a legitimate
     *  cascade-close -- the mechanism WPT's {@code charset/after-bogus.html} mismatched-tag case
     *  relies on) from "this name matches nothing open anywhere" (a genuine orphan close tag, e.g.
     *  apache/ant's `manual/running.html` stray {@code </p>} with no open {@code <p>} at all --
     *  see STATE_DATA_FORMATS.md's Open Questions item 2). Deliberately NOT the full per-insertion-
     *  mode HTML5 tree-construction state -- just enough to make that one distinction.
     */
    private final java.util.Deque<String> openTagStack = new java.util.ArrayDeque<>();

    public XmlSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this(lang, lineLengthLimit, indentWidth, "tabs", true);
    }

    public XmlSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final String  indentStyle,
        final boolean normalizeCommentStartCase
    )
    {
        this(lang, lineLengthLimit, indentWidth, indentStyle, normalizeCommentStartCase, null);
    }

    public XmlSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final Config  enclosingConfig
    )
    {
        this.lang                      = lang;
        this.lineLengthLimit           = lineLengthLimit;
        this.indentWidth               = indentWidth;
        this.useTabs                   = "tabs".equals(indentStyle);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.enclosingConfig           = enclosingConfig;
    }

    private String indent(final int depth)
    {
        if(useTabs) {
            final StringBuilder sb = new StringBuilder();
            for(int i = 0; i < depth; ++i) sb.append('\t');
            return sb.toString();
        }
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < depth * indentWidth; ++i) sb.append(' ');

        return sb.toString();
    }

    // ---- cursor helpers ----

    private boolean eof()
    {
        return pos >= s.length();
    }

    private boolean startsWith(final String tok)
    {
        return s.regionMatches( pos, tok, 0, tok.length() );
    }

    /**
     * Case-insensitive `</tagName>` (or `</tagName `) check at the cursor, used only for HTML5
     *  closing-tag matching against an {@code n.tagName} that may have been case-rewritten from the
     *  source's own literal casing ({@link #TAG_NAME_REWRITES}/{@link #SVG_TAG_NAME_CASE_FIXUP}) --
     *  e.g. source `<fegaussianblur>...</fegaussianblur>` becomes `n.tagName ==
     *  "feGaussianBlur"`, so a literal-case match against the source's own lowercase closing tag would
     *  never succeed without this. HTML5 tag-name matching is spec-mandated case-insensitive; other
     *  languages (XML/XHTML-family) keep the exact-case {@link #startsWith(String)} check since they
     *  never rewrite tag names.
     */
    private boolean startsWithCloseTagIgnoreCase(final String tagName)
    {
        if( !startsWith("</") ) return false;
        final int nameStart = pos + 2;
        final int nameLen   = tagName.length();
        if( nameStart + nameLen > s.length() || !s.regionMatches(
            true, nameStart, tagName, 0, nameLen
        ) ) return false;
        if( nameStart + nameLen >= s.length() ) return false;
        final char c = s.charAt(nameStart + nameLen);

        return c == '>' || Character.isWhitespace(c);
    }

    private void skipWs()
    {
        while( !eof() && Character.isWhitespace( s.charAt(pos) ) ) pos++;
    }

    private void skipInlineWs()
    {
        while( !eof() && ( s.charAt(pos) == ' ' || s.charAt(pos) == '\t' ) ) pos++;
    }

    private String currentLineTrimmed()
    {
        int end = s.indexOf('\n', pos);
        if(end < 0) end = s.length();

        return s.substring(pos, end).trim();
    }

    // ---- top-level ----

    public String format(final String content)
    {
        this.s   = content;
        this.pos = 0;
        final List<Node> nodes = parseNodes(false);
        if( !eof() ) throw new XmlParseException(
            "trailing content after document, near: " + s.substring( pos, Math.min( s.length(), pos + 40 ) )
        );
        final StringBuilder out = new StringBuilder();
        renderNodes(nodes, 0, out);

        return out.toString();
    }

    private List<Node> parseNodes(final boolean stopAtCloseTag)
    {
        return parseNodes(stopAtCloseTag, null);
    }

    /**
     * @param impliedCloseTriggers when non-null, children stop being consumed (implying the
     *  currently-open element is closed, with no explicit closing tag) as soon as upcoming input is a
     *  start tag whose name is in this set -- see {@link #IMPLIED_CLOSE_TRIGGERS}
     */
    private List<Node> parseNodes(
        final boolean               stopAtCloseTag,
        final java.util.Set<String> impliedCloseTriggers
    )
    {
        final List<Node> nodes = new ArrayList<>();
        while(true) {
            skipWs();
            if( eof() ) break;
            if( stopAtCloseTag && startsWith("</") ) {
                // See openTagStack's own javadoc / STATE_DATA_FORMATS.md's Open Questions item 2:
                // a closing tag here either matches the element currently being parsed or one of its
                // real ancestors (legitimate cascade-close, unchanged from prior behavior -- keep
                // breaking out to let the caller chain handle it), or it's a genuine orphan (matches
                // nothing open anywhere) that should be discarded in place rather than incorrectly
                // cascading a tolerant-close all the way up to <body>/<html>. Only meaningful for
                // HTML5's error-tolerant posture -- strict XML/XHTML keeps the original unconditional
                // break (a mismatched close tag there is a real document error, not tolerated).
                if( !lang.isHtml5 || openTagStack.contains( peekCloseTagNameLower() ) ) break;
                final int gt = s.indexOf('>', pos);
                pos = gt >= 0 ? gt + 1 : s.length();
                continue;
            } // if
            if( impliedCloseTriggers != null && startsWithTriggerTag(impliedCloseTriggers) ) break;
            if( lang.isHtml5 && !stopAtCloseTag && startsWith("</") ) {
                // Document-root-level stray closing tag with no corresponding open element anywhere
                // in this recursive-descent parse (e.g. one of the genuinely deep tree-construction
                // gaps documented in STATE_DATA_FORMATS.md's Open Questions -- adoption agency,
                // foreign-content foster-parenting -- can leave one of these bubbling all the way up).
                // Per the same "HTML5 parsing must never crash" posture as parseElement's implicit-close
                // fallback, silently discard the stray tag and keep going rather than throwing.
                final int gt = s.indexOf('>', pos);
                pos = gt >= 0 ? gt + 1 : s.length();
                continue;
            } // if
            final Node node = parseSingleNode();
            attachTrailingCommentIfAny(node);
            nodes.add(node);
        } // while

        return nodes;
    }

    /**
     * True if the cursor is positioned at a start tag (not a closing tag) whose lowercased name is
     *  in {@code triggers}. Used by {@link #IMPLIED_CLOSE_TRIGGERS}.
     */
    private boolean startsWithTriggerTag(final java.util.Set<String> triggers)
    {
        if( !startsWith("<") || startsWith("</") ) return false;
          int i         = pos + 1;
    final int nameStart = i;
        while( i < s.length() && !Character.isWhitespace(
            s.charAt(i)
        ) && s.charAt(
            i
        ) != '/' && s.charAt(
            i
        ) != '>' ) i++;
        final String tag = s.substring(nameStart, i).toLowerCase(java.util.Locale.ROOT);

        return triggers.contains(tag);
    }

    /**
     * Lowercased tag name of the closing tag at the cursor (which must already be positioned at
     *  `</`) -- used by {@link #openTagStack}'s ancestor/orphan check.
     */
    private String peekCloseTagNameLower()
    {
        int              i     = pos + 2;
        final int        start = i;
        while( i < s.length() && s.charAt(i) != '>' && !Character.isWhitespace( s.charAt(i) ) ) i++;

        return s.substring(start, i).toLowerCase(java.util.Locale.ROOT);
    }

    private void attachTrailingCommentIfAny(final Node node)
    {
        final int save = pos;
        skipInlineWs();
        if( startsWith("<!--") ) {
            final int close = s.indexOf("-->", pos + 4);
            if( close >= 0 && s.indexOf('\n', pos) > close ) {
                final String inner = s.substring(pos + 4, close).trim();
                if( !inner.startsWith("%") ) {
                    node.trailingComment = normComment(inner);
                    pos                  = close + 3;
                    return;
                }
            } // if
        } // if
        pos = save;
    }

    private Node parseSingleNode()
    {
        if( startsWith("<?") ) return parsePi();
        if( startsWith("<!--") ) return parseCommentOrFrozen();
        if( startsWith("<!DOCTYPE") || startsWith("<!doctype") ) return parseDoctype();
        if( startsWith("<![CDATA[") ) return parseCdata();
        if( startsWith(
            "</"
        ) ) throw new XmlParseException(
            "unexpected closing tag near: " + s.substring( pos, Math.min( s.length(), pos + 40 ) )
        );
        if( startsWith("<") ) return parseElement();

        return parseText();
    }

    private Node parsePi()
    {
        final int close = s.indexOf("?>", pos + 2);
        if(close < 0) throw new XmlParseException("unterminated processing instruction");
        final Node n = new Node();
        n.type = NodeType.PI;
        n.raw  = s.substring(pos, close + 2);
        pos    = close + 2;

        return n;
    }

    private Node parseCommentOrFrozen()
    {
        if( "<!--% JXM_CFMT_DIS -->".equals( currentLineTrimmed() ) ) {
            final Node n = new Node();
            n.type        = NodeType.FROZEN;
            n.frozenLines = new ArrayList<>();
            while(true) {
                  int     end   = s.indexOf('\n', pos);
            final boolean hasNl = end >= 0;
                if(!hasNl) end = s.length();
                final String line = s.substring(pos, end);
                n.frozenLines.add(line);
                pos = hasNl ? end + 1 : end;
                if( "<!--% JXM_CFMT_ENA -->".equals( line.trim() ) || eof() ) break;
            } // while
            return n;
        } // if
        final int close = s.indexOf("-->", pos + 4);
        if(close < 0) throw new XmlParseException("unterminated comment");
        final Node n = new Node();
        n.type        = NodeType.COMMENT;
        n.commentText = normComment( s.substring(pos + 4, close).trim() );
        pos           = close + 3;

        return n;
    }

    private Node parseDoctype()
    {
        final int start = pos;
              int depth = 0;
        while( !eof() ) {
            final char c = s.charAt(pos);
            if(c == '<') {
                ++depth;
            }
            else if(c == '>') {
                --depth;
                ++pos;
                if(depth == 0) break;
                continue;
            }
            ++pos;
        } // while
        final Node n = new Node();
        n.type = NodeType.DOCTYPE;
        n.raw  = s.substring(start, pos);

        return n;
    }

    private Node parseCdata()
    {
        final int close = s.indexOf("]]>", pos + 9);
        if(close < 0) throw new XmlParseException("unterminated CDATA section");
        final Node n = new Node();
        n.type = NodeType.CDATA;
        n.raw  = s.substring(pos, close + 3);
        pos    = close + 3;

        return n;
    }

    private Node parseText()
    {
        final int start = pos;
              int end   = s.indexOf('<', pos);
        if(end < 0) end = s.length();
        pos = end;
        final Node n = new Node();
        n.type = NodeType.TEXT;
        n.raw  = s.substring(start, end).trim();

        return n;
    }

    private Node parseElement()
    {
        ++pos; // '<'
        final int nameStart = pos;
        while( !eof() && !Character.isWhitespace(
            s.charAt(pos)
        ) && s.charAt(
            pos
        ) != '/' && s.charAt(
            pos
        ) != '>' ) pos++;
        final Node n = new Node();
        n.type    = NodeType.ELEMENT;
        n.tagName = s.substring(nameStart, pos);
        String lowerTag = n.tagName.toLowerCase(java.util.Locale.ROOT);
        // See TAG_NAME_REWRITES -- gated on svgDepth == 0 since real SVG foreign content has its own
        // legitimate <image> element that must NOT be rewritten (see the field's own javadoc)
        if( lang.isHtml5 && svgDepth == 0 && TAG_NAME_REWRITES.containsKey(lowerTag) ) {
            n.tagName = TAG_NAME_REWRITES.get(lowerTag);
            lowerTag  = n.tagName;
        }
        // See SVG_TAG_NAME_CASE_FIXUP -- opposite gate from TAG_NAME_REWRITES: only applies INSIDE
        // real SVG foreign content (svgDepth > 0), never in plain HTML content.
        if( lang.isHtml5 && svgDepth > 0 && SVG_TAG_NAME_CASE_FIXUP.containsKey(
            lowerTag
        ) ) n.tagName = SVG_TAG_NAME_CASE_FIXUP.get(
            lowerTag
        );
        final boolean isSvg  = lang.isHtml5&& "svg".equals(lowerTag);
        final boolean isVoid = lang.isHtml5&& VOID_ELEMENTS.contains(lowerTag);
        if( lang.isHtml5 && OPAQUE_IMPLIED_END_TAG_ELEMENTS.contains(
            lowerTag
        ) ) return parseOpaqueImpliedEndTagElement(
            nameStart - 1, n.tagName, lowerTag
        );
        while(true) {
            skipWs();
            if( eof() ) throw new XmlParseException("unterminated tag <" + n.tagName);
            if( startsWith("/>") ) {
                n.selfClosing  = true;
                pos           += 2;
                break;
            }
            if( startsWith(">") ) {
                pos += 1;
                break;
            }
            n.attrs.add( parseAttr() );
        } // while
        if(isVoid) {
            n.selfClosing = true;
            return n;
        }
        if(n.selfClosing) return n;
        if( lang.isHtml5 && ( "script".equals(
            lowerTag
        ) || "style".equals(
            lowerTag
        ) ) ) return finishRawTextElement(
            n, lowerTag
        );
        if( lang.isHtml5 && "pre".equals(lowerTag) ) return finishRawElement(n, "</pre>");
        if( lang.isHtml5 && "xmp".equals(lowerTag) ) {
            // `<xmp>` is a legacy HTML5 raw-text element (like `<pre>`/`<script>`/`<style>`) --
            // its content (including any literal `<tag>`-looking text) must never be parsed as real
            // child markup, only captured byte-for-byte through the literal closing tag. Missing this
            // case caused a real content-preservation bug found during the `web-platform-tests/wpt`
            // dogfood run: a literal `<script>...</script>` string inside `<xmp>` was mis-parsed as a
            // real nested `<script>` element and re-serialized with different whitespace.
            return finishRawElement(n, "</xmp>");
        } // if
        final java.util.Set<String> impliedTriggers = lang.isHtml5 ? IMPLIED_CLOSE_TRIGGERS.get(
            lowerTag
        ) : null;
        openTagStack.push(lowerTag);
        try {
            if(isSvg) svgDepth++;
            try {
                n.children = parseNodes(true, impliedTriggers);
            }
            finally {
                if(isSvg) svgDepth--;
            }
            final String closeTok         = "</" + n.tagName + ">";
            final int    beforeTrailingWs = pos;
            skipInlineWs();
            if( startsWith(closeTok) || startsWith("</" + n.tagName + " ")
                    || ( lang.isHtml5 && startsWithCloseTagIgnoreCase(n.tagName) ) ) {
                final int gt = s.indexOf('>', pos);
                pos = gt + 1;
                return n;
            }
            if(impliedTriggers != null) {
                // Implied close (RDD_KEY registered in IMPLIED_CLOSE_TRIGGERS): either an upcoming
                // sibling trigger tag or the parent's own closing tag ended this element's children --
                // no explicit closing tag to consume, don't swallow the inline whitespace we peeked past
                pos = beforeTrailingWs;
                return n;
            } // if
            if(lang.isHtml5) {
                // HTML5 parsing is spec-mandated to be error-tolerant and never crash (the same posture
                // html_sc.js's own doc comment already describes for real browsers) -- reaching either
                // real end-of-input (the spec's "stopped parsing" step implicitly closes every still-open
                // element, confirmed via real WPT dogfood input: many `syntax/speculative-parsing/**`
                // fixtures omit `</body>`/`</html>` entirely at EOF) or an upcoming closing tag that
                // doesn't match ours (an unrecognized/misnested element with no matching close at all,
                // e.g. a made-up `<bogus>` tag never closed before its ancestor's own closing tag --
                // confirmed via real WPT dogfood input, `charset/after-bogus.html`) both implicitly close
                // this element rather than throwing. This is a pragmatic approximation, not the spec's
                // full per-case adoption-agency/foster-parenting tree-construction algorithm (see
                // STATE_DATA_FORMATS.md's Open Questions for the cases still deliberately left unhandled
                // as genuinely out of scope) -- it only prevents a hard crash by treating the element as
                // closed-with-no-explicit-tag, same as the true-EOF and IMPLIED_CLOSE_TRIGGERS cases above.
                pos = beforeTrailingWs;
                return n;
            } // if
            throw new XmlParseException( "expected closing tag " + closeTok + " near: "
                    + s.substring( pos, Math.min( s.length(), pos + 40 ) ) );
        }
        finally {
            openTagStack.pop();
        }
    }

    /**
     * `<pre>` content is opaque like CDATA (RDD_KEY_185) -- capture verbatim through the literal
     *  closing tag, no reindentation, byte-for-byte
     */
    private Node finishRawElement(final Node n, final String closeTagLower)
    {
        final int close = indexOfIgnoreCase(s, closeTagLower, pos);
        if(close < 0) {
            // Both call sites of this method are `lang.isHtml5`-gated (`<pre>`/`<xmp>`), so reaching
            // real end-of-input with the literal closing tag never appearing is the same "stopped
            // parsing" EOF case already tolerated for ordinary elements in parseElement -- capture
            // whatever remains verbatim through EOF rather than crashing (confirmed via a synthetic
            // `<svg><script>...</s>` repro reaching EOF with no literal `</script>` anywhere; this is
            // the likely shape of the WPT `parsing/unclosed-svg-script.html` fixture, see
            // STATE_DATA_FORMATS.md's "HTML5 deep tree-construction edge cases" Open Question).
            n.raw      = s.substring(pos);
            pos        = s.length();
            n.children = null;
            n.type     = NodeType.RAW;
            return n;
        } // if
        n.raw      = s.substring(pos, close);
        pos        = close + closeTagLower.length();
        n.children = null;
        n.type     = NodeType.RAW;

        return n;
    }

    /**
     * `<script>`/`<style>` are HTML5 raw-text elements: content runs verbatim up to the literal
     *  closing tag, never tag-parsed (a `<`/`&` inside JS/CSS source must not confuse the parser)
     */
    private Node finishRawTextElement(final Node n, final String lowerTag)
    {
        final String closeTagLower = "</" + lowerTag + ">";
        final int    close         = indexOfIgnoreCase(s, closeTagLower, pos);
        if(close < 0) {
            // See finishRawElement's own comment -- same EOF-tolerance rationale, applied here for
            // `<script>`/`<style>` raw-text elements whose literal closing tag never appears at all
            n.raw      = s.substring(pos);
            pos        = s.length();
            n.children = null;
            return n;
        } // if
        n.raw      = s.substring(pos, close);
        pos        = close + closeTagLower.length();
        n.children = null;

        return n;
    }

    private static int indexOfIgnoreCase(
        final String haystack,
        final String needleLower,
        final int    from
    )
    {
        final String lower = haystack.toLowerCase(java.util.Locale.ROOT);

        return lower.indexOf(needleLower, from);
    }

    /**
     * Captures an {@link #OPAQUE_IMPLIED_END_TAG_ELEMENTS} element (e.g. `<ruby>`) as one
     *  byte-for-byte-verbatim span, from its own opening `<` through its own MATCHING `</tag>`
     *  (correctly tracking nested same-name opens/closes so an inner `<ruby>` doesn't fool the
     *  matching logic into stopping early) -- no interior parsing at all, so implied-end-tag
     *  children (`<rb>`/`<rt>`/`<rp>`/`<rtc>`, or any further nesting) are never touched.
     */
    private Node parseOpaqueImpliedEndTagElement(
        final int    tagStart,
        final String tagName,
        final String lowerTag
    )
    {
        final int openTagEnd = findTagEnd(tagStart);
        if(openTagEnd < 0) throw new XmlParseException("unterminated tag <" + tagName);
        final String openTok    = "<" + lowerTag;
        final String closeTok   = "</" + lowerTag;
              int    depth      = 1;
              int    scan       = openTagEnd + 1;
              int    closeStart = - 1;
        while(depth > 0) {
            final int nextOpen  = indexOfTagBoundary(s, openTok, scan);
            final int nextClose = indexOfTagBoundary(s, closeTok, scan);
            if(nextClose < 0) throw new XmlParseException(
                "expected closing tag </" + tagName + ">"
            );
            if(nextOpen >= 0 && nextOpen < nextClose) {
                ++depth;
                final int innerOpenEnd = findTagEnd(nextOpen);
                scan = innerOpenEnd >= 0 ? innerOpenEnd + 1 : nextOpen + openTok.length();
            }
            else {
                --depth;
                if(depth == 0) closeStart = nextClose;
                scan = nextClose + closeTok.length();
            }
        } // while
        final int closeTagEnd = findTagEnd(closeStart);
        if(closeTagEnd < 0) throw new XmlParseException(
            "unterminated closing tag </" + tagName + ">"
        );
        final Node n = new Node();
        n.type = NodeType.OPAQUE;
        n.raw  = s.substring(tagStart, closeTagEnd + 1);
        pos    = closeTagEnd + 1;

        return n;
    }

    /**
     * Scans forward from `start` (pointing at a tag's `<`) to its terminating `>`, skipping over
     *  any `>` that occurs inside a quoted attribute value. Returns -1 if unterminated.
     */
    private int findTagEnd(final int start)
    {
        boolean inQuote   = false;
        char    quoteChar = 0;
        for( int i = start; i < s.length(); ++i ) {
            final char c = s.charAt(i);
            if(inQuote) {
                if(c == quoteChar) inQuote = false;
            }
            else if(c == '"' || c == '\'') {
                inQuote   = true;
                quoteChar = c;
            }
            else if(c == '>') {
                return i;
            }
        } // for

        return -1;
    }

    /**
     * Case-insensitive search for `tokenLower` (e.g. `"<ruby"`/`"</ruby"`) in `haystack` starting
     *  at `from`, requiring a tag-boundary character (whitespace, `>`, `/`, or end-of-string)
     *  immediately after the match so `"<ruby"` doesn't false-match inside `"<rubytag"`.
     */
    private static int indexOfTagBoundary(
        final String haystack,
        final String tokenLower,
        final int    from
    )
    {
        final String lower = haystack.toLowerCase(java.util.Locale.ROOT);
              int    idx   = from;
        while(true) {
            idx = lower.indexOf(tokenLower, idx);
            if(idx < 0) return -1;
            final int after = idx + tokenLower.length();
            if( after >= haystack.length() ) return idx;
            final char c = haystack.charAt(after);
            if( Character.isWhitespace(c) || c == '>' || c == '/' ) return idx;
            idx = after + 1;
        } // while
    }

    private String parseAttr()
    {
        final int nameStart = pos;
        while( !eof() && s.charAt(
            pos
        ) != '=' && !Character.isWhitespace(
            s.charAt(pos)
        ) && s.charAt(
            pos
        ) != '/' && s.charAt(
            pos
        ) != '>' ) pos++;
        final String name = s.substring(nameStart, pos);
        skipWs();
        if( eof() || s.charAt(pos) != '=' ) {
            if(lang.isHtml5) {
                // HTML5 bare boolean attribute (e.g. `checked`, `disabled`) -- no `=value` at all.
                return name;
            }
            throw new XmlParseException("expected '=' after attribute '" + name + "'");
        } // if
        ++pos;
        skipWs();
        if( eof() || ( s.charAt(pos) != '"' && s.charAt(pos) != '\'' ) ) {
            if(lang.isHtml5) {
                // HTML5 unquoted attribute value (spec grammar: no whitespace, quote,
                // '=', '<', '>', or backtick) -- preserved unquoted on output, same
                // "preserve as written" posture the quoted branch below already gives
                // the quote-character choice (no forced normalization)
                final int valStart = pos;
                while( !eof() ) {
                    final char c = s.charAt(pos);
                    if( Character.isWhitespace(
                        c
                    ) || c == '"' || c == '\'' || c == '=' || c == '<' || c == '>' || c == '`' ) break;
                    ++pos;
                } // while
                if(pos == valStart) throw new XmlParseException(
                    "expected value for attribute '" + name + "'"
                );
                return name + "=" + s.substring(valStart, pos);
            } // if
            throw new XmlParseException("expected quoted value for attribute '" + name + "'");
        } // if
        final char quote    = s.charAt(pos);
        final int  valStart = pos;
        ++pos;
        while( !eof() && s.charAt(pos) != quote ) pos++;
        if( eof() ) throw new XmlParseException("unterminated attribute value for '" + name + "'");
        ++pos;

        return name + "=" + s.substring(valStart, pos);
    }

    // ---- comment normalization ----

    private String normComment(final String text)
    {
        if( !normalizeCommentStartCase || text.isEmpty() ) return text;
        int i = 0;
        while( i < text.length() && text.charAt(i) == ' ' ) i++;
        if( i >= text.length() || !Character.isLowerCase(
            text.charAt(i)
        ) || isSingleWordDirective(
            text
        ) || isMarkupFragmentDirective(
            text.substring(i)
        ) ) return text;

        return text.substring(
            0, i
        ) + Character.toUpperCase(
            text.charAt(i)
        ) + text.substring(
            i + 1
        );
    }

    /**
     * True iff {@code text} (already trimmed by every {@code normComment} call site) is a single
     *  word with no interior whitespace anywhere -- e.g. WordPress's magic comments
     *  {@code <!--more-->}/{@code <!--nextpage-->}/{@code <!--noteaser-->}, which are
     *  content-splitting directives a third-party tool parses literally, not prose, and must never
     *  be capitalized. Deliberately broad (unlike CSS's {@code isSingleTokenDirective}, which also
     *  requires a {@code :}/{@code -} separator): a real corpus check across three real-world HTML5
     *  dogfood trees (WordPress/wordpress-develop, web-platform-tests/wpt,
     *  alexandersandberg/html5-elements-tester) found zero genuine one-word English prose comments
     *  that this would wrongly leave lowercase -- see README.md's "Known Limitations" for the
     *  accepted false-negative risk on codebases outside that sample.
     */
    private static boolean isSingleWordDirective(final String text)
    {
        for( int i = 0; i < text.length(); ++i ) {
            if( Character.isWhitespace( text.charAt(i) ) ) return false;
        }

        return true;
    }

    /**
     * True iff {@code text}'s leading run of lowercase letters is immediately followed by
     *  {@code >} (no interior whitespace) and that run matches a real HTML tag name in
     *  {@link #MARKUP_FRAGMENT_TAG_NAMES} -- e.g. {@code tr>}/{@code p>} from a commented-out
     *  {@code <!-- <tr>...</tr> -->}/{@code <!-- <p>...</p> -->} fragment where the author's own
     *  {@code <!--}/{@code <} boundary landed mid-tag, leaving the fragment's first "word" a bare
     *  tag-name-open token rather than a capitalizable English sentence -- capitalizing it (e.g. to
     *  {@code Tr>}) corrupts commented-out markup, confirmed via real {@code apache/ant} `manual/`
     *  dogfood input ({@code Tasks/antlr.html}/{@code Tasks/attrib.html}). Deliberately restricted
     *  to a closed set of real tag names, not "any lowercase word immediately followed by
     *  {@code >}" -- a corpus grep for {@code <!--[a-z]+>} across {@code apache/ant manual/},
     *  {@code WordPress/wordpress-develop}, and
     *  {@code alexandersandberg/html5-elements-tester} found exactly this shape twice
     *  ({@code <!--tr>}) plus once ({@code <!--p>}), and zero unrelated hits -- so the tag-name
     *  restriction costs nothing in practice while guarding against a coincidental short lowercase
     *  word immediately followed by {@code >} that isn't actually a tag fragment. A same-corpus
     *  lowercase-starting comment that is NOT a markup fragment ({@code attributes inherited from
     *  MatchingTask}, {@code apache/ant manual/Tasks/imageio.html}/{@code image.html}) correctly
     *  falls through this check (no {@code >} immediately after its first word) and stays subject to
     *  ordinary capitalization -- confirmed a genuine, unrelated doc-authoring convention (identical
     *  string reused verbatim across the two files, not markup-adjacent) rather than another
     *  instance of this bug.
     */
    private static boolean isMarkupFragmentDirective(final String text)
    {
        int i = 0;
        while( i < text.length() && Character.isLowerCase( text.charAt(i) ) ) i++;
        if( i == 0 || i >= text.length() || text.charAt(i) != '>' ) return false;

        return MARKUP_FRAGMENT_TAG_NAMES.contains( text.substring(0, i) );
    }

    // ---- rendering ----

    private void renderNodes(final List<Node> nodes, final int depth, final StringBuilder out)
    {
        for(final Node n : nodes) renderNode(n, depth, out);
    }

    private void renderNode(final Node n, final int depth, final StringBuilder out)
    {
        switch(n.type) {

            case PI:
                out.append( indent(depth) ).append(n.raw).append('\n');
                return;

            case DOCTYPE:
                out.append( indent(depth) ).append(n.raw).append('\n');
                return;

            case COMMENT:
                out.append( indent(depth) ).append("<!-- ").append(n.commentText).append(" -->\n");
                return;

            case CDATA:
                out.append( indent(depth) ).append(n.raw).append('\n');
                return;

            case TEXT:
                appendWithTrailing( out, indent(depth) + n.raw, n.trailingComment );
                return;

            case FROZEN:
                for(final String line : n.frozenLines) out.append(line).append('\n');
                return;

            case RAW:
                out.append(
                    indent(depth)
                ).append(
                    '<'
                ).append(
                    n.tagName
                ).append(
                    attrsInline(n.attrs)
                )
                        .append('>').append(n.raw).append("</").append(n.tagName).append(">\n");
                return;

            case OPAQUE:
                out.append( indent(depth) ).append(n.raw).append('\n');
                return;

            case ELEMENT:
                renderElement(n, depth, out);
                return;

            default:
                throw new IllegalStateException("unhandled node type: " + n.type);

        } // switch
    }

    private void renderElement(final Node n, final int depth, final StringBuilder out)
    {
        if( lang.isHtml5 && n.raw != null
                && ( "script".equalsIgnoreCase(
                    n.tagName
                ) || "style".equalsIgnoreCase(
                    n.tagName
                ) ) ) {
            renderScriptOrStyle(n, depth, out);
            return;
        }
        final String openTightNoAngle = "<" + n.tagName + attrsInline(n.attrs);
        if(n.selfClosing) {
            final boolean isVoid = lang.isHtml5&& VOID_ELEMENTS.contains(
                n.tagName.toLowerCase(java.util.Locale.ROOT)
            );
            final String close = isVoid ? ">" : "/>";
            final String tightLine = indent(depth) + openTightNoAngle + close;
            if( tightLine.length() <= lineLengthLimit || n.attrs.isEmpty() ) {
                appendWithTrailing(out, tightLine, n.trailingComment);
            }
            else {
                out.append( indent(depth) ).append('<').append(n.tagName).append('\n');
                for( int i = 0; i < n.attrs.size(); ++i ) {
                    out.append( indent(depth + 1) ).append( n.attrs.get(i) );
                    out.append( i == n.attrs.size() - 1 ? close + "\n" : "\n" );
                }
                if(n.trailingComment != null) {
                    out.setLength( out.length() - 1 );
                    out.append(" <!-- ").append(n.trailingComment).append(" -->\n");
                }
            }
            return;
        } // if
        final Node onlyChild = soleContentChild(n.children);
        if( onlyChild != null && (onlyChild.type == NodeType.TEXT || onlyChild.type == NodeType.CDATA) ) {
            // `onlyChild` may itself carry a same-line trailing comment (e.g. `<td>text<!-- c
            // --></td>`) -- this inline fast path bypasses renderNode(onlyChild), so that comment
            // must be spliced in here too or it's silently dropped (found via apache/ant dogfood).
            final String childSuffix = onlyChild.trailingComment != null
                    ? " <!-- " + onlyChild.trailingComment + " -->" : "";
            final String inline = indent(
                depth
            ) + openTightNoAngle + ">" + onlyChild.raw + childSuffix + "</" + n.tagName + ">";
            appendWithTrailing(out, inline, n.trailingComment);
            return;
        } // if
        final String  tightOpenLine = indent(depth) + openTightNoAngle + ">";
        final boolean fits          = tightOpenLine.length() <= lineLengthLimit || n.attrs.isEmpty();
        if( n.children.isEmpty() ) {
            if(fits) {
                appendWithTrailing(out, tightOpenLine + "</" + n.tagName + ">", n.trailingComment);
            }
            else {
                appendWrappedOpenTag(n, depth, out);
                appendWithTrailing(
                    out, indent(depth) + "</" + n.tagName + ">", n.trailingComment
                );
            }
            return;
        } // if
        if(fits) out.append(tightOpenLine).append('\n');
        else     appendWrappedOpenTag(n, depth, out);
        renderNodes(n.children, depth + 1, out);
        appendWithTrailing( out, indent(depth) + "</" + n.tagName + ">", n.trailingComment );
    }

    /**
     * HTML5 §4.2: `<style>` content splices out to the CSS formatter and back, reindented one
     *  level deeper; `<script>` content splices out to the JS formatter the same way, except a
     *  non-JS `type` (e.g. `application/json`) or a `//% JXM_CFMT_DIS`/`ENA`-frozen span stays
     *  fully opaque. Uses `FormatterCore.forLanguage("js")` (not `"ts"` -- HTML `<script>` is
     *  always plain JS, TypeScript has no browser-native embedding) with a defaults-only {@link
     *  Config} built from this rule's own inherited line-length/indent/comment-case settings, so
     *  the spliced JS matches the enclosing HTML file's formatting knobs.
     */
    private void renderScriptOrStyle(final Node n, final int depth, final StringBuilder out)
    {
        final String openTag = indent(depth) + "<" + n.tagName + attrsInline(n.attrs) + ">";
        if( "style".equalsIgnoreCase(n.tagName) ) {
            final CssSpecificRule css = new CssSpecificRule(
                lang,
                lineLengthLimit,
                indentWidth,
                useTabs ? "tabs" : "spaces",
                normalizeCommentStartCase
            );
            out.append(openTag).append('\n');
            out.append( reindent( css.format( n.raw.trim() ), depth + 1 ) );
            out.append( indent(depth) ).append("</").append(n.tagName).append(">\n");
            return;
        } // if
        final String  type     = findAttrValue(n.attrs, "type");
        final boolean isJsType = type == null || JS_SCRIPT_TYPES.contains(
            type.toLowerCase(java.util.Locale.ROOT)
        );
        if( !isJsType || isFrozenScriptContent(n.raw) ) {
            out.append(openTag).append(n.raw).append("</").append(n.tagName).append(">\n");
            return;
        }
        final Config jsConfig;
        if(enclosingConfig != null) {
            jsConfig = enclosingConfig;
        }
        else {
            final java.util.Map<String, String> overrides = new java.util.LinkedHashMap<>();
            overrides.put( "line-length", Integer.toString(lineLengthLimit) );
            overrides.put( "indent-size", Integer.toString(indentWidth) );
            overrides.put("indent-style", useTabs ? "tabs" : "spaces");
            overrides.put("normalize-comment-start-case", normalizeCommentStartCase ? "on" : "off");
            jsConfig = Config.resolve(null, overrides);
        }
        final String  dedented = dedent(n.raw).trim();
        final boolean isCdata  = dedented.startsWith("<![CDATA[")&& dedented.endsWith("]]>");
        final String jsSource = isCdata
                ? dedented.substring(
                    "<![CDATA[".length(), dedented.length() - "]]>".length()
                ).trim()
                : dedented;
        final String jsFormatted = FormatterCore.forLanguage(
            "js"
        ).formatOne(
            jsSource, "<script>", jsConfig, false
        );
        final String spliced = isCdata
                ? "<![CDATA[\n" + jsFormatted.replaceAll("\\s+$", "") + "\n]]>\n"
                : jsFormatted;
        out.append(openTag).append('\n');
        out.append( reindent(spliced, depth + 1) );
        out.append( indent(depth) ).append("</").append(n.tagName).append(">\n");
    }

    /**
     * Strips the common leading whitespace shared by every non-blank line of `text`. Without this,
     *  reformatting an already-spliced `<script>` block (idempotency round2) would feed the JS
     *  formatter content that already carries the previous round's `reindent`-baked absolute
     *  indentation -- since this formatter preserves original relative indentation rather than
     *  re-deriving it from brace depth (STATE_COMMON.md's "General scope-depth reindentation" gap),
     *  that baked indentation survives untouched and `reindent` then adds a second layer on top,
     *  compounding the indentation on every round.
     */
    private String dedent(final String text)
    {
        final String[] lines     = text.split("\n", - 1);
              int      minIndent = Integer.MAX_VALUE;
        for(final String line : lines) {
            if( line.trim().isEmpty() ) continue;
            int i = 0;
            while( i < line.length() && ( line.charAt(i) == ' ' || line.charAt(i) == '\t' ) ) i++;
            minIndent = Math.min(minIndent, i);
        }
        if(minIndent == Integer.MAX_VALUE || minIndent == 0) return text;
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < lines.length; ++i) {
            final String line = lines[i];
            sb.append( line.length() >= minIndent ? line.substring(minIndent) : line.trim() );
            if(i < lines.length - 1) sb.append('\n');
        }

        return sb.toString();
    }

    /**
     * Whether `raw` (a `<script>` element's inner content, possibly `<![CDATA[ ]]>`-wrapped)
     *  contains a `//% JXM_CFMT_DIS` / `//% JXM_CFMT_ENA` line pair -- the temporary scaffold-only
     *  escape hatch for real JS content until JS/TS formatting lands
     */
    private boolean isFrozenScriptContent(final String raw)
    {
        boolean sawDis = false;
        for( final String line : raw.split("\n", -1) ) {
            final String t = line.trim();
            if( "//% JXM_CFMT_DIS".equals(t) )                sawDis = true;
            else if( sawDis && "//% JXM_CFMT_ENA".equals(t) ) return true;
        }

        return false;
    }

    private String findAttrValue(final List<String> attrs, final String name)
    {
        for(final String a : attrs) {
            final int eq = a.indexOf('=');
            if(eq < 0) continue;
            if( !a.substring(0, eq).equalsIgnoreCase(name) ) continue;
            String v = a.substring(eq + 1);
            if( v.length() >= 2 && ( v.charAt(
                0
            ) == '"' || v.charAt(
                0
            ) == '\'' ) ) v = v.substring(
                1, v.length() - 1
            );
            return v;
        } // for

        return null;
    }

    /**
     * Prefixes every non-empty line of already-formatted `text` with `depth` levels of
     *  indentation, for splicing an embedded sub-formatter's (CSS's) output back into HTML at its
     *  correct nesting depth
     */
    private String reindent(final String text, final int depth)
    {
        final String   prefix = indent(depth);
        final String[] lines  = text.split("\n", - 1);
              int      count  = lines.length;
        if( count > 0 && lines[count - 1].isEmpty() ) count--; // Drop the single trailing empty element from text's final newline
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < count; ++i) {
            final String line = lines[i];
            if( !line.isEmpty() ) sb.append(prefix).append(line);
            sb.append('\n');
        }

        return sb.toString();
    }

    private void appendWrappedOpenTag(final Node n, final int depth, final StringBuilder out)
    {
        out.append( indent(depth) ).append('<').append(n.tagName).append('\n');
        for( int i = 0; i < n.attrs.size(); ++i ) {
            out.append( indent(depth + 1) ).append( n.attrs.get(i) );
            out.append( i == n.attrs.size() - 1 ? ">\n" : "\n" );
        }
    }

    private void appendWithTrailing(
        final StringBuilder out,
        final String        line,
        final String        trailingComment
    )
    {
        out.append(line);
        if(trailingComment != null) out.append(" <!-- ").append(trailingComment).append(" -->");
        out.append('\n');
    }

    private Node soleContentChild(final List<Node> children)
    {
        if( children.size() != 1 ) return null;
        final Node only = children.get(0);

        return (only.type == NodeType.TEXT || only.type == NodeType.CDATA) ? only : null;
    }

    private String attrsInline(final List<String> attrs)
    {
        if( attrs.isEmpty() ) return "";
        final StringBuilder sb = new StringBuilder();
        for(final String a : attrs) sb.append(' ').append(a);

        return sb.toString();
    }

} // class XmlSpecificRule
