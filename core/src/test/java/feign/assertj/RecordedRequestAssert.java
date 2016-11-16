/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign.assertj;

import feign.Util;
import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.MapEntry;
import org.assertj.core.internal.ByteArrays;
import org.assertj.core.internal.Failures;
import org.assertj.core.internal.Maps;
import org.assertj.core.internal.Objects;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static org.assertj.core.data.MapEntry.entry;
import static org.assertj.core.error.ShouldNotContain.shouldNotContain;

public final class RecordedRequestAssert
    extends AbstractAssert<RecordedRequestAssert, RecordedRequest> {

  ByteArrays arrays = ByteArrays.instance();
  Objects objects = Objects.instance();
  Maps maps = Maps.instance();
  Failures failures = Failures.instance();

  public RecordedRequestAssert(RecordedRequest actual) {
    super(actual, RecordedRequestAssert.class);
  }

  public RecordedRequestAssert hasMethod(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.getMethod(), expected);
    return this;
  }

  public RecordedRequestAssert hasPath(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.getPath(), expected);
    return this;
  }

  public RecordedRequestAssert hasBody(String utf8Expected) {
    isNotNull();
    objects.assertEqual(info, actual.getBody().readUtf8(), utf8Expected);
    return this;
  }

  public RecordedRequestAssert hasGzippedBody(byte[] expectedUncompressed) {
    isNotNull();
    byte[] compressedBody = actual.getBody().readByteArray();
    byte[] uncompressedBody;
    try {
      uncompressedBody =
          Util.toByteArray(new GZIPInputStream(new ByteArrayInputStream(compressedBody)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    arrays.assertContains(info, uncompressedBody, expectedUncompressed);
    return this;
  }

  public RecordedRequestAssert hasDeflatedBody(byte[] expectedUncompressed) {
    isNotNull();
    byte[] compressedBody = actual.getBody().readByteArray();
    byte[] uncompressedBody;
    try {
      uncompressedBody =
          Util.toByteArray(new InflaterInputStream(new ByteArrayInputStream(compressedBody)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    arrays.assertContains(info, uncompressedBody, expectedUncompressed);
    return this;
  }

  public RecordedRequestAssert hasBody(byte[] expected) {
    isNotNull();
    arrays.assertContains(info, actual.getBody().readByteArray(), expected);
    return this;
  }

  /**
   * @deprecated use {@link #hasHeaders(MapEntry...)}
   */
  @Deprecated
  public RecordedRequestAssert hasHeaders(String... headerLines) {
    isNotNull();
    Headers.Builder builder = new Headers.Builder();
    for (String next : headerLines) {
      builder.add(next);
    }
    List<MapEntry> expected = builder.build().toMultimap().entrySet().stream().map(next -> entry(next.getKey(), next.getValue())).collect(Collectors.toList());
    hasHeaders(expected.toArray(new MapEntry[expected.size()]));
    return this;
  }

  public RecordedRequestAssert hasHeaders(MapEntry... expected) {
    isNotNull();
    maps.assertContains(info, actual.getHeaders().toMultimap(), expected);
    return this;
  }

  public RecordedRequestAssert hasNoHeaderNamed(final String... names) {
    isNotNull();
    Set<String> found = new LinkedHashSet<>();
    for (String header : actual.getHeaders().names()) {
      for (String name : names) {
        if (header.toLowerCase().startsWith(name.toLowerCase() + ":")) {
          found.add(header);
        }
      }
    }
    if (found.isEmpty()) {
      return this;
    }
    throw failures.failure(info, shouldNotContain(actual.getHeaders(), names, found));
  }
}
