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
package org.agrona.concurrent;

import org.agrona.UnsafeAccess;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.MatcherAssert.assertThat;

public class OffsetEpochNanoClockTest
{
    @Test
    public void shouldFindSaneEpochTimestamp()
    {
        final OffsetEpochNanoClock clock = new OffsetEpochNanoClock();

        assertSaneEpochTimeStamp(clock);
    }

    private void assertSaneEpochTimeStamp(final OffsetEpochNanoClock clock)
    {
        final long startInMs = System.currentTimeMillis();
        UnsafeAccess.UNSAFE.fullFence();
        parkForMeasurementPrecision();
        final long nanoTime = clock.nanoTime();
        final long nanoTimeInMs = NANOSECONDS.toMillis(nanoTime);
        parkForMeasurementPrecision();
        UnsafeAccess.UNSAFE.fullFence();
        final long endInMs = System.currentTimeMillis();

        assertThat(nanoTimeInMs, Matchers.lessThanOrEqualTo(endInMs));
        assertThat(nanoTimeInMs, Matchers.greaterThanOrEqualTo(startInMs));
    }

    private void parkForMeasurementPrecision()
    {
        LockSupport.parkNanos(1_000_000);
    }

    @Test
    public void shouldResampleSaneEpochTimestamp()
    {
        final OffsetEpochNanoClock clock = new OffsetEpochNanoClock();

        clock.sample();

        assertSaneEpochTimeStamp(clock);
    }

}
