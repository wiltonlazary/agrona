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
package org.agrona.concurrent.broadcast;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

/**
 * Receiver that copies messages which have been broadcast to enable a simpler API for the client.
 */
public class CopyBroadcastReceiver
{
    /**
     * Default length for the scratch buffer for copying messages into.
     */
    public static final int SCRATCH_BUFFER_LENGTH = 4096;

    private final BroadcastReceiver receiver;
    private final MutableDirectBuffer scratchBuffer;

    /**
     * Wrap a {@link BroadcastReceiver} to simplify the API for receiving messages.
     *
     * @param receiver      to be wrapped.
     * @param scratchBuffer to be used for copying receive buffers.
     */
    public CopyBroadcastReceiver(final BroadcastReceiver receiver, final MutableDirectBuffer scratchBuffer)
    {
        this.receiver = receiver;
        this.scratchBuffer = scratchBuffer;
    }

    /**
     * Get the underlying {@link BroadcastReceiver} which this is wrapping and copying out of.
     *
     * @return the underlying {@link BroadcastReceiver} which this is wrapping and copying out of.
     */
    public BroadcastReceiver broadcastReceiver()
    {
        return receiver;
    }

    /**
     * Wrap a {@link BroadcastReceiver} to simplify the API for receiving messages.
     *
     * @param receiver            to be wrapped.
     * @param scratchBufferLength is the maximum length of a message to be copied when receiving.
     */
    public CopyBroadcastReceiver(final BroadcastReceiver receiver, final int scratchBufferLength)
    {
        this.receiver = receiver;
        scratchBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(scratchBufferLength));
    }

    /**
     * Wrap a {@link BroadcastReceiver} to simplify the API for receiving messages.
     *
     * @param receiver to be wrapped.
     */
    public CopyBroadcastReceiver(final BroadcastReceiver receiver)
    {
        this(receiver, SCRATCH_BUFFER_LENGTH);
    }

    /**
     * Receive one message from the broadcast buffer.
     *
     * @param handler to be called for each message received.
     * @return the number of messages that have been received.
     */
    public int receive(final MessageHandler handler)
    {
        int messagesReceived = 0;
        final BroadcastReceiver receiver = this.receiver;
        final long lastSeenLappedCount = receiver.lappedCount();

        if (receiver.receiveNext())
        {
            if (lastSeenLappedCount != receiver.lappedCount())
            {
                throw new IllegalStateException("unable to keep up with broadcast");
            }

            final int length = receiver.length();
            final int capacity = scratchBuffer.capacity();
            if (length > capacity && !scratchBuffer.isExpandable())
            {
                throw new IllegalStateException(
                    "buffer required length of " + length + " but only has " + capacity);
            }

            final int msgTypeId = receiver.typeId();
            scratchBuffer.putBytes(0, receiver.buffer(), receiver.offset(), length);

            if (!receiver.validate())
            {
                throw new IllegalStateException("unable to keep up with broadcast");
            }

            handler.onMessage(msgTypeId, scratchBuffer, 0, length);

            messagesReceived = 1;
        }

        return messagesReceived;
    }
}
