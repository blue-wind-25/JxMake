This directory intentionally holds no committed fixture files (a self-signed cert, .cat, and .inf are
not stored in the repo/workflow).

To exercise `sideload` mode with `sideload_source: ffm`, first run this workflow in `compare` mode,
download the `wdi-compare-output` artifact, and place its `FFM-cert.cer`, `FFM-wdi_cmp.cat`, and
`FFM-wdi_cmp.inf` files here as `cert.cer`, `wdi_cmp.cat`, and `wdi_cmp.inf` respectively (locally -
do not commit them). The sideload step fails fast with a clear error if these files are absent.
