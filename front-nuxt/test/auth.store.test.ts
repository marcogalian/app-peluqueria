import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '~/modules/auth/store/auth.store'

function makeToken(payload: object): string {
  const b64 = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.fake`
}

const futureExp = Math.floor(Date.now() / 1000) + 3600
const expiredExp = Math.floor(Date.now() / 1000) - 3600

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('guardarSesion() establece accessToken y usuario desde JWT', () => {
    const store = useAuthStore()
    const token = makeToken({ sub: 'maria', email: 'maria@test.com', roles: ['ROLE_ADMIN'], exp: futureExp })

    store.guardarSesion(token)

    expect(store.accessToken).toBe(token)
    expect(store.usuario?.username).toBe('maria')
    expect(store.usuario?.rol).toBe('ROLE_ADMIN')
    expect(localStorage.getItem('access_token')).toBe(token)
  })

  it('isAuthenticated es true con token válido', () => {
    const store = useAuthStore()
    store.guardarSesion(makeToken({ sub: 'u', exp: futureExp }))

    expect(store.isAuthenticated).toBe(true)
  })

  it('isAuthenticated es false con token expirado', () => {
    const store = useAuthStore()
    store.guardarSesion(makeToken({ sub: 'u', exp: expiredExp }))

    expect(store.isAuthenticated).toBe(false)
  })

  it('isAuthenticated es false sin token', () => {
    const store = useAuthStore()

    expect(store.isAuthenticated).toBe(false)
  })

  it('isAdmin es true con ROLE_ADMIN', () => {
    const store = useAuthStore()
    store.guardarSesion(makeToken({ sub: 'admin', roles: ['ROLE_ADMIN'], exp: futureExp }))

    expect(store.isAdmin).toBe(true)
    expect(store.isEmpleado).toBe(false)
  })

  it('isEmpleado es true con ROLE_HAIRDRESSER', () => {
    const store = useAuthStore()
    store.guardarSesion(makeToken({ sub: 'juan', roles: ['ROLE_HAIRDRESSER'], exp: futureExp }))

    expect(store.isEmpleado).toBe(true)
    expect(store.isAdmin).toBe(false)
  })

  it('cerrarSesion() limpia estado y localStorage', () => {
    const store = useAuthStore()
    store.guardarSesion(makeToken({ sub: 'u', exp: futureExp }))

    store.cerrarSesion()

    expect(store.accessToken).toBeNull()
    expect(store.usuario).toBeNull()
    expect(localStorage.getItem('access_token')).toBeNull()
  })

  it('restaurarSesion() recupera sesión válida de localStorage', () => {
    const token = makeToken({ sub: 'maria', roles: ['ROLE_ADMIN'], exp: futureExp })
    localStorage.setItem('access_token', token)
    const store = useAuthStore()

    store.restaurarSesion()

    expect(store.accessToken).toBe(token)
    expect(store.usuario?.username).toBe('maria')
  })

  it('restaurarSesion() limpia sesión si token expirado', () => {
    const token = makeToken({ sub: 'u', exp: expiredExp })
    localStorage.setItem('access_token', token)
    const store = useAuthStore()

    store.restaurarSesion()

    expect(store.accessToken).toBeNull()
    expect(localStorage.getItem('access_token')).toBeNull()
  })
})
