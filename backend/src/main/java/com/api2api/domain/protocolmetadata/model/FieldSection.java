package com.api2api.domain.protocolmetadata.model;

public enum FieldSection {
    MESSAGE("消息内容"),
    CONTENT_BLOCK("内容块"),
    MODEL("模型参数"),
    TOOL("工具调用"),
    REASONING("推理"),
    USAGE("用量统计"),
    STREAMING("流式"),
    METADATA("元数据"),
    OTHER("其他");

    private final String label;

    FieldSection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
