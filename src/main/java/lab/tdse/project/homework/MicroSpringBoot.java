package lab.tdse.project.homework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * MicroSpringBoot - Servidor HTTP con IoC (Inversion of Control)
 * 
 * Funcionalidades:
 * 1. Servidor HTTP en puerto 8080
 * 2. Mapeo de rutas a métodos usando reflexión
 * 3. Inyección de parámetros con @RequestParam
 * 4. Servicio de archivos estáticos (HTML, PNG)
 */
public class MicroSpringBoot {
    private static final int PORT = 8080;
    private static Map<String, Method> routes = new HashMap<>();
    private static Map<String, Object> instances = new HashMap<>();
    
    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando MicroSpringBoot...\n");
        
        if (args.length > 0) {
            loadController(args[0]);  // Carga el controlador especificado
        } else {
            loadAllControllers();     // Escanea y carga todos los controladores
        }
        showEndpoints();
        startServer();
    }
    
    /**
     * Carga un controlador específico por nombre de clase
     */
    private static void loadController(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            
            // Verificar que tenga @RestController
            if (clazz.isAnnotationPresent(RestController.class)) {
                registerController(clazz);
                System.out.println("✓ Controlador cargado: " + className);
            } else {
                System.err.println("✗ La clase no tiene @RestController: " + className);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("✗ Clase no encontrada: " + className);
        }
    }
    
    /**
     * Carga automáticamente todos los controladores conocidos
     */
    private static void loadAllControllers() {
        System.out.println("Escaneando controladores...");
        
        String[] controllers = {
            "lab.tdse.project.homework.HelloController",
            "lab.tdse.project.homework.GreetingController"
        };
        
        for (String className : controllers) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(RestController.class)) {
                    registerController(clazz);
                    System.out.println("✓ Auto-cargado: " + clazz.getSimpleName());
                }
            } catch (ClassNotFoundException e) {
                // Ignorar controladores no encontrados
            }
        }
    }
    
    /**
     * Registra un controlador: mapea sus rutas a métodos
     * ESTO ES LO MÁS IMPORTANTE - USA REFLEXIÓN
     */
    private static void registerController(Class<?> controllerClass) {
        try {
            // Crear instancia del controlador
            Object instance = null;
            try {
                instance = controllerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
            }
            
            // Recorrer todos los métodos del controlador
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    
                    // Obtener la ruta del @GetMapping
                    GetMapping annotation = method.getAnnotation(GetMapping.class);
                    String path = annotation.value();
                    
                    // Guardar en el mapa: ruta -> método
                    routes.put(path, method);
                    instances.put(path, instance);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al registrar controlador: " + e.getMessage());
        }
    }
    
    /**
     * Muestra todos los endpoints registrados
     */
    private static void showEndpoints() {
        System.out.println("\n=== Servidor Listo ===");
        System.out.println("URL: http://localhost:" + PORT);
        System.out.println("\nEndpoints disponibles:");
        for (String path : routes.keySet()) {
            System.out.println("  GET " + path);
        }
    }
    /**
     * Inicia el servidor HTTP y espera conexiones
     */
    private static void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor escuchando...\n");
            
            // Bucle que acepta conexiones
            while (true) {
                Socket client = serverSocket.accept();
                handleRequest(client);
            }
        }
    }
    
    /**
     * Procesa una petición HTTP de un cliente
     */
    private static void handleRequest(Socket client) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            OutputStream dataOut = client.getOutputStream()
        ) {
            // Leer primera línea: GET /hello HTTP/1.1
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;
            
            System.out.println("📨 " + requestLine);
            
            // Parsear: método y URI
            String[] parts = requestLine.split(" ");
            String method = parts[0];    // GET
            String uri = parts[1];       // /hello o /greeting?name=Juan
            
            // Leer headers (no los usamos pero hay que consumirlos)
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // Consumir headers
            }
            
            // Procesar solo GET
            if (method.equals("GET")) {
                processGetRequest(uri, out, dataOut);
            } else {
                sendError(out, dataOut, 405, "Método no permitido");
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando petición: " + e.getMessage());
        }
    }

    /**
     * Procesa una petición GET
     */
    private static void processGetRequest(String uri, PrintWriter out, OutputStream dataOut) 
            throws IOException {
        
        // Separar path y query params
        // Ejemplo: /greeting?name=Juan -> path="/greeting" params={"name":"Juan"}
        String path = uri;
        Map<String, String> params = new HashMap<>();
        
        int questionMark = uri.indexOf('?');
        if (questionMark != -1) {
            path = uri.substring(0, questionMark);
            String query = uri.substring(questionMark + 1);
            parseQueryParams(query, params);
        }
        
        if (path.endsWith(".html") || path.endsWith(".png")) {
            serveFile(path, out, dataOut);
            return;
        }
        
        Method method = routes.get(path);
        if (method != null) {
            invokeController(method, path, params, out, dataOut);
        } else {
            sendError(out, dataOut, 404, "Ruta no encontrada: " + path);
        }
    }
    
    /**
     * Convierte "name=Juan&age=25" en Map {"name":"Juan", "age":"25"}
     */
    private static void parseQueryParams(String query, Map<String, String> params) {
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf("=");
            if (equals > 0) {
                try {
                    String key = URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    System.err.println("Error parseando parámetro: " + pair);
                }
            }
        }
    }

    /**
     * Invoca el método del controlador usando reflexión
     * AQUÍ OCURRE LA MAGIA: method.invoke()
     */
    private static void invokeController(Method method, String path, 
                                         Map<String, String> params,
                                         PrintWriter out, OutputStream dataOut) {
        try {
            // Preparar argumentos del método
            Object[] args = prepareArguments(method, params);
            
            // Invocar método usando reflexión
            Object instance = instances.get(path);
            Object result = method.invoke(instance, args);
            
            // Enviar respuesta
            String response = "<html><body><h1>" + result.toString() + "</h1></body></html>";
            sendResponse(out, dataOut, 200, response);
            
        } catch (Exception e) {
            System.err.println("Error invocando controlador: " + e.getMessage());
            e.printStackTrace();
            sendError(out, dataOut, 500, "Error interno: " + e.getMessage());
        }
    }
    
    /**
     * Prepara los argumentos para invocar el método
     * Lee los @RequestParam y los mapea con los query params
     */
    private static Object[] prepareArguments(Method method, Map<String, String> params) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            // ¿El parámetro tiene @RequestParam?
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam annotation = param.getAnnotation(RequestParam.class);
                
                String paramName = annotation.value();        // "name"
                String defaultValue = annotation.defaultValue(); // "World"
                
                // Obtener valor de la URL o usar default
                String value = params.getOrDefault(paramName, defaultValue);
                if (value.isEmpty() && !defaultValue.isEmpty()) {
                    value = defaultValue;
                }
                
                args[i] = value;
            } else {
                args[i] = null;
            }
        }
        
        return args;
    }

    /**
     * Sirve un archivo estático (HTML, PNG)
     */
    private static void serveFile(String path, PrintWriter out, OutputStream dataOut) 
            throws IOException {
        
        java.nio.file.Path filePath = Paths.get("src/main/resources/static" + path);
        
        if (!Files.exists(filePath)) {
            sendError(out, dataOut, 404, "Archivo no encontrado");
            return;
        }
        
        byte[] fileData = Files.readAllBytes(filePath);
        String contentType = path.endsWith(".html") ? "text/html" : "image/png";
        
        // Enviar respuesta HTTP
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + fileData.length);
        out.println();
        out.flush();
        
        dataOut.write(fileData);
        dataOut.flush();
    }
 
    /**
     * Envía una respuesta HTTP exitosa
     */
    private static void sendResponse(PrintWriter out, OutputStream dataOut, 
                                      int statusCode, String content) {
        try {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            
            String status = switch (statusCode) {
                case 200 -> "OK";
                case 404 -> "Not Found";
                case 405 -> "Method Not Allowed";
                case 500 -> "Internal Server Error";
                default -> "Unknown";
            };
            
            // Headers HTTP
            out.println("HTTP/1.1 " + statusCode + " " + status);
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Content-Length: " + data.length);
            out.println();
            out.flush();
            
            // Body
            dataOut.write(data);
            dataOut.flush();
            
        } catch (IOException e) {
            System.err.println("Error enviando respuesta: " + e.getMessage());
        }
    }
    
    /**
     * Envía una respuesta de error HTTP
     */
    private static void sendError(PrintWriter out, OutputStream dataOut, 
                                   int statusCode, String message) {
        String html = "<html><body><h1>" + statusCode + " - " + message + "</h1></body></html>";
        sendResponse(out, dataOut, statusCode, html);
    }
}
