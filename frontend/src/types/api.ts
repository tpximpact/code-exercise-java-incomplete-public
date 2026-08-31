export interface ShortenUrlRequest {
  fullUrl: string;
  customAlias?: string;
}

export interface ShortenUrlResponse {
  alias: string;
  fullUrl: string;
  shortUrl: string;
}

export interface UrlListItem {
  alias: string;
  fullUrl: string;
  shortUrl: string;
}

export interface AllUrlsResponse {
  urls: UrlListItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ApiError {
  error: string;
}
