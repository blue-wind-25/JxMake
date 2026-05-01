# ![JxMake Logo](docs/JxMake-Logo-Small.png)

[![Language: Java](https://img.shields.io/badge/Language-Java-orange?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Language: C/C++](https://img.shields.io/badge/Language-C%2FC%2B%2B-blue?style=flat&logo=c%2B%2B&logoColor=white)](https://en.cppreference.com)
[![Primary_License: LGPL v3+](https://img.shields.io/badge/Primary_License-LGPL_v3+-green?style=flat&logo=opensourcehardware&logoColor=white)](LICENSE)

JxMake is primarily licensed under **LGPL v3+**.

Copies of the licenses for third-party libraries/components included in this repository are located in the `3rd_party_library_licenses` directory. Please refer to individual sub-directories of the libraries/components for specific author credits and copyright headers.

---

JxMake is a Java-based cross-platform build system primarily inspired by the syntax and features of
GNU Make, Perforce Jam, and CMake.

Additionally, JxMake supports several features not typically found in other console-based build
systems, including a simple built-in GUI, a serial console, a serial plotter, and integrated
programmers for some commonly used MCUs. The JxMake repository also includes hardware schematics
and PCB designs ranging from simple to moderately complex, covering generic GPIO, basic programmers,
and various tools.

Building JxMake requires Java SDK 8 or later (note that Java SDK 8 is an LTS version).

This project is mirrored on both SourceForge and GitHub:
+ https://sourceforge.net/projects/jxmake
+ https://github.com/blue-wind-25/JxMake

---

Please refer to the JxMake usage page (run it with the `-h` or `--browse-docs` option) for how to
use the program.

Please refer to `docs/*.txt`, `test/*.jxm`, and `test/src/*/JxMakeFile` for the scripting language
syntax and examples.

---

All preprocessed `*.java` files generated from their corresponding `*.java.in` files (and
optionally some `*.java.inc` files) are preprocessed using `PCPP` (`3rd_party/tools/pcpp/pcmd.py`)
and GNU sed.

Currently, JxMake's loadable libraries (build scripts) are primarily focused on embedded systems
development. Future updates will enhance support for building desktop applications. However, please
note that these libraries are quite basic, so you may prefer to create custom libraries tailored
to your projects.

---

If you like this software (or part of this software), please consider donating to:<br>
&nbsp;&nbsp;&nbsp;&nbsp;PayPal: aloysius.indrayanto@gmail.com<br>
to support the software development.

---
