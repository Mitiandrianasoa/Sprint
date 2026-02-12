package com.sprint.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MultipartFile {
    private String name;
    private String originalFilename;
    private String contentType;
    private long size;
    private byte[] bytes;
    private InputStream inputStream;
    private boolean empty;

    public MultipartFile() {
    }

    public MultipartFile(String name, String originalFilename, String contentType, 
                        long size, byte[] bytes) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.bytes = bytes;
        this.empty = (bytes == null || bytes.length == 0);
    }

    // Getters et Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
        this.empty = (bytes == null || bytes.length == 0);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    // Méthodes utilitaires
    public String getExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
    }

    public String getFilenameWithoutExtension() {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            return originalFilename;
        }
        return originalFilename.substring(0, dotIndex);
    }

    public void transferTo(File dest) throws IOException {
        if (bytes != null) {
            Files.write(dest.toPath(), bytes);
        } else if (inputStream != null) {
            Files.copy(inputStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IOException("Aucune donnée disponible pour le transfert");
        }
    }

    public void transferTo(Path dest) throws IOException {
        transferTo(dest.toFile());
    }

    @Override
    public String toString() {
        return "MultipartFile{" +
                "name='" + name + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", empty=" + empty +
                '}';
    }
}