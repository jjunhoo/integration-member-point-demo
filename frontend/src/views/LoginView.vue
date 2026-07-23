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
      <label>
        채널
        <select v-model="form.channel">
          <option v-for="c in CHANNELS" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </label>

      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '처리 중…' : '로그인' }}</button>
    </form>

    <p class="hint">
      계정이 없나요?
      <RouterLink to="/register">회원가입</RouterLink>
    </p>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { api, CHANNELS } from '../api'
import { getChannel, saveAuth } from '../auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  loginId: '',
  password: '',
  channel: getChannel(),
})

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    const tokens = await api.login({
      loginId: form.loginId,
      password: form.password,
      channel: form.channel,
    })
    saveAuth({ ...tokens, channel: form.channel })
    await router.push('/')
  } catch (e) {
    error.value = e.message || '로그인에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>
