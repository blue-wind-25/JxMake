/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace demo {

namespace {
}
float run_batch_pass(
    Net*                     model,
    const std::vector<Item>& data,
    int                      workers,
    int                      seed,
    int                      round_no,
    float                    jitter
)
{
    struct Slot {
    };
    auto do_slice = [&](int t, size_t begin, size_t end) {
        for(int t = 0; t < nworkers; ++t) {
            if(t == nworkers - 1) {
            }
        }
    }
}
    for( size_t i = 0; i + 1 < args.size(); ++i ) {
                                   ? run_batch_pass(
                                       &model, data, order, rate, batch, limit, workers,
                                       seed, round_no, jitter
                                   )
                                   : run_single_pass(&model, data, order, rate, workers);
        if( !val_set.empty() ) {
            if(val_loss < best_val_loss - 1e-4f) {
            }
        }
    }; // for
    entries.push_back( {"stage1.w",
                        {(uint16_t)kStage1Out, (uint16_t)kStageIn, (uint16_t)kStage1Kernel,
                         (uint16_t)kStage1Kernel},
                        snapshot.w1} );
    entries.push_back({"stage2.w",
                        snapshot.w2});

} // namespace demo
}
