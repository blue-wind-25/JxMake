# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT

for n in 8, 16, 32, 64:
    for signedness in '', 'u':
        @register(f'Struct331_{signedness}{n}', set_name=True)
        class _cls(Structure):
            pass
