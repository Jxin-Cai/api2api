export interface CopyTextResult {
  ok: boolean;
  reason?: string;
}

function restoreSelection(selection: Selection | null, ranges: Range[]): void {
  if (!selection) {
    return;
  }
  selection.removeAllRanges();
  ranges.forEach((range) => selection.addRange(range));
}

function snapshotSelection(): { selection: Selection | null; ranges: Range[] } {
  const selection = document.getSelection();
  const ranges: Range[] = [];
  if (selection) {
    for (let index = 0; index < selection.rangeCount; index += 1) {
      ranges.push(selection.getRangeAt(index));
    }
  }
  return { selection, ranges };
}

function copySelectedText(): boolean {
  try {
    return document.execCommand('copy');
  } catch {
    return false;
  }
}

function selectElementText(element: HTMLElement): boolean {
  const selection = document.getSelection();
  if (!selection) {
    return false;
  }
  const range = document.createRange();
  range.selectNodeContents(element);
  selection.removeAllRanges();
  selection.addRange(range);
  return true;
}

function copyFromElement(element: HTMLElement): boolean {
  const { selection, ranges } = snapshotSelection();
  try {
    if (!selectElementText(element)) {
      return false;
    }
    return copyViaCopyEvent(element.textContent?.trim() ?? '') || copySelectedText();
  } finally {
    restoreSelection(selection, ranges);
  }
}

function copyViaCopyEvent(text: string): boolean {
  let wrote = false;
  const onCopy = (event: ClipboardEvent): void => {
    if (!event.clipboardData) {
      return;
    }
    event.preventDefault();
    event.clipboardData.setData('text/plain', text);
    wrote = true;
  };
  document.addEventListener('copy', onCopy);
  try {
    return copySelectedText() && wrote;
  } finally {
    document.removeEventListener('copy', onCopy);
  }
}

function resolveCopyHost(): HTMLElement {
  const modal = document.querySelector<HTMLElement>('.ant-modal-wrap:not([style*="display: none"]) .ant-modal');
  return modal ?? document.body;
}

function restoreFocus(element: HTMLElement | null): void {
  if (!element || !element.isConnected || typeof element.focus !== 'function') {
    return;
  }
  element.focus({ preventScroll: true });
}

/**
 * Synchronous copy that keeps the current user gesture.
 * Append inside an open Modal when present so the focus trap cannot steal the selection.
 */
function copyTextWithExecCommand(text: string, previousFocus: HTMLElement | null): boolean {
  const host = resolveCopyHost();
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.setAttribute('aria-hidden', 'true');
  textarea.style.position = 'fixed';
  textarea.style.top = '8px';
  textarea.style.left = '8px';
  textarea.style.width = '1px';
  textarea.style.height = '1px';
  textarea.style.padding = '0';
  textarea.style.border = '0';
  textarea.style.outline = 'none';
  textarea.style.boxShadow = 'none';
  textarea.style.background = 'transparent';
  textarea.style.opacity = '0.01';

  host.appendChild(textarea);
  textarea.focus({ preventScroll: true });
  textarea.select();
  textarea.setSelectionRange(0, textarea.value.length);

  const ok = copyViaCopyEvent(text) || copySelectedText();
  host.removeChild(textarea);
  restoreFocus(previousFocus);
  return ok;
}

function toCopyFailureReason(error: unknown): string {
  const message = error instanceof Error ? error.message : '';
  if (message.toLowerCase().includes('document is not focused')) {
    return '当前窗口未聚焦';
  }
  if (message) {
    return message;
  }
  return '浏览器拒绝复制操作';
}

function leaveSourceSelected(source?: HTMLElement | null): boolean {
  return Boolean(source?.isConnected && selectElementText(source));
}

export async function copyText(text: string, source?: HTMLElement | null): Promise<CopyTextResult> {
  const value = text.trim();
  if (!value) {
    return { ok: false, reason: '没有可复制的内容' };
  }

  const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;

  // Start the Clipboard API in the same turn as the click, before any focus changes.
  const clipboardPromise = window.isSecureContext && navigator.clipboard?.writeText
    ? navigator.clipboard.writeText(value)
    : null;

  if (source && source.isConnected && source.textContent?.trim() === value && copyFromElement(source)) {
    return { ok: true };
  }

  if (copyViaCopyEvent(value)) {
    return { ok: true };
  }

  try {
    if (copyTextWithExecCommand(value, previousFocus)) {
      return { ok: true };
    }
  } catch {
    // Fall through to the Clipboard API when execCommand is unavailable or rejected.
  }

  restoreFocus(previousFocus);

  if (clipboardPromise) {
    try {
      await Promise.race([
        clipboardPromise,
        new Promise<never>((_resolve, reject) => {
          window.setTimeout(() => reject(new Error('复制超时')), 1500);
        }),
      ]);
      return { ok: true };
    } catch (error: unknown) {
      const selected = leaveSourceSelected(source);
      return {
        ok: false,
        reason: selected ? '已选中文本，请按 ⌘C 或 Ctrl+C 复制' : toCopyFailureReason(error),
      };
    }
  }

  const selected = leaveSourceSelected(source);
  return {
    ok: false,
    reason: selected ? '已选中文本，请按 ⌘C 或 Ctrl+C 复制' : '浏览器拒绝复制操作',
  };
}
