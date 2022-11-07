import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {
    private BufferedWriter writer;
    private String filename;
    private int numLabels, numIfGotoLabels, numReturnLabels, numFunctLabels;
    private String currentFunctionName;

    public CodeWriter(File outputFile) {
        if (outputFile == null) {   // check if output file is null
            throw new IllegalArgumentException("Output file should not be null!");
        }

        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {   // file error
            throw new IllegalStateException("File could not be created, is directory, or cannot be opened!");
        }

        currentFunctionName = "";
    }

    /**
     * Sets the filename that is currently being processed
     * @param filename Filename of processing file
     */
    public void setFileName(String filename) {
        this.filename = filename.substring(0, filename.lastIndexOf("."));
    }

    /**
     * Writes the assembly code for the arithmetic commands
     * @param command The text of the arithmetic command
     */
    public void writeArithmetic(String command) {
        switch (command) {
            case "add":
                writeHeaderForBinaryCommand();
                write("M=D+M\n");   // add
                break;

            case "sub":
                writeHeaderForBinaryCommand();
                write("M=M-D\n");   // subtract
                break;

            case "neg":
                writeHeaderForUnaryCommand();
                write("M=-M\n");    // negate
                break;

            case "eq":  // check if equal
                writeHeaderForBinaryCommand();
                writeComparisonCommand("JEQ");
                break;

            case "gt":  // check if greater
                writeHeaderForBinaryCommand();
                writeComparisonCommand("JGT");
                break;

            case "lt":  // check if less than
                writeHeaderForBinaryCommand();
                writeComparisonCommand("JLT");
                break;

            case "and":
                writeHeaderForBinaryCommand();
                write("M=D&M\n");   // bitwise and
                break;

            case "or":
                writeHeaderForBinaryCommand();
                write("M=D|M\n");   // bitwise or
                break;

            case "not":
                writeHeaderForUnaryCommand();
                write("M=!M\n");    // bitwise not
                break;

        }
    }

    /**
     * Writes a push or pop command as assembly code
     * @param pushOrPop Command enum which accepts only C_PUSH or C_POP
     * @param segment Segment of memory
     * @param index Index in segment
     */
    public void writePushPop(Command pushOrPop, String segment, int index) {
        if (pushOrPop != Command.C_PUSH && pushOrPop != Command.C_POP)
            throw new IllegalArgumentException("Cannot call writePushPop to" + 
                    " write command other than push or pop!");

        // load appropriate data to push or pop into register A
        switch (segment) {
            case "constant":    // index as constant
                write("@" + index + "\n");
                break;

            case "local":       // A = LCL + index
                writeGetAddressAtRegisterWithOffset("LCL", index);
                break;

            case "argument":    // A = ARG + index
                writeGetAddressAtRegisterWithOffset("ARG", index);
                break;

            case "this":        // A = THIS + index
                writeGetAddressAtRegisterWithOffset("THIS", index);
                break;

            case "that":        // A = THAT + index
                writeGetAddressAtRegisterWithOffset("THAT", index);    
                break;

            case "pointer":     // A = 3 + index
                writeGetAddressOfRegisterWithOffset("THIS", index);
                break;

            case "temp":        // A = 5 + index
                writeGetAddressOfRegisterWithOffset("R5", index);
                break;

            case "static":      // A = filename.index
                write("@" + filename + "." + index + "\n");
                break;

            default:
                throw new IllegalArgumentException("Unknown segment for push or pop command!");
        }

        if (pushOrPop == Command.C_PUSH) {
            if (segment.equals("constant"))
                write("D=A\n"); // D has the constant
            else
                write ("D=M\n"); // D has the value at address A
            
            writePushDataToStack();
        } else {    // pop command
            write("D=A\n" +         // save address in temp variable
                  "@R13\n" + 
                  "M=D\n");
            writePopStackToData();
            write("@R13\n" +        // restore address in temp variable
                  "A=M\n" +
                  "M=D\n");
        }
    }

    /**
     * Writes initialization code to call Sys.init
     */
    public void writeInit() {
        write("@256\n" +        // initialize stack pointer
              "D=A\n" +
              "@SP\n" +
              "M=D\n");
        writeCall("Sys.init", 0);
    }

    /**
     * Writes label command in assembly
     * @param label Label name
     */
    public void writeLabel(String label) {
        String localLabel = currentFunctionName + "." + label;
        write("(" + localLabel + ")\n");
    }

    /**
     * Writes goto command in assembly code within the scope of the function
     * @param label Label name
     */
    public void writeGoto(String label) {
        String localLabel = currentFunctionName + "." + label;
        write("@" + localLabel + "\n" +
              "0;JMP\n");
    }

    /**
     * Writes the if-goto command in assembly code within the scope of the
     * function
     * @param label
     */
    public void writeIf(String label) {
        String localLabel = currentFunctionName + "." + label;
        writePopStackToData();
        write("@IF_FALSE_" + numIfGotoLabels + "\n" + 
              "D;JEQ\n" +   // check if top of stack is false
              "@" + localLabel + "\n" +
              "0;JMP\n" +
              "(IF_FALSE_" + numIfGotoLabels + ")\n");
        numIfGotoLabels++;
    }

    /**
     * Writes the call command in assembly code
     * @param functionName
     * @param numArgs Number of arguments
     */
    public void writeCall(String functionName, int numArgs) {
        write("@RETURN_" + numReturnLabels + "\n" +
              "D=A\n");
        writePushDataToStack(); // push return address
        write("@LCL\n" +
              "D=M\n");
        writePushDataToStack(); // push local
        write("@ARG\n" +
              "D=M\n");
        writePushDataToStack(); // push argument
        write("@THIS\n" +
              "D=M\n");
        writePushDataToStack(); // push this
        write("@THAT\n" +
              "D=M\n");
        writePushDataToStack(); // push that
        write("@SP\n" +
              "D=M\n" +         // D = SP
              "@LCL\n" +
              "M=D\n" +         // LCL = SP
              "@" + numArgs + "\n" +
              "D=D-A\n" +       // D = SP - n
              "@5\n" +
              "D=D-A\n" +       // D = SP - n - 5
              "@ARG\n" + 
              "M=D\n" +         // ARG + SP - n - 5
              "@" + functionName + "\n" +
              "0;JMP\n" +       // jump to function
              "(RETURN_" + numReturnLabels + ")\n");
        numReturnLabels++;
    }

    /**
     * Writes the return command in assembly code
     */
    public void writeReturn() {
        write("@LCL\n" +
              "D=M\n" +
              "@R13\n" +
              "M=D\n" +     // FRAME = LCL
              "@5\n" +
              "A=D-A\n" +   
              "D=M\n" +     // D = *(FRAME - 5)
              "@R14\n" +
              "M=D\n");     // RET = *(FRAME - 5)
        writePopStackToData();
        write("@ARG\n" +
              "A=M\n" +
              "M=D\n" +     // *ARG = pop()
              "D=A+1\n" +
              "@SP\n" +
              "M=D\n");     // SP = ARG + 1
        writeRestoreRegisterWithOffset("THAT", 1);
        writeRestoreRegisterWithOffset("THIS", 2);
        writeRestoreRegisterWithOffset("ARG", 3);
        writeRestoreRegisterWithOffset("LCL", 4);
        write("@R14\n" +
              "A=M\n" +
              "0;JMP\n");   // goto RET
    }

    /**
     * Write function command as assembly code
     * @param functionName
     * @param numLocals Number of local variables
     */
    public void writeFunction(String functionName, int numLocals) {
        write("(" + functionName + ")\n" +
              "@" + numLocals + "\n" +
              "D=A\n" +         // D = k + 1
              "(START_LOOP_" + numFunctLabels + ")\n" +
              "@END_LOOP_" + numFunctLabels + "\n" +
              "D;JLE\n" +       // check if iterated k times
              "D=D-1\n" +       // k--
              "@R13\n" +
              "M=D\n" +         // save k in temp variable
              "@0\n" +
              "D=A\n");
        writePushDataToStack(); // push 0 to stack
        write("@R13\n" +        // restore k
              "D=M\n" +
              "@START_LOOP_" + numFunctLabels + "\n" +
              "0;JMP\n" +       // loop again
              "(END_LOOP_" + numFunctLabels + ")\n");
        numFunctLabels++;
        currentFunctionName = functionName;
    }

    /**
     * Closes the buffered writer
     * @throws IOException
     */
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Loads first argument in D and second argument's address in A
     */
    private void writeHeaderForBinaryCommand() {
        write("@SP\n" + 
              "M=M-1\n" + 
              "A=M\n" + 
              "D=M\n" + 
              "A=A-1\n");
    }

    /**
     * Loads address for argument in register A
     */
    private void writeHeaderForUnaryCommand() {
        writeGetAddressAtRegister("SP");
        write("A=A-1\n");
    }

    /**
     * Writes the footer for the eq, lt, and gt commands in assembly
     * code
     * @param jumpMnemonic Jump mnemonic in HACK assembly
     */
    private void writeComparisonCommand(String jumpMnemonic) {
        write("D=M-D\n" +                   // subtract
              "@TRUE_" + numLabels + "\n" + 
              "D;" + jumpMnemonic + "\n" +  // check if we should jump
              "@SP\n" +                     // false
              "A=M-1\n" + 
              "M=0\n" +                     // push false to stack
              "@END_" + numLabels + "\n" + 
              "0;JMP\n" +                   // jump to end
              "(TRUE_" + numLabels + ")\n" + // true
              "@SP\n" +
              "A=M-1\n" +
              "M=-1\n" +                    // push true to stack
              "(END_" + numLabels + ")\n");
        numLabels++;
    }

    /**
     * Gets the address in register registerName
     * @param registerName
     */
    private void writeGetAddressAtRegister(String registerName) {
        write("@" + registerName + "\n" +
              "A=M\n");
    }

    /**
     * Writes assembly code to get the address in the register plus an offset
     * @param registerName
     * @param offset
     */
    private void writeGetAddressAtRegisterWithOffset(String registerName, int offset) {
        write("@" + registerName + "\n" +
              "D=M\n" +
              "@" + offset + "\n" +
              "A=D+A\n");
    }

    /**
     * Writes assembly code to get the address of the register (not the value
     * in the register) plus an offset
     * @param registerName
     * @param offset
     */
    private void writeGetAddressOfRegisterWithOffset(String registerName, int offset) {
        write("@" + registerName + "\n" +
              "D=A\n" +
              "@" + offset + "\n" +
              "A=D+A\n");
    }

    /**
     * Writes code to push register D to the stack
     */
    private void writePushDataToStack() {
        write("@SP\n" +
              "A=M\n" +
              "M=D\n" +
              "@SP\n" +
              "M=M+1\n");
    }

    /**
     * Writes code to pop the stack and put it into register D
     */
    private void writePopStackToData() {
        write("@SP\n" +
              "M=M-1\n" + 
              "A=M\n" + 
              "D=M\n");
    }

    /**
     * Writes code to restore register registerName at an offset by copying the value
     * in register R13 to the target
     * @param registerName
     * @param offset
     */
    private void writeRestoreRegisterWithOffset(String registerName, int offset) {
        write("@R13\n" +
              "D=M\n" +                     // D = value to restore
              "@" + offset + "\n" +
              "A=D-A\n" +
              "D=M\n" +
              "@" + registerName + "\n" +
              "M=D\n");                     // restore value
    }

    /**
     * Attempts to write a string to the output file
     * @param str
     */
    private void write(String str) {
        try {
            writer.append(str);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write to output file!");
        }
    }
}
