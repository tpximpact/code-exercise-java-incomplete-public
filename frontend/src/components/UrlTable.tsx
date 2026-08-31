import type { UrlListItem } from '../types/api';

interface UrlTableProps {
  urls: UrlListItem[];
  onDelete: (alias: string) => void;
}

export function UrlTable({ urls, onDelete }: UrlTableProps) {
  if (urls.length === 0) {
    return <p>No shortened URLs yet.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Alias</th>
          <th>Original URL</th>
          <th>Short URL</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody>
        {urls.map((url) => (
          <tr key={url.alias}>
            <td>{url.alias}</td>
            <td>{url.fullUrl}</td>
            <td>
              <a href={url.shortUrl}>
                {url.shortUrl}
              </a>
            </td>
            <td>
              <button onClick={() => onDelete(url.alias)}>
                Delete
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}