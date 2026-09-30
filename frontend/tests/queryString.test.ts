import assert from 'node:assert/strict';
import { test } from 'node:test';
import { appendQuery } from '../src/shared/api/http';
import { toQueryString, type QueryObject } from '../src/shared/lib/queryString';
import { buildAppUsageQuery } from '../src/shared/lib/usageQuery';

const cases: Array<[string, QueryObject, string]> = [
  ['values_are_empty', { absent: undefined, nil: null, empty: '', list: [] }, ''],
  ['values_are_zero_or_false', { offset: 0, descending: false }, 'offset=0&descending=false'],
  ['values_include_arrays', { model: ['a', null, '', 'b'], enabled: [false, true] }, 'model=a&model=b&enabled=false&enabled=true'],
  ['values_need_encoding', { model: '模型 / a+b&c' }, 'model=%E6%A8%A1%E5%9E%8B+%2F+a%2Bb%26c'],
];

for (const [condition, params, expected] of cases) {
  test(`test_serializes_filters_consistently_when_${condition}`, () => {
    // Arrange
    const suffix = expected ? `?${expected}` : '';

    // Act
    const results = [toQueryString(params), appendQuery('/usage', params), buildAppUsageQuery(params)];

    // Assert
    assert.deepEqual(results, [expected, `/usage${suffix}`, suffix]);
  });
}

test('test_appends_repeated_values_when_path_already_has_query', () => {
  // Arrange
  const path = '/usage?model=old&limit=50';

  // Act
  const result = appendQuery(path, { model: ['new', 'other'], offset: 0 });

  // Assert
  assert.equal(result, '/usage?model=old&limit=50&model=new&model=other&offset=0');
});

test('test_preserves_path_verbatim_when_params_are_absent', () => {
  // Arrange
  const path = '/usage?model=a%20b';

  // Act
  const result = appendQuery(path);

  // Assert
  assert.equal(result, path);
});

test('test_normalizes_existing_query_when_params_are_empty', () => {
  // Arrange
  const path = '/usage?model=a%20b';

  // Act
  const result = appendQuery(path, {});

  // Assert
  assert.equal(result, '/usage?model=a+b');
});
