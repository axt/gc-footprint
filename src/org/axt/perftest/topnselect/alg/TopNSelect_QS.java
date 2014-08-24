package org.axt.perftest.topnselect.alg;

import java.util.Arrays;

public class TopNSelect_QS implements TopNSelectAlg {

	public enum ALG {
		MEDIAN, MED3, RAND
	}

	static class IntVector {
		int elementCount = 0;
		int[] array;
		
		public IntVector(int initialCapacity) {
			this.array = new int[initialCapacity];
		}
		
		public void add(int e) {
			if (elementCount >= array.length) {
				int newCapacity = (elementCount < 10)
					? 10
					: 2 * elementCount;
				increaseCapacity(newCapacity);
			}
			array[elementCount] = e;
			++elementCount;
		}
		
		public void set(int index, int value) {
			if (index < 0 || index >= size()) {
				throw new ArrayIndexOutOfBoundsException();
			}
			array[index] = value;
		}

		public int size() {
			return elementCount;
		}

		private void increaseCapacity(int newCapacity) {
			int oldCapacity = array.length;
			assert (newCapacity > oldCapacity);
			int[] newArray = new int[newCapacity];
			System.arraycopy(this.array, 0, newArray, 0, oldCapacity);
			this.array = newArray;
		}
		
		public int[] toArray() {
			int[] retval = new int[elementCount];
			System.arraycopy(this.array, 0, retval, 0, elementCount);
			return retval;
		}	
	}

	static class DoubleVector  {
		int elementCount = 0;
		double[] array;
		
		public DoubleVector(int initialCapacity) {
			this.array = new double[initialCapacity];
		}
		
		public void add(double e) {
			if (elementCount >= array.length) {
				int newCapacity = (elementCount < 10)
					? 10
					: 2 * elementCount;
				increaseCapacity(newCapacity);
			}
			array[elementCount] = e;
			++elementCount;
		}

		public void set(int index, double value) {
			if (index < 0 || index >= size()) {
				throw new ArrayIndexOutOfBoundsException();
			}
			array[index] = value;
		}

		public int size() {
			return elementCount;
		}

		private void increaseCapacity(int newCapacity) {
			int oldCapacity = array.length;
			assert (newCapacity > oldCapacity);
			double [] newArray = new double[newCapacity];
			System.arraycopy(this.array, 0, newArray, 0, oldCapacity);
			this.array = newArray;
		}
		
		public double[] toArray() {
			double[] retval = new double[elementCount];
			System.arraycopy(this.array, 0, retval, 0, elementCount);
			return retval;
		}	
	}

	
	protected final ALG alg;
	// private final int initialCapacity;
	protected final IntVector indexes;
	protected final DoubleVector scores;

	private static final int defaultInitialCapacity = 25000;

	public TopNSelect_QS(ALG alg) {
		this(alg, defaultInitialCapacity);
	}

	public TopNSelect_QS(ALG alg, int initialCapacity) {
		this.alg = alg;
		// this.initialCapacity = initialCapacity;

		indexes = new IntVector(initialCapacity);
		scores = new DoubleVector(initialCapacity);
	}

	@Override
	public void sink(int index, double score) {
		scores.add(score);
		indexes.add(index);
	}

	@Override
	public int[] getTopN(int topN) {
		int[] itemidx = indexes.toArray();
		double[] score = scores.toArray();

		switch (alg) {
			case MED3:
				orderTheTopN3(score, itemidx, topN, -1);
				break;
			case MEDIAN:
				orderTheTopN(score, itemidx, topN, -1);
				break;
			case RAND:
				orderTheTopNRand(score, itemidx, topN, -1);
				break;
			default:
				throw new RuntimeException("ALG not implemented " + alg);
		}

		int len = Math.min(topN, score.length);
		return Arrays.copyOf(itemidx, len);
	}

	// az topN-et a tomb elejere rendezi, kozben modositja az eredeti tombot.
	public static void orderTheTopN(double[] score, int[] idx, int topN,
			int rightX) {
		// We apply the quickselect algorithm to partition about the median,
		// and then ignore the last k elements.
		int left = 0;
		int right = rightX != -1 ? Math.min(rightX, (score.length - 1))
				: (score.length - 1);

		while (left < right) {

			int pivotIndex = (left + right + 1) >>> 1;

			int pivotNewIndex = partition(score, idx, left, right, pivotIndex);

			if (pivotNewIndex > topN) {
				right = pivotNewIndex - 1;
			} else if (pivotNewIndex < topN) {
				left = Math.max(pivotNewIndex, left + 1);
			} else {
				break;
			}
		}
	}

	public static void orderTheTopN3(double[] score, int[] idx, int topN, int rightX) {
		int left = 0;
		int right = rightX != -1 ? rightX : (score.length - 1);
		while (left < right) {

			int pivotIndex = (left + right + 1) >>> 1;
			int pivotIndex2 = (left + pivotIndex + 1) >>> 1;
			int pivotIndex3 = (pivotIndex + right + 1) >>> 1;
			if (pivotIndex2 > pivotIndex) {
				if (pivotIndex3 > pivotIndex) {
					if (pivotIndex2 > pivotIndex3) {
						pivotIndex = pivotIndex3;
					} else {
						pivotIndex = pivotIndex2;
					}
				}
			} else {
				if (pivotIndex3 < pivotIndex) {
					if (pivotIndex2 > pivotIndex3) {
						pivotIndex = pivotIndex2;
					} else {
						pivotIndex = pivotIndex3;
					}
				}
			}

			int pivotNewIndex = partition(score, idx, left, right, pivotIndex);
			if (pivotNewIndex > topN) {
				right = pivotNewIndex - 1;
			} else if (pivotNewIndex < topN) {
				left = Math.max(pivotNewIndex, left + 1);
			} else {
				break;
			}
		}
	}

	public static void orderTheTopNRand(double[] score, int[] idx, int topN, int rightX) {
		int left = 0;
		int right = rightX != -1 ? rightX : (score.length - 1);

		while (left < right) {
			int pivotIndex = left + (int) (Math.random() * (right - left));
			int pivotNewIndex = partition(score, idx, left, right, pivotIndex);
			if (pivotNewIndex > topN) {
				right = pivotNewIndex - 1;
			} else if (pivotNewIndex < topN) {
				left = Math.max(pivotNewIndex, left + 1);
			} else {
				break;
			}
		}
	}

	private static int partition(double[] score, int[] idx, int left, int right,
			int pivotIndex) {
		double pivotValue = score[pivotIndex];
		int pivotValueIdx = idx[pivotIndex];

		swap(score, idx, pivotIndex, right);

		int storeIndex = left;
		for (int i = left; i < right; i++) {
			if (score[i] > pivotValue
					|| ((score[i] == pivotValue) && (idx[i] < pivotValueIdx))) {
				swap(score, idx, storeIndex, i);
				storeIndex++;
			}
		}
		swap(score, idx, right, storeIndex);

		return storeIndex;
	}

	public static void swap(double[] doublearray, int[] intarray, int idx1, int idx2) {
		int i = intarray[idx1];
		double f = doublearray[idx1];
		intarray[idx1] = intarray[idx2];
		intarray[idx2] = i;
		doublearray[idx1] = doublearray[idx2];
		doublearray[idx2] = f;
	}
}
