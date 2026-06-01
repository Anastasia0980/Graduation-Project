export const PASSWORD_RULE_MESSAGE = '密码需为8~16位，且同时包含大写字母、小写字母和数字'

export function isStrongPassword (password) {
  return /^(?=\S{8,16}$)(?=.*[A-Z])(?=.*[a-z])(?=.*\d).*$/.test(password || '')
}
