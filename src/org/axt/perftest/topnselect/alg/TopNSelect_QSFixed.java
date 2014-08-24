package org.axt.perftest.topnselect.alg;


public class TopNSelect_QSFixed extends TopNSelect_QS {

	protected int[] indexes2;
	protected double[] scores2;

	int elementIndex = 0;
	private final int topN;

	public TopNSelect_QSFixed(ALG alg, int topN) {
		this(alg, topN, 1.5);
	}

	public TopNSelect_QSFixed(ALG alg, int topN, double loadFactor) {
		super(alg, 1);
		this.topN = topN;
		indexes2 = new int[Math.max(10, (int) (loadFactor * topN))];
		scores2 = new double[Math.max(10, (int) (loadFactor * topN))];
	}

	@Override
	public void sink(int index, double score) {

		if (elementIndex == indexes2.length) {
			orderTop(topN, -1);
			elementIndex = topN;
		}

		scores2[elementIndex] = score;
		indexes2[elementIndex] = index;
		elementIndex++;

	}

	private void orderTop(int topN, int rightX) {
		switch (alg) {
			case MED3:
				orderTheTopN3(scores2, indexes2, topN, rightX);
				break;
			case MEDIAN:
				orderTheTopN(scores2, indexes2, topN, rightX);
				break;
			case RAND:
				orderTheTopNRand(scores2, indexes2, topN, rightX);
				break;
			default:
				throw new RuntimeException("ALG not implemented " + alg);

		}
	}
}
