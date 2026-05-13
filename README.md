# InkWell
Blogging platform  with Authentication and Admin Panel

## SonarQube Code Quality Setup

This repository includes independent SonarQube analysis configuration for each Spring Boot microservice.

Start a local SonarQube Community Edition instance:

```sh
docker compose -f docker-compose-sonarqube.yml up -d
```

Open `http://localhost:9000`, sign in, and generate a user token from the SonarQube UI under your account security settings. Do not commit this token.

Set the scan environment variables.

Windows Command Prompt:

```bat
set SONAR_TOKEN=your_token
set SONAR_HOST_URL=http://localhost:9000
```

PowerShell:

```powershell
$env:SONAR_TOKEN="your_token"
$env:SONAR_HOST_URL="http://localhost:9000"
```

Linux/Mac:

```sh
export SONAR_TOKEN=your_token
export SONAR_HOST_URL=http://localhost:9000
```

Run all service scans from the repository root:

```bat
run-sonar-all.bat
```

```sh
chmod +x run-sonar-all.sh
./run-sonar-all.sh
```

Each scan runs `mvn verify sonar:sonar` inside the service directory. JaCoCo XML coverage is generated at `target/site/jacoco/jacoco.xml` and passed to SonarQube through each service POM.

If you need a clean build first, set `MAVEN_GOALS` before running the script:

```powershell
$env:MAVEN_GOALS="clean verify sonar:sonar"
```

```sh
export MAVEN_GOALS="clean verify sonar:sonar"
```
