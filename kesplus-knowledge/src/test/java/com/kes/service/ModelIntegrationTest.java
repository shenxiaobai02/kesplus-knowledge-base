package com.kes.service;

import com.kes.entity.EmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ModelIntegrationTest {

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @org.springframework.beans.factory.annotation.Value("${rag.embedding.api-key}")
    private String siliconFlowApiKey;

    @Test
    void testOllamaModelIntegration() {
        EmbeddingModel localModel = new EmbeddingModel();
        localModel.setModelName("quentinz/bge-small-zh-v1.5:latest");
        localModel.setEmbeddingDimension(512);
        localModel.setModelType("ollama");
        localModel.setBaseUrl("http://localhost:11434");

        dev.langchain4j.model.embedding.EmbeddingModel langChainModel = 
            embeddingModelService.createLangChainEmbeddingModel(localModel);
        
        assertNotNull(langChainModel, "Ollama模型创建失败");

        String testText = "This is a test document for embedding.";
        Response<Embedding> response = langChainModel.embed(testText);
        Embedding embedding = response.content();
        
        assertNotNull(embedding, "嵌入生成失败");
        assertEquals(512, embedding.vector().length, "嵌入维度不匹配");
        
        System.out.println("✅ Ollama模型测试成功！维度: " + embedding.vector().length);
    }

    @Test
    void testSiliconFlowModelIntegration() {
        EmbeddingModel remoteModel = new EmbeddingModel();
        remoteModel.setModelName("BAAI/bge-m3");
        remoteModel.setEmbeddingDimension(1024);
        remoteModel.setModelType("huggingface");
        remoteModel.setBaseUrl("https://api.siliconflow.cn/v1");
        remoteModel.setApiKey(siliconFlowApiKey); // 从配置文件读取

        dev.langchain4j.model.embedding.EmbeddingModel langChainModel = 
            embeddingModelService.createLangChainEmbeddingModel(remoteModel);
        
        assertNotNull(langChainModel, "SiliconFlow模型创建失败");

        String testText = "这是一个用于测试嵌入的文档。";
        Response<Embedding> response = langChainModel.embed(testText);
        Embedding embedding = response.content();
        
        assertNotNull(embedding, "嵌入生成失败");
        assertEquals(1024, embedding.vector().length, "嵌入维度不匹配");
        
        System.out.println("✅ SiliconFlow模型测试成功！维度: " + embedding.vector().length);
    }

    @Test
    void testDocumentEmbedding() {
        EmbeddingModel model = new EmbeddingModel();
        model.setModelName("BAAI/bge-m3");
        model.setEmbeddingDimension(1024);
        model.setModelType("huggingface");
        model.setBaseUrl("https://api.siliconflow.cn/v1");
        model.setApiKey(siliconFlowApiKey); // 从配置文件读取

        dev.langchain4j.model.embedding.EmbeddingModel langChainModel = 
            embeddingModelService.createLangChainEmbeddingModel(model);
        
        Document document = Document.from("人工智能是计算机科学的一个分支，致力于研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统。");
        
        Response<Embedding> response = langChainModel.embed(document.text());
        Embedding embedding = response.content();
        
        assertNotNull(embedding);
        assertTrue(embedding.vector().length > 0);
        
        System.out.println("✅ 文档嵌入测试成功！向量长度: " + embedding.vector().length);
    }
}
