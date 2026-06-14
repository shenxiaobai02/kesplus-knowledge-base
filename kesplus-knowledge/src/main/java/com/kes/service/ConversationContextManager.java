package com.kes.service;

import com.kes.dto.ConversationContext;
import com.kes.dto.DialogTurn;
import com.kes.enums.QueryIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 对话上下文管理器
 * <p>
 * 负责管理多轮对话的上下文状态，包括：
 * - 对话上下文的创建和获取
 * - 上下文状态更新
 * - 对话历史的添加和管理
 * - 基于Redis的分布式存储
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Slf4j
@Service
public class ConversationContextManager {

    private static final String CONTEXT_KEY_PREFIX = "conversation:context:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取或创建对话上下文
     *
     * @param sessionId 会话ID
     * @param kbUuid    知识库UUID
     * @param userId    用户ID
     * @return 对话上下文
     */
    public ConversationContext getOrCreateContext(String sessionId, String kbUuid, Long userId) {
        if (redisTemplate == null) {
            log.debug("Redis not available, using in-memory context");
            return ConversationContext.create(sessionId, kbUuid, userId);
        }

        String key = buildKey(sessionId, kbUuid);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof ConversationContext) {
                log.debug("Found existing context for session: {}, kb: {}", sessionId, kbUuid);
                return (ConversationContext) value;
            }
        } catch (Exception e) {
            log.warn("Failed to get context from Redis: {}", e.getMessage());
        }

        // 创建新上下文
        log.debug("Creating new context for session: {}, kb: {}", sessionId, kbUuid);
        ConversationContext context = ConversationContext.create(sessionId, kbUuid, userId);
        saveContext(key, context);
        return context;
    }

    /**
     * 保存对话上下文
     *
     * @param sessionId 会话ID
     * @param kbUuid    知识库UUID
     * @param context   对话上下文
     */
    public void saveContext(String sessionId, String kbUuid, ConversationContext context) {
        if (redisTemplate == null) {
            log.debug("Redis not available, context not saved");
            return;
        }

        String key = buildKey(sessionId, kbUuid);
        saveContext(key, context);
    }

    /**
     * 保存对话上下文
     */
    private void saveContext(String key, ConversationContext context) {
        try {
            redisTemplate.opsForValue().set(key, context, DEFAULT_TTL);
            log.debug("Context saved with key: {}", key);
        } catch (Exception e) {
            log.error("Failed to save context to Redis: {}", e.getMessage());
        }
    }

    /**
     * 添加对话轮次
     *
     * @param sessionId 会话ID
     * @param kbUuid    知识库UUID
     * @param role      角色
     * @param content   内容
     * @param intent    意图
     */
    public void addTurn(String sessionId, String kbUuid, String role, String content, QueryIntent intent) {
        ConversationContext context = getOrCreateContext(sessionId, kbUuid, null);

        DialogTurn turn = new DialogTurn();
        turn.setRole(role);
        turn.setContent(content);
        turn.setIntent(intent);
        turn.setTimestamp(java.time.LocalDateTime.now());

        context.addTurn(turn);

        // 如果是用户轮次，提取实体
        if ("user".equals(role)) {
            extractAndStoreEntities(content, context);
        }

        saveContext(sessionId, kbUuid, context);
    }

    /**
     * 添加用户轮次
     */
    public void addUserTurn(String sessionId, String kbUuid, String content, QueryIntent intent) {
        addTurn(sessionId, kbUuid, "user", content, intent);
    }

    /**
     * 添加助手轮次
     */
    public void addAssistantTurn(String sessionId, String kbUuid, String content) {
        addTurn(sessionId, kbUuid, "assistant", content, null);
    }

    /**
     * 更新对话上下文
     *
     * @param sessionId 会话ID
     * @param kbUuid    知识库UUID
     * @param context   对话上下文
     */
    public void updateContext(String sessionId, String kbUuid, ConversationContext context) {
        saveContext(sessionId, kbUuid, context);
    }

    /**
     * 删除对话上下文
     *
     * @param sessionId 会话ID
     * @param kbUuid    知识库UUID
     */
    public void deleteContext(String sessionId, String kbUuid) {
        if (redisTemplate == null) {
            return;
        }

        String key = buildKey(sessionId, kbUuid);
        try {
            redisTemplate.delete(key);
            log.debug("Context deleted with key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete context from Redis: {}", e.getMessage());
        }
    }

    /**
     * 获取上下文TTL
     *
     * @param sessionId 会话ID
     * @param kbUuid    知识库UUID
     * @return TTL
     */
    public Optional<Duration> getContextTtl(String sessionId, String kbUuid) {
        if (redisTemplate == null) {
            return Optional.empty();
        }

        String key = buildKey(sessionId, kbUuid);
        try {
            Long ttlSeconds = redisTemplate.getExpire(key);
            if (ttlSeconds != null && ttlSeconds > 0) {
                return Optional.of(Duration.ofSeconds(ttlSeconds));
            }
        } catch (Exception e) {
            log.warn("Failed to get TTL from Redis: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * 提取并存储实体
     */
    private void extractAndStoreEntities(String content, ConversationContext context) {
        // 简单的实体提取逻辑
        String[] words = content.split("[\\s，。、,.!?]+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 15 && containsChinese(word)) {
                context.addMentionedEntity(word);
            }
        }
    }

    /**
     * 判断是否包含中文
     */
    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建缓存Key
     */
    private String buildKey(String sessionId, String kbUuid) {
        return CONTEXT_KEY_PREFIX + kbUuid + ":" + sessionId;
    }

    /**
     * 检查Redis是否可用
     */
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }
}
