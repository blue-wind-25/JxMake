#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


##### ANSI escape codes for colors
C_GRAY    = "\\033[90m"
C_RED     = "\\033[91m"
C_GREEN   = "\\033[92m"
C_YELLOW  = "\\033[93m"
C_BLUE    = "\\033[94m"
C_MAGENTA = "\\033[95m"
C_CYAN    = "\\033[96m"
C_WHITE   = "\\033[97m"
C_RESET   = "\\033[0m"


##### A helper function to perform recursive wildcard
##### Based on:
#####     https://stackoverflow.com/a/6145814
#####     https://stackoverflow.com/a/12959764
filter_out       = $(foreach v,$(2),$(if $(findstring $(1),$(v)),,$(v)))
filter_out_multi = $(foreach v,$(2),$(if $(filter $(v),$(foreach p,$(1),$(if $(findstring $(p),$(v)),$(v)))),,$(v)))
rwildcard        = $(wildcard $(strip $(1))$(strip $(2))) $(foreach d,$(wildcard $(strip $(1))*),$(call rwildcard,$d/,$(strip $(2))))
java_classes_j   = $(patsubst %.java,%.class,$(call rwildcard,$(strip $(1))/,*.java))
java_classes_i   = $(patsubst %.java.in,%.class,$(call rwildcard,$(strip $(1))/,*.java.in))
java_sources_i   = $(patsubst %.java.in,%.java,$(call rwildcard,$(strip $(1))/,*.java.in))
java_classes     = $(call filter_out,0_excluded,$(java_classes_j) $(java_classes_i))


##### Formatting aid
PLEN_1 := 13
PLEN_2 := 14
H_LINE := "$(C_CYAN)-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------$(C_RESET)\n\n"


##### The default target
GP_FMT := "␣%$(PLEN_1)s␣%$(PLEN_2)s"
FORMAT := "$(GP_FMT)$(GP_FMT)$(GP_FMT)"
default:
	@echo -e "$(C_WHITE)Available Targets$(C_RESET)"
	@echo
	@printf "$(C_GREEN)$(subst ␣, ,$(FORMAT))$(C_RESET)\n" run rjar lib clib xlib
	@echo
	@echo -e $(TEST_LIST)


##### Configuration
JFLAGS	:= -g -Xmaxerrs 5 -Xmaxwarns 5 -Werror -Xlint:all

##### !!! TODO : Update JGit and SLF4J later !!! #####
CF_PDIR	:= $(HOME)/0-JxMake/manual
CF_PTST	:= $(shell test -d "$(CF_PDIR)" && echo true)
CF_JGIT	:= org.eclipse.jgit-5.13.0.202109080827-r.jar slf4j-simple-2.0.13.jar

CF_LDIR	:= $(HOME)/Projects/JxMake/3rd_party/jars
CF_LTST	:= $(shell test -d "$(CF_LDIR)" && echo true)
CF_ANSI	:= jansi.jar
CF_JSCM	:= jSerialComm.jar
CF_ZSTD	:= zstd-jni.jar
CF_RSTA	:= spellchecker.jar rstaui.jar rsyntaxtextarea.jar autocomplete.jar
CF_JUSB	:= java-does-usb.jar
CF_PTYJ	:= pty4j.jar annotations.jar kotlin-stdlib.jar slf4j-api.jar jna.jar jna-platform.jar
CF_ALLS	:= $(CF_ANSI) $(CF_JSCM) $(CF_ZSTD) $(CF_RSTA) $(CF_JUSB) $(CF_PTYJ)

CP_MANL	:= $(if $(CF_PTST),:$(subst $(__EMPTY__) $(__EMPTY__),:,$(addprefix $(CF_PDIR)/,$(CF_JGIT))),)
CP_LOCL	:= $(if $(CF_LTST),:$(subst $(__EMPTY__) $(__EMPTY__),:,$(addprefix $(CF_LDIR)/,$(CF_ALLS))),)
CP_EXTR	:= $(CP_MANL)$(CP_LOCL)

CP_CPL	:= -cp .
CP_CPL	:= -cp .:$(CP_EXTR)

CP_RUN	:= -cp .
CP_RUN	:= -cp .$(CP_EXTR)

CP_JDS	:= --class-path .
CP_JDS	:= --class-path .:$(CP_EXTR)

JPATH	:=
#JPATH	:= /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64
#JPATH	:= /usr/lib/jvm/java-11-openjdk-11.0.17.0.8-2.el7_9.x86_64
#JPATH	:= /usr/java/jdk-11.0.16.1
#JPATH	:= /usr/java/jdk-17.0.5
#JPATH	:= /opt/openjdk-21_linux-x64_bin/jdk-21
#JPATH	:= /opt/openjdk-25_linux-x64_bin/jdk-25

JDS	:= /usr/lib/jvm/java-11-openjdk-11.0.17.0.8-2.el7_9.x86_64/bin/jdeprscan --release 11 $(CP_JDS)
JDS	:= /usr/java/jdk-11.0.16.1/bin/jdeprscan                                 --release 11 $(CP_JDS)
JDS	:= /usr/java/jdk-17.0.5/bin/jdeprscan                                    --release 17 $(CP_JDS)
JDS	:= /opt/openjdk-21_linux-x64_bin/jdk-21/bin/jdeprscan                    --release 21 $(CP_JDS)
JDS	:= /opt/openjdk-25_linux-x64_bin/jdk-25/bin/jdeprscan                    --release 25 $(CP_JDS)

JC	 = $(if $(strip $(JPATH)),$(JPATH)/bin/javac,javac)                                                         $(CP_CPL)
JVM	 = $(if $(strip $(JPATH)),$(if $(wildcard $(JPATH)/bin/java),$(JPATH)/bin/java,$(JPATH)/jre/bin/java),java) $(CP_RUN) $(EN_NAA_FLAG)

# Only for Java 17 and later
xlint:
	@$(JC) -Xlint --help-lint


##### Subdirectory and class file list
# *.java;*.java.in;*.java.inc;JxMakeFile;*.jxm;*.txt
JAR_RES_RDIR    := res
JAR_RES_DIRS    := 0-JxMake ../docs
JAR_SUB_DIRS    := jxm org com $(JAR_RES_RDIR)
JAVA_CLASSES    := $(foreach subdir, $(JAR_SUB_DIRS), $(call java_classes, $(subdir)))
JAVA_SOURCES_PP := $(foreach subdir, $(JAR_SUB_DIRS), $(call java_sources_i, $(subdir)))
JAVA_SOURCES_RM := $(JAVA_SOURCES_PP)


##### Get the Java version
JAVA_VERSION := $(shell $(JVM) -XshowSettings:properties -version 2>&1 | awk -F '=' '/java.specification.version/ { print $$2 }' | xargs)
JAVA_MAJOR   := $(shell echo "$(JAVA_VERSION)" | awk -F. '{ print $$1 }')

##### Check if "--enable-native-access=ALL-UNNAMED" is required (for Java versions 22 and above)
ifeq ($(shell test "$(JAVA_MAJOR)" -ge 22 && echo true), true)
    EN_NAA_FLAG := --enable-native-access=ALL-UNNAMED
endif

##### Check if "-Xlint:-dangling-doc-comments" is required (for Java versions 25 and above)
ifeq ($(shell test "$(JAVA_MAJOR)" -ge 25 && echo true), true)
    JFLAGS += -Xlint:-dangling-doc-comments
endif

##### Exclude source code files that rely on the Foreign Function and Memory (FFM) API when
##### building with Java versions earlier than 23 (required by JavaDoesUSB) or if the required
##### dependency JAR files are missing
ifeq ($(shell ( test "$(JAVA_MAJOR)" -lt 23 ) && echo true), true)
    ExcludeUSB      := ProgBootUSB ProgBootLUFAHID ProgBootAVRDFU ProgBootSTM32DFU
    JAVA_CLASSES    := $(call filter_out_multi,$(ExcludeUSB),$(JAVA_CLASSES))
    JAVA_SOURCES_PP := $(call filter_out_multi,$(ExcludeUSB),$(JAVA_SOURCES_PP))
endif


##### Helper functions to generate rules
TEST_LIST := ''

define GEN_SINGLE_UTIL
TEST_LIST += "$(C_GREEN)`printf '%$(PLEN_1)s' '$(strip $(1))'` `printf '%$(PLEN_2)s' 'j$(strip $(1))'`$(C_RESET)$(strip $(4))"
$(strip $(1))  j$(strip $(1)) : OPT_ARG=$(strip $(3))
$(strip $(1))  j$(strip $(1)) : JXM_SFILE=$(strip $(2))
$(strip $(1))                 : build_exec_jxm_$(strip $(5))
j$(strip $(1))                : build_exec_jxm_j$(strip $(5))
endef

