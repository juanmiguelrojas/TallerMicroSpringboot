package lab.tdse.project.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Tests para verificar que las anotaciones personalizadas funcionan correctamente
 */
class AnnotationsTest {

    @Test
    @DisplayName("La anotación @RestController existe y está presente en HelloController")
    void testRestControllerAnnotationExists() {
        assertTrue(HelloController.class.isAnnotationPresent(RestController.class),
                "@RestController debe estar presente en HelloController");
    }

    @Test
    @DisplayName("La anotación @RestController existe y está presente en GreetingController")
    void testRestControllerAnnotationOnGreetingController() {
        assertTrue(GreetingController.class.isAnnotationPresent(RestController.class),
                "@RestController debe estar presente en GreetingController");
    }

    @Test
    @DisplayName("La anotación @GetMapping existe y mapea rutas correctamente")
    void testGetMappingAnnotationExists() throws Exception {
        Method method = HelloController.class.getMethod("hola");
        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "@GetMapping debe estar presente en el método hola()");
        
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertEquals("/hello", mapping.value(),
                "El valor de @GetMapping debe ser '/hello'");
    }

    @Test
    @DisplayName("La anotación @RequestParam funciona con valores por defecto")
    void testRequestParamAnnotationWithDefaultValue() throws Exception {
        Method method = GreetingController.class.getMethod("greeting", String.class);
        
        var parameters = method.getParameters();
        assertEquals(1, parameters.length, "El método greeting debe tener 1 parámetro");
        
        var param = parameters[0];
        assertTrue(param.isAnnotationPresent(RequestParam.class),
                "El parámetro debe tener la anotación @RequestParam");
        
        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        assertEquals("name", requestParam.value(),
                "El parámetro debe llamarse 'name'");
        assertEquals("World", requestParam.defaultValue(),
                "El valor por defecto debe ser 'World'");
    }

    @Test
    @DisplayName("HelloController tiene múltiples métodos con @GetMapping")
    void testMultipleGetMappingsInHelloController() {
        int count = 0;
        for (Method method : HelloController.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                count++;
            }
        }
        assertTrue(count >= 3, 
                "HelloController debe tener al menos 3 métodos con @GetMapping");
    }

    @Test
    @DisplayName("Los métodos anotados con @GetMapping son públicos y estáticos")
    void testGetMappingMethodsArePublicAndStatic() throws Exception {
        Method method = HelloController.class.getMethod("hola");
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()),
                "Los métodos con @GetMapping deben ser públicos");
        assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()),
                "Los métodos con @GetMapping deben ser estáticos");
    }
}
