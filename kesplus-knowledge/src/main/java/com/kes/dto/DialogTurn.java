package com.kes.dto;

import com.kes.enums.QueryIntent;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话轮次
 * <p>
 * 记录一次对话的完整信息，包括角色、内容、意图和时间戳。
 * 用于对话历史的管理和上下文追踪。
 * </p>
 *
 * @author kesplus
 * @since 1.0.0
 */
@Data
public class DialogTurn {

    /**
     * 角色: user/assistant
     */
    private String role;

    /**
     * 对话内容
     */
    private String content;

    /**
     * 识别的意图
     */
    private QueryIntent intent;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建用户对话轮次
     */
    public static DialogTurn userTurn(String content, QueryIntent intent) {
        DialogTurn turn = new DialogTurn();
        turn.setRole("user");
        turn.setContent(content);
        turn.setIntent(intent);
        turn.setTimestamp(LocalDateTime.now());
        return turn;
    }

    /**
     * 创建助手对话轮次
     */
    public static DialogTurn assistantTurn(String content) {
        DialogTurn turn = new DialogTurn();
        turn.setRole("assistant");
        turn.setContent(content);
        turn.setTimestamp(LocalDateTime.now());
        return turn;
    }

    /**
     * 判断是否为用户轮次
     */
    public boolean isUserTurn() {
        return "user".equals(role);
    }

    /**
     * 判断是否为助手轮次
     */
    public boolean isAssistantTurn() {
        return "assistant".equals(role);
    }
}
