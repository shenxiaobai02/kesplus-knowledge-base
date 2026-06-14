package com.kes.dto;

import com.kes.entity.IntentRecognitionResult;
import com.kes.enums.QueryIntent;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话上下文
 * <p>
 * 继承QueryContext，扩展多轮对话所需的上下文信息，包括：
 * - 会话标识和用户信息
 * - 槽位信息
 * - 对话历史
 * - 提到的实体列表（用于指代消解）
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ConversationContext extends QueryContext {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 槽位信息: entityName -> extractedValue
     */
    @Builder.Default
    private Map<String, String> slots = new HashMap<>();

    /**
     * 最后讨论的话题
     */
    private String lastTopic;

    /**
     * 对话历史
     */
    @Builder.Default
    private List<DialogTurn> dialogHistory = new ArrayList<>();

    /**
     * 提到的实体列表（用于指代消解）
     */
    @Builder.Default
    private List<String> mentionedEntities = new ArrayList<>();

    /**
     * 意图识别置信度
     */
    private double intentConfidence;

    /**
     * 创建新的对话上下文
     */
    public static ConversationContext create(String sessionId, String kbUuid, Long userId) {
        ConversationContext ctx = ConversationContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .slots(new HashMap<>())
                .dialogHistory(new ArrayList<>())
                .mentionedEntities(new ArrayList<>())
                .build();
        ctx.setKbUuid(kbUuid);
        return ctx;
    }

    /**
     * 更新上下文
     */
    public void update(IntentRecognitionResult intentResult, String originalQuery) {
        this.setIntent(intentResult.getIntent());
        this.intentConfidence = intentResult.getConfidence();
        this.slots = extractSlots(originalQuery, intentResult.getIntent());
    }

    /**
     * 添加对话轮次
     */
    public void addTurn(DialogTurn turn) {
        this.dialogHistory.add(turn);
    }

    /**
     * 添加用户轮次
     */
    public void addUserTurn(String content, QueryIntent intent) {
        addTurn(DialogTurn.userTurn(content, intent));
    }

    /**
     * 添加助手轮次
     */
    public void addAssistantTurn(String content) {
        addTurn(DialogTurn.assistantTurn(content));
    }

    /**
     * 添加提到的实体
     */
    public void addMentionedEntity(String entity) {
        if (entity != null && !entity.isEmpty()) {
            this.mentionedEntities.add(entity);
            // 保持实体列表在合理大小
            if (this.mentionedEntities.size() > 50) {
                this.mentionedEntities.remove(0);
            }
        }
    }

    /**
     * 解析代词
     * <p>
     * 从mentionedEntities中找到指代对象
     * </p>
     */
    public String resolvePronoun(String pronoun) {
        if (pronoun == null || pronoun.isEmpty()) {
            return pronoun;
        }

        // 简单的代词解析逻辑
        switch (pronoun) {
            case "它":
            case "他":
            case "她":
                if (!mentionedEntities.isEmpty()) {
                    return mentionedEntities.get(mentionedEntities.size() - 1);
                }
                break;
            case "这个":
            case "那个":
            case "这些":
            case "那些":
                if (!mentionedEntities.isEmpty()) {
                    return mentionedEntities.get(mentionedEntities.size() - 1);
                }
                break;
            default:
                break;
        }

        return pronoun;
    }

    /**
     * 提取槽位
     */
    private Map<String, String> extractSlots(String query, QueryIntent intent) {
        Map<String, String> slots = new HashMap<>();

        // 基于意图进行简单的槽位提取
        switch (intent) {
            case FACTS:
                // 提取实体
                extractEntitySlots(query, slots);
                break;
            case PROCEDURE:
                // 提取操作和对象
                extractProcedureSlots(query, slots);
                break;
            case COMPARISON:
                // 提取被比较的实体
                extractComparisonSlots(query, slots);
                break;
            default:
                break;
        }

        return slots;
    }

    /**
     * 提取实体槽位
     */
    private void extractEntitySlots(String query, Map<String, String> slots) {
        // 简单的实体提取逻辑 - 从查询中提取连续的中文或英文词组
        String[] words = query.split("[\\s，。、,.!?]+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 15) {
                // 简单判断是否为实体（包含中文或首字母大写的英文）
                if (containsChinese(word) || (Character.isUpperCase(word.charAt(0)) && word.length() > 1)) {
                    slots.put("entity", word);
                    addMentionedEntity(word);
                    break;
                }
            }
        }
    }

    /**
     * 提取流程槽位
     */
    private void extractProcedureSlots(String query, Map<String, String> slots) {
        // 提取操作词和目标对象
        String[] words = query.split("[\\s，。、,.!?]+");
        for (String word : words) {
            if (word.contains("如何") || word.contains("怎么") || word.contains("安装")
                    || word.contains("配置") || word.contains("使用")) {
                slots.put("action", word);
            }
        }
    }

    /**
     * 提取比较槽位
     */
    private void extractComparisonSlots(String query, Map<String, String> slots) {
        // 提取被比较的实体
        if (query.contains("和") || query.contains("与") || query.contains("vs")
                || query.contains("VS") || query.contains("比较")) {
            String[] parts = query.split("(和|与|vs|VS|比较)");
            if (parts.length >= 2) {
                slots.put("entity1", parts[0].trim());
                slots.put("entity2", parts[1].trim());
                addMentionedEntity(parts[0].trim());
                addMentionedEntity(parts[1].trim());
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
     * 获取历史问题数量
     */
    public int getHistorySize() {
        return dialogHistory.size();
    }

    /**
     * 获取最后一条用户消息
     */
    public String getLastUserMessage() {
        for (int i = dialogHistory.size() - 1; i >= 0; i--) {
            DialogTurn turn = dialogHistory.get(i);
            if (turn.isUserTurn()) {
                return turn.getContent();
            }
        }
        return null;
    }

    /**
     * 获取最后一条助手消息
     */
    public String getLastAssistantMessage() {
        for (int i = dialogHistory.size() - 1; i >= 0; i--) {
            DialogTurn turn = dialogHistory.get(i);
            if (turn.isAssistantTurn()) {
                return turn.getContent();
            }
        }
        return null;
    }
}
