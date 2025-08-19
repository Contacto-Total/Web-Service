package com.foh.contacto_total_web_service.common.util;

public class FileIdentifier {
    public static String generateUniqueFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        int extensionIndex = originalFileName.lastIndexOf('.');
        String baseName = (extensionIndex == -1) ? originalFileName : originalFileName.substring(0, extensionIndex);
        String extension = (extensionIndex == -1) ? "" : originalFileName.substring(extensionIndex);
        return baseName + "-" + timestamp + extension;
    }
}
