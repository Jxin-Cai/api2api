import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { AppProviders } from '@app/providers/AppProviders';
import { router } from '@app/router';
import { useThemeStore } from '@shared/config/stores/useThemeStore';
import { clearChunkReloadFlag, handleChunkLoadFailure } from '@shared/lib';
import '@app/styles/reset.css';
import '@app/styles/tokens.css';
import '@app/styles/global.css';

window.addEventListener('vite:preloadError', (event) => {
  if (handleChunkLoadFailure()) {
    event.preventDefault();
  }
});

clearChunkReloadFlag();

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Root element #root was not found.');
}

// 首屏即应用持久化的主题，避免闪烁
document.documentElement.setAttribute('data-theme', useThemeStore.getState().mode);

createRoot(rootElement).render(
  <StrictMode>
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  </StrictMode>
);
