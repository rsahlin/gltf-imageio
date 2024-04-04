package org.ktximageio;

import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.jdt.annotation.NonNull;

public class Options {

    public static class OptionResolver {

        final Option[] options;

        public OptionResolver(@NonNull Option[] opts) {
            options = opts;
        }

        /**
         * Returns the option for the parameter string
         * 
         * @param parameter
         * @return
         */
        public Option getOption(String parameter) {
            for (Option o : options) {
                if (parameter.equalsIgnoreCase(o.getKey())) {
                    return o;
                }
            }
            return null;
        }
    }

    /**
     * The purpose of this interface is to handle user options, by matching user input parameters to String value
     * returned.
     *
     */
    public interface Option {
        /**
         * Returns the key for this option, this is the String that is used to match against user input str.
         * 
         * @return The key (string identifier, for this Option.
         */
        String getKey();
    }

    public static final String PREFIX = "--";

    HashMap<String, String[]> options = new HashMap<String, String[]>();

    public enum Result {
        OK(),
        INVALID_PREFIX(),
        ALREADY_SET(),
        UNKNOWN();
    }

    /**
     * Call this method to parse and add command line string arguments as options.
     * Checks if option starts with {@link #PREFIX}, if not {@link Result#INVALID_PREFIX} is returned.
     * A check is made if option is already set, if so {@link Result#ALREADY_SET} is returned.
     * Otherwise {@link OptionResolver#getOption(String)} is used to get option(s) from the option string.
     * 
     * @param option
     * @param resolver
     * @return
     */
    public Result add(@NonNull String option, @NonNull OptionResolver resolver) {
        if (!option.startsWith(PREFIX)) {
            return Result.INVALID_PREFIX;
        }
        if (!options.isEmpty()) {
            return Result.ALREADY_SET;
        }
        StringTokenizer st = new StringTokenizer(option.substring(PREFIX.length()));
        Option p = resolver.getOption(st.nextToken());
        if (p == null) {
            return Result.UNKNOWN;
        }
        int tokenCount = st.countTokens();
        if (tokenCount > 0) {
            // TODO - iterate tokens and add as String[]
            options.put(p.getKey(), null);
        } else {
            options.put(p.getKey(), null);
        }
        return Result.OK;
    }

    /**
     * Returns true if option string starts with {@link #PREFIX}, ie returns true of it is an option string.
     * 
     * @param option
     * @return True if option is an option string.
     */
    public static boolean isOption(@NonNull String option) {
        return option.startsWith(PREFIX);
    }

    /**
     * Returns true if the option is set
     * 
     * @param option
     * @return
     */
    public boolean isOptionSet(Option option) {
        return options.containsKey(option.getKey());
    }

    @Override
    public String toString() {
        return options.toString();
    }
}
