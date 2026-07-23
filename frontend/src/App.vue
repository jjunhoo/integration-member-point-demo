<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink to="/" class="brand">Membership</RouterLink>
      <nav v-if="!isAuthed" class="nav">
        <RouterLink to="/login">로그인</RouterLink>
        <RouterLink to="/register">회원가입</RouterLink>
      </nav>
    </header>
    <main class="page">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { isLoggedIn } from './auth'

const isAuthed = isLoggedIn
</script>
