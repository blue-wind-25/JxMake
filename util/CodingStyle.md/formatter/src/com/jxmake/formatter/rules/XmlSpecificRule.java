/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.Config;
import com.jxmake.formatter.FormatterCore;
import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore;

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
     * per STYLE_DATA_FORMATS.md §4.1.
     */
    private static final java.util.Set<String> VOID_ELEMENTS = new java.util.HashSet<>( java.util.Arrays.asList(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param",
        "source", "track", "wbr"
    ) );

    /**
     * `<script>` MIME types that mean "this is JavaScript" per HTML5 semantics -- anything else
     * (or a recognized non-executable type such as `application/json`) is left fully opaque
     */
    private static final java.util.Set<String> JS_SCRIPT_TYPES = new java.util.HashSet<>( java.util.Arrays.asList(
        "text/javascript", "application/javascript", "application/ecmascript",
        "text/ecmascript", "module"
    ) );

    /**
     * HTML5 tree-construction spec tag-name rewrites: a start tag whose name (lowercased) is a key
     * here is renamed to the mapped value and reprocessed as that element instead -- currently just
     * `image` -> `img` (a real, spec-mandated quirk, confirmed via real WPT dogfood input,
     * `speculative-parsing/**\/resources/image-src-framed.sub.html` etc.). Map (not a single constant)
     * so any future tag-name rewrite the spec turns out to need is one entry, no new code -- per the
     * spec's tree-construction algorithm this is currently the only such rewrite ("isindex" is a
     * different, much larger obsolete-element-expansion quirk, not a simple rename, and out of scope).
     * Only applies to HTML content -- callers must additionally gate on not being inside real foreign
     * content (see {@link #svgDepth}) since e.g. a real `<image>` inside `<svg>` is a legitimate SVG
     * element, not this quirk.
     */
    private static final java.util.Map<String, String> TAG_NAME_REWRITES = new java.util.HashMap<>();
    static {
        TAG_NAME_REWRITES.put("image", "img");
    }

    /**
     * HTML5 tree-construction spec's "Adjust SVG tag names" step: inside real SVG foreign content,
     * a start tag whose lowercased name is a key here is corrected to the mapped mixed-case spec
     * name (SVG is XML-based/case-sensitive even though HTML5 parsing is otherwise case-insensitive).
     * Opposite gating from {@link #TAG_NAME_REWRITES}: that map applies only OUTSIDE svg
     * ({@code svgDepth == 0}), this one applies only INSIDE svg ({@code svgDepth > 0}) -- kept as a
     * separate map/gate rather than merged, since the two conditions are mutually exclusive. Table is
     * the WHATWG HTML5 spec's stable "Adjust SVG tag names" list (unchanged for years).
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

    private static final java.util.Map<String, String> SVG_ATTRIBUTE_CASE_FIXUP = new java.util.HashMap<>();
    static {
        SVG_ATTRIBUTE_CASE_FIXUP.put("attributename", "attributeName");
        SVG_ATTRIBUTE_CASE_FIXUP.put("attributetype", "attributeType");
        SVG_ATTRIBUTE_CASE_FIXUP.put("basefrequency", "baseFrequency");
        SVG_ATTRIBUTE_CASE_FIXUP.put("baseprofile", "baseProfile");
        SVG_ATTRIBUTE_CASE_FIXUP.put("calcmode", "calcMode");
        SVG_ATTRIBUTE_CASE_FIXUP.put("clippathunits", "clipPathUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("diffuseconstant", "diffuseConstant");
        SVG_ATTRIBUTE_CASE_FIXUP.put("edgemode", "edgeMode");
        SVG_ATTRIBUTE_CASE_FIXUP.put("filterunits", "filterUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("glyphname", "glyphName");
        SVG_ATTRIBUTE_CASE_FIXUP.put("glyphref", "glyphRef");
        SVG_ATTRIBUTE_CASE_FIXUP.put("gradienttransform", "gradientTransform");
        SVG_ATTRIBUTE_CASE_FIXUP.put("gradientunits", "gradientUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("kernelmatrix", "kernelMatrix");
        SVG_ATTRIBUTE_CASE_FIXUP.put("kernelunitlength", "kernelUnitLength");
        SVG_ATTRIBUTE_CASE_FIXUP.put("keypoints", "keyPoints");
        SVG_ATTRIBUTE_CASE_FIXUP.put("keysplines", "keySplines");
        SVG_ATTRIBUTE_CASE_FIXUP.put("keytimes", "keyTimes");
        SVG_ATTRIBUTE_CASE_FIXUP.put("lengthadjust", "lengthAdjust");
        SVG_ATTRIBUTE_CASE_FIXUP.put("limitingconeangle", "limitingConeAngle");
        SVG_ATTRIBUTE_CASE_FIXUP.put("markerheight", "markerHeight");
        SVG_ATTRIBUTE_CASE_FIXUP.put("markerunits", "markerUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("markerwidth", "markerWidth");
        SVG_ATTRIBUTE_CASE_FIXUP.put("maskcontentunits", "maskContentUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("maskunits", "maskUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("numoctaves", "numOctaves");
        SVG_ATTRIBUTE_CASE_FIXUP.put("pathlength", "pathLength");
        SVG_ATTRIBUTE_CASE_FIXUP.put("patterncontentunits", "patternContentUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("patterntransform", "patternTransform");
        SVG_ATTRIBUTE_CASE_FIXUP.put("patternunits", "patternUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("pointsatx", "pointsAtX");
        SVG_ATTRIBUTE_CASE_FIXUP.put("pointsaty", "pointsAtY");
        SVG_ATTRIBUTE_CASE_FIXUP.put("pointsatz", "pointsAtZ");
        SVG_ATTRIBUTE_CASE_FIXUP.put("preservealpha", "preserveAlpha");
        SVG_ATTRIBUTE_CASE_FIXUP.put("preserveaspectratio", "preserveAspectRatio");
        SVG_ATTRIBUTE_CASE_FIXUP.put("primitiveunits", "primitiveUnits");
        SVG_ATTRIBUTE_CASE_FIXUP.put("refx", "refX");
        SVG_ATTRIBUTE_CASE_FIXUP.put("refy", "refY");
        SVG_ATTRIBUTE_CASE_FIXUP.put("repeatcount", "repeatCount");
        SVG_ATTRIBUTE_CASE_FIXUP.put("repeatdur", "repeatDur");
        SVG_ATTRIBUTE_CASE_FIXUP.put("requiredextensions", "requiredExtensions");
        SVG_ATTRIBUTE_CASE_FIXUP.put("requiredfeatures", "requiredFeatures");
        SVG_ATTRIBUTE_CASE_FIXUP.put("specularconstant", "specularConstant");
        SVG_ATTRIBUTE_CASE_FIXUP.put("specularexponent", "specularExponent");
        SVG_ATTRIBUTE_CASE_FIXUP.put("spreadmethod", "spreadMethod");
        SVG_ATTRIBUTE_CASE_FIXUP.put("startoffset", "startOffset");
        SVG_ATTRIBUTE_CASE_FIXUP.put("stddeviation", "stdDeviation");
        SVG_ATTRIBUTE_CASE_FIXUP.put("stitchtiles", "stitchTiles");
        SVG_ATTRIBUTE_CASE_FIXUP.put("surfacescale", "surfaceScale");
        SVG_ATTRIBUTE_CASE_FIXUP.put("systemlanguage", "systemLanguage");
        SVG_ATTRIBUTE_CASE_FIXUP.put("tablevalues", "tableValues");
        SVG_ATTRIBUTE_CASE_FIXUP.put("targetx", "targetX");
        SVG_ATTRIBUTE_CASE_FIXUP.put("targety", "targetY");
        SVG_ATTRIBUTE_CASE_FIXUP.put("textlength", "textLength");
        SVG_ATTRIBUTE_CASE_FIXUP.put("transformorigin", "transformOrigin");
        SVG_ATTRIBUTE_CASE_FIXUP.put("viewbox", "viewBox");
        SVG_ATTRIBUTE_CASE_FIXUP.put("viewtarget", "viewTarget");
        SVG_ATTRIBUTE_CASE_FIXUP.put("zoomandpan", "zoomAndPan");
    }

    /**
     * HTML5 tree-construction spec's "Adjust MathML attributes" step: inside real MathML foreign content
     * ({@code mathmlDepth > 0}), attribute and tag names are corrected to the mapped mixed-case spec name
     * (e.g. `definitionurl` -> `definitionURL`)
     */
    private static final java.util.Map<String, String> MATHML_TAG_NAME_CASE_FIXUP = new java.util.HashMap<>();
    static {
        // MathML specification accept tags of any case; therefore, this map is empty
    }

    private static final java.util.Map<String, String> MATHML_ATTRIBUTE_CASE_FIXUP = new java.util.HashMap<>();
    static {
        MATHML_ATTRIBUTE_CASE_FIXUP.put("definitionurl", "definitionURL");
    }

    /**
     * HTML5 elements whose children rely on the spec's implied-end-tag tree-construction rule
     * (e.g. `<rb>`/`<rt>`/`<rp>`/`<rtc>` inside `<ruby>` never carry an explicit closing tag in
     * valid markup) -- rather than modeling the full per-element-family implied-closing-trigger
     * spec (a large feature, RDD_KEY_198), each name here is instead scanned as one opaque,
     * byte-for-byte-verbatim span from its opening tag to its own MATCHING closing tag (nested
     * same-name opens/closes tracked), reusing the same "don't parse the interior, just find the
     * matching close" pattern {@link #finishRawTextElement}/{@link #finishRawElement} already use
     * for `<script>`/`<style>`/`<pre>`. Extend by adding a name here only -- no other code change
     * needed for a simple case; do not add per-element implied-closing-trigger logic.
     */
    private static final java.util.Set<String> OPAQUE_IMPLIED_END_TAG_ELEMENTS = new java.util.HashSet<>(
            java.util.Arrays.asList("ruby") );

    /**
     * Real HTML5 tag names, used only by {@link #isMarkupFragmentDirective} to recognize a comment
     * whose content is a leftover markup fragment (e.g. a commented-out {@code <tr>...</tr>} or
     * {@code <p>...</p>} block where the author's {@code <!--}/{@code <} boundary landed mid-tag,
     * leaving the fragment's first "word" a bare tag-name-open token like {@code tr>}/{@code p>}
     * rather than a capitalizable English sentence). Deliberately a closed set of real tag names
     * (not "any lowercase word immediately followed by {@code >}") -- see that method's Javadoc for
     * the corpus evidence this restriction is based on.
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
     * as a REAL node (attributes/children/normal rendering, unlike {@link #OPAQUE_IMPLIED_END_TAG_ELEMENTS}'
     * whole-span opaque capture) whose children stop -- implying an unwritten closing tag -- as soon
     * as one of the mapped sibling start-tag names begins, in addition to the existing "parent's own
     * closing tag also ends me" behavior every element already gets via {@code parseNodes}'s
     * {@code stopAtCloseTag}. Populate with one entry per element only once real dogfood input needs
     * it -- currently `option` (closes on a sibling `<option>`/`<optgroup>` start, or when its
     * parent `<select>`/`<datalist>`/`<optgroup>` closes), `head` (closes on a sibling `<body>`
     * start, confirmed via real WPT dogfood input, `meta-inhead-insertion-mode.html`), and `p`
     * (RDD_KEY_204 -- closes on any of the HTML5 spec's fixed "close a p element" trigger-tag
     * list, confirmed via real `apache/ant` `manual/` dogfood input: a `<p>...` paragraph with no
     * explicit `</p>` before a following `<h3>` caused the parser to swallow the rest of the
     * document as that `<p>`'s children until the first unrelated closing tag anywhere downstream,
     * producing a spurious duplicate `</p>` at the very end). Do NOT add `li`/`td`/`tr`/etc.
     * speculatively without similar real dogfood evidence -- see STATE_DATA_FORMATS.md's Open
     * Questions/RDD_LOG.md for the rationale.
     */
    /**
     * Level-2 tc-gap (RDD_KEY_230, foster-parenting): element names allowed to remain as direct
     * children of a {@code <table>} without being relocated -- the HTML5 spec's own "in table"
     * insertion-mode structural set (row-group/row/cell-boundary elements, plus {@code <script>}/
     * {@code <style>}/{@code <template>}, which the spec also exempts from foster-parenting). Any
     * other element, or non-whitespace text, encountered directly inside {@code <table>} (i.e. while
     * {@link #isInTableInsertionMode()} is true) gets foster-parented instead -- see
     * {@link #shouldFosterParent}.
     */
    private static final java.util.Set<String> TABLE_STRUCTURE_CHILDREN = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "caption", "colgroup", "col", "tbody", "tfoot", "thead", "tr", "td", "th", "script",
            "style", "template"
        )
    );

    /**
     * Level-4 tc-gap (RDD_KEY_230, adoption agency): the HTML5 spec's own "formatting elements"
     * vocabulary -- the element names the adoption agency algorithm exists to recover misnesting
     * for (e.g. {@code <b>1<i>2</b>3</i>}). See {@link #reconstructFormattingElement} for the
     * narrow, single-level approximation actually implemented -- NOT the spec's full "list of
     * active formatting elements" + "furthest block" + "bookmark" algorithm.
     */
    private static final java.util.Set<String> FORMATTING_ELEMENTS = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong",
            "tt", "u"
        )
    );

    /**
     * Level-1 tc-gap (RDD_KEY_230, {@link #insertImplicitBodyIfNeeded}): the HTML5 spec's
     * "in head"/"before head" insertion-mode vocabulary -- element names that, when no explicit
     * {@code <head>} is present, still belong to an implicit head rather than triggering the
     * "head insertion mode closed" transition to body content. Tracked explicitly via
     * {@link #headInsertionModeClosed} rather than inferred per-call from sibling structure.
     */
    private static final java.util.Set<String> HEAD_ELIGIBLE_ELEMENTS = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "title", "script", "style", "meta", "link", "base", "noscript"
        )
    );

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

        NodeType type;
        String   raw;             // PI / DOCTYPE / CDATA / TEXT: verbatim content
        String   commentText;     // COMMENT: normalized inner text
        boolean  commentVerbatim; // COMMENT: true if commentText must render with no
                                                // Added inner spacing (a `%`-prefixed marker/directive comment)
        List<String> commentBannerLines; // COMMENT: non-null iff a multi-line comment already
                                                // Follows the conventional ` * `-per-line continuation-marker
                                                // banner shape (curly's `/* */` equivalent, RDD_KEY_262-adjacent
                                                // gap) -- content lines already capitalize/period-normalized,
                                                // rendered with a reindented ` * `/` -->` banner rather than
                                                // frozen verbatim
        List<String> frozenLines;               // FROZEN: raw lines, verbatim, DIS..ENA inclusive
        String       tagName;
        List<String> attrs = new ArrayList<>();
        boolean      selfClosing;
        List<Node>   children;                  // Null if self-closing; empty list if open/close-with-nothing
        String       trailingComment;           // Normalized text of a same-line trailing comment, or null
        /**
         * Set (non-null) only when this ELEMENT's children are "mixed content" -- at least one
         * non-whitespace-only TEXT node AND at least one ELEMENT node interleaved (e.g.
         * {@code <p>Click <a href="x">here</a> to continue.</p>}). Holds the ORIGINAL source text of
         * the element's content, verbatim (leading/trailing whitespace trimmed only at the outer
         * boundary, nothing normalized/reflowed internally), captured at parse time rather than
         * re-derived by recursing through the pretty-printer -- XML text-node whitespace can be
         * semantically significant (XHTML-like prose, Android string resources with embedded
         * {@code <b>}/{@code <a>} markup, DocBook, SVG {@code <text>}), so inserting any
         * reflow/reindentation between text and inline elements would silently change the represented
         * value. Rendered inline as a single line with no wrapping, even if it overflows
         * {@code line-length} -- deliberately mirrors the existing opaque/preserve-verbatim posture
         * already used for DOCTYPE/PI (§2.3), CDATA (§2.4 default case), and multi-line comments
         * (§2.5). {@code null} for every other element (pure-text/CDATA-only or
         * pure-child-element-only content).
         */
        String mixedContentRaw;

    } // class Node

    /**
     * Level-2 tc-gap (RDD_KEY_230, foster-parenting): holds nodes relocated out of a {@code <table>}
     * while it's being parsed, pending splice into the table's own parent's children list (just
     * before the table node itself) once the table element finishes parsing -- see
     * {@link #fosterBufferStack} and {@link #pendingFosterBuffer}
     */
    private static final class FosterBuffer {

        final List<Node> nodes = new ArrayList<>();

    } // class FosterBuffer

    private final Lang    lang;
    private final int     lineLengthLimit;
    private final int     indentWidth;
    private final boolean useTabs;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;
    /**
     * Real resolved Config of the enclosing HTML file, threaded through so a spliced
     * {@code <script>} block inherits every JS/TS-specific config key (e.g. `js-import-order`),
     * not just the 4 primitive fields above. May be null (legacy/test constructors) -- in that
     * case {@link #renderScriptOrStyle} falls back to a throwaway {@code Config.resolve(null, ...)}
     * built from those 4 fields, same as before this was threaded through.
     */
    private final Config enclosingConfig;

    private String s;
    /**
     * Lowercased copy of {@link #s}, computed once in {@link #format} -- {@link
     * #indexOfIgnoreCase}/{@link #indexOfTagBoundary} search this instead of re-lowercasing the
     * whole document on every call
     */
    private String sLower;
    private int    pos;
    /**
     * Depth counter for `<svg>` ancestors, tracked only so the HTML5 "image" -> "img" tag-name
     * rewrite (see {@link #parseElement}) can be correctly scoped to HTML content only -- inside
     * real SVG foreign content, `<image>` is a legitimate SVG element with its own closing tag, not
     * a quirk alias for `<img>`. Confirmed via real WPT dogfood input (`svg-image-href.tentative.html`
     * etc., which nest a real `<image>`/`</image>` pair inside `<svg>`).
     */
    private int svgDepth;

    /**
     * Depth counter for `<math>` ancestors, tracked so MathML case fixup (e.g. `definitionurl` ->
     * `definitionURL`) applies only inside MathML foreign content
     */
    private int mathmlDepth;

    /**
     * Lightweight name-only stack of currently-open element tag names (lowercased), pushed in
     * {@link #parseElement} right after a start tag is recognized and popped on every return path
     * (matched close, implied-close-trigger path, and the tolerant-close fallback alike). Lets
     * {@link #parseNodes} distinguish, at a closing tag encountered mid-children-parse, "this name
     * matches something actually open on the path from the document root to here" (a legitimate
     * cascade-close -- the mechanism WPT's {@code charset/after-bogus.html} mismatched-tag case
     * relies on) from "this name matches nothing open anywhere" (a genuine orphan close tag, e.g.
     * apache/ant's `manual/running.html` stray {@code </p>} with no open {@code <p>} at all --
     * see STATE_DATA_FORMATS.md's Open Questions item 2). Deliberately NOT the full per-insertion-
     * mode HTML5 tree-construction state -- just enough to make that one distinction.
     */
    private final java.util.Deque<String> openTagStack = new java.util.ArrayDeque<>();

    /**
     * {@code html5-tc-gap-level} (tc gap job, {@code STATE_HTML5_TCG.md}, {@code RDD_KEY_230}),
     * read once per file from {@link #enclosingConfig} (which may be null -- legacy/test
     * constructors -- in which case this falls back to {@code 0}, same as the config default).
     * Guards this class's tc-gap code paths on their own (`>= N` for the level introducing
     * each gap) -- deliberately NOT ANDed with {@code lang.isHtml5} on top, since the config key
     * only has effect when {@code lang.isHtml5} is already true elsewhere in the pipeline (see
     * STATE_HTML5_TCG.md's Non-goals).
     */
    private final int html5TcGapLevel;

    /** {@code html5-tc-gap-level} threshold for gap 3 -- implicit {@code <body>} insertion */
    private static final int LEVEL_BODY_SYNTHESIS = 1;
    /** {@code html5-tc-gap-level} threshold for gap 1 -- table foster-parenting tree reshaping */
    private static final int LEVEL_TABLE_FOSTER = 2;
    /** {@code html5-tc-gap-level} threshold for gap 2 -- misnested {@code <form>}/{@code <template>} reconstruction */
    private static final int LEVEL_TEMPLATE_FORM = 3;
    /** {@code html5-tc-gap-level} threshold for gap 4 -- adoption agency (active-formatting-element) reconstruction */
    private static final int LEVEL_FORMATTING_RECONSTRUCT = 4;

    /**
     * Level-1 tc-gap guard (RDD_KEY_230): set once an implicit {@code <body>} has been
     * synthesized for the document currently being parsed, so a document with multiple
     * head-adjacent content nodes only ever gets one synthetic {@code <body>} inserted
     */
    private boolean bodyInserted;

    /**
     * Level-1 tc-gap fix (root cause noted in STATE_HTML5_TCG.md's "Known residual gap"): tracks
     * the real HTML5 tree-construction "head insertion mode closed" transition explicitly, instead
     * of inferring it per-call from sibling structure. Starts {@code false} for every document;
     * set {@code true} the moment {@link #insertImplicitBodyIfNeeded} either finds an explicit
     * {@code <head>} element, or encounters a top-level sibling that is not head-eligible (see
     * {@link #HEAD_ELIGIBLE_ELEMENTS}) -- mirroring the spec's own criterion for when "in head"/
     * "before head" insertion mode ends and "in body" begins. While this flag is still
     * {@code false}, a leading run of {@code <meta>}/{@code <title>}/{@code <script>}/etc. siblings
     * is head content, not body content, even when no explicit {@code <head>} tag exists at all.
     */
    private boolean headInsertionModeClosed;

    /**
     * Level-2 tc-gap (RDD_KEY_230, foster-parenting): one {@link FosterBuffer} per currently-open
     * {@code <table>} ancestor, pushed/popped in {@link #parseElement} alongside {@link #openTagStack}
     * on {@code <table>} open/close. A {@code Deque}-of-buffers (not one flat buffer) so a fostered
     * node inside a nested {@code <table>} splices into its own immediately-enclosing table's
     * relocation point, not an outer table's. Asserted empty at the end of {@link #format} as a leak
     * guard (every push must be matched by a pop in {@code parseElement}'s {@code finally}).
     */
    private final java.util.Deque<FosterBuffer> fosterBufferStack = new java.util.ArrayDeque<>();

    /**
     * Level-2 tc-gap (RDD_KEY_230) side channel (Option B): set by {@link #parseElement} the instant
     * a {@code <table>} with non-empty buffered foster content finishes parsing, consumed by the
     * immediate caller in {@link #parseNodes} right before it would otherwise add the just-returned
     * {@code <table>} node to its own children list -- the buffered nodes are spliced in first, so
     * they land immediately before the table in that ancestor's children, matching the spec's "insert
     * immediately before the table" requirement. Always {@code null} again immediately after being
     * consumed.
     */
    private FosterBuffer pendingFosterBuffer;

    /**
     * Level-3 tc-gap (RDD_KEY_230, misnested {@code <form>} reconstruction): the "form element
     * pointer" -- the currently active {@code <form>} {@link Node}, or {@code null} if none. A
     * {@code <form>} start tag encountered while this is non-null is suppressed (see
     * {@link #pendingSuppressedFormNode}) instead of creating a second nested form element, per the
     * spec's own single-slot form-pointer concept. Scoped per {@code <template>} boundary via a plain
     * local-variable save/restore in {@link #parseElement} (the same pattern {@code isSvg}/
     * {@code svgDepth} and {@code isTable}/{@code fosterBufferStack} already use) rather than a
     * separate explicit {@code Deque} field -- the Java call stack itself already provides the
     * correct nesting behavior for a save/restore-shaped local, confirmed via a manual smoke test of
     * a {@code <form>} nested inside a {@code <template>} that is itself inside another
     * {@code <form>}'s content (the inner form is correctly allowed, not suppressed, because
     * entering the {@code <template>} resets this field to {@code null} for its own local scope and
     * restores the outer form's pointer on exit) before this field's shape was settled -- see
     * STATE_HTML5_TCG.md checklist item 6's own note.
     */
    private Node currentFormElementPointer;

    /**
     * Level-3 tc-gap (RDD_KEY_230) side channel: set by {@link #parseElement}, in its {@code finally}
     * block, to the just-finished {@code <form>} {@link Node} when that form was suppressed (a second
     * {@code <form>} encountered while {@link #currentFormElementPointer} was already non-null).
     * Consumed by {@link #parseNodes} immediately after receiving that same node back from
     * {@link #parseSingleNode} -- rather than adding the suppressed wrapper element itself, its own
     * children are spliced directly into the caller's children list instead, matching the spec's
     * "ignore the start tag" recovery (the form's content still appears in the tree, just without its
     * own now-ignored wrapping element). Always {@code null} again immediately after being consumed.
     */
    private Node pendingSuppressedFormNode;

    /**
     * Level-4 tc-gap (RDD_KEY_230, adoption agency) side channel, part 1: set by {@link #parseElement}
     * the instant a formatting element (see {@link #FORMATTING_ELEMENTS}) is implicitly closed
     * because the very next token is a closing tag belonging to one of ITS OWN ancestors (the
     * classic {@code <b>1<i>2</b>3</i>} misnesting -- {@code <i>} is implicitly closed here because
     * {@code </b>} is next, not {@code </i>}), paired with {@link #pendingAdoptionOuterTagLower}
     * (the ancestor tag name that triggered it). Cleared the moment that ancestor's own real
     * closing tag is actually matched (see {@link #pendingReconstructFormattingTemplate}) or left
     * set (and simply never consumed) if that never happens -- deliberately only tracks the single
     * most-recently-orphaned formatting element, not a full stack of simultaneous misnestings; see
     * STATE_HTML5_TCG.md checklist item 7's own note on what subset of the spec algorithm this is.
     */
    private Node   pendingAdoptionNode;
    private String pendingAdoptionOuterTagLower;

    /**
     * Level-4 tc-gap (RDD_KEY_230, adoption agency) side channel, part 2: set by {@link #parseElement}
     * in the {@code closeTok}-match branch when the element that just genuinely closed is the same
     * ancestor recorded in {@link #pendingAdoptionOuterTagLower}. Consumed by {@link #parseNodes}
     * immediately after adding that ancestor node to its own children list -- a clone of the
     * orphaned formatting element (see {@link #reconstructFormattingElement}) is parsed as the next
     * sibling, reconstructing the spec's "reopen the formatting element after the misnesting
     * ancestor closes" recovery. Always {@code null} again immediately after being consumed.
     */
    private Node pendingReconstructFormattingTemplate;

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
        this(lang, lineLengthLimit, indentWidth, indentStyle, normalizeCommentStartCase, false, enclosingConfig);
    }

    public XmlSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final Config  enclosingConfig
    )
    {
        this.lang                      = lang;
        this.lineLengthLimit           = lineLengthLimit;
        this.indentWidth               = indentWidth;
        this.useTabs                   = "tabs".equals(indentStyle);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
        this.enclosingConfig           = enclosingConfig;
        this.html5TcGapLevel           = enclosingConfig != null ? enclosingConfig.html5TcGapLevel() : 0;
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
     * closing-tag matching against an {@code n.tagName} that may have been case-rewritten from the
     * source's own literal casing ({@link #TAG_NAME_REWRITES}/{@link #SVG_TAG_NAME_CASE_FIXUP}/{@link #MATHML_TAG_NAME_CASE_FIXUP}) --
     * e.g. source `<fegaussianblur>...</fegaussianblur>` becomes `n.tagName ==
     * "feGaussianBlur"`, so a literal-case match against the source's own lowercase closing tag would
     * never succeed without this. HTML5 tag-name matching is spec-mandated case-insensitive; other
     * languages (XML/XHTML-family) keep the exact-case {@link #startsWith(String)} check since they
     * never rewrite tag names.
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
        this.s      = content;
        this.sLower = content.toLowerCase(java.util.Locale.ROOT);
        this.pos    = 0;
        final List<Node> nodes = parseNodes(false);
        if( !eof() ) throw new XmlParseException(
            "trailing content after document, near: " + s.substring( pos, Math.min( s.length(), pos + 40 ) )
        );
        if(html5TcGapLevel >= LEVEL_BODY_SYNTHESIS) insertImplicitBodyIfNeeded(nodes);
        assert fosterBufferStack.isEmpty() : "fosterBufferStack leaked "
            + fosterBufferStack.size() + " unclosed buffer(s)";
        final StringBuilder out = new StringBuilder();
        renderNodes(nodes, 0, out);

        return out.toString();
    }

    /**
     * Level-2 tc-gap (RDD_KEY_230, foster-parenting trigger detection): true iff the node about to
     * be added is a DIRECT child of an open {@code <table>} -- i.e. {@link #openTagStack}'s innermost
     * (top) entry is exactly {@code "table"}. Deliberately NOT a full ancestor scan: once any
     * structural child (a {@code <tr>}, a stray {@code <div>}, etc.) is itself pushed onto
     * {@code openTagStack}, that child's own descendants are no longer being evaluated in the "in
     * table" insertion mode -- they're in whatever nested mode that child established (e.g. "in row"
     * inside a {@code <tr>}, or plain "in body" inside a fostered {@code <div>}) and must NOT be
     * independently re-evaluated for fostering, or a fostered element's own children would be
     * incorrectly stripped back out of it. This single-level check is what makes that distinction:
     * {@code openTagStack.peek()} only equals {@code "table"} while {@link #parseNodes} is building
     * the table's own direct children list.
     */
    private boolean isInTableInsertionMode()
    {
        return "table".equals( openTagStack.peek() );
    }

    /**
     * Level-2 tc-gap (RDD_KEY_230): true iff {@code node}, encountered directly inside a
     * {@code <table>} (i.e. while {@link #isInTableInsertionMode()} is true), must be foster-parented
     * rather than left as a direct child of the table -- any element not in
     * {@link #TABLE_STRUCTURE_CHILDREN}, or non-whitespace text. Comments/PIs/frozen spans are never
     * fostered (they carry no visible tree-shape content the spec's foster-parenting rule cares
     * about).
     */
    private boolean shouldFosterParent(final Node node)
    {
        if(node.type == NodeType.TEXT) return node.raw != null && !node.raw.trim().isEmpty();
        if(node.type == NodeType.ELEMENT) {
            final String lowerTag = node.tagName != null ? node.tagName.toLowerCase(
                java.util.Locale.ROOT
            ) : "";

            return !TABLE_STRUCTURE_CHILDREN.contains(lowerTag);
        } // if

        return false;
    }

    /**
     * Level-1 tc-gap fix (RDD_KEY_230, STATE_HTML5_TCG.md checklist item 3): a document with no
     * explicit {@code <body>} start tag anywhere still gets one implicitly inserted at the point
     * the spec calls "in body"-eligible content. Simplification (noted in STATE_HTML5_TCG.md):
     * rather than modeling "head closed" as a distinct insertion-mode transition, this treats the
     * first non-whitespace, non-comment, non-DOCTYPE, non-{@code <head>} sibling encountered
     * (searching the {@code <html>} element's children if one exists, else the top-level document
     * nodes) as the synthesis point, and wraps it plus every sibling after it in a synthesized
     * {@code <body>} element. This is the first fabricated-node path in this otherwise strictly
     * preserve-as-written formatter -- see RDD_KEY_230. Guarded by {@link #bodyInserted} so a
     * document is never given more than one synthetic {@code <body>}, even if this were ever
     * called more than once for the same parse.
     */
    /** True iff {@code n} is an ELEMENT node named {@code tagName} (case-insensitive) */
    private static boolean isElementNamed(final Node n, final String tagName)
    {
        return n.type == NodeType.ELEMENT && n.tagName != null && n.tagName.equalsIgnoreCase(
            tagName
        );
    }

    private void insertImplicitBodyIfNeeded(final List<Node> nodes)
    {
        if(bodyInserted) return;
        Node htmlNode = null;
        for(final Node n : nodes) {
            if( isElementNamed(n, "html") ) {
                htmlNode = n;
                break;
            } // if
        } // for
        final List<Node> target = htmlNode != null && htmlNode.children != null ? htmlNode.children : nodes;
        for(final Node n : target) {
            // Explicit <body> already present somewhere in the target sibling list -- nothing to do
            if( isElementNamed(n, "body") ) return;
        } // for
        headInsertionModeClosed = false;
        int firstContentIdx = -1;
        for( int i = 0; i < target.size(); ++i ) {
            final Node n = target.get(i);
            if(n.type == NodeType.DOCTYPE || n.type == NodeType.COMMENT) continue;
            if( isElementNamed(n, "head") ) {
                // An explicit <head> closes head-insertion mode once we're past it -- everything
                //  after belongs to "in body", matching the spec's real transition criterion
                headInsertionModeClosed = true;
                continue;
            } // if
            if( n.type == NodeType.TEXT && ( n.raw == null || n.raw.trim().isEmpty() ) ) continue;
            if( !headInsertionModeClosed && n.type == NodeType.ELEMENT && n.tagName != null
                && HEAD_ELIGIBLE_ELEMENTS.contains(
                    n.tagName.toLowerCase(java.util.Locale.ROOT)
                ) ) {
                // No explicit <head> has appeared yet, and this sibling is head-eligible (e.g.
                //  <meta>/<title>/<script>) -- per the real tree-construction insertion-mode
                //  transition, this still belongs to an implicit head, not to body. Leave it out of
                //  the body-wrap range rather than the old sibling heuristic that wrapped it.
                continue;
            } // if
            // Any other real content closes head-insertion mode (spec: a non-head-eligible token
            //  forces the implicit transition out of "before head"/"in head")
            headInsertionModeClosed = true;
            firstContentIdx         = i;
            break;
        } // for
        if(firstContentIdx < 0) return; // Nothing eligible to wrap -- e.g. head-only document

        final Node body = new Node();
        body.type        = NodeType.ELEMENT;
        body.tagName     = "body";
        body.selfClosing = false;
        body.children    = new ArrayList<>( target.subList( firstContentIdx, target.size() ) );
        for( int i = target.size() - 1; i >= firstContentIdx; --i ) target.remove(i);
        target.add(body);
        bodyInserted = true;
    }

    /**
     * Level-4 tc-gap (RDD_KEY_230, adoption agency): reconstructs a clone of an orphaned
     * {@code template} formatting element as a fresh sibling, parsing forward from the current
     * cursor position exactly like {@link #parseElement} would for a freshly-encountered start tag
     * of the same name (push {@link #openTagStack}, parse children via {@link #parseNodes}, consume
     * a matching real close tag if one appears, pop {@link #openTagStack}) -- except the open tag
     * itself is synthesized (copied from {@code template}, which is never re-added to any children
     * list itself) rather than read from source text, since the source's own open tag for it was
     * already consumed the first time it was parsed.
     * <p>
     * <b>What subset of the spec's adoption agency algorithm this is, and why (STATE_HTML5_TCG.md
     * checklist item 7):</b> the full spec algorithm maintains an explicit "list of active
     * formatting elements" plus a "furthest block"/"bookmark"-tracking bounded-iteration loop
     * capable of correctly resolving arbitrarily deep and/or multiple SIMULTANEOUS misnestings in
     * one pass. This formatter builds its tree via plain recursive descent (no reified, mutable,
     * randomly-addressable tree the way the spec's algorithm assumes), so implementing that full
     * generality was judged too large/risky a change for one checkpoint (per this checklist item's
     * own documented allowance) and was not attempted. What's implemented instead: {@link
     * #pendingAdoptionNode} tracks only the SINGLE most-recently-orphaned formatting element at a
     * time (a plain field, not a stack/list), detected only for the narrow "next token is a real
     * closing tag belonging to one of my own ancestors" case (not the spec's full furthest-block
     * search), and reconstructed as a plain next-sibling clone (not spliced back into the original
     * misnesting position via a bookmark). This correctly handles the classic single-level case
     * (e.g. {@code <b>1<i>2</b>3</i>}), but a second, simultaneous misnesting (e.g. two formatting
     * elements both orphaned by the same ancestor close) only reconstructs the innermost/most-recent
     * one -- the outer one is silently dropped, same accepted-limitation posture as level 1's
     * head-less-document gap and level 2's single-level table check.
     */
    private Node reconstructFormattingElement(final Node template)
    {
        final Node clone = new Node();
        clone.type        = NodeType.ELEMENT;
        clone.tagName     = template.tagName;
        clone.attrs       = new ArrayList<>(template.attrs);
        clone.selfClosing = false;
        final String lowerTag = clone.tagName.toLowerCase(java.util.Locale.ROOT);
        openTagStack.push(lowerTag);
        try {
            clone.children = parseNodes(true, null);
            final String closeTok = "</" + clone.tagName + ">";
            skipInlineWs();
            if( startsWith(closeTok) || startsWith("</" + clone.tagName + " ")
                    || startsWithCloseTagIgnoreCase(clone.tagName) ) {
                final int gt = s.indexOf('>', pos);
                pos = gt + 1;
            } // if
            // Else: no real closing tag followed (EOF, or an ancestor's close instead) -- tolerate
            // silently, same posture as parseElement's own lang.isHtml5 implicit-close fallback
        }
        finally {
            openTagStack.pop();
        }

        return clone;
    }

    private List<Node> parseNodes(final boolean stopAtCloseTag)
    {
        return parseNodes(stopAtCloseTag, null);
    }

    /**
     * @param impliedCloseTriggers when non-null, children stop being consumed (implying the
     * currently-open element is closed, with no explicit closing tag) as soon as upcoming input is a
     * start tag whose name is in this set -- see {@link #IMPLIED_CLOSE_TRIGGERS}
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
                final String orphanTag = peekCloseTagNameLower();
                final int    gt        = s.indexOf('>', pos);
                pos = gt >= 0 ? gt + 1 : s.length();
                if( "p".equals(orphanTag) ) nodes.add( synthesizeEmptyElement("p") );
                continue;
            } // if
            if( impliedCloseTriggers != null && startsWithTriggerTag(impliedCloseTriggers) ) break;
            if( lang.isHtml5 && !stopAtCloseTag && startsWith("</") ) {
                // Document-root-level stray closing tag with no corresponding open element anywhere
                // in this recursive-descent parse (e.g. one of the genuinely deep tree-construction
                // gaps documented in STATE_DATA_FORMATS.md's Open Questions -- adoption agency,
                // foreign-content foster-parenting -- can leave one of these bubbling all the way up).
                // Per the same "HTML5 parsing must never crash" posture as parseElement's implicit-close
                // fallback, silently discard the stray tag and keep going rather than throwing -- except
                // an orphan `</p>` (RDD_KEY_223's spec note), where the real HTML5 "p end tag" algorithm
                // synthesizes an empty `<p></p>` rather than discarding, so match that here too.
                final String orphanTag = peekCloseTagNameLower();
                final int    gt        = s.indexOf('>', pos);
                pos = gt >= 0 ? gt + 1 : s.length();
                if( "p".equals(orphanTag) ) nodes.add( synthesizeEmptyElement("p") );
                continue;
            } // if
            final Node node = parseSingleNode();
            attachTrailingCommentIfAny(node);
            // Level-3 tc-gap (RDD_KEY_230, misnested <form>): a suppressed second <form> (see
            // pendingSuppressedFormNode's own javadoc) contributes its own children directly to this
            // frame's children list instead of its own now-ignored wrapping element
            if(node == pendingSuppressedFormNode) {
                pendingSuppressedFormNode = null;
                if(node.children != null) nodes.addAll(node.children);
                continue;
            } // if
            // Level-2 tc-gap (RDD_KEY_230, foster-parenting): a node encountered directly inside a
            // <table> (outside a <td>/<th>/<caption> cell) that isn't part of the table's own
            // structural vocabulary gets relocated to just before the table instead of nested inside
            // it -- redirect it into the innermost FosterBuffer rather than this frame's own nodes.
            // (Kept as a boolean rather than an early `continue` so the level-4 reconstruction check
            // below still runs for a fostered node too -- a real bug found via smoke-testing before
            // this item's own fixtures were authored: with an early `continue` here, a formatting
            // element reconstructed by adoption agency while directly inside a <table> was silently
            // dropped instead of being foster-parented itself.)
            final boolean fostered = html5TcGapLevel >= LEVEL_TABLE_FOSTER && !fosterBufferStack.isEmpty() && isInTableInsertionMode() && shouldFosterParent(
                node
            );
            if(fostered) {
                fosterBufferStack.peek().nodes.add(node);
            }
            else {
                if(pendingFosterBuffer != null) {
                    nodes.addAll(pendingFosterBuffer.nodes);
                    pendingFosterBuffer = null;
                } // if
                nodes.add(node);
            }
            // Level-4 tc-gap (RDD_KEY_230, adoption agency): the node just added (or fostered) closed
            // an ancestor that had orphaned a formatting element inside it (see pendingAdoptionNode's
            // own javadoc) -- reconstruct that formatting element as this node's own next sibling (in
            // whichever destination -- fosterBufferStack or nodes -- the ancestor itself just landed
            // in) so the content that follows continues to render as if the formatting element had
            // never been misnested-closed early
            if(html5TcGapLevel >= LEVEL_FORMATTING_RECONSTRUCT && pendingReconstructFormattingTemplate != null) {
                final Node template = pendingReconstructFormattingTemplate;
                pendingReconstructFormattingTemplate = null;
                skipWs();
                final String lowerTemplateTag = template.tagName.toLowerCase(java.util.Locale.ROOT);
                // If the cursor already sits at a literal start tag matching the template's own name,
                // this is very likely a re-parse of already-reconstructed output from a prior format
                // round (idempotency) -- reconstructing a NEW wrapper here would double-nest the
                // literal element that follows (found via the mixed-content fix: preserving a
                // misnested formatting element's raw, unclosed source verbatim -- required by
                // §2.2's mixed-content rule -- means the same misnesting is still literally present
                // for the parser to re-detect on round2, unlike before when every child was always
                // re-emitted well-formed). Skip synthesizing a second wrapper in that case; the
                // literal element that follows IS the reconstruction.
                if( !startsWithTriggerTag( java.util.Collections.singleton(lowerTemplateTag) ) ) {
                    final Node reconstructed = reconstructFormattingElement(template);
                    if(fostered) fosterBufferStack.peek().nodes.add(reconstructed);
                    else         nodes.add(reconstructed);
                } // if
            } // if
        } // while

        return nodes;
    }

    /**
     * True if the cursor is positioned at a start tag (not a closing tag) whose lowercased name is
     * in {@code triggers}. Used by {@link #IMPLIED_CLOSE_TRIGGERS}.
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
     * `</`) -- used by {@link #openTagStack}'s ancestor/orphan check
     */
    private String peekCloseTagNameLower()
    {
          int i     = pos + 2;
    final int start = i;
        while( i < s.length() && s.charAt(i) != '>' && !Character.isWhitespace( s.charAt(i) ) ) i++;

        return s.substring(start, i).toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Builds a synthetic empty (open/close-with-nothing) element node, e.g. for an orphan
     * {@code </p>} -- see the "p end tag" spec note in {@code RDD_LOG.md}'s {@code RDD_KEY_223}
     */
    private Node synthesizeEmptyElement(final String tagNameLower)
    {
        final Node n = new Node();
        n.type     = NodeType.ELEMENT;
        n.tagName  = tagNameLower;
        n.children = new ArrayList<>();

        return n;
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
        if( ("<!--% " + TokenizerCore.JXM_CFMT_DIS + " -->").equals( currentLineTrimmed() ) ) {
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
                if( ("<!--% " + TokenizerCore.JXM_CFMT_ENA + " -->").equals(
                    line.trim()
                ) || eof() ) break;
            } // while
            return n;
        } // if
        final int close = s.indexOf("-->", pos + 4);
        if(close < 0) throw new XmlParseException("unterminated comment");
        final Node   n     = new Node();
        final String raw   = s.substring(pos + 4, close);
        final String inner = raw.trim();
        n.type = NodeType.COMMENT;
        if( inner.startsWith("%") ) {
            // A `%`-prefixed marker/directive comment must survive byte-for-byte -- normal comment
            //  rendering always inserts a space after `<!--`, which would turn `<!--%` into `<!-- %`
            //  and permanently break the marker's required exact prefix on any subsequent parse
            //  (InFileConfig.java's own regex, and //  this method's own DIS/ENA literal-string checks
            //  above, both require `<!--%` with no intervening space)
            n.commentVerbatim = true;
            n.commentText     = raw;
        } // if
        else if( raw.indexOf('\n') >= 0 ) {
            // Multi-line comment (interior contains a newline, checked on the RAW pre-trim content).
            //  If every continuation line already follows the conventional ` * `-per-line banner
            //  shape (same detection curly's MiscRuleCore.reformatMultiLineBlockComment uses for
            //  `/* */`), treat it the same way: capitalize the first content line, strip a sole
            //  trailing period across the whole comment, reindent to the banner shape at render time
            //  (see commentBannerLines). Otherwise -- unrecognized shape, e.g. a plain wrapped-prose
            //  header with no `*` markers -- fall back to the pre-existing freeze-verbatim posture
            //  (RDD_KEY_232), same as PI/CDATA "opaque, preserved verbatim" (STYLE_DATA_FORMATS.md
            //  SS2.3/2.4).
            final List<String> bannerLines = tryBannerShape(raw);
            if(bannerLines != null) {
                n.commentBannerLines = bannerLines;
            } // if
            else {
                n.commentVerbatim = true;
                n.commentText     = raw;
            } // else
        } // else if
        else {
            n.commentText = normComment(inner);
        }
        pos = close + 3;

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
        // See MATHML_TAG_NAME_CASE_FIXUP -- opposite gate from TAG_NAME_REWRITES: only applies INSIDE
        // real MathML foreign content (mathmlDepth > 0), never in plain HTML content.
        if( lang.isHtml5 && mathmlDepth > 0 && MATHML_TAG_NAME_CASE_FIXUP.containsKey(
            lowerTag
        ) ) n.tagName = MATHML_TAG_NAME_CASE_FIXUP.get(
            lowerTag
        );
        final boolean isSvg    = lang.isHtml5 && "svg".equals(lowerTag);
        final boolean isMathml = lang.isHtml5 && "math".equals(lowerTag);
        final boolean isVoid   = lang.isHtml5 && VOID_ELEMENTS.contains(lowerTag);
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
        // Level-2 tc-gap (RDD_KEY_230, foster-parenting): a new FosterBuffer per <table> ancestor,
        // pushed/popped alongside openTagStack -- see fosterBufferStack's own javadoc
        final boolean isTable = html5TcGapLevel >= LEVEL_TABLE_FOSTER && "table".equals(lowerTag);
        // Level-3 tc-gap (RDD_KEY_230, misnested <form>): a <template> gets its own fresh form-pointer
        // scope (saved/restored via the plain local below -- see currentFormElementPointer's own
        // javadoc for why this doesn't need a separate Deque); a second <form> seen while a form
        // pointer is already active is suppressed rather than nested
        final boolean isTemplate       = html5TcGapLevel >= LEVEL_TEMPLATE_FORM && "template".equals(
            lowerTag
        );
        final boolean isForm           = html5TcGapLevel >= LEVEL_TEMPLATE_FORM && "form".equals(
            lowerTag
        );
        final boolean formSuppressed   = isForm && currentFormElementPointer != null;
        final Node    savedFormPointer = currentFormElementPointer;
        // Level-4 tc-gap (RDD_KEY_230, adoption agency): see FORMATTING_ELEMENTS/pendingAdoptionNode's
        // own javadocs
        final boolean isFormatting = html5TcGapLevel >= LEVEL_FORMATTING_RECONSTRUCT && FORMATTING_ELEMENTS.contains(
            lowerTag
        );
        openTagStack.push(lowerTag);
        if(isTable) fosterBufferStack.push( new FosterBuffer() );
        if(isTemplate) currentFormElementPointer = null;
        if(isForm && !formSuppressed) currentFormElementPointer = n;
        final int childStart = pos;
        try {
            if(isSvg) svgDepth++;
            if(isMathml) mathmlDepth++;
            try {
                n.children = parseNodes(true, impliedTriggers);
            }
            finally {
                if(isSvg) svgDepth--;
                if(isMathml) mathmlDepth--;
            }
            final String closeTok         = "</" + n.tagName + ">";
            final int    beforeTrailingWs = pos;
            if( isMixedContent(n.children) ) {
                final String candidate = s.substring(childStart, beforeTrailingWs).trim();
                // Only collapse to a single inline line when the ORIGINAL source already wrote this
                // element's whole content on one line (e.g. `<p>Click <a href="x">here</a> to
                // continue.</p>`) -- a block-level container that already spans multiple source lines
                // (e.g. a <div> with a bare "Here is a list of items:" sibling line before a <ul>) is
                // "mixed" by the same non-whitespace-text + element definition but is NOT the
                // text-flow-prose shape this fix targets; its bare text-node siblings keep the
                // pre-existing per-sibling reindentation behavior (RDD_KEY_185) instead of being
                // forced onto one (possibly very long) line.
                if( candidate.indexOf('\n') < 0 ) n.mixedContentRaw = candidate;
            } // if
            skipInlineWs();
            if( startsWith(closeTok) || startsWith("</" + n.tagName + " ")
                    || ( lang.isHtml5 && startsWithCloseTagIgnoreCase(n.tagName) ) ) {
                final int gt = s.indexOf('>', pos);
                pos = gt + 1;
                // Level-4 tc-gap (RDD_KEY_230, adoption agency): this element (n) is the ancestor
                // recorded in pendingAdoptionOuterTagLower and it just genuinely closed -- hand off
                // to parseNodes (see pendingReconstructFormattingTemplate's own javadoc) to
                // reconstruct the formatting element it had orphaned, as n's own next sibling
                if( html5TcGapLevel >= LEVEL_FORMATTING_RECONSTRUCT && pendingAdoptionNode != null
                        && lowerTag.equals(pendingAdoptionOuterTagLower) ) {
                    pendingReconstructFormattingTemplate = pendingAdoptionNode;
                    pendingAdoptionNode                  = null;
                    pendingAdoptionOuterTagLower         = null;
                } // if
                return n;
            } // if
            if(impliedTriggers != null) {
                // Implied close (RDD_KEY registered in IMPLIED_CLOSE_TRIGGERS): either an upcoming
                // sibling trigger tag or the parent's own closing tag ended this element's children --
                // no explicit closing tag to consume, don't swallow the inline whitespace we peeked past
                pos = beforeTrailingWs;
                return n;
            } // if
            // Level-4 tc-gap (RDD_KEY_230, adoption agency): n (a formatting element) is about to be
            // implicitly closed below because the very next token is a closing tag that isn't n's own
            // -- if that next token is a real closing tag (not just EOF) belonging to one of n's own
            // ancestors, this is the classic <b>1<i>2</b>3</i> misnesting shape. Record n plus the
            // ancestor's tag name so the ancestor's own eventual real close (see the closeTok-match
            // branch above) can trigger reconstructing n as the ancestor's next sibling.
            if( html5TcGapLevel >= LEVEL_FORMATTING_RECONSTRUCT && isFormatting && startsWith(
                "</"
            ) ) {
                pendingAdoptionNode          = n;
                pendingAdoptionOuterTagLower = peekCloseTagNameLower();
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
            if(isTable) {
                final FosterBuffer fb = fosterBufferStack.pop();
                if( !fb.nodes.isEmpty() ) pendingFosterBuffer = fb;
            } // if
            if(isTemplate) currentFormElementPointer = savedFormPointer;
            if(isForm && !formSuppressed && currentFormElementPointer == n) currentFormElementPointer = null;
            if(formSuppressed) pendingSuppressedFormNode = n;
        }
    }

    /**
     * `<pre>` content is opaque like CDATA (RDD_KEY_185) -- capture verbatim through the literal
     * closing tag, no reindentation, byte-for-byte
     */
    private Node finishRawElement(final Node n, final String closeTagLower)
    {
        final int close = indexOfIgnoreCase(closeTagLower, pos);
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
     * closing tag, never tag-parsed (a `<`/`&` inside JS/CSS source must not confuse the parser)
     */
    private Node finishRawTextElement(final Node n, final String lowerTag)
    {
        final String closeTagLower = "</" + lowerTag + ">";
        final int    close         = indexOfIgnoreCase(closeTagLower, pos);
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

    private int indexOfIgnoreCase(final String needleLower, final int from)
    {
        return sLower.indexOf(needleLower, from);
    }

    /**
     * Captures an {@link #OPAQUE_IMPLIED_END_TAG_ELEMENTS} element (e.g. `<ruby>`) as one
     * byte-for-byte-verbatim span, from its own opening `<` through its own MATCHING `</tag>`
     * (correctly tracking nested same-name opens/closes so an inner `<ruby>` doesn't fool the
     * matching logic into stopping early) -- no interior parsing at all, so implied-end-tag
     * children (`<rb>`/`<rt>`/`<rp>`/`<rtc>`, or any further nesting) are never touched
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
              int    closeStart = -1;
        while(depth > 0) {
            final int nextOpen  = indexOfTagBoundary(openTok, scan);
            final int nextClose = indexOfTagBoundary(closeTok, scan);
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
     * any `>` that occurs inside a quoted attribute value. Returns -1 if unterminated.
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
     * at `from`, requiring a tag-boundary character (whitespace, `>`, `/`, or end-of-string)
     * immediately after the match so `"<ruby"` doesn't false-match inside `"<rubytag"`
     */
    private int indexOfTagBoundary(final String tokenLower, final int from)
    {
        int idx = from;
        while(true) {
            idx = sLower.indexOf(tokenLower, idx);
            if(idx < 0) return -1;
            final int after = idx + tokenLower.length();
            if( after >= s.length() ) return idx;
            final char c = s.charAt(after);
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
        String name = s.substring(nameStart, pos);
        if(lang.isHtml5 && svgDepth > 0) {
            final String lowerName = name.toLowerCase(java.util.Locale.ROOT);
            if( SVG_ATTRIBUTE_CASE_FIXUP.containsKey(
                lowerName
            ) ) name = SVG_ATTRIBUTE_CASE_FIXUP.get(
                lowerName
            );
        } // if
        if(lang.isHtml5 && mathmlDepth > 0) {
            final String lowerName = name.toLowerCase(java.util.Locale.ROOT);
            if( MATHML_ATTRIBUTE_CASE_FIXUP.containsKey(
                lowerName
            ) ) name = MATHML_ATTRIBUTE_CASE_FIXUP.get(
                lowerName
            );
        } // if
        skipWs();
        if( eof() || s.charAt(pos) != '=' ) {
            if(lang.isHtml5) {
                // HTML5 bare boolean attribute (e.g. `checked`, `disabled`) -- no `=value` at all
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

    /**
     * Detects the curly-equivalent " * "-per-line continuation-marker banner shape on a multi-line
     * {@code <!-- -->} interior (RAW, pre-trim content between the markers) and, if matched, returns
     * the already capitalize/period-normalized content lines (no `*` markers, no indentation --
     * render time reindents per the node's own depth); returns {@code null} if any continuation
     * line doesn't start with `*` after stripping leading whitespace, meaning the caller should fall
     * back to freeze-verbatim. Mirrors {@code MiscRuleCore.reformatMultiLineBlockComment}'s shape
     * check/content-extraction, adapted for `<!--`/`-->` markers living outside {@code raw} (unlike
     * curly's `/*`/`*&#47;` which are embedded in the first/last physical lines).
     */
    private List<String> tryBannerShape(final String raw)
    {
        final String[] rawLines = raw.split("\r\n|\r|\n", -1);
        // The line right before the closing `-->` marker is just that marker's leading indentation
        //  (e.g. the " " in " * ...\n -->") -- it lives outside `raw` for curly's `*/`-embedded
        //  equivalent, so it's exempt from the `*`-prefix requirement and contributes no content line.
        final boolean lastLineIsCloseIndent = rawLines.length > 1 && stripLeadingWs(
            rawLines[rawLines.length - 1]
        ).isEmpty();
        final int     lastContinuationLine  = lastLineIsCloseIndent ? rawLines.length - 1 : rawLines.length;
        for(int i = 1; i < lastContinuationLine; ++i) {
            if( !stripLeadingWs( rawLines[i] ).startsWith("*") ) return null;
        }

        final List<String> contentLines = new ArrayList<>();
        final String       firstContent = rawLines[0].trim();
        if( !firstContent.isEmpty() ) contentLines.add(firstContent);
        for(int i = 1; i < lastContinuationLine; ++i) {
            String afterStar = stripLeadingWs( rawLines[i] ).substring(1);
            if( afterStar.startsWith(" ") ) afterStar = afterStar.substring(1);
            contentLines.add( trimTrailingWs(afterStar) );
        } // for

        if( contentLines.isEmpty() ) return contentLines;

        if(normalizeCommentStartCase) {
            final String first = contentLines.get(0);
                  int    i     = 0;
            while( i < first.length() && first.charAt(i) == ' ' ) i++;
            if( i < first.length() && Character.isLowerCase(
                first.charAt(i)
            ) && !isSingleWordDirective(
                first
            ) && !isMarkupFragmentDirective(
                first.substring(i)
            ) ) contentLines.set(
                0,
                first.substring(0, i) + Character.toUpperCase( first.charAt(i) ) + first.substring(i + 1)
            ); // If
        } // if

        if(normalizeCommentEndPeriod) {
            int dotCount = 0;
            for(final String line : contentLines) for( int i = 0; i < line.length(); ++i ) if(
                line.charAt(i) == '.'
            ) dotCount++;
            if(dotCount == 1) {
                for( int i = contentLines.size() - 1; i >= 0; --i ) {
                    final String line = contentLines.get(i);
                    if( line.endsWith(".") ) {
                        contentLines.set(
                            i, trimTrailingWs( line.substring( 0, line.length() - 1 ) )
                        );
                        break;
                    } // if
                    if( !line.trim().isEmpty() ) break;
                } // for
            } // if
        } // if

        return contentLines;
    }

    private static String stripLeadingWs(final String line)
    {
        int i = 0;
        while( i < line.length() && Character.isWhitespace( line.charAt(i) ) ) i++;

        return line.substring(i);
    }

    private static String trimTrailingWs(final String s)
    {
        int end = s.length();
        while( end > 0 && Character.isWhitespace( s.charAt(end - 1) ) ) end--;

        return s.substring(0, end);
    }

    // ---- comment normalization ----

    private String normComment(final String rawText)
    {
        final String text = normalizeCommentEndPeriod ? ToolingCommentNormalizer.stripSoleTrailingPeriod(
            rawText
        ) : rawText;
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
     * word with no interior whitespace anywhere -- e.g. WordPress's magic comments
     * {@code <!--more-->}/{@code <!--nextpage-->}/{@code <!--noteaser-->}, which are
     * content-splitting directives a third-party tool parses literally, not prose, and must never
     * be capitalized. Deliberately broad (unlike CSS's {@code isSingleTokenDirective}, which also
     * requires a {@code :}/{@code -} separator): a real corpus check across three real-world HTML5
     * dogfood trees (WordPress/wordpress-develop, web-platform-tests/wpt,
     * alexandersandberg/html5-elements-tester) found zero genuine one-word English prose comments
     * that this would wrongly leave lowercase -- see README.md's "Known Limitations" for the
     * accepted false-negative risk on codebases outside that sample.
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
     * {@code >} (no interior whitespace) and that run matches a real HTML tag name in
     * {@link #MARKUP_FRAGMENT_TAG_NAMES} -- e.g. {@code tr>}/{@code p>} from a commented-out
     * {@code <!-- <tr>...</tr> -->}/{@code <!-- <p>...</p> -->} fragment where the author's own
     * {@code <!--}/{@code <} boundary landed mid-tag, leaving the fragment's first "word" a bare
     * tag-name-open token rather than a capitalizable English sentence -- capitalizing it (e.g. to
     * {@code Tr>}) corrupts commented-out markup, confirmed via real {@code apache/ant} `manual/`
     * dogfood input ({@code Tasks/antlr.html}/{@code Tasks/attrib.html}). Deliberately restricted
     * to a closed set of real tag names, not "any lowercase word immediately followed by
     * {@code >}" -- a corpus grep for {@code <!--[a-z]+>} across {@code apache/ant manual/},
     * {@code WordPress/wordpress-develop}, and
     * {@code alexandersandberg/html5-elements-tester} found exactly this shape twice
     * ({@code <!--tr>}) plus once ({@code <!--p>}), and zero unrelated hits -- so the tag-name
     * restriction costs nothing in practice while guarding against a coincidental short lowercase
     * word immediately followed by {@code >} that isn't actually a tag fragment. A same-corpus
     * lowercase-starting comment that is NOT a markup fragment ({@code attributes inherited from
     * MatchingTask}, {@code apache/ant manual/Tasks/imageio.html}/{@code image.html}) correctly
     * falls through this check (no {@code >} immediately after its first word) and stays subject to
     * ordinary capitalization -- confirmed a genuine, unrelated doc-authoring convention (identical
     * string reused verbatim across the two files, not markup-adjacent) rather than another
     * instance of this bug.
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
                if(n.commentBannerLines != null) {
                    out.append( indent(depth) ).append("<!--\n");
                    for(final String line : n.commentBannerLines) {
                        out.append( indent(depth) ).append(" *");
                        if( !line.isEmpty() ) out.append(' ').append(line);
                        out.append('\n');
                    } // for
                    out.append( indent(depth) ).append("-->\n");
                    return;
                } // if
                if(n.commentVerbatim) out.append(
                    indent(depth)
                ).append(
                    "<!--"
                ).append(
                    n.commentText
                ).append(
                    "-->\n"
                );
                else out.append( indent(depth) ).append( wrapComment(n.commentText) ).append('\n');
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
            final boolean isVoid    = lang.isHtml5 && VOID_ELEMENTS.contains(
                n.tagName.toLowerCase(java.util.Locale.ROOT)
            );
            final String  close     = isVoid ? ">" : "/>";
            final String  tightLine = indent(depth) + openTightNoAngle + close;
            if( tightLine.length() <= lineLengthLimit || n.attrs.isEmpty() ) {
                appendWithTrailing(out, tightLine, n.trailingComment);
            }
            else {
                appendWrappedOpenTag(n, depth, out, close);
                if(n.trailingComment != null) {
                    out.setLength( out.length() - 1 );
                    out.append(' ').append( wrapComment(n.trailingComment) ).append('\n');
                }
            }
            return;
        } // if
        if(n.mixedContentRaw != null) {
            final String inline = indent(
                depth
            ) + openTightNoAngle + ">" + n.mixedContentRaw + "</" + n.tagName + ">";
            appendWithTrailing(out, inline, n.trailingComment);
            return;
        } // if
        final Node onlyChild = soleContentChild(n.children);
        if( onlyChild != null && (onlyChild.type == NodeType.TEXT || onlyChild.type == NodeType.CDATA) ) {
            // `onlyChild` may itself carry a same-line trailing comment (e.g. `<td>text<!-- c
            // --></td>`) -- this inline fast path bypasses renderNode(onlyChild), so that comment
            // must be spliced in here too or it's silently dropped (found via apache/ant dogfood)
            final String childSuffix = onlyChild.trailingComment != null ? " " + wrapComment(
                onlyChild.trailingComment
            ) : "";
            final String inline      = indent(
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
     * level deeper; `<script>` content splices out to the JS formatter the same way, except a
     * non-JS `type` (e.g. `application/json`) or a `//% JXM_CFMT_DIS`/`ENA`-frozen span stays
     * fully opaque. Uses `FormatterCore.forLanguage("js")` (not `"ts"` -- HTML `<script>` is
     * always plain JS, TypeScript has no browser-native embedding) with a defaults-only {@link
     * Config} built from this rule's own inherited line-length/indent/comment-case settings, so
     * the spliced JS matches the enclosing HTML file's formatting knobs.
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
                normalizeCommentStartCase,
                normalizeCommentEndPeriod
            );
            // §2.4's XHTML idiom exception: `<style><![CDATA[ ... ]]></style>` unwraps, dispatches
            //  its inner text through the same CSS formatter as the plain (non-CDATA) case, then
            //  re-wraps the formatted result in `<![CDATA[ ]]>` -- mirrors `<script>`'s identical
            //  handling below. Same known limitation as the `<script>` path: if the formatted CSS
            //  ever happened to contain the literal sequence `]]>` (extremely unlikely for CSS
            //  content), the naive re-wrap would prematurely terminate the CDATA section -- not
            //  worth defensive escaping machinery for so rare an edge case.
            final String  dedentedStyle = dedent(n.raw).trim();
            final boolean isCdataStyle  = dedentedStyle.startsWith(
                "<![CDATA["
            ) && dedentedStyle.endsWith(
                "]]>"
            );
            final String  cssSource     = isCdataStyle ? dedentedStyle.substring(
                "<![CDATA[".length(), dedentedStyle.length() - "]]>".length()
            ).trim() : n.raw.trim();
            final String  cssFormatted  = css.format(cssSource);
            final String  spliceStyle   = isCdataStyle ? "<![CDATA[\n" + cssFormatted.replaceAll(
                "\\s+$", ""
            ) + "\n]]>\n" : cssFormatted;
            out.append(openTag).append('\n');
            out.append( reindent(spliceStyle, depth + 1) );
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
            overrides.put("normalize-comment-end-period", normalizeCommentEndPeriod ? "on" : "off");
            jsConfig = Config.resolve(null, overrides);
        }
        final String  dedented    = dedent(n.raw).trim();
        final boolean isCdata     = dedented.startsWith("<![CDATA[") && dedented.endsWith("]]>");
        final String  jsSource    = isCdata ? dedented.substring(
            "<![CDATA[".length(), dedented.length() - "]]>".length()
        ).trim() : dedented;
        final String  gdrJsSource = com.jxmake.formatter.gdr.GdrPipelineGate.apply(
            jsSource, "js", jsConfig
        );
        final String  jsFormatted = FormatterCore.forLanguage(
            "js"
        ).formatOne(
            gdrJsSource, "<script>", jsConfig, false
        );
        final String  spliced     = isCdata ? "<![CDATA[\n" + jsFormatted.replaceAll(
            "\\s+$", ""
        ) + "\n]]>\n" : jsFormatted;
        out.append(openTag).append('\n');
        out.append( reindent(spliced, depth + 1) );
        out.append( indent(depth) ).append("</").append(n.tagName).append(">\n");
    }

    /**
     * Strips the common leading whitespace shared by every non-blank line of `text`. Without this,
     * reformatting an already-spliced `<script>` block (idempotency round2) would feed the JS
     * formatter content that already carries the previous round's `reindent`-baked absolute
     * indentation -- since this formatter preserves original relative indentation rather than
     * re-deriving it from brace depth (STATE_COMMON.md's "General scope-depth reindentation" gap),
     * that baked indentation survives untouched and `reindent` then adds a second layer on top,
     * compounding the indentation on every round.
     */
    private String dedent(final String text)
    {
        return MiscRuleCore.dedentLines(text, 0);
    }

    /**
     * Whether `raw` (a `<script>` element's inner content, possibly `<![CDATA[ ]]>`-wrapped)
     * contains a `//% JXM_CFMT_DIS` / `//% JXM_CFMT_ENA` line pair -- the temporary scaffold-only
     * escape hatch for real JS content until JS/TS formatting lands
     */
    private boolean isFrozenScriptContent(final String raw)
    {
        boolean sawDis = false;
        for( final String line : raw.split("\n", -1) ) {
            final String t = line.trim();
                 if( ("//% " + TokenizerCore.JXM_CFMT_DIS).equals(t) )           sawDis = true;
            else if( sawDis && ("//% " + TokenizerCore.JXM_CFMT_ENA).equals(t) ) return true;
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
     * indentation, for splicing an embedded sub-formatter's (CSS's) output back into HTML at its
     * correct nesting depth
     */
    private String reindent(final String text, final int depth)
    {
        final String   prefix = indent(depth);
        final String[] lines  = text.split("\n", -1);
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
        appendWrappedOpenTag(n, depth, out, ">");
    }

    /** {@code closer} is the text that terminates the last attribute line (e.g. {@code ">"}, {@code "/>"}, or a void element's {@code ">"}) */
    private void appendWrappedOpenTag(
        final Node          n,
        final int           depth,
        final StringBuilder out,
        final String        closer
    )
    {
        out.append( indent(depth) ).append('<').append(n.tagName).append('\n');
        for( int i = 0; i < n.attrs.size(); ++i ) {
            out.append( indent(depth + 1) ).append( n.attrs.get(i) );
            out.append( i == n.attrs.size() - 1 ? closer + "\n" : "\n" );
        }
    }

    /**
     * Renders a comment's `<!--`/`-->` markers with the normal one-space padding, except a
     * single-word directive (WordPress's `<!--more-->`/`<!--nextpage-->`/`<!--noteaser-->` and the
     * like, see {@link #isSingleWordDirective}) renders tight -- padding would rewrite the exact
     * literal byte sequence such a directive's third-party consumer requires just as surely as
     * capitalizing it would, defeating the whole point of {@code isSingleWordDirective} already
     * skipping capitalization for this shape
     */
    private static String wrapComment(final String text)
    {
        return isSingleWordDirective(text) ? "<!--" + text + "-->" : "<!-- " + text + " -->";
    }

    private void appendWithTrailing(
        final StringBuilder out,
        final String        line,
        final String        trailingComment
    )
    {
        out.append(line);
        if(trailingComment != null) out.append(' ').append( wrapComment(trailingComment) );
        out.append('\n');
    }

    /**
     * True iff `children` is "mixed content" per §2.2's mixed-content rule: at least one
     * non-whitespace-only TEXT node AND at least one ELEMENT node among the same sibling list. A
     * nested mixed-content element (an ELEMENT child whose own content is itself mixed) still just
     * counts as "an ELEMENT node" here -- its own inner text/markup is part of the literal source
     * span this outer element's {@link Node#mixedContentRaw} captures, so nested mixed content falls
     * out naturally without any recursive handling.
     */
    private boolean isMixedContent(final List<Node> children)
    {
        if( children == null || children.size() < 2 ) return false;
        boolean sawNonWhitespaceText = false;
        boolean sawElement           = false;
        for(final Node c : children) {
            if( c.type == NodeType.TEXT && c.raw != null && !c.raw.trim().isEmpty() ) sawNonWhitespaceText = true;
            else if(c.type == NodeType.ELEMENT) sawElement = true;
        }

        return sawNonWhitespaceText && sawElement;
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
