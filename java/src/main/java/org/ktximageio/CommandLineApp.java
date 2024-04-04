package org.ktximageio;

import org.ktximageio.Options.OptionResolver;
import org.ktximageio.Options.Result;
import org.ktximageio.output.TonemappWindow.WindowListener;

/**
 * Base for a command line app
 */
public abstract class CommandLineApp implements WindowListener {

    private final String[] arguments;
    protected String outfile;
    protected String[] infiles;
    protected Options options = new Options();
    protected int index = 0;

    protected CommandLineApp(String[] args, OptionResolver optionResolver) {
        arguments = args;
        init(optionResolver);
    }

    private void init(OptionResolver optionResolver) {
        parseCommandLineArguments(arguments, optionResolver);
        validateParameters(arguments);
    }

    protected abstract void run();

    protected abstract void validateParameters(String[] args);

    protected abstract boolean hasArgs();

    /**
     * Returns the number of arguments, this is the length of the array of args used when calling constructor.
     * 
     * @return
     */
    protected int getArgumentCount() {
        return arguments == null ? 0 : arguments.length;
    }

    /**
     * Returns the infiles for processing or null
     * 
     * @return
     */
    public String[] getInfiles() {
        return infiles;
    }

    /**
     * Returns the name of the output file
     * 
     * @return
     */
    public String getOutfile() {
        return outfile;
    }

    /**
     * Fetch the output filename from args array at position index, index is incremented after being read.
     * 
     * @param args
     * @return
     */
    protected String parseOutFile(String[] args) {
        return index < args.length ? args[index++] : null;
    }

    /**
     * Fetch infiles from the remaining array elements at index -> args.length
     * Infiles must be last in list of parameters
     * 
     * @param args
     * @return
     */
    private String[] parseInfiles(String[] args) {
        if (index >= args.length) {
            return null;
        }
        String[] infileStrs = new String[args.length - index];
        for (int i = 0; i < infileStrs.length; i++) {
            infileStrs[i] = args[index++];
        }
        return infileStrs;
    }

    /**
     * Exit the app with an error message
     * 
     * @param reason
     */
    protected void exit(String reason) {
        System.err.println(reason);
        System.exit(1);
    }

    private void parseCommandLineArguments(String[] args, OptionResolver resolver) {
        if (!hasArgs()) {
            exit("Missing input parameters");
        }
        parseOptions(args, resolver);
        outfile = parseOutFile(args);
        infiles = parseInfiles(args);
    }

    private void parseOptions(String[] args, OptionResolver resolver) {
        while (index < args.length && Options.isOption(args[index])) {
            Result result = options.add(args[index], resolver);
            switch (result) {
                case OK:
                    break;
                case ALREADY_SET:
                case UNKNOWN:
                case INVALID_PREFIX:
                    exit("Exit with error " + result + ", for arguments \"" + args[index] + "\"");
                    break;
                default:
                    throw new IllegalArgumentException("Not implemented for result " + result);
            }
            index++;
        }
    }

    /**
     * Check if infiles has been set, if not the app is aborted by calling {@link #exit(String)}
     */
    protected void checkInfiles() {
        if (infiles == null) {
            exit("Missing infile");
        }
        System.out.println("Outputting to : " + outfile + ", using inputs:");
        for (String f : infiles) {
            System.out.println(f);
        }
    }

}
