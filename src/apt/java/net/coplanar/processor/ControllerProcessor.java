package net.coplanar.processor;

import com.google.auto.service.AutoService;
import net.coplanar.annotations.Controller;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("net.coplanar.annotations.Controller")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ControllerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, 
            "ControllerProcessor running, found " + roundEnv.getElementsAnnotatedWith(Controller.class).size() + " @Controller annotations");

        try {
            JavaFileObject sourceFile = processingEnv.getFiler()
                .createSourceFile("net.coplanar.app.ControllerRegistry");
            
            try (PrintWriter writer = new PrintWriter(sourceFile.openWriter())) {
                writer.println("package net.coplanar.app;");
                writer.println();
                writer.println("import java.util.Map;");
                writer.println("import java.util.HashMap;");
                writer.println();
                writer.println("public class ControllerRegistry {");
                writer.println("    private static final Map<String, Class<?>> controllers = new HashMap<>();");
                writer.println();
                writer.println("    static {");
                
                for (Element element : roundEnv.getElementsAnnotatedWith(Controller.class)) {
                    if (element instanceof TypeElement typeElement) {
                        Controller annotation = element.getAnnotation(Controller.class);
                        String path = annotation.path();
                        String className = typeElement.getSimpleName().toString();
                        
                        // Use class name if path is empty
                        if (path.isEmpty()) {
                            // Convert TscController -> tsc
                            path = className.toLowerCase();
                        }
                        
                        String fullClassName = typeElement.getQualifiedName().toString();
                        writer.println(String.format("        controllers.put(\"%s\", %s.class);", 
                            path, fullClassName));
                    }
                }
                
                writer.println("    }");
                writer.println();
                writer.println("    public static Class<?> getController(String path) {");
                writer.println("        return controllers.get(path);");
                writer.println("    }");
                writer.println();
                writer.println("    public static Map<String, Class<?>> getAllControllers() {");
                writer.println("        return new HashMap<>(controllers);");
                writer.println("    }");
                writer.println("}");
            }
            
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR, "Error generating ControllerRegistry: " + e.getMessage());
        }

        return true;
    }
}
