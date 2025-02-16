plugins {
    id("java")
    id("maven-publish")
}

group = "org.cneko"
version = "0.1.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("com.google.code.gson:gson:2.12.1")
    implementation("commons-io:commons-io:2.18.0")
    implementation("io.netty:netty-all:4.1.117.Final")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.2")
    implementation("org.slf4j:slf4j-api:2.0.16")


}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.property("group").toString()
            artifactId = "NekoAI"
            version = project.property("version").toString()
        }
    }
    repositories {
        mavenLocal()
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
