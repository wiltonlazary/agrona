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
package org.agrona.concurrent.ringbuffer;

import org.agrona.collections.MutableInteger;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.HEADER_LENGTH;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.typeOffset;
import static org.agrona.concurrent.ringbuffer.RingBuffer.INSUFFICIENT_CAPACITY;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OneToOneRingBufferConcurrentTest
{
    private static final int MSG_TYPE_ID = 7;
    public static final int REPETITIONS = 1;

    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect((16 * 1024) + TRAILER_LENGTH);
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(byteBuffer);
    private final RingBuffer ringBuffer = new OneToOneRingBuffer(unsafeBuffer);

    @Test
    public void shouldExchangeMessages()
    {
        new Producer().start();

        final MutableInteger count = new MutableInteger();
        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                final int iteration = buffer.getInt(index);

                assertEquals(count.get(), iteration);

                count.increment();
            };

        while (count.get() < REPETITIONS)
        {
            final int readCount = ringBuffer.read(handler);
            if (0 == readCount)
            {
                Thread.yield();
            }
        }
    }

    @Test
    public void shouldExchangeMessagesViaTryClaimCommit()
    {
        new ClaimCommit().start();

        final MutableInteger count = new MutableInteger();
        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                final int iteration = buffer.getInt(index);
                final long longVal = buffer.getLong(index + SIZE_OF_INT);

                assertEquals(count.get(), iteration);
                assertEquals(count.get() * 20L, longVal);

                count.increment();
            };

        while (count.get() < REPETITIONS)
        {
            final int readCount = ringBuffer.read(handler);
            if (0 == readCount)
            {
                Thread.yield();
            }
        }
    }

    @Test
    public void shouldExchangeMessagesViaTryClaimAbort()
    {
        new ClaimAbort().start();

        final MutableInteger count = new MutableInteger();
        final MessageHandler handler =
            (msgTypeId, buffer, index, length) ->
            {
                final int iteration = buffer.getInt(index);

                assertEquals(count.get(), iteration);
                assertEquals(MSG_TYPE_ID, buffer.getInt(typeOffset(index - HEADER_LENGTH)));

                count.increment();
            };

        while (count.get() < REPETITIONS)
        {
            final int readCount = ringBuffer.read(handler);
            if (0 == readCount)
            {
                Thread.yield();
            }
        }
    }

    class Producer extends Thread
    {
        Producer()
        {
            super("producer");
        }

        public void run()
        {
            final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);

            for (int i = 0; i < REPETITIONS; i++)
            {
                srcBuffer.putInt(0, i);

                while (!ringBuffer.write(MSG_TYPE_ID, srcBuffer, 0, SIZE_OF_INT))
                {
                    Thread.yield();
                }
            }
        }
    }

    class ClaimCommit extends Thread
    {
        ClaimCommit()
        {
            super("tryClaim-commit");
        }

        public void run()
        {
            final int length = SIZE_OF_INT + SIZE_OF_LONG;
            for (int i = 0; i < REPETITIONS; i++)
            {

                int index = -1;
                try
                {
                    while (INSUFFICIENT_CAPACITY == (index = ringBuffer.tryClaim(MSG_TYPE_ID, length)))
                    {
                        Thread.yield();
                    }

                    final AtomicBuffer buffer = ringBuffer.buffer();
                    buffer.putInt(index, i);
                    buffer.putLong(index + SIZE_OF_INT, i * 20L);
                }
                finally
                {
                    ringBuffer.commit(index);
                }
            }
        }
    }

    class ClaimAbort extends Thread
    {
        ClaimAbort()
        {
            super("tryClaim-abort");
        }

        public void run()
        {
            final UnsafeBuffer srcBuffer = new UnsafeBuffer(new byte[1024]);
            for (int i = 0; i < REPETITIONS; i++)
            {

                int index = -1;
                try
                {
                    while (INSUFFICIENT_CAPACITY == (index = ringBuffer.tryClaim(MSG_TYPE_ID, SIZE_OF_INT)))
                    {
                        Thread.yield();
                    }
                    ringBuffer.buffer().putInt(index, -i); // should be skipped
                }
                finally
                {
                    ringBuffer.abort(index);
                }

                srcBuffer.putInt(16, i);
                while (!ringBuffer.write(MSG_TYPE_ID, srcBuffer, 16, SIZE_OF_INT))
                {
                    Thread.yield();
                }
            }
        }
    }
}
