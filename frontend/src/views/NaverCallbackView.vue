<template>
  <section class="auth-panel">
    <p class="eyebrow">Naver</p>
    <h1>네이버 로그인</h1>
    <p class="lead">{{ status }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="error" class="hint">
      <RouterLink to="/login">로그인으로 돌아가기</RouterLink>
    </p>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { api } from '../api'
import { saveAuth } from '../auth'

const route = useRoute()
const router = useRouter()
const status = ref('네이버 로그인 처리 중…')
const error = ref('')

onMounted(async () => {
  const code = String(route.query.code || '')
  const state = String(route.query.state || '')
  const oauthError = route.query.error

  if (oauthError) {
    status.value = '로그인이 취소되었거나 실패했습니다.'
    error.value = String(oauthError)
    return
  }

  const savedState = sessionStorage.getItem('naverOAuthState')
  sessionStorage.removeItem('naverOAuthState')

  if (!code || !state) {
    status.value = '인가 정보가 없습니다.'
    error.value = 'code/state 가 비어 있습니다.'
    return
  }
  if (!savedState || savedState !== state) {
    status.value = '보안 검증에 실패했습니다.'
    error.value = 'OAuth state 가 일치하지 않습니다. 다시 시도해 주세요.'
    return
  }

  try {
    const tokens = await api.naverOAuthLogin({ code, state })
    saveAuth(tokens)
    status.value = '로그인 성공. 메인으로 이동합니다…'
    await router.replace('/')
  } catch (e) {
    status.value = '네이버 로그인에 실패했습니다.'
    error.value = e.message || '알 수 없는 오류'
  }
})
</script>
