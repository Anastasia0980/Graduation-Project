export function clearAuthState () {
  localStorage.removeItem('auth_token')
  localStorage.removeItem('auth_role')
  localStorage.removeItem('auth_name')
  localStorage.removeItem('auth_email')
  sessionStorage.removeItem('mock_logged_out_view')
}

export function hasAuthToken () {
  return !!localStorage.getItem('auth_token')
}
