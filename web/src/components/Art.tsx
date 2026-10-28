import { useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react';

const EXTENSIONS = ['png', 'jpg', 'webp'];

let fallbackCount = 0;
const listeners = new Set<() => void>();

function reportFallback() {
  fallbackCount += 1;
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** 当前有多少张图退回了自绘，用来在界面上提示「没找到官方图」。 */
export function useArtFallbackCount(): number {
  return useSyncExternalStore(subscribe, () => fallbackCount, () => 0);
}

interface ArtProps {
  game: string;
  name: string;
  alt: string;
  fallback: ReactNode;
}

/**
 * 优先用本地官方图（web/public/art-official/<game>/<name>.png，不进仓库），
 * 依次尝试 png/jpg/webp，全部失败才退回自绘 SVG。
 */
export function Art({ game, name, alt, fallback }: ArtProps) {
  const sources = useMemo(
    () => EXTENSIONS.map((ext) => `/art-official/${game}/${name}.${ext}`),
    [game, name],
  );
  const [index, setIndex] = useState(0);
  const [exhausted, setExhausted] = useState(false);

  useEffect(() => {
    setIndex(0);
    setExhausted(false);
  }, [sources]);

  if (exhausted) {
    return <>{fallback}</>;
  }

  const handleError = () => {
    if (index + 1 < sources.length) {
      setIndex(index + 1);
      return;
    }
    setExhausted(true);
    reportFallback();
  };

  return <img className="art" src={sources[index]} alt={alt} onError={handleError} />;
}
