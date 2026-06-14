package app;

import models.App;
import models.Template;
import symbols.SymbolTable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FlaskCompiler {
    public static void main(String[] args) {

        // getting current path
        Path currentPath = Paths.get("").toAbsolutePath();

        // creating shared symbol table
        SymbolTable symbolTable = new SymbolTable();

        // parsing app (app.py file)
        String appFile = args.length > 0 ? args[0] : "tests/app.py";
        AppHandler appHandler = new AppHandler(currentPath, symbolTable, appFile);
        App app = appHandler.start();

        // parsing templates (.html files)
        TemplatesHandler templatesHandler = new TemplatesHandler(currentPath, symbolTable);
        Map<String, Template> templates = templatesHandler.start();

        // Generate HTML output
        if (app != null && templates != null) {
            CodeGenerator generator = new CodeGenerator(app, templates, "out/generated");
            generator.generate();
        }
    }


}