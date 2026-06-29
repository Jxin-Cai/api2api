import { PersonalSettingsPanel } from '@features/edit-personal-settings';
import { PageHeader } from '@shared/ui';

export default function SettingsPage() {
  return (
    <div className="app-page">
      <PageHeader title="个人设置" />
      <PersonalSettingsPanel />
    </div>
  );
}
