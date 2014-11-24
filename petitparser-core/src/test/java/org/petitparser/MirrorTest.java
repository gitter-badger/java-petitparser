package org.petitparser;

import org.junit.Test;
import org.petitparser.parser.Parser;
import org.petitparser.parser.characters.CharacterParser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.utils.Mirror;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests {@link Mirror}.
 */
public class MirrorTest {

  @Test
  public void testSingleElementIteration() {
    Parser parser = CharacterParser.lowerCase();
    Mirror mirror = Mirror.of(parser);
    List<Parser> parsers = mirror.stream().collect(Collectors.toList());
    assertEquals(Arrays.asList(parser), parsers);
  }

  @Test
  public void testNestedElementsIteration() {
    Parser parser3 = CharacterParser.lowerCase();
    Parser parser2 = parser3.star();
    Parser parser1 = parser2.flatten();
    Mirror mirror = Mirror.of(parser1);
    List<Parser> parsers = mirror.stream().collect(Collectors.toList());
    assertEquals(Arrays.asList(parser1, parser2, parser3), parsers);
  }

  @Test
  public void testBranchedElementsIteration() {
    Parser parser3 = CharacterParser.lowerCase();
    Parser parser2 = CharacterParser.upperCase();
    Parser parser1 = parser2.seq(parser3);
    Mirror mirror = Mirror.of(parser1);
    List<Parser> parsers = mirror.stream().collect(Collectors.toList());
    assertEquals(Arrays.asList(parser1, parser3, parser2), parsers);
  }

  @Test
  public void testDuplicatedElementsIteration() {
    Parser parser2 = CharacterParser.upperCase();
    Parser parser1 = parser2.seq(parser2);
    Mirror mirror = Mirror.of(parser1);
    List<Parser> parsers = mirror.stream().collect(Collectors.toList());
    assertEquals(Arrays.asList(parser1, parser2), parsers);
  }

  @Test
  public void testKnotParserIteration() {
    SettableParser parser1 = SettableParser.undefined();
    parser1.setDelegate(parser1);
    Mirror mirror = Mirror.of(parser1);
    List<Parser> parsers = mirror.stream().collect(Collectors.toList());
    assertEquals(Arrays.<Parser>asList(parser1), parsers);
  }

  @Test
  public void testLoopingParserIteration() {
    SettableParser parser1 = SettableParser.undefined();
    SettableParser parser2 = SettableParser.undefined();
    SettableParser parser3 = SettableParser.undefined();
    parser1.setDelegate(parser2);
    parser2.setDelegate(parser3);
    parser3.setDelegate(parser1);
    Mirror mirror = Mirror.of(parser1);
    List<Parser> parsers = mirror.stream().collect(Collectors.toList());
    assertEquals(Arrays.<Parser>asList(parser1, parser2, parser3), parsers);
  }

  @Test
  public void testBasicIteration() {
    Parser parser = CharacterParser.lowerCase();
    Iterator<Parser> iterator = Mirror.of(parser).iterator();
    assertTrue(iterator.hasNext());
    assertEquals(parser, iterator.next());
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail();
    } catch (NoSuchElementException exception) {
      // expected
    }
  }

  @Test
  public void testIdentityTransformation() {
    Parser input = CharacterParser.lowerCase().settable();
    Parser output = Mirror.of(input).transform(Function.identity());
    assertNotEquals(input, output);
    assertTrue(input.isEqualTo(output));
    assertNotEquals(input.getChildren().get(0), output.getChildren().get(0));
  }

  @Test
  public void testReplaceRootTransformation() {
    Parser source = CharacterParser.lowerCase();
    Parser target = CharacterParser.upperCase();
    Parser output = Mirror.of(source).transform(
        parser -> source.isEqualTo(parser) ? target : parser);
    assertNotEquals(source, output);
    assertFalse(source.isEqualTo(output));
    assertEquals(output, target);
  }

  @Test
  public void testSingleElementTransformation() {
    Parser source = CharacterParser.lowerCase();
    Parser input = source.settable();
    Parser target = CharacterParser.upperCase();
    Parser output = Mirror.of(input).transform(parser -> source.isEqualTo(parser) ? target : parser);
    assertNotEquals(input, output);
    assertFalse(input.isEqualTo(output));
    assertEquals(input.getChildren().get(0), source);
    assertEquals(output.getChildren().get(0), target);
  }

  @Test
  public void testDoubleElementTransformation() {
    Parser source = CharacterParser.lowerCase();
    Parser input = source.seq(source);
    Parser target = CharacterParser.upperCase();
    Parser output = Mirror.of(input).transform(parser -> source.isEqualTo(parser) ? target : parser);
    assertNotEquals(input, output);
    assertFalse(input.isEqualTo(output));
    assertTrue(input.isEqualTo(source.seq(source)));
    assertEquals(input.getChildren().get(0), input.getChildren().get(1));
    assertTrue(output.isEqualTo(target.seq(target)));
    assertEquals(output.getChildren().get(0), output.getChildren().get(1));
  }

  @Test
  public void testExistingLoopTransformation() {
    Parser input = SettableParser.undefined().settable().settable().settable();
    SettableParser settable = (SettableParser) input.getChildren().get(0).getChildren().get(0);
    settable.setDelegate(input);
    Parser output = Mirror.of(input).transform(Function.identity());
    assertNotEquals(input, output);
    assertTrue(input.isEqualTo(output));
    Set<Parser> inputs = Mirror.of(input).stream().collect(Collectors.toSet());
    Set<Parser> outputs = Mirror.of(output).stream().collect(Collectors.toSet());
    inputs.forEach(each -> assertFalse(outputs.contains(each)));
    outputs.forEach(each -> assertFalse(inputs.contains(each)));
  }

  @Test
  public void testNewLoopTransformation() {
    Parser source = CharacterParser.lowerCase();
    Parser target = SettableParser.undefined().settable().settable().settable();
    SettableParser settable = (SettableParser) target.getChildren().get(0).getChildren().get(0);
    settable.setDelegate(source);
    Parser output = Mirror.of(source).transform(
        parser -> source.isEqualTo(parser) ? target : parser);
    assertNotEquals(source, output);
    assertFalse(source.isEqualTo(output));
    assertTrue(output.isEqualTo(target));
  }
}