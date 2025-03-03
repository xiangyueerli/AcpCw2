# Getting Started

The following sections describe in some detail the needed steps to setup the environment and give explanations why and how things are configured

## Background information

To run our container we need to have access to redis, kafka, rabbitMQ - either as other containers or standalone (host or cloud). 

Any access the application performs is controlled via environment variables which are passed in when running the container. This can be either done using the command line with <code>docker run --network xxx -e yyy </code> or in intelliJ  using a GUI. 
On my system this looks like:

![intelliJ config.png](assets/intelliJ%20config.png)

For the different runtime environments I have different settings. The one above if for a docker environment. 
For a local process (which you will use most of your time) it looks like:

![local process environment.png](assets/local%20process%20environment.png)

**So what is done?**

- Port binding is setup for 8080 to 8080 in the container
- a specialized network is used to have the docker image running in the same network as kafka. This way addressing via the name is possible (like in <code>kafka:9093</code>).
- the mentioned environment variables are defined to specify which host / port to use

Should Kafka be run in a secured environment (thus, at confluent.io and not locally) three more variables have to be set:
- KAFKA_SECURITY_PROTOCOL (usually <code>SASL_SSL</code>)
- KAFKA_SASL_MECHANISM (usually <code>PLAIN</code>)
- KAFKA_SASL_JAAS_CONFIG (usually <code>org.apache.kafka.common.security.plain.PlainLoginModule required username='....' password='...';</code>)

_The value for the JAAS config (only username / password are needed and the server must be adjusted) can be retrieved from your subscription._

**If either of these is set, all of them must be set - otherwise the template generates an error**

### Local development
For local development the easiest configuration looks like: 

![acp_cw2_docker_landscape.png](assets/acp_cw2_docker_landscape.png)

Here, redis and rabbitMQ are running directly in docker, whereas kafka is running in is own network (for technical details why and how please refer to: https://www.confluent.io/en-gb/blog/kafka-client-cannot-connect-to-broker-on-aws-on-docker-etc/).

After the setup is done (see below), your docker dashboard will most likely look similar to this one (no mongo-db and postgre for now): 

![docker view.png](assets/docker%20view.png)


### Maven pom.xml and environment variables

As the template uses a bean to load the runtime environment and during the <code>package</code> run of maven the unit tests are run, this will require some environment variables to be set during maven operations as otherwise the tests will fail.

```Java
@Configuration
public class AppConfig {
    @Bean
    public RuntimeEnvironment CurrentRuntimeEnvironment() {
        return RuntimeEnvironment.getEnvironment();
    }
}
```

This is handled by specifying the needed environment variables in the pom.xml in the relevant section:

```Maven
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>

    <configuration>
        <systemPropertyVariables>
            <REDIS_HOST>localhost</REDIS_HOST>
            <REDIS_PORT>6379</REDIS_PORT>
            <RABBITMQ_HOST>localhost</RABBITMQ_HOST>
            <RABBITMQ_PORT>5672</RABBITMQ_PORT>
            <KAFKA_BOOTSTRAP_SERVERS>localhost:9092</KAFKA_BOOTSTRAP_SERVERS>
        </systemPropertyVariables>
    </configuration>
</plugin>
```


## Setting up the environment

The setup for the tutorials and CW2 is a bit complicated and needs attention. There are plenty of alternative approaches possible, the one shown below is one that works and is thus suggested. 
Please feel free to use your own approach...

**What do you need?** 

- docker installation (assumed to be present)
- rabbitMQ
- redis
- kafka
- kafka tools

### Installing redis

This will install redis as well as the management console (port 8001) 

`docker run -d --name redis -p 6379:6379 -p 8001:8001 redis/redis-stack:latest`

### Testing up to now... (redis)

### Installing rabbitMQ

This will install rabbitMQ as well as the management console (port 15672)

`docker run -it --rm -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.0-management`





**Installing kafka**



### Reference Documentation
For further reference, please consider the following sections:
* [Official Maven Documentation](${mavenDocs})
* [Spring Boot Maven Plugin Reference Guide](${springBootMavenPlugin}/reference/html/)
* [Create an OCI Image](${springBootMavenPlugin}/reference/html/#build-image)
* [Spring Web](${springBootDocs}/#web)

### Guides
The following guides illustrate how to use some features concretely:
* [Building a RESTful Web Service](${springGuides}/rest-service/)
* [Serving Web Content with Spring MVC](${springGuides}/serving-web-content/)
* [Building REST Services with Spring](${springGuides}/tutorials/rest/)

---

<!-- Variables for Easy Updates -->
[mavenDocs]: https://maven.apache.org/guides/index.html
[springBootMavenPlugin]: https://docs.spring.io/spring-boot/docs/3.2.2/maven-plugin
[springBootDocs]: https://docs.spring.io/spring-boot/docs/3.2.2/reference/htmlsingle/index.html
[springGuides]: https://spring.io/guides