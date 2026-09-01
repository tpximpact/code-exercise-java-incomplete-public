import type { UrlListItem } from '../types/api';

interface UrlTableProps {
  urls: UrlListItem[];
  onDelete: (alias: string) => void;
}

export function UrlTable({ urls, onDelete }: UrlTableProps) {
  if (urls.length === 0) {
    return <p className="empty-state">No shortened URLs yet.</p>;
  }

  return (
    <div className="url-table-wrapper">
      <table className="url-table">
        <thead>
          <tr>
            <th>Alias</th>
            <th>Original URL</th>
            <th>Short URL</th>
            <th className="actions-cell">Actions</th>
          </tr>
        </thead>

        <tbody>
          {urls.map((url) => (
            <tr key={url.alias}>
              <td>
                <strong>{url.alias}</strong>
              </td>

              <td>
                <span className="full-url" title={url.fullUrl}>
                  {url.fullUrl}
                </span>
              </td>

              <td>
                <a
                  className="short-url-link"
                  href={url.shortUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  {url.shortUrl}
                </a>
              </td>

              <td className="actions-cell">
                <button
                  className="btn-danger"
                  onClick={() => onDelete(url.alias)}
                  aria-label={`Delete ${url.alias}`}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}