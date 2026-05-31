import axios from 'axios'

/**
 * Factory — each app passes its own resolved baseURL.
 * No env sniffing here; environment resolution is the app's responsibility.
 */
export const createApiClient = (baseURL: string) =>
  axios.create({
    baseURL,
    headers: { 'Content-Type': 'application/json' },
  })
