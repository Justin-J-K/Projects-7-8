import java.io.File;
import java.io.IOException;

public class VMTranslator {
    private CodeWriter codeWriter;

    public static void main(String[] args) {
        if (args.length != 1) { // check if there is a valid number of arguments
            System.out.println("Command accepts only one argument (filename or directory)!");
            return;
        }

        String filenameOrDirectory = args[0];
        File file = new File(filenameOrDirectory);

        if (!file.exists()) {   // check if the file exists
            System.out.println("File or directory does not exist!");
            return;
        }

        // check if it is not a directory and has the .vm extension
        if (file.isFile() && !file.getName().endsWith(".vm")) {
            System.out.println("File must be a .vm file!");
            return;
        }

        String outputFilename = file.getName();
        
        if (file.isFile())  // remove .vm extension if it is a file
            outputFilename = outputFilename.substring(0, outputFilename.lastIndexOf(".vm"));

        outputFilename += ".asm";

        File outputFile = new File(file.isDirectory() ? file : file.getParentFile(), outputFilename);
        VMTranslator translator = new VMTranslator(outputFile);

        if (file.isDirectory()) {
            translator.translateFilesInDirectory(file);
        } else {
            translator.translateFile(file);
        }

        translator.close();

        System.out.println("Finished translating file(s)");
    }

    /**
     * Initializes the code writer at writes the init code
     * @param outputFile File to be outputted to
     */
    public VMTranslator(File outputFile) {
        codeWriter = new CodeWriter(outputFile);
        codeWriter.writeInit();
    }

    /**
     * Translates the VM file into HACK assembly
     * @param file File to be translated
     */
    public void translateFile(File file) {
        Parser parser = new Parser(file);

        // set the new file name in the code writer
        codeWriter.setFileName(file.getName());

        while (parser.hasMoreCommands()) {
            parser.advance();

            // get the command type and arguments
            Command commandType = parser.commandType();
            String arg1 = parser.arg1();
            int arg2 = parser.arg2();

            switch (commandType) {
                case C_ARITHMETIC:
                    codeWriter.writeArithmetic(arg1);
                    break;
                case C_PUSH: case C_POP:
                    codeWriter.writePushPop(commandType, arg1, arg2);
                    break;
                case C_LABEL:
                    codeWriter.writeLabel(arg1);
                    break;
                case C_GOTO:
                    codeWriter.writeGoto(arg1);
                    break;
                case C_IF:
                    codeWriter.writeIf(arg1);
                    break;
                case C_FUNCTION:
                    codeWriter.writeFunction(arg1, arg2);
                    break;
                case C_RETURN:
                    codeWriter.writeReturn();
                    break;
                case C_CALL:
                    codeWriter.writeCall(arg1, arg2);
            }
        }
    }

    /**
     * Iterates over a directory non-recursively and translates each .vm file.
     * The output is still in one assembly output file. 
     * @param directory File that represents the directory
     */
    public void translateFilesInDirectory(File directory) {
        for (File file : directory.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".vm"))
                translateFile(file);
        }
    }

    /**
     * Closes the buffered writer
     */
    public void close() {
        try {
            codeWriter.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write to file!");
        }
    }
}
