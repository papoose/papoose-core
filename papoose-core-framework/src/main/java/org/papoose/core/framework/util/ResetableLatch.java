/**
 *
 * Copyright 2007 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.papoose.core.framework.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @version $Revision$ $Date$
 */
public class ResetableLatch
{
    private final AtomicReference<CountDownLatch> reference = new AtomicReference<CountDownLatch>();

    public void set()
    {
        reference.set(new CountDownLatch(1));
    }

    public void clear()
    {
        reference.set(null);
    }

    public void await(long milliseconds) throws InterruptedException
    {
        CountDownLatch latch = reference.get();
        if (latch != null) latch.await(milliseconds, TimeUnit.MILLISECONDS);
    }
}
