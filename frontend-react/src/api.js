const API_BASE = 'http://localhost:8080/api';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || 'Erreur serveur');
  }
  return data;
}

export function fetchState() {
  return request('/game/state');
}

export function startGame(playerNames) {
  return request('/game/start', {
    method: 'POST',
    body: JSON.stringify({ playerNames })
  });
}

export function resetGame() {
  return request('/game/reset', {
    method: 'POST',
    body: JSON.stringify({})
  });
}

export function playMove(payload) {
  return request('/game/play', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function passTurn() {
  return request('/game/pass', {
    method: 'POST',
    body: JSON.stringify({})
  });
}

export function exchangeTiles(tileIds) {
  return request('/game/exchange', {
    method: 'POST',
    body: JSON.stringify({ tileIds })
  });
}