define GEN_SINGLE_TEST
TEST_LIST += "$(C_GREEN)`printf '%$(PLEN_1)s' '$(strip $(1))test'` `printf '%$(PLEN_2)s' 'j$(strip $(1))test'`$(C_RESET)$(strip $(4))"
$(strip $(1))test  j$(strip $(1))test : OPT_ARG=$(strip $(3))
$(strip $(1))test  j$(strip $(1))test : JXM_SFILE=$(strip $(2))
$(strip $(1))test                     : build_exec_jxm_$(strip $(5))test
j$(strip $(1))test                    : build_exec_jxm_j$(strip $(5))test
endef

define GEN_XMULTI_TEST
TEST_LIST += "$(C_GREEN)`printf '%$(PLEN_1)s' 'x$(strip $(1))test'` `printf '%$(PLEN_2)s' 'jx$(strip $(1))test'`$(C_RESET)$(strip $(3))"
x$(strip $(1))test:
	@echo
	@$(MAKE) -s $(strip $(2))test
	@echo
	@$(MAKE) -s $(strip $(1))test
	@echo
jx$(strip $(1))test:
	@echo
	@$(MAKE) -s $(strip $(2))test
	@echo
	@$(MAKE) -s j$(strip $(1))test
	@echo
endef

define GEN_FBUILD_RULE_IMPL
$(strip $(1)):
	@echo
	@$(MAKE) -s MAIN_CLASS=$($(strip $(3))) build_classes
	@$(JVM) -ea $($(strip $(3)):.class=) $(if $(strip $(5)), $$(OPT_ARG) -C $(strip $(5)) -f $(strip $$(JXM_SFILE)).jxm, )
	@echo

$(strip $(2)):
	@echo
	@$(MAKE) -s MAIN_CLASS=$($(strip $(3))) JAR_FILE=$($(strip $(4))) build_jar
	@$(JVM) -ea -jar $($(strip $(4))) $(if $(strip $(5)), $$(OPT_ARG) -f $(strip $(5))/$(strip $$(JXM_SFILE)).jxm, )
	@echo
endef
GEN_FBUILD_RULE = $(eval $(call GEN_FBUILD_RULE_IMPL, build_exec_jxm_$(strip $(1))test, build_exec_jxm_j$(strip $(1))test, $(strip $(2))_CLASS , $(strip $(2))_JAR , $(strip $(3))))
GEN_UBUILD_RULE = $(eval $(call GEN_FBUILD_RULE_IMPL, build_exec_jxm_$(strip $(1)), build_exec_jxm_j$(strip $(1)), $(strip $(2))_CLASS , $(strip $(2))_JAR , $(strip $(3))))

define GEN_DBUILD_RULE_IMPL
$(strip $(1)):
	@echo
	@$(MAKE) -s MAIN_CLASS=$($(strip $(3))) build_classes
	@$(JVM) -ea $($(strip $(3)):.class=) $(if $(strip $(5)), $$(OPT_ARG) -C $(strip $(5)), )
	@echo

$(strip $(2)):
	@echo
	@$(MAKE) -s MAIN_CLASS=$($(strip $(3))) JAR_FILE=$($(strip $(4))) build_jar
	@$(JVM) -ea -jar $($(strip $(4))) $(if $(strip $(5)), $$(OPT_ARG) -C $(strip $(5)), )
	@echo
endef
GEN_DBUILD_RULE = $(call GEN_DBUILD_RULE_IMPL, build_exec_jxm_$(strip $(1))test, build_exec_jxm_j$(strip $(1))test, $(strip $(2))_CLASS , $(strip $(2))_JAR , $(strip $(3)))


##### Build and execute the primary/test application
APP_CLASS = JXM_Main.class
APP_JAR   = jxmake.jar

$(eval $(call GEN_FBUILD_RULE_IMPL, run, rjar, APP_CLASS, APP_JAR))

TEST_LIST += $(H_LINE)

jds: jar
	@$(JDS) $(APP_JAR)

jar:
	@$(MAKE) -s MAIN_CLASS=$(APP_CLASS) JAR_FILE=$(APP_JAR) build_jar

xjar: jar
	@$(subst .:,$(APP_JAR):,$(JVM)) $(basename $(APP_CLASS)) --en-headless         -h

jbd: jar
	@$(subst .:,$(APP_JAR):,$(JVM)) $(basename $(APP_CLASS)) --en-all-exception-st --en-opengl --lct --browse-docs

jse0: jar
	@$(subst .:,$(APP_JAR):,$(JVM)) $(basename $(APP_CLASS)) --en-all-exception-st --en-opengl --lct --script-editor

jse1: jar
	@$(subst .:,$(APP_JAR):,$(JVM)) $(basename $(APP_CLASS)) --en-all-exception-st --en-opengl --lct --script-editor -f ../test/JxMakeTokenMaker_Test.jxm

jse2: jar
	@$(subst .:,$(APP_JAR):,$(JVM)) $(basename $(APP_CLASS)) --en-all-exception-st --en-opengl --lct --script-editor -f ../test/JxMakeScriptEditor_Test.jxm

jsef: jar
	@$(subst .:,$(APP_JAR):,$(JVM)) $(basename $(APP_CLASS)) --en-all-exception-st --en-opengl --lct --script-editor -f ../test/FunSupTest.jxm


##### Build distribution
##### !!! NOTE : Synchronize with 'jxm/SysUtil.java' !!!
DIST_VER = 0.9.9-tp2
DIST_DIR = ../dist_build

