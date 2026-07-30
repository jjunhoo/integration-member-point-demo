import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from './auth'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import NaverCallbackView from './views/NaverCallbackView.vue'
import RegisterView from './views/RegisterView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/auth/naver/callback', name: 'naver-callback', component: NaverCallbackView },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return { name: 'login' }
  }
  if ((to.name === 'login' || to.name === 'register') && isLoggedIn.value) {
    return { name: 'home' }
  }
  return true
})

export default router
