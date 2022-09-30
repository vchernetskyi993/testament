plugins {
    application
    kotlin("jvm") version "1.7.20"
}

group = "com.example"
version = "0.1.0"
description = "ethereum-node"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.example.AppKt")
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.44.0")
    implementation("software.constructs:constructs:10.1.116")
    implementation("software.amazon.awscdk:apigatewayv2:1.175.0")
}