dist: jar
	@rm -rvf $(DIST_DIR)
	@mkdir -p $(DIST_DIR)/jxmake_dist/libs
	@mkdir -p $(DIST_DIR)/jxmake_dist/dics
	@mkdir -p $(DIST_DIR)/jxmake_dist/unicode
	@cp -v ../3rd_party/jars/*    $(DIST_DIR)/jxmake_dist/libs
	@cp -v ../3rd_party/dics/*    $(DIST_DIR)/jxmake_dist/dics
	@cp -v ../3rd_party/unicode/* $(DIST_DIR)/jxmake_dist/unicode
	@cp -v $(APP_JAR)             $(DIST_DIR)/jxmake_dist
	@cp -v jxmake.sh              $(DIST_DIR)/jxmake
	@cp -v jxmake.bat             $(DIST_DIR)/jxmake.bat
	@(                                                                   \
		cd $(DIST_DIR)  ;                                            \
		zip -r jxmake-$(DIST_VER).zip jxmake_dist jxmake jxmake.bat; \
	)

dist_clean:
	@rm -rvf $(DIST_DIR)


##### Build the libraries
LIB_DIR   = 0-JxMake/lib
LIB_FILES = Null                          \
            JavaBuildTool                 \
            C++BuildTool                  \
            ArduinoXBuildTool             \
            C++XBuildTool_BM_ESP8266      \
            C++XBuildTool_BM_ESP32        \
            C++XBuildTool_BM_ESP32C3      \
            C++XBuildTool_BM_ATSAMD21     \
            C++XBuildTool_BM_ATSAM3X      \
            C++XBuildTool_BM_NRF51        \
            C++XBuildTool_BM_NRF52        \
            C++XBuildTool_AVR_Native      \
            C++XBuildTool_ESP8266_Arduino \
            C++XBuildTool_RP2040_Pico     \
            C++XBuildTool_STM32_Cube      \
            C++XBuildTool_STM8_SPL        \
            C++XBuildTool_MCS51_Native    \
            XMLFrame

lib: jxmlib
	@echo > /dev/null

clib: jxmlib_clean
	@echo > /dev/null

xlib:
	@$(MAKE) -s clib
	@$(MAKE) -s lib


jxmlib:
	@$(MAKE) -s MAIN_CLASS=$(APP_CLASS) JAR_FILE=$(APP_JAR) build_jar
	@$(MAKE) -s jxmlib_clean
	@(                                                           \
		for I in $(LIB_FILES); do                            \
			FILE=$(LIB_DIR)/$$I.jxm;                     \
			$(JVM) -jar jxm.jar --__compile__ -f $$FILE; \
			echo "compiled ‘$$FILE.bin’";                \
		done                                                 \
	)

jxmlib_clean:
	@rm  -vf $(LIB_DIR)/*.bin


##### Build and execute the low-level test applications
GTEST1_CLASS  = GTest1.class
GTEST1_JAR    = gtest1.jar

GTEST2_CLASS  = GTest2.class
GTEST2_JAR    = gtest2.jar

ATEST1_CLASS  = ATest1.class
ATEST1_JAR    = atest1.jar

PTEST1X_CLASS = PTest1X.class
PTEST1X_JAR   = ptest1x.jar

PTEST2X_CLASS = PTest2X.class
PTEST2X_JAR   = ptest2x.jar

PTEST3X_CLASS = PTest3X.class
PTEST3X_JAR   = ptest3x.jar

PTEST1B_CLASS = PTest1B.class
PTEST1B_JAR   = ptest1b.jar

PTEST2B_CLASS = PTest2B.class
PTEST2B_JAR   = ptest2b.jar

PTESTF_CLASS  = PTestF.class
PTESTF_JAR    = ptestf.jar

TEST_JARS     = $(GTEST1_JAR) $(GTEST2_JAR) $(ATEST1_JAR) $(PTEST1X_JAR) $(PTEST2X_JAR) $(PTEST3X_JAR) $(PTEST1B_JAR) $(PTEST2B_JAR) $(PTESTF_JAR)

$(eval $(call GEN_SINGLE_TEST, g1 , EmptyTest,                      , \\n   , gtest1 ))
$(eval $(call GEN_SINGLE_TEST, g2 , EmptyTest,                      , \\n\\n, gtest2 ))

$(eval $(call GEN_SINGLE_TEST, a1 , EmptyTest, --en-all-exception-st, \\n\\n, atest1 ))

$(eval $(call GEN_SINGLE_TEST, p1x, EmptyTest,                      , \\n   , ptest1x))
$(eval $(call GEN_SINGLE_TEST, p2x, EmptyTest, --en-all-exception-st, \\n   , ptest2x))
$(eval $(call GEN_SINGLE_TEST, p3x, EmptyTest, --en-all-exception-st, \\n   , ptest3x))
$(eval $(call GEN_SINGLE_TEST, p1b, EmptyTest, --en-all-exception-st, \\n   , ptest1b))
$(eval $(call GEN_SINGLE_TEST, p2b, EmptyTest, --en-all-exception-st, \\n   , ptest2b))
$(eval $(call GEN_SINGLE_TEST, pf , EmptyTest,                      , \\n\\n, ptestf ))


##### Test the documentation browser
$(eval $(call GEN_SINGLE_TEST, doc, , --browse-docs, \\n\\n, htest))


##### Build and execute the utility applications
$(eval $(call GEN_SINGLE_UTIL, gstm32csl, STM32ChipSpec    , , \\n   , util_STM32Spec))
$(eval $(call GEN_SINGLE_UTIL, gstm32lds, STM32LinkerScript, , \\n\\n, util_STM32Spec))

TEST_LIST += $(H_LINE)


##### Build and execute the JxMake test applications
ET_OPTS := --en-jxm-lload-verbose --en-all-exception-st --en-warn-all

$(eval $(call GEN_SINGLE_TEST,    empty , EmptyTest    , $(ET_OPTS)                                              , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    macro , MacroTest    ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    inc   , IncTest_Main ,                                                         , \\n   , htest    ))

VAR_ARGS = ArgVar1='Value1' ArgVar3='Value3'
$(eval $(call GEN_SINGLE_TEST,    var   , VarTest      , $(VAR_ARGS) --__en_dtp__                                ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   svar   , VarTest      , $(VAR_ARGS) --__en_dtp__ -s                             , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    fun   , FunTest      , --en-all-exception-st                                   , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    funsup, FunSupTest   ,                                                         , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    local , LocalTest    , $(VAR_ARGS) --__en_dtp__                                , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    varinh, VarInhTest   , $(VAR_ARGS) --__en_dtp__                                , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    varref, VarRefTest   , $(VAR_ARGS) --__en_dtp__ --en-jxm-cache-verbose         , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    depr  , DeprTest     , $(VAR_ARGS) --__en_dtp__ --en-jxm-cache-verbose         , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    cache , CacheTest    , --en-jxm-cache --en-jxm-cache-verbose --__en_dtp__ test ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   dcache , CacheTest    , --rm-prj-cache                                          ,       , htest    ))
$(eval $(call GEN_XMULTI_TEST,    cache , dcache                                                                 , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    regex , RegExpTest   ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    map   , MapTest      ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    stack , StackTest    , --en-all-exception-st                                   , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    bse   , BSETest      ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    fwc   , FWCTest      , $(ET_OPTS)                                              , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    tar   , TargetTest   , test 123.o src/cpp/main.o dummy1.out dummy2.out         ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   ctar   , TargetTest   , clean                                                   ,       , htest    ))
$(eval $(call GEN_XMULTI_TEST,    tar   , ctar                                                                   ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   mtar   , TargetTest   , 1.abc 123.abc 123.def.abc 7.abc 789.abc 789.def.abc     ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   ntar   , TargetTest   , clone                                                   , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    alo   , ALOTest      , --__en_dtp__                                            , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    lgoto , LabelGotoTest, --en-jxm-cache-verbose trg_goto                         ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   dlgoto , LabelGotoTest, --rm-prj-cache                                          ,       , htest    ))
$(eval $(call GEN_XMULTI_TEST,    lgoto , dlgoto                                                                 , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    if    , IfTest       ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    for   , ForTest      ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    loop  , LoopTest     ,                                                         , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    str   , StrTest      , --en-jxm-cache --en-jxm-cache-verbose                   ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   dstr   , StrTest      , --rm-prj-cache                                          ,       , htest    ))
$(eval $(call GEN_XMULTI_TEST,    str   , dstr                                                                   , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    cmd   , CmdTest      ,                                                         , \\n   , htest    ))
$(eval $(call GEN_SINGLE_TEST,    parse , ParseTest    ,                                                         , \\n   , htest    ))

$(eval $(call GEN_SINGLE_TEST,    xmf   , XMLFrameTest , --en-jxm-cache-verbose --en-jxm-lload-verbose run       ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   cxmf   , XMLFrameTest , --rm-prj-cache                                          ,       , htest    ))
$(eval $(call GEN_XMULTI_TEST,    xmf   , cxmf                                                                   ,       , htest    ))
$(eval $(call GEN_SINGLE_TEST,   hxmf   , XMLFrameTest , --en-jxm-cache-verbose --en-jxm-lload-verbose xflib_help, \\n\\n, htest    ))

MCUPrEx := 'MCUProgExec-ATmega328P'
$(eval $(call GEN_SINGLE_TEST,  px_m328 , $(MCUPrEx)   , --en-all-exception-st                                   , \\n   , htest    ))

MCUPrEx := 'MCUProgExec-LGT8F328P'
$(eval $(call GEN_SINGLE_TEST,  px_lgt8 , $(MCUPrEx)   , --en-all-exception-st                                   , \\n   , htest    ))

MCUPrEx := 'MCUProgExec-STM8S103'
$(eval $(call GEN_SINGLE_TEST , px_stm8 , $(MCUPrEx)   , --en-all-exception-st                                   , \\n   , htest    ))

MCUPrEx := 'MCUProgExec-PIC18F4520'
$(eval $(call GEN_SINGLE_TEST , px_pic18, $(MCUPrEx)   , --en-all-exception-st                                   , \\n   , htest    ))

MCUPrEx := 'MCUProgExec-PIC24FJ64GB002'
$(eval $(call GEN_SINGLE_TEST , px_pic24, $(MCUPrEx)   , --en-all-exception-st                                   , \\n   , htest    ))

MCUPrEx := 'MCUProgExec-ATmega32A_JTAG'
$(eval $(call GEN_SINGLE_TEST,  px_j32a , $(MCUPrEx)   , --en-all-exception-st                                   , \\n\\n, htest    ))

MCUPrEx := 'MCUProgExec-BootOpenBLT'
$(eval $(call GEN_SINGLE_TEST,  px_oblt , $(MCUPrEx)   , --en-all-exception-st                                   , \\n\\n, htest    ))

MCUPrEx := 'MCUProgExec-BootLUFAPrinter'
$(eval $(call GEN_SINGLE_TEST,  px_lprn , $(MCUPrEx)   , --en-all-exception-st                                   , \\n\\n, htest    ))

TEST_LIST += $(H_LINE)

$(eval $(call GEN_SINGLE_TEST,    java  ,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , java     ))
$(eval $(call GEN_SINGLE_TEST,   cjava  ,              ,                        clean   --en-jxm-lload-verbose   ,       , java     ))
$(eval $(call GEN_XMULTI_TEST,    java  , cjava                                                                  ,       , java     ))
$(eval $(call GEN_SINGLE_TEST,   hjava  ,              ,                        btlib_help                       , \\n\\n, java     ))

$(eval $(call GEN_SINGLE_TEST,    cpp   ,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , cpp      ))
$(eval $(call GEN_SINGLE_TEST,   ccpp   ,              ,                        clean   --en-jxm-lload-verbose   ,       , cpp      ))
$(eval $(call GEN_XMULTI_TEST,    cpp   , ccpp                                                                   ,       , cpp      ))
$(eval $(call GEN_SINGLE_TEST,   hcpp   ,              ,                        btlib_help                       , \\n   , cpp      ))

$(eval $(call GEN_SINGLE_TEST,    cpp20m,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , cpp20m   ))
$(eval $(call GEN_SINGLE_TEST,   ccpp20m,              ,                        clean   --en-jxm-lload-verbose   ,       , cpp20m   ))
$(eval $(call GEN_XMULTI_TEST,    cpp20m, ccpp20m                                                                ,       , cpp20m   ))
$(eval $(call GEN_SINGLE_TEST,   hcpp20m,              ,                        btlib_help                       , \\n\\n, cpp20m   ))

TEST_LIST += $(H_LINE)

$(eval $(call GEN_SINGLE_TEST,      ardu,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , ardu     ))
$(eval $(call GEN_SINGLE_TEST,     cardu,              ,                        clean   --en-jxm-lload-verbose   ,       , ardu     ))
$(eval $(call GEN_XMULTI_TEST,      ardu, cardu                                                                  ,       , ardu     ))
$(eval $(call GEN_SINGLE_TEST,     hardu,              ,                        btlib_help                       , \\n   , ardu     ))
$(eval $(call GEN_SINGLE_TEST,     uardu,              ,                        upload                           ,       , ardu     ))
$(eval $(call GEN_SINGLE_TEST,     pardu,              ,                        upload_prg                       ,       , ardu     ))
$(eval $(call GEN_SINGLE_TEST,     nardu,              ,                        upload_net                       , \\n\\n, ardu     ))

$(eval $(call GEN_SINGLE_TEST,    8266bm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bm8266   ))
$(eval $(call GEN_SINGLE_TEST,   c8266bm,              ,                        clean   --en-jxm-lload-verbose   ,       , bm8266   ))
$(eval $(call GEN_XMULTI_TEST,    8266bm, c8266bm                                                                ,       , bm8266   ))
$(eval $(call GEN_SINGLE_TEST,   h8266bm,              ,                        btlib_help                       ,       , bm8266   ))
$(eval $(call GEN_SINGLE_TEST,   s8266bm,              ,                        sercon                           ,       , bm8266   ))
$(eval $(call GEN_SINGLE_TEST,  us8266bm,              ,                        userial                          , \\n\\n, bm8266   ))

$(eval $(call GEN_SINGLE_TEST,      32bm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bm32     ))
$(eval $(call GEN_SINGLE_TEST,     c32bm,              ,                        clean   --en-jxm-lload-verbose   ,       , bm32     ))
$(eval $(call GEN_XMULTI_TEST,      32bm, c32bm                                                                  ,       , bm32     ))
$(eval $(call GEN_SINGLE_TEST,     h32bm,              ,                        btlib_help                       ,       , bm32     ))
$(eval $(call GEN_SINGLE_TEST,     s32bm,              ,                        sercon                           ,       , bm32     ))
$(eval $(call GEN_SINGLE_TEST,    us32bm,              ,                        userial                          , \\n\\n, bm32     ))

$(eval $(call GEN_SINGLE_TEST,      c3bm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bm32c3   ))
$(eval $(call GEN_SINGLE_TEST,     cc3bm,              ,                        clean   --en-jxm-lload-verbose   ,       , bm32c3   ))
$(eval $(call GEN_XMULTI_TEST,      c3bm, cc3bm                                                                  ,       , bm32c3   ))
$(eval $(call GEN_SINGLE_TEST,     hc3bm,              ,                        btlib_help                       ,       , bm32c3   ))
$(eval $(call GEN_SINGLE_TEST,     sc3bm,              ,                        sercon                           ,       , bm32c3   ))
$(eval $(call GEN_SINGLE_TEST,    usc3bm,              ,                        userial                          , \\n\\n, bm32c3   ))

$(eval $(call GEN_SINGLE_TEST,     d21bm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,    cd21bm,              ,                        clean   --en-jxm-lload-verbose   ,       , bmsd21   ))
$(eval $(call GEN_XMULTI_TEST,     d21bm, cd21bm                                                                 ,       , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,    hd21bm,              ,                        btlib_help                       , \\n   , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,   ubmsd21,              ,                        usamba                           ,       , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,   uwd21bm,              ,                        uswd                             ,       , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,   dwd21bm,              ,                        dswd                             ,       , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,   dgd21bm,              ,                        dgdb                             ,       , bmsd21   ))
$(eval $(call GEN_SINGLE_TEST,   rwd21bm,              ,                        rswd                             , \\n\\n, bmsd21   ))

$(eval $(call GEN_SINGLE_TEST,     s3xbm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,    cs3xbm,              ,                        clean   --en-jxm-lload-verbose   ,       , bms3x8   ))
$(eval $(call GEN_XMULTI_TEST,     s3xbm, cs3xbm                                                                 ,       , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,    hs3xbm,              ,                        btlib_help                       , \\n   , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,   uss3xbm,              ,                        usamba                           ,       , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,   uws3xbm,              ,                        uswd                             ,       , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,   dws3xbm,              ,                        dswd                             ,       , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,   dgs3xbm,              ,                        dgdb                             ,       , bms3x8   ))
$(eval $(call GEN_SINGLE_TEST,   rws3xbm,              ,                        rswd                             , \\n\\n, bms3x8   ))

$(eval $(call GEN_SINGLE_TEST,     n51bm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bmnr51   ))
$(eval $(call GEN_SINGLE_TEST,    cn51bm,              ,                        clean   --en-jxm-lload-verbose   ,       , bmnr51   ))
$(eval $(call GEN_XMULTI_TEST,     n51bm, cn51bm                                                                 ,       , bmnr51   ))
$(eval $(call GEN_SINGLE_TEST,    hn51bm,              ,                        btlib_help                       , \\n   , bmnr51   ))
$(eval $(call GEN_SINGLE_TEST,   uwn51bm,              ,                        uswd                             ,       , bmnr51   ))
$(eval $(call GEN_SINGLE_TEST,   dwn51bm,              ,                        dswd                             ,       , bmnr51   ))
$(eval $(call GEN_SINGLE_TEST,   dgn51bm,              ,                        dgdb                             ,       , bmnr51   ))
$(eval $(call GEN_SINGLE_TEST,   rwn51bm,              ,                        rswd                             , \\n\\n, bmnr51   ))

$(eval $(call GEN_SINGLE_TEST,     n52bm,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , bmnr52   ))
$(eval $(call GEN_SINGLE_TEST,    cn52bm,              ,                        clean   --en-jxm-lload-verbose   ,       , bmnr52   ))
$(eval $(call GEN_XMULTI_TEST,     n52bm, cn52bm                                                                 ,       , bmnr52   ))
$(eval $(call GEN_SINGLE_TEST,    hn52bm,              ,                        btlib_help                       , \\n   , bmnr52   ))
$(eval $(call GEN_SINGLE_TEST,   usn52bm,              ,                        unordic                          ,       , bmnr52   ))
$(eval $(call GEN_SINGLE_TEST,   uwn52bm,              ,                        uswd                             ,       , bmnr52   ))
$(eval $(call GEN_SINGLE_TEST,   dwn52bm,              ,                        dswd                             ,       , bmnr52   ))
$(eval $(call GEN_SINGLE_TEST,   dgn52bm,              ,                        dgdb                             ,       , bmnr52  ))
$(eval $(call GEN_SINGLE_TEST,   rwn52bm,              ,                        rswd                             , \\n\\n, bmnr52   ))

$(eval $(call GEN_SINGLE_TEST,      tavr,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST,     ctavr,              ,                        clean   --en-jxm-lload-verbose   ,       , tavr     ))
$(eval $(call GEN_XMULTI_TEST,      tavr, ctavr                                                                  ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST,     htavr,              ,                        btlib_help                       , \\n   , tavr     ))
$(eval $(call GEN_SINGLE_TEST, uft2rtavr,              ,                        uft232r                          ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST, utinytavr,              ,                        uusbtinyisp                      ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST, uuasptavr,              ,                        uusbasp                          ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST, uaisptavr,              ,                        uavrispmkii                      ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST, uaicetavr,              ,                        uatmelice                        ,       , tavr     ))
$(eval $(call GEN_SINGLE_TEST, uupditavr,              ,                        userialupdi                      , \\n\\n, tavr     ))

$(eval $(call GEN_SINGLE_TEST,      mavr,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,     cmavr,              ,                        clean   --en-jxm-lload-verbose   ,       , mavr     ))
$(eval $(call GEN_XMULTI_TEST,      mavr, cmavr                                                                  ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,     hmavr,              ,                        btlib_help                       , \\n   , mavr     ))
$(eval $(call GEN_SINGLE_TEST,     s1mavr,             ,                        sercon1                          ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,     s2mavr,             ,                        sercon2                          ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,     ssmavr,             ,                        sercon                           ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,     tmavr,              ,                        tcpcon                           , \\n   , mavr     ))
$(eval $(call GEN_SINGLE_TEST, uft2rmavr,              ,                        uft232r                          ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST, utinymavr,              ,                        uusbtinyisp                      ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST, uuaspmavr,              ,                        uusbasp                          , \\n   , mavr     ))
$(eval $(call GEN_SINGLE_TEST, uaispmavr,              ,                        uavrispmkii                      ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST, uaicemavr,              ,                        uatmelice                        ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST, uupdimavr,              ,                        userialupdi                      ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST, ugenxmavr,              ,                        ugenextprog                      , \\n   , mavr     ))
$(eval $(call GEN_SINGLE_TEST,  udfumavr,              ,                        udfu                             ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,  uhidmavr,              ,                        uhid                             ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,  ulptmavr,              ,                        ulpt                             ,       , mavr     ))
$(eval $(call GEN_SINGLE_TEST,    usmavr,              ,                        userial                          , \\n\\n, mavr     ))

$(eval $(call GEN_SINGLE_TEST,      xavr,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , xavr     ))
$(eval $(call GEN_SINGLE_TEST,     cxavr,              ,                        clean   --en-jxm-lload-verbose   ,       , xavr     ))
$(eval $(call GEN_XMULTI_TEST,      xavr, cxavr                                                                  ,       , xavr     ))
$(eval $(call GEN_SINGLE_TEST,     hxavr,              ,                        btlib_help                       , \\n   , xavr     ))
$(eval $(call GEN_SINGLE_TEST, uuaspxavr,              ,                        uusbasp                          ,       , xavr     ))
$(eval $(call GEN_SINGLE_TEST, uaicexavr,              ,                        uatmelice                        ,       , xavr     ))
$(eval $(call GEN_SINGLE_TEST, uaispxavr,              ,                        uavrispmkii                      , \\n\\n, xavr     ))

$(eval $(call GEN_SINGLE_TEST,       lds,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , lds      ))
$(eval $(call GEN_SINGLE_TEST,      clds,              ,                        clean   --en-jxm-lload-verbose   ,       , lds      ))
$(eval $(call GEN_XMULTI_TEST,       lds, clds                                                                   , \\n\\n, lds      ))

$(eval $(call GEN_SINGLE_TEST,      8266,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,     c8266,              ,                        clean   --en-jxm-lload-verbose   ,       , 8266     ))
$(eval $(call GEN_XMULTI_TEST,      8266, c8266                                                                  ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,     h8266,              ,                        btlib_help                       ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,     s8266,              ,                        sercon                           ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,     p8266,              ,                        serplot --en-opengl              , \\n   , 8266     ))
$(eval $(call GEN_SINGLE_TEST,    us8266,              ,                        userial                          ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,    uo8266,              ,                        uota                             ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,  usfs8266,              ,                        ufsserial                        ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,  uofs8266,              ,                        ufsota                           ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,     z8266,              ,                        clean_all                        ,       , 8266     ))
$(eval $(call GEN_SINGLE_TEST,     d8266,              ,                        stdec                            , \\n\\n, 8266     ))

$(eval $(call GEN_SINGLE_TEST,      2040,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,     c2040,              ,                        clean   --en-jxm-lload-verbose   ,       , 2040     ))
$(eval $(call GEN_XMULTI_TEST,      2040, c2040                                                                  ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,     h2040,              ,                        btlib_help                       ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,     s2040,              ,                        sercon                           , \\n   , 2040     ))
$(eval $(call GEN_SINGLE_TEST,    uf2040,              ,                        uuf2                             ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,    uw2040,              ,                        uswd                             ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,    dw2040,              ,                        dswd                             ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,    dg2040,              ,                        dgdb                             ,       , 2040     ))
$(eval $(call GEN_SINGLE_TEST,    rw2040,              ,                        rswd                             , \\n\\n, 2040     ))

$(eval $(call GEN_SINGLE_TEST,     s32f1,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 32f103   ))
$(eval $(call GEN_SINGLE_TEST,    cs32f1,              ,                        clean   --en-jxm-lload-verbose   ,       , 32f103   ))
$(eval $(call GEN_XMULTI_TEST,     s32f1, cs32f1                                                                 ,       , 32f103   ))
$(eval $(call GEN_SINGLE_TEST,    hs32f1,              ,                        btlib_help                       , \\n   , 32f103   ))
$(eval $(call GEN_SINGLE_TEST,   uws32f1,              ,                        uswd                             ,       , 32f103   ))
$(eval $(call GEN_SINGLE_TEST,   dws32f1,              ,                        dswd                             ,       , 32f103   ))
$(eval $(call GEN_SINGLE_TEST,   dgs32f1,              ,                        dgdb                             ,       , 32f103   ))
$(eval $(call GEN_SINGLE_TEST,   rws32f1,              ,                        rswd                             , \\n\\n, 32f103   ))

$(eval $(call GEN_SINGLE_TEST,   s32f1o8,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 32f103o8k))
$(eval $(call GEN_SINGLE_TEST,  cs32f1o8,              ,                        clean   --en-jxm-lload-verbose   ,       , 32f103o8k))
$(eval $(call GEN_XMULTI_TEST,   s32f1o8, cs32f1o8                                                               ,       , 32f103o8k))
$(eval $(call GEN_SINGLE_TEST,  hs32f1o8,              ,                        btlib_help                       , \\n\\n, 32f103o8k))

$(eval $(call GEN_SINGLE_TEST,     s32f4,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,    cs32f4,              ,                        clean   --en-jxm-lload-verbose   ,       , 32f411   ))
$(eval $(call GEN_XMULTI_TEST,     s32f4, cs32f4                                                                 ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,    hs32f4,              ,                        btlib_help                       , \\n   , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   ufs32f4,              ,                        udfu                             ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   uws32f4,              ,                        uswd                             ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   dws32f4,              ,                        dswd                             ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   rws32f4,              ,                        rswd                             ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   ujs32f4,              ,                        ujtag                            , \\n   , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   djs32f4,              ,                        djtag                            ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   rjs32f4,              ,                        rjtag                            ,       , 32f411   ))
$(eval $(call GEN_SINGLE_TEST,   dgs32f4,              ,                        dgdb                             , \\n\\n, 32f411   ))

##### ??? TODO : Add support for STM32G4XX in JxMake loadable library ??? #####
#$(eval $(call GEN_SINGLE_TEST,     s32g4,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 32g431   ))
#$(eval $(call GEN_SINGLE_TEST,    cs32g4,              ,                        clean   --en-jxm-lload-verbose   ,       , 32g431   ))
#$(eval $(call GEN_XMULTI_TEST,     s32g4, cs32g4                                                                 ,       , 32g431   ))
#$(eval $(call GEN_SINGLE_TEST,    hs32g4,              ,                        btlib_help                       , \\n   , 32g431   ))
#$(eval $(call GEN_SINGLE_TEST,   uws32g4,              ,                        uswd                             ,       , 32g431   ))
#$(eval $(call GEN_SINGLE_TEST,   dws32g4,              ,                        dswd                             ,       , 32g431   ))
#$(eval $(call GEN_SINGLE_TEST,   dgs32g4,              ,                        dgdb                             ,       , 32g431   ))
#$(eval $(call GEN_SINGLE_TEST,   rws32g4,              ,                        rswd                             , \\n\\n, 32g431   ))

$(eval $(call GEN_SINGLE_TEST,     s32h7,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 32h750   ))
$(eval $(call GEN_SINGLE_TEST,    cs32h7,              ,                        clean   --en-jxm-lload-verbose   ,       , 32h750   ))
$(eval $(call GEN_XMULTI_TEST,     s32h7, cs32h7                                                                 ,       , 32h750   ))
$(eval $(call GEN_SINGLE_TEST,    hs32h7,              ,                        btlib_help                       , \\n   , 32h750   ))
$(eval $(call GEN_SINGLE_TEST,   uws32h7,              ,                        uswd                             ,       , 32h750   ))
$(eval $(call GEN_SINGLE_TEST,   dws32h7,              ,                        dswd                             ,       , 32h750   ))
$(eval $(call GEN_SINGLE_TEST,   dgs32h7,              ,                        dgdb                             ,       , 32h750   ))
$(eval $(call GEN_SINGLE_TEST,   rws32h7,              ,                        rswd                             , \\n\\n, 32h750   ))

##### ??? TODO : Add support for STM32L0XX in JxMake loadable library ??? #####
#$(eval $(call GEN_SINGLE_TEST,     s32l0,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 32l011   ))
#$(eval $(call GEN_SINGLE_TEST,    cs32l0,              ,                        clean   --en-jxm-lload-verbose   ,       , 32l011   ))
#$(eval $(call GEN_XMULTI_TEST,     s32l0, cs32l0                                                                 ,       , 32l011   ))
#$(eval $(call GEN_SINGLE_TEST,    hs32l0,              ,                        btlib_help                       , \\n   , 32l011   ))
#$(eval $(call GEN_SINGLE_TEST,   uws32l0,              ,                        uswd                             ,       , 32l011   ))
#$(eval $(call GEN_SINGLE_TEST,   dws32l0,              ,                        dswd                             ,       , 32l011   ))
#$(eval $(call GEN_SINGLE_TEST,   dgs32l0,              ,                        dgdb                             ,       , 32l011   ))
#$(eval $(call GEN_SINGLE_TEST,   rws32l0,              ,                        rswd                             , \\n\\n, 32l011   ))

$(eval $(call GEN_SINGLE_TEST,      s8s0,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 8s003    ))
$(eval $(call GEN_SINGLE_TEST,     cs8s0,              ,                        clean   --en-jxm-lload-verbose   ,       , 8s003    ))
$(eval $(call GEN_XMULTI_TEST,      s8s0, cs8s0                                                                  ,       , 8s003    ))
$(eval $(call GEN_SINGLE_TEST,     hs8s0,              ,                        btlib_help                       , \\n   , 8s003    ))
$(eval $(call GEN_SINGLE_TEST,    uws8s0,              ,                        uswim                            ,       , 8s003    ))
$(eval $(call GEN_SINGLE_TEST,    rws8s0,              ,                        rswim                            , \\n\\n, 8s003    ))

$(eval $(call GEN_SINGLE_TEST,      s8s1,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 8s103    ))
$(eval $(call GEN_SINGLE_TEST,     cs8s1,              ,                        clean   --en-jxm-lload-verbose   ,       , 8s103    ))
$(eval $(call GEN_XMULTI_TEST,      s8s1, cs8s1                                                                  ,       , 8s103    ))
$(eval $(call GEN_SINGLE_TEST,     hs8s1,              ,                        btlib_help                       , \\n   , 8s103    ))
$(eval $(call GEN_SINGLE_TEST,    uws8s1,              ,                        uswim                            ,       , 8s103    ))
$(eval $(call GEN_SINGLE_TEST,    rws8s1,              ,                        rswim                            , \\n\\n, 8s103    ))

$(eval $(call GEN_SINGLE_TEST,      s8l1,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 8l151    ))
$(eval $(call GEN_SINGLE_TEST,     cs8l1,              ,                        clean   --en-jxm-lload-verbose   ,       , 8l151    ))
$(eval $(call GEN_XMULTI_TEST,      s8l1, cs8l1                                                                  ,       , 8l151    ))
$(eval $(call GEN_SINGLE_TEST,     hs8l1,              ,                        btlib_help                       , \\n   , 8l151    ))
$(eval $(call GEN_SINGLE_TEST,    uws8l1,              ,                        uswim                            ,       , 8l151    ))
$(eval $(call GEN_SINGLE_TEST,    rws8l1,              ,                        rswim                            , \\n\\n, 8l151    ))

$(eval $(call GEN_SINGLE_TEST,      8952,              , --en-jxm-cache-verbose run     --en-jxm-lload-verbose   ,       , 8952     ))
$(eval $(call GEN_SINGLE_TEST,     c8952,              ,                        clean   --en-jxm-lload-verbose   ,       , 8952     ))
$(eval $(call GEN_XMULTI_TEST,      8952, c8952                                                                  ,       , 8952     ))
$(eval $(call GEN_SINGLE_TEST,     h8952,              ,                        btlib_help                       ,       , 8952     ))
$(eval $(call GEN_SINGLE_TEST, utiny8952,              ,                        uusbtinyisp                      ,       , 8952     ))
$(eval $(call GEN_SINGLE_TEST, uuasp8952,              ,                        uusbasp                          , \\n\\n, 8952     ))


##### Rule details for the applications above
$(eval $(call GEN_FBUILD_RULE, gtest1        , GTEST1 , ../test                        ))
$(eval $(call GEN_FBUILD_RULE, gtest2        , GTEST2 , ../test                        ))
$(eval $(call GEN_FBUILD_RULE, atest1        , ATEST1 , ../test                        ))
$(eval $(call GEN_FBUILD_RULE, ptest1x       , PTEST1X, ../test                        ))
$(eval $(call GEN_FBUILD_RULE, ptest2x       , PTEST2X, ../test                        ))
$(eval $(call GEN_FBUILD_RULE, ptest3x       , PTEST3X, ../test                        ))
$(eval $(call GEN_FBUILD_RULE, ptest1b       , PTEST1B, ../test                        ))
$(eval $(call GEN_FBUILD_RULE, ptest2b       , PTEST2B, ../test                        ))
$(eval $(call GEN_FBUILD_RULE, ptestf        , PTESTF , ../test                        ))

$(eval $(call GEN_FBUILD_RULE, htest         , APP    , ../test                        ))

$(eval $(call GEN_UBUILD_RULE, util_STM32Spec, APP    , ../util/STM32Spec              ))

$(eval $(call GEN_DBUILD_RULE, cpp           , APP    , ../test/src/cpp                ))
$(eval $(call GEN_DBUILD_RULE, cpp20m        , APP    , ../test/src/cpp20mod           ))
$(eval $(call GEN_DBUILD_RULE, java          , APP    , ../test/src/java               ))

$(eval $(call GEN_DBUILD_RULE, lds           , APP    , ../test/src/lds                ))

$(eval $(call GEN_DBUILD_RULE, ardu          , APP    , ../test/src/cpp_arduino        ))
$(eval $(call GEN_DBUILD_RULE, bm8266        , APP    , ../test/src/cpp_bm_esp8266     ))
$(eval $(call GEN_DBUILD_RULE, bm32          , APP    , ../test/src/cpp_bm_esp32       ))
$(eval $(call GEN_DBUILD_RULE, bm32c3        , APP    , ../test/src/cpp_bm_esp32c3     ))
$(eval $(call GEN_DBUILD_RULE, bmsd21        , APP    , ../test/src/cpp_bm_atsamd21    ))
$(eval $(call GEN_DBUILD_RULE, bms3x8        , APP    , ../test/src/cpp_bm_atsam3x8    ))
$(eval $(call GEN_DBUILD_RULE, bmnr51        , APP    , ../test/src/cpp_bm_nrf51822    ))
$(eval $(call GEN_DBUILD_RULE, bmnr52        , APP    , ../test/src/cpp_bm_nrf52840    ))
$(eval $(call GEN_DBUILD_RULE, tavr          , APP    , ../test/src/cpp_attiny         ))
$(eval $(call GEN_DBUILD_RULE, mavr          , APP    , ../test/src/cpp_atmega         ))
$(eval $(call GEN_DBUILD_RULE, xavr          , APP    , ../test/src/cpp_atxmega        ))
$(eval $(call GEN_DBUILD_RULE, 8266          , APP    , ../test/src/cpp_esp8266        ))
$(eval $(call GEN_DBUILD_RULE, 2040          , APP    , ../test/src/cpp_rp2040         ))
$(eval $(call GEN_DBUILD_RULE, 32f103        , APP    , ../test/src/cpp_stm32f103      ))
$(eval $(call GEN_DBUILD_RULE, 32f103o8k     , APP    , ../test/src/cpp_stm32f103_ofs8k))
$(eval $(call GEN_DBUILD_RULE, 32f411        , APP    , ../test/src/cpp_stm32f411      ))
#$(eval $(call GEN_DBUILD_RULE, 32g431        , APP    , ../test/src/cpp_stm32g431      ))
$(eval $(call GEN_DBUILD_RULE, 32h750        , APP    , ../test/src/cpp_stm32h750      ))
#$(eval $(call GEN_DBUILD_RULE, 32l011        , APP    , ../test/src/cpp_stm32l011      ))
$(eval $(call GEN_DBUILD_RULE, 8s003         , APP    , ../test/src/cpp_stm8s003       ))
$(eval $(call GEN_DBUILD_RULE, 8s103         , APP    , ../test/src/cpp_stm8s103       ))
$(eval $(call GEN_DBUILD_RULE, 8l151         , APP    , ../test/src/cpp_stm8l151       ))
$(eval $(call GEN_DBUILD_RULE, 8952          , APP    , ../test/src/cpp_at89s52        ))


##### Build the Java class files
%.class: %.java
	@echo $@
	@$(JC) $(JFLAGS) $<

define PP_JSI
$(1): $(1).in
	@echo $(1)
#	@gcc   -x c -nostdinc -E -P -C $(1).in -o $(1).tmp
#	@clang -x c -nostdinc -E -P -C $(1).in -o $(1).tmp
	@python3 ../3rd_party/tools/pcpp/pcmd.py --line-directive '' --passthru-comments $(1).in -o $(1).tmp
	@cat $(1).tmp | sed -r '/\/\*{5} PCPP_END \*{5}\//{n;N;d}' | sed -r '/\/\*{5} PCPP_BEG \*{5}\//,/\/\*{5} PCPP_END \*{5}\//d' | sed 's/__NL__/\n   /g' > $(1)
	@rm -f $(1).tmp
endef

define PP_DEP
jxm/ugc/Prog$(strip $(1)).java : $(wildcard jxm/ugc/config.inc/$(strip $(2)).java.inc)
endef

define PB_DEP
jxm/ugc/Prog$(strip $(1)).java : $(wildcard jxm/ugc/Prog$(strip $(2))_*.java.inc)
endef

$(foreach I,$(JAVA_SOURCES_PP),$(eval $(call PP_JSI,$(I))))

build_classes: $(JAVA_SOURCES_PP) $(MAIN_CLASS) $(JAVA_CLASSES)
	@echo > /dev/null

##### NOTE : Do not forget to update these manual dependency list!  #####
$(eval $(call PP_DEP, ISP            , ISP.*            ))
$(eval $(call PP_DEP, TPI            , TPI.*            ))
$(eval $(call PP_DEP, UPDI           , UPDI.*           ))
$(eval $(call PP_DEP, PDI            , PDI.*            ))

$(eval $(call PP_DEP, LGT8           , LGT8.*           ))

$(eval $(call PP_DEP, SWIM           , SWIM.*           ))
$(eval $(call PP_DEP, SWDLowLevel    , SWD.*            ))

$(eval $(call PP_DEP, PIC16          , PIC.PIC10        ))
$(eval $(call PP_DEP, PIC16          , PIC.PIC12        ))
$(eval $(call PP_DEP, PIC16          , PIC.PIC16        ))
$(eval $(call PP_DEP, PIC18          , PIC.PIC18        ))
$(eval $(call PP_DEP, PIC24          , PIC.PIC24        ))
$(eval $(call PP_DEP, DSPIC30        , PIC.dsPIC30      ))
$(eval $(call PP_DEP, DSPIC33        , PIC.dsPIC33      ))

$(eval $(call PP_DEP, BootAVR109     , BootAVRSimpleUSB ))
$(eval $(call PP_DEP, BootSTK500     , BootAVRSimpleUART))
$(eval $(call PP_DEP, BootSTK500v2   , BootAVRSimpleUART))
$(eval $(call PP_DEP, BootChip45     , BootAVRSimpleUART))
$(eval $(call PP_DEP, BootTSB        , BootAVRSimpleUART))
$(eval $(call PP_DEP, BootURCLOCK    , BootURCLOCK      ))
$(eval $(call PP_DEP, BootSTM32Serial, BootSTM32Common  ))
$(eval $(call PP_DEP, BootSTM32DFU   , BootSTM32Common  )) # USB
$(eval $(call PP_DEP, ProgBootLUFAHID, BootAVRSimpleUSB )) # USB
$(eval $(call PP_DEP, ProgBootAVRDFU , BootAVRSimpleUSB )) # USB
$(eval $(call PP_DEP, ProgBootAVRDFU , BootAVRXSimpleUSB)) # USB
$(eval $(call PP_DEP, BootOpenBLT    , BootSTM32Common  ))
$(eval $(call PP_DEP, BootSAMBA      , BootSAMBA        ))
$(eval $(call PB_DEP, BootSAMBA      , BootSAMBA        ))
$(eval $(call PP_DEP, BootUSBasp     , BootAVRSimpleUART)) # VUSB


##### Build the Java JAR file
JFL = filelist.txt
JMF = manifest_JXM_Main.txt
CMF = manifest_test.txt

EXF = -not -name '*.java'     \
      -not -name '*.java.in'  \
      -not -name '*.java.inc' \
      -not -name '*.c'        \
      -not -name '*.h'        \
      -not -name '*.jxm.bin'  \
      -not -name '*.diff'     \
      -not -name '*.jar'

build_jar: $(JAR_FILE)

$(JAR_FILE) : build_classes $(JMF)
	@echo $@
	@rm -f $(JFL)
	@rm -f $(JAR_FILE)
	@(                                                     \
		mkdir -p $(JAR_RES_RDIR);                      \
		for I in $(JAR_RES_DIRS); do                   \
			cp -R $$I $(JAR_RES_RDIR);             \
		done;                                          \
		for I in $(JAR_SUB_DIRS); do                   \
			find $$I -type f $(EXF) >> $(JFL).tmp; \
		done;                                          \
		cat $(JFL).tmp | sort >> $(JFL);               \
	)
	@(                                                                                                                 \
		if [ "$(MAIN_CLASS)" == "$(APP_CLASS)" ]; then                                                             \
			jar cfmv $(JAR_FILE) $(JMF) $(basename $(MAIN_CLASS))*.class `cat $(JFL)` ../LICENSE* > /dev/null; \
		else                                                                                                       \
			echo "Main-Class: $(basename $(MAIN_CLASS))" > $(CMF);                                             \
			jar cfmv $(JAR_FILE) $(CMF) $(basename $(MAIN_CLASS))*.class `cat $(JFL)` ../LICENSE* > /dev/null; \
			rm -f $(CMF);                                                                                      \
		fi;                                                                                                        \
	)
	@$(MAKE) -s clean_jxm_jar_tmp

clean_jxm_jar_tmp:
	@rm  -f $(JFL)
	@rm  -f $(JFL).tmp
	@rm -rf $(JAR_RES_RDIR)


##### Targets for deleting all unused KiCad8 files and restoring all KiCad8 'fp-info-cache' files
KC8_HW_Experiment     := ../hardware/Experiment/KiCad8
KC8_HW_Tools          := ../hardware/Tools/KiCad8
KC8_HW_USB_GPIO_II    := ../hardware/JxMake_USB_GPIO_II/KiCad8

KC8_fpInfoCache       := fp-info-cache
KC8_dash_backups      := -backups
KC8_SRC_fpInfoCache   := ../hardware/_KiCad8_Common_fp-info-cache

KC8_DIRS_fpInfoCache  :=                                                \
    $(KC8_HW_Experiment)/Analog_Probe_Buffer                            \
    $(KC8_HW_Experiment)/Analog_Probe_Buffer_II                         \
    $(KC8_HW_Experiment)/Digital_Probe_Buffer                           \
    $(KC8_HW_Experiment)/Charge_Pump_Supply                             \
    $(KC8_HW_Experiment)/Isolated_DC-DC-Converter_Supply                \
    $(KC8_HW_Experiment)/LM338                                          \
    $(KC8_HW_Experiment)/AVR_DxExSx_SSOP28                              \
    $(KC8_HW_Tools)/4Digit_Voltmeter                                    \
    $(KC8_HW_Tools)/4Digit_Voltmeter_II                                 \
    $(KC8_HW_Tools)/Analog_Switch_Variant                               \
    $(KC8_HW_Tools)/Power_Supply_Control                                \
    $(KC8_HW_Tools)/UPDI_Connector_Converter                            \
    $(KC8_HW_Tools)/USB_Serial_Hub_FEFT                                 \
    $(KC8_HW_Tools)/USB_Serial_Hub_GLST                                 \
    $(KC8_HW_USB_GPIO_II)/Logic_Level_Translator                        \
    $(KC8_HW_USB_GPIO_II)/LLT_RSide_Protector                           \
    $(KC8_HW_USB_GPIO_II)/USB_GPIO_Module                               \
    $(KC8_HW_USB_GPIO_II)/Versatile_MCU_Programmer/Normal_PCB           \
    $(KC8_HW_USB_GPIO_II)/Versatile_MCU_Programmer/Prototype_PCB_Matrix \
    $(KC8_HW_USB_GPIO_II)/Versatile_MCU_Programmer_II                   \
    $(KC8_HW_USB_GPIO_II)/High_Voltage_Attachment                       \
    $(KC8_HW_USB_GPIO_II)/High_Voltage_Attachment_II                    \
    $(KC8_HW_USB_GPIO_II)/HVA_II_Alternative_Boost_Converter

KC8_DIRS_dash_backups :=                                                      \
    $(KC8_HW_Experiment)/Analog_Probe_Buffer/APB                              \
    $(KC8_HW_Experiment)/Analog_Probe_Buffer_II/APB                           \
    $(KC8_HW_Experiment)/Analog_Probe_Buffer_II/APB_Lite                      \
    $(KC8_HW_Experiment)/Digital_Probe_Buffer/DPB                             \
    $(KC8_HW_Experiment)/Charge_Pump_Supply/CPS                               \
    $(KC8_HW_Experiment)/Charge_Pump_Supply/Prototype_PCB_Matrix              \
    $(KC8_HW_Experiment)/Isolated_DC-DC-Converter_Supply/ICS                  \
    $(KC8_HW_Experiment)/Isolated_DC-DC-Converter_Supply/Prototype_PCB_Matrix \
    $(KC8_HW_Experiment)/LM338/338                                            \
    $(KC8_HW_Experiment)/AVR_DxExSx_SSOP28/A28                                \
    $(KC8_HW_Tools)/4Digit_Voltmeter/VM4                                      \
    $(KC8_HW_Tools)/4Digit_Voltmeter_II/VM4                                   \
    $(KC8_HW_Tools)/Analog_Switch_Variant/ASV                                 \
    $(KC8_HW_Tools)/Power_Supply_Control/PSC                                  \
    $(KC8_HW_Tools)/UPDI_Connector_Converter/UCC                              \
    $(KC8_HW_Tools)/USB_Serial_Hub_FEFT/USH                                   \
    $(KC8_HW_Tools)/USB_Serial_Hub_GLST/USH                                   \
    $(KC8_HW_USB_GPIO_II)/USB_GPIO_Module/UGM                                 \
    $(KC8_HW_USB_GPIO_II)/Logic_Level_Translator/LLT                          \
    $(KC8_HW_USB_GPIO_II)/LLT_RSide_Protector/RSP                             \
    $(KC8_HW_USB_GPIO_II)/LLT_RSide_Protector/Prototype_PCB_Matrix            \
    $(KC8_HW_USB_GPIO_II)/Versatile_MCU_Programmer/Normal_PCB/VMP             \
    $(KC8_HW_USB_GPIO_II)/Versatile_MCU_Programmer/Prototype_PCB_Matrix/VMP   \
    $(KC8_HW_USB_GPIO_II)/Versatile_MCU_Programmer_II/VMP                     \
    $(KC8_HW_USB_GPIO_II)/High_Voltage_Attachment/HVA                         \
    $(KC8_HW_USB_GPIO_II)/High_Voltage_Attachment_II/HVA                      \
    $(KC8_HW_USB_GPIO_II)/HVA_II_Alternative_Boost_Converter/ABC

kicad8_clean:
	@(                                              \
		for D in $(KC8_DIRS_fpInfoCache); do    \
			rm  -vf $$D/$(KC8_fpInfoCache); \
			rm  -vf $$D/*.wrl;              \
		done;                                   \
		for D in $(KC8_DIRS_dash_backups); do   \
			rm -rvf $$D$(KC8_dash_backups); \
		done;                                   \
	)

kicad8_fp-info-cache:
	@(                                                                           \
		for D in $(KC8_DIRS_fpInfoCache); do                                 \
			if [ -d "$$D" ]; then                                        \
				cp -v $(KC8_SRC_fpInfoCache) $$D/$(KC8_fpInfoCache); \
			fi;                                                          \
		done;                                                                \
	)


##### Clean all
clean: jxmlib_clean kicad8_clean
	@rm  -vf $(JAVA_SOURCES_RM)
	@rm  -vf `find . -name '*.class'`
	@rm   -f $(APP_JAR)
	@$(MAKE) -s clean_jxm_jar_tmp
	@rm  -vf $(TEST_JARS)
	@rm  -vf tmp.xml
	@rm  -vf *.log.txt
	@rm -rvf ~/.jSerialComm /tmp/jSerialComm
	@rm -rvf ../test/src/cpp/build
	@rm  -vf ../test/src/cpp/*.pdb
	@rm  -vf ../test/src/cpp/*.stackdump
	@rm -rvf ../test/src/cpp20mod/build
	@rm  -vf ../test/src/cpp20mod/*.pdb
	@rm  -vf ../test/src/cpp20mod/*.stackdump
	@$(MAKE) -s -C ../test/src/java clean
	@$(MAKE) -s -C ../test/src/cpp20mod clean
	@rm -rvf ../test/src/cpp_attiny/build
	@rm -rvf ../test/src/cpp_atmega/build
	@rm -rvf ../test/src/cpp_atxmega/build
	@rm -rvf ../test/src/cpp_esp8266/build
	@rm -rvf ../test/src/cpp_bm_xtensa_dummy/build
	@rm -rvf ../test/src/cpp_bm_esp8266/build
	@rm -rvf ../test/src/cpp_bm_esp32/build
	@rm -rvf ../test/src/cpp_bm_esp32c3/build
	@rm -rvf ../test/src/cpp_bm_atsamd21/build
	@rm -rvf ../test/src/cpp_bm_atsam3x8/build
	@rm -rvf ../test/src/cpp_bm_nrf51822/build
	@rm -rvf ../test/src/cpp_bm_nrf52840/build
	@rm -rvf ../test/src/cpp_rp2040/build
	@rm -rvf ../test/src/cpp_rp2040/build2
	@rm -rvf ../test/src/cpp_stm32f103/build
	@rm -rvf ../test/src/cpp_stm32f103/test_dir
	@rm -rvf ../test/src/cpp_stm32f103_ofs8k/build
	@rm -rvf ../test/src/cpp_stm32f103_ofs8k/test_dir
	@rm -rvf ../test/src/cpp_stm32f411/build
	@rm -rvf ../test/src/cpp_stm32f411/test_dir
	@rm -rvf ../test/src/cpp_stm32g431/build
	@rm -rvf ../test/src/cpp_stm32g431/test_dir
	@rm -rvf ../test/src/cpp_stm32h750/build
	@rm -rvf ../test/src/cpp_stm32h750/test_dir
	@rm -rvf ../test/src/cpp_stm32l011/build
	@rm -rvf ../test/src/cpp_stm32l011/test_dir
	@rm -rvf ../test/src/cpp_stm8s003/build
	@rm -rvf ../test/src/cpp_stm8s003/test_dir
	@rm -rvf ../test/src/cpp_stm8s103/build
	@rm -rvf ../test/src/cpp_stm8s103/test_dir
	@rm -rvf ../test/src/cpp_stm8l151/build
	@rm -rvf ../test/src/cpp_stm8l151/test_dir
	@rm -rvf ../test/src/cpp_at89s52/build
	@rm -rvf ../test/src/cpp_arduino/build
	@rm -rvf ../test/src/cpp_arduino/Blink/private.key
	@rm -rvf ../test/src/cpp_arduino/Blink/public.key
	@rm -rvf ../test/src/cpp_arduino/OTA/private.key
	@rm -rvf ../test/src/cpp_arduino/OTA/public.key
	@rm -rvf ../test/src/java/build
	@rm -rvf ../test/src/lds/build
	@rm  -vf ../test/test.tar.zst
	@rm  -vf ../test/test.tar.xz
	@rm  -vf ../test/test.tar.bz2
	@rm  -vf ../test/test.tar.gz
	@rm  -vf ../test/test.tar.zip
	@rm  -vf ../test/test.zip
	@rm  -vf ../test/1-test.zip
	@rm  -vf ../test/1-tarinpdir-*.tar.zst
	@rm  -vf ../test/1-tarinpdir-*.tar.xz
	@rm  -vf ../test/1-tarinpdir-*.tar.bz2
	@rm  -vf ../test/1-tarinpdir-*.tar.gz
	@rm  -vf ../test/1-tarinpdir-*.tar.zip
	@rm -rvf ../test/1-tarinpdir_copy
	@rm -rvf ../test/1-taroutdir_untzst
	@rm -rvf ../test/1-taroutdir_untxz
	@rm -rvf ../test/1-taroutdir_untxz_copy
	@rm -rvf ../test/1-taroutdir_untbz2
	@rm -rvf ../test/1-taroutdir_untbz2_copy
	@rm -rvf ../test/1-taroutdir_untgz
	@rm -rvf ../test/1-taroutdir_untgz_copy
	@rm -rvf ../test/1-taroutdir_untzip
	@rm -rvf ../test/1-taroutdir_untzip_copy
	@rm -rvf ../test/1-zipoutdir
	@rm  -vf ../test/2-1-tarinpdir-*.tar
	@rm  -vf ../test/2-1-tarinpdir-*.tar.zst
	@rm  -vf ../test/2-1-tarinpdir-*.tar.xz
	@rm  -vf ../test/2-1-tarinpdir-*.tar.bz2
	@rm  -vf ../test/2-1-tarinpdir-*.tar.gz
	@rm -rvf ../test/2-1-taroutdir_untzs
	@rm -rvf ../test/2-1-taroutdir_untxz
	@rm -rvf ../test/2-1-taroutdir_untbz
	@rm -rvf ../test/2-1-taroutdir_untgz
	@rm -rvf ../test/3-fwctestdata-res
