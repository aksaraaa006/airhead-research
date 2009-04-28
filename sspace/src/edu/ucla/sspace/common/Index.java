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

package edu.ucla.sspace.common;

public class Index {
  public final String word;

  public final String document;

  public Index(String word, String document) {
    if (word == null || document == null)
      throw new IllegalArgumentException("arguments cannot be null");
    this.word = word;
    this.document = document;
  }

  public boolean equals(Object o) {
    if (o instanceof Index) {
      Index i = (Index)o;
      return word.equals(i.word) && document.equals(i.document);
    }
    return false;
  }
  
  public int hashCode() {
    return word.hashCode() ^ document.hashCode();
  }

  public String toString() {
    return "(" + word + "," + document + ")";
  }
}
