package app;

import models.App;
import models.Template;
import visitors.JinjaRenderer;
import visitors.PythonDataExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CodeGenerator {

    private App app;
    private Map<String, Template> templates;
    private String outputPath;

    public CodeGenerator(App app, Map<String, Template> templates, String outputPath) {
        this.app = app;
        this.templates = templates;
        this.outputPath = outputPath;
    }

    public void generate() {
        if (app.semanticErrors != null && !app.semanticErrors.isEmpty()) {
            System.out.println("[CodeGen] Skipping code generation — " +
                app.semanticErrors.size() + " semantic error(s) found.");
            return;
        }

        // Remove old single-product files that are now replaced by per-product files
        new File(outputPath + "/detail.html").delete();
        new File(outputPath + "/edit.html").delete();

        // STEP 1: Extract data from Python AST
        PythonDataExtractor extractor = new PythonDataExtractor();
        extractor.extract(app);

        Map<String, Object> moduleVars = extractor.getModuleVars();
        Map<String, Map<String, Object>> routeContexts = extractor.getRouteContexts();
        Map<String, String> routeTemplates = extractor.getRouteTemplates();
        Map<String, String> urlForRoutes = extractor.getUrlForRoutes();

        // STEP 2: Create renderer
        JinjaRenderer renderer = new JinjaRenderer(templates, urlForRoutes);

        // STEP 3: Create output directory
        File outDir = new File(outputPath);
        outDir.mkdirs();

        // STEP 4: For each route context, render the template
        int generatedCount = 0;
        for (Map.Entry<String, Map<String, Object>> entry : routeContexts.entrySet()) {
            String routeName = entry.getKey();
            Map<String, Object> routeContext = entry.getValue();
            String templateFile = routeTemplates.get(routeName);

            if (templateFile == null) {
                System.out.println("[CodeGen] Warning: no template found for route: " + routeName);
                continue;
            }

            // Merge module-level vars with route-specific context
            Map<String, Object> mergedContext = new HashMap<>(moduleVars);
            mergedContext.putAll(routeContext);

            // Find the template
            Template template = templates.get(templateFile);
            if (template == null) {
                System.out.println("[CodeGen] Warning: template not found in registry: " + templateFile);
                continue;
            }

            // Check for per-product generation
            boolean needsPerProductGeneration =
                mergedContext.containsKey("product") &&
                mergedContext.get("product") == null &&
                mergedContext.get("products") instanceof java.util.List;

            if (needsPerProductGeneration) {
                java.util.List<?> products = (java.util.List<?>) mergedContext.get("products");
                for (Object product : products) {
                    if (!(product instanceof java.util.Map)) continue;
                    java.util.Map<?, ?> productMap = (java.util.Map<?, ?>) product;

                    // Build per-product context
                    Map<String, Object> productContext = new HashMap<>(mergedContext);
                    productContext.put("product", product);

                    // Get product id for filename
                    Object productId = productMap.get("id");
                    String outFileName = routeName + "_" +
                        (productId != null ? productId.toString() : "x")
                        + ".html";

                    // Render and write
                    String html = renderer.render(template, productContext);
                    Path outPath = Paths.get(outputPath, outFileName);
                    try {
                        Files.writeString(outPath, html);
                        generatedCount++;
                        System.out.println("[CodeGen] Generated: " + outPath);
                    } catch (IOException e) {
                        System.out.println("[CodeGen] Error writing: " +
                            outPath + " — " + e.getMessage());
                    }
                }
                continue; // skip the normal single-file generation below
            }

            // Normal single-file generation
            String html = renderer.render(template, mergedContext);
            String outFileName = routeName + ".html";
            Path outPath = Paths.get(outputPath, outFileName);
            try {
                Files.writeString(outPath, html);
                generatedCount++;
                System.out.println("[CodeGen] Generated: " + outPath);
            } catch (IOException e) {
                System.out.println("[CodeGen] Error writing file: " + outPath + " - " + e.getMessage());
            }
        }

        System.out.println("[CodeGen] Extracted " + moduleVars.size() + " module-level variables");
        System.out.println("[CodeGen] Found " + routeContexts.size() + " routes with render_template calls");
        System.out.println("[CodeGen] Generated " + generatedCount + " HTML files");
    }
}
