import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

import { clearChunkReloadFlag, handleChunkLoadFailure, isChunkLoadError } from '@shared/lib';

export function lazyWithRetry(
  importer: () => Promise<{ default: ComponentType }>
): LazyExoticComponent<ComponentType> {
  return lazy(async () => {
    try {
      const module = await importer();
      clearChunkReloadFlag();
      return module;
    } catch (error: unknown) {
      if (isChunkLoadError(error) && handleChunkLoadFailure()) {
        return await new Promise<{ default: ComponentType }>(() => undefined);
      }
      throw error;
    }
  });
}
