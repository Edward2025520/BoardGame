import { useRef, useState } from 'react';
import { getVolume, isMuted, play, setMuted, setVolume } from '../lib/audio';

/** 头部的音效设置：音量滑杆 + 静音开关，都写进 localStorage。 */
export function SoundPanel() {
  const [open, setOpen] = useState(false);
  const [volume, setVolumeState] = useState(getVolume());
  const [muted, setMutedState] = useState(isMuted());
  const lastPreview = useRef(0);

  const changeVolume = (next: number) => {
    setVolumeState(next);
    setVolume(next);
    const now = Date.now();
    if (now - lastPreview.current > 150) {
      lastPreview.current = now;
      play('select');
    }
  };

  const toggleMute = () => {
    const next = !muted;
    setMutedState(next);
    setMuted(next);
    if (!next) {
      play('take');
    }
  };

  return (
    <div className="sound-wrap">
      <button
        type="button"
        className="ghost"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        音效
      </button>
      {open && (
        <div className="sound-panel" role="dialog" aria-label="音效设置">
          <label className="sound-row">
            <span>音量</span>
            <input
              type="range"
              min={0}
              max={100}
              step={1}
              value={volume}
              onChange={(event) => changeVolume(Number(event.target.value))}
            />
            <span className="sound-value">{volume}</span>
          </label>
          <button type="button" className={muted ? 'chip active' : 'chip'} onClick={toggleMute}>
            {muted ? '已静音，点击开启' : '静音'}
          </button>
          <button type="button" className="chip" onClick={() => play('buy')}>
            试听
          </button>
        </div>
      )}
    </div>
  );
}
