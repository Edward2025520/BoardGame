import { clampVolume, loadMuted, loadVolume, saveMuted, saveVolume } from './audio-settings';

let context: AudioContext | null = null;
let master: GainNode | null = null;
let volume = loadVolume();
let muted = loadMuted();

function ensureContext(): AudioContext | null {
  if (typeof window === 'undefined') {
    return null;
  }
  if (!context) {
    const Ctor =
      window.AudioContext ??
      (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctor) {
      return null;
    }
    context = new Ctor();
    master = context.createGain();
    master.gain.value = muted ? 0 : volume / 100;
    master.connect(context.destination);
    document.addEventListener('pointerdown', () => void context?.resume(), {
      once: true,
      capture: true,
    });
  }
  if (context.state === 'suspended') {
    void context.resume();
  }
  return context;
}

export function getVolume(): number {
  return volume;
}

export function setVolume(next: number): void {
  volume = clampVolume(next);
  saveVolume(volume);
  if (master && !muted) {
    master.gain.value = volume / 100;
  }
}

export function isMuted(): boolean {
  return muted;
}

export function setMuted(next: boolean): void {
  muted = next;
  saveMuted(muted);
  if (master) {
    master.gain.value = muted ? 0 : volume / 100;
  }
}

/** 12ms 线性起音 + 指数衰减，配方参考 shanzhai-splendor 的 sound.ts。 */
function tone(frequency: number, duration: number, delay: number, type: OscillatorType, gain: number): void {
  if (!context || !master) {
    return;
  }
  const start = context.currentTime + delay;
  const oscillator = context.createOscillator();
  const envelope = context.createGain();
  oscillator.type = type;
  oscillator.frequency.value = frequency;
  envelope.gain.setValueAtTime(0, start);
  envelope.gain.linearRampToValueAtTime(gain, start + 0.012);
  envelope.gain.exponentialRampToValueAtTime(0.0001, start + duration);
  oscillator.connect(envelope).connect(master);
  oscillator.start(start);
  oscillator.stop(start + duration + 0.02);
}

export type SoundName = 'select' | 'take' | 'buy' | 'error' | 'hint' | 'noble' | 'win';

export function play(name: SoundName): void {
  if (muted || !ensureContext()) {
    return;
  }
  try {
    switch (name) {
      case 'select':
        tone(660, 0.09, 0, 'triangle', 0.12);
        break;
      case 'take':
        tone(740, 0.09, 0, 'triangle', 0.12);
        tone(988, 0.1, 0.07, 'triangle', 0.1);
        break;
      case 'buy':
        tone(523, 0.14, 0, 'sine', 0.13);
        tone(784, 0.2, 0.09, 'sine', 0.12);
        break;
      case 'error':
        tone(196, 0.16, 0, 'square', 0.07);
        break;
      case 'hint':
        tone(880, 0.08, 0, 'sine', 0.1);
        tone(1175, 0.12, 0.07, 'sine', 0.1);
        break;
      case 'noble':
        tone(523, 0.12, 0, 'sine', 0.14);
        tone(659, 0.12, 0.09, 'sine', 0.14);
        tone(784, 0.2, 0.18, 'sine', 0.14);
        break;
      case 'win':
        tone(523, 0.14, 0, 'triangle', 0.15);
        tone(659, 0.14, 0.12, 'triangle', 0.15);
        tone(784, 0.14, 0.24, 'triangle', 0.15);
        tone(1047, 0.32, 0.36, 'triangle', 0.16);
        break;
    }
  } catch {
    // 音频不可用时静默
  }
}
