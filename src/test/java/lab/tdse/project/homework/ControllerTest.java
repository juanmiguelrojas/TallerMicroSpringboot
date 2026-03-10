package lab.tdse.project.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para verificar la funcionalidad de los controladores
 */
class ControllerTest {

    @Test
    @DisplayName("HelloController.hola() retorna un mensaje válido")
    void testHelloControllerHelloMethod() {
        String result = HelloController.hola();
        assertNotNull(result, "El método hola() no debe retornar null");
        assertFalse(result.isEmpty(), "El resultado no debe estar vacío");
        assertTrue(result.contains("LIMBUS"), 
                "El mensaje debe contener 'LIMBUS'");
    }

    @Test
    @DisplayName("HelloController.getPi() retorna el valor de PI")
    void testHelloControllerPiMethod() {
        String result = HelloController.getPi();
        assertNotNull(result, "El método getPi() no debe retornar null");
        assertTrue(result.contains("3.14"), 
                "El resultado debe contener el valor de PI");
        assertTrue(result.contains("PI"), 
                "El resultado debe mencionar 'PI'");
    }

    @Test
    @DisplayName("HelloController.index() retorna un mensaje de bienvenida")
    void testHelloControllerRootMethod() {
        String result = HelloController.index();
        assertNotNull(result, "El método index() no debe retornar null");
        assertFalse(result.isEmpty(), "El resultado no debe estar vacío");
    }

    @Test
    @DisplayName("GreetingController.greeting() con parámetro personalizado")
    void testGreetingControllerWithCustomName() {
        String result = GreetingController.greeting("TDSE");
        assertNotNull(result, "El método no debe retornar null");
        assertEquals("Hola TDSE", result, 
                "Debe retornar 'Hola' + el nombre proporcionado");
    }

    @Test
    @DisplayName("GreetingController.greeting() con valor por defecto")
    void testGreetingControllerWithDefaultValue() {
        // Simular el comportamiento del valor por defecto
        String result = GreetingController.greeting("World");
        assertNotNull(result, "El método no debe retornar null");
        assertEquals("Hola World", result, 
                "Con el valor por defecto debe retornar 'Hola World'");
    }

    @Test
    @DisplayName("Los controladores son clases válidas con @RestController")
    void testControllersAreValid() {
        assertNotNull(HelloController.class, "HelloController debe existir");
        assertNotNull(GreetingController.class, "GreetingController debe existir");
        
        assertTrue(HelloController.class.isAnnotationPresent(RestController.class),
                "HelloController debe tener @RestController");
        assertTrue(GreetingController.class.isAnnotationPresent(RestController.class),
                "GreetingController debe tener @RestController");
    }

    @Test
    @DisplayName("Verificar que todos los métodos públicos son accesibles")
    void testPublicMethodsAreAccessible() throws Exception {
        // Verificar HelloController
        assertDoesNotThrow(() -> HelloController.class.getMethod("hola"),
                "El método hola() debe ser público y accesible");
        assertDoesNotThrow(() -> HelloController.class.getMethod("getPi"),
                "El método getPi() debe ser público y accesible");
        assertDoesNotThrow(() -> HelloController.class.getMethod("index"),
                "El método index() debe ser público y accesible");
        
        // Verificar GreetingController
        assertDoesNotThrow(() -> GreetingController.class.getMethod("greeting", String.class),
                "El método greeting(String) debe ser público y accesible");
    }
}
