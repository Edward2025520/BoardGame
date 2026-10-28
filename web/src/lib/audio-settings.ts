const VOLUME_KEY = 'boardgame.volume';
const MUTED_KEY = 'boardgame.muted';

export const DEFAULT_VOLUME = 70;

function read(key: string): string | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(key, value);
    }
  } catch {
    // 隐私模式下写不了，忽略
  }
}

export function clampVolume(value: number): number {
  if (!Number.isFinite(value)) {
    return DEFAULT_VOLUME;
  }
  return Math.min(100, Math.max(0, Math.round(value)));
}

export function loadVolume(): number {
  const raw = read(VOLUME_KEY);
  if (raw === null) {
    return DEFAULT_VOLUME;
  }
  return clampVolume(Number(raw));
}

export function saveVolume(value: number): void {
  write(VOLUME_KEY, String(clampVolume(value)));
}

export function loadMuted(): boolean {
  return read(MUTED_KEY) === '1';
}

export function saveMuted(muted: boolean): void {
  write(MUTED_KEY, muted ? '1' : '0');
}
