import { reactive, computed } from 'vue'

const state = reactive({
  accessToken: localStorage.getItem('accessToken') || '',
  refreshToken: localStorage.getItem('refreshToken') || '',
  channel: localStorage.getItem('channel') || 'O4O_APP',
})

export function getAccessToken() {
  return state.accessToken
}

export function getRefreshToken() {
  return state.refreshToken
}

export function getChannel() {
  return state.channel
}

export const isLoggedIn = computed(() => Boolean(state.accessToken))

export function saveAuth({ accessToken, refreshToken, channel }) {
  state.accessToken = accessToken || ''
  state.refreshToken = refreshToken || ''
  if (channel) state.channel = channel
  localStorage.setItem('accessToken', state.accessToken)
  localStorage.setItem('refreshToken', state.refreshToken)
  localStorage.setItem('channel', state.channel)
}

export function clearAuth() {
  state.accessToken = ''
  state.refreshToken = ''
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}
