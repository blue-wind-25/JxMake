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

SOURCES       = $(shell find $(SRC_DIR) -name "*.java")

.PHONY: all build jar clean test bench test-server

all build jar: $(JAR_FILE)

$(CLASS_DIR):
	@mkdir -p $(CLASS_DIR)

$(JAR_FILE): $(SOURCES) | $(CLASS_DIR)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -d $(CLASS_DIR) $(SOURCES)
	@printf 'Main-Class: %s\n' '$(MAIN_CLASS)' > $(MANIFEST)
	@$(JAR) cfm $(JAR_FILE) $(MANIFEST) -C $(CLASS_DIR) .
#	@cp $(JAR_FILE) .

clean:
	@rm -rvf $(BUILD_DIR)


# ── Test target ───────────────────────────────────────────────────────────────
#
# Runs two passes over every file pair in $(TEST_DIR):
#
#     Forward pass:    format *_inp.<ext>  → diff result vs *_out.<ext>
#     Idempotency:     format *_out.<ext>  → diff result vs *_out.<ext>
#
# Uses --standalone to avoid depending on a running server instance.
# On any diff mismatch the loop prints the offending file and exits non-zero.
#
# Override JAVA or JAR_FILE on the command line if needed:
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

REF_FILES := $(subst _inp,_out,$(INP_FILES))

# svn add util/CodingStyle.md/formatter/test/real_code_regressions*
# git push && svn commit -m 'Follow GIT changes' && svn stat && git status

# make test
# make test 2>/dev/null
# make test 2>&1 | grep -E 'PASS|FAIL'
test: $(JAR_FILE)
	@(                                                                               \
		export JXMAKE_CODE_FORMATTER_FORMAT_MACROS=on;                           \
		mkdir -p $(TEST_TMP);                                                    \
		clear;                                                                   \
		echo "";                                                                 \
		echo "=== Forward pass (inp → out) ===";                                 \
		passed="$(TEST_TMP)/passed.list";                                        \
		rm -f "$$passed";                                                        \
		fail=0;                                                                  \
		forward_pass=0;                                                          \
		forward_total=0;                                                         \
		idem_pass=0;                                                             \
		idem_total=0;                                                            \
		for inp in $(addprefix $(TEST_DIR)/,$(INP_FILES)); do                    \
			base=$$(basename "$$inp");                                       \
			ext="$${base##*_inp.}";                                          \
			stem="$${base%_inp.*}";                                          \
			out="$(TEST_DIR)/$${stem}_out.$$ext";                            \
			if [ ! -f "$$out" ]; then                                        \
				echo "SKIP $$inp (no matching $$out)";                   \
				continue;                                                \
			fi;                                                              \
			forward_total=$$((forward_total + 1));                           \
			$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$inp"; \
			result="$(TEST_TMP)/$$base";                                     \
			if diff -u "$$out" "$$result" > /dev/null 2>&1; then             \
				echo "PASS $$inp";                                       \
				echo "$$out" >> "$$passed";                              \
				forward_pass=$$((forward_pass + 1));                     \
			else                                                             \
				echo "FAIL $$inp";                                       \
				diff -u "$$out" "$$result";                              \
				fail=1;                                                  \
			fi;                                                              \
		done;                                                                    \
		echo "";                                                                 \
		echo "=== Idempotency pass (out → out) ===";                             \
		while IFS= read -r out; do                                               \
			base=$$(basename "$$out");                                       \
			idem_total=$$((idem_total + 1));                                 \
			$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$out"; \
			result="$(TEST_TMP)/$$base";                                     \
			if diff -u "$$out" "$$result" > /dev/null 2>&1; then             \
				echo "PASS $$out";                                       \
				idem_pass=$$((idem_pass + 1));                           \
			else                                                             \
				echo "FAIL $$out (not idempotent)";                      \
				diff -u "$$out" "$$result";                              \
				fail=1;                                                  \
			fi;                                                              \
		done <  "$$passed";                                                      \
		rm -f "$$passed";                                                        \
		echo "";                                                                 \
		if [ $$fail -ne 0 ]; then exit 1; fi;                                    \
		printf "All tests passed:\n";                                            \
		printf "    %2d/%2d forward\n"     $$forward_pass $$forward_total;       \
		printf "    %2d/%2d idempotency\n" $$idem_pass $$idem_total;             \
		printf "\n";                                                             \
	)


# ── Bench target ─────────────────────────────────────────────────────────────
#
# Benchmarks total formatting time (excluding server start/stop) across the
# 15 *_inp.* test files in four configurations:
#
#     1. --standalone,  one file per invocation
#     2. --standalone,  all files in one invocation
#     3. client-server, one file per invocation
#     4. client-server, all files in one invocation
#
# Override JAVA or JAR_FILE on the command line if needed, same as `make test`.
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
		printf "Total: %4dmS\n" $$(( base_ns / 1000000 ));                                                                                      \
		echo "";                                                                                                                                \
		echo "=== Bench: standalone, all-at-once ===";                                                                                          \
		start=$$(date +%s%N);                                                                                                                   \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) $$files;                                                                        \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %4dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
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
		printf "Total: %4dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		echo "=== Bench: client-server, all-at-once ===";                                                                                       \
		start=$$(date +%s%N);                                                                                                                   \
		$(JAVA) -jar $(JAR_FILE) --out $(TEST_TMP) $$files;                                                                                     \
		end=$$(date +%s%N);                                                                                                                     \
		ns=$$(( end - start ));                                                                                                                 \
		printf "Total: %4dmS (%s)\n" $$(( ns / 1000000 )) "$$(awk -v base_ns="$$base_ns" -v ns="$$ns" 'BEGIN{printf "%5.2fx", base_ns / ns}')"; \
		echo "";                                                                                                                                \
		$(JAVA) -jar $(JAR_FILE) --stop;                                                                                                        \
		echo "";                                                                                                                                \
	)


# ── Server-mode test target ───────────────────────────────────────────────────
#
# Functional (not benchmark) coverage of the /format inline-config query
# parameters added for browser/no-real-file clients:
#
#     1. inline config, no `path` (path optional when lang + inline present)
#     2. inline config overriding a `.jxmake-code-formatter` file next to `path`
#     3. an unrecognized config-shaped query key -> HTTP 400
#
# Runs against a dedicated port so it doesn't collide with a server a
# developer already has running. Always stops its own server on exit, pass
# or fail.
#
TEST_SERVER_PORT := 17999
TEST_SERVER_TMP   = $(BUILD_DIR)/test-server-out

test-server: $(JAR_FILE)
	@(                                                                                                                        \
		mkdir -p $(TEST_SERVER_TMP);                                                                                      \
		fail=0;                                                                                                           \
		printf 'class X {\n  void f() {\n    if (true) {\n      int y = 1;\n    }\n  }\n}\n'                              \
			> $(TEST_SERVER_TMP)/Sample.java;                                                                         \
		printf 'indent-size = 8\n' > $(TEST_SERVER_TMP)/.jxmake-code-formatter;                                           \
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
		$(JAVA) -jar $(JAR_FILE) --port $(TEST_SERVER_PORT) --stop > /dev/null 2>&1 || true;                              \
		if [ $$fail -ne 0 ]; then exit 1; fi;                                                                             \
		echo "All server tests passed.";                                                                                  \
	)
