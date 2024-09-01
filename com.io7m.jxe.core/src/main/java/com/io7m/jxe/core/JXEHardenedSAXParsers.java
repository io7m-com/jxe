/*
 * Copyright © 2018 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jxe.core;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A provider of hardened SAX parsers.
 */

public final class JXEHardenedSAXParsers
{
  private final Supplier<SAXParserFactory> parsers;

  /**
   * Construct a provider.
   */

  public JXEHardenedSAXParsers()
  {
    this(
      SAXParserFactory::newNSInstance
    );
  }

  /**
   * Construct a provider.
   *
   * @param inParsers The factory of SAX parsers to use
   */

  public JXEHardenedSAXParsers(
    final Supplier<SAXParserFactory> inParsers)
  {
    this.parsers =
      Objects.requireNonNull(inParsers, "inParsers");
  }

  /**
   * Create a non-validating XML reader.
   *
   * @param baseDirectory A directory that will contain parsed resources, if any
   * @param xinclude       A specification of whether XInclude should be enabled
   *
   * @return A new non-validating XML reader
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws SAXException                 On SAX parser errors
   */

  public XMLReader createXMLReaderNonValidating(
    final Optional<Path> baseDirectory,
    final JXEXInclude xinclude)
    throws ParserConfigurationException, SAXException
  {
    Objects.requireNonNull(baseDirectory, "Base directory");
    Objects.requireNonNull(xinclude, "xinclude");

    final var parser =
      this.parsers.get().newSAXParser();

    final var className =
      parser.getClass().getCanonicalName();

    if (className.startsWith("org.apache.xerces.jaxp.")) {
      return createXMLReaderNonValidatingXerces(
        baseDirectory,
        xinclude,
        parser
      );
    }

    return createXMLReaderNonValidatingJDK(baseDirectory, xinclude, parser);
  }

  private static XMLReader createXMLReaderNonValidatingXerces(
    final Optional<Path> baseDirectory,
    final JXEXInclude xinclude,
    final SAXParser parser)
    throws SAXException
  {
    final var reader =
      parser.getXMLReader();

    /*
     * Turn on "secure processing". Sets various resource limits to prevent
     * various denial of service attacks.
     */

    reader.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING,
      true
    );

    /*
     * Don't load DTDs at all.
     */

