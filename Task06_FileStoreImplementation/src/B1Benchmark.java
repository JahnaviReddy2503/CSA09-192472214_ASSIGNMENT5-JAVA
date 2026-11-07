import java.io.BufferedWriter;
import java.io.IOException;
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
        System.out.println("sizeMiB,unbufferedMedianMs,bufferedMedianMs,lineCount");
        for (int sizeMiB : FILE_SIZES_MIB) {
            Path file = Files.createTempFile("b1-" + sizeMiB + "MiB-", ".log");
            try {
                writeLog(file, sizeMiB * 1024L * 1024L);
                LineCounter.countBuffered(file);
                LineCounter.countUnbuffered(file);
                long[] unbuffered = measure(file, false);
                long[] buffered = measure(file, true);
                System.out.printf(Locale.ROOT, "%d,%d,%d,%d%n", sizeMiB,
                        median(unbuffered), median(buffered),
                        LineCounter.countBuffered(file));
            } finally {
                Files.deleteIfExists(file);
            }
        }

        System.out.println("search_growth");
        System.out.println("records,medianFindMs,found");
        for (int recordCount : RECORD_COUNTS) {
            Path file = Files.createTempFile("b1-search-", ".csv");
            try {
                writePasses(file, recordCount);
                PassStore store = new PassStore(file);
                String wanted = "P" + (recordCount - 1);
                store.find(wanted);
                long[] times = new long[3];
                for (int i = 0; i < times.length; i++) {
                    long start = System.nanoTime();
                    String[] result = store.find(wanted);
                    times[i] = (System.nanoTime() - start) / 1_000_000;
                    if (result == null) {
                        throw new IllegalStateException("Expected record was not found");
                    }
                }
                System.out.printf(Locale.ROOT, "%d,%d,true%n",
                        recordCount, median(times));
            } finally {
                Files.deleteIfExists(file);
            }
        }
    }

    private static void writeLog(Path file, long targetBytes) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            long written = 0;
            while (written < targetBytes) {
                String line = "attendance-record-000000000000000000000000000000000000000000\n";
                writer.write(line);
                written += line.getBytes().length;
            }
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
            times[i] = (System.nanoTime() - start) / 1_000_000;
        }
        return times;
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
}
