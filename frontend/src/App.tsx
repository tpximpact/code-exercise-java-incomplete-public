import { useUrlShortener } from './hooks/useUrlShortener';
import { ShortenForm } from './components/ShortenForm';
import { ResultBanner } from './components/ResultBanner';
import { UrlTable } from './components/UrlTable';

export default function App() {
  const {
    urls,
    loading,
    error,
    lastCreated,
    page,
    totalPages,
    totalItems,
    shorten,
    deleteUrl,
    goToPage,
    clearLastCreated,
  } = useUrlShortener();

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__inner">
          <div className="logo">
            <span className="logo__mark" aria-hidden="true">⌁</span>
            <span className="logo__name">Snip</span>
          </div>
          <p className="tagline">Long URLs, made short.</p>
        </div>
      </header>

      <main className="app-main">
        <section className="card shorten-card" aria-labelledby="shorten-heading">
          <h2 id="shorten-heading" className="card__title">
            Shorten a URL
          </h2>
          <ShortenForm onSubmit={shorten} />
        </section>

        {lastCreated && (
          <ResultBanner result={lastCreated} onDismiss={clearLastCreated} />
        )}

        {error && (
          <div className="banner banner--error" role="alert">
            <strong>Error:</strong> {error}
          </div>
        )}

        <section className="card" aria-labelledby="list-heading">
          <h2 id="list-heading" className="card__title">
            All shortened URLs
            {urls.length > 0 && (
              <span className="badge">{totalItems}</span>
            )}
          </h2>

          {loading ? (
            <div className="loading" aria-live="polite" aria-busy="true">
              Loading…
            </div>
          ) : (
            <>
              <UrlTable urls={urls} onDelete={deleteUrl} />

                {totalPages > 1 && (
                  <div className="pagination" aria-label="Pagination">
                    <button
                      className="btn-ghost pagination__button"
                      disabled={page === 0}
                      onClick={() => goToPage(page - 1)}
                    >
                      ← Previous
                    </button>

                    <span className="pagination__status">
                      Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
                    </span>

                    <button
                      className="btn-ghost pagination__button"
                      disabled={page >= totalPages - 1}
                      onClick={() => goToPage(page + 1)}
                    >
                      Next →
                    </button>
                  </div>
                )}
            </>
          )}
        </section>
      </main>

      <footer className="app-footer">
        <p>Snip URL Shortener</p>
      </footer>
    </div>
  );
}
