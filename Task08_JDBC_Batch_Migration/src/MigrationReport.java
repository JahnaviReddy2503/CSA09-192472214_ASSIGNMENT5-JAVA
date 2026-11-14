import java.util.ArrayList;
import java.util.List;

public final class MigrationReport {
    private final int recordsRead;
    private final int validRecords;
    private final List<String> rejections;
    private int insertedRecords;
    private int failedRecords;
    private int notAttemptedRecords;
    private String databaseFailure;

    public MigrationReport(int recordsRead, int validRecords, List<String> rejections) {
        this.recordsRead = recordsRead;
        this.validRecords = validRecords;
        this.rejections = new ArrayList<>(rejections);
    }

    public void complete(int inserted, int failed) {
        insertedRecords = inserted;
        failedRecords = failed;
    }

    public void fail(String reason, int failed, int notAttempted) {
        databaseFailure = reason;
        failedRecords = failed;
        notAttemptedRecords = notAttempted;
        insertedRecords = 0;
    }

    @Override
    public String toString() {
        StringBuilder report = new StringBuilder()
                .append("recordsRead=").append(recordsRead).append(System.lineSeparator())
                .append("validRecords=").append(validRecords).append(System.lineSeparator())
                .append("rejectedRecords=").append(rejections.size()).append(System.lineSeparator())
                .append("insertedRecords=").append(insertedRecords).append(System.lineSeparator())
                .append("failedRecords=").append(failedRecords).append(System.lineSeparator())
                .append("notAttemptedRecords=").append(notAttemptedRecords)
                .append(System.lineSeparator());
        if (databaseFailure != null) {
            report.append("databaseFailure=").append(databaseFailure)
                    .append(System.lineSeparator());
        }
        for (String rejection : rejections) {
            report.append("rejected=").append(rejection).append(System.lineSeparator());
        }
        return report.toString();
    }
}
