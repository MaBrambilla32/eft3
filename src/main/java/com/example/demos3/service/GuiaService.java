package com.example.demos3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials; // Soporta las 3 llaves de estudiante
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class GuiaService {

    private final String RUTA_BASE_EFS = "./temporal_efs/";

    @Value("${aws.access.key}")
    private String accessKey;

    @Value("${aws.secret.key}")
    private String secretKey;

    @Value("${aws.session.token}") // Variable para el token largo de AWS Academy
    private String sessionToken;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    // 1. Crear el archivo de manera local simulando el EFS
    public String crearGuiaTemporal(String fecha, String transportista, String idGuia, String contenido) throws IOException {
        String fechaLimpia = fecha.replaceAll("-", "");
        String transportistaLimpio = transportista.replaceAll("\\s+", "");
        
        String rutaCarpetas = RUTA_BASE_EFS + fechaLimpia + "/" + transportistaLimpio + "/";
        String nombreArchivo = "guia" + idGuia + ".pdf"; 
        String rutaCompleta = rutaCarpetas + nombreArchivo;

        File directorio = new File(rutaCarpetas);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        FileWriter writer = new FileWriter(rutaCompleta);
        writer.write("GUÍA DE DESPACHO N° " + idGuia + "\n");
        writer.write("Transportista: " + transportista + "\n");
        writer.write("Fecha: " + fecha + "\n");
        writer.write("Detalle del Pedido: " + contenido);
        writer.close();

        return rutaCompleta;
    }

    // 2. Subir el archivo a AWS S3 usando las credenciales de sesión de alumno
    public void subirAS3(String rutaLocal) {
        try {
            System.out.println("Conectando a AWS con las llaves proporcionadas...");
            
            // Creamos las credenciales pasando obligatoriamente las 3 llaves
            AwsSessionCredentials credentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
            
            // Construimos el cliente de S3 estándar compatible con tu versión del SDK
            S3Client s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            String nombreS3 = rutaLocal.replace("./temporal_efs/", "").replace("\\", "/");

            System.out.println("Subiendo archivo '" + nombreS3 + "' al bucket: " + bucketName);

            // Preparamos la estructura de la petición
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(nombreS3)
                    .build();

            // Sube el archivo físico a la nube
            s3.putObject(putOb, Paths.get(rutaLocal));
            System.out.println("¡Subido con éxito total a AWS S3!");

        } catch (Exception e) {
            System.err.println("Error real al subir a AWS: " + e.getMessage());
            throw new RuntimeException("Error en la conexión con AWS S3: " + e.getMessage());
        }
    }

    // 3. Validación de token de seguridad para el endpoint
    public boolean validarPermisos(String token) {
        return "TokenSecreto123".equals(token);
    }

    // 4. Obtener el archivo local para la descarga
    public byte[] obtenerArchivo(String fecha, String transportista, String idGuia) throws IOException {
        String fechaLimpia = fecha.replaceAll("-", "");
        String transportistaLimpio = transportista.replaceAll("\\s+", "");
        String rutaCompleta = RUTA_BASE_EFS + fechaLimpia + "/" + transportistaLimpio + "/guia" + idGuia + ".pdf";
        
        return Files.readAllBytes(Paths.get(rutaCompleta));
    }
}