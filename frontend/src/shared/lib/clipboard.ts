export interface CopyTextResult {
  ok: boolean;
  reason?: string;
}

export async function copyText(text: string): Promise<CopyTextResult> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return { ok: true };
    }
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', 'true');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(textarea);
    return ok ? { ok: true } : { ok: false, reason: '浏览器拒绝复制操作' };
  } catch (error: unknown) {
    return { ok: false, reason: error instanceof Error ? error.message : '复制失败' };
  }
}
