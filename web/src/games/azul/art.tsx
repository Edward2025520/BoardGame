import { useId, type CSSProperties } from 'react';
import { Art } from '../../components/Art';
import { AZUL_COLORS, type AzulColor } from '../../api/types';

const BASE: Record<AzulColor, { deep: string; light: string }> = {
  blue: { deep: '#1d5c9e', light: '#5ea3e0' },
  yellow: { deep: '#c79a1e', light: '#f2cf6b' },
  red: { deep: '#a8322b', light: '#e2736a' },
  black: { deep: '#2b2b33', light: '#5b5b6b' },
  white: { deep: '#dfe7ef', light: '#ffffff' },
};

/** 五种瓦片的自绘纹样。先做蓝、黄两套细节，其余保持同一风格。 */
function TileSvg({ color, uid }: { color: AzulColor; uid: string }) {
  const { deep, light } = BASE[color];
  const gid = `tile-${uid}`;
  const ink = color === 'white' ? '#7ba3c9' : 'rgba(255,255,255,0.85)';

  return (
    <svg viewBox="0 0 40 40" className="azul-tile-svg" aria-hidden="true">
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="0.4" y2="1">
          <stop offset="0%" stopColor={light} />
          <stop offset="100%" stopColor={deep} />
        </linearGradient>
      </defs>
      <rect x="1" y="1" width="38" height="38" rx="7" fill={`url(#${gid})`} stroke="rgba(0,0,0,0.35)" />
      <rect x="4" y="4" width="32" height="32" rx="5" fill="none" stroke={ink} strokeOpacity="0.5" />

      {color === 'blue' && (
        <g stroke={ink} strokeWidth="1.1" fill="none" strokeLinecap="round">
          <path d="M20 31 V19" />
          <path d="M20 22c-5 1-7-2-7-6 4 0 7 2 7 6z" fill={ink} fillOpacity="0.18" />
          <path d="M20 25c5 1 7-2 7-6-4 0-7 2-7 6z" fill={ink} fillOpacity="0.18" />
          <circle cx="20" cy="14" r="4.2" fill={ink} fillOpacity="0.22" />
          <circle cx="20" cy="14" r="1.6" fill={ink} fillOpacity="0.7" />
        </g>
      )}

      {color === 'yellow' && (
        <g stroke={ink} strokeWidth="1.1" fill="none" strokeLinecap="round">
          <path d="M11 27c4 0 6-3 8-6 2 3 5 5 9 4" />
          <path d="M20 20c0-4 2-7 6-8-1 4-2 7-6 8z" fill={ink} fillOpacity="0.22" />
          <circle cx="19" cy="13" r="2.2" fill={ink} fillOpacity="0.75" />
          <path d="M19 15l-4 4" />
          <path d="M12 29c3-1 6-1 8 0" />
        </g>
      )}

      {color === 'red' && (
        <g stroke={ink} strokeWidth="1.1" fill="none">
          <path d="M20 9l7 11-7 11-7-11z" fill={ink} fillOpacity="0.16" />
          <path d="M20 15l3.4 5-3.4 5-3.4-5z" fill={ink} fillOpacity="0.3" />
          <path d="M9 20h3M28 20h3M20 34v-3" />
        </g>
      )}

      {color === 'black' && (
        <g fill={ink} fillOpacity="0.8">
          <circle cx="20" cy="20" r="3" />
          <circle cx="20" cy="12" r="2" fillOpacity="0.5" />
          <circle cx="20" cy="28" r="2" fillOpacity="0.5" />
          <circle cx="12" cy="20" r="2" fillOpacity="0.5" />
          <circle cx="28" cy="20" r="2" fillOpacity="0.5" />
          <circle cx="14.3" cy="14.3" r="1.4" fillOpacity="0.35" />
          <circle cx="25.7" cy="25.7" r="1.4" fillOpacity="0.35" />
          <circle cx="25.7" cy="14.3" r="1.4" fillOpacity="0.35" />
          <circle cx="14.3" cy="25.7" r="1.4" fillOpacity="0.35" />
        </g>
      )}

      {color === 'white' && (
        <g stroke={ink} strokeWidth="1" fill="none" strokeLinecap="round">
          <path d="M10 26c4-6 6-8 10-8s6 2 10 8" />
          <path d="M12 30c3-4 5-5 8-5s5 1 8 5" />
          <path d="M16 14c2-2 6-2 8 0" />
          <circle cx="20" cy="18" r="1.4" fill={ink} />
        </g>
      )}
    </svg>
  );
}

export function Tile({ color, size = 30 }: { color: AzulColor; size?: number }) {
  const uid = useId().replace(/[^a-zA-Z0-9_-]/g, '');
  return (
    <span className="azul-tile" style={{ '--tile-size': `${size}px` } as CSSProperties} title={color}>
      <Art
        game="azul"
        name={`tile-${color}`}
        alt={color}
        fallback={<TileSvg color={color} uid={uid} />}
      />
    </span>
  );
}

export function StartMarkerTile({ size = 30 }: { size?: number }) {
  const uid = useId().replace(/[^a-zA-Z0-9_-]/g, '');
  return (
    <span className="azul-tile" style={{ '--tile-size': `${size}px` } as CSSProperties} title="首拿标记">
      <Art
        game="azul"
        name="tile-start"
        alt="首拿标记"
        fallback={
          <svg viewBox="0 0 40 40" className="azul-tile-svg" aria-hidden="true">
            <defs>
              <linearGradient id={`start-${uid}`} x1="0" y1="0" x2="0.4" y2="1">
                <stop offset="0%" stopColor="#3f6ea8" />
                <stop offset="100%" stopColor="#12233d" />
              </linearGradient>
            </defs>
            <rect x="1" y="1" width="38" height="38" rx="7" fill={`url(#start-${uid})`} stroke="rgba(0,0,0,0.4)" />
            <rect x="4" y="4" width="32" height="32" rx="5" fill="none" stroke="rgba(255,255,255,0.5)" />
            <text x="20" y="27" textAnchor="middle" fontSize="18" fontWeight="700" fill="#f4ecd6">
              1
            </text>
          </svg>
        }
      />
    </span>
  );
}

export { AZUL_COLORS };
