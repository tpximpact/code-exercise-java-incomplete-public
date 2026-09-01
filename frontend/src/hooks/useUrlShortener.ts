import { useState, useCallback, useEffect } from 'react';
import { api } from '../services/api';
import type { ShortenUrlResponse, UrlListItem } from '../types/api';

interface UseUrlShortenerReturn {
  urls: UrlListItem[];
  loading: boolean;
  error: string | null;
  lastCreated: ShortenUrlResponse | null;
  page: number;
  totalPages: number;
  totalItems: number;
  shorten: (fullUrl: string, customAlias?: string) => Promise<void>;
  deleteUrl: (alias: string) => Promise<void>;
  goToPage: (page: number) => Promise<void>;
  clearLastCreated: () => void;
}

const PAGE_SIZE = 5;

export function useUrlShortener(): UseUrlShortenerReturn {
  const [urls, setUrls] = useState<UrlListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastCreated, setLastCreated] = useState<ShortenUrlResponse | null>(null);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  const fetchAll = useCallback(async (pageNumber = 0) => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.listAll(pageNumber, PAGE_SIZE);

      setUrls(data.urls);
      setPage(data.page);
      setTotalPages(data.totalPages);
      setTotalItems(data.totalItems);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load URLs.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  const goToPage = useCallback(async (pageNumber: number) => {
    await fetchAll(pageNumber);
  }, [fetchAll]);

  const shorten = useCallback(async (fullUrl: string, customAlias?: string) => {
    setError(null);
    setLastCreated(null);

    try {
      const result = await api.shorten({
        fullUrl,
        customAlias: customAlias || undefined,
      });

      setLastCreated(result);

      // Refresh the current page
      await fetchAll(page);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to shorten URL.');
    }
  }, [fetchAll, page]);

  const deleteUrl = useCallback(async (alias: string) => {
    setError(null);

    try {
      await api.delete(alias);

      if (urls.length === 1 && page > 0) {
        await fetchAll(page - 1);
      } else {
        await fetchAll(page);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete URL.');
    }
  }, [fetchAll, page, urls]);

  const clearLastCreated = useCallback(() => setLastCreated(null), []);

  return {
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
  };
}