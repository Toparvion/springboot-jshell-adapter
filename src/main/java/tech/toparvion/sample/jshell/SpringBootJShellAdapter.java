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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static java.lang.String.format;

/**
 * An adapter for JShell to run against Spring Boot packaged classpath.<p/>
 * The program does the following: <ol>
 *   <li>Locates given Spring Boot JAR/WAR file and checks it for reading</li>
 *   <li>Extracts its {@code BOOT-INF} directory content into temporary directory ({@code java.io.tmp})</li>
 *   <li>Composes a string of all paths to all extracted jars and a path to {@code classes} dir</li>
 *   <li>Launches JShell with the composed string as {@code --class-path} option</li>
 *   <li>After JShell exit, deletes temporary directory and exits as well</li>
 * </ol>
 * 
 * @author Toparvion
 * @see <a href="https://github.com/Toparvion/springboot-jshell-adapter">Readme on GitHub</a>
 */
public class SpringBootJShellAdapter {
  private static final String BOOT_INF_DIR_NAME = "BOOT-INF/";

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1) {
      System.out.println("Usage: $ jshellw <path/to/spring-boot-app.jar>");
      System.exit(1);
    }
    Path bootInfPath = extractClasspathFiles(args[0]);
    String jshellClasspathString = composeClasspathString(bootInfPath);
    launchJShell(jshellClasspathString);
  }

  private static Path extractClasspathFiles(String fatJarPath) throws IOException {
    var appJarPath = Paths.get(fatJarPath);
    if (!Files.isReadable(appJarPath)) {
      throw new IllegalArgumentException(format("File '%s' cannot be read. Check its path and permissions.", fatJarPath));
    }
    Path extractRoot = Files.createTempDirectory("springboot-jshell-adapter-");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> deletePathRecursively(extractRoot)));
    System.out.printf("Created temp directory '%s'. Extracting BOOT-INF content...\n", extractRoot);
    try (InputStream fis = Files.newInputStream(appJarPath)) {
      JarInputStream jis = new JarInputStream(fis);
      JarEntry nextEntry;
      while ((nextEntry = jis.getNextJarEntry()) != null) {
        String jarFilePath = nextEntry.getName();
        var isPathAcceptable = jarFilePath.startsWith(BOOT_INF_DIR_NAME);
        if (!isPathAcceptable) {
          continue;
        }
        // System.out.printf("Processing JAR entry: %s\n", jarFilePath);
        Path nextEntryPath = extractRoot.resolve(jarFilePath);
        if (nextEntry.isDirectory()) {
          Files.createDirectories(nextEntryPath);

        } else {
          Files.createDirectories(nextEntryPath.getParent());
          OutputStream nextFileOutStream = Files.newOutputStream(nextEntryPath);
          jis.transferTo(nextFileOutStream);
          nextFileOutStream.close();
        }
      }
      jis.closeEntry();
    }
    Path bootInfPath = extractRoot.resolve(BOOT_INF_DIR_NAME);
    System.out.printf("Extracted BOOT-INF content to '%s'.\n", bootInfPath);
    return bootInfPath;
  }

  private static String composeClasspathString(Path extractRoot) throws IOException {
    var classesPath = extractRoot.resolve("classes/").toAbsolutePath().toString();
    var libDirPath = extractRoot.resolve("lib/");
    var pathSeparator = System.getProperty("path.separator");
    var classPathAccumulator = new StringBuilder(classesPath);
    var cpEntryCount = 1; // including libDirPath
    try (DirectoryStream<Path> jarList = Files.newDirectoryStream(libDirPath)) {
      for (Path jarPath: jarList) {
        if (Files.isDirectory(jarPath)) {
          continue;
        }
        classPathAccumulator.append(pathSeparator)
                            .append(jarPath.toAbsolutePath().toString());
        cpEntryCount++;
      }
    }
    String jshellClassPath = classPathAccumulator.toString();
    System.out.printf("JShell --class-path option composed: %d bytes, %d entries, '%s'-separated\n",
        classPathAccumulator.length(), cpEntryCount, pathSeparator);
    return jshellClassPath;
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
    System.out.printf("Starting JShell with '%s'...\n", jshellPath);
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
