# SpringBoot JShell Adapter
A simple script to launch [JShell](http://openjdk.java.net/jeps/222) against Spring Boot packaged classpath.  

The script can be useful to quickly prototype on non-development environments (like Docker containers) where environment difference matters (e.g. OS features, file system structure, network policy restrictions, etc).

## Usage
### Deploy

0. Make sure you have Java 11+ installed on target environment

1. Copy [`jshellw`](https://github.com/Toparvion/springboot-jshell-adapter/blob/master/jshellw) wrapper script into destination, e.g.
```bash
$ sudo docker cp jshellw mycontainer:/microservice/jshellw
```

2. Make the script executable
```bash
$ sudo docker exec -w /microservice mycontainer chmod +x jshellw
```

3. Run the script pointing to Spring Boot JAR or WAR archive 
```bash
$ sudo docker exec -it -w /microservice mycontainer ./jshellw app.jar
```

The output should look like:
```text
Created temp directory '/tmp/...'. Extracting classpath content...
Extracted 191 files from the archive to '/tmp/.../BOOT-INF'.
Starting JShell with '/usr/lib/jvm/java-11-openjdk-amd64/bin/jshell --feedback verbose --class-path /tmp/.../BOOT-INF/classes:/tmp/.../BOOT-INF/lib/*'...
|  Welcome to JShell -- Version 11.0.1
|  For an introduction type: /help intro

jshell>
```
##### In case of Windows
To run the script in Windows just execute the following instead of
steps 2 and 3:
```
java --source 11 jshellw app.jar
```

### Check
To check if classpath has been composed and applied correctly, type `/env` and you should see something like:
```text
jshell> /env
|     --class-path /tmp/.../BOOT-INF/classes:/tmp/.../BOOT-INF/lib/HdrHistogram-2.1.9.jar:...<other-jars>...
```

### Play
Now you can import any classes from your classpath and work with them in JShell like in your dev environment.  
For example:
```
jshell> import org.springframework.util.StringUtils
jshell> var cleanedPath = StringUtils.cleanPath(".\\..\\core/inst/meg.dump")
cleanedPath ==> "../core/inst/meg.dump"
|  created variable cleanedPath : String
```
#### Need help on using JShell?
See this [Comprehensive Guide](https://www.infoq.com/articles/jshell-java-repl) or google for `jara repl`.
