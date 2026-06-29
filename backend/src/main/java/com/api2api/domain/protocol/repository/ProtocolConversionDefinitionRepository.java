package com.api2api.domain.protocol.repository;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import java.util.List;
import java.util.Optional;

/**
 * 协议转换定义仓储接口。
 */
public interface ProtocolConversionDefinitionRepository {

    /**
     * 保存协议转换定义聚合。
     * 实现方按 id 判断新增或更新，并保证 sourceProtocol + targetProtocol 方向唯一。
     * 当方向重复、协议关系与转换类型不一致或持久化失败时，应抛出领域业务异常。
     *
     * @param definition 待保存的协议转换定义聚合
     */
    void save(ProtocolConversionDefinition definition);

    /**
     * 根据协议转换定义标识加载完整聚合。
     * 实现方需加载能力、请求/响应映射元数据与启停状态；不存在时返回 Optional.empty()。
     * id 为空或非法时，应抛出领域业务异常。
     *
     * @param id 协议转换定义标识
     * @return 完整协议转换定义聚合或空值
     */
    Optional<ProtocolConversionDefinition> findById(ProtocolConversionDefinitionId id);

    /**
     * 根据源协议和目标协议加载协议转换定义。
     * 用于路由和网关判断某一转换方向是否可用；不存在时返回 Optional.empty()。
     * sourceProtocol 或 targetProtocol 为空时，应抛出领域业务异常。
     *
     * @param sourceProtocol 客户端请求协议
     * @param targetProtocol 上游渠道协议
     * @return 对应方向的协议转换定义或空值
     */
    Optional<ProtocolConversionDefinition> findBySourceAndTarget(ProtocolType sourceProtocol, ProtocolType targetProtocol);

    /**
     * 加载全部协议转换定义。
     * 用于后台协议转换页面展示，结果应包含三种同协议 def 透传与六种异协议转换定义。
     * 无数据时返回空 List，不返回 null。
     *
     * @return 全部协议转换定义列表
     */
    List<ProtocolConversionDefinition> findAll();
}
