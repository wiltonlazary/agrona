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
package org.agrona;

import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.SIZE_OF_INT;
import static org.agrona.BitUtil.SIZE_OF_LONG;

/**
 * Ring buffer for storing messages which can expand to accommodate the messages written into it. Message are written
 * and read in a FIFO order with capacity up to {@link #maxCapacity()}. Messages can be iterated via for-each methods
 * without consuming and having the option to begin iteration an offset from the current {@link #head()} position.
 * <p>
 * <b>Note:</b> This class is not thread safe.
 */
public class ExpandableRingBuffer
{
    /**
     * Maximum capacity to which the ring buffer can grow which is 1GB.
     */
    public static final int MAX_CAPACITY = 1 << 30;

    /**
     * Alignment in bytes for the beginning of message header.
     */
    public static final int HEADER_ALIGNMENT = SIZE_OF_LONG;

    /**
     * Length of encapsulating header.
     */
    public static final int HEADER_LENGTH = SIZE_OF_INT + SIZE_OF_INT;

    /**
     * Consumers of messages implement this interface and pass it to {@link #consume(MessageConsumer, int)}.
     */
    @FunctionalInterface
    public interface MessageConsumer
    {
        /**
         * Called for the processing of each message from a buffer in turn. Returning false aborts consumption
         * so that current message remains and any after it.
         *
         * @param buffer     containing the encoded message.
         * @param offset     at which the encoded message begins.
         * @param length     in bytes of the encoded message.
         * @param headOffset how much of an offset from {@link #head()} has passed for the end of this message.
         * @return true is the message was consumed otherwise false. Returning false aborts further consumption.
         */
        boolean onMessage(MutableDirectBuffer buffer, int offset, int length, int headOffset);
    }

    private static final int MESSAGE_LENGTH_OFFSET = 0;
    private static final int MESSAGE_TYPE_OFFSET = MESSAGE_LENGTH_OFFSET + SIZE_OF_INT;
    private static final int MESSAGE_TYPE_PADDING = 0;
    private static final int MESSAGE_TYPE_DATA = 1;

    private final int maxCapacity;
    private int capacity;
    private int mask;
    private long head;
    private long tail;
    private final UnsafeBuffer buffer = new UnsafeBuffer();
    private final boolean isDirect;

    /**
     * Create a new ring buffer which is initially compact and empty, has potential for {@link #MAX_CAPACITY},
     * and using a direct {@link ByteBuffer}.
     */
    public ExpandableRingBuffer()
    {
        this(0, MAX_CAPACITY, true);
    }

    /**
     * Create a new ring buffer providing configuration for initial and max capacity, plus whether it is direct or not.
     *
     * @param initialCapacity required in the buffer.
     * @param maxCapacity     the the buffer can expand to.
     * @param isDirect        is the {@link ByteBuffer} allocated direct or heap based.
     */
    public ExpandableRingBuffer(final int initialCapacity, final int maxCapacity, final boolean isDirect)
    {
        this.isDirect = isDirect;
        this.maxCapacity = maxCapacity;

        if (maxCapacity < 0 || maxCapacity > MAX_CAPACITY || !BitUtil.isPowerOfTwo(maxCapacity))
        {
            throw new IllegalArgumentException("illegal max capacity: " + maxCapacity);
        }

        if (0 == initialCapacity)
        {
            buffer.wrap(ArrayUtil.EMPTY_BYTE_ARRAY);
            return;
        }

        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity < 0 : " + initialCapacity);
        }

        capacity = BitUtil.findNextPositivePowerOfTwo(initialCapacity);
        if (capacity < 0)
        {
            throw new IllegalArgumentException("invalid initial capacity: " + initialCapacity);
        }

        mask = capacity - 1;
        buffer.wrap(isDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
    }

    /**
     * Is the {@link ByteBuffer} used for backing storage direct, that is off Java heap, or not.
     *
     * @return return true if direct {@link ByteBuffer} or false for heap based {@link ByteBuffer}.
     */
    public boolean isDirect()
    {
        return isDirect;
    }

    /**
     * The maximum capacity to which the buffer can expand.
     *
     * @return maximum capacity to which the buffer can expand.
     */
    public int maxCapacity()
    {
        return maxCapacity;
    }

    /**
     * Current capacity of the ring buffer in bytes.
     *
     * @return the current capacity of the ring buffer in bytes.
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * Size of the ring buffer currently populated in bytes.
     *
     * @return size of the ring buffer currently populated in bytes.
     */
    public int size()
    {
        return (int)(tail - head);
    }

