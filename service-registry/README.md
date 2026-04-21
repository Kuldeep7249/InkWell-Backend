# InkWell Service Registry

This project is the Eureka discovery server for the InkWell microservices system.

## Port
- `8761`

## Run
```bash
mvn spring-boot:run
```

## Eureka Dashboard
- `http://localhost:8761`

## Add to every microservice
Add this dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

Add this dependency management block if not already present:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2025.1.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Add these properties in each service:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
management.endpoints.web.exposure.include=health,info
```

## Suggested service names
- auth-service
- post-service
- comment-service
- category-service
- media-service
- newsletter-service
- notification-service
- inkwell-web

## Example per-service name
```properties
spring.application.name=auth-service
```
