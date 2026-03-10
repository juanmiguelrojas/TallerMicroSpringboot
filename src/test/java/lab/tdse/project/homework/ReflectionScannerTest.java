package lab.tdse.project.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests para verificar el escaneo de controladores mediante reflexión
 */
class ReflectionScannerTest {

    @Test
    @DisplayName("El escáner encuentra clases con @RestController en el paquete")
    void testScanForRestControllers() throws Exception {
        List<Class<?>> controllers = scanForRestControllers("lab.tdse.project.homework");
        
        assertFalse(controllers.isEmpty(), 
                "Debe encontrar al menos un controlador con @RestController");
        
        assertTrue(controllers.stream()
                .anyMatch(c -> c.getSimpleName().equals("HelloController")),
                "Debe encontrar HelloController");
        
        assertTrue(controllers.stream()
                .anyMatch(c -> c.getSimpleName().equals("GreetingController")),
                "Debe encontrar GreetingController");
    }

    @Test
    @DisplayName("Los controladores encontrados tienen métodos con @GetMapping")
    void testControllersHaveGetMappingMethods() throws Exception {
        List<Class<?>> controllers = scanForRestControllers("lab.tdse.project.homework");
        
        for (Class<?> controller : controllers) {
            boolean hasGetMapping = false;
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    hasGetMapping = true;
                    break;
                }
            }
            assertTrue(hasGetMapping, 
                    controller.getSimpleName() + " debe tener al menos un método con @GetMapping");
        }
    }

    @Test
    @DisplayName("Verificar que se pueden obtener los valores de @GetMapping mediante reflexión")
    void testGetMappingValues() throws Exception {
        List<String> mappings = new ArrayList<>();
        
        for (Method method : HelloController.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping mapping = method.getAnnotation(GetMapping.class);
                mappings.add(mapping.value());
            }
        }
        
        assertTrue(mappings.contains("/hello"), "Debe contener el mapping '/hello'");
        assertTrue(mappings.contains("/pi"), "Debe contener el mapping '/pi'");
        assertTrue(mappings.contains("/"), "Debe contener el mapping '/'");
    }

    @Test
    @DisplayName("Verificar invocación de método con reflexión")
    void testMethodInvocationWithReflection() throws Exception {
        Method method = HelloController.class.getMethod("hola");
        Object result = method.invoke(null); // null porque es estático
        
        assertNotNull(result, "El método debe retornar un valor");
        assertTrue(result instanceof String, "El resultado debe ser un String");
        assertTrue(((String) result).contains("LIMBUS"), 
                "El resultado debe contener 'LIMBUS'");
    }

    @Test
    @DisplayName("Verificar invocación de método con parámetros usando reflexión")
    void testMethodInvocationWithParameters() throws Exception {
        Method method = GreetingController.class.getMethod("greeting", String.class);
        
        // Invocar con un parámetro
        Object result = method.invoke(null, "JUnit");
        assertNotNull(result, "El método debe retornar un valor");
        assertEquals("Hola JUnit", result, 
                "El resultado debe ser 'Hola JUnit'");
    }

    // Método auxiliar para escanear controladores (simplificado para tests)
    private List<Class<?>> scanForRestControllers(String packageName) throws Exception {
        List<Class<?>> controllers = new ArrayList<>();
        
        // Cargar clases conocidas del paquete
        String[] classes = {"HelloController", "GreetingController"};
        
        for (String className : classes) {
            try {
                Class<?> clazz = Class.forName(packageName + "." + className);
                if (clazz.isAnnotationPresent(RestController.class)) {
                    controllers.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                // Ignorar si no existe
            }
        }
        
        return controllers;
    }
}
