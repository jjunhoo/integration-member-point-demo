import { reactive, computed } from 'vue'

const state = reactive({
  accessToken: localStorage.getItem('accessToken') || '',
  refreshToken: localStorage.getItem('refreshToken') || '',
})

export function getAccessToken() {
  return state.accessToken
}

export function getRefreshToken() {
  return state.refreshToken
}

export const isLoggedIn = computed(() => Boolean(state.accessToken))

export function saveAuth({ accessToken, refreshToken }) {
  state.accessToken = accessToken || ''
  state.refreshToken = refreshToken || ''
  localStorage.setItem('accessToken', state.accessToken)
  localStorage.setItem('refreshToken', state.refreshToken)
}

export function clearAuth() {
  state.accessToken = ''
  state.refreshToken = ''
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('channel')
}
