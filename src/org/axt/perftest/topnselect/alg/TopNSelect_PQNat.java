package org.axt.perftest.topnselect.alg;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class TopNSelect_PQNat implements TopNSelectAlg {

	interface IntDoubleComparator {
		public int compare(int i1, double f1, int i2, double f2);
	}

	static class IntDoublePriorityQueue {

		int elementCount = 0;
		int[] intarray; 
		double[] doublearray;

		final IntDoubleComparator comparator;

		public IntDoublePriorityQueue(int initialCapacity, IntDoubleComparator comparator) {
			this.intarray = new int[initialCapacity];
			this.doublearray = new double[initialCapacity];

			this.comparator = comparator;
		}

		public void add(int i, double f) {
			if (elementCount >= intarray.length) {
				if (comparator.compare(i, f, intarray[0], doublearray[0]) > 0) return;
				removeMax();
			}
			int idx = elementCount;
			intarray[idx] = i;
			doublearray[idx] = f;
			++elementCount;
			siftUp(idx);
		}

		public void removeMax() {
			if (elementCount == 0) throw new NoSuchElementException("Queue is empty");

			intarray[0] = intarray[elementCount - 1];
			doublearray[0] = doublearray[elementCount - 1];

			intarray[elementCount - 1] = 0;
			doublearray[elementCount - 1] = 0;

			elementCount--;

			siftDown(0, elementCount - 1);
		}

		public int getIntMax() {
			if (elementCount == 0) throw new NoSuchElementException("Queue is empty");
			return intarray[0];
		}

		public double getdoubleMax() {
			if (elementCount == 0) throw new NoSuchElementException("Queue is empty");
			return doublearray[0];
		}

		public int size() {
			return elementCount;
		}

		public void clear() {
			elementCount = 0;
		} 
		
		public int[] toIntArray() {
			return Arrays.copyOf(intarray, elementCount);
		}

		public double[] todoubleArray() {
			return Arrays.copyOf(doublearray, elementCount);
		}

		private void siftDown(int start, int end) {
			int root = start;
			while (root * 2 + 1 <= end) {
				int child = root * 2 + 1;
				if (child + 1 <= end && comparator.compare(intarray[child], doublearray[child], intarray[child + 1], doublearray[child + 1]) < 0) {
					child = child + 1;
				}
				if (comparator.compare(intarray[root], doublearray[root], intarray[child], doublearray[child]) < 0) {
					swap(intarray, doublearray, root, child);
					root = child;
				} else {
					return;
				}
			}
		}

		private void siftUp(int start) {
			while (start > 0) {
				int parent = (start - 1) / 2;
				if (comparator.compare(intarray[parent], doublearray[parent], intarray[start], doublearray[start]) >= 0) return;
				swap(intarray, doublearray, parent, start);
				start = parent;
			}
		}

		public static void swap(int[] intarray, double[] doublearray, int idx1, int idx2) {
			int i = intarray[idx1];
			double f = doublearray[idx1];
			intarray[idx1] = intarray[idx2];
			intarray[idx2] = i;
			doublearray[idx1] = doublearray[idx2];
			doublearray[idx2] = f;
		}


	}

	final IntDoublePriorityQueue pq;

	public TopNSelect_PQNat(int topN) {
		pq = new IntDoublePriorityQueue(topN, new IntDoubleComparator() {
			@Override
			public int compare(int i1, double f1, int i2, double f2) {
				return (f1 < f2 ? 1 : (f1 > f2 ? -1 : (i1 < i2 ? -1 : (i1 > i2 ? 1 : 0))));
			}
		});
	}

	@Override
	public void sink(int index, double score) {
		pq.add(index, score);
	}

	@Override
	public int[] getTopN(int topN) {
		return pq.toIntArray();
	}
}
