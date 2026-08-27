import { zodResolver } from '@hookform/resolvers/zod'
import {
  ArrowRightIcon as ArrowRight,
  CheckCircleIcon as CheckCircle,
  EyeIcon as Eye,
  EyeSlashIcon as EyeSlash,
  ShieldCheckIcon as ShieldCheck,
} from '@phosphor-icons/react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'
import { z } from 'zod'
import { Brand } from '../components/Brand'
import { ApiError } from '../lib/api'
import { useAuth } from '../state/AuthContext'

const loginSchema = z.object({
  email: z.email('Informe um e-mail válido.'),
  password: z.string().min(1, 'Informe sua senha.'),
})

const registerSchema = loginSchema.extend({
  name: z.string().trim().min(2, 'Informe seu nome.').max(100),
  password: z.string().min(10, 'Use pelo menos 10 caracteres.').max(72),
})

type LoginForm = z.infer<typeof loginSchema>
type RegisterForm = z.infer<typeof registerSchema>
type AuthForm = LoginForm & Partial<Pick<RegisterForm, 'name'>>

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { session, login, register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const isRegister = mode === 'register'
  const schema = isRegister ? registerSchema : loginSchema
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AuthForm>({ resolver: zodResolver(schema) })

  if (session) return <Navigate to="/app" replace />

  async function submit(values: AuthForm) {
    setSubmitError(null)
    try {
      if (isRegister) await registerUser(values as RegisterForm)
      else await login(values)
      void navigate('/app', { replace: true })
    } catch (error) {
      setSubmitError(error instanceof ApiError ? error.message : 'Não foi possível entrar agora.')
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-story" aria-label="Sobre o CasaContas">
        <Brand />
        <div className="auth-story__copy">
          <span className="eyebrow eyebrow--light">Finanças da casa, sem ruído</span>
          <h1>Cada conta no seu lugar. Cada pessoa sabendo a sua parte.</h1>
          <p>
            Divida despesas, acompanhe reembolsos e preserve o histórico da casa em um só lugar.
          </p>
          <ul className="auth-benefits">
            <li>
              <CheckCircle weight="fill" /> rateios exatos, inclusive nos centavos
            </li>
            <li>
              <CheckCircle weight="fill" /> confirmações sem pagamento duplicado
            </li>
            <li>
              <ShieldCheck weight="fill" /> acesso isolado para cada casa
            </li>
          </ul>
        </div>
        <p className="auth-story__note">Feito para casas reais, com acordos claros.</p>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <span className="eyebrow">
            {isRegister ? 'Comece por aqui' : 'Que bom ter você de volta'}
          </span>
          <h2>{isRegister ? 'Crie sua conta' : 'Entre na sua casa'}</h2>
          <p className="muted">
            {isRegister
              ? 'Leva menos de um minuto. Depois você cria uma casa ou aceita um convite.'
              : 'Use o mesmo e-mail com que você entrou na casa.'}
          </p>

          <form className="form-stack" onSubmit={handleSubmit(submit)} noValidate>
            {isRegister && (
              <label className="field">
                <span>Seu nome</span>
                <input
                  autoComplete="name"
                  placeholder="Como podemos chamar você?"
                  {...register('name')}
                />
                {errors.name && <small role="alert">{errors.name.message}</small>}
              </label>
            )}
            <label className="field">
              <span>E-mail</span>
              <input
                type="email"
                autoComplete="email"
                placeholder="voce@exemplo.com"
                {...register('email')}
              />
              {errors.email && <small role="alert">{errors.email.message}</small>}
            </label>
            <label className="field">
              <span>Senha</span>
              <span className="password-field">
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  placeholder={isRegister ? 'Pelo menos 10 caracteres' : 'Sua senha'}
                  {...register('password')}
                />
                <button
                  type="button"
                  className="icon-button"
                  onClick={() => {
                    setShowPassword((value) => !value)
                  }}
                  aria-label={showPassword ? 'Esconder senha' : 'Mostrar senha'}
                >
                  {showPassword ? <EyeSlash /> : <Eye />}
                </button>
              </span>
              {errors.password && <small role="alert">{errors.password.message}</small>}
            </label>

            {submitError && (
              <div className="inline-error" role="alert">
                {submitError}
              </div>
            )}

            <button className="button button--primary button--wide" disabled={isSubmitting}>
              {isSubmitting ? 'Só um instante…' : isRegister ? 'Criar minha conta' : 'Entrar'}
              {!isSubmitting && <ArrowRight weight="bold" />}
            </button>
          </form>

          <p className="auth-switch">
            {isRegister ? 'Já tem uma conta?' : 'É sua primeira vez?'}{' '}
            <Link to={isRegister ? '/entrar' : '/cadastro'}>
              {isRegister ? 'Entre aqui' : 'Crie sua conta'}
            </Link>
          </p>
        </div>
      </section>
    </main>
  )
}
