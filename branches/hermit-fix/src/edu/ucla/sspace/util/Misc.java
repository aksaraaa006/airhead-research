package edu.ucla.sspace.util;

public class Misc {

    /**
     * Returns an arbitrary object instance based on a class name.
     *
     * @param className The name of a desired class to instantiate.
     */
    public static Object getObjectInstance(String className) {
        try {
            Class clazz = Class.forName(className);
            return clazz.newInstance();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
