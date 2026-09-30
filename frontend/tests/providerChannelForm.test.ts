import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { ChannelModelSupportResponse } from '../src/entities/channel-model-support/model/types';
import type { ProviderChannelResponse } from '../src/entities/provider-channel/model/types';
import {
  derivePreviewUpstreamProtocols,
  deriveSupportedProtocols,
  isHttpHost,
  mergeWithExistingModels,
  normalizeProtocolMappings,
  sanitizeEditableKey,
} from '../src/features/manage-provider-channels/model/providerChannelForm';

test('test_retains_selected_mapping_order_when_protocol_selection_changes', () => {
  // Arrange
  const selected = ['CLAUDE_MESSAGES', 'OPENAI_RESPONSES'];
  const mappings = [
    { requestProtocol: 'OPENAI_CHAT_COMPLETIONS', upstreamProtocol: 'CLAUDE_MESSAGES' },
    { requestProtocol: 'CLAUDE_MESSAGES', upstreamProtocol: 'OPENAI_CHAT_COMPLETIONS' },
  ];

  // Act
  const result = normalizeProtocolMappings(selected, mappings);

  // Assert
  assert.deepEqual(result, [
    { requestProtocol: 'CLAUDE_MESSAGES', upstreamProtocol: 'OPENAI_CHAT_COMPLETIONS' },
    { requestProtocol: 'OPENAI_RESPONSES', upstreamProtocol: 'OPENAI_RESPONSES' },
  ]);
});

test('test_uses_last_saved_mapping_when_entry_protocol_has_duplicate_mappings', () => {
  // Arrange
  const mappings = [
    { requestProtocol: 'CLAUDE_MESSAGES', upstreamProtocol: 'OPENAI_CHAT_COMPLETIONS' },
    { requestProtocol: 'CLAUDE_MESSAGES', upstreamProtocol: 'OPENAI_RESPONSES' },
  ];

  // Act
  const result = normalizeProtocolMappings(['CLAUDE_MESSAGES'], mappings);

  // Assert
  assert.deepEqual(result, [{ requestProtocol: 'CLAUDE_MESSAGES', upstreamProtocol: 'OPENAI_RESPONSES' }]);
});

test('test_deduplicates_preview_protocols_when_entries_share_an_upstream', () => {
  // Arrange
  const selected = ['CLAUDE_MESSAGES', 'OPENAI_RESPONSES', 'OPENAI_IMAGES'];
  const mappings = [{ requestProtocol: 'CLAUDE_MESSAGES', upstreamProtocol: 'OPENAI_RESPONSES' }];

  // Act
  const result = derivePreviewUpstreamProtocols(selected, mappings);

  // Assert
  assert.deepEqual(result, ['OPENAI_RESPONSES', 'OPENAI_IMAGES']);
});

test('test_derives_entries_from_mappings_when_legacy_protocol_list_disagrees', () => {
  // Arrange
  const value = channel({ protocolMappings: [{ requestProtocol: 'OPENAI_RESPONSES', upstreamProtocol: 'CLAUDE_MESSAGES' }] });

  // Act
  const result = deriveSupportedProtocols(value);

  // Assert
  assert.deepEqual(result, ['OPENAI_RESPONSES']);
});

test('test_uses_legacy_protocols_when_channel_has_no_mappings', () => {
  // Arrange
  const value = channel({ protocolMappings: [] });

  // Act
  const result = deriveSupportedProtocols(value);

  // Assert
  assert.deepEqual(result, ['CLAUDE_MESSAGES']);
});

const hosts: Array<[string, string, boolean]> = [
  ['https_host_has_whitespace', ' https://example.test ', true],
  ['http_scheme_is_uppercase', 'HTTP://example.test', true],
  ['scheme_is_missing', 'example.test', false],
  ['scheme_is_unsupported', 'ftp://example.test', false],
];
for (const [condition, host, expected] of hosts) {
  test(`test_checks_http_scheme_when_${condition}`, () => {
    // Act
    const result = isHttpHost(host);

    // Assert
    assert.equal(result, expected);
  });
}

const keys: Array<[string, string | undefined, string | undefined, string]> = [
  ['key_is_absent', undefined, undefined, ''],
  ['key_is_blank', '  ', undefined, ''],
  ['key_contains_mask', ' example-**** ', undefined, ''],
  ['key_equals_saved_mask', ' masked-value ', 'masked-value', ''],
  ['key_is_new_plaintext', ' new-example-key ', 'masked-value', 'new-example-key'],
];
for (const [condition, value, mask, expected] of keys) {
  test(`test_prepares_editable_key_when_${condition}`, () => {
    // Act
    const result = sanitizeEditableKey(value, mask);

    // Assert
    assert.equal(result, expected);
  });
}

test('test_preserves_saved_configuration_when_preview_matches_existing_model', () => {
  // Arrange
  const existing = Object.freeze(model({ id: 17, priority: 3, preferred: true, status: 'DISABLED', source: 'MANUAL' }));
  const fetched = Object.freeze(model({ id: 23, upstreamModel: 'new-upstream-model' }));

  // Act
  const result = mergeWithExistingModels([fetched], [existing]);

  // Assert
  assert.deepEqual(result, [{
    ...fetched, id: 17, priority: 3, preferred: true, status: 'DISABLED', source: 'MANUAL',
  }]);
});

test('test_retains_missing_models_when_upstream_preview_omits_saved_models', () => {
  // Arrange
  const existing = model({ id: 17 });
  const missing = model({ id: 18, requestedModel: 'missing-model' });
  const fetched = model({ id: 23 });
  const added = model({ id: 24, requestedModel: 'new-model' });

  // Act
  const result = mergeWithExistingModels([fetched, added], [missing, existing]);

  // Assert
  assert.deepEqual(result.map((item) => item.id), [17, 24, 18]);
});

test('test_keeps_protocol_variants_separate_when_requested_model_name_matches', () => {
  // Arrange
  const existing = model({ id: 17, upstreamProtocol: 'CLAUDE_MESSAGES' });
  const fetched = model({ id: 23, upstreamProtocol: 'OPENAI_RESPONSES' });

  // Act
  const result = mergeWithExistingModels([fetched], [existing]);

  // Assert
  assert.deepEqual(result, [fetched, existing]);
});

test('test_uses_first_existing_match_when_saved_model_keys_are_duplicated', () => {
  // Arrange
  const existing = [model({ id: 17 }), model({ id: 18 })];
  const fetched = model({ id: 23 });

  // Act
  const result = mergeWithExistingModels([fetched], existing);

  // Assert
  assert.deepEqual(result.map((item) => item.id), [17]);
});

test('test_keeps_preview_ids_when_creating_or_copying_channel', () => {
  // Arrange
  const fetched = [model({ id: 23 })];

  // Act
  const result = mergeWithExistingModels(fetched, []);

  // Assert
  assert.deepEqual(result, fetched);
});

function model(overrides: Partial<ChannelModelSupportResponse> = {}): ChannelModelSupportResponse {
  return {
    id: 1, requestedModel: 'example-model', upstreamModel: 'example-upstream',
    upstreamProtocol: 'OPENAI_RESPONSES', priority: 10, preferred: false,
    status: 'ENABLED', source: 'FETCHED', ...overrides,
  };
}

function channel(overrides: Partial<ProviderChannelResponse> = {}): ProviderChannelResponse {
  return {
    id: 1, name: 'example-channel', host: 'https://example.test', keyRef: '',
    supportedProtocols: ['CLAUDE_MESSAGES'], supportedModels: [], status: 'ENABLED', ...overrides,
  };
}