    /**
     * Is the ring buffer currently empty.
     *
     * @return true if the ring buffer is empty otherwise false.
     */
    public boolean isEmpty()
    {
        return head == tail;
    }

    /**
     * Head position in the buffer from which bytes are consumed forward toward the {@link #tail()}.
     *
     * @return head position in the buffer from which bytes are consumed forward towards the {@link #tail()}.
     * @see #consume(MessageConsumer, int)
     */
    public long head()
    {
        return head;
    }

    /**
     * Tail position in the buffer at which new bytes are appended.
     *
     * @return tail position in the buffer at which new bytes are appended.
     * @see #append(DirectBuffer, int, int)
     */
    public long tail()
    {
        return tail;
    }

    /**
     * Reset the buffer with a new capacity and empty state. Buffer capacity will grow or shrink as necessary.
     *
     * @param requiredCapacity for the ring buffer. If the same as exiting capacity then no adjustment is made.
     */
    public void reset(final int requiredCapacity)
    {
        if (requiredCapacity < 0)
        {
            throw new IllegalArgumentException("required capacity <= 0 : " + requiredCapacity);
        }

        final int newCapacity = BitUtil.findNextPositivePowerOfTwo(requiredCapacity);
        if (newCapacity < 0)
        {
            throw new IllegalArgumentException("invalid required capacity: " + requiredCapacity);
        }

        if (newCapacity > maxCapacity)
        {
            throw new IllegalArgumentException(
                "requiredCapacity=" + requiredCapacity + " > maxCapacity=" + maxCapacity);
        }

        if (newCapacity != capacity)
        {
            capacity = newCapacity;
            mask = newCapacity == 0 ? 0 : newCapacity - 1;
            buffer.wrap(isDirect ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity));
        }

