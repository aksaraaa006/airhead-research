package edu.ucla.sspace.hermit;


/**
 * A marker interface for any bottom up implmementation of Hermit.
 */
public interface BottomUpHermit {
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.hermit.BottomUpHermit";

    public static final String DROP_PERCENTAGE =
        PROPERTY_PREFIX + ".dropPercentage";
}
