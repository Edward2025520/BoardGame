import { useId } from 'react';
import { Art } from '../../components/Art';
import { GEMS, GEM_COLOR, GEM_LABEL, type Gem, type SplendorCard, type SplendorNoble } from '../../api/types';

const TIER_COLOR: Record<number, string> = {
  1: '#4f9d69',
  2: '#c9a227',
  3: '#5a7fc0',
};

const GEM_LIGHT: Record<string, string> = {
  white: '#fffdf6',
  blue: '#8fc0f0',
  green: '#8fd6a8',
  red: '#f0968c',
  black: '#6d6d7d',
  gold: '#f3dd9a',
};

function safeId(raw: string): string {
  return raw.replace(/[^a-zA-Z0-9_-]/g, '');
}

/** 一颗宝石：径向渐变 + 高光 + 文字，用来替代官方图标。 */
export function GemCircle({
  gem,
  x,
  y,
  r,
  uid,
}: {
  gem: string;
  x: number;
  y: number;
  r: number;
  uid: string;
}) {
  const base = GEM_COLOR[gem as Gem] ?? '#888';
  const light = GEM_LIGHT[gem] ?? '#ddd';
  const gradientId = `gem-${uid}`;
  const label = GEM_LABEL[gem as Gem] ?? gem;

  return (
    <g>
      <defs>
        <radialGradient id={gradientId} cx="35%" cy="28%" r="80%">
          <stop offset="0%" stopColor={light} />
          <stop offset="65%" stopColor={base} />
          <stop offset="100%" stopColor={base} />
        </radialGradient>
      </defs>
      <circle cx={x} cy={y} r={r} fill={`url(#${gradientId})`} stroke="rgba(0,0,0,0.45)" strokeWidth={1.2} />
      <ellipse
        cx={x - r * 0.28}
        cy={y - r * 0.34}
        rx={r * 0.34}
        ry={r * 0.2}
        fill="rgba(255,255,255,0.4)"
      />
      <text
        x={x}
        y={y + r * 0.33}
        textAnchor="middle"
        fontSize={r * 0.95}
        fontWeight={600}
        fill={gem === 'white' || gem === 'gold' ? '#2b2415' : '#fff'}
      >
        {label}
      </text>
    </g>
  );
}

/** 切面宝石图标：用于信息卡、成本行、支付面板与银行。 */
export function GemIcon({ gem, size = 22, dimmed = false }: { gem: string; size?: number; dimmed?: boolean }) {
  const base = GEM_COLOR[gem as Gem] ?? '#888';
  const light = GEM_LIGHT[gem] ?? '#ddd';
  const edge = gem === 'white' || gem === 'gold' ? 'rgba(64,50,20,0.6)' : 'rgba(0,0,0,0.45)';

  return (
    <svg
      className={dimmed ? 'gem-icon dimmed' : 'gem-icon'}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <polygon points="7,3.5 17,3.5 21,9.5 12,21.5 3,9.5" fill={base} stroke={edge} strokeWidth="1" />
      <polygon points="7.5,4.5 16.5,4.5 19.5,9 4.5,9" fill={light} opacity="0.55" />
      <polygon points="4.5,9 9,9 12,19" fill="#fff" opacity="0.16" />
      <polygon points="15,9 19.5,9 12,19" fill="#000" opacity="0.22" />
      <polygon points="9,9 15,9 12,19" fill="#fff" opacity="0.08" />
    </svg>
  );
}

/** 折扣图标：一张小卡牌，颜色代表它提供的折扣。 */
export function DiscountIcon({ gem, size = 22 }: { gem: string; size?: number }) {
  const base = GEM_COLOR[gem as Gem] ?? '#888';
  const light = GEM_LIGHT[gem] ?? '#ddd';
  const edge = gem === 'white' || gem === 'gold' ? 'rgba(64,50,20,0.6)' : 'rgba(0,0,0,0.45)';

  return (
    <svg className="discount-icon" width={size * 0.75} height={size} viewBox="0 0 18 24" aria-hidden="true">
      <rect x="1" y="1" width="16" height="22" rx="2.5" fill={base} stroke={edge} strokeWidth="1" />
      <rect x="2.5" y="2.5" width="13" height="19" rx="1.5" fill="none" stroke="rgba(255,255,255,0.28)" />
      <polygon points="9,7 12,9.5 9,14 6,9.5" fill={light} opacity="0.85" />
      <rect x="2.5" y="16" width="13" height="5.5" rx="1.5" fill="#000" opacity="0.18" />
    </svg>
  );
}

