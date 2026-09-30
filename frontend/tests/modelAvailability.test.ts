import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createElement } from 'react';
import { renderToString } from 'react-dom/server';
import { QueryClient, QueryClientProvider, QueryObserver } from '@tanstack/react-query';

import { useProviderChannelMutations } from '../src/features/manage-provider-channels/model/useProviderChannelMutations';
import { useChannelModelMutations } from '../src/features/manage-channel-models/model/useChannelModelMutations';
import { MODEL_GROUPS_QUERY_KEY } from '../src/entities/model-group/model/useModelGroups';
import { listModelGroups } from '../src/entities/model-group/api/modelGroupApi';
import { providerChannelQueryKeys } from '../src/entities/provider-channel/model/useProviderChannels';
import { providerModelQueryKeys } from '../src/entities/provider-model/model/useProviderModels';

const affectedKeys = [providerChannelQueryKeys.all, providerModelQueryKeys.all, MODEL_GROUPS_QUERY_KEY];

function renderHook<T>(client: QueryClient, hook: () => T): T {
  let result: T | undefined;
  function Probe() {
    result = hook();
    return null;
  }
  renderToString(createElement(QueryClientProvider, { client }, createElement(Probe)));
  return result!;
}

function createClient(): QueryClient {
  return new QueryClient({ defaultOptions: { queries: { staleTime: Infinity, retry: false }, mutations: { retry: false } } });
}

type Mutations = {
  channels: ReturnType<typeof useProviderChannelMutations>;
  models: ReturnType<typeof useChannelModelMutations>;
};

const changes: Array<[string, (mutations: Mutations) => Promise<unknown>]> = [
  ['provider_disabled', ({ channels }) => channels.disableMutation.mutateAsync(1)],
  ['provider_enabled', ({ channels }) => channels.enableMutation.mutateAsync(1)],
  ['provider_deleted', ({ channels }) => channels.deleteMutation.mutateAsync(1)],
  ['provider_rate_limits_reset', ({ channels }) => channels.resetAllRateLimitsMutation.mutateAsync()],
  ['models_replaced', ({ models }) => models.batchUpsertMutation.mutateAsync({ channelId: 1, body: { models: [], replaceExisting: true } })],
  ['model_removed', ({ models }) => models.removeMutation.mutateAsync({ channelId: 1, body: { requestedModel: 'test-model', upstreamProtocol: 'OPENAI_RESPONSES' } })],
];

for (const [condition, change] of changes) {
  test(`test_invalidates_related_cached_queries_when_${condition}`, async (context) => {
    // Arrange
    const client = createClient();
    context.after(() => client.clear());
    affectedKeys.forEach((queryKey) => client.setQueryData(queryKey, []));
    context.mock.method(globalThis, 'fetch', async () => Response.json({ code: 'SUCCESS', data: {} }));
    const mutations = renderHook(client, () => ({ channels: useProviderChannelMutations(), models: useChannelModelMutations() }));

    // Act
    await change(mutations);

    // Assert
    assert.deepEqual(affectedKeys.map((key) => client.getQueryState(key)?.isInvalidated), [true, true, true]);
  });
}

test('test_refetches_active_group_support_when_provider_is_disabled_and_reenabled', async (context) => {
  // Arrange
  const client = createClient();
  context.after(() => client.clear());
  let enabled = true;
  context.mock.method(globalThis, 'fetch', async (url: string) => {
    if (url.endsWith('/disable')) enabled = false;
    if (url.endsWith('/enable')) enabled = true;
    return Response.json({ code: 'SUCCESS', data: {} });
  });
  const observer = new QueryObserver(client, {
    queryKey: MODEL_GROUPS_QUERY_KEY,
    queryFn: async () => ({ effectiveModels: enabled ? ['test-model'] : [] }),
  });
  const unsubscribe = observer.subscribe(() => undefined);
  context.after(unsubscribe);
  await observer.refetch();
  const mutations = renderHook(client, useProviderChannelMutations);

  // Act
  await mutations.disableMutation.mutateAsync(1);
  const disabled = client.getQueryData(MODEL_GROUPS_QUERY_KEY);
  await mutations.enableMutation.mutateAsync(1);
  const restored = client.getQueryData(MODEL_GROUPS_QUERY_KEY);

  // Assert
  assert.deepEqual([disabled, restored], [{ effectiveModels: [] }, { effectiveModels: ['test-model'] }]);
});

test('test_preserves_configured_models_and_limits_when_no_effective_support_exists', async (context) => {
  // Arrange
  context.mock.method(globalThis, 'fetch', async () => Response.json({ code: 'SUCCESS', data: { groups: [{
    id: 1, name: 'test', modelWhitelist: ['test-model'], effectiveModels: [], modelDailyLimits: { 'test-model': '100' },
  }] } }));

  // Act
  const { data } = await listModelGroups();

  // Assert
  const group = data.groups[0];
  assert.deepEqual({ configured: group.modelWhitelist, effective: group.effectiveModels, limits: group.modelDailyLimits },
    { configured: ['test-model'], effective: [], limits: { 'test-model': 100 } });
});
