package org.agrona.collections;

import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Map;

public class Int2IntHashMapConformanceTest
{
    // Generated suite to test conformity to the java.util.Set interface
    public static TestSuite suite()
    {
        return mapTestSuite(new TestMapGenerator<Integer, Integer>()
        {
            public Integer[] createKeyArray(final int length)
            {
                return new Integer[length];
            }

            public Integer[] createValueArray(final int length)
            {
                return new Integer[length];
            }

            public SampleElements<Map.Entry<Integer, Integer>> samples()
            {
                return new SampleElements<>(
                    Helpers.mapEntry(1, 123),
                    Helpers.mapEntry(2, 234),
                    Helpers.mapEntry(3, 345),
                    Helpers.mapEntry(345, 6),
                    Helpers.mapEntry(777, 666));
            }

            public Map<Integer, Integer> create(final Object... entries)
            {
                final Int2IntHashMap map = new Int2IntHashMap(
                    entries.length * 2, Hashing.DEFAULT_LOAD_FACTOR, -1, false);

                for (final Object o : entries)
                {
                    @SuppressWarnings("unchecked")
                    final Map.Entry<Integer, Integer> e = (Map.Entry<Integer, Integer>)o;
                    map.put(e.getKey(), e.getValue());
                }

                return map;
            }

            @SuppressWarnings("unchecked")
            public Map.Entry<Integer, Integer>[] createArray(final int length)
            {
                return new Map.Entry[length];
            }

            public Iterable<Map.Entry<Integer, Integer>> order(final List<Map.Entry<Integer, Integer>> insertionOrder)
            {
                return insertionOrder;
            }
        },
            Int2IntHashMap.class.getSimpleName());
    }

    private static <T> TestSuite mapTestSuite(final TestMapGenerator<T, T> testMapGenerator, final String name)
    {
        return new MapTestSuiteBuilder<T, T>()
        {
            {
                usingGenerator(testMapGenerator);
            }
        }.withFeatures(
            MapFeature.GENERAL_PURPOSE,
            CollectionSize.ANY,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .named(name)
            .createTestSuite();
    }
}