/** 自绘卡面：等级色边框、分数角标、宝石与费用。 */
export function CardFace({ card }: { card: SplendorCard }) {
  const uid = safeId(useId());
  const accent = TIER_COLOR[card.tier] ?? '#888';
  const cost = Object.entries(card.cost);

  return (
    <svg className="card-face" viewBox="0 0 96 140" role="img" aria-label={`${card.tier} 级卡`}>
      <defs>
        <linearGradient id={`bg-${uid}`} x1="0" y1="0" x2="0.6" y2="1">
          <stop offset="0%" stopColor="#242b3c" />
          <stop offset="100%" stopColor="#12151f" />
        </linearGradient>
      </defs>
      <rect x="2" y="2" width="92" height="136" rx="9" fill={`url(#bg-${uid})`} stroke={accent} strokeWidth="2.5" />
      <rect x="6" y="6" width="84" height="128" rx="6" fill="none" stroke="rgba(255,255,255,0.06)" />

      {card.points > 0 && (
        <g>
          <circle cx="17" cy="19" r="11" fill="#f0e2b8" stroke="#8a7745" strokeWidth="1" />
          <text x="17" y="24" textAnchor="middle" fontSize="13" fontWeight={700} fill="#3a2f14">
            {card.points}
          </text>
        </g>
      )}

      <GemCircle gem={card.bonus} x={48} y={62} r={22} uid={`${uid}-bonus`} />

      <text x="48" y="96" textAnchor="middle" fontSize="8" letterSpacing="1.5" fill="#8e97ab">
        TIER {card.tier}
      </text>

      {cost.map(([gem, amount], index) => (
        <g key={gem}>
          <GemCircle gem={gem} x={16 + index * 21} y={116} r={8.5} uid={`${uid}-c${index}`} />
          <text x={16 + index * 21} y={134} textAnchor="middle" fontSize="9" fill="#cfd6e4">
            {amount}
          </text>
        </g>
      ))}
    </svg>
  );
}

/** 自绘贵族：金色六边形 + 需求宝石。 */
export function NobleTile({ noble }: { noble: SplendorNoble }) {
  const uid = safeId(useId());
  const requirement = Object.entries(noble.requirement);

  return (
    <svg className="noble-tile" viewBox="0 0 76 78" role="img" aria-label={`贵族 ${noble.code}`}>
      <defs>
        <linearGradient id={`noble-${uid}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={noble.qualified ? '#463a22' : '#242938'} />
          <stop offset="100%" stopColor={noble.qualified ? '#241d10' : '#151922'} />
        </linearGradient>
      </defs>
      <polygon
        points="38,3 72,21 72,57 38,75 4,57 4,21"
        fill={`url(#noble-${uid})`}
        stroke={noble.qualified ? '#e0bb55' : '#4a5064'}
        strokeWidth="2"
      />
      <text x="38" y="24" textAnchor="middle" fontSize="10" letterSpacing="1" fill="#e8dcc0">
        贵族
      </text>
      <text x="38" y="36" textAnchor="middle" fontSize="8" fill="#8e97ab">
        3 分
      </text>
      {requirement.map(([gem, amount], index) => (
        <g key={gem}>
          <GemCircle gem={gem} x={16 + index * 22} y={52} r={7.5} uid={`${uid}-r${index}`} />
          <text x={16 + index * 22} y={70} textAnchor="middle" fontSize="8" fill="#cfd6e4">
            {amount}
          </text>
        </g>
      ))}
    </svg>
  );
}

/** 卡面文件名：用「等级-颜色-分数-成本」而不是卡号，避开 3b/3B 在 Windows 上撞名。 */
export function cardArtName(card: SplendorCard): string {
  const cost = GEMS.slice(0, 5).map((gem) => card.cost[gem] ?? 0).join('-');
  return `card-${card.tier}-${card.bonus}-${card.points}-${cost}`;
}

export function nobleArtName(noble: SplendorNoble): string {
  const requirement = GEMS.slice(0, 5).map((gem) => noble.requirement[gem] ?? 0).join('-');
  return `noble-${requirement}`;
}

export function CardArt({ card }: { card: SplendorCard }) {
  return (
    <Art
      game="splendor"
      name={cardArtName(card)}
      alt={`${card.tier} 级卡`}
      fallback={<CardFace card={card} />}
    />
  );
}

export function NobleArt({ noble }: { noble: SplendorNoble }) {
  return <Art game="splendor" name={nobleArtName(noble)} alt="贵族" fallback={<NobleTile noble={noble} />} />;
}
