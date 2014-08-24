package org.axt.perftest.topnselect.alg;

import java.util.PriorityQueue;

public class TopNSelect_PQ implements TopNSelectAlg {

	public static final class Entry implements Comparable<Entry> {
		private final int index;
		private final double score;

		public Entry(int index, double score) {
			this.index = index;
			this.score = score;
		}

		public int getIndex() {
			return index;
		}

		public double getScore() {
			return score;
		}

		@Override
		public int compareTo(Entry t) {
			if (t.score == score) {
				return Integer.compare(index, t.index);
			} else {
				return (t.score < score) ? -1 : 1;
			}
		}
	}

	private final PriorityQueue<Entry> results = new PriorityQueue<Entry>();

	@Override
	public void sink(int index, double score) {
		Entry entry = new Entry(index, score);
		results.add(entry);
	}

	// Note: not optimized for cases where size < topN
	@Override
	public int[] getTopN(int topN) {
		int size = Math.min(topN, results.size());
		int[] indexes = new int[size];
		for (int i = 0; i < size; i++) {
			Entry entry = results.poll();
			indexes[i] = entry.index;
		}
		return indexes;
	}
}
