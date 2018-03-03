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
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class JXEHardenedSAXParsersTest
{
  private JXEHardenedSAXParsers parsers;
  private Path tmpdir;

  @Rule public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp()
    throws IOException
  {
    this.tmpdir = Files.createTempDirectory("jxe-tests-");
    this.parsers = new JXEHardenedSAXParsers();
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

      this.expected.expect(SAXParseException.class);
      reader.parse(new InputSource(input));
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

      this.expected.expect(SAXException.class);
      this.expected.expectMessage(StringContains.containsString(
        "Refusing to allow access to files above the base directory"));
      reader.parse(new InputSource(input));
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

      this.expected.expect(SAXException.class);
      this.expected.expectMessage(StringContains.containsString(
        "Refusing to resolve a non-file URI"));
      reader.parse(new InputSource(input));
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

      this.expected.expect(SAXException.class);
      this.expected.expectMessage(StringContains.containsString(
        "Cannot find the declaration of element 'simple'"));
      reader.parse(new InputSource(input));
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

    try (InputStream input =
           Files.newInputStream(this.copyResource("simple_valid.xml"))) {
      reader.parse(new InputSource(input));
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
      this.expected.expect(SAXParseException.class);
      this.expected.expectMessage(StringContains.containsString(
        "File does not exist or is not a regular file"));
      reader.parse(new InputSource(input));
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

      this.expected.expect(SAXException.class);
      this.expected.expectMessage(StringContains.containsString("Refusing to allow access to the filesystem"));
      reader.parse(new InputSource(input));
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
      this.expected.expect(SAXException.class);
      this.expected.expectMessage(StringContains.containsString(
        "External subsets are explicitly forbidden by this parser configuration"));
      reader.parse(new InputSource(input));
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
}
