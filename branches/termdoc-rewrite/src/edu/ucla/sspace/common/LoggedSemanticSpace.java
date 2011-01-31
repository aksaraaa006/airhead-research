/*
 * Copyright 2010 Keith Stevens
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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class provides logging utilities for all semantic space classes, it
 * provides no other utilities or methods.  {@link #verbose(String, Object...)
 * verbose} prints all messages to the {@link Level#FINE} level if the logger
 * has access to this level.  {@link #verbose(string, Object...) verbose} prints
 * all messages to {@link Level#INFO} if the logger has access to this level.
 *
 * @author Keith Stevens
 */
public abstract class LoggedSemanticSpace implements SemanticSpace {

    private static final Logger LOGGER = 
        Logger.getLogger(LoggedSemanticSpace.class.getName());

    /**
     * Prints {@link Level#FINE} messages.
     */
    public void info(String format, Object... args) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(String.format(format, args));
    }

    /**
     * Prints {@link Level#INFO} messages.
     */
    public void verbose(String format, Object... args) {
        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info(String.format(format, args));
    }
}
