package org.axt.perftest.util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to measure the generated garbage between two calls.
 * It works only if there are no other threads generating garbage.
 * It works well only if you are using -XX:-UseTLAB, but you don't want to use that option in production (only in a test environment)
 */
public class GCMeasure {

	private final List<GarbageCollectorMXBean> gcAllMXBeans;
	private final List<GarbageCollectorMXBean> gcMinorCollectorMXBeans;

	public GCMeasure() {
		gcAllMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
		gcMinorCollectorMXBeans = getMinorCollectorBeans(gcAllMXBeans);
		clear();
	}
	
	private static boolean isMinorCollector(GarbageCollectorMXBean gcBean) {
		String n = gcBean.getName();
		return "Copy".equals(n) || "ParNew".equals(n) || "PS Scavenge".equals(n) || (n != null && n.toLowerCase().contains("young"));
	}

	private List<GarbageCollectorMXBean> getMinorCollectorBeans(List<GarbageCollectorMXBean> gcAllMXBeans) {
		List<GarbageCollectorMXBean> gcMinorCollectorMXBeans = new ArrayList<>(); 
		for(GarbageCollectorMXBean mxbean : gcAllMXBeans) {
			if (isMinorCollector(mxbean)) {
				gcMinorCollectorMXBeans.add(mxbean);
			}
		}
		return gcMinorCollectorMXBeans;
	}

	/**
	 * This class represents the garbage collector statistics between two points in the program execution.
	 */
	public static class Diff {
		public final long gcTime;
		public final long gcCount;
		public final double garbage;

		private Diff(Point p1, Point p2) {
			this.gcCount = p2.gcCount - p1.gcCount;
			this.gcTime = p2.gcTime - p1.gcTime;

			// sanity checks
			if (p2.edenSpaceCommittedSize == 0) {
				throw new IllegalStateException("problem: size of eden space is zero!");
			} else if (p1.oldSpaceCommittedSize != p2.oldSpaceCommittedSize || Math.abs(p1.oldSpaceUsedSize - p2.oldSpaceUsedSize) > 10_000) {
				throw new IllegalStateException("problem: size of old space was changed " + p1 + " " + p2);
			} else if (p1.edenSpaceCommittedSize != p2.edenSpaceCommittedSize) {
				throw new IllegalStateException("problem: size of eden space was changed");
			} else if (p1.edenSpaceUsedSize > p1.edenSpaceCommittedSize || p2.edenSpaceUsedSize > p2.edenSpaceCommittedSize || p1.edenSpaceUsedSize < 0 || p2.edenSpaceUsedSize < 0 || gcCount < 0) {
				throw new IllegalStateException("problem: invalid used/committed size of eden spaces, or minorGcCount is negative");
			} else {
				if (gcCount == 0) {
					// no garbage collection during the two point, simply check used space change
					this.garbage = (p2.edenSpaceUsedSize - p1.edenSpaceUsedSize) / 1024.0 / 1024.0;
				} else {
					// approximate generated garbage bases on number of garbage collections time committed size of eden space
					this.garbage = ((p1.edenSpaceCommittedSize - p1.edenSpaceUsedSize) + (gcCount - 1) * p1.edenSpaceCommittedSize + p2.edenSpaceUsedSize) / 1024.0 / 1024.0;
				}
			}
			if (this.garbage < 0) {
				throw new IllegalStateException("problem: calculated garbage is negative, internal error");
			}
		}
	}

	/**
	 * This class represents the garbage collector statistics at a given point of program execution.
	 */
	private class Point {
		final long gcTime;
		final long minorGcCount;
		final long gcCount;
		final long edenSpaceCommittedSize;
		final long edenSpaceUsedSize;
		final long oldSpaceCommittedSize;
		final long oldSpaceUsedSize;
	
		Point() {
			gcTime = getGcTime();
			minorGcCount = getMinorGCCount();
			gcCount = getGCCount();
			edenSpaceCommittedSize = getEdenSpaceCommittedSize();
			edenSpaceUsedSize = getEdenSpaceUsedSize();
			oldSpaceCommittedSize = getOldSpaceCommittedSize();
			oldSpaceUsedSize = getOldSpaceUsedSize();
		}

