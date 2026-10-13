/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace pettrain {

namespace {
}
float train_minibatch_epoch_ext(
    ExtractorModelExt*         model,
    const std::vector<Sample>& data,
    int                        train_threads,
    int                        seed,
    int                        epoch,
    float                      gray_aug
)
{
    struct Worker {
    };
    auto run_slice = [&](int t, size_t begin, size_t end) {
        for(int t = 0; t < nthreads; ++t) {
            if(t == nthreads - 1) {
            }
        }
    }
}
    for( size_t i = 0; i + 1 < args.size(); ++i ) {
                                   ? train_minibatch_epoch_ext(
                                       &model, data, train_idx, lr_cur, batch, clip, train_threads,
                                       seed, epoch, gray_aug
                                   )
                                   : train_one_epoch_ext(
                                       &model, data, train_idx, lr_cur, train_threads
                                   );
        if( !held_out.empty() ) {
            if(held_out_loss < best_held_out_loss - 1e-4f) {
            }
        }
    }; // for
    tensors.push_back( {"extractor_conv1.w",
                        {(uint16_t)kExtractorConv1OutC, (uint16_t)kExtractorInC, (uint16_t)kExtractorConv1K,
                         (uint16_t)kExtractorConv1K},
                        emit_model.conv1_w} );
    tensors.push_back({"extractor_conv2.w",
                        emit_model.conv2_w});

} // namespace pettrain
}