        head = 0;
        tail = 0;
    }

    /**
     * Iterate encoded contents and pass messages to the {@link MessageConsumer} which can stop by returning false.
     *
     * @param messageConsumer to which the encoded messages are passed.
     * @param limit           for the number of entries to iterate over.
     * @return count of bytes iterated.
     * @see MessageConsumer
     */
    public int forEach(final MessageConsumer messageConsumer, final int limit)
    {
        long position = head;
        int count = 0;

        while (count < limit && position < tail)
        {
            final int offset = (int)position & mask;
            final int length = buffer.getInt(offset + MESSAGE_LENGTH_OFFSET);
            final int typeId = buffer.getInt(offset + MESSAGE_TYPE_OFFSET);
            final int alignedLength = BitUtil.align(length, HEADER_ALIGNMENT);
            position += alignedLength;

            if (MESSAGE_TYPE_PADDING != typeId)
            {
                final int headOffset = (int)(position - head);
                if (!messageConsumer.onMessage(buffer, offset + HEADER_LENGTH, length - HEADER_LENGTH, headOffset))
                {
                    break;
                }

                ++count;
            }
        }

        return (int)(position - head);
    }

    /**
     * Iterate encoded contents and pass messages to the {@link MessageConsumer} which can stop by returning false.
     *
     * @param headOffset      offset from {@link #head} which must be &lt;= {@link #tail()}, and be the start a message.
     * @param messageConsumer to which the encoded messages are passed.
     * @param limit           for the number of entries to iterate over.
     * @return count of bytes iterated.
     * @see MessageConsumer
     */
    public int forEach(final int headOffset, final MessageConsumer messageConsumer, final int limit)
    {
        if (headOffset < 0 || headOffset > size())
        {
            throw new IllegalArgumentException("size=" + size() + " : headOffset=" + headOffset);
        }

        if (!BitUtil.isAligned(headOffset, HEADER_ALIGNMENT))
        {
            throw new IllegalArgumentException(headOffset + " not aligned to " + HEADER_ALIGNMENT);
        }

        final long initialPosition = head + headOffset;
        long position = initialPosition;
        int count = 0;

        while (count < limit && position < tail)
        {
            final int offset = (int)position & mask;
            final int length = buffer.getInt(offset + MESSAGE_LENGTH_OFFSET);
            final int typeId = buffer.getInt(offset + MESSAGE_TYPE_OFFSET);
            final int alignedLength = BitUtil.align(length, HEADER_ALIGNMENT);
            position += alignedLength;

            if (MESSAGE_TYPE_PADDING != typeId)
            {
                final int result = (int)(position - head);
                if (!messageConsumer.onMessage(buffer, offset + HEADER_LENGTH, length - HEADER_LENGTH, result))
                {
                    break;
                }

                ++count;
            }
        }

        return (int)(position - initialPosition);
    }

    /**
     * Consume messages up to a limit and pass them to the {@link MessageConsumer}.
     *
     * @param messageConsumer to which the encoded messages are passed.
     * @param messageLimit    on the number of messages to consume per read operation.
     * @return the number of bytes consumed
     * @see MessageConsumer
     */
    public int consume(final MessageConsumer messageConsumer, final int messageLimit)
    {
        final int bytes;
        int count = 0;
        long position = head;

        try
        {
            while (count < messageLimit && position < tail)
            {
                final int offset = (int)position & mask;
                final int length = buffer.getInt(offset + MESSAGE_LENGTH_OFFSET);
                final int typeId = buffer.getInt(offset + MESSAGE_TYPE_OFFSET);
                final int alignedLength = BitUtil.align(length, HEADER_ALIGNMENT);

                position += alignedLength;

                if (MESSAGE_TYPE_PADDING != typeId)
                {
                    final int headOffset = (int)(position - head);
                    if (!messageConsumer.onMessage(buffer, offset + HEADER_LENGTH, length - HEADER_LENGTH, headOffset))
                    {
                        position -= alignedLength;
                        break;
                    }

                    ++count;
                }
            }
        }
        finally
        {
            bytes = (int)(position - head);
            head = position;
        }

        return bytes;
    }

    /**
     * Append a message into the ring buffer, expanding the buffer if required.
     *
     * @param srcBuffer containing the encoded message.
     * @param srcOffset within the buffer at which the message begins.
     * @param srcLength of the encoded message in the buffer.
     * @return true if successful otherwise false if {@link #maxCapacity()} is reached.
     */
    public boolean append(final DirectBuffer srcBuffer, final int srcOffset, final int srcLength)
    {
        final int headOffset = (int)head & mask;
        final int tailOffset = (int)tail & mask;
        final int alignedLength = BitUtil.align(HEADER_LENGTH + srcLength, HEADER_ALIGNMENT);

        final int totalRemaining = capacity - (int)(tail - head);
        if (alignedLength > totalRemaining)
        {
            resize(alignedLength);
        }
        else if (tailOffset >= headOffset)
        {
            final int toEndRemaining = capacity - tailOffset;
            if (alignedLength > toEndRemaining)
            {
                if (alignedLength <= (totalRemaining - toEndRemaining))
                {
                    buffer.putInt(tailOffset + MESSAGE_LENGTH_OFFSET, toEndRemaining);
                    buffer.putInt(tailOffset + MESSAGE_TYPE_OFFSET, MESSAGE_TYPE_PADDING);
                    tail += toEndRemaining;
                }
                else
                {
                    resize(alignedLength);
                }
            }
        }

        final int newTotalRemaining = capacity - (int)(tail - head);
        if (alignedLength > newTotalRemaining)
        {
            return false;
        }

        writeMessage(srcBuffer, srcOffset, srcLength);
        tail += alignedLength;
        return true;
    }

    private void resize(final int newMessageLength)
    {
        final int newCapacity = BitUtil.findNextPositivePowerOfTwo(capacity + newMessageLength);
        if (newCapacity < capacity || newCapacity > maxCapacity)
        {
            return;
        }

        final UnsafeBuffer tempBuffer = new UnsafeBuffer(
            isDirect ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity));

        final int headOffset = (int)head & mask;
        final int remaining = (int)(tail - head);
        final int firstCopyLength = Math.min(remaining, capacity - headOffset);
        tempBuffer.putBytes(0, buffer, headOffset, firstCopyLength);
        int tailOffset = firstCopyLength;

        if (firstCopyLength < remaining)
        {
            final int length = remaining - firstCopyLength;
            tempBuffer.putBytes(firstCopyLength, buffer, 0, length);
            tailOffset += length;
        }

        buffer.wrap(tempBuffer);
        capacity = newCapacity;
        mask = newCapacity - 1;
        head = 0;
        tail = tailOffset;
    }

    private void writeMessage(final DirectBuffer srcBuffer, final int srcOffset, final int srcLength)
    {
        final int offset = (int)tail & mask;

        buffer.putInt(offset + MESSAGE_LENGTH_OFFSET, HEADER_LENGTH + srcLength);
        buffer.putInt(offset + MESSAGE_TYPE_OFFSET, MESSAGE_TYPE_DATA);
        buffer.putBytes(offset + HEADER_LENGTH, srcBuffer, srcOffset, srcLength);
    }
}
