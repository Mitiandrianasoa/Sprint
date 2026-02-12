package com.sprint.util;

import com.sprint.model.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MultipartRequestHandler {

    /**
     * Vérifie si une requête est de type multipart/form-data
     */
    public static boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    /**
     * Extrait tous les fichiers d'une requête multipart
     */
    public static Map<String, MultipartFile> extractMultipartFiles(HttpServletRequest request) 
            throws IOException, jakarta.servlet.ServletException {
        
        Map<String, MultipartFile> files = new HashMap<>();
        
        if (!isMultipartRequest(request)) {
            return files;
        }
        
        Collection<Part> parts = request.getParts();
        
        for (Part part : parts) {
            if (part.getSize() > 0 && part.getSubmittedFileName() != null) {
                // C'est un fichier uploadé
                MultipartFile multipartFile = convertPartToMultipartFile(part);
                files.put(part.getName(), multipartFile);
            }
        }
        
        return files;
    }

    /**
     * Extrait tous les paramètres textuels d'une requête multipart
     */
    public static Map<String, String> extractMultipartParameters(HttpServletRequest request) 
            throws IOException, jakarta.servlet.ServletException {
        
        Map<String, String> parameters = new HashMap<>();
        
        if (!isMultipartRequest(request)) {
            // Pour les requêtes non-multipart, utiliser les paramètres normaux
            Map<String, String[]> paramMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    parameters.put(entry.getKey(), entry.getValue()[0]);
                }
            }
            return parameters;
        }
        
        Collection<Part> parts = request.getParts();
        
        for (Part part : parts) {
            if (part.getSubmittedFileName() == null) {
                // C'est un paramètre textuel
                InputStream inputStream = part.getInputStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String value = result.toString("UTF-8");
                parameters.put(part.getName(), value);
            }
        }
        
        return parameters;
    }

    /**
     * Convertit une Part Servlet en MultipartFile
     */
    private static MultipartFile convertPartToMultipartFile(Part part) throws IOException {
        MultipartFile multipartFile = new MultipartFile();
        
        multipartFile.setName(part.getName());
        multipartFile.setOriginalFilename(part.getSubmittedFileName());
        multipartFile.setContentType(part.getContentType());
        multipartFile.setSize(part.getSize());
        
        // Lire le contenu du fichier
        InputStream inputStream = part.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        int nRead;
        byte[] data = new byte[16384]; // 16KB buffer
        
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        buffer.flush();
        multipartFile.setBytes(buffer.toByteArray());
        multipartFile.setInputStream(inputStream);
        multipartFile.setEmpty(part.getSize() == 0);
        
        return multipartFile;
    }

    /**
     * Extrait un fichier spécifique par son nom
     */
    public static MultipartFile getMultipartFile(HttpServletRequest request, String name) 
            throws IOException, jakarta.servlet.ServletException {
        
        if (!isMultipartRequest(request)) {
            return null;
        }
        
        Part part = request.getPart(name);
        if (part != null && part.getSize() > 0) {
            return convertPartToMultipartFile(part);
        }
        
        return null;
    }

    /**
     * Sauvegarde tous les fichiers uploadés dans un répertoire
     */
    public static List<String> saveAllFiles(HttpServletRequest request, String uploadDir) 
            throws IOException, jakarta.servlet.ServletException {
        
        List<String> savedFiles = new ArrayList<>();
        
        if (!isMultipartRequest(request)) {
            return savedFiles;
        }
        
        Collection<Part> parts = request.getParts();
        
        for (Part part : parts) {
            if (part.getSize() > 0 && part.getSubmittedFileName() != null) {
                String fileName = part.getSubmittedFileName();
                String filePath = uploadDir + File.separator + fileName;
                
                part.write(filePath);
                savedFiles.add(filePath);
            }
        }
        
        return savedFiles;
    }
}