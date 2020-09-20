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

import java.io.Serializable;

/**
 * Holder for an int value that is mutable. Useful for being a counter in a {@link java.util.Map} or for passing by
 * reference.
 */
public class MutableInteger extends Number implements Comparable<MutableInteger>, Serializable
{
    public int value = 0;

    public MutableInteger()
    {
    }

    public MutableInteger(final int value)
    {
        this.value = value;
    }

    public int get()
    {
        return value;
    }

    public void set(final int value)
    {
        this.value = value;
    }

    public byte byteValue()
    {
        return (byte)value;
    }

    public short shortValue()
    {
        return (short)value;
    }

    public int intValue()
    {
        return value;
    }

    public long longValue()
    {
        return value;
    }

    public float floatValue()
    {
        return (float)value;
    }

    public double doubleValue()
    {
        return value;
    }

    public void increment()
    {
        value++;
    }

    public int incrementAndGet()
    {
        return ++value;
    }

    public int getAndIncrement()
    {
        return value++;
    }

    public void decrement()
    {
        value--;
    }

    public int decrementAndGet()
    {
        return --value;
    }

    public int getAndDecrement()
    {
        return value--;
    }

    public int getAndAdd(final int delta)
    {
        final int result = value;
        value += delta;
        return result;
    }

    public int addAndGet(final int delta)
    {
        return value += delta;
    }

    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final MutableInteger that = (MutableInteger)o;

        return value == that.value;
    }

    public int hashCode()
    {
        return Integer.hashCode(value);
    }

    public String toString()
    {
        return Integer.toString(value);
    }

    public int compareTo(final MutableInteger that)
    {
        return compare(this.value, that.value);
    }

    public static int compare(final int lhs, final int rhs)
    {
        return Integer.compare(lhs, rhs);
    }
}
