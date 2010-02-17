/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.util;

import java.util.concurrent.BlockingQueue;


/**
 * A daemon thread that continuously dequeues {@code Runnable} instances from a
 * queue and executes them.  This class is intended to be used with a {@link
 * java.util.concurrent.Semaphore Semaphore}, whereby work is added the to the
 * queue and the semaphore indicates when processing has finished.
 *
 * @author David Jurgens
 */
public class WorkerThread extends Thread {

    /**
     * A static variable to indicate which instance of the class the current
     * thread is in its name.
     */
    private static int threadInstanceCount;

    /**
     * The queue from which work items will be taken
     */
    private final BlockingQueue<Runnable> workQueue;
    
    public WorkerThread(BlockingQueue<Runnable> workQueue) {
        this.workQueue = workQueue;
        setDaemon(true);
        synchronized(WorkerThread.class) {
            setName("WorkerThread-" + (threadInstanceCount++));
        }
    }

    /**
     * Continuously dequeues {@code Runnable} instances from the work queue and
     * execute them.
     */
    public void run() {
        while (true) {
            try {
                Runnable r = workQueue.take();
                r.run();
            } catch (InterruptedException ie) {
                throw new Error(ie);
            }
        }
    }
}
