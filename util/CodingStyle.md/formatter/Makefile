#
# Makefile for formatter
#

JAVAC        ?= javac
JAR          ?= jar
JAVA         ?= java
JAVA_VERSION ?= 8

# Keep version number consistent with README.md
VERSION       = 1.00
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

.PHONY: all build jar clean gru-extract-pool-a gru-extract-pool-b gru-measure-abstain-rate gru-train test _test_serial bench test-server

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
	@printf 'Main-Class: %s\n' '$(MAIN_CLASS)' > $(MANIFEST)
	@$(JAR) cfm $(JAR_FILE) $(MANIFEST) -C $(CLASS_DIR) .
	@echo "Built $(JAR_FILE)"
#	@cp $(JAR_FILE) .

clean:
	@rm -rvf $(BUILD_DIR)
	@rm -rvf tools/verifiers/__pycache__
	@rm -vf  tools/verifiers/*.class


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
	@if [ -z "$(GRU_ABSTAIN_INPUT)" ] || [ -z "$(GRU_POOL_A_OUT)" ]; then                                              \
		echo "gru-extract-pool-a: set GRU_ABSTAIN_INPUT=<path from extract_comments.py> GRU_POOL_A_OUT=<output path>"; \
		exit 2;                                                                                                        \
	fi
	@mkdir -p $(GRU_TOOLS_CLASSES)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -cp $(CLASS_DIR) -d $(GRU_TOOLS_CLASSES) $(GRU_TOOLS_DIR)/ExtractPoolA.java
	@$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) ExtractPoolA $(GRU_ABSTAIN_INPUT) $(GRU_POOL_A_OUT)

gru-extract-pool-b:
	@if [ -z "$(GRU_ABSTAIN_INPUT)" ] || [ -z "$(GRU_POOL_B_OUT)" ]; then                                              \
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


# ── GRU trainer target ───────────────────────────────────────────────────────
#
# Compiles and runs `tools/gru/GruTrainer.java` (a non-shipped, outside-src/training entry point per
# `STATE_AI.md`), writing a scalar-architecture- constants-only weights file to `$(BUILD_DIR)/gru/weights.json`.
# The real training loop remains blocked on `STATE_AI.md` open items 3/4/9/10; see `GruTrainer`'s own javadoc.
# Uses `tools/gru/sample_examples.txt`, a placeholder/dummy labeled-examples file (no real labeled dataset exists
# yet), just to satisfy `GruTrainer`'s positional-arg validation.
#
# `Config.DEFAULT_GRU_WEIGHTS_PATH` is empty by default -- `GruAbstainResolver` derives the weights-file location
# from the running program's own directory instead (the jar's parent dir when run via `-jar`, or `$(CLASS_DIR)`
# itself for a dev/test run against classes -- see `GruAbstainResolver.programDirectory`/WEIGHTS_FILENAME).
# So after writing the build-artifact weights file, this target also copies it into `$(CLASS_DIR)` under that
# expected filename, so `make test`/developer runs against `$(CLASS_DIR)` can find it. The commented-out `cp` below
# mirrors the similarly-commented-out `@cp $(JAR_FILE) .` above: when that jar-copy line is enabled (e.g. for
# the top-level dist_build/jxmake_dist packaging step, see `apps/code-formatter/` in that tree), enable this one
# alongside it so the weights file ships next to the jar the same way.
#
GRU_WEIGHTS_FILENAME = code-formatter-ai-assist-weights.json
GRU_WEIGHTS_OUT      = $(GRU_BUILD_DIR)/$(GRU_WEIGHTS_FILENAME)
GRU_SAMPLE_EXAMPLES  = $(GRU_TOOLS_DIR)/sample_examples.txt

gru-train: $(JAR_FILE)
	@mkdir -p $(GRU_TOOLS_CLASSES)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -cp $(CLASS_DIR) -d $(GRU_TOOLS_CLASSES) $(GRU_TOOLS_DIR)/GruTrainer.java
	@$(JAVA) -cp $(CLASS_DIR):$(GRU_TOOLS_CLASSES) GruTrainer $(GRU_SAMPLE_EXAMPLES) $(GRU_WEIGHTS_OUT)
	@cp $(GRU_WEIGHTS_OUT) $(CLASS_DIR)
#	@cp $(GRU_WEIGHTS_OUT) .


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
#     make test JAR_FILE=/path/to/code-formatter-1.00.jar
#
INP_FILES :=

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

INP_FILES += in_file_config_inp.hpp
INP_FILES += in_file_config_inp.java
INP_FILES += in_file_config_inp.kt
# - Deliberately error-only (duplicate `JXM_CFMT_CFG` directive).
# - It will always FAIL the normal `diff` loop, since the formatter exits 1 with no output for it.
# - It is kept commented out so `make test` stays green; uncomment to exercise its hard-error path manually.
# - See `test/in_file_config_error_out.hpp` for the expected error message.
#INP_FILES += in_file_config_error_inp.hpp

INP_FILES += json_core_inp.json
INP_FILES += json5_core_inp.json5
INP_FILES += json5_comments_inp.json5

INP_FILES += css_combined_inp.css
INP_FILES += css_comments_inp.css

INP_FILES += yaml_core_inp.yaml
INP_FILES += yaml_comments_inp.yaml

INP_FILES += toml_core_inp.toml
INP_FILES += toml_comments_inp.toml

INP_FILES += xml_combined_inp.xml
INP_FILES += xml_comments_inp.xml

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

INP_FILES += py_combined_inp.py
INP_FILES += py_comments_inp.py

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

REF_FILES := $(subst _inp,_out,$(INP_FILES))

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
test: $(JAR_FILE)
	@mkdir -p $(BUILD_DIR)
	@flock $(TEST_LOCK) $(MAKE) --no-print-directory _test_serial

# Actual `test` body, run one-at-a-time under `test`'s `flock` -- concurrent # `make test` invocations
# (e.g. from parallel dogfood sessions) share the same `$(TEST_TMP)` directory, so running them simultaneously
# would race on it (one invocation's `rm $(TEST_TMP)/*` deleting another's in-flight results). `flock` makes
# a second concurrent call block and queue instead of racing.
_test_serial:
	@(                                                                             \
		export JXMAKE_CODE_FORMATTER_FORMAT_MACROS=on;                         \
		mkdir -p $(TEST_TMP);                                                  \
		rm $(TEST_TMP)/*;                                                      \
		clear;                                                                 \
		echo "";                                                               \
		echo "=== Forward pass (inp → out) ===";                               \
		passed="$(TEST_TMP)/passed.list";                                      \
		rm -f "$$passed";                                                      \
		fail=0;                                                                \
		forward_pass=0;                                                        \
		forward_total=0;                                                       \
		idem_pass=0;                                                           \
		idem_total=0;                                                          \
		set -- $(addprefix $(TEST_DIR)/,$(INP_FILES));                         \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$@";         \
		for inp in "$$@"; do                                                   \
			base=$$(basename "$$inp");                                     \
			ext="$${base##*_inp.}";                                        \
			stem="$${base%_inp.*}";                                        \
			out="$(TEST_DIR)/$${stem}_out.$$ext";                          \
			if [ ! -f "$$out" ]; then                                      \
				echo "SKIP $$inp (no matching $$out)";                 \
				continue;                                              \
			fi;                                                            \
			forward_total=$$((forward_total + 1));                         \
			result="$(TEST_TMP)/$$base";                                   \
			if diff -u "$$out" "$$result" > /dev/null 2>&1; then           \
				echo "PASS $$inp";                                     \
				echo "$$out" >> "$$passed";                            \
				forward_pass=$$((forward_pass + 1));                   \
			else                                                           \
				echo "FAIL $$inp";                                     \
				diff -u "$$out" "$$result";                            \
				fail=1;                                                \
			fi;                                                            \
		done;                                                                  \
		echo "";                                                               \
		echo "=== Idempotency pass (out → out) ===";                           \
		count=0;                                                               \
		set --;                                                                \
		while IFS= read -r out; do                                             \
			set -- "$$@" "$$out";                                          \
			count=$$((count + 1));                                         \
		done < "$$passed";                                                     \
		if [ $$count -gt 0 ]; then                                             \
			idem_total=$$count;                                            \
			$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$@"; \
			for out in "$$@"; do                                           \
				base=$$(basename "$$out");                             \
				result="$(TEST_TMP)/$$base";                           \
				if diff -u "$$out" "$$result" > /dev/null 2>&1; then   \
					echo "PASS $$out";                             \
					idem_pass=$$((idem_pass + 1));                 \
				else                                                   \
					echo "FAIL $$out (not idempotent)";            \
					diff -u "$$out" "$$result";                    \
					fail=1;                                        \
				fi;                                                    \
			done;                                                          \
		fi;                                                                    \
		rm -f "$$passed";                                                      \
		echo "";                                                               \
		if [ $$fail -ne 0 ]; then exit 1; fi;                                  \
		printf "All tests passed:\n";                                          \
		printf "    %3d/%3d forward\n"     $$forward_pass $$forward_total;     \
		printf "    %3d/%3d idempotency\n" $$idem_pass $$idem_total;           \
		printf "\n";                                                           \
	)


# ── Bench target ─────────────────────────────────────────────────────────────
#
# Benchmarks total formatting time (excluding server start/stop) across the test files in four
# configurations:
#
#     1. `--standalone`, one file per invocation
#     2. `--standalone`, all files in one invocation
#     3. client-server , one file per invocation
#     4. client-server , all files in one invocation
#
# Override `JAVA` or `JAR_FILE` on the command line if needed, same as `make test`.
#
bench: $(JAR_FILE)
	@(                                                                                                                                              \
		mkdir -p $(TEST_TMP);                                                                                                                   \
		files="$(addprefix $(TEST_DIR)/,$(INP_FILES))";                                                                                         \
		set -- $$files; probe="$$1";                                                                                                            \
		clear;                                                                                                                                  \
		echo "";                                                                                                                                \
		echo "=== Bench: standalone, one-by-one ===";                                                                                           \
		start=$$(date +%s%N);                                                                                                                   \
		for f in $$files; do                                                                                                                    \
			$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$f";                                                                  \
		done;                                                                                                                                   \
		end=$$(date +%s%N);                                                                                                                     \
		base_ns=$$(( end - start ));                                                                                                            \
		printf "Total: %5dmS\n" $$(( base_ns / 1000000 ));                                                                                      \
		echo "";                                                                                                                                \
		echo "=== Bench: standalone, all-at-once ===";                                                                                          \
		start=$$(date +%s%N);                                                                                                                   \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) $$files;                                                                        \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		$(JAVA) -jar $(JAR_FILE) --server 2>&1 &                                                                                                \
		sleep 1;                                                                                                                                \
		echo "";                                                                                                                                \
		echo "=== Bench: client-server, one-by-one ===";                                                                                        \
		tries=0;                                                                                                                                \
		while [ $$tries -lt 50 ]; do                                                                                                            \
			if $(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) "$$probe" > /dev/null 2>&1; then break; fi;                                       \
			tries=$$((tries + 1));                                                                                                          \
			sleep 0.1;                                                                                                                      \
		done;                                                                                                                                   \
		start=$$(date +%s%N);                                                                                                                   \
		for f in $$files; do                                                                                                                    \
			$(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) "$$f";                                                                               \
		done;                                                                                                                                   \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		echo "=== Bench: client-server, all-at-once ===";                                                                                       \
		start=$$(date +%s%N);                                                                                                                   \
		$(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) $$files;                                                                                     \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %5dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		$(JAVA) -jar $(JAR_FILE) --stop;                                                                                                        \
		echo "";                                                                                                                                \
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
#     5. a full round-trip through a non-Java language (yaml), proving the
#        server actually reformats rather than echoing the body back verbatim
#     6. an unrecognized `lang` (e.g. `jsx` -- no longer even scaffold-only,
#        since `Lang.SCAFFOLD_ONLY_LANGUAGES` is now empty) -> HTTP 400
#
# Runs against a dedicated port so it doesn't collide with a server a developer already has running.
# Always stops its own server on exit, pass or fail.
#
TEST_SERVER_PORT := 17999
TEST_SERVER_TMP   = $(BUILD_DIR)/test-server-out

test-server: $(JAR_FILE)
	@(                                                                                                                        \
		mkdir -p $(TEST_SERVER_TMP);                                                                                      \
		fail=0;                                                                                                           \
		clear;                                                                                                            \
		echo "";                                                                                                          \
		printf 'class X {\n  void f() {\n    if (true) {\n      int y = 1;\n    }\n  }\n}\n'                              \
			> $(TEST_SERVER_TMP)/Sample.java;                                                                         \
		printf 'indent-size = 8\n' > $(TEST_SERVER_TMP)/.jxmake-code-formatter;                                           \
		printf 'foo:   bar   \nbaz:\n\n\n\n' > $(TEST_SERVER_TMP)/Sample.yaml;                                            \
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
