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

package com.io7m.jxe.tests.core;

import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;
import com.io7m.jxe.core.JXEXInclude;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class JXEHardenedSAXParsersTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(JXEHardenedSAXParsersTest.class);

  private JXEHardenedSAXParsers parsers;
  private Path tmpdir;

  @BeforeEach
  public void setUp()
    throws IOException
  {
    this.tmpdir =
      Files.createTempDirectory("jxe-tests-");
    this.parsers =
      new JXEHardenedSAXParsers(SAXParserFactory::newNSInstance);
  }

  @Test
  public void testParseNonValidating()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple.xml"))) {
      reader.parse(new InputSource(input));
    }
  }

  @Test
  public void testParseNonValidatingExplicitSystemID()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple.xml"))) {
      final InputSource source = new InputSource(input);
      source.setSystemId("simple.xml");
      reader.parse(source);
    }
  }

  @Test
  public void testParseNonValidatingIllFormed()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_ill_formed.xml"))) {

      Assertions.assertThrows(SAXParseException.class, () -> {
        reader.parse(new InputSource(input));
      });
    }
  }

  @Test
  public void testParseNonValidatingRefuseTraversal()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_refuse_traversal.xml"))) {

      final var ex =
        Assertions.assertThrows(SAXException.class, () -> {
          reader.parse(new InputSource(input));
        });
      Assertions.assertTrue(
        ex.getMessage().contains(
          "Refusing to allow access to files above the base directory")
      );
    }
  }

  @Test
  public void testParseNonValidatingRefuseNetwork()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_refuse_network.xml"))) {

      final var ex =
        Assertions.assertThrows(SAXException.class, () -> {
          reader.parse(new InputSource(input));
        });
      Assertions.assertTrue(
        ex.getMessage().contains("Refusing to resolve a non-file URI")
      );
    }
  }

  @Test
  public void testParseValidatingNoSchemas()
    throws Exception
  {
    final JXESchemaResolutionMappings schemas =
      JXESchemaResolutionMappings.builder()
        .build();

    final XMLReader reader =
      this.parsers.createXMLReader(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED,
        schemas);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple.xml"))) {

      final var ex =
        Assertions.assertThrows(SAXException.class, () -> {
          reader.parse(new InputSource(input));
        });
      Assertions.assertTrue(
        ex.getMessage().contains(
          "simple")
      );
    }
  }

  @Test
  public void testParseValidatingValid()
    throws Exception
  {
    final JXESchemaResolutionMappings schemas =
      JXESchemaResolutionMappings.builder()
        .putMappings(
          URI.create("urn:com.io7m.example:simple:1:0"),
          JXESchemaDefinition.of(
            URI.create("urn:com.io7m.example:simple:1:0"),
            "/schema_simple_1_0.xsd",
            JXEHardenedSAXParsersTest.class.getResource("simple.xsd")))
        .build();

    final XMLReader reader =
      this.parsers.createXMLReader(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED,
        schemas);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());
    reader.setContentHandler(new DisplayContentHandler());

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_valid.xml"))) {
      reader.parse(new InputSource(input));
    }
  }

  @Test
  public void testParseValidatingInvalid()
    throws Exception
  {
    final JXESchemaResolutionMappings schemas =
      JXESchemaResolutionMappings.builder()
        .putMappings(
          URI.create("urn:com.io7m.example:simple:1:0"),
          JXESchemaDefinition.of(
            URI.create("urn:com.io7m.example:simple:1:0"),
            "/schema_simple_1_0.xsd",
            JXEHardenedSAXParsersTest.class.getResource("simple.xsd")))
        .build();

    final XMLReader reader =
      this.parsers.createXMLReader(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED,
        schemas);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());
    reader.setContentHandler(new DisplayContentHandler());

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_invalid.xml"))) {
      final var ex =
        Assertions.assertThrows(SAXException.class, () -> {
          reader.parse(new InputSource(input));
        });

      LOG.debug("Exception: ", ex);
    }
  }

  @Test
  public void testParseNonValidatingNotAFile()
    throws Exception
  {
    final Path directory = this.tmpdir.resolve("xyz");
    Files.createDirectories(directory);

    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_invalid_not_file.xml"))) {

      final var ex =
        Assertions.assertThrows(SAXParseException.class, () -> {
          reader.parse(new InputSource(input));
        });
      Assertions.assertTrue(
        ex.getMessage().contains("File does not exist or is not a regular file")
      );
    }
  }

  @Test
  public void testParseNonValidatingRegularFile()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());

    this.copyResource("simple.xml");
    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_regular_file.xml"))) {
      reader.parse(new InputSource(input));
    }
  }

  @Test
  public void testParseNonValidatingRegularFileNoFilesystem()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.empty(),
        JXEXInclude.XINCLUDE_ENABLED);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());

    this.copyResource("simple.xml");
    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_regular_file.xml"))) {

      final var ex =
        Assertions.assertThrows(SAXException.class, () -> {
          reader.parse(new InputSource(input));
        });
      Assertions.assertTrue(
        ex.getMessage().contains("Refusing to allow access to the filesystem")
      );
    }
  }

  @Test
  public void testBillionLaughs()
    throws Exception
  {
    final XMLReader reader =
      this.parsers.createXMLReaderNonValidating(
        Optional.of(this.tmpdir),
        JXEXInclude.XINCLUDE_ENABLED);

    reader.setErrorHandler(new EverythingIsFatalErrorHandler());

    try (InputStream input =
           Files.newInputStream(this.copyResource("billion.xml"))) {

      final var ex =
        Assertions.assertThrows(SAXException.class, () -> {
          reader.parse(new InputSource(input));
        });

      LOG.debug("Exception: ", ex);
    }
  }

  private Path copyResource(final String file)
    throws IOException
  {
    final URL url = JXEHardenedSAXParsersTest.class.getResource(file);
    if (url == null) {
      throw new AssertionError("No such resource: " + file);
    }
    try (InputStream stream = url.openStream()) {
      final Path path = this.tmpdir.resolve(file);
      try (OutputStream out = Files.newOutputStream(path)) {
        stream.transferTo(out);
      }
      return path;
    }
  }

  private final static class EverythingIsFatalErrorHandler implements
    ErrorHandler
  {
    EverythingIsFatalErrorHandler()
    {

    }

    @Override
    public void warning(final SAXParseException e)
      throws SAXException
    {
      throw e;
    }

    @Override
    public void error(final SAXParseException e)
      throws SAXException
    {
      throw e;
    }

    @Override
    public void fatalError(final SAXParseException e)
      throws SAXException
    {
      throw e;
    }
  }

  private static final class DisplayContentHandler implements ContentHandler
  {
    DisplayContentHandler()
    {

    }

    @Override
    public void setDocumentLocator(
      final Locator locator)
    {
      LOG.debug("setDocumentLocator: {}", locator);
    }

    @Override
    public void startDocument()
    {
      LOG.debug("startDocument");
    }

    @Override
    public void endDocument()
    {
      LOG.debug("endDocument");
    }

    @Override
    public void startPrefixMapping(
      final String prefix,
      final String uri)
    {
      LOG.debug("startPrefixMapping {} {}", prefix, uri);
    }

    @Override
    public void endPrefixMapping(
      final String prefix)
    {
      LOG.debug("endPrefixMapping {}", prefix);
    }

    @Override
    public void startElement(
      final String uri,
      final String localName,
      final String qName,
      final Attributes atts)
    {
      LOG.debug("startElement {} {} {} {}", uri, localName, qName, atts);
    }

    @Override
    public void endElement(
      final String uri,
      final String localName,
      final String qName)
    {
      LOG.debug("endElement {} {} {}", uri, localName, qName);
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
    {
      LOG.debug("characters {} {} {}", "<redacted>", start, length);
    }

    @Override
    public void ignorableWhitespace(
      final char[] ch,
      final int start,
      final int length)
    {
      LOG.debug("ignorableWhitespace {} {} {}", "<redacted>", start, length);
    }

    @Override
    public void processingInstruction(
      final String target,
      final String data)
    {
      LOG.debug("processingInstruction {} {}", target, data);
    }

    @Override
    public void skippedEntity(final String name)
    {
      LOG.debug("skippedEntity {}", name);
    }
  }
}
