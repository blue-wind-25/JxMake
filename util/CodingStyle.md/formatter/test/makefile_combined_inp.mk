#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

CC=gcc
CFLAGS=-O2 -Wall
LDFLAGS+=-lm
TARGET:=app

SRC = a.c \
b.c \
c.c

app:main.o  util.o

ifdef DEBUG
CFLAGS += -g
else
CFLAGS += -O2
endif

# recipe lines (leading tab) must stay byte-identical
app: main.o
	$(CC) -o $@ $^
	@echo done

# comment breaks assignment group
A=1
# note
BB=2
