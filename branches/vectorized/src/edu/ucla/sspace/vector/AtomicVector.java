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
