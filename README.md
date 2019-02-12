# SpringBoot JShell Adapter
A simple script to launch [JShell](http://openjdk.java.net/jeps/222) against Spring Boot packaged classpath.  

The script can be useful to quickly prototype on non-development environments (like Docker containers) where environment difference matters (e.g. OS features, file system structure, network policy restrictions, etc).

## Usage
### Deployment
0. Make sure you have Java 11+ installed on target environment
1. Copy [`jshellw`](https://github.com/Toparvion/springboot-jshell-adapter/blob/master/jshellw) wrapper script into destination, e.g.
```bash
$ sudo docker cp jshellw mycontainer:/microservice/jshellw
```
2. Make the script executable
```bash
$ sudo docker exec -w /microservice mycontainer chmod +x jshellw
```
3. Run the script pointing to Spring Boot archive 
```bash
sudo docker exec -it -w /microservice mycontainer ./jshellw app.jar
```
The output should look like:
```text
Created temp directory '/tmp/springboot-jshell-adapter-5697341775544310278'. Extracting BOOT-INF content...
Extracted BOOT-INF content to '/tmp/springboot-jshell-adapter-5697341775544310278/BOOT-INF'.
JShell --class-path option composed: 6937 bytes, 74 entries, ':'-separated
Starting JShell with '/usr/lib/jvm/java-11-openjdk-amd64/bin/jshell'...
|  Welcome to JShell -- Version 11.0.1
|  For an introduction type: /help intro

jshell>
```
### Checking
To check if classpath has been composed and applied correctly, type `/env` and you should see something like:
```text
jshell> /env
|     --class-path /tmp/springboot-jshell-adapter-5697341775544310278/BOOT-INF/classes:/tmp/springboot-jshell-adapter-5697341775544310278/BOOT-INF/lib/HdrHistogram-2.1.9.jar:...<other-jars>...
```
### Working
Now you can import any classes from your classpath and work with them in JShell like in your dev environment.  
For example:
```
jshell> import org.springframework.util.StringUtils
jshell> var cleanedPath = StringUtils.cleanPath(".\\..\\core/inst/meg.dump")
cleanedPath ==> "../core/inst/meg.dump"
|  created variable cleanedPath : String
```
