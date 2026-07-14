/*
 * This fixture is deliberately error-only: `in_file_config_error_inp.hpp` carries two
 * JXM_CFMT_CFG directives (one block-comment form, one line-comment form), which the
 * formatter must reject with a hard IOException/exit-1 -- never a partial-apply or a
 * silent "last one wins". There is no valid formatted result to compare against, so
 * this file exists only to satisfy the *_inp/*_out naming convention; it is not used
 * by `make test`'s diff loop (the fixture is commented out of INP_FILES on purpose).
 *
 * To exercise it manually, uncomment its INP_FILES line in the Makefile and confirm:
 *   java -jar target/code-formatter-1.00.jar --standalone --check test/in_file_config_error_inp.hpp
 * exits 1 with:
 *   jxmake-code-formatter: error: test/in_file_config_error_inp.hpp: multiple JXM_CFMT_CFG
 *   directives found -- only one is allowed per file
 */
