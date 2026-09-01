This directory intentionally holds no committed fixture files (a self-signed cert, .cat, and .inf are
not stored in the repo/workflow).

To exercise `sideload` mode with `sideload_source: ffm`, first run this workflow in `compare` mode,
download the `wdi-compare-output` artifact, and place its `FFM-cert.cer`, `FFM-wdi_cmp.cat`, and
`FFM-wdi_cmp.inf` files here as `cert.cer`, `wdi_cmp.cat`, and `wdi_cmp.inf` respectively - only
temporarily, for that one CI run; never commit them permanently, since they are throwaway self-signed test
fixtures, not a real production signing certificate. The sideload step fails fast with a clear error
if these files are absent.
