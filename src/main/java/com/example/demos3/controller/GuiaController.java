package com.example.demos3.controller;

import com.example.demos3.service.GuiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    @Autowired
    private GuiaService guiaService;

    // 1. ENDPOINT: Crear  (Guarda temporalmente en EFS)
    @PostMapping
    public ResponseEntity<?> crearGuia(@RequestBody Map<String, String> body) {
        try {
            String id = body.get("id");
            String fecha = body.get("fecha");
            String transportista = body.get("transportista");
            String detalle = body.get("detalle");

            String rutaArchivo = guiaService.crearGuiaTemporal(fecha, transportista, id, detalle);
            
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("status", "Creada"); 
            respuesta.put("mensaje", "Guía guardada temporalmente en EFS");
            respuesta.put("ruta_efs", rutaArchivo);
            
            return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear guía: " + e.getMessage());
        }
    }

    // 2. ENDPOINT: Subir a s3
    @PostMapping("/subir-s3")
    public ResponseEntity<?> subirS3(@RequestParam String rutaEfs) {
        guiaService.subirAS3(rutaEfs);
        return ResponseEntity.ok("Archivo subido exitosamente a AWS S3 en su carpeta correspondiente.");
    }

    // 3. ENDPOINT: Descargar 
    @GetMapping("/descargar")
    public ResponseEntity<?> descargarGuia(
            @RequestParam String fecha,
            @RequestParam String transportista,
            @RequestParam String id,
            @RequestHeader("Authorization") String token) {
        
        // Validamos permisos antes de dejar descargar
        if (!guiaService.validarPermisos(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: No tienes permisos para descargar esta guía.");
        }

        try {
            byte[] archivo = guiaService.obtenerArchivo(fecha, transportista, id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guia" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(archivo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Archivo no encontrado en las carpetas.");
        }
    }

    // 4. ENDPOINT: Modificar o actualizar 
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarGuia(@PathVariable String id, @RequestBody Map<String, String> body) {
        // Simulación de actualización de datos
        return ResponseEntity.ok("Guía N° " + id + " modificada correctamente en el sistema.");
    }

    // 5. ENDPOINT: Eliminar 
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarGuia(@PathVariable String id) {
        return ResponseEntity.ok("Guía N° " + id + " eliminada del almacenamiento.");
    }

    // 6. ENDPOINT: Consultar 
    @GetMapping("/buscar")
    public ResponseEntity<?> consultarGuias(@RequestParam String transportista, @RequestParam String fecha) {
        Map<String, String> resultadoBusqueda = new HashMap<>();
        resultadoBusqueda.put("fecha_busqueda", fecha);
        resultadoBusqueda.put("transportista", transportista);
        resultadoBusqueda.put("resultado", "Se encontraron guías en la ruta: /" + fecha.replaceAll("-", "") + "/" + transportista + "/");
        
        return ResponseEntity.ok(resultadoBusqueda);
    }
}
