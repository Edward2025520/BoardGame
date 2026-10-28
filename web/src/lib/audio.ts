import { clampVolume, loadMuted, loadVolume, saveMuted, saveVolume } from './audio-settings';

let context: AudioContext | null = null;
let master: GainNode | null = null;
let noiseBuffer: AudioBuffer | null = null;
let volume = loadVolume();
let muted = loadMuted();

function effectiveGain(): number {
  return muted ? 0 : (volume / 100) * 0.9;
}

function createNoise(ctx: AudioContext, seconds: number): AudioBuffer {
  const length = Math.floor(ctx.sampleRate * seconds);
  const buffer = ctx.createBuffer(1, length, ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let index = 0; index < length; index++) {
    data[index] = Math.random() * 2 - 1;
  }
  return buffer;
}

function unlock(): void {
  void context?.resume();
}

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
    master.gain.value = effectiveGain();
    master.connect(context.destination);
    noiseBuffer = createNoise(context, 0.4);
    document.addEventListener('pointerdown', unlock, { once: true, capture: true });
  }
  return context;
}

export function getVolume(): number {
  return volume;
}

export function setVolume(next: number): void {
  volume = clampVolume(next);
  saveVolume(volume);
  if (master) {
    master.gain.value = effectiveGain();
  }
}

export function isMuted(): boolean {
  return muted;
}

export function setMuted(next: boolean): void {
  muted = next;
  saveMuted(muted);
  if (master) {
    master.gain.value = effectiveGain();
  }
}

interface SoundContext {
  ctx: AudioContext;
  out: GainNode;
  now: number;
}

function tone(
  sound: SoundContext,
  frequency: number,
  delay: number,
  duration: number,
  type: OscillatorType,
  peak: number,
  endFrequency?: number,
): void {
  const start = sound.now + delay;
  const oscillator = sound.ctx.createOscillator();
  oscillator.type = type;
  oscillator.frequency.setValueAtTime(frequency, start);
  if (endFrequency) {
    oscillator.frequency.exponentialRampToValueAtTime(endFrequency, start + duration);
  }

  const gain = sound.ctx.createGain();
  gain.gain.setValueAtTime(0.0001, start);
  gain.gain.exponentialRampToValueAtTime(peak, start + 0.006);
  gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);

  oscillator.connect(gain).connect(sound.out);
  oscillator.start(start);
  oscillator.stop(start + duration + 0.03);
}

function noise(
  sound: SoundContext,
  delay: number,
  duration: number,
  frequency: number,
  q: number,
  peak: number,
  endFrequency?: number,
): void {
  if (!noiseBuffer) {
    return;
  }
  const start = sound.now + delay;
  const source = sound.ctx.createBufferSource();
  source.buffer = noiseBuffer;

  const filter = sound.ctx.createBiquadFilter();
  filter.type = 'bandpass';
  filter.Q.value = q;
  filter.frequency.setValueAtTime(frequency, start);
  if (endFrequency) {
    filter.frequency.exponentialRampToValueAtTime(endFrequency, start + duration);
  }

  const gain = sound.ctx.createGain();
  gain.gain.setValueAtTime(0.0001, start);
  gain.gain.exponentialRampToValueAtTime(peak, start + 0.008);
  gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);

  source.connect(filter).connect(gain).connect(sound.out);
  source.start(start);
  source.stop(start + duration + 0.02);
}

export type SoundName = 'select' | 'take' | 'buy' | 'error' | 'win';

function render(name: SoundName, ctx: AudioContext, out: GainNode): void {
  const sound: SoundContext = { ctx, out, now: ctx.currentTime + 0.01 };
  switch (name) {
    case 'select':
      // 玻璃珠：一段高频带通噪声 + 极短正弦
      noise(sound, 0, 0.05, 2200, 9, 0.16);
      tone(sound, 1880, 0, 0.05, 'sine', 0.07);
      break;
    case 'take':
      // 硬币落桌：两个金属短音
      tone(sound, 880, 0, 0.1, 'triangle', 0.16);
      tone(sound, 1320, 0.025, 0.1, 'triangle', 0.09);
      break;
    case 'buy':
      // 纸牌滑动 + 一声轻和弦
      noise(sound, 0, 0.14, 900, 1.4, 0.14, 320);
      tone(sound, 523.25, 0.03, 0.2, 'sine', 0.14);
      tone(sound, 783.99, 0.1, 0.22, 'sine', 0.1);
      break;
    case 'error':
      tone(sound, 220, 0, 0.14, 'sine', 0.12, 160);
      break;
    case 'win':
      [523.25, 659.25, 783.99, 1046.5].forEach((frequency, index) => {
        tone(sound, frequency, index * 0.11, 0.34, 'sine', 0.13);
      });
      noise(sound, 0.4, 0.3, 3200, 0.7, 0.05);
      break;
  }
}

/** 全部现场合成，不带任何音频文件。 */
export function play(name: SoundName): void {
  const ctx = ensureContext();
  if (!ctx || !master || muted) {
    return;
  }
  const out = master;
  if (ctx.state === 'suspended') {
    void ctx.resume().then(() => render(name, ctx, out));
    return;
  }
  render(name, ctx, out);
}
