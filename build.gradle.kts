plugins {
    java
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "dev.d1s"
version = "0.2.0-beta.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

val liquibaseVersion: String by project
val starterAdviceVersion: String by project
val starterSimpleSecurityVersion: String by project
val longPollingStarterVersion: String by project
val teabagsVersion: String by project
val tikaVersion: String by project
val jnCryptorVersion: String by project
val jetbrainsAnnotationsVersion: String by project
val apacheCommonsCodecVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.mariadb.jdbc:mariadb-java-client")
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("dev.d1s:spring-boot-starter-advice:$starterAdviceVersion")
    implementation("dev.d1s:spring-boot-starter-simple-security:$starterSimpleSecurityVersion")
    implementation("dev.d1s.long-polling:spring-boot-starter-lp-server-web:$longPollingStarterVersion")
    implementation("dev.d1s.teabags:teabag-spring-web:$teabagsVersion")
    implementation("dev.d1s.teabags:teabag-dto:$teabagsVersion")
    implementation("dev.d1s.teabags:teabag-stdlib:$teabagsVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("org.cryptonode.jncryptor:jncryptor:$jnCryptorVersion")
    implementation("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    implementation("commons-codec:commons-codec:$apacheCommonsCodecVersion")
    implementation(kotlin("stdlib")) // for backwards compatibility
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

configurations {
    implementation {
        exclude(
            "org.springframework.boot",
            "spring-boot-starter-tomcat"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events.addAll(
            setOf(
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
            )
        )
    }
}

sourceSets.getByName("test") {
    java.srcDir("./test")
}