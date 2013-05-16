package edu.ucsc.ib.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ShellProcess {
    public static final String DELIMITER = "\t";

    private final ArrayList<String[]> output;

    private final StringBuffer error;

    private int retval;

    /**
     * Create and run a ShellProcess
     * 
     * @param cmd
     */
    public ShellProcess(String[] cmd) {
	error = new StringBuffer();
	output = new ArrayList<String[]>();
	retval = 123456789; // should instill suspicion if this is ever returned

	Process tempP;
	try {
	    tempP = Runtime.getRuntime().exec(cmd);
	} catch (IOException ioe) {
	    error.append("Error on exec: " + ioe.getMessage());
	    return;
	}

	final Process p = tempP;

	// consume error in a new thread
	Thread errorThread = new Thread(new Runnable() {
	    public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(p
			.getErrorStream()));
		String line;
		try {
		    while ((line = br.readLine()) != null) {
			error.append(line);
		    }
		} catch (IOException ioe) {
		    error.append(ioe.getMessage());
		    p.destroy();
		}
	    }
	}, "ErrorThread");

	// consume stdout in a new thread
	Thread stdoutThread = new Thread(new Runnable() {
	    public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(p
			.getInputStream()));
		String line;
		try {
		    while ((line = br.readLine()) != null) {
			output.add(line.split(DELIMITER));
		    }
		} catch (IOException ioe) {
		    error.append(ioe.getMessage());
		    p.destroy();
		}
	    }
	}, "StdoutThread");

	errorThread.start();
	stdoutThread.start();

	try {
	    retval = p.waitFor();
	} catch (InterruptedException ie) {
	    p.destroy();
	    error.append("Process interrupted: " + ie.getMessage());
	}
    }

    /**
     * By convention, 0 means normal termination.
     * 
     * @return
     */
    public int getExitStatus() {
	return retval;
    }

    /**
     * Get the error message for ShellProcess
     * 
     * @return
     */
    public String getError() {
	return error.toString();
    }

    /**
     * Get the output of the ShellProcess
     * 
     * @return
     */
    public String[][] getOutput() {
	return output.toArray(new String[0][]);
    }
}
