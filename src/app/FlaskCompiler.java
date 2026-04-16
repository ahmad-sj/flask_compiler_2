package app;

import symbols.SymbolTable;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FlaskCompiler {
    public static void main(String[] args) {

        // getting current path
        Path currentPath = Paths.get("").toAbsolutePath();

        // creating shared symbol table
        SymbolTable symbolTable = new SymbolTable();

        // parsing app (app.py file)
        AppHandler appHandler = new AppHandler(currentPath, symbolTable);
        appHandler.start();

        // parsing templates (.html files)
        TemplatesHandler templatesHandler = new TemplatesHandler(currentPath, symbolTable);
//        templatesHandler.start();
    }


}