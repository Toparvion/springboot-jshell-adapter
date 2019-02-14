package tech.toparvion.sample.jshell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

/**
 * An adapter for JShell to run against Spring Boot packaged classpath.<p/>
 * The program does the following: <ol>
 *   <li>Locates given Spring Boot JAR/WAR file and checks it for reading</li>
 *   <li>Extracts its {@code BOOT-INF} or {@code WEB-INF} directory content into temporary directory ({@code java.io.tmp})</li>
 *   <li>Composes a string for passing to JShell as {@code --class-path} option</li>
 *   <li>Launches JShell with the composed option</li>
 *   <li>After JShell's exit, deletes temporary directory and exits as well</li>
 * </ol>
 * 
 * @author Toparvion
 * @see <a href="https://github.com/Toparvion/springboot-jshell-adapter">Readme on GitHub</a>
 */
public class SpringBootJShellAdapter {
  private static final String BOOT_INF_DIR_NAME = "BOOT-INF/";
  private static final String WEB_INF_DIR_NAME = "WEB-INF/";

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1) {
      System.out.println("Usage: $ jshellw <path/to/spring-boot-app.jar>");
      System.exit(1);
    }
    Path extractDirPath = extractClasspathFiles(args[0]);
    String jshellClasspathString = composeClasspathString(extractDirPath);
    launchJShell(jshellClasspathString);
  }

  private static Path extractClasspathFiles(String appArchivePathString) throws IOException {
    var appArchivePath = Paths.get(appArchivePathString);
    if (!Files.isReadable(appArchivePath)) {
      throw new IllegalArgumentException(format("File '%s' cannot be read. Check its path and permissions.", appArchivePathString));
    }
    Path extractRoot = Files.createTempDirectory("springboot-jshell-adapter-");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> deletePathRecursively(extractRoot)));
    System.out.printf("Created temp directory '%s'. Extracting classpath content...\n", extractRoot);
    var filesCount = 0;
    try (InputStream fis = Files.newInputStream(appArchivePath)) {
      ZipInputStream zis = new ZipInputStream(fis);
      ZipEntry nextEntry;
      while ((nextEntry = zis.getNextEntry()) != null) {
        String archivedEntryPath = nextEntry.getName();
        var isPathAcceptable = archivedEntryPath.startsWith(BOOT_INF_DIR_NAME) 
                            || archivedEntryPath.startsWith(WEB_INF_DIR_NAME);
        if (!isPathAcceptable) {
          continue;
        } 
        // System.out.printf("Processing archive entry: %s\n", archivedEntryPath);
        Path extractedEntryPath = extractRoot.resolve(archivedEntryPath);
        if (nextEntry.isDirectory()) {
          Files.createDirectories(extractedEntryPath);

        } else {
          OutputStream nextFileOutStream = Files.newOutputStream(extractedEntryPath);
          zis.transferTo(nextFileOutStream);
          nextFileOutStream.close();
          filesCount++;
        }
      }
      zis.closeEntry();
    }
    Path infPath;
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(extractRoot)) {
      infPath = dirStream.iterator().next().toAbsolutePath();     // it's enough to take the very first path only
    }
    System.out.printf("Extracted %d files from the archive to '%s'.\n", filesCount, infPath);
    return infPath;
  }

  private static String composeClasspathString(Path infPath) {
    return infPath.resolve("classes/").toAbsolutePath().toString() +
           System.getProperty("path.separator") +
           infPath.resolve("lib/").toAbsolutePath().toString() +
           System.getProperty("file.separator") +
           '*';
  }

  private static void launchJShell(String jshellClassPath) throws InterruptedException, IOException {
    var jshellExecutable = System.getProperty("os.name").toLowerCase().startsWith("windows")
        ? "jshell.exe"
        : "jshell";
    var jshellPath = Paths.get(System.getProperty("java.home"))
        .resolve("bin")
        .resolve(jshellExecutable)
        .toAbsolutePath()
        .toString();
    var jshellArgs = List.of(
        jshellPath,
        "--feedback", "verbose",
        "--class-path", jshellClassPath
    );
    ProcessBuilder jshellLauncher = new ProcessBuilder(jshellArgs);
    System.out.printf("Starting JShell with '%s'...\n", String.join(" ", jshellArgs));
    jshellLauncher.inheritIO();
    int jshellExitCode = jshellLauncher.start().waitFor();
    System.out.printf("JShell exited with code %d.\n", jshellExitCode);
  }

  private static void deletePathRecursively(Path path2delete) {
    System.out.printf("Deleting temp directory '%s'...\n", path2delete);
    try {
      //noinspection ResultOfMethodCallIgnored
      Files.walk(path2delete)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
      System.out.println("Temp directory deleted.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
