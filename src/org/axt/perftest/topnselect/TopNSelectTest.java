package org.axt.perftest.topnselect;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.axt.perftest.topnselect.alg.TopNSelectAlg;
import org.axt.perftest.topnselect.alg.TopNSelect_QS.ALG;
import org.axt.perftest.topnselect.alg.TopNSelect_QSFixed;
import org.axt.perftest.util.GCMeasure;
import org.axt.perftest.util.GCMeasure.Diff;
import org.axt.perftest.util.Statistics;

public class TopNSelectTest {

	public static final int DEFAULT_WARMUP_RUNS 	= 30;
	public static final int DEFAULT_STATISTIC_RUNS 	= 50;

	public interface AlgProvider<T> {
		T get(int maxResults, int topResults);
	}
	
	
	public static class Config {
		private final AlgProvider<TopNSelectAlg> provider;
		private final int warmupRuns 	= DEFAULT_WARMUP_RUNS;
		private final int statisticRuns = DEFAULT_STATISTIC_RUNS;

		public Config(AlgProvider<TopNSelectAlg> provider) {
			this.provider = provider;
		}

		TopNSelectAlg getAlg(int maxResults, int topResults) throws IOException {
			TopNSelectAlg alg = provider.get(maxResults, topResults);
			return alg;
		}
	}

	private static int[] global_indexes;
	private static double[] global_scores;

	private static void testAlg(Config config, int maxResults, int topResults) throws Exception {

		double[] scores = createScores(maxResults);
		int[] indexes 	= createIndexes(maxResults);

		System.gc();
		
		for (int i = 0; i < config.warmupRuns; i++) {
			TopNSelectAlg alg = config.getAlg(maxResults, topResults);
			for (int j = 0; j < maxResults; j++) {
				alg.sink(indexes[j], scores[j]);
			}
			alg.getTopN(topResults);
		}

		Statistics statCreate = new Statistics();
		Statistics statAdd = new Statistics();
		Statistics statGetTop = new Statistics();

		Statistics statGCTime = new Statistics();
		Statistics statGCCount = new Statistics();
		Statistics statGCGarbageCreate = new Statistics();
		Statistics statGCGarbageTop = new Statistics();
		Statistics statGCGarbageAdd = new Statistics();

		// force garbage collection to start with empty eden space
		System.gc();
		GCMeasure gm = new GCMeasure();

		Diff diff;
		long before, after;
		for (int i = 0; i < config.statisticRuns; i++) {
			gm.clear();

			before = System.nanoTime();
			TopNSelectAlg alg = config.getAlg(maxResults, topResults);
			after = System.nanoTime();
		
			statCreate.add((after - before) / 1000000);
			diff = gm.getDiff();
			statGCGarbageCreate.add(diff.garbage);
			statGCTime.add(diff.gcTime);
			statGCCount.add(diff.gcCount);

			
			before = System.nanoTime();
			for (int j = 0; j < maxResults; j++) {
				alg.sink(indexes[j], scores[j]);
			}
			after = System.nanoTime();
			statAdd.add((after - before) / 1000000);

			diff = gm.getDiff();
			statGCGarbageAdd.add(diff.garbage);
			statGCTime.add(diff.gcTime);
			statGCCount.add(diff.gcCount);

			before = System.nanoTime();
			alg.getTopN(topResults);
			after = System.nanoTime();
			statGetTop.add((after - before) / 1000000);

			diff = gm.getDiff();
			statGCGarbageTop.add(diff.garbage);
			statGCTime.add(diff.gcTime);
			statGCCount.add(diff.gcCount);
		}

		System.out.println(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f", 
				statCreate.avg(), statAdd.avg(), statGetTop.avg(), 
				statGCTime.avg(), statGCCount.avg(), 
				statGCGarbageCreate.avg(), statGCGarbageAdd.avg(), statGCGarbageTop.avg()));
	}

	private static int[] createIndexes(int maxResults) {
		if (global_indexes == null) {
			int[] ret = new int[maxResults];
			Random r = new Random();
			for (int i = 0; i < ret.length; i++) {
				ret[i] = r.nextInt();
			}
			global_indexes = ret;
		}
		return Arrays.copyOf(global_indexes, maxResults);
	}

	private static double[] createScores(int maxResults) {
		if (global_scores == null) {
			double[] ret = new double[maxResults];
			Random r = new Random();
			for (int i = 0; i < ret.length; i++) {
				ret[i] = r.nextDouble();
			}
			global_scores = ret;
		}
		return Arrays.copyOf(global_scores, maxResults);
	}

	public static void main(String[] args) throws Exception {

		checkPreRequisites();
		
		int maxInput = 1_000_000;

		Config config = new Config(new AlgProvider<TopNSelectAlg>() {
			@Override
			public TopNSelectAlg get(int max, int top) {
				//return new TopNSelect_QS(ALG.RAND);
				//return new TopNSelect_QSFixed(ALG.MEDIAN, top);
				return new TopNSelect_QSFixed(ALG.MEDIAN, top, 10.0);
				//return new TopNSelect_PQ();
				//return new TopNSelect_PQNat(top);
			}
		});

		int step = maxInput / 25;

		for (int topResults = maxInput-step; topResults > 0; topResults -= step) {
			System.out.print(topResults + "\t");
			testAlg(config, topResults, 1000);
		}
	}

	/**
	 * Check if the JVM options are correct.  
	 */
	private static void checkPreRequisites() {
		RuntimeMXBean rmb = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArguments = rmb.getInputArguments();
		boolean tlab = false, sizepolicy = false, newsize = false, maxnewsize = false;

		for (String s : jvmArguments) {
			if ("-XX:-UseTLAB".equalsIgnoreCase(s)) tlab = true;
			if ("-XX:-UseAdaptiveSizePolicy".equalsIgnoreCase(s)) sizepolicy = true;
			if ("-XX:NewSize=1G".equalsIgnoreCase(s)) newsize = true;
			if ("-XX:MaxNewSize=1G".equalsIgnoreCase(s)) maxnewsize = true;
		}

		if (!(tlab && sizepolicy && newsize && maxnewsize)) {
			System.out.println("Use the following jvm options: -XX:-UseTLAB -XX:NewSize=1G -XX:MaxNewSize=1G -XX:-UseAdaptiveSizePolicy");
			System.exit(0);
		}
	}
}
