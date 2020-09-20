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
package org.agrona.nio;

import org.agrona.collections.ArrayUtil;

import java.nio.channels.SelectionKey;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.ToIntFunction;

/**
 * Try to fix handling of HashSet for {@link java.nio.channels.Selector} to avoid excessive allocation.
 * Assumes single threaded usage.
 */
public class NioSelectedKeySet extends AbstractSet<SelectionKey>
{
    private static final int INITIAL_CAPACITY = 10;

    private SelectionKey[] keys;
    private int size = 0;

    /**
     * Construct a key set with default capacity
     */
    public NioSelectedKeySet()
    {
        keys = new SelectionKey[INITIAL_CAPACITY];
    }

    /**
     * Construct a key set with the given capacity.
     *
     * @param initialCapacity for the key set
     */
    public NioSelectedKeySet(final int initialCapacity)
    {
        if (initialCapacity < 0 || initialCapacity > ArrayUtil.MAX_CAPACITY)
        {
            throw new IllegalArgumentException("invalid initial capacity: " + initialCapacity);
        }

        keys = new SelectionKey[Math.max(initialCapacity, INITIAL_CAPACITY)];
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return size;
    }

    /**
     * Capacity of the current set
     *
     * @return capacity of the set
     */
    public int capacity()
    {
        return keys.length;
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(final SelectionKey selectionKey)
    {
        if (null == selectionKey)
        {
            return false;
        }

        ensureCapacity(size + 1);
        keys[size++] = selectionKey;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object o)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(final Object o)
    {
        return false;
    }

    /**
     * Return selected keys for direct processing which is valid up to {@link #size()} index.
     *
     * @return selected keys for direct processing which is valid up to {@link #size()} index.
     */
    public SelectionKey[] keys()
    {
        return keys;
    }

    /**
     * Reset for next iteration.
     */
    public void reset()
    {
        size = 0;
    }

    /**
     * Null out the keys and set size to 0.
     */
    public void clear()
    {
        Arrays.fill(keys, null);
        size = 0;
    }

    /**
     * Reset for next iteration, having only processed a subset of the selection keys.
     * <p>
     * The {@link NioSelectedKeySet} will still contain the keys representing IO events after
     * the skip Count have been removed, the remaining events can be processed in a future iteration.
     *
     * @param skipCount the number of keys to be skipped over that have already been processed.
     */
    public void reset(final int skipCount)
    {
        if (skipCount > size)
        {
            throw new IllegalArgumentException("skipCount " + skipCount + " > size " + size);
        }

        if (0 != size)
        {
            final SelectionKey[] keys = this.keys;
            final int newSize = size - skipCount;

            System.arraycopy(keys, skipCount, keys, 0, newSize);

            size = newSize;
        }
    }

    /**
     * Iterate over the key set and apply the given function.
     *
     * @param function to apply to each {@link java.nio.channels.SelectionKey}
     * @return number of handled frames.
     */
    public int forEach(final ToIntFunction<SelectionKey> function)
    {
        int handledFrames = 0;
        final SelectionKey[] keys = this.keys;

        for (int i = size - 1; i >= 0; i--)
        {
            handledFrames += function.applyAsInt(keys[i]);
        }

        size = 0;

        return handledFrames;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<SelectionKey> iterator()
    {
        throw new UnsupportedOperationException();
    }

    private void ensureCapacity(final int requiredCapacity)
    {
        if (requiredCapacity < 0)
        {
            throw new IllegalStateException(
                "Insufficient capacity: length=" + keys.length + " required=" + requiredCapacity);
        }

        final int currentCapacity = keys.length;
        if (requiredCapacity > currentCapacity)
        {
            int newCapacity = currentCapacity + (currentCapacity >> 1);

            if (newCapacity < 0 || newCapacity > ArrayUtil.MAX_CAPACITY)
            {
                if (currentCapacity == ArrayUtil.MAX_CAPACITY)
                {
                    throw new IllegalStateException("max capacity reached: " + ArrayUtil.MAX_CAPACITY);
                }

                newCapacity = ArrayUtil.MAX_CAPACITY;
            }

            keys = Arrays.copyOf(keys, newCapacity);
        }
    }
}
