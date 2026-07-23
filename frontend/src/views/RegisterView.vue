<template>
  <section class="auth-panel">
    <p class="eyebrow">Join</p>
    <h1>회원가입</h1>
    <p class="lead">loginId와 비밀번호로 통합 회원을 만듭니다.</p>

    <form class="form" @submit.prevent="onSubmit">
      <label>
        로그인 ID
        <input v-model.trim="form.loginId" autocomplete="username" required minlength="4" maxlength="30" />
      </label>
      <label>
        비밀번호
        <input v-model="form.password" type="password" autocomplete="new-password" required minlength="8" />
      </label>
      <label>
        채널
        <select v-model="form.channel">
          <option v-for="c in CHANNELS" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </label>
      <label>
        이름 <span class="optional">(선택)</span>
        <input v-model.trim="form.name" />
      </label>
      <label>
        이메일 <span class="optional">(선택)</span>
        <input v-model.trim="form.email" type="email" />
      </label>

      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '처리 중…' : '가입하고 시작' }}</button>
    </form>

    <p class="hint">
      이미 계정이 있나요?
      <RouterLink to="/login">로그인</RouterLink>
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
  name: '',
  email: '',
})

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    const payload = {
      loginId: form.loginId,
      password: form.password,
      channel: form.channel,
    }
    if (form.name) payload.name = form.name
    if (form.email) payload.email = form.email

    const tokens = await api.register(payload)
    saveAuth({ ...tokens, channel: form.channel })
    await router.push('/')
  } catch (e) {
    error.value = e.message || '회원가입에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>
