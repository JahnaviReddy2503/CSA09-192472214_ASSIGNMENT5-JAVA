import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class B1Benchmark {
    private static final int[] FILE_SIZES_MIB = {1, 10, 50};
    private static final int[] RECORD_COUNTS = {1_000, 10_000, 100_000};

    private B1Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("buffered_vs_unbuffered");
        System.out.println("sizeMiB,run,unbufferedMs,bufferedMs,unbufferedReadCalls,"
                + "bufferedReadCalls,lineCount,actualBytes");
        for (int sizeMiB : FILE_SIZES_MIB) {
            Path file = Files.createTempFile("b1-" + sizeMiB + "MiB-", ".log");
            try {
                writeLog(file, sizeMiB * 1024L * 1024L);
                LineCounter.countUnbufferedWithMetrics(file);
                LineCounter.countBufferedWithMetrics(file);
                long[] unbuffered = measure(file, false);
                long[] buffered = measure(file, true);
                LineCounter.Result unbufferedResult =
                        LineCounter.countUnbufferedWithMetrics(file);
                LineCounter.Result bufferedResult =
                        LineCounter.countBufferedWithMetrics(file);
                if (unbufferedResult.lines() != bufferedResult.lines()
                        || Files.size(file) != sizeMiB * 1024L * 1024L) {
                    throw new IllegalStateException("Benchmark inputs or results do not match");
                }
                for (int i = 0; i < unbuffered.length; i++) {
                    System.out.printf(Locale.ROOT, "%d,%s,%.3f,%.3f,%d,%d,%d,%d%n",
                            sizeMiB, i + 1, unbuffered[i] / 1_000_000.0,
                            buffered[i] / 1_000_000.0,
                            unbufferedResult.readCalls(), bufferedResult.readCalls(),
                            unbufferedResult.lines(), Files.size(file));
                }
                System.out.printf(Locale.ROOT, "%d,AVERAGE,%.3f,%.3f,%d,%d,%d,%d%n",
                        sizeMiB, averageMs(unbuffered), averageMs(buffered),
                        unbufferedResult.readCalls(), bufferedResult.readCalls(),
                        unbufferedResult.lines(), Files.size(file));
            } finally {
                Files.deleteIfExists(file);
            }
        }

        System.out.println("search_growth");
        System.out.println("records,run,findAtEndMs,findMissingMs,foundAtEnd,missingIsNull");
        for (int recordCount : RECORD_COUNTS) {
            Path file = Files.createTempFile("b1-search-", ".csv");
            try {
                writePasses(file, recordCount);
                PassStore store = new PassStore(file);
                String wanted = "P" + (recordCount - 1);
                store.find(wanted);
                store.find("NOT_PRESENT");
                long[] foundTimes = new long[3];
                long[] missingTimes = new long[3];
                for (int i = 0; i < 3; i++) {
                    long startFound = System.nanoTime();
                    String[] result = store.find(wanted);
                    long foundNanos = System.nanoTime() - startFound;
                    long startMissing = System.nanoTime();
                    String[] missing = store.find("NOT_PRESENT");
                    long missingNanos = System.nanoTime() - startMissing;
                    if (result == null || missing != null) {
                        throw new IllegalStateException("Expected record was not found");
                    }
                    foundTimes[i] = foundNanos;
                    missingTimes[i] = missingNanos;
                    System.out.printf(Locale.ROOT, "%d,%d,%.3f,%.3f,true,true%n",
                            recordCount, i + 1, foundNanos / 1_000_000.0,
                            missingNanos / 1_000_000.0);
                }
                System.out.printf(Locale.ROOT, "%d,AVERAGE,%.3f,%.3f,true,true%n",
                        recordCount, averageMs(foundTimes), averageMs(missingTimes));
            } finally {
                Files.deleteIfExists(file);
            }
        }

        System.out.println("map_vs_scan");
        System.out.println("records,queries,scanTotalMs,mapBuildMs,mapSearchTotalMs,"
                + "approxMemoryBytes");
        for (int recordCount : RECORD_COUNTS) {
            Path file = Files.createTempFile("b1-map-", ".csv");
            try {
                writePasses(file, recordCount);
                String wanted = "P" + (recordCount - 1);
                int queries = 100;
                PassStore scanStore = new PassStore(file);
                scanStore.find(wanted);
                scanStore.find("NOT_PRESENT");
                long scanStart = System.nanoTime();
                for (int i = 0; i < queries; i++) {
                    if (scanStore.find(wanted) == null) {
                        throw new IllegalStateException("Scan lookup failed");
                    }
                }
                long scanNanos = System.nanoTime() - scanStart;

                collectGarbage();
                long beforeBytes = usedMemory();
                long buildStart = System.nanoTime();
                PassStoreMap map = new PassStoreMap(file);
                long buildNanos = System.nanoTime() - buildStart;
                long memoryBytes = usedMemory() - beforeBytes;
                map.find(wanted);
                long mapStart = System.nanoTime();
                for (int i = 0; i < queries; i++) {
                    if (map.find(wanted) == null) {
                        throw new IllegalStateException("Map lookup failed");
                    }
                }
                long mapSearchNanos = System.nanoTime() - mapStart;
                System.out.printf(Locale.ROOT, "%d,%d,%.3f,%.3f,%.3f,%d%n",
                        recordCount, queries, scanNanos / 1_000_000.0,
                        buildNanos / 1_000_000.0, mapSearchNanos / 1_000_000.0,
                        memoryBytes);
            } finally {
                Files.deleteIfExists(file);
            }
        }
    }

    private static void writeLog(Path file, long targetBytes) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            String line = "attendance-record-000000000000000000000000000000000000000000\n";
            long lineBytes = line.getBytes(StandardCharsets.UTF_8).length;
            long fullLines = targetBytes / lineBytes;
            int remaining = (int) (targetBytes % lineBytes);
            for (long i = 0; i < fullLines; i++) {
                writer.write(line);
            }
            writer.write(line, 0, remaining);
        }
    }

    private static void writePasses(Path file, int count) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (int i = 0; i < count; i++) {
                writer.write("P" + i + ",R" + i + ",Student" + i + ",12,4500.0");
                writer.newLine();
            }
        }
    }

    private static long[] measure(Path file, boolean buffered) throws IOException {
        long[] times = new long[3];
        for (int i = 0; i < times.length; i++) {
            long start = System.nanoTime();
            if (buffered) {
                LineCounter.countBuffered(file);
            } else {
                LineCounter.countUnbuffered(file);
            }
            times[i] = System.nanoTime() - start;
        }
        return times;
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static double averageMs(long[] nanos) {
        long total = 0;
        for (long value : nanos) {
            total += value;
        }
        return total / (double) nanos.length / 1_000_000.0;
    }

    private static void collectGarbage() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while measuring approximate memory",
                    error);
        }
    }

}
