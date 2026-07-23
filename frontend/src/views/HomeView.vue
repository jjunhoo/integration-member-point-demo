<template>
  <section class="home">
    <div class="home-head">
      <div>
        <p class="eyebrow">Dashboard</p>
        <h1>메인</h1>
        <p class="lead">회원 정보와 포인트를 한곳에서 확인합니다.</p>
      </div>
      <button class="ghost" type="button" :disabled="busy" @click="onLogout">로그아웃</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="notice" class="notice">{{ notice }}</p>

    <section v-if="member" class="block">
      <h2>내 정보</h2>
      <dl class="meta">
        <div><dt>회원 ID</dt><dd>{{ member.memberId }}</dd></div>
        <div><dt>이름</dt><dd>{{ member.name || '-' }}</dd></div>
        <div><dt>이메일</dt><dd>{{ member.email || '-' }}</dd></div>
        <div><dt>상태</dt><dd>{{ member.status }}</dd></div>
      </dl>
      <ul v-if="member.channels?.length" class="channels">
        <li v-for="c in member.channels" :key="c.channel + c.channelMemberNo">
          {{ c.brand }} · {{ c.channelMemberNo }}
        </li>
      </ul>
    </section>

    <section class="block">
      <h2>포인트</h2>
      <p class="balance">
        잔액
        <strong>{{ view ? view.pointBalance.toLocaleString() : '—' }}</strong>
        <span v-if="view"> / {{ view.grade }}</span>
      </p>
      <p v-if="!view" class="muted">아직 동기화된 포인트 뷰가 없습니다. 적립을 해보세요.</p>

      <form class="form inline" @submit.prevent="onAccumulate">
        <label>
          적립 금액
          <input v-model.number="amount" type="number" min="1" required />
        </label>
        <button type="submit" :disabled="busy || !member">적립</button>
        <button type="button" class="secondary" :disabled="busy || !member" @click="onDeduct">차감</button>
        <button type="button" class="ghost" :disabled="busy || !member" @click="refreshView">새로고침</button>
      </form>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { clearAuth } from '../auth'

const router = useRouter()
const member = ref(null)
const view = ref(null)
const amount = ref(1000)
const busy = ref(false)
const error = ref('')
const notice = ref('')

async function load() {
  error.value = ''
  member.value = await api.me()
  await refreshView()
}

async function refreshView() {
  if (!member.value) return
  view.value = await api.view(member.value.memberId)
}

async function waitAndRefresh() {
  // Kafka → Redis 뷰 반영 대기 (데모용)
  await new Promise((r) => setTimeout(r, 800))
  await refreshView()
}

async function onAccumulate() {
  if (!member.value || amount.value < 1) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await api.accumulate(member.value.memberId, amount.value)
    notice.value = '적립 요청을 보냈습니다.'
    await waitAndRefresh()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

async function onDeduct() {
  if (!member.value || amount.value < 1) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await api.deduct(member.value.memberId, amount.value)
    notice.value = '차감 요청을 보냈습니다.'
    await waitAndRefresh()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

async function onLogout() {
  busy.value = true
  try {
    await api.logout()
  } catch {
    // ignore
  } finally {
    clearAuth()
    busy.value = false
    await router.push('/login')
  }
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    error.value = e.message
    if (String(e.message).includes('401') || !member.value) {
      clearAuth()
      await router.push('/login')
    }
  }
})
</script>
