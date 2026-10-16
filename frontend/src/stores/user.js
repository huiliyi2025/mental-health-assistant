import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户登录态 Store
 * <p>
 * 统一管理 token 与用户信息，并同步到 localStorage，供路由守卫、组件共享。
 */
export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(parseUserInfo())

    const isLoggedIn = computed(() => !!token.value)
    const isAdmin = computed(() => userInfo.value?.userType === 2)

    function parseUserInfo() {
        try {
            return JSON.parse(localStorage.getItem('userInfo') || 'null')
        } catch (e) {
            return null
        }
    }

    /**
     * 登录成功后写入 token 与用户信息
     */
    function setLogin(loginData) {
        token.value = loginData.token
        userInfo.value = loginData.userInfo
        localStorage.setItem('token', loginData.token)
        localStorage.setItem('userInfo', JSON.stringify(loginData.userInfo))
    }

    /**
     * 清除登录态
     */
    function clearLogin() {
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return { token, userInfo, isLoggedIn, isAdmin, setLogin, clearLogin }
})
