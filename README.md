jxe
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.jxe/com.io7m.jxe.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.jxe%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.jxe/com.io7m.jxe?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/jxe/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/jxe.svg?style=flat-square)](https://codecov.io/gh/io7m-com/jxe)
![Java Version](https://img.shields.io/badge/21-java?label=java&color=007fff)

![com.io7m.jxe](./src/site/resources/jxe.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/jxe/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/jxe/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/jxe/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/jxe/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/jxe/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/jxe/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/jxe/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/jxe/actions?query=workflow%3Amain.windows.temurin.lts)|

## jxe

The jxe package implements a set of classes intended to both provide more
secure defaults and to eliminate much of the boilerplate required to set up
the standard JDK SAX parsers.

### Features

  * Hardened SAX parsers: Prevent path traversal attacks, prevent entity
    expansion attacks, prevent network access!
  * Dispatching XSD schema resolvers; XML documents specify namespaces and the
    resolvers find their respective XSD schemas from a provided whitelist of
    locations. Reject non-validated XML!
  * Written in pure Java 17.
  * [OSGi](https://www.osgi.org/) ready.
  * [JPMS](https://en.wikipedia.org/wiki/Java_Platform_Module_System) ready.
  * ISC license.
  * High-coverage automated test suite.

### Motivation

The `jxe` package provides a sane API for setting up secure-by-default
validating SAX parsers that dynamically locate schemas for incoming documents
from a whitelisted set of locations without those documents knowing or caring
where those schemas are actually located.

The package is capable of setting up extremely strict validating parsers.
For example, many applications that receive XML have the following requirements
on incoming data:

  * XML documents _must_ be validated against one of a small set of schemas.
    Data that has not been validated _must_ be rejected.
  * Documents _must_ declare the namespace to which their data belongs, but
    _must not_ be required to actually state the physical location of the
    schema. This is security sensitive: A document should not be able to tell
    a parser where to find a schema, because hostile documents could cause
    the parser to read a schema that trivially accepts all data.
    This would allow the document to essentially pass through without
    having to conform to the structure that an application expects.
    Documents that do not declare a namespace must be rejected.
  * The XML parser _must not_ access the network except to explicitly permitted
    locations. This is security sensitive: A hostile document could declare a
    dependency on a schema that could cause the parser to contact
    attacker-controlled servers.
  * The XML parser _must_ be robust in the face of attacks such as entity
    expansion attacks.
  * The XML parser _must_ prevent path traversal attacks: Documents must not be
    able to cause files outside of a particular directory to be accessed.

### Usage

The `jxe` package allows applications to enforce all of the above requirements
via a very simple API:

```
// Incoming documents *must* be in the "urn:com.io7m.example:simple:1:0" namespace

URI schema_namespace =
  URI.create("urn:com.io7m.example:simple:1:0");

// When a document states that it is in the "urn:com.io7m.example:simple:1:0" namespace,
// the parser will open the schema at the URL returned by getResource("simple_1_0.xsd").
// All other namespaces will be rejected.

JXESchemaDefinition schema =
  JXESchemaDefinition.of(
    schema_namespace, "simple_1_0.xsd", Example.class.getResource("simple_1_0.xsd")));

// Declare an immutable map of schemas. In this example, there is only the
// one schema declared above.

final JXESchemaResolutionMappings schemas =
  JXESchemaResolutionMappings.builder()
    .putMappings(schema_namespace, schema)
    .build();

// Create a provider of hardened SAX parsers.

JXEHardenedSAXParsers parsers =
  new JXEHardenedSAXParsers();

// Specify a directory containing documents. The parser will not be allowed
// to access paths that are ancestors of the given directory. This prevents
// path traversal attacks such as trying to xinclude "../../../../etc/passwd".

Path document_directory = ...;

// Create an XInclude aware SAX parser.

XMLReader reader =
  parsers.createXMLReader(
    document_directory, JXEXInclude.XINCLUDE_ENABLED, schemas);

// Use the SAX parser.

reader.setContentHandler(...);
reader.parse(...);
```

