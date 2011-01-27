

package edu.ucla.sspace.common;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
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
