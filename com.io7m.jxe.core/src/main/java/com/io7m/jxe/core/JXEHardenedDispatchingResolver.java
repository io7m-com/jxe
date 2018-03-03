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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * A hardened entity resolver that can resolve resources from a set of
 * given schemas or from any descendant of a given directory. The resolver
 * prevents path traversal attacks by refusing to resolve resources outside
 * of the given directory.
 */

public final class JXEHardenedDispatchingResolver implements EntityResolver2
{
  private static final Logger LOG =
    LoggerFactory.getLogger(JXEHardenedDispatchingResolver.class);

  private final Optional<Path> base_directory;
  private final JXESchemaResolutionMappings schemas;

  private JXEHardenedDispatchingResolver(
    final Optional<Path> in_base_directory,
    final JXESchemaResolutionMappings in_schemas)
  {
    this.base_directory =
      Objects.requireNonNull(in_base_directory, "Base directory")
        .map(p -> p.toAbsolutePath().normalize());

    this.schemas =
      Objects.requireNonNull(in_schemas, "Schemas");
  }

  /**
   * Create a new resolver. The resolver will resolve schemas from the given
   * schema mappings, and will optionall resolve other file resources from the
   * given base directory. If no base directory is provided, no resolution of
   * resources from the filesystem will occur.
   *
   * @param in_base_directory The base directory used to resolve resources, if any
   * @param in_schemas        A set of schema mappings
   *
   * @return A new resolver
   */

  public static JXEHardenedDispatchingResolver create(
    final Optional<Path> in_base_directory,
    final JXESchemaResolutionMappings in_schemas)
  {
    return new JXEHardenedDispatchingResolver(in_base_directory, in_schemas);
  }

  @Override
  public InputSource getExternalSubset(
    final String name,
    final String base_uri)
    throws SAXException
  {
    /*
     * This will be encountered upon inline entity definitions.
     */

    throw new SAXException(
      "External subsets are explicitly forbidden by this parser configuration.");
  }

  @Override
  public InputSource resolveEntity(
    final String name,
    final String public_id,
    final String base_uri,
    final String system_id)
    throws SAXException, IOException
  {
    LOG.debug(
      "resolveEntity: {} {} {} {}", name, public_id, base_uri, system_id);

    final Optional<JXESchemaDefinition> schema_opt =
      this.schemas.mappings()
        .values()
        .stream()
        .filter(def -> Objects.equals(def.fileIdentifier(), system_id))
        .findAny();

    if (schema_opt.isPresent()) {
      final JXESchemaDefinition schema = schema_opt.get();
      LOG.debug("resolving {} from internal resources -> {}",
                system_id, schema.location());

      /*
       * It's necessary to explicitly set a system ID for the input
       * source, or Xerces XIncludeHandler.searchForRecursiveIncludes()
       * method will raise a null pointer exception when it tries to
       * call equals() on a null system ID.
       */

      final InputSource source =
        new InputSource(schema.location().openStream());
      source.setSystemId(schema.location().toString());
      return source;
    }

    try {
      final URI uri = new URI(system_id);
      final String scheme = uri.getScheme();

      if (!isResolvable(scheme)) {
        throw new SAXException(
          new StringBuilder(128)
            .append("Refusing to resolve a non-file URI.")
            .append(System.lineSeparator())
            .append("  Base: ")
            .append(this.base_directory)
            .append(System.lineSeparator())
            .append("  URI: ")
            .append(uri)
            .append(System.lineSeparator())
            .toString());
      }

      if (!this.base_directory.isPresent()) {
        throw new SAXException(
          new StringBuilder(128)
            .append(
              "Refusing to allow access to the filesystem.")
            .append(System.lineSeparator())
            .append("  Input URI: ")
            .append(uri)
            .append(System.lineSeparator())
            .toString());
      }

      final Path base = this.base_directory.get();
      LOG.debug("resolving {} from filesystem", system_id);

      final Path resolved =
        base.resolve(system_id)
          .toAbsolutePath()
          .normalize();

      if (!resolved.startsWith(base)) {
        throw new SAXException(
          new StringBuilder(128)
            .append(
              "Refusing to allow access to files above the base directory.")
            .append(System.lineSeparator())
            .append("  Base: ")
            .append(base)
            .append(System.lineSeparator())
            .append("  Path: ")
            .append(resolved)
            .append(System.lineSeparator())
            .toString());
      }

      if (!Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS)) {
        throw new NoSuchFileException(
          resolved.toString(),
          null,
          "File does not exist or is not a regular file");
      }

      /*
       * It's necessary to explicitly set a system ID for the input
       * source, or Xerces XIncludeHandler.searchForRecursiveIncludes()
       * method will raise a null pointer exception when it tries to
       * call equals() on a null system ID.
       */

      final InputSource source =
        new InputSource(Files.newInputStream(resolved));
      source.setSystemId(resolved.toString());
      return source;

    } catch (final URISyntaxException e) {
      throw new SAXException(
        new StringBuilder(128)
          .append("Refusing to resolve an unparseable URI.")
          .append(System.lineSeparator())
          .append("  Base: ")
          .append(this.base_directory)
          .append(System.lineSeparator())
          .append("  URI: ")
          .append(system_id)
          .append(System.lineSeparator())
          .toString(),
        e);
    }
  }

  private static boolean isResolvable(final String scheme)
  {
    return Objects.equals("file", scheme) || scheme == null;
  }

  @Override
  public InputSource resolveEntity(
    final String public_id,
    final String system_id)
  {
    throw new UnsupportedOperationException(
      "Simple entity resolution not supported");
  }
}
