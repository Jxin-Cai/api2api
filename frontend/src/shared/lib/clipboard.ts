export interface CopyTextResult {
  ok: boolean;
  reason?: string;
}

function copyTextWithTextarea(text: string): CopyTextResult {
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', 'true');
  textarea.style.position = 'fixed';
  textarea.style.top = '0';
  textarea.style.left = '-9999px';
  textarea.style.opacity = '0';
  textarea.style.pointerEvents = 'none';

  const selection = document.getSelection();
  const ranges: Range[] = [];
  if (selection) {
    for (let index = 0; index < selection.rangeCount; index += 1) {
      ranges.push(selection.getRangeAt(index));
    }
  }

  document.body.appendChild(textarea);
  textarea.focus({ preventScroll: true });
  textarea.select();
  textarea.setSelectionRange(0, textarea.value.length);

  const ok = document.execCommand('copy');
  document.body.removeChild(textarea);

  if (selection) {
    selection.removeAllRanges();
    ranges.forEach((range) => selection.addRange(range));
  }

  return ok ? { ok: true } : { ok: false, reason: '浏览器拒绝复制操作' };
}

export async function copyText(text: string): Promise<CopyTextResult> {
  let clipboardError: unknown;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return { ok: true };
    }
  } catch (error: unknown) {
    clipboardError = error;
  }

  try {
    return copyTextWithTextarea(text);
  } catch (error: unknown) {
    return {
      ok: false,
      reason: error instanceof Error ? error.message : clipboardError instanceof Error ? clipboardError.message : '复制失败',
    };
  }
}
