/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.vector;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AtomicVector implements Vector {
    Vector vector;

    private final Lock readLock;
    private final Lock writeLock;

    public AtomicVector(Vector v) {
        vector = v;

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();
    }
    
    public void addVector(Vector other) {
        writeLock.lock();
        vector.addVector(other);
        writeLock.unlock();
    }

    public double addAndGet(int index, double delta) {
        writeLock.lock();
        double value = vector.add(index, delta);
        writeLock.unlock();
        return value;
    }

    public double getAndAdd(int index, double delta) {
        writeLock.lock();
        double value = vector.get(index);
        vector.set(index, value + delta);
        writeLock.unlock();
        return value;
    }

    public double add(int index, double delta) {
        writeLock.lock();
        double value = vector.add(index, delta);
        writeLock.unlock();
        return value;
    }

    public double get(int index) {
        readLock.lock();
        double value = vector.get(index);
        readLock.unlock();
        return value;
    }

    public void set(double[] values) {
        writeLock.lock();
        vector.set(values);
        writeLock.unlock();
    }

    public void set(int index, double value) {
        writeLock.lock();
        vector.set(index, value);
        writeLock.unlock();
    }

    public double[] toArray(int size) {
        readLock.lock();
        double[] array = vector.toArray(size);
        readLock.lock();
        return array;
    }

    public int length() {
        readLock.lock();
        int length = vector.length();
        readLock.unlock();
        return length;
    }
}