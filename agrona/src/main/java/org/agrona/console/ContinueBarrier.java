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
package org.agrona.console;

import java.util.Scanner;

/**
 * Barrier to block the calling thread until a command is given on the command line.
 */
public class ContinueBarrier
{
    final String label;

    /**
     * Create a barrier that will display the provided label and interact
     * via {@link System#out}, {@link System#in} and {@link java.util.Scanner}.
     *
     * @param label to prompt the user.
     */
    public ContinueBarrier(final String label)
    {
        this.label = label;
    }

    /**
     * Await for input that matches the provided command.
     *
     * @return true if y otherwise false.
     */
    public boolean await()
    {
        System.out.format("%n%s (y/n): ", label).flush();
        final Scanner in = new Scanner(System.in);
        final String line = in.nextLine();

        return "y".equalsIgnoreCase(line);
    }
}
