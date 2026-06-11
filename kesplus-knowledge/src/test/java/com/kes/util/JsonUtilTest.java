package com.kes.util;

import com.kes.entity.KnowledgeBase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void testToJson() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTitle("Test KB");
        kb.setRemark("Test remark");

        String json = JsonUtil.toJson(kb);

        assertNotNull(json);
        assertTrue(json.contains("\"title\":\"Test KB\""));
        assertTrue(json.contains("\"remark\":\"Test remark\""));
    }

    @Test
    void testFromJson() {
        String json = "{\"title\":\"Test KB\",\"remark\":\"Test remark\",\"isPublic\":false}";

        KnowledgeBase kb = JsonUtil.fromJson(json, KnowledgeBase.class);

        assertNotNull(kb);
        assertEquals("Test KB", kb.getTitle());
        assertEquals("Test remark", kb.getRemark());
        assertFalse(kb.getIsPublic());
    }

    @Test
    void testToJsonNull() {
        String result = JsonUtil.toJson(null);

        assertNull(result);
    }

    @Test
    void testFromJsonNull() {
        KnowledgeBase result = JsonUtil.fromJson(null, KnowledgeBase.class);

        assertNull(result);
    }

    @Test
    void testToJsonList() {
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setTitle("KB1");
        KnowledgeBase kb2 = new KnowledgeBase();
        kb2.setTitle("KB2");

        String json = JsonUtil.toJson(Arrays.asList(kb1, kb2));

        assertNotNull(json);
        assertTrue(json.contains("\"title\":\"KB1\""));
        assertTrue(json.contains("\"title\":\"KB2\""));
    }

    @Test
    void testFromJsonList() {
        String json = "[{\"title\":\"KB1\"},{\"title\":\"KB2\"}]";

        List<KnowledgeBase> list = JsonUtil.fromJsonArray(json, KnowledgeBase.class);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("KB1", list.get(0).getTitle());
        assertEquals("KB2", list.get(1).getTitle());
    }
}
