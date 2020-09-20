/*
 * Copyright 2014-2020 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.collections;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class Int2IntCounterMapTest
{
    public static final int INITIAL_VALUE = 0;

    private final Int2IntCounterMap map = new Int2IntCounterMap(INITIAL_VALUE);

    @Test
    public void shouldInitiallyBeEmpty()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void getShouldReturnInitialValueWhenEmpty()
    {
        assertEquals(INITIAL_VALUE, map.get(1));
    }

    @Test
    public void getShouldReturnInitialValueWhenThereIsNoElement()
    {
        map.put(1, 1);

        assertEquals(INITIAL_VALUE, map.get(2));
    }

    @Test
    public void getShouldReturnPutValues()
    {
        map.put(1, 1);

        assertEquals(1, map.get(1));
    }

    @Test
    public void putShouldReturnOldValue()
    {
        map.put(1, 1);

        assertEquals(1, map.put(1, 2));
    }

    @Test
    public void clearShouldResetSize()
    {
        map.put(1, 1);
        map.put(100, 100);

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void clearShouldRemoveValues()
    {
        map.put(1, 1);
        map.put(100, 100);

        map.clear();

        assertEquals(INITIAL_VALUE, map.get(1));
        assertEquals(INITIAL_VALUE, map.get(100));
    }

    @Test
    public void forEachShouldLoopOverEveryElement()
    {
        map.put(1, 1);
        map.put(100, 100);

        final IntIntConsumer mockConsumer = mock(IntIntConsumer.class);
        map.forEach(mockConsumer);

        final InOrder inOrder = inOrder(mockConsumer);
        inOrder.verify(mockConsumer).accept(1, 1);
        inOrder.verify(mockConsumer).accept(100, 100);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldNotContainKeyOfAMissingKey()
    {
        assertFalse(map.containsKey(1));
    }

    @Test
    public void shouldContainKeyOfAPresentKey()
    {
        map.put(1, 1);

        assertTrue(map.containsKey(1));
    }

    @Test
    public void shouldNotContainValueForAMissingEntry()
    {
        assertFalse(map.containsValue(1));
    }

    @Test
    public void shouldContainValueForAPresentEntry()
    {
        map.put(1, 1);

        assertTrue(map.containsValue(1));
    }

    @Test
    public void removeShouldReturnMissing()
    {
        assertEquals(INITIAL_VALUE, map.remove(1));
    }

    @Test
    public void removeShouldReturnValueRemoved()
    {
        map.put(1, 2);

        assertEquals(2, map.remove(1));
    }

    @Test
    public void removeShouldRemoveEntry()
    {
        map.put(1, 2);

        map.remove(1);

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(1));
        assertFalse(map.containsValue(2));
    }

    @Test
    public void shouldOnlyRemoveTheSpecifiedEntry()
    {
        IntStream.range(1, 8).forEach((i) -> map.put(i, i * 2));

        map.remove(5);

        IntStream
            .range(1, 8)
            .filter((i) -> i != 5L)
            .forEach((i) ->
            {
                assertTrue(map.containsKey(i));
                assertTrue(map.containsValue(2 * i));
            });
    }

    @Test
    public void shouldResizeWhenMoreElementsAreAdded()
    {
        IntStream
            .range(1, 100)
            .forEach((key) ->
            {
                final int value = key * 2;
                assertEquals(INITIAL_VALUE, map.put(key, value));
                assertEquals(value, map.get(key));
            });
    }

    @Test
    public void shouldHaveNoMinValueForEmptyCollection()
    {
        assertEquals(INITIAL_VALUE, map.minValue());
    }

    @Test
    public void shouldFindMinValue()
    {
        map.put(1, 2);
        map.put(2, 10);
        map.put(3, -5);

        assertEquals(-5, map.minValue());
    }

    @Test
    public void shouldHaveNoMaxValueForEmptyCollection()
    {
        assertEquals(INITIAL_VALUE, map.maxValue());
    }

    @Test
    public void shouldFindMaxValue()
    {
        map.put(1, 2);
        map.put(2, 10);
        map.put(3, -5);

        assertEquals(10, map.maxValue());
    }

    @Test
    public void sizeShouldReturnNumberOfEntries()
    {
        final int count = 100;
        for (int key = 0; key < count; key++)
        {
            map.put(key, 1);
        }

        assertEquals(count, map.size());
    }

    @Test
    public void shouldNotSupportLoadFactorOfGreaterThanOne()
    {
        assertThrows(IllegalArgumentException.class, () -> new Int2IntHashMap(4, 2, 0));
    }

    @Test
    public void shouldNotSupportLoadFactorOfOne()
    {
        assertThrows(IllegalArgumentException.class, () -> new Int2IntHashMap(4, 1, 0));
    }

    @Test
    public void correctSizeAfterRehash()
    {
        final Int2IntHashMap map = new Int2IntHashMap(16, 0.6f, -1);

        IntStream.range(1, 17).forEach((i) -> map.put(i, i));
        assertEquals(16, map.size(), "Map has correct size");

        final List<Integer> keys = new ArrayList<>(map.keySet());
        keys.forEach(map::remove);

        assertTrue(map.isEmpty(), "Map isn't empty");
    }

    @Test
    public void shouldComputeIfAbsent()
    {
        final int testKey = 7;
        final int testValue = 7;

        final IntUnaryOperator function = (i) -> testValue;

        assertEquals(map.initialValue(), map.get(testKey));

        assertThat(map.computeIfAbsent(testKey, function), is(testValue));
        assertThat(map.get(testKey), is(testValue));
    }

    @Test
    public void shouldContainValueForIncAndDecEntries()
    {
        map.incrementAndGet(1);
        map.getAndIncrement(2);
        map.getAndAdd(3, 2);
        map.addAndGet(4, 3);
        map.decrementAndGet(5);
        map.getAndDecrement(6);

        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        assertTrue(map.containsKey(3));
        assertTrue(map.containsKey(4));
        assertTrue(map.containsKey(5));
        assertTrue(map.containsKey(6));
    }

    @Test
    public void shouldResultInEmptyAfterIncAndDecWhenEmpty()
    {
        map.incrementAndGet(1);
        map.decrementAndGet(1);

        assertTrue(map.isEmpty());

        map.getAndIncrement(1);
        map.getAndDecrement(1);

        assertTrue(map.isEmpty());

        map.incrementAndGet(1);
        map.getAndDecrement(1);

        assertTrue(map.isEmpty());

        map.getAndIncrement(1);
        map.decrementAndGet(1);

        assertTrue(map.isEmpty());
    }

    @Test
    public void shouldResultInNotEmptyAfterIncAndDecWhenHaveKey()
    {
        map.put(1, 1);

        map.incrementAndGet(1);
        map.decrementAndGet(1);

        assertEquals(1, map.get(1));
        assertEquals(1, map.size());
    }

    @Test
    public void shouldReturnInitialValueForGetAndAdd0IfKeyMissing()
    {
        final int val = map.getAndAdd(1, 0);

        assertEquals(INITIAL_VALUE, val);
        assertTrue(map.isEmpty());
    }

    @Test
    public void shouldReturnOldValueForGetAndAdd0IfKeyExists()
    {
        map.put(1, 1);
        final int val = map.getAndAdd(1, 0);

        assertEquals(1, val);
        assertEquals(1, map.size());
    }

    @Test
    public void shouldReturnOldValueForGetAndAddNot0IfKeyExists()
    {
        map.put(1, 1);
        int amount;
        do
        {
            amount = ThreadLocalRandom.current().nextInt();
        }
        while (amount == 0);
        final int val = map.getAndAdd(1, amount);

        assertEquals(1, val);
        assertEquals(1, map.size());
    }

    @Test
    public void shouldRemoveEntryAfterDecToInitialVal()
    {
        map.put(1, INITIAL_VALUE + 1);

        map.decrementAndGet(1);

        assertEquals(INITIAL_VALUE, map.get(1));
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(1));
    }

    @Test
    public void shouldRemoveEntryAfterIncToInitialVal()
    {
        map.put(1, INITIAL_VALUE - 1);

        map.incrementAndGet(1);

        assertEquals(INITIAL_VALUE, map.get(1));
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(1));
    }

    @Test
    public void shouldNotAllowInitialValueAsValue()
    {
        assertThrows(IllegalArgumentException.class, () -> map.put(1, INITIAL_VALUE));
    }

    @Test
    public void shouldAllowInitialValueAsKey()
    {
        map.put(INITIAL_VALUE, 1);

        assertEquals(1, map.get(INITIAL_VALUE));
        assertTrue(map.containsKey(INITIAL_VALUE));
        assertEquals(1, map.size());

        final int[] tuple = new int[2];
        map.forEach((k, v) ->
        {
            tuple[0] = k;
            tuple[1] = v;
        });

        assertEquals(INITIAL_VALUE, tuple[0]);
        assertEquals(1, tuple[1]);

        assertEquals(1, map.remove(INITIAL_VALUE));
        assertEquals(0, map.size());
        assertEquals(INITIAL_VALUE, map.get(INITIAL_VALUE));
    }

    @Test
    public void shouldNotContainInitialValue()
    {
        assertFalse(map.containsValue(INITIAL_VALUE));
        map.put(INITIAL_VALUE, 1);
        assertFalse(map.containsValue(INITIAL_VALUE));
    }
}
