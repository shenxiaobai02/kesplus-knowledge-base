package com.kes.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    private static final List<String> SUPPORTED_EXTENSIONS = List.of("txt", "md", "doc", "docx", "pdf");

    public String upload(MultipartFile file, String kbUuid) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }

        String fileUuid = UUID.randomUUID().toString();
        String filename = fileUuid + "." + extension;
        Path filePath = getFilePath(kbUuid, filename);

        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath.toFile());

        log.info("File uploaded: {} -> {}", originalFilename, filePath);
        return fileUuid;
    }

    public Document parse(String kbUuid, String fileUuid) throws IOException {
        File file = findFile(kbUuid, fileUuid);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在: " + fileUuid);
        }

        String extension = getFileExtension(file.getName());
        String content = switch (extension.toLowerCase()) {
            case "txt" -> parseTxt(file);
            case "md" -> parseMd(file);
            case "doc" -> parseDoc(file);
            case "docx" -> parseDocx(file);
            case "pdf" -> parsePdf(file);
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };

        Metadata metadata = Metadata.from("fileName", file.getName())
            .add("fileUuid", fileUuid)
            .add("kbUuid", kbUuid);

        return Document.from(content, metadata);
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

    private String parseTxt(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    private String parseMd(File file) throws IOException {
        String markdown = Files.readString(file.toPath());
        Parser parser = Parser.builder().build();
        TextContentRenderer renderer = new TextContentRenderer();
        return renderer.render(parser.parse(markdown));
    }

    private String parseDoc(File file) throws IOException {
        try (InputStream is = Files.newInputStream(file.toPath());
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String parseDocx(File file) throws IOException {
        try (InputStream is = Files.newInputStream(file.toPath());
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String parsePdf(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    @lombok.Data
    public static class FileInfo {
        private final String uuid;
        private final String filename;
        private final String extension;
    }
}