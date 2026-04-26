import { clearAuthState } from './auth'
import { ElMessageBox } from 'element-plus'

const DEFAULT_AUTH_EXPIRED_MESSAGE = '会话已过期，请重新登录后操作'
const AUTH_EXPIRED_PENDING_KEY = 'auth_expired_pending_confirm'

export class AuthExpiredError extends Error {
  constructor (message = DEFAULT_AUTH_EXPIRED_MESSAGE, options = {}) {
    super(message)
    this.name = 'AuthExpiredError'
    this.alreadyHandled = !!options.alreadyHandled
  }
}

export function isAuthExpiredError (error) {
  return error instanceof AuthExpiredError && !error.alreadyHandled
}

let fetchInterceptorInstalled = false
let authExpiredDialogPromise = null
let activeRouter = null

function markAuthExpiredPendingConfirm (pending) {
  if (typeof window === 'undefined') return
  if (pending) {
    sessionStorage.setItem(AUTH_EXPIRED_PENDING_KEY, '1')
  } else {
    sessionStorage.removeItem(AUTH_EXPIRED_PENDING_KEY)
  }
}

export function isAuthExpiredPendingConfirm () {
  if (typeof window === 'undefined') return false
  return sessionStorage.getItem(AUTH_EXPIRED_PENDING_KEY) === '1'
}

export function notifyAuthExpiredAndRedirect (router, message = DEFAULT_AUTH_EXPIRED_MESSAGE) {
  const currentPath = router?.currentRoute?.value?.path || ''
  if (currentPath === '/login') return Promise.resolve()

  clearAuthState()
  if (authExpiredDialogPromise) {
    return authExpiredDialogPromise
  }

  markAuthExpiredPendingConfirm(true)
  authExpiredDialogPromise = ElMessageBox.confirm(message, '提示', {
    type: 'warning',
    zIndex: 6000,
    showClose: false,
    closeOnClickModal: false,
    closeOnPressEscape: false,
    confirmButtonText: '确定',
    showCancelButton: false
  })
    .catch(() => {})
    .finally(() => {
      markAuthExpiredPendingConfirm(false)
      authExpiredDialogPromise = null
      const latestPath = router?.currentRoute?.value?.path || ''
      if (latestPath !== '/login') {
        router?.push('/login')
      }
    })

  return authExpiredDialogPromise
}

export function installGlobalAuthInterceptor (router) {
  activeRouter = router || activeRouter
  if (fetchInterceptorInstalled || typeof window === 'undefined' || typeof window.fetch !== 'function') {
    return
  }
  const rawFetch = window.fetch.bind(window)
  window.fetch = async (...args) => {
    const response = await rawFetch(...args)
    if (response.status === 401) {
      notifyAuthExpiredAndRedirect(activeRouter)
    }
    return response
  }
  fetchInterceptorInstalled = true
}

export async function apiRequest (url, options = {}) {
  const {
    requireAuth = true,
    authExpiredMessage = DEFAULT_AUTH_EXPIRED_MESSAGE,
    router,
    ...fetchOptions
  } = options
  const routerInstance = router || activeRouter

  const headers = { ...(fetchOptions.headers || {}) }

  if (requireAuth) {
    const token = localStorage.getItem('auth_token')
    if (!token) {
      notifyAuthExpiredAndRedirect(routerInstance, authExpiredMessage)
      return null
    }
    if (!headers.Authorization) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  const response = await fetch(url, { ...fetchOptions, headers })

  if (response.status === 401) {
    notifyAuthExpiredAndRedirect(routerInstance, authExpiredMessage)
    return null
  }

  let result = {}
  try {
    result = await response.json()
  } catch (e) {
    result = {}
  }

  if (!response.ok || result.code !== 0) {
    throw new Error(result.message || '请求失败')
  }

  return result
}
