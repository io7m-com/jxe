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

import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaDefinitions;
import com.io7m.jxe.core.JXESchemaResolutionMappings;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.List;

public final class JXESchemaResolutionMappingsTest
{
  @Test
  public void testEqualsHashCode()
  {
    EqualsVerifier.forClass(JXESchemaResolutionMappings.class)
      .withNonnullFields("mappings")
      .verify();
  }

  @Test
  public void testOf()
    throws Exception
  {
    final var schema0 =
      JXESchemaDefinition.of(
        URI.create("urn:test0"),
        "test0.xsd",
        new URL("http://www.example.com/test0.xsd")
      );
    final var schema1 =
      JXESchemaDefinition.of(
        URI.create("urn:test1"),
        "test1.xsd",
        new URL("http://www.example.com/test1.xsd")
      );

    {
      final var mappings =
        JXESchemaDefinitions.mapOf(schema0, schema1);
      Assertions.assertEquals(2, mappings.size());
      Assertions.assertEquals(schema0, mappings.get(URI.create("urn:test0")));
      Assertions.assertEquals(schema1, mappings.get(URI.create("urn:test1")));
    }

    {
      final var mappings =
        JXESchemaDefinitions.mapOfList(List.of(schema0, schema1));
      Assertions.assertEquals(2, mappings.size());
      Assertions.assertEquals(schema0, mappings.get(URI.create("urn:test0")));
      Assertions.assertEquals(schema1, mappings.get(URI.create("urn:test1")));
    }

    {
      final var mappings =
        JXESchemaDefinitions.mappingsOfList(List.of(schema0, schema1));
      Assertions.assertEquals(2, mappings.mappings().size());
      Assertions.assertEquals(schema0, mappings.mappings().get(URI.create("urn:test0")));
      Assertions.assertEquals(schema1, mappings.mappings().get(URI.create("urn:test1")));
    }

    {
      final var mappings =
        JXESchemaDefinitions.mappingsOf(schema0, schema1);
      Assertions.assertEquals(2, mappings.mappings().size());
      Assertions.assertEquals(schema0, mappings.mappings().get(URI.create("urn:test0")));
      Assertions.assertEquals(schema1, mappings.mappings().get(URI.create("urn:test1")));
    }
  }
}
