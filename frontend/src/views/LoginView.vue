<template>
  <section class="auth-panel">
    <p class="eyebrow">Sign in</p>
    <h1>로그인</h1>
    <p class="lead">통합 멤버십 데모에 로그인합니다.</p>

    <form class="form" @submit.prevent="onSubmit">
      <label>
        로그인 ID
        <input v-model.trim="form.loginId" autocomplete="username" required />
      </label>
      <label>
        비밀번호
        <input v-model="form.password" type="password" autocomplete="current-password" required />
      </label>

      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '처리 중…' : '로그인' }}</button>
    </form>

    <div class="divider"><span>또는</span></div>
    <button type="button" class="naver" :disabled="loading" @click="onNaverLogin">
      네이버로 로그인
    </button>

    <p class="hint">
      계정이 없나요?
      <RouterLink to="/register">회원가입</RouterLink>
    </p>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { api } from '../api'
import { saveAuth } from '../auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  loginId: '',
  password: '',
})

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    const tokens = await api.login({
      loginId: form.loginId,
      password: form.password,
    })
    saveAuth(tokens)
    await router.push('/')
  } catch (e) {
    error.value = e.message || '로그인에 실패했습니다'
  } finally {
    loading.value = false
  }
}

async function onNaverLogin() {
  loading.value = true
  error.value = ''
  try {
    const { authorizeUrl, state } = await api.naverAuthorizeUrl()
    sessionStorage.setItem('naverOAuthState', state)
    window.location.href = authorizeUrl
  } catch (e) {
    error.value = e.message || '네이버 로그인을 시작할 수 없습니다'
    loading.value = false
  }
}
</script>
