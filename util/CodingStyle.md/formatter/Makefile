#
# Makefile for formatter
#

JAVAC        ?= javac
JAR          ?= jar
JAVA         ?= java
JAVA_VERSION ?= 8

# Single source of truth for the version number -- propagated into the JAR
# manifest (Implementation-Version), read at runtime by `Main.java`'s
# `printUsage()`/`--version`, and picked up by `code-formatter.sh/.cmd` via glob.
VERSION       = 1.0.0
ARTIFACT      = code-formatter-$(VERSION)
MAIN_CLASS    = com.jxmake.formatter.Main

SRC_DIR       = src

BUILD_DIR     = target
CLASS_DIR     = $(BUILD_DIR)/classes
JAR_FILE      = $(BUILD_DIR)/$(ARTIFACT).jar
MANIFEST      = $(CLASS_DIR)/MANIFEST.MF

TEST_DIR      = test
TEST_TMP      = $(BUILD_DIR)/test-out
TEST_LOCK     = $(BUILD_DIR)/.test.lock

SOURCES       = $(shell find $(SRC_DIR) -name "*.java")


.PHONY: all build jar clean                \
	gru-extract-pool-a                 \
	gru-extract-pool-b                 \
	gru-measure-abstain-rate           \
	gru-acquire-corpus                 \
	gru-hand-labeled-examples          \
	gru-train                          \
	gru-cv-corpus                      \
	gru-sync-weights                   \
	test test-quiet _test_serial       \
	test-server test-server-concurrent \
	bench


all build jar:
	@if $(MAKE) --no-print-directory -q $(JAR_FILE); then \
		echo "JAR is already built and up-to-date.";  \
	else                                                  \
		$(MAKE) --no-print-directory $(JAR_FILE);     \
	fi

$(CLASS_DIR):
	@mkdir -p $(CLASS_DIR)

$(JAR_FILE): $(SOURCES) | $(CLASS_DIR)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -d $(CLASS_DIR) $(SOURCES)
	@mkdir -p $(CLASS_DIR)/META-INF
	@printf 'Manifest-Version: 1.0\nMain-Class: %s\nImplementation-Version: %s\n' '$(MAIN_CLASS)' '$(VERSION)' > $(CLASS_DIR)/META-INF/MANIFEST.MF
	@(                                                                     \
		export TZ=UTC;                                                 \
		cd $(CLASS_DIR)                                             && \
		entries=$$(find . -type f | sed 's|^\./||' | LC_ALL=C sort) && \
		touch -t 198001010000 $$entries                             && \
		args=""; for f in $$entries; do args="$$args -C . $$f"; done;  \
		$(JAR) cfM $(abspath $(JAR_FILE)) $$args;                      \
	)
	@echo "Built $(JAR_FILE)"
	@cp $(JAR_FILE) .

