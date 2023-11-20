Some of xtrasonnet dependencies leverage Log4J directly, instead of utilizing the more standard SLF4J framework. In order to provide a standard logging experience, the xtrasonnet library includes the `log4j-to-slf4j` bridge in order to forward Log4J events to SLF4J. Applications can then bundle any SLF4J logging implementation to process the log events, such as `logback` for example.


## Using Log4J

If an application intends to use Log4J as the logging implementation, it is required to exclude the `log4j-to-slf4j` bridge as a transitive dependency of xtrasonnet. In Maven this is done as follows:

```xml
<dependency>
    <groupId>io.github.jam01</groupId>
    <artifactId>xtrasonnet</artifactId> <!-- or camel-xtrasonnet -->
    <version>0.6.1</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-to-slf4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
