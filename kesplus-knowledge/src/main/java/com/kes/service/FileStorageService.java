package com.kes.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${rag.storage.path:./data/uploads}")
    private String storagePath;

    // 扩展支持的文件格式，与LangChain4j Tika解析器保持一致
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
        "txt", "md", "markdown", 
        "doc", "docx", "ppt", "pptx",
        "pdf", 
        "html", "htm", "xml",
        "csv", "tsv",
        "json", "yaml", "yml"
    );

    /**
     * 上传文件并校验格式
     */
    public String upload(MultipartFile file, String kbUuid) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        if (extension.isEmpty()) {
            log.warn("Upload failed: file has no extension, originalName={}", sanitizeFileName(originalFilename));
            throw new IllegalArgumentException("文件必须有扩展名");
        }
        
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("Upload failed: unsupported format={}, originalName={}", extension, sanitizeFileName(originalFilename));
            throw new IllegalArgumentException("不支持的文件格式: " + extension + "，支持的格式: " + String.join(", ", SUPPORTED_EXTENSIONS));
        }

        String fileUuid = UUID.randomUUID().toString();
        String filename = fileUuid + "." + extension;
        Path filePath = getFilePath(kbUuid, filename);

        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath.toFile());

        log.info("File uploaded: uuid={}, name={}, size={}KB, path={}", 
                fileUuid, sanitizeFileName(originalFilename), file.getSize() / 1024, filePath);
        return fileUuid;
    }

    /**
     * 解析文档 - 使用Apache Tika自动识别格式并解析
     * 优势：
     * 1. 基于文件魔数+后缀名双重识别，防止伪造格式
     * 2. 自动提取MIME类型存入metadata
     * 3. 统一处理所有格式，无需手动switch-case分支
     */
    public Document parse(String kbUuid, String fileUuid) throws IOException {
        File file = findFile(kbUuid, fileUuid);
        if (file == null) {
            log.error("Parse failed: file not found, kbUuid={}, fileUuid={}", kbUuid, fileUuid);
            throw new IllegalArgumentException("文件不存在: " + fileUuid);
        }
        
        if (!file.exists() || file.length() == 0) {
            log.error("Parse failed: file empty or deleted, path={}", file.getPath());
            throw new IllegalArgumentException("文件为空或已删除");
        }

        try {
            // 使用Tika自动识别文档类型并解析（底层：魔数校验+后缀初筛+内容特征匹配）
            ApacheTikaDocumentParser tikaParser = new ApacheTikaDocumentParser();
            Document document = tikaParser.parse(Files.newInputStream(file.toPath()));
            
            // 补充业务元数据（Tika已自动注入mime_type等元数据）
            Metadata metadata = document.metadata();
            metadata.put("fileName", file.getName());
            metadata.put("fileUuid", fileUuid);
            metadata.put("kbUuid", kbUuid);
            metadata.put("filePath", file.getAbsolutePath());
            metadata.put("fileSize", String.valueOf(file.length()));
            
            String mimeType = metadata.getString("mime_type");
            log.info("File parsed successfully: uuid={}, name={}, mimeType={}, size={}KB",
                    fileUuid, sanitizeFileName(file.getName()), mimeType, file.length() / 1024);
            
            return document;
        } catch (Exception e) {
            log.error("Parse failed: fileUuid={}, fileName={}, error={}", 
                    fileUuid, sanitizeFileName(file.getName()), e.getMessage(), e);
            throw new IOException("文档解析失败: " + e.getMessage(), e);
        }
    }

    public void delete(String kbUuid, String fileUuid) throws IOException {
        File file = findFile(kbUuid, fileUuid);
        if (file != null && file.exists()) {
            Files.delete(file.toPath());
            log.info("File deleted: {}", file.getPath());
        }
    }

    public List<FileInfo> listFiles(String kbUuid) {
        List<FileInfo> files = new ArrayList<>();
        Path kbPath = Paths.get(storagePath, kbUuid);
        
        if (!Files.exists(kbPath)) {
            return files;
        }

        try {
            Files.list(kbPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String filename = path.getFileName().toString();
                    String fileUuid = filename.substring(0, filename.lastIndexOf('.'));
                    String extension = filename.substring(filename.lastIndexOf('.') + 1);
                    
                    files.add(new FileInfo(fileUuid, filename, extension));
                });
        } catch (IOException e) {
            log.error("Failed to list files for kb: {}", kbUuid, e);
        }

        return files;
    }

    private File findFile(String kbUuid, String fileUuid) {
        Path kbPath = Paths.get(storagePath, kbUuid);
        
        if (!Files.exists(kbPath)) {
            return null;
        }

        try {
            return Files.list(kbPath)
                .filter(path -> path.getFileName().toString().startsWith(fileUuid))
                .findFirst()
                .map(Path::toFile)
                .orElse(null);
        } catch (IOException e) {
            log.error("Failed to find file: {}", fileUuid, e);
            return null;
        }
    }

    private Path getFilePath(String kbUuid, String filename) {
        return Paths.get(storagePath, kbUuid, filename);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * 文件名脱敏处理（安全规范：防止敏感信息泄露）
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        // 移除路径分隔符，防止路径遍历攻击
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @lombok.Data
    public static class FileInfo {
        private final String uuid;
        private final String filename;
        private final String extension;
    }
}