const STORAGE_KEY = 'boardgame.muted';

let context: AudioContext | null = null;
let muted = false;

try {
  muted = localStorage.getItem(STORAGE_KEY) === '1';
} catch {
  muted = false;
}

export function isMuted(): boolean {
  return muted;
}

export function setMuted(next: boolean): void {
  muted = next;
  try {
    localStorage.setItem(STORAGE_KEY, next ? '1' : '0');
  } catch {
    // 隐私模式下写不了，忽略
  }
}

function ensureContext(): AudioContext | null {
  if (typeof window === 'undefined') {
    return null;
  }
  if (!context) {
    const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctor) {
      return null;
    }
    context = new Ctor();
  }
  if (context.state === 'suspended') {
    void context.resume();
  }
  return context;
}

function tone(frequency: number, delay: number, duration: number, type: OscillatorType, volume: number): void {
  const ctx = context;
  if (!ctx) {
    return;
  }
  const start = ctx.currentTime + delay;
  const oscillator = ctx.createOscillator();
  const gain = ctx.createGain();
  oscillator.type = type;
  oscillator.frequency.setValueAtTime(frequency, start);
  gain.gain.setValueAtTime(0.0001, start);
  gain.gain.exponentialRampToValueAtTime(volume, start + 0.01);
  gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
  oscillator.connect(gain).connect(ctx.destination);
  oscillator.start(start);
  oscillator.stop(start + duration + 0.02);
}

export type SoundName = 'click' | 'take' | 'buy' | 'error' | 'win';

/** 全部现场合成，不带任何音频文件。 */
export function play(name: SoundName): void {
  if (muted || !ensureContext()) {
    return;
  }
  switch (name) {
    case 'click':
      tone(520, 0, 0.05, 'square', 0.03);
      break;
    case 'take':
      tone(620, 0, 0.09, 'triangle', 0.06);
      tone(880, 0.06, 0.1, 'triangle', 0.05);
      break;
    case 'buy':
      tone(523.25, 0, 0.16, 'sine', 0.06);
      tone(659.25, 0.07, 0.18, 'sine', 0.05);
      tone(783.99, 0.14, 0.22, 'sine', 0.05);
      break;
    case 'error':
      tone(180, 0, 0.1, 'sawtooth', 0.04);
      break;
    case 'win':
      [523.25, 659.25, 783.99, 1046.5].forEach((frequency, index) => {
        tone(frequency, index * 0.11, 0.3, 'sine', 0.06);
      });
      break;
  }
}
