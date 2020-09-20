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

import org.agrona.collections.MutableLong;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class DeadlineTimerWheelTest
{
    private static final TimeUnit TIME_UNIT = TimeUnit.NANOSECONDS;
    private static final int RESOLUTION =
        BitUtil.findNextPositivePowerOfTwo((int)TimeUnit.MILLISECONDS.toNanos(1));

    @Test
    public void shouldExceptionOnNonPowerOfTwoTicksPerWheel()
    {
        assertThrows(IllegalArgumentException.class, () -> new DeadlineTimerWheel(TIME_UNIT, 0, 16, 10));
    }

    @Test
    public void shouldExceptionOnNonPowerOfTwoResolution()
    {
        assertThrows(IllegalArgumentException.class, () -> new DeadlineTimerWheel(TIME_UNIT, 0, 17, 8));
    }

    @Test
    public void shouldDefaultConfigure()
    {
        final int startTime = 7;
        final int tickResolution = 16;
        final int ticksPerWheel = 8;
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, startTime, tickResolution, ticksPerWheel);

        assertEquals(wheel.timeUnit(), TIME_UNIT);
        assertEquals(wheel.tickResolution(), tickResolution);
        assertEquals(wheel.ticksPerWheel(), ticksPerWheel);
        assertEquals(wheel.startTime(), startTime);
    }

    @Test
    public void shouldBeAbleToScheduleTimerOnEdgeOfTick()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 1024);

            final long deadline = 5 * wheel.tickResolution();
            final long id = wheel.scheduleTimer(deadline);
            assertEquals(wheel.deadline(id), deadline);

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value);

            // this is the first tick after the timer, so it should be on this edge
            assertThat(firedTimestamp.value, is(6 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldHandleNonZeroStartTime()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 100 * RESOLUTION;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 1024);

            final long id = wheel.scheduleTimer(controlTimestamp + (5 * wheel.tickResolution()));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value);

            // this is the first tick after the timer, so it should be on this edge
            assertThat(firedTimestamp.value, is(106 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldHandleNanoTimeUnitTimers()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 1024);

            final long id = wheel.scheduleTimer(controlTimestamp + (5 * wheel.tickResolution()) + 1);

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value);

            // this is the first tick after the timer, so it should be on this edge
            assertThat(firedTimestamp.value, is(6 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldHandleMultipleRounds()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 16);

            final long id = wheel.scheduleTimer(controlTimestamp + (63 * wheel.tickResolution()));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value);

            // this is the first tick after the timer, so it should be on this edge
            assertThat(firedTimestamp.value, is(64 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldBeAbleToCancelTimer()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 256);

            final long id = wheel.scheduleTimer(controlTimestamp + (63 * wheel.tickResolution()));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value && controlTimestamp < (16 * wheel.tickResolution()));

            assertTrue(wheel.cancelTimer(id));
            assertFalse(wheel.cancelTimer(id));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value && controlTimestamp < (128 * wheel.tickResolution()));

            assertThat(firedTimestamp.value, is(-1L));
        });
    }

    @Test
    public void shouldHandleExpiringTimersInPreviousTicks()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 256);

            final long id = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));

            final long pollStartTimeNs = 32 * wheel.tickResolution();
            controlTimestamp += pollStartTimeNs;

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                if (wheel.currentTickTime() > pollStartTimeNs)
                {
                    controlTimestamp += wheel.tickResolution();
                }
            }
            while (-1 == firedTimestamp.value && controlTimestamp < (128 * wheel.tickResolution()));

            assertThat(firedTimestamp.value, is(pollStartTimeNs));
        });
    }

    @Test
    public void shouldHandleMultipleTimersInDifferentTicks()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp1 = new MutableLong(-1);
            final MutableLong firedTimestamp2 = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 256);

            final long id1 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));
            final long id2 = wheel.scheduleTimer(controlTimestamp + (23 * wheel.tickResolution()));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        if (timerId == id1)
                        {
                            firedTimestamp1.value = now;
                        }
                        else if (timerId == id2)
                        {
                            firedTimestamp2.value = now;
                        }

                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

            assertThat(firedTimestamp1.value, is(16 * wheel.tickResolution()));
            assertThat(firedTimestamp2.value, is(24 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldHandleMultipleTimersInSameTickSameRound()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp1 = new MutableLong(-1);
            final MutableLong firedTimestamp2 = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);

            final long id1 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));
            final long id2 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        if (timerId == id1)
                        {
                            firedTimestamp1.value = now;
                        }
                        else if (timerId == id2)
                        {
                            firedTimestamp2.value = now;
                        }

                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

            assertThat(firedTimestamp1.value, is(16 * wheel.tickResolution()));
            assertThat(firedTimestamp2.value, is(16 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldHandleMultipleTimersInSameTickDifferentRound()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp1 = new MutableLong(-1);
            final MutableLong firedTimestamp2 = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);

            final long id1 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));
            final long id2 = wheel.scheduleTimer(controlTimestamp + (23 * wheel.tickResolution()));

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        if (timerId == id1)
                        {
                            firedTimestamp1.value = now;
                        }
                        else if (timerId == id2)
                        {
                            firedTimestamp2.value = now;
                        }

                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

            assertThat(firedTimestamp1.value, is(16 * wheel.tickResolution()));
            assertThat(firedTimestamp2.value, is(24 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldLimitExpiringTimers()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp1 = new MutableLong(-1);
            final MutableLong firedTimestamp2 = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);

            final long id1 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));
            final long id2 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));

            int numExpired = 0;

            do
            {
                numExpired += wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id1));
                        firedTimestamp1.value = now;
                        return true;
                    },
                    1);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp1.value && -1 == firedTimestamp2.value);

            assertThat(numExpired, is(1));

            do
            {
                numExpired += wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id2));
                        firedTimestamp2.value = now;
                        return true;
                    },
                    1);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp1.value && -1 == firedTimestamp2.value);

            assertThat(numExpired, is(2));

            assertThat(firedTimestamp1.value, is(16 * wheel.tickResolution()));
            assertThat(firedTimestamp2.value, is(17 * wheel.tickResolution()));
        });
    }

    @Test
    public void shouldHandleFalseReturnToExpireTimerAgain()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp1 = new MutableLong(-1);
            final MutableLong firedTimestamp2 = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);

            final long id1 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));
            final long id2 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));

            int numExpired = 0;

            do
            {
                numExpired += wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        if (timerId == id1)
                        {
                            if (-1 == firedTimestamp1.value)
                            {
                                firedTimestamp1.value = now;
                                return false;
                            }

                            firedTimestamp1.value = now;
                        }
                        else if (timerId == id2)
                        {
                            firedTimestamp2.value = now;
                        }

                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

            assertThat(firedTimestamp1.value, is(17 * wheel.tickResolution()));
            assertThat(firedTimestamp2.value, is(17 * wheel.tickResolution()));
            assertThat(numExpired, is(2));
        });
    }

    @Test
    public void shouldCopeWithExceptionFromHandler()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 0;
            final MutableLong firedTimestamp1 = new MutableLong(-1);
            final MutableLong firedTimestamp2 = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);

            final long id1 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));
            final long id2 = wheel.scheduleTimer(controlTimestamp + (15 * wheel.tickResolution()));

            int numExpired = 0;
            Exception e = null;
            do
            {
                try
                {
                    numExpired += wheel.poll(
                        controlTimestamp,
                        (timeUnit, now, timerId) ->
                        {
                            if (timerId == id1)
                            {
                                firedTimestamp1.value = now;
                                throw new IllegalStateException();
                            }
                            else if (timerId == id2)
                            {
                                firedTimestamp2.value = now;
                            }

                            return true;
                        },
                        Integer.MAX_VALUE);

                    controlTimestamp += wheel.tickResolution();
                }
                catch (final Exception ex)
                {
                    e = ex;
                }
            }
            while (-1 == firedTimestamp1.value || -1 == firedTimestamp2.value);

            assertThat(firedTimestamp1.value, is(16 * wheel.tickResolution()));
            assertThat(firedTimestamp2.value, is(16 * wheel.tickResolution()));
            assertThat(numExpired, is((1)));
            assertThat(wheel.timerCount(), is(0L));
            assertNotNull(e);
        });
    }

    @Test
    public void shouldBeAbleToIterateOverTimers()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            final long controlTimestamp = 0;
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);
            final long deadline1 = controlTimestamp + (15 * wheel.tickResolution());
            final long deadline2 = controlTimestamp + ((15 + 7) * wheel.tickResolution());

            final long id1 = wheel.scheduleTimer(deadline1);
            final long id2 = wheel.scheduleTimer(deadline2);

            final Map<Long, Long> timerIdByDeadlineMap = new HashMap<>();

            wheel.forEach(timerIdByDeadlineMap::put);

            assertThat(timerIdByDeadlineMap.size(), is(2));
            assertThat(timerIdByDeadlineMap.get(deadline1), is(id1));
            assertThat(timerIdByDeadlineMap.get(deadline2), is(id2));
        });
    }

    @Test
    public void shouldClearOutScheduledTimers()
    {
        final long controlTimestamp = 0;
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);
        final long deadline1 = controlTimestamp + (15 * wheel.tickResolution());
        final long deadline2 = controlTimestamp + ((15 + 7) * wheel.tickResolution());

        final long id1 = wheel.scheduleTimer(deadline1);
        final long id2 = wheel.scheduleTimer(deadline2);

        wheel.clear();

        assertThat(wheel.timerCount(), is(0L));
        assertThat(wheel.deadline(id1), is(DeadlineTimerWheel.NULL_DEADLINE));
        assertThat(wheel.deadline(id2), is(DeadlineTimerWheel.NULL_DEADLINE));
    }

    @Test
    public void shouldNotAllowResetWhenTimersActive()
    {
        final long controlTimestamp = 0;
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 8);

        wheel.scheduleTimer(controlTimestamp + 100);
        assertThrows(IllegalStateException.class, () -> wheel.resetStartTime(controlTimestamp + 1));
    }

    @Test
    public void shouldAdvanceWheelToLaterTime()
    {
        final long startTime = 0;
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, startTime, RESOLUTION, 8);

        wheel.scheduleTimer(startTime + 100000);

        final long currentTickTime = wheel.currentTickTime();
        wheel.currentTickTime(currentTickTime * 5);

        assertThat(wheel.currentTickTime(), is(currentTickTime * 6));
    }

    @Test
    public void shouldScheduleDeadlineInThePast()
    {
        assertTimeoutPreemptively(ofSeconds(1), () ->
        {
            long controlTimestamp = 100 * RESOLUTION;
            final MutableLong firedTimestamp = new MutableLong(-1);
            final DeadlineTimerWheel wheel = new DeadlineTimerWheel(TIME_UNIT, controlTimestamp, RESOLUTION, 1024);

            final long deadline = controlTimestamp - 3;
            final long id = wheel.scheduleTimer(deadline);

            do
            {
                wheel.poll(
                    controlTimestamp,
                    (timeUnit, now, timerId) ->
                    {
                        assertThat(timerId, is(id));
                        firedTimestamp.value = now;
                        return true;
                    },
                    Integer.MAX_VALUE);

                controlTimestamp += wheel.tickResolution();
            }
            while (-1 == firedTimestamp.value);

            assertThat(firedTimestamp.value, greaterThan(deadline));
        });
    }

    @Test
    public void shouldExpandTickAllocation()
    {
        final int tickAllocation = 4;
        final int ticksPerWheel = 8;
        final DeadlineTimerWheel wheel = new DeadlineTimerWheel(
            TIME_UNIT, 0, RESOLUTION, ticksPerWheel, tickAllocation);

        final int timerCount = tickAllocation + 1;
        final long[] timerIds = new long[timerCount];

        for (int i = 0; i < timerCount; i++)
        {
            timerIds[i] = wheel.scheduleTimer(i + 1L);
        }

        for (int i = 0; i < timerCount; i++)
        {
            assertThat(wheel.deadline(timerIds[i]), is(i + 1L));
        }

        final Map<Long, Long> deadlineByTimerId = new HashMap<>();
        final int expiredCount = wheel.poll(
            timerCount + 1L,
            (timeUnit, now, timerId) ->
            {
                deadlineByTimerId.put(timerId, now);
                return true;
            },
            timerCount);

        assertThat(expiredCount, is(timerCount));
        assertThat(deadlineByTimerId.size(), is(timerCount));
    }
}