    reader.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false
    );

    /*
     * Enable XInclude.
     */

    reader.setFeature(
      "http://apache.org/xml/features/xinclude",
      xinclude == JXEXInclude.XINCLUDE_ENABLED
    );

    /*
     * Ensure namespace processing is enabled.
     */

    reader.setFeature(
      "http://xml.org/sax/features/namespaces",
      true
    );

    /*
     * Disable validation.
     */

    reader.setFeature(
      "http://xml.org/sax/features/validation",
      false
    );

    reader.setFeature(
      "http://apache.org/xml/features/validation/schema",
      false
    );

    /*
     * Tell the parser to use the full EntityResolver2 interface (by default,
     * the extra EntityResolver2 methods will not be called - only those of
     * the original EntityResolver interface would be called).
     */

    reader.setFeature(
      "http://xml.org/sax/features/use-entity-resolver2",
      true
    );

    reader.setEntityResolver(
      JXEHardenedDispatchingResolver.create(
        baseDirectory, JXESchemaResolutionMappings.builder().build()));
    return reader;
  }

  private static XMLReader createXMLReaderNonValidatingJDK(
    final Optional<Path> baseDirectory,
    final JXEXInclude xinclude,
    final SAXParser parser)
    throws SAXException
  {
    final var reader =
      parser.getXMLReader();

    /*
     * Turn on "secure processing". Sets various resource limits to prevent
     * various denial of service attacks.
     */

    reader.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING,
      true
    );

    /*
     * Don't allow access to schemas or DTD files.
     */

    reader.setProperty(
      XMLConstants.ACCESS_EXTERNAL_SCHEMA,
      ""
    );

    reader.setProperty(
      XMLConstants.ACCESS_EXTERNAL_DTD,
      ""
    );

    /*
     * Don't load DTDs at all.
     */

    reader.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false
    );

    /*
     * Enable XInclude.
     */

    reader.setFeature(
      "http://apache.org/xml/features/xinclude",
      xinclude == JXEXInclude.XINCLUDE_ENABLED
    );

    /*
     * Ensure namespace processing is enabled.
     */

    reader.setFeature(
      "http://xml.org/sax/features/namespaces",
      true
    );

    /*
     * Disable validation.
     */

    reader.setFeature(
      "http://xml.org/sax/features/validation",
      false
    );

    reader.setFeature(
      "http://apache.org/xml/features/validation/schema",
      false
    );

    /*
     * Tell the parser to use the full EntityResolver2 interface (by default,
     * the extra EntityResolver2 methods will not be called - only those of
     * the original EntityResolver interface would be called).
     */

    reader.setFeature(
      "http://xml.org/sax/features/use-entity-resolver2",
      true
    );

    reader.setEntityResolver(
      JXEHardenedDispatchingResolver.create(
        baseDirectory, JXESchemaResolutionMappings.builder().build()));
    return reader;
  }

  /**
   * Create a XSD-validating XML reader.
   *
   * @param xinclude      A specification of whether or not XInclude should be enabled for parsers
   * @param baseDirectory A directory that will contain parsed resources
   * @param inSchemas     A set of schemas that will be consulted for validation
   *
   * @return A new XSD-validating XML reader
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws SAXException                 On SAX parser errors
   */

  public XMLReader createXMLReader(
    final Optional<Path> baseDirectory,
    final JXEXInclude xinclude,
    final JXESchemaResolutionMappings inSchemas)
    throws ParserConfigurationException, SAXException
  {
    Objects.requireNonNull(baseDirectory, "Base directory");
    Objects.requireNonNull(xinclude, "xinclude");
    Objects.requireNonNull(inSchemas, "Schemas");

    final var parserFactory =
      this.parsers.get();

    final Schema schema;
    try {
      schema = createCompositeSchemaFromMappings(inSchemas);
    } catch (final IOException e) {
      throw new SAXException(e);
    }

    parserFactory.setSchema(schema);

    final var parser =
      parserFactory.newSAXParser();
    final var className =
      parser.getClass().getCanonicalName();

    /*
     * Sadly, Xerces requires a different set of properties to be set for
     * validation.
     */

    if (className.startsWith("org.apache.xerces.jaxp.")) {
      return createXMLReaderXerces(
        baseDirectory,
        xinclude,
        inSchemas,
        parser
      );
    }

    return createXMLReaderJDK(baseDirectory, xinclude, inSchemas, parser);
  }

  private static Schema createCompositeSchemaFromMappings(
    final JXESchemaResolutionMappings schemaMappings)
    throws IOException, SAXException
  {
    final var schemaFactory =
      SchemaFactory.newDefaultInstance();
    final var schemaDefinitions =
      schemaMappings.mappings().values();
    final var schemaSources =
      new Source[schemaDefinitions.size()];

    final var index = 0;
    for (final var schemaMapping : schemaDefinitions) {
        final var schemaStream =
          schemaMapping.location().openStream();
        final var streamSource =
          new StreamSource(schemaStream, schemaMapping.fileIdentifier());
        schemaSources[index] = streamSource;
    }

    return schemaFactory.newSchema(schemaSources);
  }

  private static XMLReader createXMLReaderJDK(
    final Optional<Path> baseDirectory,
    final JXEXInclude xinclude,
    final JXESchemaResolutionMappings inSchemas,
    final SAXParser parser)
    throws SAXException
  {
    final var reader =
      parser.getXMLReader();

    /*
     * Turn on "secure processing". Sets various resource limits to prevent
     * various denial of service attacks.
     */

    reader.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING,
      true
    );

    /*
     * Only allow access to schema files using a "file" URL scheme. This is
     * restricted even further via a custom entity resolver.
     */

    reader.setProperty(
      XMLConstants.ACCESS_EXTERNAL_SCHEMA,
      "file"
    );

    /*
     * Deny access to external DTD files.
     */

    reader.setProperty(
      XMLConstants.ACCESS_EXTERNAL_DTD,
      ""
    );

    /*
     * Don't load DTDs at all.
     */

    reader.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false
    );

    /*
     * Enable XInclude.
     */

    reader.setFeature(
      "http://apache.org/xml/features/xinclude",
      xinclude == JXEXInclude.XINCLUDE_ENABLED
    );

    /*
     * Ensure namespace processing is enabled.
     */

    reader.setFeature(
      "http://xml.org/sax/features/namespaces",
      true
    );

    /*
     * Enable validation and, more to the point, enable XSD schema validation.
     */

    reader.setFeature(
      "http://xml.org/sax/features/validation",
      true
    );
    reader.setFeature(
      "http://apache.org/xml/features/validation/schema",
      true
    );

    /*
     * Create a space separated list of mappings from namespace URIs to
     * schema system IDs. This will indicate to the parser that when it encounters
     * a given namespace, it should ask the _entity resolver_ to resolve the
     * corresponding system ID specified here.
     */

    final var locations = new StringBuilder(128);
    inSchemas.mappings().forEach((uri, schema) -> {
      locations.append(uri);
      locations.append(' ');
      locations.append(schema.fileIdentifier());
      locations.append(' ');
    });

    reader.setProperty(
      "http://apache.org/xml/properties/schema/external-schemaLocation",
      locations.toString()
    );

    /*
     * Tell the parser to use the full EntityResolver2 interface (by default,
     * the extra EntityResolver2 methods will not be called - only those of
     * the original EntityResolver interface would be called).
     */

    reader.setFeature(
      "http://xml.org/sax/features/use-entity-resolver2",
      true
    );

    reader.setEntityResolver(
      JXEHardenedDispatchingResolver.create(baseDirectory, inSchemas));

    return reader;
  }

  private static XMLReader createXMLReaderXerces(
    final Optional<Path> baseDirectory,
    final JXEXInclude xinclude,
    final JXESchemaResolutionMappings inSchemas,
    final SAXParser parser)
    throws SAXException
  {
    final var reader =
      parser.getXMLReader();

    /*
     * Turn on "secure processing". Sets various resource limits to prevent
     * various denial of service attacks.
     */

    reader.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING,
      true
    );

    /*
     * Enable XInclude.
     */

    reader.setFeature(
      "http://apache.org/xml/features/xinclude",
      xinclude == JXEXInclude.XINCLUDE_ENABLED
    );

    /*
     * Ensure namespace processing is enabled.
     */

    reader.setFeature(
      "http://xml.org/sax/features/namespaces",
      true
    );

    /*
     * Enable validation.
     *
     * @see "https://xerces.apache.org/xerces2-j/faq-pcfp.html#faq-2"
     */

    reader.setFeature(
      "http://xml.org/sax/features/validation",
      true
    );
    reader.setFeature(
      "http://apache.org/xml/features/validation/schema",
      true
    );

    /*
     * Create a space separated list of mappings from namespace URIs to
     * schema system IDs. This will indicate to the parser that when it encounters
     * a given namespace, it should ask the _entity resolver_ to resolve the
     * corresponding system ID specified here.
     */

    final var locations = new StringBuilder(128);
    inSchemas.mappings().forEach((uri, schema) -> {
      locations.append(uri);
      locations.append(' ');
      locations.append(schema.fileIdentifier());
      locations.append(' ');
    });

    reader.setProperty(
      "http://apache.org/xml/properties/schema/external-schemaLocation",
      locations.toString()
    );

    /*
     * Tell the parser to use the full EntityResolver2 interface (by default,
     * the extra EntityResolver2 methods will not be called - only those of
     * the original EntityResolver interface would be called).
     */

    reader.setFeature(
      "http://xml.org/sax/features/use-entity-resolver2",
      true
    );

    reader.setEntityResolver(
      JXEHardenedDispatchingResolver.create(baseDirectory, inSchemas));

    return reader;
  }
}
