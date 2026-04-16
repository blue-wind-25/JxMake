ROOT_DICT_DIR := /data/aloysius/_Excluded_/Hunspell
BASE_DICT_DIR := $(ROOT_DICT_DIR)/dictionaries-cp-25.04.7-4

HUNSPELL_PATH := /opt/hunspell-1.7.2
UNMUNCH       := LD_LIBRARY_PATH=$(HUNSPELL_PATH)/lib:$$LD_LIBRARY_PATH $(HUNSPELL_PATH)/bin/unmunch
TMP_DICT_FILE := word_list.dic

all: de en es fr id nl ru

clean:
	@rm -vf *.zip $(TMP_DICT_FILE)

%.zip:
	@$(UNMUNCH) $(BASE_DICT_DIR)/$(SUBDIR)/$*.dic $(BASE_DICT_DIR)/$(SUBDIR)/$*.aff \
		| cut -d '/' -f1           \
		| grep -v '^-'             \
		| grep -v '[./]'           \
		| sed 's/[[:space:]]*$$//' \
		$(EXTRA_FILTER)            \
	> $(TMP_DICT_FILE)
	@zip -q  $(basename $@)$(ZIP_SUFFIX).zip $(TMP_DICT_FILE)
	@rm  -vf                                 $(TMP_DICT_FILE)


de: SUBDIR=de
de: de_DE_frami.zip

en: SUBDIR=en
en: en_AU.zip en_CA.zip en_GB.zip en_US.zip en_ZA.zip

es: SUBDIR=es
es: es_ES.zip

fr: SUBDIR=fr_FR
fr: fr.zip

id: SUBDIR=id
id: id_ID.zip

ja: SUBDIR=../hunspell-ja-1
ja: EXTRA_FILTER=| grep -P "^[\\p{Hiragana}\\p{Katakana}]+$$"
ja: ZIP_SUFFIX=_kana
ja: ja_JP.zip

nl: SUBDIR=nl_NL
nl: nl_NL.zip

ru: SUBDIR=ru_RU
ru: ru_RU.zip

