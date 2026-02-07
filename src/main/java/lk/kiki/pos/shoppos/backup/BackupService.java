package lk.kiki.pos.shoppos.backup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BackupService {

    public static Path backupSql(String mysqlBinPath, String user, String pass, String dbName) throws Exception {
        Files.createDirectories(Path.of("backups"));
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path out = Path.of("backups", dbName + "_" + ts + ".sql");

        String exe = Path.of(mysqlBinPath, "mysqldump.exe").toString();
        ProcessBuilder pb = new ProcessBuilder(
                exe,
                "-u" + user,
                "-p" + pass,
                dbName
        );
        pb.redirectOutput(out.toFile());
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) throw new RuntimeException("Backup failed. Exit code: " + code);

        return out;
    }
}
