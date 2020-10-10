# Usage
Package the agent jar file.
```
mvn package
```

Find the packaged jar file in target directory and add it to the java command line options.
For example.
```bash
java -javaagent:/Users/praveen.ravi/code/java-debug-agent/target/javaagent-0.1.0-SNAPSHOT-jar-with-dependencies.jar Test
```

Most processing happens in `com.praveen.javaagent.AgentMain`, you can change it and add your own logic.
