/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.util.*;


/**
 * A special-purpose tuple {@link Writable} for storing text and int values
 * together in one object.  This class is designed to record the co-occurrence a
 * term and a relative offset indicating the distance from the focus term.
 */
public class TextIntWritable implements WritableComparable<TextIntWritable> {

    /**
     * The term that co-occurred with the focus term
     */
    Text t;

    /**
     * The relative position of the co-occurring term from the focus term.
     * Note that if this value is negative, the co-occurring term appeared
     * <i>before</i> the focus term.
     */
    int position;

    /**
     * Creates an empty {@code TextIntWritable} with no text and no position.
     */
    public TextIntWritable() {
        t = new Text();
        position = 0;
    }

    public TextIntWritable(String s, int position) {
        this.t = new Text(s);
        this.position = position;
    }

    public TextIntWritable(Text t, int position) {
        this.t = t;
        this.position = position;
    }

    public static TextIntWritable read(DataInput in) throws IOException {
        TextIntWritable tiw = new TextIntWritable();
        tiw.t.readFields(in);
        tiw.position = in.readInt();
        return tiw;
    }

    public int compareTo(TextIntWritable o) {
        int c = t.compareTo(o.t);
        if (c != 0)
            return c;
        return position - o.position;
    } 

    public int hashCode() {
        return t.hashCode() 
            ^ position;
    }

    public boolean equals(Object o) {
        if (o instanceof TextIntWritable) {
            TextIntWritable tiw = (TextIntWritable)o;
            return t.equals(tiw.t) &&
                position == tiw.position;
        }
        return false;
    }
        
    public void readFields(DataInput in) throws IOException {
        t.readFields(in);
        position = in.readInt();
    }

    public void write(DataOutput out) throws IOException {
        t.write(out);
        out.writeInt(position);
    }

    public String toString() {
        return t + "\t" + position;
    }
}
