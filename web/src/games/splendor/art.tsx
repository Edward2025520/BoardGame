import { Art } from '../../components/Art';
import { GEM_COLOR, GEM_LABEL, type Gem, type SplendorCard, type SplendorNoble } from '../../api/types';

const TIER_COLOR: Record<number, string> = {
  1: '#4f9d69',
  2: '#c9a227',
  3: '#4a6fb5',
};

export function GemDot({ gem, x, y, r }: { gem: string; x: number; y: number; r: number }) {
  const color = GEM_COLOR[gem as Gem] ?? '#888';
  return (
    <g>
      <circle cx={x} cy={y} r={r} fill={color} stroke="rgba(0,0,0,0.45)" strokeWidth={1.5} />
      <text x={x} y={y + r * 0.36} textAnchor="middle" fontSize={r * 0.95} fill={gem === 'white' ? '#2a2a33' : '#fff'}>
        {GEM_LABEL[gem as Gem] ?? gem}
      </text>
    </g>
  );
}

export function CardFace({ card }: { card: SplendorCard }) {
  const accent = TIER_COLOR[card.tier] ?? '#888';
  const cost = Object.entries(card.cost);
  return (
    <svg className="card-face" viewBox="0 0 96 132" role="img" aria-label={`${card.tier} 级卡`}>
      <rect x="2" y="2" width="92" height="128" rx="8" fill="#1b2030" stroke={accent} strokeWidth="2.5" />
      <text x="10" y="28" fontSize="18" fontWeight="700" fill="#f2e6c8">
        {card.points > 0 ? card.points : ''}
      </text>
      <GemDot gem={card.bonus} x={48} y={58} r={20} />
      {cost.map(([gem, amount], index) => (
        <g key={gem}>
          <GemDot gem={gem} x={14 + index * 22} y={108} r={9} />
          <text x={14 + index * 22} y={124} textAnchor="middle" fontSize={9} fill="#cfd6e4">
            {amount}
          </text>
        </g>
      ))}
    </svg>
  );
}

export function NobleTile({ noble }: { noble: SplendorNoble }) {
  const requirement = Object.entries(noble.requirement);
  return (
    <svg className="noble-tile" viewBox="0 0 76 76" role="img" aria-label={`贵族 ${noble.code}`}>
      <polygon
        points="38,3 72,21 72,55 38,73 4,55 4,21"
        fill={noble.qualified ? '#3a3226' : '#232734'}
        stroke={noble.qualified ? '#d8b23c' : '#4a5064'}
        strokeWidth="2"
      />
      <text x="38" y="26" textAnchor="middle" fontSize="12" fill="#e8dcc0">
        贵族
      </text>
      {requirement.map(([gem, amount], index) => (
        <g key={gem}>
          <GemDot gem={gem} x={18 + index * 19} y={48} r={8} />
          <text x={18 + index * 19} y={66} textAnchor="middle" fontSize={8} fill="#cfd6e4">
            {amount}
          </text>
        </g>
      ))}
    </svg>
  );
}

export function CardArt({ card }: { card: SplendorCard }) {
  return (
    <Art
      game="splendor"
      name={card.code}
      alt={`${card.tier} 级卡`}
      fallback={<CardFace card={card} />}
    />
  );
}

export function NobleArt({ noble }: { noble: SplendorNoble }) {
  return (
    <Art game="splendor" name={`noble-${noble.code}`} alt="贵族" fallback={<NobleTile noble={noble} />} />
  );
}
