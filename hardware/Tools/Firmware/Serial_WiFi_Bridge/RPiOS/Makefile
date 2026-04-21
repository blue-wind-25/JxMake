#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####


##### Configuration
MAKEFLAGS   += --no-print-directory

SSH_OPTS    := -o LogLevel=ERROR -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null
SSH_KEY     := -i $(HOME)/ssh-pi@raspberrypi-key
SSH_USR_HST := pi@raspberrypi.local

SRC_DIR     := daemon
PI_DATA     := /mnt/data
PI_DATA_PI  := $(PI_DATA)/pi
PI_DATA_DMN := $(PI_DATA_PI)/$(SRC_DIR)
PI_SSH      := ssh $(SSH_OPTS) $(SSH_KEY) $(SSH_USR_HST)

define PI_RSYNC
rsync -avz -e "ssh $(SSH_OPTS) $(SSH_KEY)" "$(1)/" "$(SSH_USR_HST):$(2)"
endef

define PI_GCC
gcc -O3 -Wall -Wextra "$(1).c" -o "$(1).elf"
endef


.PHONY: default all serial console monitor data_rw data_ro copy
.NOTPARALLEL: all serial console monitor


##### The default target
default:
	@echo ""
	@echo "Available targets : all, serial, console, monitor"
	@echo ""


##### Build targets
all: serial_impl console_impl monitor_impl
	@$(MAKE) data_ro

serial: serial_impl
	@$(MAKE) data_ro

console: console_impl
	@$(MAKE) data_ro

monitor: monitor_impl
	@$(MAKE) data_ro


serial_impl: copy
	$(PI_SSH) "/bin/bash -c 'set -eo pipefail;                                                                             \
		sudo systemctl stop pi-serial-bridge-io0.service pi-serial-bridge-io1.service pi-serial-bridge-dbg.service  && \
		$(call PI_GCC,$(PI_DATA_DMN)/tcp_ser_dev_bridge)                                                            && \
		sudo systemctl start pi-serial-bridge-io0.service pi-serial-bridge-io1.service pi-serial-bridge-dbg.service    \
	'"

console_impl: copy
	$(PI_SSH) "/bin/bash -c 'set -eo pipefail;                   \
		sudo systemctl stop pi-console.service            && \
		$(call PI_GCC,$(PI_DATA_DMN)/tcp_pty_bash_bridge) && \
		sudo systemctl start pi-console.service              \
	'"

monitor_impl: copy
	$(PI_SSH) "/bin/bash -c 'set -eo pipefail;                       \
		sudo systemctl stop serial-device-monitoring.service  && \
		$(call PI_GCC,$(PI_DATA_DMN)/tcp_fstate_monitor)      && \
		sudo systemctl start serial-device-monitoring.service    \
	'"


##### Helper targets
data_rw:
	$(PI_SSH) 'sudo mount -o remount,rw $(PI_DATA)'

data_ro:
	$(PI_SSH) 'sudo mount -o remount,ro $(PI_DATA)'

copy: data_rw
	$(call PI_RSYNC,$(SRC_DIR),$(PI_DATA_DMN))
