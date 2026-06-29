package com.api2api.domain.protocol.service;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionPayload;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.ConversionResult;
import com.api2api.domain.protocol.model.ConversionRoute;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import java.util.List;

/**
 * 协议转换领域服务契约。
 * 实现方负责基于领域定义选择可用路线，并执行请求/响应 JSON 或 SSE 片段转换。
 */
public interface ProtocolConversionService {

    /**
     * 从候选定义集合中解析一条源协议到目标协议的可用转换路线。
     * 实现方必须校验定义存在、已启用、实现状态为 IMPLEMENTED，并满足流式、工具调用与 reasoning 能力要求。
     * 校验失败时抛出 ProtocolConversionException，调用方不得继续转发上游。
     *
     * @param sourceProtocol 客户端请求协议
     * @param targetProtocol 上游目标协议
     * @param requirement 本次调用的转换需求
     * @param definitions 候选协议转换定义集合
     * @return 已选中且可路由的转换路线
     */
    ConversionRoute resolve(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    );

    /**
     * 将客户端请求 payload 转换为目标上游协议 payload。
     * 同协议时返回透传结果；异协议时由实现方执行对应方向的请求字段转换策略。
     * 转换失败时抛出 ProtocolConversionException。
     *
     * @param payload 客户端请求协议载体
     * @param targetProtocol 上游目标协议
     * @param requirement 本次调用的转换需求
     * @param definitions 候选协议转换定义集合
     * @return 协议转换结果
     */
    ProtocolConversionResult convertRequest(
            ProtocolPayload payload,
            ProtocolType targetProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    );

    /**
     * 将上游响应 payload 转换回原始客户端协议 payload。
     * 同协议时返回透传结果；异协议时由实现方执行对应方向的响应字段转换策略，并可提取统一 usage。
     * 转换失败时抛出 ProtocolConversionException。
     *
     * @param payload 上游响应协议载体
     * @param originalClientProtocol 原始客户端协议
     * @param requirement 本次调用的转换需求
     * @param definitions 候选协议转换定义集合
     * @return 协议转换结果
     */
    ProtocolConversionResult convertResponse(
            ProtocolPayload payload,
            ProtocolType originalClientProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    );

    /**
     * 兼容设计文档中 ConversionRequirement 命名的路线解析契约。
     */
    default ConversionRoute resolve(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ConversionRequirement requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        return resolve(sourceProtocol, targetProtocol, requirement.toProtocolConversionRequest(), definitions);
    }

    /**
     * 兼容设计文档中 ConversionPayload/ConversionResult 命名的请求转换契约。
     */
    default ConversionResult convertRequest(
            ConversionPayload payload,
            ProtocolType targetProtocol,
            ConversionRequirement requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        return ConversionResult.from(convertRequest(
                payload.toProtocolPayload(),
                targetProtocol,
                requirement.toProtocolConversionRequest(),
                definitions
        ));
    }

    /**
     * 兼容设计文档中 ConversionPayload/ConversionResult 命名的响应转换契约。
     */
    default ConversionResult convertResponse(
            ConversionPayload payload,
            ProtocolType originalClientProtocol,
            ConversionRequirement requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        return ConversionResult.from(convertResponse(
                payload.toProtocolPayload(),
                originalClientProtocol,
                requirement.toProtocolConversionRequest(),
                definitions
        ));
    }
}
