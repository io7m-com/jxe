/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Functions over schema definitions.
 */

public final class JXESchemaDefinitions
{
  private JXESchemaDefinitions()
  {

  }

  /**
   * Make a map of schema definitions from the given list of schemas.
   *
   * @param schemas The list of schemas
   *
   * @return The schemas
   */

  public static Map<URI, JXESchemaDefinition> mapOf(
    final JXESchemaDefinition... schemas)
  {
    Objects.requireNonNull(schemas, "schemas");
    return mapOfList(List.of(schemas));
  }

  /**
   * Make a map of schema definitions from the given list of schemas.
   *
   * @param schemas The list of schemas
   *
   * @return The schemas
   */

  public static Map<URI, JXESchemaDefinition> mapOfList(
    final List<JXESchemaDefinition> schemas)
  {
    Objects.requireNonNull(schemas, "schemas");
    return schemas.stream().collect(Collectors.toMap(
      JXESchemaDefinition::namespace,
      Function.identity()
    ));
  }

  /**
   * Make a map of schema definitions from the given list of schemas.
   *
   * @param schemas The list of schemas
   *
   * @return The schemas
   */

  public static JXESchemaResolutionMappings mappingsOf(
    final JXESchemaDefinition... schemas)
  {
    Objects.requireNonNull(schemas, "schemas");
    return JXESchemaResolutionMappings.builder()
      .putAllMappings(mapOf(schemas))
      .build();
  }

  /**
   * Make a map of schema definitions from the given list of schemas.
   *
   * @param schemas The list of schemas
   *
   * @return The schemas
   */

  public static JXESchemaResolutionMappings mappingsOfList(
    final List<JXESchemaDefinition> schemas)
  {
    Objects.requireNonNull(schemas, "schemas");
    return JXESchemaResolutionMappings.builder()
      .putAllMappings(mapOfList(schemas))
      .build();
  }
}
