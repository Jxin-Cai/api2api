import { Alert, Button, Form, Input, InputNumber, Modal, Select, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState, type ReactElement } from 'react';

import type { ModelDailyLimits, ModelGroupResponse, SaveModelGroupRequest } from '@entities/model-group';

import { useModelGroupMutations } from '../model/useModelGroupMutations';

interface ModelGroupFormModalProps {
  open: boolean;
  group: ModelGroupResponse | null;
  modelOptions: Array<{ label: string; value: string }>;
  onClose: () => void;
  onSaved: (group: ModelGroupResponse) => void;
}

interface DailyLimitRow {
  model: string;
  limit: number | undefined;
}

const EMPTY_FORM: SaveModelGroupRequest = { name: '', modelWhitelist: [], modelDailyLimits: {} };

/** 仅保留仍在白名单内且为正整数的上限，避免提交已移除模型的残留配置 */
function pruneDailyLimits(limits: ModelDailyLimits, whitelist: string[]): ModelDailyLimits {
  const allowed = new Set(whitelist);
  return Object.fromEntries(
    Object.entries(limits).filter(([model, limit]) => allowed.has(model) && Number.isFinite(limit) && limit > 0)
  );
}

export function ModelGroupFormModal({ open, group, modelOptions, onClose, onSaved }: ModelGroupFormModalProps) {
  const { createMutation, updateMutation } = useModelGroupMutations();
  const [form, setForm] = useState<SaveModelGroupRequest>(EMPTY_FORM);

  useEffect((): void => {
    if (open) {
      setForm(group
        ? { name: group.name, modelWhitelist: group.modelWhitelist, modelDailyLimits: { ...group.modelDailyLimits } }
        : EMPTY_FORM);
    }
  }, [group, open]);

  const mutation = group ? updateMutation : createMutation;

  function setDailyLimit(model: string, limit: number | null): void {
    setForm((current) => {
      const modelDailyLimits = { ...current.modelDailyLimits };
      if (limit === null || !Number.isFinite(limit) || limit <= 0) {
        delete modelDailyLimits[model];
      } else {
        modelDailyLimits[model] = Math.floor(limit);
      }
      return { ...current, modelDailyLimits };
    });
  }

  async function handleSubmit(): Promise<void> {
    const params: SaveModelGroupRequest = {
      name: form.name.trim(),
      modelWhitelist: form.modelWhitelist,
      modelDailyLimits: pruneDailyLimits(form.modelDailyLimits, form.modelWhitelist),
    };
    if (!params.name) {
      return;
    }
    const saved = group
      ? await updateMutation.mutateAsync({ id: group.id, params })
      : await createMutation.mutateAsync(params);
    onSaved(saved);
    onClose();
  }

  const dailyLimitRows: DailyLimitRow[] = form.modelWhitelist.map((model) => ({ model, limit: form.modelDailyLimits[model] }));
  const dailyLimitColumns: ColumnsType<DailyLimitRow> = [
    { title: '模型', dataIndex: 'model', key: 'model', ellipsis: true },
    {
      title: '每日上限（Token）',
      key: 'limit',
      width: 240,
      render: (_: unknown, row): ReactElement => (
        <InputNumber
          value={row.limit}
          min={1}
          step={100000}
          precision={0}
          style={{ width: '100%' }}
          placeholder="不限"
          disabled={mutation.isPending}
          formatter={(value): string => (value === undefined || value === null || String(value) === '' ? '' : Number(value).toLocaleString())}
          parser={(value): number => Number((value ?? '').replace(/[^\d]/g, ''))}
          onChange={(value): void => setDailyLimit(row.model, typeof value === 'number' ? value : null)}
          aria-label={`${row.model} 每日上限`}
        />
      ),
    },
  ];

  return (
    <Modal title={group ? '编辑模型分组' : '创建模型分组'} open={open} onCancel={onClose} footer={null} width={640} destroyOnHidden>
      <Form layout="vertical" onFinish={handleSubmit}>
        <Form.Item label="分组名称" required validateStatus={!form.name.trim() ? 'error' : undefined} help={!form.name.trim() ? '请输入分组名称' : undefined}>
          <Input autoFocus value={form.name} maxLength={100} onChange={(event): void => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="例如：生产环境标准模型" disabled={mutation.isPending} />
        </Form.Item>
        <Form.Item label="允许的大模型" extra="留空表示该分组禁止调用所有模型。修改后会立即作用于该分组下的全部 Key。">
          <Select
            mode="tags"
            value={form.modelWhitelist}
            onChange={(modelWhitelist: string[]): void => setForm((current) => ({ ...current, modelWhitelist }))}
            options={modelOptions}
            placeholder="选择或输入允许的模型名"
            disabled={mutation.isPending}
          />
          {modelOptions.length === 0 ? <Alert type="info" showIcon message="暂无已配置模型，也可以手动输入模型名。" style={{ marginTop: 8 }} /> : null}
        </Form.Item>
        <Form.Item
          label="模型每日上限"
          extra="按模型限制分组内所有 Key 当天合计可消耗的加权 Token；留空表示不限。达到上限后，该模型对分组内全部 Key 返回限流错误，次日自动恢复。"
        >
          {dailyLimitRows.length === 0 ? (
            <Typography.Text type="secondary">先在上方选择允许的模型，再为其设置每日上限。</Typography.Text>
          ) : (
            <Table<DailyLimitRow> rowKey="model" size="small" pagination={false} columns={dailyLimitColumns} dataSource={dailyLimitRows} scroll={{ y: 240 }} />
          )}
        </Form.Item>
        {mutation.isError ? <Alert type="error" showIcon message="保存失败" description={mutation.error.message} style={{ marginBottom: 16 }} /> : null}
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button onClick={onClose} disabled={mutation.isPending}>取消</Button>
          <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={!form.name.trim()}>保存分组</Button>
        </Space>
      </Form>
    </Modal>
  );
}
