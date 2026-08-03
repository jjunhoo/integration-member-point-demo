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
        <div><dt>로그인 ID</dt><dd>{{ member.loginId || '-' }}</dd></div>
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
        <label>
          만료일시 (선택)
          <input v-model="expiresLocal" type="datetime-local" />
        </label>
        <button type="submit" :disabled="busy || !member">적립</button>
        <button type="button" class="secondary" :disabled="busy || !member" @click="onDeduct">차감</button>
        <button type="button" class="ghost" :disabled="busy || !member" @click="refreshAll">새로고침</button>
      </form>
      <p class="muted">만료일을 비우면 적립 시각 + 1년. 차감은 만료 임박 lot 부터(FEFO).</p>

      <h3 class="lot-title">적립 Lot</h3>
      <p v-if="!lots.length" class="muted">표시할 lot 이 없습니다.</p>
      <table v-else class="lot-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>잔여 / 원금</th>
            <th>적립</th>
            <th>만료</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="lot in lots" :key="lot.id">
            <td>{{ lot.id }}</td>
            <td>{{ lot.remainingAmount.toLocaleString() }} / {{ lot.originalAmount.toLocaleString() }}</td>
            <td>{{ formatInstant(lot.earnedAt) }}</td>
            <td>{{ formatInstant(lot.expiresAt) }}</td>
            <td>{{ lotStatus(lot) }}</td>
          </tr>
        </tbody>
      </table>
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
const lots = ref([])
const amount = ref(1000)
const expiresLocal = ref('')
const busy = ref(false)
const error = ref('')
const notice = ref('')

function formatInstant(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function lotStatus(lot) {
  if (lot.remainingAmount <= 0) return '소진/만료'
  return lot.usable ? '사용가능' : '만료'
}

function toExpiresAtIso() {
  if (!expiresLocal.value) return null
  const date = new Date(expiresLocal.value)
  if (Number.isNaN(date.getTime())) {
    throw new Error('만료일시 형식이 올바르지 않습니다.')
  }
  return date.toISOString()
}

async function load() {
  error.value = ''
  member.value = await api.me()
  await refreshAll()
}

async function refreshView() {
  if (!member.value) return
  view.value = await api.view(member.value.memberId)
}

async function refreshLots() {
  if (!member.value) return
  lots.value = await api.pointLots(member.value.memberId)
}

async function refreshAll() {
  await Promise.all([refreshView(), refreshLots()])
}

async function waitAndRefresh() {
  // Kafka → Redis 뷰 반영 대기 (데모용)
  await new Promise((r) => setTimeout(r, 800))
  await refreshAll()
}

async function onAccumulate() {
  if (!member.value || amount.value < 1) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    const expiresAt = toExpiresAtIso()
    await api.accumulate(member.value.memberId, amount.value, expiresAt)
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
