import { useState } from 'react';
import { Alert, Form, Modal, Select } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { testProviderChannel, type ProviderChannelResponse } from '@entities/provider-channel';
import { getApiErrorMessage } from '@shared/api';
import { formatProtocolDirection, getProtocolMeta } from '@shared/lib/protocols';
import { deriveSupportedProtocols, normalizeProtocolMappings } from '../model/providerChannelForm';

interface Props {
  channel: ProviderChannelResponse;
  onClose: () => void;
}

export function ProviderChannelTestModal({ channel, onClose }: Props) {
  const mappings = normalizeProtocolMappings(deriveSupportedProtocols(channel), channel.protocolMappings);
  const firstMapping = mappings.find((mapping) => channel.supportedModels.some(
    (model) => model.upstreamProtocol === mapping.upstreamProtocol
  ));
  const [protocol, setProtocol] = useState(firstMapping?.requestProtocol ?? mappings[0]?.requestProtocol);
  const [selectedModelId, setSelectedModelId] = useState<number>();
  const upstream = mappings.find((mapping) => mapping.requestProtocol === protocol)?.upstreamProtocol;
  const models = channel.supportedModels.filter((model) => model.upstreamProtocol === upstream);
  const modelId = selectedModelId ?? (models.find((model) => model.status === 'ENABLED') ?? models[0])?.id;
  const isImage = upstream === 'OPENAI_IMAGES';
  const test = useMutation({
    mutationFn: (selection: { requestProtocol: string; modelId: number }) =>
      testProviderChannel(channel.id, selection.requestProtocol, selection.modelId),
    retry: false,
  });
  const result = test.data?.data;

  return (
    <Modal
      open
      title={`测试渠道 · ${channel.name}`}
      onCancel={onClose}
      cancelText="关闭"
      okText={isImage ? '生成测试图片' : '发送 hello'}
      confirmLoading={test.isPending}
      okButtonProps={{ disabled: !modelId }}
      onOk={() => {
        if (modelId && !test.isPending) test.mutate({ requestProtocol: protocol, modelId });
      }}
    >
      <Form layout="vertical" disabled={test.isPending}>
        <Form.Item label="协议映射" htmlFor="channel-test-protocol">
          <Select
            id="channel-test-protocol"
            value={protocol}
            options={mappings.map(({ requestProtocol, upstreamProtocol }) => ({
              value: requestProtocol,
              label: requestProtocol === upstreamProtocol ? getProtocolMeta(requestProtocol).label
                : formatProtocolDirection(requestProtocol, upstreamProtocol),
            }))}
            onChange={(value) => {
              setProtocol(value);
              setSelectedModelId(undefined);
              test.reset();
            }}
          />
        </Form.Item>
        <Form.Item
          label="测试模型"
          htmlFor="channel-test-model"
          extra={isImage ? '将以 hello 为提示词调用上游生成一张图片。' : undefined}
        >
          <Select
            id="channel-test-model"
            showSearch
            optionFilterProp="label"
            placeholder="该协议尚未配置模型"
            disabled={models.length === 0 || test.isPending}
            value={modelId}
            options={models.map((model) => {
              const name = model.requestedModel === model.upstreamModel
                ? model.requestedModel : `${model.requestedModel} → ${model.upstreamModel}`;
              const status = model.status === 'DISABLED' ? '（已禁用）' : model.status === 'RATE_LIMITED' ? '（限流中）' : '';
              return { value: model.id, label: name + status };
            })}
            onChange={(id) => {
              setSelectedModelId(id);
              test.reset();
            }}
          />
        </Form.Item>
      </Form>
      {test.isPending && <Alert type="info" showIcon message="已发送，正在等待上游响应…" />}
      {test.isError && <Alert type="error" showIcon message="测试失败" description={getApiErrorMessage(test.error, '渠道请求失败')} />}
      {result && <Alert type="success" showIcon message={`测试成功 · HTTP ${result.statusCode} · ${result.elapsedMillis} ms`}
        description={<span style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{result.reply}</span>} />}
    </Modal>
  );
}
