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
package org.agrona.agent;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

import net.bytebuddy.asm.Advice;

/**
 * Interceptor to be applied when verifying buffer alignment accesses.
 */
@SuppressWarnings("unused")
public class BufferAlignmentInterceptor
{
    abstract static class Verifier
    {
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer, final int alignment)
        {
            final int alignmentOffset = (int)(buffer.addressOffset() + index) % alignment;
            if (0 != alignmentOffset)
            {
                throw new BufferAlignmentException(
                    "Unaligned " + alignment + "-byte access (index=" + index + ", offset=" + alignmentOffset + ")");
            }
        }
    }

    /**
     * Verifier for {@code long} types.
     */
    public static final class LongVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_LONG);
        }
    }

    /**
     * Verifier for {@code double} types.
     */
    public static final class DoubleVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_DOUBLE);
        }
    }

    /**
     * Verifier for {@code int} types.
     */
    public static final class IntVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_INT);
        }
    }

    /**
     * Verifier for {@code float} types.
     */
    public static final class FloatVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_FLOAT);
        }
    }

    /**
     * Verifier for {@code short} types.
     */
    public static final class ShortVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_SHORT);
        }
    }

    /**
     * Verifier for {@code char} types.
     */
    public static final class CharVerifier extends Verifier
    {
        @Advice.OnMethodEnter
        public static void verifyAlignment(final int index, final @Advice.This DirectBuffer buffer)
        {
            verifyAlignment(index, buffer, BitUtil.SIZE_OF_CHAR);
        }
    }
}
