import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Lobby } from './pages/Lobby';
import { GamePage } from './pages/GamePage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Lobby />} />
        <Route path="/g/:gameId" element={<GamePage />} />
      </Routes>
    </BrowserRouter>
  );
}
