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

.PHONY: all clean test

all: $(JAR_FILE)

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
#   Forward pass:    format *_inp.<ext>  → diff result vs *_out.<ext>
#   Idempotency:     format *_out.<ext>  → diff result vs *_out.<ext>
#
# Uses --standalone to avoid depending on a running server instance.
# On any diff mismatch the loop prints the offending file and exits non-zero.
#
# Override JAVA or JAR_FILE on the command line if needed:
#   make test JAVA=/path/to/java
#   make test JAR_FILE=/path/to/code-formatter-1.00.jar
#

INP_FILES :=
INP_FILES += h_core_inp.h
INP_FILES += c_core_inp.c

INP_FILES += hpp_core_inp.hpp
INP_FILES += cpp_core_inp.cpp
#INP_FILES += java_core_inp.java

#INP_FILES += cpp_modern_inp.cpp
#INP_FILES += java_modern_inp.java

#INP_FILES += combined_inp.h
#INP_FILES += combined_inp.c
#INP_FILES += combined_inp.hpp
#INP_FILES += combined_inp.cpp
#INP_FILES += combined_inp.java

#INP_FILES += c_comments_inp.c
#INP_FILES += cpp_comments_inp.cpp
#INP_FILES += java_comments_inp.java

REF_FILES := $(subst _inp,_out,$(INP_FILES))

# make && clear && rm -rvf target/test-out && make test | ../../../3rd_party/tools/colordiff/colordiff.pl | more
# git status | grep _inp

test: $(JAR_FILE)
	@mkdir -p $(TEST_TMP)
	@echo "=== Forward pass (inp → out) ==="
	@fail=0;                                                                 \
	for inp in $(addprefix $(TEST_DIR)/,$(INP_FILES)); do                    \
		base=$$(basename "$$inp");                                       \
		ext="$${base##*_inp.}";                                          \
		stem="$${base%_inp.*}";                                          \
		out="$(TEST_DIR)/$${stem}_out.$$ext";                            \
		if [ ! -f "$$out" ]; then                                        \
			echo "SKIP $$inp (no matching $$out)";                   \
			continue;                                                \
		fi;                                                              \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$inp"; \
		result="$(TEST_TMP)/$$base";                                     \
		if diff -u "$$out" "$$result" > /dev/null 2>&1; then             \
			echo "PASS $$inp";                                       \
		else                                                             \
			echo "FAIL $$inp";                                       \
			diff -u "$$out" "$$result";                              \
			fail=1;                                                  \
		fi;                                                              \
	done;                                                                    \
	if [ $$fail -ne 0 ]; then exit 1; fi
	@echo ""
	@echo "=== Idempotency pass (out → out) ==="
	@fail=0;                                                                 \
	for out in $(addprefix $(TEST_DIR)/,$(REF_FILES)); do                    \
		base=$$(basename "$$out");                                       \
		$(JAVA) -jar $(JAR_FILE) --standalone --out $(TEST_TMP) "$$out"; \
		result="$(TEST_TMP)/$$base";                                     \
		if diff -u "$$out" "$$result" > /dev/null 2>&1; then             \
			echo "PASS $$out";                                       \
		else                                                             \
			echo "FAIL $$out (not idempotent)";                      \
			diff -u "$$out" "$$result";                              \
			fail=1;                                                  \
		fi;                                                              \
	done;                                                                    \
	if [ $$fail -ne 0 ]; then exit 1; fi
	@echo ""
	@echo "All tests passed."
