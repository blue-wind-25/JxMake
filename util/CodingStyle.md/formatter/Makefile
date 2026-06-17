#
# Makefile for formatter
#

JAVAC        ?= javac
JAR          ?= jar
JAVA_VERSION ?= 8

VERSION       = 1.00
ARTIFACT      = formatter-$(VERSION)
MAIN_CLASS    = com.jxmake.formatter

SRC_DIR       = src

BUILD_DIR     = target
CLASS_DIR     = $(BUILD_DIR)/classes
JAR_FILE      = $(BUILD_DIR)/$(ARTIFACT).jar
MANIFEST      = $(CLASS_DIR)/MANIFEST.MF

SOURCES       = $(shell find $(SRC_DIR) -name "*.java")

.PHONY: all clean

all: $(JAR_FILE)

$(CLASS_DIR):
	@mkdir -p $(CLASS_DIR)

$(JAR_FILE): $(SOURCES) | $(CLASS_DIR)
	@$(JAVAC) -encoding UTF-8 -source $(JAVA_VERSION) -target $(JAVA_VERSION) -d $(CLASS_DIR) $(SOURCES)
	@printf 'Main-Class: %s\n' '$(MAIN_CLASS)' > $(MANIFEST)
	@$(JAR) cfm $(JAR_FILE) $(MANIFEST) -C $(CLASS_DIR) .
	@cp $(JAR_FILE) .

clean:
	@rm -rvf $(BUILD_DIR)
