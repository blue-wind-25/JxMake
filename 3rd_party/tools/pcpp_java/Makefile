#
# Makefile for pcpp-java
#

JAVAC        ?= javac
JAR          ?= jar
JAVA_VERSION ?= 8

VERSION       = 1.30
ARTIFACT      = pcpp-java-$(VERSION)
MAIN_CLASS    = com.pcpp.pcmd.CmdPreprocessor

SRC_DIR       = src/main/java
BUILD_DIR     = target/classes
JAR_FILE      = target/$(ARTIFACT).jar
MANIFEST      = $(BUILD_DIR)/MANIFEST.MF

SOURCES       = $(shell find $(SRC_DIR) -name "*.java")

.PHONY: all clean

all: $(JAR_FILE)

$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

$(JAR_FILE): $(SOURCES) | $(BUILD_DIR)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -d $(BUILD_DIR) $(SOURCES)
	@printf 'Main-Class: %s\n' '$(MAIN_CLASS)' > $(MANIFEST)
	@$(JAR) cfm $(JAR_FILE) $(MANIFEST) -C $(BUILD_DIR) .
	@cp $(JAR_FILE) ..

clean:
	@rm -rf $(BUILD_DIR) $(JAR_FILE)