		@Override
		public String toString() {
			return "Point [gcTime=" + gcTime + ", minorGcCount=" + minorGcCount
					+ ", gcCount=" + gcCount + ", edenSpaceCommittedSize="
					+ edenSpaceCommittedSize + ", edenSpaceUsedSize="
					+ edenSpaceUsedSize + ", oldSpaceCommittedSize="
					+ oldSpaceCommittedSize + ", oldSpaceUsedSize="
					+ oldSpaceUsedSize + "]";
		}

		private long getGcTime() {
			long sum = 0;
			for (GarbageCollectorMXBean gcMBean : gcAllMXBeans) {
				sum += gcMBean.getCollectionTime();
			}
			return sum;
		}

		private long getMinorGCCount() {
			long sum = 0;
			for (GarbageCollectorMXBean gcBean : gcMinorCollectorMXBeans) {
				sum += gcBean.getCollectionCount();
			}
			return sum;
		}

		private long getGCCount() {
			long sum = 0;
			for (GarbageCollectorMXBean gcBean : gcAllMXBeans) {
				sum += gcBean.getCollectionCount();
			}
			return sum;
		}


		boolean isEden(MemoryPoolMXBean mpb) {
			return mpb != null && mpb.getName() != null && mpb.getName().toLowerCase().contains("eden");
		}

		boolean isOld(MemoryPoolMXBean mpb) {
			return mpb != null && mpb.getName() != null && mpb.getName().toLowerCase().contains("old");
		}

		public long getEdenSpaceCommittedSize() {
			long sumCommitted = 0;
			List<MemoryPoolMXBean> mpbs = ManagementFactory.getMemoryPoolMXBeans();
			for (MemoryPoolMXBean mpb : mpbs) {
				try {
					MemoryUsage mu = mpb.getCollectionUsage(); // memory usage after the last collection // can throw exception, see above
					if (isEden(mpb) && mu != null) sumCommitted += mu.getCommitted();
				} catch (RuntimeException ex) {
					// eating this exception -- we don't care, this method is used only by ProbaServlet
				}
			}
			return sumCommitted;
		}
		public long getEdenSpaceUsedSize() {
			long sumUsed = 0;
			List<MemoryPoolMXBean> mpbs = ManagementFactory.getMemoryPoolMXBeans();
			for (MemoryPoolMXBean mpb : mpbs) {
				MemoryUsage mu = mpb.getUsage();
				if (isEden(mpb) && mu != null) sumUsed += mu.getUsed();
			}
			return sumUsed;
		}

		public long getOldSpaceCommittedSize() {
			long sumCommitted = 0;
			List<MemoryPoolMXBean> mpbs = ManagementFactory.getMemoryPoolMXBeans();
			for (MemoryPoolMXBean mpb : mpbs) {
				MemoryUsage mu = mpb.getCollectionUsage(); // memory usage after the last collection // can throw exception, see above
				if (isOld(mpb) && mu != null) sumCommitted += mu.getCommitted();
			}
			return sumCommitted;
		}
		public long getOldSpaceUsedSize() {
			long sumUsed = 0;
			List<MemoryPoolMXBean> mpbs = ManagementFactory.getMemoryPoolMXBeans();
			for (MemoryPoolMXBean mpb : mpbs) {
				MemoryUsage mu = mpb.getUsage(); // memory usage after the last collection // can throw exception, see above
				if (isOld(mpb) && mu != null) sumUsed += mu.getUsed();
			}
			return sumUsed;
		}

	}
	
	Point lastPoint;
	
	/**
	 * @return true if the setting is fine for GC measurement
	 * Note that you should NEVER use -XX:-UseTLAB on a production environment. 
	 */
	public boolean checkTLAB() {
		RuntimeMXBean rmb = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArguments = rmb.getInputArguments();
		for (String s : jvmArguments) if ("-XX:-UseTLAB".equalsIgnoreCase(s)) return true; // FIXME: ez igy nem pontos, siman lehet, hogy valamelyik JVM-en nincs is ilyen beallitas, vagy defaultbol ki van kapcsolva. Nekunk most jo lesz.
		return false;
	}
	
	public void clear() {
		lastPoint = new Point();
	}

	public Diff getDiff() {
		Point pold = lastPoint;
		lastPoint = new Point();
		return new Diff(pold, lastPoint);
	}
}
