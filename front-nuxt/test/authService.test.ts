import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('~/infrastructure/http/api', () => ({
  api: { post: vi.fn() },
}))

import { authService } from '~/modules/auth/services/authService'
import { api } from '~/infrastructure/http/api'

describe('authService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('login() retorna AuthResponse del backend', async () => {
    const mockResponse = { token: 'abc.def.ghi', refreshToken: 'refresh123' }
    vi.mocked(api.post).mockResolvedValue({ data: mockResponse })

    const result = await authService.login({ username: 'admin', password: 'test-demo-password' })

    expect(api.post).toHaveBeenCalledWith('/auth/login', { username: 'admin', password: 'test-demo-password' })
    expect(result).toEqual(mockResponse)
  })

  it('solicitarResetPassword() llama al endpoint correcto', async () => {
    vi.mocked(api.post).mockResolvedValue({})

    await authService.solicitarResetPassword('test@example.com')

    expect(api.post).toHaveBeenCalledWith('/auth/forgot-password', { email: 'test@example.com' })
  })

  it('resetPassword() envía token y nueva contraseña', async () => {
    vi.mocked(api.post).mockResolvedValue({})

    await authService.resetPassword('reset-token-123', 'nuevaPass1!')

    expect(api.post).toHaveBeenCalledWith('/auth/reset-password', {
      token: 'reset-token-123',
      nuevaPassword: 'nuevaPass1!',
    })
  })
})
