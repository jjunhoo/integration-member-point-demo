import { clearAuth, getAccessToken } from './auth'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

async function request(path, { method = 'GET', body, auth = false } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getAccessToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401 && auth) {
    clearAuth()
  }

  if (res.status === 204 || res.status === 202) {
    return null
  }

  const text = await res.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!res.ok) {
    const message =
      data?.message ||
      data?.error ||
      (typeof data === 'string' ? data : null) ||
      `요청 실패 (${res.status})`
    throw new Error(message)
  }

  return data
}

export const api = {
  register: (body) => request('/api/v1/auth/register', { method: 'POST', body }),
  login: (body) => request('/api/v1/auth/login', { method: 'POST', body }),
  logout: () => request('/api/v1/auth/logout', { method: 'POST', auth: true }),
  me: () => request('/api/v1/members/me', { auth: true }),
  accumulate: (userId, amount) =>
    request('/api/v1/membership/points/accumulate', {
      method: 'POST',
      body: { userId, amount },
    }),
  deduct: (userId, amount) =>
    request('/api/v1/membership/points/deduct', {
      method: 'POST',
      body: { userId, amount },
    }),
  view: async (userId) => {
    const res = await fetch(`${API_BASE}/api/v1/membership/${userId}/view`)
    if (res.status === 404) return null
    if (!res.ok) throw new Error(`뷰 조회 실패 (${res.status})`)
    return res.json()
  },
}

export const CHANNELS = [
  { value: 'CVS', label: '편의점' },
  { value: 'SUPERMARKET', label: '슈퍼' },
  { value: 'HOME_SHOPPING', label: '홈쇼핑' },
  { value: 'O4O_APP', label: 'O4O 앱' },
]
