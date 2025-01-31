/*-
 * ========================LICENSE_START=================================
 * jgea-problem
 * %%
 * Copyright (C) 2018 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.ericmedvet.jgea.problem.extraction.string;

import io.github.ericmedvet.jgea.core.representation.graph.finiteautomata.Extractor;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.core.util.Sized;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexBasedExtractor implements Extractor<Character>, Sized {

  private final String regex;

  public RegexBasedExtractor(String regex) {
    this.regex = regex;
  }

  @Override
  public Set<IntRange> extract(List<Character> sequence) {
    String string = sequence.stream().map(String::valueOf).collect(Collectors.joining());
    if (Pattern.compile(regex).matcher("").matches()) {
      return Set.of();
    }
    Matcher matcher = Pattern.compile(regex).matcher(string);
    Set<IntRange> extractions = new LinkedHashSet<>();
    int s = 0;
    while (matcher.find(s)) {
      IntRange extraction = new IntRange(matcher.start(), matcher.end());
      s = extraction.max();
      extractions.add(extraction);
    }
    return extractions;
  }

  @Override
  public boolean match(List<Character> sequence) {
    String string = sequence.stream().map(String::valueOf).collect(Collectors.joining());
    Matcher matcher = Pattern.compile(regex).matcher(string);
    return matcher.matches();
  }

  @Override
  public Set<IntRange> extractNonOverlapping(List<Character> sequence) {
    return extract(sequence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(regex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RegexBasedExtractor that = (RegexBasedExtractor) o;
    return regex.equals(that.regex);
  }

  @Override
  public String toString() {
    return regex;
  }

  @Override
  public int size() {
    return regex.length();
  }
}
