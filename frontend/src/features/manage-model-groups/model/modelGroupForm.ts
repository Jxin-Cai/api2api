import type { ModelGroupResponse } from '@entities/model-group';

export type ModelGroupFormState =
  | { mode: 'create' }
  | { mode: 'edit'; group: ModelGroupResponse }
  | { mode: 'copy'; group: ModelGroupResponse; name: string };

export function createModelGroupCopyName(name: string, groups: ModelGroupResponse[]): string {
  const existingNames = new Set(groups.map((group) => group.name));
  for (let index = 1; ; index += 1) {
    const suffix = index === 1 ? ' - 副本' : ` - 副本 (${index})`;
    const characters = Array.from(name);
    while (characters.join('').length + suffix.length > 100) characters.pop();
    const candidate = `${characters.join('').trimEnd()}${suffix}`;
    if (!existingNames.has(candidate)) return candidate;
  }
}
