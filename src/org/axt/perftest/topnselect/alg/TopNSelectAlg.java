package org.axt.perftest.topnselect.alg;

public interface TopNSelectAlg {
	
	void sink(int index, double score);

	int[] getTopN(int topN);

}
