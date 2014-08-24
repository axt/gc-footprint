package org.axt.perftest.util;

/**
 * Simple statistics collector class which implements:
 * <ul>
 * <li>average</li>
 * <li>standard deviation</li>
 * <li>minimum</li>
 * <li>maximum</li>
 * <li>count</li>
 * </ul>
 * 
 * @author axt
 */
public class Statistics {

	private double sumx = 0.0;
	private double sumxx = 0.0;
	private double minx = 0.0;
	private double maxx = 0.0;
	private int n;

	public Statistics() {}

	public void add(double v) {
		if (n == 0) {
			maxx = minx = v;
		}
		
		n++;
		sumx += v;
		sumxx += v*v;
		
 		if (minx > v) minx = v;
 		if (maxx < v) maxx = v;
	}
	
	private void checkState() {
		if (n == 0) throw new IllegalStateException("n == 0");
	}
	
	public int count() {
		return n;
	}

	public double min() {
		checkState();
		return minx;
	}

	public double max() {
		checkState();
		return maxx;
	}

	public double avg() {
		checkState();
		return sumx/n;
	}

	public double var() {
		checkState();
		return Math.sqrt(sumxx - sumx*sumx);
	}
	
	@Override
	public String toString() {
		return String.format("%d\t%.4f\t%.4f\t%.4f\t%.4f", count(), avg(), var(), min(), max());
	}
}
