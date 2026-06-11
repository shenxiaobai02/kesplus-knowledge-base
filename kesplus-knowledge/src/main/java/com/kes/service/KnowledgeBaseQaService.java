package com.kes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kes.config.RagConfig;
import com.kes.dto.response.QaResponse;
import com.kes.entity.KnowledgeBase;
import com.kes.entity.KnowledgeBaseEmbedding;
import com.kes.entity.KnowledgeBaseQa;
import com.kes.mapper.KnowledgeBaseMapper;
import com.kes.mapper.KnowledgeBaseQaMapper;
import com.kes.util.UuidUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KnowledgeBaseQaService extends ServiceImpl<KnowledgeBaseQaMapper, KnowledgeBaseQa> {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private EmbeddingRagService embeddingRagService;

    @Autowired
    private EmbeddingModelService embeddingModelService;

    @Autowired
    private RagConfig ragConfig;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Autowired(required = false)
    private StreamingChatModel streamingChatModel;

    private static final String RAG_PROMPT_TEMPLATE = """
        你是一个知识库问答助手，根据提供的参考文档回答用户问题。

        参考文档：
        {context}

        用户问题：
        {question}

        请根据参考文档内容，用简洁准确的语言回答用户问题。
        如果参考文档中没有相关信息，请明确说明"根据现有知识库，无法回答该问题"。
        """;

    public QaResponse qa(String kbUuid, String question, Integer maxResults, Double minScore, Double temperature) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(kbUuid);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }

        com.kes.entity.EmbeddingModel entityModel = embeddingModelService.getByUuid(kb.getEmbeddingModelUuid());
        if (entityModel == null) {
            throw new RuntimeException("嵌入模型不存在");
        }
        
        EmbeddingModel embeddingModel = embeddingModelService.createLangChainEmbeddingModel(entityModel);
        if (embeddingModel == null) {
            throw new RuntimeException("嵌入模型创建失败");
        }

        List<KnowledgeBaseEmbedding> retrieved = embeddingRagService.retrieve(kb, question, embeddingModel);

        String context = buildContext(retrieved);
        String answer = generateAnswer(question, context);

        int promptTokens = estimateTokens(context + question);
        int answerTokens = estimateTokens(answer);

        KnowledgeBaseQa qaRecord = saveQaRecord(kbUuid, question, answer, promptTokens, answerTokens);

        QaResponse response = QaResponse.fromEntity(qaRecord);
        response.setReferences(buildReferences(retrieved));

        log.info("QA completed for kb: {}, question: {}", kbUuid, question);
        return response;
    }

    public void streamQa(String kbUuid, String question, Integer maxResults, Double minScore,
                         Double temperature, SseEmitter emitter) {
        try {
            KnowledgeBase kb = knowledgeBaseMapper.selectByUuid(kbUuid);
            if (kb == null) {
                emitter.send(SseEmitter.event().name("error").data("知识库不存在"));
                emitter.complete();
                return;
            }

            com.kes.entity.EmbeddingModel entityModel = embeddingModelService.getByUuid(kb.getEmbeddingModelUuid());
            if (entityModel == null) {
                emitter.send(SseEmitter.event().name("error").data("嵌入模型不存在"));
                emitter.complete();
                return;
            }
            
            EmbeddingModel embeddingModel = embeddingModelService.createLangChainEmbeddingModel(entityModel);
            if (embeddingModel == null) {
                emitter.send(SseEmitter.event().name("error").data("嵌入模型创建失败"));
                emitter.complete();
                return;
            }

            List<KnowledgeBaseEmbedding> retrieved = embeddingRagService.retrieve(kb, question, embeddingModel);
            String context = buildContext(retrieved);

            StringBuilder fullAnswer = new StringBuilder();

            if (streamingChatModel != null) {
                String prompt = buildPrompt(question, context);
                streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            fullAnswer.append(partialResponse);
                            emitter.send(SseEmitter.event().name("token").data(partialResponse));
                        } catch (IOException e) {
                            log.error("Failed to send SSE event", e);
                        }
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                        // Response is already complete
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        log.error("Streaming chat error", error);
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                        } catch (IOException e) {
                            log.error("Failed to send error event", e);
                        }
                    }
                });
            }

            emitter.send(SseEmitter.event().name("complete").data(""));
            emitter.complete();

            int promptTokens = estimateTokens(context + question);
            int answerTokens = estimateTokens(fullAnswer.toString());

            saveQaRecord(kbUuid, question, fullAnswer.toString(), promptTokens, answerTokens);

            log.info("Stream QA completed for kb: {}, question: {}", kbUuid, question);

        } catch (Exception e) {
            log.error("Stream QA failed", e);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            } catch (IOException ex) {
                log.error("Failed to send error event", ex);
            } finally {
                emitter.completeWithError(e);
            }
        }
    }

    public List<QaResponse> getHistory(String kbUuid, int limit) {
        List<KnowledgeBaseQa> records = baseMapper.selectByKbUuid(kbUuid);
        List<QaResponse> responses = new ArrayList<>();
        for (int i = 0; i < Math.min(records.size(), limit); i++) {
            responses.add(QaResponse.fromEntity(records.get(i)));
        }
        return responses;
    }

    @Transactional
    public KnowledgeBaseQa saveQaRecord(String kbUuid, String question, String answer,
                                         Integer promptTokens, Integer answerTokens) {
        KnowledgeBaseQa qa = new KnowledgeBaseQa();
        qa.setUuid(UuidUtil.create());
        qa.setKbUuid(kbUuid);
        qa.setQuestion(question);
        qa.setAnswer(answer);
        qa.setPromptTokens(promptTokens);
        qa.setAnswerTokens(answerTokens);
        qa.setIsDeleted(false);
        qa.setCreatedTime(LocalDateTime.now());

        baseMapper.insert(qa);
        return qa;
    }

    private String buildContext(List<KnowledgeBaseEmbedding> embeddings) {
        StringBuilder context = new StringBuilder();
        for (KnowledgeBaseEmbedding embedding : embeddings) {
            context.append(embedding.getText()).append("\n\n");
        }
        return context.toString().trim();
    }

    private List<QaResponse.ReferenceDocument> buildReferences(List<KnowledgeBaseEmbedding> embeddings) {
        List<QaResponse.ReferenceDocument> references = new ArrayList<>();
        for (KnowledgeBaseEmbedding embedding : embeddings) {
            QaResponse.ReferenceDocument ref = new QaResponse.ReferenceDocument();
            ref.setText(embedding.getText());
            ref.setMetadataJson(embedding.getMetadataJson());
            references.add(ref);
        }
        return references;
    }

    private String generateAnswer(String question, String context) {
        if (chatModel == null) {
            throw new RuntimeException("LLM模型未配置");
        }

        String prompt = buildPrompt(question, context);
        ChatResponse response = chatModel.chat(UserMessage.from(prompt));
        return response.aiMessage().text();
    }

    private String buildPrompt(String question, String context) {
        return String.format(RAG_PROMPT_TEMPLATE, context, question);
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }
}
