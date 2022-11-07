import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    private BufferedReader reader;
    private String currentLine;
    private int currentLineNumber;

    private Command commandType;
    private String arg1;
    private int arg2;

    public Parser(File inputFile) {
        if (inputFile == null) {    // check if file is null
            throw new IllegalArgumentException("Input file should not be null!");
        }

        try {
            // create a buffered reader for the input file
            reader = new BufferedReader(new FileReader(inputFile));
        } catch (FileNotFoundException e) { // file error
            throw new IllegalArgumentException("File not found, is directory, or cannot be opened!");
        }

        advanceToNextLine();
    }

    /**
     * Checks if there is more commands in the input file
     * @return True if there are more commands, false otherwise
     */
    public boolean hasMoreCommands() {
        return currentLine != null && currentLine.length() >= 1;
    }

    /**
     * Advances to the next command in the input file and processes 
     * the command type and arguments
     */
    public void advance() {
        // split by whitespace
        String[] tokens = currentLine.split("\\s+");
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "add": case "sub": case "neg": case "eq": case "gt":
            case "lt": case "and": case "or": case "not":
                // parse arithmetic commands
                if (tokens.length > 1) // check if there is valid number of tokens
                    throw new IllegalStateException("Syntax error (line " + currentLineNumber
                            + "): unkown token \"" + tokens[1] + "\"");

                commandType = Command.C_ARITHMETIC;
                arg1 = command;
                arg2 = 0;
                break;

            case "push": case "pop":
                // parse push and pop commands
                if (tokens.length < 3) // check if there is valid number of tokens
                    throw new IllegalStateException("Syntax error (line " + currentLineNumber
                            + "): command missing arguments");
                else if (tokens.length > 3)
                    throw new IllegalStateException("Syntax error (line " + currentLineNumber
                            + "): unkown token \"" + tokens[3] + "\"");

                commandType = command.equals("push") ? Command.C_PUSH : Command.C_POP;
                arg1 = tokens[1];   // segment
                arg2 = Integer.parseInt(tokens[2]); // index
                break;
            
            case "label": case "goto": case "if-goto":
                // parse label, goto, and if-goto commands
                commandType = command.equals("label") ? Command.C_LABEL : 
                        (command.equals("goto") ? Command.C_GOTO : Command.C_IF);
                arg1 = tokens[1];   // label
                arg2 = 0;           // ignore second arg
                break;

            case "function": case "call":
                // parse function and call commands
                commandType = command.equals("function") ? Command.C_FUNCTION
                        : Command.C_CALL;
                arg1 = tokens[1];   // function name
                arg2 = Integer.parseInt(tokens[2]); // num local variables or arguments
                break;
            
            case "return":
                // parse return command
                commandType = Command.C_RETURN;
                arg1 = null; // ignore 
                arg2 = 0;    // ignore
                break;
            
            default:
                throw new IllegalStateException("Syntax error (line " + currentLineNumber
                        + "): \"" + command + "\" is not a command");
        }

        advanceToNextLine();
    }

    /**
     * Returns command type
     * @return Command type enum
     */
    public Command commandType() {
        return commandType;
    }

    /**
     * Returns first argument depending on the command type
     * @return Argument 1
     */
    public String arg1() {
        return arg1;
    }

    /**
     * Returns second argument depending on the command type
     * @return Argument 2
     */
    public int arg2() {
        return arg2;
    }

    /**
     * Closes the buffered reader
     * @throws IOException
     */
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Finds the next non-empty line in the input file
     */
    private void advanceToNextLine() {
        boolean done = false;

        while (!done) {
            try {
                currentLine = reader.readLine();
            } catch (IOException e) {
                throw new IllegalStateException("IO exception: " + e.getMessage());
            }

            currentLineNumber++;

            if (currentLine == null)    // check if end of file
                break;

            int commentIndex = currentLine.indexOf("//");

            if (commentIndex >= 0)  // ignore comments in current line
                currentLine = currentLine.substring(0, commentIndex);

            currentLine = currentLine.trim(); // remove whitespace

            if (currentLine.length() > 0) // check if non-empty
                done = true;
        }
    }
}