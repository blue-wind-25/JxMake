/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

static device_record_t* find_slot(int existing, int have, workspace_t* ws)
{
          device_record_t* rec      = &device_table[device_count++];
    const spec_t*          spec_ptr = have ? &spec : NULL;
          workspace_t*     w        = ws ? ws : &local;
          int              mask     = flags & bits[existing];
    send_request.callback = &send_pending_response;
    rec                   = existing ? &device_table[existing] : rec;

    return mask ? rec : &device_table[0];
}
