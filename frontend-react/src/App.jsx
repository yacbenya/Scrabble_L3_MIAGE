import React, { useEffect, useMemo, useRef, useState } from 'react';
import { exchangeTiles, fetchState, loadGame, passTurn, playMove, resetGame, saveGame, startGame } from './api';

const PRIME_LABELS = {
  MOT_TRIPLE: 'MT',
  MOT_DOUBLE: 'MD',
  LETTRE_TRIPLE: 'LT',
  LETTRE_DOUBLE: 'LD',
  DEPART: '★',
  AUCUNE: ''
};

const EMPTY_SETUP = ['Joueur 1', 'Joueur 2'];

function App() {
  const [gameState, setGameState] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedTileId, setSelectedTileId] = useState(null);
  const [direction, setDirection] = useState('HORIZONTALE');
  const [placements, setPlacements] = useState([]);
  const [setupNames, setSetupNames] = useState(EMPTY_SETUP);
  const [exchangeMode, setExchangeMode] = useState(false);
  const [selectedExchangeIds, setSelectedExchangeIds] = useState([]);
  const [turnTransition, setTurnTransition] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    void loadState();
  }, []);

  async function loadState() {
    try {
      setLoading(true);
      setError('');
      const state = await fetchState();
      setGameState(state);
      syncSetupNamesFromState(state);
    } catch (err) {
      setError(err?.message || 'Erreur lors du chargement de la partie');
    } finally {
      setLoading(false);
    }
  }

  function syncSetupNamesFromState(state) {
    const names = state?.players?.map((player) => player.name).filter(Boolean) || [];
    if (names.length >= 2) {
      setSetupNames(names);
    }
  }

  async function handleStartGame(event) {
    event.preventDefault();

    try {
      setError('');
      const names = setupNames.map((name) => name.trim()).filter(Boolean);
      const state = await startGame(names);
      resetLocalTurnState();
      setGameState(state);
      syncSetupNamesFromState(state);
    } catch (err) {
      setError(err?.message || 'Erreur au démarrage de la partie');
    }
  }

  async function handleResetGame() {
    try {
      setError('');
      const fallbackNames = gameState?.players?.map((player) => player.name).filter(Boolean) || [];
      const state = await resetGame();
      resetLocalTurnState();
      setGameState(state);
      setSetupNames(fallbackNames.length >= 2 ? fallbackNames : EMPTY_SETUP);
    } catch (err) {
      setError(err?.message || 'Erreur lors de la réinitialisation de la partie');
    }
  }

  async function handleSauvegarder() {
    try {
      setError('');
      const data = await saveGame();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'partie.json';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err?.message || 'Erreur lors de la sauvegarde');
    }
  }

  function handleChargerClick() {
    fileInputRef.current?.click();
  }

  async function handleFichierChoisi(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    try {
      setError('');
      const texte = await file.text();
      const state = await loadGame(JSON.parse(texte));
      resetLocalTurnState();
      setGameState(state);
      syncSetupNamesFromState(state);
    } catch (err) {
      setError(err?.message || 'Erreur lors du chargement');
    }
  }

  function resetLocalTurnState() {
    setPlacements([]);
    setSelectedTileId(null);
    setDirection('HORIZONTALE');
    setExchangeMode(false);
    setSelectedExchangeIds([]);
  }

  function applyTurnTransition(newState) {
    if (newState?.gameStarted && !newState?.finished) {
      setTurnTransition(true);
    }
  }

  const rack = gameState?.rack || [];

  const availableRack = useMemo(() => {
    return rack.filter(
      (tile) => !placements.some((placement) => String(placement.tileId) === String(tile.id))
    );
  }, [rack, placements]);

  const detectedDirection = useMemo(() => {
    if (placements.length < 2) return null;
    const allSameRow = placements.every((p) => p.row === placements[0].row);
    if (allSameRow) return 'HORIZONTALE';
    const allSameCol = placements.every((p) => p.col === placements[0].col);
    if (allSameCol) return 'VERTICALE';
    return null;
  }, [placements]);

  const effectiveDirection = detectedDirection || direction;

  const displayedBoard = useMemo(() => {
    if (!gameState?.board) return [];

    return gameState.board.map((row) =>
      row.map((cell) => {
        const pending = placements.find(
          (placement) => placement.row === cell.row && placement.col === cell.col
        );

        if (!pending) return cell;

        return {
          ...cell,
          pending: true,
          tile: {
            letter: pending.letter,
            points: pending.points,
            joker: pending.joker,
            jokerFace: pending.jokerFace || ''
          }
        };
      })
    );
  }, [gameState, placements]);

  function getTileById(tileId) {
    return rack.find((tile) => String(tile.id) === String(tileId));
  }

  function handleBoardCellClick(cell) {
    if (cell.tile && !cell.pending) return;

    const existingPending = placements.find(
      (placement) => placement.row === cell.row && placement.col === cell.col
    );

    if (existingPending) {
      setPlacements((current) =>
        current.filter(
          (placement) => !(placement.row === cell.row && placement.col === cell.col)
        )
      );
      return;
    }

    if (!selectedTileId) return;

    placeTileOnBoard(selectedTileId, cell.row, cell.col);
  }

  function placeTileOnBoard(tileId, row, col) {
    if (!gameState?.board) return;

    const tile = getTileById(tileId);
    if (!tile) return;

    if (placements.some((placement) => placement.row === row && placement.col === col)) return;

    const backendCell = gameState.board[row]?.[col];
    if (!backendCell || backendCell.tile) return;

    let jokerFace = '';

    if (tile.joker) {
      const answer = window.prompt('Choisis la lettre du joker', 'A') || '';
      jokerFace = answer.trim().slice(0, 1).toUpperCase();
      if (!/^[A-Z]$/.test(jokerFace)) return;
    }

    setPlacements((current) => [
      ...current,
      {
        tileId: tile.id,
        row,
        col,
        jokerFace,
        letter: tile.joker ? jokerFace : tile.letter,
        points: tile.points,
        joker: tile.joker
      }
    ]);
    setSelectedTileId(null);
  }

  async function handleValidateMove() {
    if (!placements.length) return;

    try {
      setError('');
      const state = await playMove({
        direction: effectiveDirection,
        placements: placements.map((placement) => ({
          tileId: placement.tileId,
          row: placement.row,
          col: placement.col,
          jokerFace: placement.jokerFace || null
        }))
      });
      resetLocalTurnState();
      setGameState(state);
      syncSetupNamesFromState(state);
      applyTurnTransition(state);
    } catch (err) {
      setError(err?.message || 'Erreur lors de la validation du coup');
    }
  }

  async function handlePass() {
    try {
      setError('');
      const state = await passTurn();
      resetLocalTurnState();
      setGameState(state);
      syncSetupNamesFromState(state);
      applyTurnTransition(state);
    } catch (err) {
      setError(err?.message || 'Erreur lors du passage du tour');
    }
  }

  async function handleExchangeConfirm() {
    try {
      setError('');
      const state = await exchangeTiles(selectedExchangeIds);
      resetLocalTurnState();
      setGameState(state);
      syncSetupNamesFromState(state);
      applyTurnTransition(state);
    } catch (err) {
      setError(err?.message || "Erreur lors de l'échange des tuiles");
    }
  }

  function toggleExchangeTile(tileId) {
    setSelectedExchangeIds((current) =>
      current.includes(tileId)
        ? current.filter((id) => id !== tileId)
        : [...current, tileId]
    );
  }

  function handleTileDragStart(event, tileId) {
    event.dataTransfer.setData('text/plain', String(tileId));
  }

  function handleCellDrop(event, cell) {
    event.preventDefault();
    const tileId = event.dataTransfer.getData('text/plain');
    if (tileId) {
      placeTileOnBoard(tileId, cell.row, cell.col);
    }
  }

  function addPlayerField() {
    if (setupNames.length >= 4) return;
    setSetupNames((current) => [...current, `Joueur ${current.length + 1}`]);
  }

  function removePlayerField(index) {
    if (setupNames.length <= 2) return;
    setSetupNames((current) => current.filter((_, i) => i !== index));
  }

  if (loading) {
    return (
      <div className="page-shell">
        <div className="status-card">Chargement…</div>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <header className="top-banner">
        <div>
          <p className="eyebrow">Scrabble</p>
          <h1>Partie multijoueur</h1>
        </div>
        <div className="top-badges">
          <span className="badge">Sac : {gameState?.bagCount ?? 0}</span>
          <span className="badge">Passes : {gameState?.consecutivePasses ?? 0}</span>
          <span className="badge">{gameState?.finished ? 'Partie terminée' : 'Partie en cours'}</span>
          <button type="button" className="badge-button" onClick={handleSauvegarder} disabled={!gameState?.gameStarted}>Sauvegarder</button>
          <button type="button" className="badge-button" onClick={handleChargerClick}>Charger</button>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/json,.json"
            onChange={handleFichierChoisi}
            style={{ display: 'none' }}
          />
        </div>
      </header>

      {error ? <div className="alert error">{error}</div> : null}

      {turnTransition && gameState?.gameStarted && !gameState?.finished ? (
        <TurnTransitionScreen
          playerName={gameState.currentPlayerName}
          onReveal={() => setTurnTransition(false)}
        />
      ) : null}

      {gameState?.finished ? (
        <GameOverScreen
          players={gameState.players || []}
          winner={gameState.winner}
          history={gameState.history || []}
          onNewGame={handleResetGame}
        />
      ) : null}

      {!gameState?.gameStarted ? (
        <SetupScreen
          setupNames={setupNames}
          setSetupNames={setSetupNames}
          onSubmit={handleStartGame}
          onAddPlayer={addPlayerField}
          onRemovePlayer={removePlayerField}
        />
      ) : !turnTransition && !gameState?.finished ? (
        <main className="game-layout">
          <aside className="sidebar brutal-card">
            <section>
              <p className="section-label">Tour en cours</p>
              <h2>{gameState.currentPlayerName}</h2>
              <p className="muted">
                Direction : {effectiveDirection === 'HORIZONTALE' ? 'Horizontale' : 'Verticale'}
                {detectedDirection ? ' (auto)' : ''}
              </p>
            </section>

            <section>
              <p className="section-label">Scores</p>
              <div className="score-list">
                {gameState.players.map((player) => (
                  <div key={player.name} className={`score-item ${player.current ? 'current' : ''}`}>
                    <div>
                      <strong>{player.name}</strong>
                      <span>{player.rackCount} tuiles</span>
                    </div>
                    <div className="score-value">{player.score}</div>
                  </div>
                ))}
              </div>
            </section>

            <section>
              <p className="section-label">Dernière action</p>
              <div className="history-box">
                <strong>{gameState.lastPoints} pts</strong>
                <p>{gameState.lastMessage}</p>
                {gameState.lastWords?.length ? (
                  <div className="word-pills">
                    {gameState.lastWords.map((word) => (
                      <span key={word} className="word-pill">{word}</span>
                    ))}
                  </div>
                ) : null}
              </div>
            </section>

            {gameState.history?.length ? (
              <section>
                <p className="section-label">Historique</p>
                <div className="history-list">
                  {gameState.history.slice().reverse().map((entry, i) => (
                    <div key={i} className={`history-entry history-type-${entry.type.toLowerCase()}`}>
                      <span className="history-entry-message">{entry.message}</span>
                      {entry.points > 0 ? <span className="history-entry-points">+{entry.points}</span> : null}
                    </div>
                  ))}
                </div>
              </section>
            ) : null}

            <section className="action-stack">
              {detectedDirection === null ? (
                <div className="direction-switch">
                  <button
                    type="button"
                    className={direction === 'HORIZONTALE' ? 'active' : ''}
                    onClick={() => setDirection('HORIZONTALE')}
                  >
                    Horizontal
                  </button>
                  <button
                    type="button"
                    className={direction === 'VERTICALE' ? 'active' : ''}
                    onClick={() => setDirection('VERTICALE')}
                  >
                    Vertical
                  </button>
                </div>
              ) : null}

              <button
                className="primary-button"
                type="button"
                onClick={handleValidateMove}
                disabled={!placements.length || gameState.finished}
              >
                Valider le coup
              </button>

              <button
                className="secondary-button"
                type="button"
                onClick={resetLocalTurnState}
                disabled={!placements.length && !selectedTileId && !exchangeMode}
              >
                Annuler les placements
              </button>

              <button
                className="secondary-button"
                type="button"
                onClick={handlePass}
                disabled={gameState.finished}
              >
                Passer
              </button>

              <button
                className="secondary-button"
                type="button"
                onClick={() => {
                  setExchangeMode((value) => !value);
                  setSelectedExchangeIds([]);
                  setSelectedTileId(null);
                }}
                disabled={!gameState.canExchange || placements.length > 0 || gameState.finished}
              >
                {exchangeMode ? 'Fermer échange' : 'Échanger des tuiles'}
              </button>

              <button
                className="secondary-button"
                type="button"
                onClick={handleResetGame}
              >
                Recommencer de zéro
              </button>
            </section>
          </aside>

          <section className="board-panel brutal-card">
            <div className="board-grid">
              {displayedBoard.map((row, rowIndex) =>
                row.map((cell, colIndex) => (
                  <button
                    key={`${rowIndex}-${colIndex}`}
                    type="button"
                    className={`board-cell prime-${String(cell.prime).toLowerCase()} ${cell.pending ? 'pending' : ''} ${selectedTileId ? 'armed' : ''}`}
                    onClick={() => handleBoardCellClick(cell)}
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={(event) => handleCellDrop(event, cell)}
                  >
                    {cell.tile ? (
                      <>
                        <span className="tile-letter">{cell.tile.letter}</span>
                        <span className="tile-points">{cell.tile.points}</span>
                      </>
                    ) : (
                      <span className="prime-label">{PRIME_LABELS[cell.prime] ?? ''}</span>
                    )}
                  </button>
                ))
              )}
            </div>
          </section>
        </main>
      ) : null}

      {gameState?.gameStarted && !turnTransition && !gameState?.finished ? (
        <section className="rack-panel brutal-card" onDragOver={(event) => event.preventDefault()}>
          <div className="rack-header">
            <div>
              <p className="section-label">Chevalet</p>
              <h3>Clic ou glisser-déposer</h3>
            </div>
            {placements.length ? <span className="badge">{placements.length} placement(s)</span> : null}
          </div>

          <div className="rack-list">
            {availableRack.map((tile) => (
              <button
                key={tile.id}
                type="button"
                draggable={!exchangeMode}
                onDragStart={(event) => handleTileDragStart(event, tile.id)}
                onClick={() => {
                  if (exchangeMode) {
                    toggleExchangeTile(tile.id);
                  } else {
                    setSelectedTileId((current) => String(current) === String(tile.id) ? null : tile.id);
                  }
                }}
                className={`rack-tile ${String(selectedTileId) === String(tile.id) ? 'selected' : ''} ${selectedExchangeIds.includes(tile.id) ? 'exchange-selected' : ''}`}
              >
                <span className="tile-letter">{tile.letter}</span>
                <span className="tile-points">{tile.points}</span>
              </button>
            ))}
          </div>

          {exchangeMode ? (
            <div className="exchange-bar">
              <p>{selectedExchangeIds.length} tuile(s) sélectionnée(s) pour l’échange.</p>
              <button
                type="button"
                className="primary-button"
                onClick={handleExchangeConfirm}
                disabled={!selectedExchangeIds.length}
              >
                Confirmer l’échange
              </button>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}

function SetupScreen({ setupNames, setSetupNames, onSubmit, onAddPlayer, onRemovePlayer }) {
  return (
    <form className="setup-card brutal-card" onSubmit={onSubmit}>
      <div className="setup-copy">
        <p className="section-label">Nouvelle partie</p>
        <h2>Choisis les joueurs et démarre la partie</h2>
        <p className="muted">Entre 2 et 4 joueurs. Les règles du Scrabble s'appliquent et la validité des mots est vérifiée par le dictionnaire.</p>
      </div>

      <div className="setup-grid">
        {setupNames.map((name, index) => (
          <div key={index} className="setup-field">
            <label htmlFor={`player-${index}`}>Joueur {index + 1}</label>
            <div className="setup-input-row">
              <input
                id={`player-${index}`}
                value={name}
                onChange={(event) => {
                  const next = [...setupNames];
                  next[index] = event.target.value;
                  setSetupNames(next);
                }}
              />
              <button type="button" className="ghost-button" onClick={() => onRemovePlayer(index)}>
                –
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="setup-actions">
        <button type="button" className="secondary-button" onClick={onAddPlayer} disabled={setupNames.length >= 4}>
          Ajouter un joueur
        </button>
        <button type="submit" className="primary-button">
          Commencer
        </button>
      </div>
    </form>
  );
}

function TurnTransitionScreen({ playerName, onReveal }) {
  return (
    <div className="turn-overlay">
      <div className="turn-card brutal-card">
        <p className="section-label">Changement de joueur</p>
        <h2>C'est au tour de {playerName}</h2>
        <p className="muted">Passe l'appareil au joueur concerné, puis clique pour voir ton chevalet.</p>
        <button type="button" className="primary-button" onClick={onReveal}>
          Voir mon chevalet
        </button>
      </div>
    </div>
  );
}

function GameOverScreen({ players, winner, history, onNewGame }) {
  const sorted = [...players].sort((a, b) => b.score - a.score);
  return (
    <div className="gameover-container">
      <div className="gameover-card brutal-card">
        <p className="section-label">Partie terminée</p>
        <h2>{winner ? `${winner} remporte la partie !` : 'Fin de la partie'}</h2>

        <div className="gameover-rankings">
          {sorted.map((player, index) => (
            <div key={player.name} className={`gameover-rank ${index === 0 ? 'first' : ''}`}>
              <span className="gameover-position">{index + 1}</span>
              <span className="gameover-name">{player.name}</span>
              <span className="gameover-score">{player.score} pts</span>
            </div>
          ))}
        </div>

        {history.length ? (
          <div className="gameover-history">
            <p className="section-label">Résumé de la partie</p>
            <div className="history-list">
              {history.map((entry, i) => (
                <div key={i} className={`history-entry history-type-${entry.type.toLowerCase()}`}>
                  <span className="history-entry-message">{entry.message}</span>
                  {entry.points > 0 ? <span className="history-entry-points">+{entry.points}</span> : null}
                </div>
              ))}
            </div>
          </div>
        ) : null}

        <button type="button" className="primary-button" onClick={onNewGame}>
          Nouvelle partie
        </button>
      </div>
    </div>
  );
}

export default App;
