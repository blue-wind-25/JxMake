# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT

def f(func_sig):
    call_args = []
    for param in func_sig.parameters.values():
        match param.kind:
            case (
                inspect.Parameter.POSITIONAL_ONLY
                | inspect.Parameter.POSITIONAL_OR_KEYWORD
            ):
                call_args.append(param.name)
            case inspect.Parameter.VAR_POSITIONAL: call_args.append(f'*{param.name}')
            case inspect.Parameter.KEYWORD_ONLY: call_args.append(f'{param.name}={param.name}')
            case inspect.Parameter.VAR_KEYWORD: call_args.append(f'**{param.name}')
            case _: raise RuntimeError('Unsupported parameter kind', param.kind)
