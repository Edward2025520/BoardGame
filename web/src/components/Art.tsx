import { useEffect, useState, type ReactNode } from 'react';

interface ArtProps {
  game: string;
  name: string;
  alt: string;
  fallback: ReactNode;
}

/**
 * 优先用官方图（放在 web/public/art-official/<game>/<name>.png，不进仓库），
 * 加载失败就退回自绘 SVG。
 */
export function Art({ game, name, alt, fallback }: ArtProps) {
  const src = `/art-official/${game}/${name}.png`;
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setFailed(false);
  }, [src]);

  if (failed) {
    return <>{fallback}</>;
  }

  return <img className="art" src={src} alt={alt} onError={() => setFailed(true)} />;
}
