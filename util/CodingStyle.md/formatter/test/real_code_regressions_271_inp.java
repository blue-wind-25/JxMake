/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class LineLengthAudit {

    String describe(Object obj)
    {
        if(obj instanceof Config cc) {
            return "config: " + cc.channels() + "ch @ " + cc.sampleRate() + "Hz / " + cc.label();
        }
        else if(obj instanceof String s) {
            return "label: " + s;
        }

        return "unknown";
    }

    void collapse(int[] out, int[] cur, int size)
    {
        for(int x = 0; x < size; ++x) {
            out[x * size + x] = cur[(size - 1 - x) * size + x] + cur[x * size + (size - 1 - x)] + 1;
        }
        if(size > 0) {
            out[0] = 1;
        }
    }

    void declarations(Engine a, Scored s, double threshold)
    {
        boolean multipass = a.isGeneralScopeReindentEnabled() && a.isMultipassEnabled()
                && a.isLineSplitEnabled();

        Decision verdict = s.probabilities == null
                ? Decision.ABSTAIN : Classifier.decide(s.probabilities, threshold);

        boolean hasLeadingKeywordMatch = targetWordIndex == 0 && KeywordAmbiguityGate.hasLeadingKeywordMatch(
            targetWord, lang
        );
    }

}
