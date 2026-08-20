package visitors;

import antlr.templateParser;
import antlr.templateParserBaseVisitor;
import models.Node;
import symbols.SymbolTable;
import models.Template;
import models.DocType;

public class TemplateVisitor extends templateParserBaseVisitor<Template> {

    String templateName;
    SymbolTable symbolTable;

    public TemplateVisitor(String fileName) {
        this.templateName = fileName;
        this.symbolTable = new SymbolTable();
    }

    public TemplateVisitor(String templateName, SymbolTable symbolTable) {
        this.templateName = templateName;
        this.symbolTable = symbolTable;
    }

    @Override
    public Template visitTemplate(templateParser.TemplateContext ctx) {
        Template template = new Template(templateName);

        NodeVisitor nodeVisitor = new NodeVisitor(symbolTable);

        // The doctype is a terminal rather than a rule, so it is turned into a
        // node here instead of being dispatched through the node visitor.
        if (ctx.DOCTYPE() != null) {
            DocType docType = new DocType(ctx.DOCTYPE().getText());
            docType.setNodeName("doctype");
            docType.setLineNumber(ctx.DOCTYPE().getSymbol().getLine());
            template.addNode(docType);
        }

        for (int i = 0; i < ctx.getChildCount() - 1; i++) {
            // Skip the doctype terminal already handled above.
            if (ctx.DOCTYPE() != null && ctx.getChild(i) == ctx.DOCTYPE()) continue;

            Node child = nodeVisitor.visit(ctx.getChild(i));
            if (child != null) template.addNode(child);
        }

        symbolTable.exitScope();

        return template;
    }
}