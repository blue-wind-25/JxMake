/*
 * ##### This file has been modified by JxMake project #####
 */

package org.itadaki.bzip2;

/*
 * Suffix Array generator (Manber-Myers Doubling Algorithm with Radix Sort)
 */
public class BZip2DivSufSort {

	private final byte[] T;
	private final int[] SA;
	private final int n;

	public BZip2DivSufSort (final byte[] T, final int[] SA, final int n) {
		this.T = T;
		this.SA = SA;
		this.n = n;
	}

	public int bwt() {
		final int n = this.n;
		if (n == 0) return -1;
		if (n == 1) {
			SA[0] = T[0] & 0xff;
			return 0;
		}

		final int[] sa = this.SA;
		final int[] rank = new int[n];
		final int[] tmpSa = new int[n];
		final int[] tmpRank = new int[n];
		final int[] cnt = new int[Math.max (256, n + 1)];

		// Initial sort by single byte
		for (int i = 0; i < n; i++) cnt[T[i] & 0xff]++;
		for (int i = 1; i < 256; i++) cnt[i] += cnt[i - 1];
		for (int i = n - 1; i >= 0; i--) sa[--cnt[T[i] & 0xff]] = i;

		rank[sa[0]] = 0;
		int p = 0;
		for (int i = 1; i < n; i++) {
			if (T[sa[i]] != T[sa[i - 1]]) p++;
			rank[sa[i]] = p;
		}

		for (int len = 1; p < n - 1; len <<= 1) {
			int cur = 0;
			for (int i = 0; i < n; i++) {
				tmpSa[cur++] = (sa[i] - len + n) % n;
			}

			for (int i = 0; i <= p; i++) cnt[i] = 0;
			for (int i = 0; i < n; i++) cnt[rank[tmpSa[i]]]++;
			for (int i = 1; i <= p; i++) cnt[i] += cnt[i - 1];
			for (int i = n - 1; i >= 0; i--) sa[--cnt[rank[tmpSa[i]]]] = tmpSa[i];

			tmpRank[sa[0]] = 0;
			p = 0;
			for (int i = 1; i < n; i++) {
				int r1 = rank[sa[i]];
				int r2 = rank[(sa[i] + len) % n];
				int r3 = rank[sa[i - 1]];
				int r4 = rank[(sa[i - 1] + len) % n];
				if ((r1 != r3) || (r2 != r4)) p++;
				tmpRank[sa[i]] = p;
			}
			System.arraycopy (tmpRank, 0, rank, 0, n);
		}

		int bwtStartPointer = -1;
		for (int i = 0; i < n; i++) {
			if (sa[i] == 0) {
				bwtStartPointer = i;
				break;
			}
		}

		byte[] bwt = new byte[n];
		for (int i = 0; i < n; i++) {
			int idx = sa[i];
			bwt[i] = T[(idx == 0) ? n - 1 : idx - 1];
		}
		for (int i = 0; i < n; i++) {
			sa[i] = bwt[i] & 0xff;
		}

		return bwtStartPointer;
	}

}
