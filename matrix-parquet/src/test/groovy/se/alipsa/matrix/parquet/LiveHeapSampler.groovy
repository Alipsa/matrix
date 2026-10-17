package se.alipsa.matrix.parquet

import java.lang.management.ManagementFactory

/** Test-only maximum live-heap sampler using post-GC notifications. */
class LiveHeapSampler {
  final java.util.concurrent.atomic.AtomicLong peak = new java.util.concurrent.atomic.AtomicLong()
  private final List<javax.management.NotificationEmitter> emitters = []
  private final Set<String> heapPools = ManagementFactory.memoryPoolMXBeans.findAll { it.type == java.lang.management.MemoryType.HEAP }*.name.toSet()
  private final Map<String, Long> initialCounts = [:]
  private final java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1)
  private volatile Map<String, Long> finalCounts
  private final javax.management.NotificationListener listener = { notification, handback ->
    if (notification.type == com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION) {
      def event = com.sun.management.GarbageCollectionNotificationInfo.from((javax.management.openmbean.CompositeData) notification.userData)
      def info = event.gcInfo
      if (info.id > (initialCounts[event.gcName] ?: 0L)) {
        long used = info.memoryUsageAfterGc.findAll { String pool, usage -> heapPools.contains(pool) }.values().sum { it.used } as long
        peak.accumulateAndGet(used, { long a, long b -> Math.max(a, b) } as java.util.function.LongBinaryOperator)
        Map<String, Long> target = finalCounts
        if (target != null && info.id > (target[event.gcName] ?: 0L)) { finished.countDown() }
      }
    }
  } as javax.management.NotificationListener

  LiveHeapSampler() {
    ManagementFactory.garbageCollectorMXBeans.each { collector ->
      if (collector instanceof javax.management.NotificationEmitter) {
        initialCounts[collector.name] = collector.collectionCount
        collector.addNotificationListener(listener, null, null)
        emitters.add(collector)
      }
    }
  }

  @SuppressWarnings('ExplicitGarbageCollection')
  void finish() {
    finalCounts = ManagementFactory.garbageCollectorMXBeans.collectEntries { [(it.name): it.collectionCount] }
    System.gc()
    assert finished.await(5, java.util.concurrent.TimeUnit.SECONDS) : 'Final GC notification was not observed'
    emitters.each { it.removeNotificationListener(listener) }
  }
}