clean:
	@rm -rvf $(BUILD_DIR)
	@rm -rvf tools/verifiers/__pycache__
	@rm -vf  tools/verifiers/*.class

# clear && git status && echo && svn stat && echo
# git stash list
# git fsck --unreachable --no-reflogs

# svn add util/CodingStyle.md/formatter/test/*
# svn add util/CodingStyle.md/formatter/test/real_code_regressions*
# svn commit -m 'Follow GIT changes' && git push && echo && git status && echo && svn stat && echo

# clear && uptime && echo && sensors && free -h && echo && df -h && echo
# svn update && svn cleanups

# make test
# make test 2>/dev/null
# make test 2>&1 | grep -E 'PASS|FAIL'


# ── GRU Pool A/Pool B training-data extraction (STATE_AI.md "Training-set
#    acquisition, verification, fixing") ─────────────────────────────────────
#
# Pool A (keyword-ambiguity): compiles and runs `tools/gru/ExtractPoolA.java` against a comment corpus
# extracted by `tools/gru/extract_comments.py`, keeping only comments the real CommentClassifier ABSTAINs
# on due to `hasLeadingKeywordMatch` (excludes `NonLatinScriptGate` ABSTAINs -- those are a different,
# non-keyword-ambiguity signal, see STATE_AI.md item 9's TTGO_VGA32_Lite/RobotCoding investigation).
#
# Pool B (period-ambiguity): runs `tools/gru/extract_pool_b.py`'s RDD_EXT_15 grep-based recall-favoring
# filter directly over the same corpus format -- not derived from `CommentClassifier` at all, a separate
# ambiguity class.
#
# Usage:
#     python3 tools/gru/extract_comments.py <root-dir> [<root-dir> ...] --out /tmp/comments.txt
#     make gru-extract-pool-a GRU_ABSTAIN_INPUT=/tmp/comments.txt GRU_POOL_A_OUT=/tmp/pool_a.txt
#     make gru-extract-pool-b GRU_ABSTAIN_INPUT=/tmp/comments.txt GRU_POOL_B_OUT=/tmp/pool_b.txt
#
GRU_TOOLS_DIR      = tools/gru
GRU_BUILD_DIR      = $(BUILD_DIR)/gru
GRU_TOOLS_CLASSES  = $(GRU_BUILD_DIR)/classes
GRU_POOL_A_OUT    ?=
GRU_POOL_B_OUT    ?=

gru-extract-pool-a: $(JAR_FILE)
	@if [ -z "$(GRU_ABSTAIN_INPUT)" ] || [ -z "$(GRU_POOL_A_OUT)" ]; then                                                  \
		echo "gru-extract-pool-a: set GRU_ABSTAIN_INPUT=<path from extract_comments.py> GRU_POOL_A_OUT=<output path>"; \
		exit 2;                                                                                                        \
	fi
	@mkdir -p $(GRU_TOOLS_CLASSES)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -cp $(CLASS_DIR) -d $(GRU_TOOLS_CLASSES) $(GRU_TOOLS_DIR)/ExtractPoolA.java
	@$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) ExtractPoolA $(GRU_ABSTAIN_INPUT) $(GRU_POOL_A_OUT)

gru-extract-pool-b:
	@if [ -z "$(GRU_ABSTAIN_INPUT)" ] || [ -z "$(GRU_POOL_B_OUT)" ]; then                                                  \
		echo "gru-extract-pool-b: set GRU_ABSTAIN_INPUT=<path from extract_comments.py> GRU_POOL_B_OUT=<output path>"; \
		exit 2;                                                                                                        \
	fi
	@python3 $(GRU_TOOLS_DIR)/extract_pool_b.py $(GRU_ABSTAIN_INPUT) --out $(GRU_POOL_B_OUT)

# ── GRU ABSTAIN-rate measurement (STATE_AI.md Step 3 open item 9) ───────────
#
# Compiles and runs `tools/gru/CommentAbstainTally.java` against a comment corpus extracted by
# `tools/gru/extract_comments.py`, tallying the real rule-based `CommentClassifier`'s YES/NO/ABSTAIN counts.
# This is item 9's  measurement step -- not a decision, an actual run against real comments.
#
# Usage:
#     python3 tools/gru/extract_comments.py <root-dir> [<root-dir> ...] --out /tmp/comments.txt
#     make gru-measure-abstain-rate GRU_ABSTAIN_INPUT=/tmp/comments.txt
#
GRU_ABSTAIN_INPUT ?=

gru-measure-abstain-rate: $(JAR_FILE)
	@if [ -z "$(GRU_ABSTAIN_INPUT)" ]; then                                                         \
		echo "gru-measure-abstain-rate: set GRU_ABSTAIN_INPUT=<path from extract_comments.py>"; \
		exit 2;                                                                                 \
	fi
	@mkdir -p $(GRU_TOOLS_CLASSES)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -cp $(CLASS_DIR) -d $(GRU_TOOLS_CLASSES) $(GRU_TOOLS_DIR)/CommentAbstainTally.java
	@$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) CommentAbstainTally $(GRU_ABSTAIN_INPUT)

# ── GRU corpus acquisition + auto-labeling target ────────────────────────────
#
# Runs `tools/gru/acquire_corpus.sh` (extraction + secret-redaction + per-source exact-duplicate-line
# dedup + Pool A/Pool B candidate extraction across every configured source) into a scratch dir,
# concatenates every source's already-redacted/deduped extracted comments into one combined file, then
# compiles and runs `tools/gru/GenerateSampleDefault.java` over that combined file: it feeds every
# comment through the real rule-based (linear) `CommentClassifier` and keeps only the high-confidence
# (YES/NO) decisions, writing them in `GruTrainer`'s RDD_EXT_20/21 schema to
# `tools/gru/sample_default.txt`, with a header documenting provenance. Unlike the old manual workflow,
# nothing here is archived to the personal GruArtifacts directory -- the classifier-labeled
# `sample_default.txt` output is the artifact going forward.
#
# After GenerateSampleDefault runs, this target also appends acquire_corpus.sh's
# classifier_weights_examples.tsv (the hand-labeled tools/classifier_weights/examples_*.md rows,
# already RDD_EXT_21 schema) directly onto sample_default.txt, then exact-duplicate-line dedups the
# result -- these carry a human-reviewed ground-truth label already, so they're appended as-is
# rather than re-labeled by CommentClassifier (see acquire_corpus.sh's own comment for why).
#
# This single baked-in copy is DELIBERATELY a separate, smaller guarantee than gru-train's
# GRU_HAND_LABELED_REPEAT oversampling (below) -- the two stack, they aren't the same mechanism
# twice. This append makes the hard cases present at least once in the committed base corpus, so
# even a bare `make gru-train` with GRU_HAND_LABELED_REPEAT unset still trains on them. Repeat
# oversampling exists because presence alone wasn't enough: the hard cases are only ~0.3% of
# sample_default.txt's ~74000 rows, so a single copy's gradient signal gets swamped by the
# majority-YES auto-labeled examples (see STATE_AI.md's 65.2%-precision retrain finding, which
# trained on this one baked-in copy with no oversampling). Setting GRU_HAND_LABELED_REPEAT=N on
# top adds N further copies for that run, so the hard cases end up appearing 1 (this baseline) + N
# (oversample) times total -- intentional amplification, not double-counting the same thing.
#
# Usage:
#     make gru-acquire-corpus [GRU_CORPUS_OUT_DIR=/tmp/gru_corpus]
#
GRU_CORPUS_OUT_DIR           ?= /tmp/gru_corpus
GRU_SAMPLE_DEFAULT            = $(GRU_TOOLS_DIR)/sample_default.txt
GRU_DISAGREEMENT_CORRECTIONS  = $(GRU_TOOLS_DIR)/disagreement_corrections.txt

gru-acquire-corpus: $(JAR_FILE)
	@rm -rf $(GRU_CORPUS_OUT_DIR)
	@$(GRU_TOOLS_DIR)/acquire_corpus.sh --out-dir $(GRU_CORPUS_OUT_DIR)
	@cat $(GRU_CORPUS_OUT_DIR)/comments_*.txt > $(GRU_CORPUS_OUT_DIR)/comments_combined.txt
	@mkdir -p $(GRU_TOOLS_CLASSES)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -cp $(CLASS_DIR) -d $(GRU_TOOLS_CLASSES) $(GRU_TOOLS_DIR)/GenerateSampleDefault.java
	@$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) GenerateSampleDefault $(GRU_CORPUS_OUT_DIR)/comments_combined.txt $(GRU_SAMPLE_DEFAULT)
	@cat $(GRU_CORPUS_OUT_DIR)/classifier_weights_examples.tsv >> $(GRU_SAMPLE_DEFAULT)
	@python3 $(GRU_TOOLS_DIR)/apply_disagreement_corrections.py $(GRU_SAMPLE_DEFAULT) $(GRU_DISAGREEMENT_CORRECTIONS)
	@awk '!seen[$$0]++' $(GRU_SAMPLE_DEFAULT) > $(GRU_SAMPLE_DEFAULT).dedup && mv $(GRU_SAMPLE_DEFAULT).dedup $(GRU_SAMPLE_DEFAULT)
	@LINES=$$(wc -l < $(GRU_SAMPLE_DEFAULT) 2>/dev/null || echo '?'); \
	echo "gru-acquire-corpus: wrote $(GRU_SAMPLE_DEFAULT) ($$LINES line(s), incl. tools/classifier_weights/examples_*.md hand-labeled rows + tools/gru/disagreement_corrections.txt)"

# ── GRU hand-labeled examples (RDD_EXT_21) target ─────────────────────────────
#
# Converts tools/classifier_weights/examples_{c,cpp,java,kotlin,js,ts}.md (the same hand-labeled
# hard-case tables `make gru-acquire-corpus` folds into sample_default.txt) into a standalone
# RDD_EXT_21-schema file via convert_classifier_weights_examples.py, without running the rest of
# corpus acquisition (no cloning/extraction needed -- these rows are already human-labeled). This
# is the file `gru-train`'s optional --hand-labeled oversampling (below) points at. Per RDD_EXT_19
# this derived artifact is never committed -- it's written under $(GRU_BUILD_DIR), not the repo.
#
GRU_HAND_LABELED_EXAMPLES = $(GRU_BUILD_DIR)/classifier_weights_examples.tsv

gru-hand-labeled-examples:
	@mkdir -p $(GRU_BUILD_DIR)
	@python3 $(GRU_TOOLS_DIR)/convert_classifier_weights_examples.py tools/classifier_weights --out $(GRU_HAND_LABELED_EXAMPLES)
	@LINES=$$(wc -l < $(GRU_HAND_LABELED_EXAMPLES) 2>/dev/null || echo '?'); \
	echo "gru-hand-labeled-examples: wrote $(GRU_HAND_LABELED_EXAMPLES) ($$LINES line(s))"

# ── GRU trainer target ───────────────────────────────────────────────────────
#
# Compiles and runs `tools/gru/GruTrainer.java` (a non-shipped, outside-src/training entry point per
# `STATE_AI.md`), training against `tools/gru/sample_default.txt` (the classifier-auto-labeled corpus
# `make gru-acquire-corpus` produces -- run that target first at least once) and writing a real
# trained weights file to `$(BUILD_DIR)/gru/$(GRU_WEIGHTS_FILENAME)`.
#
# `Config.DEFAULT_GRU_WEIGHTS_PATH` is empty by default -- `GruAbstainResolver` derives the weights-file location
# from the running program's own directory instead (the jar's parent dir when run via `-jar`, or `$(CLASS_DIR)`
# itself for a dev/test run against classes -- see `GruAbstainResolver.programDirectory`/WEIGHTS_FILENAME).
# So after writing the build-artifact weights file, this target also copies it into `$(CLASS_DIR)` under that
# expected filename, so `make test`/developer runs against `$(CLASS_DIR)` can find it, and into the formatter
# root (`code-formatter-ai-assist-weights.json`) so it can be committed alongside `sample_default.txt`.
#
# GRU_HAND_LABELED_REPEAT (default 0, i.e. off) wires GruTrainer's --hand-labeled/--hand-labeled-repeat
# oversampling (see tools/gru/README.txt) to the hard-case set gru-hand-labeled-examples produces --
# set it to a positive integer to repeat those rows N extra times in the training split, e.g.
# `make gru-train GRU_HAND_LABELED_REPEAT=3`. Left at 0, gru-train's behavior is unchanged from before
# oversampling support existed.
#
# This stacks with, rather than duplicates, gru-acquire-corpus's single baked-in copy of the same
# hand-labeled rows in sample_default.txt (see that target's comment for why the baseline copy
# alone isn't enough) -- with GRU_HAND_LABELED_REPEAT=N, a hard-case row appears 1 + N times in
# that training run: once from sample_default.txt, N more from this oversampling pass.
#
GRU_WEIGHTS_FILENAME     = code-formatter-ai-assist-weights.json
GRU_WEIGHTS_OUT          = $(GRU_BUILD_DIR)/$(GRU_WEIGHTS_FILENAME)
GRU_CKPT_CURRENT         = $(GRU_WEIGHTS_OUT).ckpt-current.bin
GRU_SAMPLE_EXAMPLES      = $(GRU_SAMPLE_DEFAULT)
GRU_TRAIN_ARGS          ?= --threads=3 --epochs=9 --patience=3 --batch-size=16 --progress-every=2500
GRU_HAND_LABELED_REPEAT ?= 3

gru-train: $(JAR_FILE)
	@mkdir -p $(GRU_TOOLS_CLASSES)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -cp $(CLASS_DIR) -d $(GRU_TOOLS_CLASSES) $(GRU_TOOLS_DIR)/GruTrainer.java
	@if [ "$(GRU_HAND_LABELED_REPEAT)" != "0" ]; then \
		$(MAKE) gru-hand-labeled-examples;        \
	fi
	@if [ -f $(GRU_CKPT_CURRENT) ]; then                                                                                                                                                                                                                                                                     \
		echo "gru-train: found $(GRU_CKPT_CURRENT), resuming";                                                                                                                                                                                                                                           \
		$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) GruTrainer $(GRU_SAMPLE_EXAMPLES) $(GRU_WEIGHTS_OUT) $(GRU_TRAIN_ARGS) --resume=$(GRU_CKPT_CURRENT) $(if $(filter-out 0,$(GRU_HAND_LABELED_REPEAT)),--hand-labeled=$(GRU_HAND_LABELED_EXAMPLES) --hand-labeled-repeat=$(GRU_HAND_LABELED_REPEAT)); \
	else                                                                                                                                                                                                                                                                                                     \
		$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) GruTrainer $(GRU_SAMPLE_EXAMPLES) $(GRU_WEIGHTS_OUT) $(GRU_TRAIN_ARGS) $(if $(filter-out 0,$(GRU_HAND_LABELED_REPEAT)),--hand-labeled=$(GRU_HAND_LABELED_EXAMPLES) --hand-labeled-repeat=$(GRU_HAND_LABELED_REPEAT));                              \
	fi
	@cp $(GRU_WEIGHTS_OUT) $(CLASS_DIR)
	@cp $(GRU_WEIGHTS_OUT) .

# ── GRU cross-validation on the full corpus ──────────────────────────────────
#
# Runs tools/gru/cross_validate.py's Monte Carlo 5-round CV directly agains the full `sample_default.txt`
# corpus (run `gru-acquire-corpus` first at least once) instead of the small 522-row hand-labeled bench --
# this is the only way to directly measure whether a `sample_default.txt/disagreement_corrections.txt`
# change (e.g. the 2026-08-04 44-row correction batch) actually moved held-out precision, since `cross_validate.py`
# trains purely from whatever file it's pointed at. Each of the 5 rounds retrains from scratch on an 80%
# split of the FULL corpus, i.e. is roughly one full `gru-train`-sized training run (the ~2700-4050s single run
# you already timed) -- so the whole target is a multi-hour job. Logs everything to $(GRU_CV_LOG) so it
# can be kicked off unattended (e.g. `nohup make gru-cv-corpus &`, or just run it directly on a machine you
# don't need back for a while) and checked on later.
#
# Usage:
#     make gru-cv-corpus
#     make gru-cv-corpus GRU_CV_ROUNDS=3 GRU_CV_LOG=/tmp/my_cv.log
#
# CM5:
#     ( make gru-cv-corpus; cd ~; data-ro.sh ) < /dev/null >> /tmp/gru_cv_corpus.out 2>&1 &
#     clear && cat /tmp/gru_cv_corpus.out && echo && cat /mnt/data/pi/JxMake/util/CodingStyle.md/formatter/target/gru/cv_corpus.log && echo
#
GRU_CV_ROUNDS   ?= 5
GRU_CV_WORK_DIR ?= $(GRU_BUILD_DIR)/cv_corpus
GRU_CV_LOG      ?= $(GRU_BUILD_DIR)/cv_corpus.log
GRU_CV_ARGS     ?= --epochs 9 --patience 3 --eval-threshold 0.7 --progress-every=5000

gru-cv-corpus: $(JAR_FILE)
	@mkdir -p $(GRU_BUILD_DIR)
	@echo "gru-cv-corpus: starting $(GRU_CV_ROUNDS)-round CV on $(GRU_SAMPLE_DEFAULT), logging to $(GRU_CV_LOG)"
	@echo "gru-cv-corpus: this retrains from scratch $(GRU_CV_ROUNDS) times on the full corpus -- expect several hours"
	@python3 $(GRU_TOOLS_DIR)/cross_validate.py $(GRU_SAMPLE_DEFAULT) --rounds $(GRU_CV_ROUNDS) --work-dir $(GRU_CV_WORK_DIR) $(GRU_CV_ARGS) >> $(GRU_CV_LOG) 2>&1
	@echo "gru-cv-corpus: done, see $(GRU_CV_LOG)"
	@tail -n 8 $(GRU_CV_LOG)


# ── Test target ───────────────────────────────────────────────────────────────
#
# Runs two passes over every file pair in $(TEST_DIR):
#
#     Forward pass:    format *_inp.<ext>  → diff result vs *_out.<ext>
#     Idempotency:     format *_out.<ext>  → diff result vs *_out.<ext>
#
# Uses `--standalone` to avoid depending on a running server instance.
# On any diff mismatch the loop prints the offending file and exits non-zero.
#
# Override `JAVA` or `JAR_FILE` on the command line if needed:
#     make test JAVA=/path/to/java
#     make test JAR_FILE=/path/to/code-formatter-1.0.0.jar
#
INP_FILES :=

INP_FILES += in_file_config_inp.hpp
INP_FILES += in_file_config_inp.java
INP_FILES += in_file_config_inp.kt
# - Deliberately error-only (duplicate `JXM_CFMT_CFG` directive).
# - It will always FAIL the normal `diff` loop, since the formatter exits 1 with no output for it.
# - It is kept commented out so `make test` stays green; uncomment to exercise its hard-error path manually.
# - See `test/in_file_config_error_out.hpp` for the expected error message.
#INP_FILES += in_file_config_error_inp.hpp

INP_FILES += h_core_inp.h
INP_FILES += c_core_inp.c
INP_FILES += h_combined_inp.h
INP_FILES += c_combined_inp.c
INP_FILES += c_comments_inp.c
INP_FILES += c_cpp_decl_gaps_inp.c

INP_FILES += hpp_core_inp.hpp
INP_FILES += cpp_core_inp.cpp
INP_FILES += cpp_modern_inp.cpp
INP_FILES += hpp_combined_inp.hpp
INP_FILES += cpp_combined_inp.cpp
INP_FILES += cpp_comments_inp.cpp

INP_FILES += java_core_inp.java
INP_FILES += java_modern_inp.java
INP_FILES += java_combined_inp.java
INP_FILES += java_comments_inp.java
INP_FILES += java_format_toggle_inp.java
INP_FILES += java_preprocessor_method_inp.java

INP_FILES += kt_combined_inp.kt
INP_FILES += kt_comments_inp.kt

INP_FILES += json_core_inp.json
INP_FILES += json5_core_inp.json5
INP_FILES += json5_comments_inp.json5
INP_FILES += json5_comment_banner_inp.json5

INP_FILES += css_combined_inp.css
INP_FILES += css_comments_inp.css

INP_FILES += yaml_core_inp.yaml
INP_FILES += yaml_comments_inp.yaml
INP_FILES += yaml_comment_chain_inp.yaml

INP_FILES += toml_core_inp.toml
INP_FILES += toml_comments_inp.toml
INP_FILES += toml_comment_chain_inp.toml

INP_FILES += xml_combined_inp.xml
INP_FILES += xml_comments_inp.xml
INP_FILES += xml_indent_auto_tabs_inp.xml
INP_FILES += xml_indent_auto_spaces_inp.xml
INP_FILES += xml_indent_auto_fallback_inp.xml
INP_FILES += xml_mixed_content_inp.xml

INP_FILES += cpp26_core_inp.cpp
INP_FILES += cpp26_comments_inp.cpp
INP_FILES += cpp26_reflection_inp.cpp

INP_FILES += js_combined_inp.js
INP_FILES += js_comments_inp.js
INP_FILES += js_getter_setter_asi_inp.js
INP_FILES += js_import_ordering_comments_inp.js
INP_FILES += js_nested_template_literal_inp.js

INP_FILES += ts_combined_inp.ts
INP_FILES += ts_comments_inp.ts
INP_FILES += ts_decl_grid_ext_inp.ts

INP_FILES += html_combined_inp.html
INP_FILES += html_comments_inp.html
INP_FILES += html_multiline_comment_verbatim_inp.html
INP_FILES += html_multiline_comment_banner_inp.html
INP_FILES += html_tc_gap_level0_body_unchanged_inp.html
INP_FILES += html_tc_gap_level1_body_insertion_inp.html
INP_FILES += html_tc_gap_level1_no_head_inp.html
INP_FILES += html_tc_gap_level1_foster_unchanged_inp.html
INP_FILES += html_tc_gap_level2_foster_parenting_inp.html
INP_FILES += html_tc_gap_level2_form_unchanged_inp.html
INP_FILES += html_tc_gap_level3_form_template_inp.html
INP_FILES += html_tc_gap_level3_adoption_unchanged_inp.html
INP_FILES += html_tc_gap_level4_adoption_agency_inp.html

INP_FILES += py_combined_inp.py
INP_FILES += py_comments_inp.py
INP_FILES += py_comments_normalization_inp.py
INP_FILES += py_import_blank_lines_inp.py
INP_FILES += py_import_multiline_inp.py

INP_FILES += makefile_combined_inp.mk
INP_FILES += bash_combined_inp.sh
INP_FILES += powershell_combined_inp.ps1

INP_FILES += curly_general_scope_reindent_inp.hpp
INP_FILES += java_flush_left_inp.java
INP_FILES += curly_gdr_multipass_inp.java
INP_FILES += curly_gdr_js_regex_inp.ts
INP_FILES += html_js_flush_left_inp.html

INP_FILES += multi_sentence_comment_inp.java
INP_FILES += multi_sentence_comment_inp.sh

INP_FILES += real_code_regressions_1_inp.cpp
INP_FILES += real_code_regressions_2_inp.java
INP_FILES += real_code_regressions_3_inp.java
INP_FILES += real_code_regressions_4_inp.hpp
INP_FILES += real_code_regressions_5_inp.cpp
INP_FILES += real_code_regressions_6_inp.java
INP_FILES += real_code_regressions_7_inp.java
INP_FILES += real_code_regressions_8_inp.java
INP_FILES += real_code_regressions_9_inp.java
INP_FILES += real_code_regressions_10_inp.java
INP_FILES += real_code_regressions_11_inp.c
INP_FILES += real_code_regressions_12_inp.hpp
INP_FILES += real_code_regressions_13_inp.hpp
INP_FILES += real_code_regressions_14_inp.hpp
INP_FILES += real_code_regressions_15_inp.hpp
INP_FILES += real_code_regressions_16_inp.hpp
INP_FILES += real_code_regressions_17_inp.kt
INP_FILES += real_code_regressions_18_inp.kt
INP_FILES += real_code_regressions_19_inp.kt
INP_FILES += real_code_regressions_20_inp.kt
INP_FILES += real_code_regressions_21_inp.kt
INP_FILES += real_code_regressions_22_inp.kt
INP_FILES += real_code_regressions_23_inp.kt
INP_FILES += real_code_regressions_24_inp.kt
INP_FILES += real_code_regressions_25_inp.kt
INP_FILES += real_code_regressions_26_inp.kt
INP_FILES += real_code_regressions_27_inp.kt
INP_FILES += real_code_regressions_28_inp.hpp
INP_FILES += real_code_regressions_29_inp.java
INP_FILES += real_code_regressions_30_inp.kt
INP_FILES += real_code_regressions_31_inp.kt
INP_FILES += real_code_regressions_32_inp.kt
INP_FILES += real_code_regressions_33_inp.kt
INP_FILES += real_code_regressions_34_inp.hpp
INP_FILES += real_code_regressions_35_inp.hpp
INP_FILES += real_code_regressions_36_inp.cpp
INP_FILES += real_code_regressions_37_inp.kt
INP_FILES += real_code_regressions_38_inp.kt
INP_FILES += real_code_regressions_39_inp.kt
INP_FILES += real_code_regressions_40_inp.kt
INP_FILES += real_code_regressions_41_inp.kt
INP_FILES += real_code_regressions_42_inp.kt
INP_FILES += real_code_regressions_43_inp.kt
INP_FILES += real_code_regressions_44_inp.kt
INP_FILES += real_code_regressions_45_inp.kt
INP_FILES += real_code_regressions_46_inp.kt
INP_FILES += real_code_regressions_47_inp.kt
INP_FILES += real_code_regressions_48_inp.kt
INP_FILES += real_code_regressions_49_inp.kt
INP_FILES += real_code_regressions_50_inp.cpp
INP_FILES += real_code_regressions_51_inp.cpp
INP_FILES += real_code_regressions_52_inp.cpp
INP_FILES += real_code_regressions_53_inp.cpp
INP_FILES += real_code_regressions_54_inp.java
INP_FILES += real_code_regressions_55_inp.java
INP_FILES += real_code_regressions_56_inp.java
INP_FILES += real_code_regressions_57_inp.java
INP_FILES += real_code_regressions_58_inp.java
INP_FILES += real_code_regressions_59_inp.kt
INP_FILES += real_code_regressions_60_inp.kt
INP_FILES += real_code_regressions_61_inp.kt
INP_FILES += real_code_regressions_62_inp.kt
INP_FILES += real_code_regressions_63_inp.kt
INP_FILES += real_code_regressions_64_inp.kt
INP_FILES += real_code_regressions_65_inp.java
INP_FILES += real_code_regressions_66_inp.java
INP_FILES += real_code_regressions_67_inp.hpp
INP_FILES += real_code_regressions_68_inp.json
INP_FILES += real_code_regressions_69_inp.css
INP_FILES += real_code_regressions_70_inp.toml
INP_FILES += real_code_regressions_71_inp.yaml
INP_FILES += real_code_regressions_72_inp.yaml
INP_FILES += real_code_regressions_73_inp.yaml
INP_FILES += real_code_regressions_74_inp.svg
INP_FILES += real_code_regressions_75_inp.yaml
INP_FILES += real_code_regressions_76_inp.hpp
INP_FILES += real_code_regressions_77_inp.js
INP_FILES += real_code_regressions_78_inp.py
INP_FILES += real_code_regressions_79_inp.py
INP_FILES += real_code_regressions_80_inp.py
INP_FILES += real_code_regressions_81_inp.ts
INP_FILES += real_code_regressions_82_inp.ts
INP_FILES += real_code_regressions_83_inp.yaml
INP_FILES += real_code_regressions_84_inp.ts
INP_FILES += real_code_regressions_85_inp.ts
INP_FILES += real_code_regressions_86_inp.yaml
INP_FILES += real_code_regressions_87_inp.ts
INP_FILES += real_code_regressions_88_inp.ts
INP_FILES += real_code_regressions_89_inp.ts
INP_FILES += real_code_regressions_90_inp.ts
INP_FILES += real_code_regressions_91_inp.xsl
INP_FILES += real_code_regressions_92_inp.xsd
INP_FILES += real_code_regressions_93_inp.ts
INP_FILES += real_code_regressions_94_inp.js
INP_FILES += real_code_regressions_95_inp.java
INP_FILES += real_code_regressions_96_inp.ts
INP_FILES += real_code_regressions_97_inp.ts
INP_FILES += real_code_regressions_98_inp.ts
INP_FILES += real_code_regressions_99_inp.ts
INP_FILES += real_code_regressions_100_inp.ts
INP_FILES += real_code_regressions_101_inp.ts
INP_FILES += real_code_regressions_102_inp.ts
INP_FILES += real_code_regressions_103_inp.html
INP_FILES += real_code_regressions_104_inp.html
INP_FILES += real_code_regressions_105_inp.ts
INP_FILES += real_code_regressions_107_inp.ts
INP_FILES += real_code_regressions_106_inp.html
INP_FILES += real_code_regressions_108_inp.html
INP_FILES += real_code_regressions_109_inp.html
INP_FILES += real_code_regressions_110_inp.html
INP_FILES += real_code_regressions_111_inp.html
INP_FILES += real_code_regressions_112_inp.html
INP_FILES += real_code_regressions_113_inp.java
INP_FILES += real_code_regressions_114_inp.py
INP_FILES += real_code_regressions_115_inp.py
INP_FILES += real_code_regressions_116_inp.py
INP_FILES += real_code_regressions_117_inp.py
INP_FILES += real_code_regressions_118_inp.hpp
INP_FILES += real_code_regressions_119_inp.hpp
INP_FILES += real_code_regressions_120_inp.hpp
INP_FILES += real_code_regressions_121_inp.hpp
INP_FILES += real_code_regressions_122_inp.hpp
INP_FILES += real_code_regressions_123_inp.hpp
INP_FILES += real_code_regressions_124_inp.hpp
INP_FILES += real_code_regressions_125_inp.html
INP_FILES += real_code_regressions_126_inp.java
INP_FILES += real_code_regressions_127_inp.py
INP_FILES += real_code_regressions_128_inp.java
INP_FILES += real_code_regressions_129_inp.java
INP_FILES += real_code_regressions_130_inp.java
INP_FILES += real_code_regressions_131_inp.java
INP_FILES += real_code_regressions_132_inp.java
INP_FILES += real_code_regressions_133_inp.py
INP_FILES += real_code_regressions_134_inp.ts
INP_FILES += real_code_regressions_135_inp.ts
INP_FILES += real_code_regressions_136_inp.ts
INP_FILES += real_code_regressions_137_inp.py
INP_FILES += real_code_regressions_138_inp.py
INP_FILES += real_code_regressions_139_inp.py
INP_FILES += real_code_regressions_140_inp.ts
INP_FILES += real_code_regressions_141_inp.ts
INP_FILES += real_code_regressions_142_inp.ts
INP_FILES += real_code_regressions_143_inp.ts
INP_FILES += real_code_regressions_144_inp.kt
INP_FILES += real_code_regressions_145_inp.ts
INP_FILES += real_code_regressions_146_inp.kt
INP_FILES += real_code_regressions_147_inp.ts
INP_FILES += real_code_regressions_148_inp.kt
INP_FILES += real_code_regressions_149_inp.kt
INP_FILES += real_code_regressions_150_inp.kt
INP_FILES += real_code_regressions_151_inp.kt
INP_FILES += real_code_regressions_152_inp.kt
INP_FILES += real_code_regressions_153_inp.kt
INP_FILES += real_code_regressions_154_inp.kt
INP_FILES += real_code_regressions_155_inp.kt
INP_FILES += real_code_regressions_156_inp.kt
INP_FILES += real_code_regressions_157_inp.kt
INP_FILES += real_code_regressions_158_inp.kt
INP_FILES += real_code_regressions_159_inp.kt
INP_FILES += real_code_regressions_160_inp.kt
INP_FILES += real_code_regressions_161_inp.kt
INP_FILES += real_code_regressions_162_inp.kt
INP_FILES += real_code_regressions_163_inp.kt
INP_FILES += real_code_regressions_164_inp.kt
INP_FILES += real_code_regressions_165_inp.kt
INP_FILES += real_code_regressions_166_inp.kt
INP_FILES += real_code_regressions_167_inp.kt
INP_FILES += real_code_regressions_168_inp.kt
INP_FILES += real_code_regressions_169_inp.kt
INP_FILES += real_code_regressions_170_inp.kt
INP_FILES += real_code_regressions_171_inp.java
INP_FILES += real_code_regressions_172_inp.ts
INP_FILES += real_code_regressions_173_inp.html
INP_FILES += real_code_regressions_174_inp.html
INP_FILES += real_code_regressions_175_inp.html
INP_FILES += real_code_regressions_176_inp.java
INP_FILES += real_code_regressions_177_inp.ts
INP_FILES += real_code_regressions_178_inp.py
INP_FILES += real_code_regressions_179_inp.ts
INP_FILES += real_code_regressions_180_inp.ts
INP_FILES += real_code_regressions_181_inp.java
INP_FILES += real_code_regressions_182_inp.ps1
INP_FILES += real_code_regressions_183_inp.js
INP_FILES += real_code_regressions_184_inp.ts
INP_FILES += real_code_regressions_185_inp.ts
INP_FILES += real_code_regressions_186_inp.ts
INP_FILES += real_code_regressions_187_inp.java
INP_FILES += real_code_regressions_188_inp.sh
INP_FILES += real_code_regressions_189_inp.sh
INP_FILES += real_code_regressions_190_inp.sh
INP_FILES += real_code_regressions_191_inp.ps1
INP_FILES += real_code_regressions_192_inp.ps1
INP_FILES += real_code_regressions_193_inp.java
INP_FILES += real_code_regressions_194_inp.ts
INP_FILES += real_code_regressions_195_inp.ts
INP_FILES += real_code_regressions_196_inp.ts
INP_FILES += real_code_regressions_197_inp.ts
INP_FILES += real_code_regressions_198_inp.ts
INP_FILES += real_code_regressions_199_inp.kt

REF_FILES := $(subst _inp,_out,$(INP_FILES))

test: $(JAR_FILE)
	@mkdir -p $(BUILD_DIR)
	@flock $(TEST_LOCK) $(MAKE) --no-print-directory _test_serial

# Same as `test`, but hides the JAR's own stderr (e.g. the per-file/per-request diagnostic
# trace added to Main.java/ServerMode.java) -- test PASS/FAIL/diff output is unaffected, since
# that's all written by this Makefile's own `echo`/`diff`, not the JAR.
test-quiet: $(JAR_FILE)
	@mkdir -p $(BUILD_DIR)
	@flock $(TEST_LOCK) $(MAKE) --no-print-directory _test_serial TEST_STDERR=/dev/null

# Copies the root-level weights file into $(BUILD_DIR) so tests/bench/server have something to load.
# Guarded so a fresher copy already sitting in $(BUILD_DIR) -- e.g. one `gru-train` just produced there,
# even if `gru-train` itself then failed to propagate it up to the root -- is never overwritten by this sync.
gru-sync-weights:
	@if [ ! -f "$(BUILD_DIR)/$(GRU_WEIGHTS_FILENAME)" ] && [ -f "$(GRU_WEIGHTS_FILENAME)" ]; then \
		cp "$(GRU_WEIGHTS_FILENAME)" "$(BUILD_DIR)/$(GRU_WEIGHTS_FILENAME)";                  \
	fi

# Actual `test` body, run one-at-a-time under `test`'s `flock` -- concurrent # `make test` invocations
# (e.g. from parallel dogfood sessions) share the same `$(TEST_TMP)` directory, so running them simultaneously
# would race on it (one invocation's `rm $(TEST_TMP)/*` deleting another's in-flight results). `flock` makes
# a second concurrent call block and queue instead of racing.
TEST_STDERR ?= /dev/stderr

_test_serial: gru-sync-weights
	@(                                                                                              \
		export JXMAKE_CODE_FORMATTER_FORMAT_MACROS=on;                                          \
		mkdir -p $(TEST_TMP);                                                                   \
		rm $(TEST_TMP)/*;                                                                       \
		clear;                                                                                  \
		echo "";                                                                                \
		echo "=== Forward pass (inp → out) ===";                                                \
		passed="$(TEST_TMP)/passed.list";                                                       \
		rm -f "$$passed";                                                                       \
		fail=0;                                                                                 \
		forward_pass=0;                                                                         \
		forward_total=0;                                                                        \
		idem_pass=0;                                                                            \
		idem_total=0;                                                                           \
		set -- $(addprefix $(TEST_DIR)/,$(INP_FILES));                                          \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$@" 2>$(TEST_STDERR);         \
		for inp in "$$@"; do                                                                    \
			base=$$(basename "$$inp");                                                      \
			ext="$${base##*_inp.}";                                                         \
			stem="$${base%_inp.*}";                                                         \
			out="$(TEST_DIR)/$${stem}_out.$$ext";                                           \
			if [ ! -f "$$out" ]; then                                                       \
				echo "SKIP $$inp (no matching $$out)";                                  \
				continue;                                                               \
			fi;                                                                             \
			forward_total=$$((forward_total + 1));                                          \
			result="$(TEST_TMP)/$$base";                                                    \
			if diff -u "$$out" "$$result" > /dev/null 2>&1; then                            \
				echo "PASS $$inp";                                                      \
				echo "$$out" >> "$$passed";                                             \
				forward_pass=$$((forward_pass + 1));                                    \
			else                                                                            \
				echo "FAIL $$inp";                                                      \
				diff -u "$$out" "$$result";                                             \
				fail=1;                                                                 \
			fi;                                                                             \
		done;                                                                                   \
		echo "";                                                                                \
		echo "=== Idempotency pass (out → out) ===";                                            \
		count=0;                                                                                \
		set --;                                                                                 \
		while IFS= read -r out; do                                                              \
			set -- "$$@" "$$out";                                                           \
			count=$$((count + 1));                                                          \
		done < "$$passed";                                                                      \
		if [ $$count -gt 0 ]; then                                                              \
			idem_total=$$count;                                                             \
			$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$@" 2>$(TEST_STDERR); \
			for out in "$$@"; do                                                            \
				base=$$(basename "$$out");                                              \
				result="$(TEST_TMP)/$$base";                                            \
				if diff -u "$$out" "$$result" > /dev/null 2>&1; then                    \
					echo "PASS $$out";                                              \
					idem_pass=$$((idem_pass + 1));                                  \
				else                                                                    \
					echo "FAIL $$out (not idempotent)";                             \
					diff -u "$$out" "$$result";                                     \
					fail=1;                                                         \
				fi;                                                                     \
			done;                                                                           \
		fi;                                                                                     \
		rm -f "$$passed";                                                                       \
		echo "";                                                                                \
		if [ $$fail -ne 0 ]; then exit 1; fi;                                                   \
		printf "All tests passed:\n";                                                           \
		printf "    %3d/%3d forward\n"     $$forward_pass $$forward_total;                      \
		printf "    %3d/%3d idempotency\n" $$idem_pass $$idem_total;                            \
		printf "\n";                                                                            \
	)


# ── Server-mode test target ───────────────────────────────────────────────────
#
# Functional (not benchmark) coverage of the /format inline-config query parameters added for
# browser/no-real-file clients, plus general dispatch coverage now that data formats/JS-TS/
# Python3/HTML5 are all real (no longer scaffold-only), so JSX/TSX are the only languages left
# to exercise the scaffold-only-rejection path:
#
#     1. inline config, no `path` (path optional when lang + inline present)
#     2. inline config overriding a `.jxmake-code-formatter` file next to `path`
#     3. an unrecognized config-shaped query key -> HTTP 400
#     4. language auto-inferred from `path`'s extension, no explicit `lang`
#     5. a full round-trip through a non-Java language (yaml), proving the server actually reformats
#        rather than echoing the body back verbatim
#     6. an unrecognized `lang` (e.g. `jsx` -- no longer even scaffold-only, since
#        `Lang.SCAFFOLD_ONLY_LANGUAGES` is now empty) -> HTTP 400
#
# Runs against a dedicated port so it doesn't collide with a server a developer already has running.
# Always stops its own server on exit, pass or fail.
#
TEST_SERVER_PORT := 17999
TEST_SERVER_TMP   = $(BUILD_DIR)/test-server-out

test-server: $(JAR_FILE) gru-sync-weights
	@(                                                                                                                        \
		mkdir -p $(TEST_SERVER_TMP);                                                                                      \
		fail=0;                                                                                                           \
		clear;                                                                                                            \
		echo "";                                                                                                          \
		printf 'class X {\n  void f() {\n    if (true) {\n      int y = 1;\n    }\n  }\n}\n'                              \
			> $(TEST_SERVER_TMP)/Sample.java;                                                                         \
		printf 'indent-size = 8\n' > $(TEST_SERVER_TMP)/.jxmake-code-formatter;                                           \
		printf 'foo:   bar                  \nbaz:\n\n\n\n' > $(TEST_SERVER_TMP)/Sample.yaml;                             \
		$(JAVA) -jar $(JAR_FILE) --server --port $(TEST_SERVER_PORT) 2>&1 &                                               \
		tries=0;                                                                                                          \
		while [ $$tries -lt 50 ]; do                                                                                      \
			if curl -s -o /dev/null "http://localhost:$(TEST_SERVER_PORT)/format?path=x&lang=java"                    \
				--data 'x'; then break; fi;                                                                       \
			tries=$$((tries + 1));                                                                                    \
			sleep 0.1;                                                                                                \
		done;                                                                                                             \
		echo "=== Server test: inline config, no path ===";                                                               \
		code=$$(curl -s -o $(TEST_SERVER_TMP)/r1 -w '%{http_code}'                                                        \
			"http://localhost:$(TEST_SERVER_PORT)/format?lang=java&indent-size=2"                                     \
			--data-binary @$(TEST_SERVER_TMP)/Sample.java);                                                           \
		if [ "$$code" = "200" ]; then echo "PASS (HTTP $$code)"; else echo "FAIL (HTTP $$code)"; fail=1; fi;              \
		echo "";                                                                                                          \
		echo "=== Server test: inline config overrides file-based config ===";                                            \
		code=$$(curl -s -o $(TEST_SERVER_TMP)/r2 -w '%{http_code}'                                                        \
			"http://localhost:$(TEST_SERVER_PORT)/format?path=$(TEST_SERVER_TMP)/Sample.java&lang=java&indent-size=2" \
			--data-binary @$(TEST_SERVER_TMP)/Sample.java);                                                           \
		if [ "$$code" = "200" ] && diff -q $(TEST_SERVER_TMP)/r1 $(TEST_SERVER_TMP)/r2 > /dev/null 2>&1; then             \
			echo "PASS (HTTP $$code, inline result matches no-path result -- file's indent-size=8 was overridden)";   \
		else                                                                                                              \
			echo "FAIL (HTTP $$code, or inline override did not match)"; fail=1;                                      \
		fi;                                                                                                               \
		echo "";                                                                                                          \
		echo "=== Server test: unrecognized query key -> 400 ===";                                                        \
		code=$$(curl -s -o $(TEST_SERVER_TMP)/r3 -w '%{http_code}'                                                        \
			"http://localhost:$(TEST_SERVER_PORT)/format?lang=java&bogus-key=1"                                       \
			--data-binary @$(TEST_SERVER_TMP)/Sample.java);                                                           \
		if [ "$$code" = "400" ]; then echo "PASS (HTTP $$code)"; else echo "FAIL (HTTP $$code)"; fail=1; fi;              \
		echo "";                                                                                                          \
		echo "=== Server test: lang auto-inferred from path extension ===";                                               \
		code=$$(curl -s -o $(TEST_SERVER_TMP)/r4 -w '%{http_code}'                                                        \
			"http://localhost:$(TEST_SERVER_PORT)/format?path=$(TEST_SERVER_TMP)/Sample.yaml"                         \
			--data-binary @$(TEST_SERVER_TMP)/Sample.yaml);                                                           \
		if [ "$$code" = "200" ]; then echo "PASS (HTTP $$code, .yaml path inferred lang without 'lang=')";                \
		else echo "FAIL (HTTP $$code)"; fail=1; fi;                                                                       \
		echo "";                                                                                                          \
		echo "=== Server test: full round-trip through a non-Java language (yaml) ===";                                   \
		code=$$(curl -s -o $(TEST_SERVER_TMP)/r5 -w '%{http_code}'                                                        \
			"http://localhost:$(TEST_SERVER_PORT)/format?path=$(TEST_SERVER_TMP)/Sample.yaml&lang=yaml"               \
			--data-binary @$(TEST_SERVER_TMP)/Sample.yaml);                                                           \
		if [ "$$code" = "200" ] && ! diff -q $(TEST_SERVER_TMP)/Sample.yaml $(TEST_SERVER_TMP)/r5 > /dev/null 2>&1; then  \
			echo "PASS (HTTP $$code, output differs from input -- real formatting happened, not an echo)";            \
		else                                                                                                              \
			echo "FAIL (HTTP $$code, or output was an unmodified echo of the input)"; fail=1;                         \
		fi;                                                                                                               \
		echo "";                                                                                                          \
		echo "=== Server test: unrecognized lang (jsx) still rejected ===";                                               \
		code=$$(curl -s -o $(TEST_SERVER_TMP)/r6 -w '%{http_code}'                                                        \
			"http://localhost:$(TEST_SERVER_PORT)/format?path=$(TEST_SERVER_TMP)/Sample.yaml&lang=jsx"                \
			--data-binary @$(TEST_SERVER_TMP)/Sample.yaml);                                                           \
		if [ "$$code" = "400" ]; then echo "PASS (HTTP $$code, jsx correctly rejected)";                                  \
		else echo "FAIL (HTTP $$code)"; fail=1; fi;                                                                       \
		echo "";                                                                                                          \
		$(JAVA) -jar $(JAR_FILE) --port $(TEST_SERVER_PORT) --stop > /dev/null 2>&1 || true;                              \
		if [ $$fail -ne 0 ]; then exit 1; fi;                                                                             \
		echo "All server tests passed.";                                                                                  \
		echo "";                                                                                                          \
	)

# ── Server-concurrency load test ──────────────────────────────────────────────
#
# This target is the dedicated concurrent-load coverage the "server concurrency + client read-ahead".
# It starts a server with `server-concurrency = 4`, fires many concurrent requests at it for two distinct inputs
# (interleaved, so a race would corrupt one input with the other's output), and verifies every single response
# byte-for-byte matches the same request's single-threaded/serial result -- not just "no exception", which a data
# race could easily pass while still corrupting output. It runs on its own dedicated port so it doesn't collide
# with `test-server` or a developer's own running server.
#
TEST_SERVER_CONCURRENT_PORT := 17998
TEST_SERVER_CONCURRENT_TMP   = $(BUILD_DIR)/test-server-concurrent-out

test-server-concurrent: $(JAR_FILE) gru-sync-weights
	@(                                                                                                                                        \
		mkdir -p $(TEST_SERVER_CONCURRENT_TMP);                                                                                           \
		fail=0;                                                                                                                           \
		clear;                                                                                                                            \
		echo "";                                                                                                                          \
		printf 'class X {\n  void f() {\n    if (true) {\n      int y = 1;\n    }\n  }\n}\n'                                              \
			> $(TEST_SERVER_CONCURRENT_TMP)/A.java;                                                                                   \
		printf 'class Y {\n  void g() {\n    for (int i = 0; i < 9; i++) {\n      int z = 2;\n    }\n  }\n}\n'                            \
			> $(TEST_SERVER_CONCURRENT_TMP)/B.java;                                                                                   \
		echo "=== Server concurrency load test: computing single-threaded reference outputs ===";                                         \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_SERVER_CONCURRENT_TMP)/ref-a $(TEST_SERVER_CONCURRENT_TMP)/A.java 2>/dev/null; \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_SERVER_CONCURRENT_TMP)/ref-b $(TEST_SERVER_CONCURRENT_TMP)/B.java 2>/dev/null; \
		echo "PASS (single-threaded reference outputs computed)";                                                                         \
		echo "";                                                                                                                          \
		echo "=== Server concurrency load test: starting server-concurrency=4 ===";                                                       \
		JXMAKE_CODE_FORMATTER_SERVER_CONCURRENCY=4 $(JAVA) -jar $(JAR_FILE) --server --port $(TEST_SERVER_CONCURRENT_PORT) 2>&1 &         \
		tries=0;                                                                                                                          \
		while [ $$tries -lt 50 ]; do                                                                                                      \
			if curl -s -o /dev/null "http://localhost:$(TEST_SERVER_CONCURRENT_PORT)/format?path=x&lang=java"                         \
				--data 'x'; then break; fi;                                                                                       \
			tries=$$((tries + 1));                                                                                                    \
			sleep 0.1;                                                                                                                \
		done;                                                                                                                             \
		echo "PASS (server up, concurrency=4)";                                                                                           \
		echo "";                                                                                                                          \
		echo "=== Server concurrency load test: firing 80 interleaved concurrent requests (2 inputs x 40 each) ===";                      \
		pids="";                                                                                                                          \
		for i in $$(seq 1 40); do                                                                                                         \
			curl -s -o $(TEST_SERVER_CONCURRENT_TMP)/a-$$i.out                                                                        \
				"http://localhost:$(TEST_SERVER_CONCURRENT_PORT)/format?path=A.java&lang=java"                                    \
				--data-binary @$(TEST_SERVER_CONCURRENT_TMP)/A.java &                                                             \
			pids="$$pids $$!";                                                                                                        \
			curl -s -o $(TEST_SERVER_CONCURRENT_TMP)/b-$$i.out                                                                        \
				"http://localhost:$(TEST_SERVER_CONCURRENT_PORT)/format?path=B.java&lang=java"                                    \
				--data-binary @$(TEST_SERVER_CONCURRENT_TMP)/B.java &                                                             \
			pids="$$pids $$!";                                                                                                        \
		done;                                                                                                                             \
		wait $$pids;                                                                                                                      \
		echo "PASS (80 concurrent requests completed, no hang)";                                                                          \
		echo "";                                                                                                                          \
		echo "=== Server concurrency load test: verifying all 80 responses match single-threaded reference ===";                          \
		for i in $$(seq 1 40); do                                                                                                         \
			if ! diff -q $(TEST_SERVER_CONCURRENT_TMP)/a-$$i.out $(TEST_SERVER_CONCURRENT_TMP)/ref-a/A.java > /dev/null 2>&1; then    \
				echo "FAIL: a-$$i.out does not match reference (possible race/corruption)"; fail=1;                               \
			fi;                                                                                                                       \
			if ! diff -q $(TEST_SERVER_CONCURRENT_TMP)/b-$$i.out $(TEST_SERVER_CONCURRENT_TMP)/ref-b/B.java > /dev/null 2>&1; then    \
				echo "FAIL: b-$$i.out does not match reference (possible race/corruption)"; fail=1;                               \
			fi;                                                                                                                       \
		done;                                                                                                                             \
		if [ $$fail -eq 0 ]; then echo "PASS (all 80 concurrent responses byte-identical to single-threaded reference)"; fi;              \
		echo "";                                                                                                                          \
		$(JAVA) -jar $(JAR_FILE) --port $(TEST_SERVER_CONCURRENT_PORT) --stop > /dev/null 2>&1 || true;                                   \
		if [ $$fail -ne 0 ]; then exit 1; fi;                                                                                             \
		echo "All server-concurrency load tests passed.";                                                                                 \
		echo "";                                                                                                                          \
	)


# ── Bench target ─────────────────────────────────────────────────────────────
#
# Benchmarks total formatting time (excluding server start/stop) across the test files in various
# configurations:
#
#     1. `--standalone`, one file     per invocation
#     2. `--standalone`, all files in one invocation
#     3. client-server , one file     per invocation (1st run)
#     4. client-server , one file     per invocation (2nd run)
#     5. client-server , all files in one invocation (read-ahead=1)
#     6. client-server , all files in one invocation (read-ahead=N)
#
# Override `JAVA` or `JAR_FILE` on the command line if needed, same as `make test`.
#
bench: $(JAR_FILE) gru-sync-weights
	@(                                                                                                                                              \
		mkdir -p $(TEST_TMP);                                                                                                                   \
		files="$(addprefix $(TEST_DIR)/,$(INP_FILES))";                                                                                         \
		set -- $$files; probe="$$1";                                                                                                            \
		clear;                                                                                                                                  \
		echo "";                                                                                                                                \
		echo "=== Bench: standalone, one-by-one ===";                                                                                           \
		start=$$(date +%s%N);                                                                                                                   \
		for f in $$files; do                                                                                                                    \
			$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$f" 2>/dev/null;                                                      \
		done;                                                                                                                                   \
		end=$$(date +%s%N);                                                                                                                     \
		base_ns=$$(( end - start ));                                                                                                            \
		printf "Total: %5dmS\n" $$(( base_ns / 1000000 ));                                                                                      \
		echo "";                                                                                                                                \
		echo "=== Bench: standalone, all-at-once ===";                                                                                          \
		start=$$(date +%s%N);                                                                                                                   \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) $$files 2>/dev/null;                                                            \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		                                                                                                                                        \
		$(JAVA) -jar $(JAR_FILE) --server 2>/dev/null &                                                                                         \
		sleep 1;                                                                                                                                \
		echo "";                                                                                                                                \
		                                                                                                                                        \
		echo "=== Bench: client-server, one-by-one (1st run) ===";                                                                              \
		start=$$(date +%s%N);                                                                                                                   \
		for f in $$files; do                                                                                                                    \
			$(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) "$$f" 2>/dev/null;                                                                   \
		done;                                                                                                                                   \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		                                                                                                                                        \
		$(JAVA) -jar $(JAR_FILE) --stop;                                                                                                        \
		sleep 1;                                                                                                                                \
		CONCURRENCY=3;                                                                                                                          \
		READ_AHEAD=$$(( CONCURRENCY + 2 ));                                                                                                     \
		JXMAKE_CODE_FORMATTER_SERVER_CONCURRENCY=$$CONCURRENCY $(JAVA) -jar $(JAR_FILE) --server 2>/dev/null &                                  \
		sleep 1;                                                                                                                                \
		echo "";                                                                                                                                \
		                                                                                                                                        \
		echo "=== Bench: client-server, one-by-one (2nd run) ===";                                                                              \
		start=$$(date +%s%N);                                                                                                                   \
		for f in $$files; do                                                                                                                    \
			$(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) "$$f" 2>/dev/null;                                                                   \
		done;                                                                                                                                   \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		echo "=== Bench: client-server, all-at-once (read-ahead=1) ===";                                                                        \
		start=$$(date +%s%N);                                                                                                                   \
		$(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) $$files 2>/dev/null;                                                                         \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		echo "=== Bench: client-server, all-at-once (read-ahead=$$READ_AHEAD) ===";                                                             \
		start=$$(date +%s%N);                                                                                                                   \
		JXMAKE_CODE_FORMATTER_CLIENT_READ_AHEAD=$$READ_AHEAD $(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) $$files 2>/dev/null;                    \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		                                                                                                                                        \
		$(JAVA) -jar $(JAR_FILE) --stop;                                                                                                        \
		sleep 1;                                                                                                                                \
		echo "";                                                                                                                                \
	)
